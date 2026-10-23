package com.inferno.gallery.data.ai

import android.content.Context
import android.util.Log
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.FaceDao
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.PersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages identity clustering with:
 * 1. Multi-Exemplar Anchors to prevent centroid drift across poses/lighting.
 * 2. 3-Photo Admission Rule: Clusters require >= 3 distinct photos before promotion to UI.
 * 3. Two-tier matching: Real-time multi-exemplar matching + periodic Chinese Whispers graph consolidation.
 */
class FaceClusteringManager(private val context: Context) {

    companion object {
        private const val TAG = "FaceClusteringManager"

        // Similarity thresholds on LFW/WebFace600k 512-D MobileFaceNet embeddings
        const val MATCH_THRESHOLD = 0.65f          // Minimum similarity to associate with an existing identity
        const val GRAPH_EDGE_THRESHOLD = 0.68f     // Stricter threshold for building graph edges during batch clustering
        const val PROMOTION_MIN_PHOTOS = 3         // 3-Photo Admission Rule: prevents stranger pollution in UI
        const val MAX_EXEMPLARS_PER_CLUSTER = 3    // Number of clean frontal exemplars stored per identity

        @Volatile
        private var INSTANCE: FaceClusteringManager? = null

        fun getInstance(context: Context): FaceClusteringManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FaceClusteringManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val faceDao: FaceDao = DatabaseProvider.getDatabase(context).faceDao()
    private val mutex = Mutex()

    data class ClusterCacheItem(
        val personId: Long,
        val exemplars: MutableList<FloatArray>,
        var centroid: FloatArray,
        var faceCount: Int,
        var isPromoted: Boolean,
        var coverCropPath: String?
    )

    private val clusterCache = mutableListOf<ClusterCacheItem>()
    private var isCacheInitialized = false

    private suspend fun ensureCacheLoaded() {
        if (isCacheInitialized) return
        val persons = faceDao.getAllPersonsWithCentroids()
        clusterCache.clear()

        for (person in persons) {
            val centroid = person.centroid ?: continue
            val exemplarFaces = faceDao.getExemplarFacesForPerson(person.id)
            val exemplars = if (exemplarFaces.isNotEmpty()) {
                exemplarFaces.map { it.embedding }.toMutableList()
            } else {
                mutableListOf(centroid)
            }

            clusterCache.add(
                ClusterCacheItem(
                    personId = person.id,
                    exemplars = exemplars,
                    centroid = centroid,
                    faceCount = person.faceCount,
                    isPromoted = person.isPromoted,
                    coverCropPath = person.coverCropPath
                )
            )
        }
        isCacheInitialized = true
    }

    /**
     * Assigns a newly detected face to the best matching person cluster or spawns a new candidate cluster.
     * Guaranteed thread-safe via mutex.
     */
    suspend fun assignFaceToCluster(
        mediaId: Long,
        embedding: FloatArray,
        bboxLeft: Float = 0f,
        bboxTop: Float = 0f,
        bboxRight: Float = 0f,
        bboxBottom: Float = 0f,
        qualityScore: Float = 0.5f,
        cropCachePath: String? = null
    ): Long = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureCacheLoaded()

            var bestPersonId = -1L
            var maxSimilarity = -1f
            var bestClusterIndex = -1

            // 1. Multi-exemplar matching against all known clusters
            for (i in clusterCache.indices) {
                val cluster = clusterCache[i]
                for (exemplar in cluster.exemplars) {
                    val sim = FaceRecognitionEngine.cosineSimilarity(embedding, exemplar)
                    if (sim > maxSimilarity) {
                        maxSimilarity = sim
                        bestPersonId = cluster.personId
                        bestClusterIndex = i
                    }
                }
            }

            if (maxSimilarity >= MATCH_THRESHOLD && bestClusterIndex != -1) {
                // Match confirmed: update existing cluster
                val cluster = clusterCache[bestClusterIndex]
                val newCount = cluster.faceCount + 1

                // Update running centroid: C_new = normalize(C_old * N + embedding)
                val updatedCentroid = FloatArray(embedding.size)
                for (j in embedding.indices) {
                    updatedCentroid[j] = cluster.centroid[j] * cluster.faceCount + embedding[j]
                }
                val normalizedCentroid = FaceRecognitionEngine.normalizeL2(updatedCentroid)

                // Update exemplars if high quality frontal face
                var isExemplar = false
                if (qualityScore >= 0.70f && cluster.exemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                    cluster.exemplars.add(embedding)
                    isExemplar = true
                }

                cluster.centroid = normalizedCentroid
                cluster.faceCount = newCount
                if (cluster.coverCropPath == null && cropCachePath != null) {
                    cluster.coverCropPath = cropCachePath
                }

                // 3-Photo Admission Rule: promote cluster once 3 distinct photos contain this person
                val shouldPromote = if (!cluster.isPromoted) {
                    val distinctPhotos = faceDao.countDistinctMediaForPerson(cluster.personId) + 1
                    distinctPhotos >= PROMOTION_MIN_PHOTOS
                } else {
                    true
                }
                cluster.isPromoted = shouldPromote

                // Persist updates
                val existingPerson = faceDao.getPersonById(cluster.personId)
                if (existingPerson != null) {
                    faceDao.updatePerson(
                        existingPerson.copy(
                            centroid = normalizedCentroid,
                            faceCount = newCount,
                            coverCropPath = cluster.coverCropPath,
                            isPromoted = shouldPromote
                        )
                    )
                }

                val faceEntity = FaceEntity(
                    mediaId = mediaId,
                    personId = cluster.personId,
                    bboxLeft = bboxLeft,
                    bboxTop = bboxTop,
                    bboxRight = bboxRight,
                    bboxBottom = bboxBottom,
                    qualityScore = qualityScore,
                    isExemplar = isExemplar,
                    cropCachePath = cropCachePath,
                    embedding = embedding
                )
                faceDao.insertFace(faceEntity)
                return@withContext cluster.personId
            } else {
                // No match: spawn new unpromoted candidate cluster
                val newPerson = PersonEntity(
                    name = null,
                    coverCropPath = cropCachePath,
                    isHidden = false,
                    isFavorite = false,
                    isPromoted = false, // Must earn promotion via 3-photo rule
                    faceCount = 1,
                    centroid = embedding
                )
                val newPersonId = faceDao.insertPerson(newPerson)

                val faceEntity = FaceEntity(
                    mediaId = mediaId,
                    personId = newPersonId,
                    bboxLeft = bboxLeft,
                    bboxTop = bboxTop,
                    bboxRight = bboxRight,
                    bboxBottom = bboxBottom,
                    qualityScore = qualityScore,
                    isExemplar = true,
                    cropCachePath = cropCachePath,
                    embedding = embedding
                )
                faceDao.insertFace(faceEntity)

                clusterCache.add(
                    ClusterCacheItem(
                        personId = newPersonId,
                        exemplars = mutableListOf(embedding),
                        centroid = embedding,
                        faceCount = 1,
                        isPromoted = false,
                        coverCropPath = cropCachePath
                    )
                )

                return@withContext newPersonId
            }
        }
    }

    /**
     * Merges two person identities into one (e.g. from user action or periodic consolidation).
     */
    suspend fun mergePersons(sourcePersonId: Long, targetPersonId: Long) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (sourcePersonId == targetPersonId) return@withContext
            ensureCacheLoaded()

            val source = faceDao.getPersonById(sourcePersonId) ?: return@withContext
            val target = faceDao.getPersonById(targetPersonId) ?: return@withContext

            val totalCount = source.faceCount + target.faceCount
            val mergedCentroid = if (source.centroid != null && target.centroid != null) {
                val combined = FloatArray(target.centroid.size)
                for (i in combined.indices) {
                    combined[i] = target.centroid[i] * target.faceCount + source.centroid[i] * source.faceCount
                }
                FaceRecognitionEngine.normalizeL2(combined)
            } else {
                target.centroid ?: source.centroid ?: FloatArray(FaceModelRegistry.EMBEDDING_DIM)
            }

            val cover = target.coverCropPath ?: source.coverCropPath
            faceDao.mergePersons(
                sourcePersonId = sourcePersonId,
                targetPersonId = targetPersonId,
                mergedCentroid = mergedCentroid,
                faceCount = totalCount,
                coverCropPath = cover
            )

            // Invalidate cache to reflect merged state
            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Merged person $sourcePersonId into $targetPersonId (total $totalCount faces)")
        }
    }
}
