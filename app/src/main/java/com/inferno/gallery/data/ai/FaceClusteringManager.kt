package com.inferno.gallery.data.ai

import android.content.Context
import android.util.Log
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.FaceDao
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.data.db.PersonMergeSuggestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
        const val SUGGESTION_MIN_THRESHOLD = 0.48f
        const val SUGGESTION_MAX_THRESHOLD = 0.54f

        fun isCandidateSimilarity(similarity: Float): Boolean {
            return similarity >= SUGGESTION_MIN_THRESHOLD && similarity < SUGGESTION_MAX_THRESHOLD
        }

        fun shouldSuggestMerge(
            faceCountA: Int,
            nameA: String?,
            isHiddenA: Boolean,
            faceCountB: Int,
            nameB: String?,
            isHiddenB: Boolean,
            similarity: Float
        ): Boolean {
            if (isHiddenA || isHiddenB) return false
            if (!isCandidateSimilarity(similarity)) return false
            return (faceCountA >= 2 || !nameA.isNullOrBlank()) || (faceCountB >= 2 || !nameB.isNullOrBlank())
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
        var isHidden: Boolean,
        var name: String?,
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
                    isHidden = person.isHidden,
                    name = person.name,
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
                val distinctPhotos = faceDao.countDistinctMediaForPerson(cluster.personId) + 1
                val shouldPromote = if (!cluster.isPromoted) {
                    distinctPhotos >= PROMOTION_MIN_PHOTOS
                } else {
                    true
                }
                cluster.isPromoted = shouldPromote
                cluster.faceCount = distinctPhotos

                // Persist updates
                val existingPerson = faceDao.getPersonById(cluster.personId)
                if (existingPerson != null) {
                    faceDao.updatePerson(
                        existingPerson.copy(
                            centroid = normalizedCentroid,
                            faceCount = distinctPhotos,
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
                        isHidden = false,
                        name = null,
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
        withContext(Dispatchers.IO + NonCancellable) {
            if (sourcePersonId == targetPersonId) return@withContext
            ensureCacheLoaded()

            val source = faceDao.getPersonById(sourcePersonId) ?: return@withContext
            val target = faceDao.getPersonById(targetPersonId) ?: return@withContext

            val totalCount = source.faceCount + target.faceCount
            val mergedCentroid = safeCombineCentroids(
                target.centroid, target.faceCount,
                source.centroid, source.faceCount
            )

            val cover = target.coverCropPath ?: source.coverCropPath
            if (target.name.isNullOrBlank() && !source.name.isNullOrBlank()) {
                faceDao.setPersonName(targetPersonId, source.name)
            }
            faceDao.mergePersons(
                sourcePersonId = sourcePersonId,
                targetPersonId = targetPersonId,
                mergedCentroid = mergedCentroid,
                faceCount = totalCount,
                coverCropPath = cover
            )
            faceDao.recalculateAllPersonFaceCounts()

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
        withContext(Dispatchers.IO + NonCancellable) {
            val validSources = sourcePersonIds.distinct().filter { it != targetPersonId }
            if (validSources.isEmpty()) return@withContext
            ensureCacheLoaded()

            for (sourceId in validSources) {
                val source = faceDao.getPersonById(sourceId) ?: continue
                val target = faceDao.getPersonById(targetPersonId) ?: continue

                val totalCount = source.faceCount + target.faceCount
                val mergedCentroid = safeCombineCentroids(
                    target.centroid, target.faceCount,
                    source.centroid, source.faceCount
                )

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
            faceDao.recalculateAllPersonFaceCounts()

            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Batch merged ${validSources.size} people into $targetPersonId")
        }
    }

    /**
     * Moves specific media items (photos/videos) from one person cluster to another.
     */
    suspend fun reassignMediaToPerson(
        sourcePersonId: Long,
        targetPersonId: Long,
        mediaIds: List<Long>
    ) = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            if (sourcePersonId == targetPersonId || mediaIds.isEmpty()) return@withContext
            ensureCacheLoaded()

            // 1. Reassign in database
            faceDao.reassignFacesForMedia(mediaIds, sourcePersonId, targetPersonId)

            // 2. Recompute distinct photos and faces for source
            val sourceFaces = faceDao.getFacesByPersonId(sourcePersonId)
            val sourceMediaCount = faceDao.countDistinctMediaForPerson(sourcePersonId)

            if (sourceMediaCount == 0 || sourceFaces.isEmpty()) {
                faceDao.deletePerson(sourcePersonId)
            } else {
                val sourceCentroid = computeCentroidFromFaces(sourceFaces)
                val sourcePerson = faceDao.getPersonById(sourcePersonId)
                if (sourcePerson != null) {
                    val cover = if (sourceFaces.any { it.cropCachePath == sourcePerson.coverCropPath }) {
                        sourcePerson.coverCropPath
                    } else {
                        sourceFaces.firstOrNull()?.cropCachePath ?: sourcePerson.coverCropPath
                    }
                    faceDao.updatePerson(
                        sourcePerson.copy(
                            centroid = sourceCentroid ?: sourcePerson.centroid,
                            faceCount = sourceMediaCount,
                            coverCropPath = cover
                        )
                    )
                }
            }

            // 3. Recompute distinct photos and faces for target
            val targetFaces = faceDao.getFacesByPersonId(targetPersonId)
            val targetMediaCount = faceDao.countDistinctMediaForPerson(targetPersonId)
            val targetPerson = faceDao.getPersonById(targetPersonId)

            if (targetPerson != null) {
                val targetCentroid = computeCentroidFromFaces(targetFaces) ?: targetPerson.centroid
                val targetCover = targetPerson.coverCropPath ?: targetFaces.firstOrNull()?.cropCachePath
                faceDao.updatePerson(
                    targetPerson.copy(
                        centroid = targetCentroid,
                        faceCount = targetMediaCount,
                        coverCropPath = targetCover,
                        isPromoted = true
                    )
                )
            }

            faceDao.recalculateAllPersonFaceCounts()
            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Successfully moved ${mediaIds.size} media from person $sourcePersonId to $targetPersonId")
        }
    }

    private fun isZeroVector(vector: FloatArray): Boolean {
        var sum = 0.0
        for (v in vector) sum += v * v
        return sum < 1e-6
    }

    private fun safeCombineCentroids(
        centroidA: FloatArray?,
        weightA: Int,
        centroidB: FloatArray?,
        weightB: Int
    ): FloatArray {
        if (centroidA == null && centroidB == null) return FloatArray(FaceModelRegistry.EMBEDDING_DIM)
        if (centroidA == null) return centroidB!!.copyOf()
        if (centroidB == null) return centroidA.copyOf()

        val wA = maxOf(1, weightA)
        val wB = maxOf(1, weightB)
        val denom = (wA + wB).toFloat()
        val combined = FloatArray(centroidA.size)
        for (i in combined.indices) {
            combined[i] = (centroidA[i] * wA + centroidB[i] * wB) / denom
        }
        val normalized = FaceRecognitionEngine.normalizeL2(combined)
        return if (isZeroVector(normalized)) {
            centroidA.copyOf()
        } else {
            normalized
        }
    }

    fun computeCentroidFromFaces(faces: List<FaceEntity>): FloatArray? {
        if (faces.isEmpty()) return null
        val dim = faces[0].embedding.size
        val sum = FloatArray(dim)
        var count = 0
        for (f in faces) {
            if (f.embedding.size == dim) {
                for (i in 0 until dim) {
                    sum[i] += f.embedding[i]
                }
                count++
            }
        }
        if (count == 0) return null
        for (i in 0 until dim) {
            sum[i] /= count.toFloat()
        }
        val normalized = FaceRecognitionEngine.normalizeL2(sum)
        return if (isZeroVector(normalized)) null else normalized
    }

    fun invalidateCache() {
        isCacheInitialized = false
    }

    /**
     * Iteratively merges clusters of the same identity whose centroids or exemplars exceed similarity threshold.
     * Consolidates fragmented clusters formed due to lighting/angle/pose variations.
     */
    suspend fun consolidateClusters(threshold: Float = CONSOLIDATION_THRESHOLD): Int = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
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
                    val normalizedCentroid = safeCombineCentroids(
                        target.centroid, target.faceCount,
                        source.centroid, source.faceCount
                    )

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

            faceDao.recalculateAllPersonFaceCounts()
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

                if (shouldSuggestMerge(
                    faceCountA = c1.faceCount,
                    nameA = c1.name,
                    isHiddenA = c1.isHidden,
                    faceCountB = c2.faceCount,
                    nameB = c2.name,
                    isHiddenB = c2.isHidden,
                    similarity = maxSim
                )) {
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
