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
 * 1. Diverse Pose/Lighting Exemplar Anchors (up to 10 per identity) to bridge frontal -> 3/4 -> profile poses.
 * 2. Quality-weighted centroids anchored on crisp frontal detections so steep profiles don't drift centroids.
 * 3. Corroborated pose-bridge matching + centroid sanity floor to boost profile recall without false merges.
 */
class FaceClusteringManager(private val context: Context) {

    companion object {
        private const val TAG = "FaceClusteringManager"

        // Similarity thresholds on LFW/WebFace600k 512-D MobileFaceNet embeddings
        const val MATCH_THRESHOLD = 0.52f          // Direct match threshold
        const val CONSOLIDATION_THRESHOLD = 0.54f  // Direct consolidation threshold
        const val MIN_CENTROID_SANITY = 0.38f      // Blocks single-outlier exemplar chaining against strangers
        const val POSE_BRIDGE_EXEMPLAR_MIN = 0.495f
        const val POSE_BRIDGE_SECOND_MIN = 0.465f
        const val POSE_BRIDGE_CENTROID_MIN = 0.43f
        const val SAME_PHOTO_COLLAGE_OVERRIDE_SIM = 0.72f
        const val PROMOTION_MIN_PHOTOS = 1
        const val MAX_EXEMPLARS_PER_CLUSTER = 10

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
            if (hasConflictingNames(nameA, nameB)) return false
            if (!isCandidateSimilarity(similarity)) return false
            return (faceCountA >= 2 || !nameA.isNullOrBlank()) || (faceCountB >= 2 || !nameB.isNullOrBlank())
        }

        fun hasConflictingNames(nameA: String?, nameB: String?): Boolean {
            val cleanA = nameA?.trim().orEmpty()
            val cleanB = nameB?.trim().orEmpty()
            return cleanA.isNotEmpty() && cleanB.isNotEmpty() && !cleanA.equals(cleanB, ignoreCase = true)
        }

        fun computeWeightedCentroid(
            embeddings: List<FloatArray>,
            weights: List<Float>
        ): FloatArray? {
            if (embeddings.isEmpty()) return null
            val dim = embeddings[0].size
            if (dim == 0) return null
            val sum = FloatArray(dim)
            var totalWeight = 0f
            for (idx in embeddings.indices) {
                val emb = embeddings[idx]
                if (emb.size != dim) continue
                val w = weights.getOrElse(idx) { 0.5f }.coerceIn(0.25f, 1.0f)
                for (i in 0 until dim) {
                    sum[i] += emb[i] * w
                }
                totalWeight += w
            }
            if (totalWeight <= 1e-6f) return null
            for (i in 0 until dim) {
                sum[i] /= totalWeight
            }
            val normalized = FaceRecognitionEngine.normalizeL2(sum)
            var energy = 0.0
            for (v in normalized) energy += v * v
            return if (energy < 1e-6) null else normalized
        }

        /**
         * Evaluates similarity between a candidate face embedding and an identity cluster.
         * Returns the effective similarity if accepted (either direct match with centroid sanity
         * or corroborated pose-bridge across exemplars + centroid), or `-1f` if rejected.
         */
        fun evaluateClusterSimilarity(
            embedding: FloatArray,
            centroid: FloatArray,
            exemplars: List<FloatArray>
        ): Float {
            val centroidSim = FaceRecognitionEngine.cosineSimilarity(embedding, centroid)
            var bestExSim = -1f
            var secondExSim = -1f
            for (ex in exemplars) {
                val s = FaceRecognitionEngine.cosineSimilarity(embedding, ex)
                if (s > bestExSim) {
                    secondExSim = bestExSim
                    bestExSim = s
                } else if (s > secondExSim) {
                    secondExSim = s
                }
            }

            val topDirect = maxOf(centroidSim, bestExSim)
            if (topDirect >= MATCH_THRESHOLD && centroidSim >= MIN_CENTROID_SANITY) {
                return topDirect
            }

            // Pose-bridge: side profile strongly matches a 3/4 exemplar AND is corroborated by
            // both the cluster centroid and a second reference anchor.
            val secondSupport = maxOf(secondExSim, centroidSim)
            if (bestExSim >= POSE_BRIDGE_EXEMPLAR_MIN &&
                secondSupport >= POSE_BRIDGE_SECOND_MIN &&
                centroidSim >= POSE_BRIDGE_CENTROID_MIN
            ) {
                return bestExSim
            }

            return -1f
        }

        fun isClusterMatch(
            embedding: FloatArray,
            centroid: FloatArray,
            exemplars: List<FloatArray>
        ): Boolean = evaluateClusterSimilarity(embedding, centroid, exemplars) > 0f

        /**
         * Returns consolidated similarity between two clusters if they belong to the same identity,
         * or `-1f` if they should remain separate. Never merges distinct user-named identities.
         */
        fun evaluateClusterConsolidation(
            centroidA: FloatArray,
            exemplarsA: List<FloatArray>,
            nameA: String?,
            centroidB: FloatArray,
            exemplarsB: List<FloatArray>,
            nameB: String?,
            threshold: Float = CONSOLIDATION_THRESHOLD
        ): Float {
            if (hasConflictingNames(nameA, nameB)) return -1f

            val centroidSim = FaceRecognitionEngine.cosineSimilarity(centroidA, centroidB)
            var maxExSim = -1f
            var secondExSim = -1f
            for (e1 in exemplarsA) {
                for (e2 in exemplarsB) {
                    val s = FaceRecognitionEngine.cosineSimilarity(e1, e2)
                    if (s > maxExSim) {
                        secondExSim = maxExSim
                        maxExSim = s
                    } else if (s > secondExSim) {
                        secondExSim = s
                    }
                }
            }

            val maxSim = maxOf(centroidSim, maxExSim)
            if (maxSim >= threshold && centroidSim >= 0.40f) {
                return maxSim
            }

            val secondSupport = maxOf(secondExSim, centroidSim)
            if (maxExSim >= 0.505f && secondSupport >= 0.475f && centroidSim >= POSE_BRIDGE_CENTROID_MIN) {
                return maxExSim
            }

            return -1f
        }

        fun shouldConsolidateClusters(
            centroidA: FloatArray,
            exemplarsA: List<FloatArray>,
            nameA: String?,
            centroidB: FloatArray,
            exemplarsB: List<FloatArray>,
            nameB: String?,
            threshold: Float = CONSOLIDATION_THRESHOLD
        ): Boolean = evaluateClusterConsolidation(
            centroidA, exemplarsA, nameA,
            centroidB, exemplarsB, nameB,
            threshold
        ) > 0f

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
        var centroidWeight: Float,
        var faceCount: Int,
        var bestQualityScore: Float,
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
            val bestQuality = exemplarFaces.maxOfOrNull { it.qualityScore } ?: 0.6f

            clusterCache.add(
                ClusterCacheItem(
                    personId = person.id,
                    exemplars = exemplars,
                    centroid = centroid,
                    centroidWeight = maxOf(1f, person.faceCount * 0.7f),
                    faceCount = person.faceCount,
                    bestQualityScore = bestQuality,
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
     * Assigns a newly detected face to the best matching person cluster or spawns a new cluster.
     * [excludedPersonIds] guards against assigning two distinct simultaneous faces in the same
     * group photo to the same identity on borderline similarity.
     */
    suspend fun assignFaceToCluster(
        mediaId: Long,
        embedding: FloatArray,
        bboxLeft: Float = 0f,
        bboxTop: Float = 0f,
        bboxRight: Float = 0f,
        bboxBottom: Float = 0f,
        qualityScore: Float = 0.5f,
        cropCachePath: String? = null,
        excludedPersonIds: Set<Long> = emptySet()
    ): Long = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureCacheLoaded()

            var maxSimilarity = -1f
            var bestClusterIndex = -1

            for (i in clusterCache.indices) {
                val cluster = clusterCache[i]
                val sim = evaluateClusterSimilarity(embedding, cluster.centroid, cluster.exemplars)
                if (sim <= 0f) continue
                if (cluster.personId in excludedPersonIds && sim < SAME_PHOTO_COLLAGE_OVERRIDE_SIM) {
                    continue
                }
                if (sim > maxSimilarity) {
                    maxSimilarity = sim
                    bestClusterIndex = i
                }
            }

            if (bestClusterIndex != -1) {
                val cluster = clusterCache[bestClusterIndex]
                val effectiveWeight = qualityScore.coerceIn(0.25f, 1.0f)

                val updatedCentroid = FloatArray(embedding.size)
                for (j in embedding.indices) {
                    updatedCentroid[j] = cluster.centroid[j] * cluster.centroidWeight + embedding[j] * effectiveWeight
                }
                val normalizedCentroid = FaceRecognitionEngine.normalizeL2(updatedCentroid)

                // Retain up to 10 pose/lighting-diverse exemplars per identity
                var isExemplar = false
                if (cluster.exemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                    val isDiverse = cluster.exemplars.all {
                        FaceRecognitionEngine.cosineSimilarity(embedding, it) < 0.88f
                    }
                    if (isDiverse) {
                        cluster.exemplars.add(embedding)
                        isExemplar = true
                    }
                }

                cluster.centroid = normalizedCentroid
                cluster.centroidWeight += effectiveWeight

                val hasManualCover = cluster.coverCropPath?.endsWith("_cover.webp") == true
                if (cropCachePath != null && (
                        cluster.coverCropPath == null ||
                            (!hasManualCover && cluster.bestQualityScore < 0.62f && qualityScore >= 0.80f)
                    )
                ) {
                    cluster.coverCropPath = cropCachePath
                }
                if (qualityScore > cluster.bestQualityScore) {
                    cluster.bestQualityScore = qualityScore
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

                val distinctPhotos = faceDao.countDistinctMediaForPerson(cluster.personId).coerceAtLeast(1)
                val shouldPromote = if (!cluster.isPromoted) {
                    distinctPhotos >= PROMOTION_MIN_PHOTOS
                } else {
                    true
                }
                cluster.isPromoted = shouldPromote
                cluster.faceCount = distinctPhotos

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

                Log.d(TAG, "Assigned face from media $mediaId to Person ${cluster.personId} (sim=$maxSimilarity, count=${cluster.faceCount})")
                return@withContext cluster.personId
            } else {
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
                        centroidWeight = qualityScore.coerceIn(0.25f, 1.0f),
                        faceCount = 1,
                        bestQualityScore = qualityScore,
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
     * Recomputes quality-weighted centroids and pose-diverse exemplars from stored FaceEntity rows
     * (e.g. after an in-place rescan upgrades embeddings).
     */
    suspend fun refreshAllClusterCentroids() = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            val persons = faceDao.getAllPersonsWithCentroids()
            for (person in persons) {
                val faces = faceDao.getFacesByPersonId(person.id)
                if (faces.isEmpty()) continue
                val updatedCentroid = computeCentroidFromFaces(faces) ?: continue

                // Ensure up to MAX_EXEMPLARS_PER_CLUSTER diverse faces are flagged as exemplars
                val selectedExemplars = ArrayList<FloatArray>(MAX_EXEMPLARS_PER_CLUSTER)
                // Seed with highest-quality frontal face first, then greedily add diverse angles
                val ordered = faces.sortedByDescending { it.qualityScore }
                for (face in ordered) {
                    val shouldBeExemplar = if (selectedExemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                        selectedExemplars.isEmpty() || selectedExemplars.all {
                            FaceRecognitionEngine.cosineSimilarity(face.embedding, it) < 0.88f
                        }
                    } else {
                        false
                    }
                    if (shouldBeExemplar) {
                        selectedExemplars.add(face.embedding)
                    }
                    if (face.isExemplar != shouldBeExemplar) {
                        faceDao.updateFace(face.copy(isExemplar = shouldBeExemplar))
                    }
                }

                val hasManualCover = person.coverCropPath?.endsWith("_cover.webp") == true
                val bestFaceCrop = ordered.firstOrNull { !it.cropCachePath.isNullOrBlank() }?.cropCachePath
                val updatedCover = if (!hasManualCover && person.coverCropPath == null && bestFaceCrop != null) {
                    bestFaceCrop
                } else {
                    person.coverCropPath
                }

                val distinctPhotos = faceDao.countDistinctMediaForPerson(person.id)
                faceDao.updatePerson(
                    person.copy(
                        centroid = updatedCentroid,
                        faceCount = distinctPhotos,
                        coverCropPath = updatedCover
                    )
                )
            }
            isCacheInitialized = false
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

            faceDao.reassignFacesForMedia(mediaIds, sourcePersonId, targetPersonId)

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
                        sourceFaces.maxByOrNull { it.qualityScore }?.cropCachePath ?: sourcePerson.coverCropPath
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

            val targetFaces = faceDao.getFacesByPersonId(targetPersonId)
            val targetMediaCount = faceDao.countDistinctMediaForPerson(targetPersonId)
            val targetPerson = faceDao.getPersonById(targetPersonId)

            if (targetPerson != null) {
                val targetCentroid = computeCentroidFromFaces(targetFaces) ?: targetPerson.centroid
                val targetCover = targetPerson.coverCropPath ?: targetFaces.maxByOrNull { it.qualityScore }?.cropCachePath
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
        return computeWeightedCentroid(
            embeddings = faces.map { it.embedding },
            weights = faces.map { it.qualityScore }
        )
    }

    fun invalidateCache() {
        isCacheInitialized = false
    }

    /**
     * Iteratively merges clusters of the same identity whose centroids or exemplars pass
     * consolidation (either high direct similarity with centroid sanity, or corroborated pose bridge).
     * Never merges two clusters that have different user-assigned names.
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
                        val sim = evaluateClusterConsolidation(
                            centroidA = c1.centroid,
                            exemplarsA = c1.exemplars,
                            nameA = c1.name,
                            centroidB = c2.centroid,
                            exemplarsB = c2.exemplars,
                            nameB = c2.name,
                            threshold = threshold
                        )
                        if (sim > bestSim) {
                            bestSim = sim
                            bestPair = Pair(i, j)
                        }
                    }
                }

                if (bestPair != null && bestSim > 0f) {
                    val (i, j) = bestPair
                    val c1 = clusterCache[i]
                    val c2 = clusterCache[j]

                    // Prefer keeping named cluster as target, otherwise larger cluster
                    val keepFirst = when {
                        !c1.name.isNullOrBlank() && c2.name.isNullOrBlank() -> true
                        c1.name.isNullOrBlank() && !c2.name.isNullOrBlank() -> false
                        else -> c1.faceCount >= c2.faceCount
                    }
                    val (target, source) = if (keepFirst) Pair(c1, c2) else Pair(c2, c1)

                    val totalCount = target.faceCount + source.faceCount
                    val normalizedCentroid = safeCombineCentroids(
                        target.centroid, target.faceCount,
                        source.centroid, source.faceCount
                    )

                    val targetPerson = faceDao.getPersonById(target.personId)
                    val sourcePerson = faceDao.getPersonById(source.personId)

                    if (targetPerson?.name.isNullOrBlank() && !sourcePerson?.name.isNullOrBlank()) {
                        faceDao.setPersonName(target.personId, sourcePerson?.name)
                        target.name = sourcePerson?.name
                    }

                    val chosenCover = if (target.coverCropPath?.endsWith("_cover.webp") == true) {
                        target.coverCropPath
                    } else if (source.coverCropPath?.endsWith("_cover.webp") == true) {
                        source.coverCropPath
                    } else if (source.bestQualityScore > target.bestQualityScore + 0.18f && source.coverCropPath != null) {
                        source.coverCropPath
                    } else {
                        target.coverCropPath ?: source.coverCropPath
                    }

                    faceDao.mergePersons(
                        sourcePersonId = source.personId,
                        targetPersonId = target.personId,
                        mergedCentroid = normalizedCentroid,
                        faceCount = totalCount,
                        coverCropPath = chosenCover
                    )

                    for (ex in source.exemplars) {
                        if (target.exemplars.size < MAX_EXEMPLARS_PER_CLUSTER) {
                            val isDiverse = target.exemplars.all {
                                FaceRecognitionEngine.cosineSimilarity(ex, it) < 0.88f
                            }
                            if (isDiverse) {
                                target.exemplars.add(ex)
                            }
                        }
                    }
                    target.centroid = normalizedCentroid
                    target.centroidWeight += source.centroidWeight
                    target.faceCount = totalCount
                    target.bestQualityScore = maxOf(target.bestQualityScore, source.bestQualityScore)
                    target.coverCropPath = chosenCover
                    target.isPromoted = true

                    clusterCache.remove(source)
                    mergeCount++
                    changed = true
                }
            }

            faceDao.recalculateAllPersonFaceCounts()
            Log.d(TAG, "Cluster consolidation complete: performed $mergeCount merges.")

            computeAndSaveMergeSuggestions()

            mergeCount
        }
    }

    /**
     * Finds pairs of clusters with ambiguous similarity and saves them as PENDING merge suggestions.
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
