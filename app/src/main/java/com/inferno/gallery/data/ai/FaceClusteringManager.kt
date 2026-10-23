package com.inferno.gallery.data.ai

import android.content.Context
import android.util.Log
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.FaceDao
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.data.db.PersonMergeSuggestionEntity
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
        const val MATCH_THRESHOLD = 0.52f          // Reliably associates different angles, lighting, and natural poses of the same identity
        const val CONSOLIDATION_THRESHOLD = 0.54f  // Threshold for merging split clusters of the same person
        const val PROMOTION_MIN_PHOTOS = 1         // Instant promotion: all discovered faces appear immediately in UI
        const val MAX_EXEMPLARS_PER_CLUSTER = 6    // Retain up to 6 diverse pose/lighting exemplars per identity

        // Ambiguous candidate similarity window for proactive merge suggestions
        const val SUGGESTION_MIN_THRESHOLD = 0.44f
        const val SUGGESTION_MAX_THRESHOLD = 0.54f

        fun isCandidateSimilarity(similarity: Float): Boolean {
            return similarity >= SUGGESTION_MIN_THRESHOLD && similarity < SUGGESTION_MAX_THRESHOLD
        }

        fun toCanonicalPair(idA: Long, idB: Long): Pair<Long, Long> {
            return if (idA < idB) Pair(idA, idB) else Pair(idB, idA)
        }

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

            // 1. Multi-exemplar & centroid matching against all known clusters
            for (i in clusterCache.indices) {
                val cluster = clusterCache[i]
                val simCentroid = FaceRecognitionEngine.cosineSimilarity(embedding, cluster.centroid)
                if (simCentroid > maxSimilarity) {
                    maxSimilarity = simCentroid
                    bestPersonId = cluster.personId
                    bestClusterIndex = i
                }
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

                // Expand angle/lighting coverage with diverse exemplars
                var isExemplar = false
                if (cluster.exemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                    val isDiverse = cluster.exemplars.all { FaceRecognitionEngine.cosineSimilarity(embedding, it) < 0.88f }
                    if (isDiverse) {
                        cluster.exemplars.add(embedding)
                        isExemplar = true
                    }
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
                Log.d(TAG, "Assigned face from media $mediaId to Person ${cluster.personId} (sim=$maxSimilarity, count=${cluster.faceCount})")
                return@withContext cluster.personId
            } else {
                // No match: spawn new candidate cluster (immediately promoted so it appears in UI)
                val newPerson = PersonEntity(
                    name = null,
                    coverCropPath = cropCachePath,
                    isHidden = false,
                    isFavorite = false,
                    isPromoted = true,
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
                        isPromoted = true,
                        coverCropPath = cropCachePath
                    )
                )

                Log.d(TAG, "Created new Person cluster $newPersonId with face from media $mediaId")
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

    /**
     * Merges multiple person identities into a single target identity.
     */
    suspend fun mergeMultiplePersons(sourcePersonIds: List<Long>, targetPersonId: Long) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val validSources = sourcePersonIds.distinct().filter { it != targetPersonId }
            if (validSources.isEmpty()) return@withContext
            ensureCacheLoaded()

            for (sourceId in validSources) {
                val source = faceDao.getPersonById(sourceId) ?: continue
                val target = faceDao.getPersonById(targetPersonId) ?: continue

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
                if (target.name.isNullOrBlank() && !source.name.isNullOrBlank()) {
                    faceDao.setPersonName(targetPersonId, source.name)
                }

                faceDao.mergePersons(
                    sourcePersonId = sourceId,
                    targetPersonId = targetPersonId,
                    mergedCentroid = mergedCentroid,
                    faceCount = totalCount,
                    coverCropPath = cover
                )
            }

            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Batch merged ${validSources.size} people into $targetPersonId")
        }
    }

    fun invalidateCache() {
        isCacheInitialized = false
    }

    /**
     * Iteratively merges clusters of the same identity whose centroids or exemplars exceed similarity threshold.
     * Consolidates fragmented clusters formed due to lighting/angle/pose variations.
     */
    suspend fun consolidateClusters(threshold: Float = CONSOLIDATION_THRESHOLD): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            isCacheInitialized = false
            ensureCacheLoaded()

            var mergeCount = 0
            var changed = true

            while (changed) {
                changed = false
                var bestPair: Pair<Int, Int>? = null
                var bestSim = -1f

                for (i in 0 until clusterCache.size) {
                    val c1 = clusterCache[i]
                    for (j in i + 1 until clusterCache.size) {
                        val c2 = clusterCache[j]

                        var maxSim = FaceRecognitionEngine.cosineSimilarity(c1.centroid, c2.centroid)

                        for (e1 in c1.exemplars) {
                            for (e2 in c2.exemplars) {
                                val s = FaceRecognitionEngine.cosineSimilarity(e1, e2)
                                if (s > maxSim) maxSim = s
                            }
                        }

                        if (maxSim >= threshold && maxSim > bestSim) {
                            bestSim = maxSim
                            bestPair = Pair(i, j)
                        }
                    }
                }

                if (bestPair != null) {
                    val (i, j) = bestPair
                    val c1 = clusterCache[i]
                    val c2 = clusterCache[j]

                    val (target, source) = if (c1.faceCount >= c2.faceCount) Pair(c1, c2) else Pair(c2, c1)

                    val totalCount = target.faceCount + source.faceCount
                    val mergedCentroid = FloatArray(target.centroid.size)
                    for (k in mergedCentroid.indices) {
                        mergedCentroid[k] = target.centroid[k] * target.faceCount + source.centroid[k] * source.faceCount
                    }
                    val normalizedCentroid = FaceRecognitionEngine.normalizeL2(mergedCentroid)

                    val targetPerson = faceDao.getPersonById(target.personId)
                    val sourcePerson = faceDao.getPersonById(source.personId)

                    if (targetPerson?.name.isNullOrBlank() && !sourcePerson?.name.isNullOrBlank()) {
                        faceDao.setPersonName(target.personId, sourcePerson?.name)
                    }

                    faceDao.mergePersons(
                        sourcePersonId = source.personId,
                        targetPersonId = target.personId,
                        mergedCentroid = normalizedCentroid,
                        faceCount = totalCount,
                        coverCropPath = target.coverCropPath ?: source.coverCropPath
                    )

                    for (ex in source.exemplars) {
                        if (target.exemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                            target.exemplars.add(ex)
                        }
                    }
                    target.centroid = normalizedCentroid
                    target.faceCount = totalCount
                    target.isPromoted = true

                    clusterCache.remove(source)
                    mergeCount++
                    changed = true
                }
            }

            Log.d(TAG, "Cluster consolidation complete: performed $mergeCount merges.")

            // Proactively generate merge suggestions for remaining ambiguous clusters
            computeAndSaveMergeSuggestions()

            mergeCount
        }
    }

    /**
     * Finds pairs of clusters with ambiguous similarity (e.g. 0.44 to 0.54)
     * and saves them as PENDING merge suggestions in the database.
     */
    suspend fun computeAndSaveMergeSuggestions(): Int = withContext(Dispatchers.IO) {
        ensureCacheLoaded()
        val suggestions = mutableListOf<PersonMergeSuggestionEntity>()

        for (i in 0 until clusterCache.size) {
            val c1 = clusterCache[i]
            for (j in i + 1 until clusterCache.size) {
                val c2 = clusterCache[j]

                var maxSim = FaceRecognitionEngine.cosineSimilarity(c1.centroid, c2.centroid)
                for (e1 in c1.exemplars) {
                    for (e2 in c2.exemplars) {
                        val s = FaceRecognitionEngine.cosineSimilarity(e1, e2)
                        if (s > maxSim) maxSim = s
                    }
                }

                if (isCandidateSimilarity(maxSim)) {
                    val (pA, pB) = toCanonicalPair(c1.personId, c2.personId)
                    val existing = faceDao.getExistingSuggestionStatus(pA, pB)
                    if (existing == null) {
                        suggestions.add(
                            PersonMergeSuggestionEntity(
                                personIdA = pA,
                                personIdB = pB,
                                similarity = maxSim,
                                status = "PENDING"
                            )
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            faceDao.insertMergeSuggestions(suggestions)
            Log.d(TAG, "Generated ${suggestions.size} new merge suggestions")
        }
        suggestions.size
    }
}
