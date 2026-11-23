package com.inferno.gallery.data.ai

import android.content.Context
import android.util.Log
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.FaceDao
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.IgnoredFaceExemplarEntity
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.data.db.PersonMergeSuggestionEntity
import java.io.File
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
        // Broader one-shot pass triggered explicitly via "Find more matches"
        const val BROAD_SUGGESTION_MIN_THRESHOLD = 0.40f
        // Minimum similarity to highlight a person under "Closest matches" in the Merge picker
        const val CLOSEST_MATCH_HIGHLIGHT_THRESHOLD = 0.36f

        // Strict per-exemplar threshold for dropping recurring false detections ("Not a face").
        // Kept intentionally high and never averaged across unrelated objects so real faces are never swallowed.
        const val IGNORED_EXEMPLAR_MATCH_THRESHOLD = 0.68f
        const val IGNORED_EXEMPLAR_DEDUP_THRESHOLD = 0.88f
        const val MAX_IGNORED_EXEMPLARS_PER_CLUSTER = 4
        const val MAX_IGNORED_EXEMPLARS_TOTAL = 500

        /**
         * Returns true if [embedding] closely matches any individual dismissed non-face exemplar.
         */
        fun matchesIgnoredExemplar(
            embedding: FloatArray,
            ignoredExemplars: List<FloatArray>,
            threshold: Float = IGNORED_EXEMPLAR_MATCH_THRESHOLD
        ): Boolean {
            if (ignoredExemplars.isEmpty()) return false
            for (ex in ignoredExemplars) {
                if (FaceRecognitionEngine.cosineSimilarity(embedding, ex) >= threshold) {
                    return true
                }
            }
            return false
        }

        /**
         * Picks up to [maxToAdd] diverse embeddings from [candidates] that are not already
         * covered by [existingIgnored] (cosine similarity < [dedupThreshold]).
         */
        fun selectDiverseExemplarsToIgnore(
            candidates: List<FloatArray>,
            existingIgnored: List<FloatArray>,
            maxToAdd: Int = MAX_IGNORED_EXEMPLARS_PER_CLUSTER,
            dedupThreshold: Float = IGNORED_EXEMPLAR_DEDUP_THRESHOLD
        ): List<FloatArray> {
            if (candidates.isEmpty() || maxToAdd <= 0) return emptyList()
            val selected = ArrayList<FloatArray>(minOf(maxToAdd, candidates.size))
            for (cand in candidates) {
                if (selected.size >= maxToAdd) break
                val alreadyCovered = existingIgnored.any {
                    FaceRecognitionEngine.cosineSimilarity(cand, it) >= dedupThreshold
                } || selected.any {
                    FaceRecognitionEngine.cosineSimilarity(cand, it) >= dedupThreshold
                }
                if (!alreadyCovered) {
                    selected.add(cand)
                }
            }
            return selected
        }

        fun isCandidateSimilarity(
            similarity: Float,
            minThreshold: Float = SUGGESTION_MIN_THRESHOLD
        ): Boolean {
            return similarity >= minThreshold && similarity < SUGGESTION_MAX_THRESHOLD
        }

        fun shouldSuggestMerge(
            faceCountA: Int,
            nameA: String?,
            isHiddenA: Boolean,
            faceCountB: Int,
            nameB: String?,
            isHiddenB: Boolean,
            similarity: Float,
            minThreshold: Float = SUGGESTION_MIN_THRESHOLD,
            includeSinglePhotoPairs: Boolean = minThreshold < SUGGESTION_MIN_THRESHOLD,
            allowAboveMaxThreshold: Boolean = minThreshold < SUGGESTION_MIN_THRESHOLD
        ): Boolean {
            if (isHiddenA || isHiddenB) return false
            if (hasConflictingNames(nameA, nameB)) return false
            val matchesSimilarity = if (allowAboveMaxThreshold) {
                similarity >= minThreshold
            } else {
                isCandidateSimilarity(similarity, minThreshold)
            }
            if (!matchesSimilarity) return false
            if (includeSinglePhotoPairs) return faceCountA >= 1 && faceCountB >= 1
            return (faceCountA >= 2 || !nameA.isNullOrBlank()) || (faceCountB >= 2 || !nameB.isNullOrBlank())
        }

        /**
         * Splits merge-picker candidates into similarity-ranked "closest matches" (>= [minHighlightSimilarity])
         * and the remaining people in their existing order.
         */
        fun <T> partitionCandidatesBySimilarity(
            candidates: List<T>,
            idSelector: (T) -> Long,
            similarityScores: Map<Long, Float>,
            minHighlightSimilarity: Float = CLOSEST_MATCH_HIGHLIGHT_THRESHOLD,
            maxClosestCount: Int = 8
        ): Pair<List<T>, List<T>> {
            if (similarityScores.isEmpty()) return emptyList<T>() to candidates
            val closest = candidates
                .filter { (similarityScores[idSelector(it)] ?: 0f) >= minHighlightSimilarity }
                .sortedByDescending { similarityScores[idSelector(it)] ?: 0f }
                .take(maxClosestCount)
            val closestIds = closest.mapTo(HashSet(closest.size), idSelector)
            val others = candidates.filterNot { idSelector(it) in closestIds }
            return closest to others
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
         * or `-1f` if they should remain separate. Never merges distinct user-named identities or
         * mixes hidden clusters with visible clusters.
         */
        fun evaluateClusterConsolidation(
            centroidA: FloatArray,
            exemplarsA: List<FloatArray>,
            nameA: String?,
            centroidB: FloatArray,
            exemplarsB: List<FloatArray>,
            nameB: String?,
            threshold: Float = CONSOLIDATION_THRESHOLD,
            isHiddenA: Boolean = false,
            isHiddenB: Boolean = false
        ): Float {
            if (isHiddenA != isHiddenB) return -1f
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
            threshold: Float = CONSOLIDATION_THRESHOLD,
            isHiddenA: Boolean = false,
            isHiddenB: Boolean = false
        ): Boolean = evaluateClusterConsolidation(
            centroidA, exemplarsA, nameA,
            centroidB, exemplarsB, nameB,
            threshold,
            isHiddenA = isHiddenA,
            isHiddenB = isHiddenB
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
    private val ignoredExemplarsCache = mutableListOf<FloatArray>()
    private var isCacheInitialized = false

    private suspend fun ensureCacheLoaded() {
        if (isCacheInitialized) return
        val persons = faceDao.getAllPersonsWithCentroids()
        clusterCache.clear()
        ignoredExemplarsCache.clear()
        ignoredExemplarsCache.addAll(
            faceDao.getIgnoredFaceExemplars(MAX_IGNORED_EXEMPLARS_TOTAL).map { it.embedding }
        )

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
     * Checks whether a newly detected face embedding closely matches a previously dismissed
     * false detection ("Not a face") so future scans drop recurring non-person artifacts.
     */
    suspend fun isIgnoredFace(embedding: FloatArray): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureCacheLoaded()
            matchesIgnoredExemplar(embedding, ignoredExemplarsCache)
        }
    }

    /**
     * Permanently removes false-positive person clusters ("Not a face") from People and
     * optionally records up to [MAX_IGNORED_EXEMPLARS_PER_CLUSTER] diverse representative
     * embeddings per cluster to suppress recurring artifacts on future scans.
     * Actual photos and `media_face_status` rows remain untouched.
     */
    suspend fun dismissPersonsAsNotFaces(
        personIds: List<Long>,
        suppressSimilar: Boolean = true
    ): Int = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            val validIds = personIds.distinct()
            if (validIds.isEmpty()) return@withContext 0
            ensureCacheLoaded()

            val cropPathsToDelete = HashSet<String>()
            val existingIgnored = ArrayList(ignoredExemplarsCache)
            val newIgnoredEntities = ArrayList<IgnoredFaceExemplarEntity>()
            val now = System.currentTimeMillis()

            for (personId in validIds) {
                val person = faceDao.getPersonById(personId)
                person?.coverCropPath?.takeIf { it.isNotBlank() }?.let { cropPathsToDelete.add(it) }

                val faces = faceDao.getFacesByPersonId(personId)
                faces.forEach { face ->
                    face.cropCachePath?.takeIf { it.isNotBlank() }?.let { cropPathsToDelete.add(it) }
                }

                if (suppressSimilar) {
                    val candidates = ArrayList<FloatArray>()
                    person?.centroid?.takeIf { !isZeroVector(it) }?.let { candidates.add(it) }
                    faces.sortedByDescending { (if (it.isExemplar) 1f else 0f) + it.qualityScore }
                        .forEach { face ->
                            if (!isZeroVector(face.embedding)) {
                                candidates.add(face.embedding)
                            }
                        }
                    val chosen = selectDiverseExemplarsToIgnore(
                        candidates = candidates,
                        existingIgnored = existingIgnored,
                        maxToAdd = MAX_IGNORED_EXEMPLARS_PER_CLUSTER
                    )
                    for (emb in chosen) {
                        existingIgnored.add(emb)
                        newIgnoredEntities.add(IgnoredFaceExemplarEntity(embedding = emb, createdAt = now))
                    }
                }
            }

            if (newIgnoredEntities.isNotEmpty()) {
                faceDao.insertIgnoredFaceExemplars(newIgnoredEntities)
                faceDao.pruneIgnoredFaceExemplars(MAX_IGNORED_EXEMPLARS_TOTAL)
            }

            faceDao.deleteMergeSuggestionsForPersonsChunked(validIds)
            faceDao.deleteFacesByPersonIdsChunked(validIds)
            faceDao.deletePersonsByIdsChunked(validIds)

            for (path in cropPathsToDelete) {
                runCatching {
                    val f = File(path)
                    if (f.exists()) f.delete()
                }
            }

            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Dismissed ${validIds.size} person cluster(s) as not faces (saved ${newIgnoredEntities.size} ignored exemplars)")
            validIds.size
        }
    }

    /**
     * Removes specific photos' face detections from [personId] as false detections ("Not a face"),
     * records diverse embeddings for future suppression if requested, and refreshes or removes the
     * person cluster if empty. Actual photos remain untouched.
     */
    suspend fun dismissMediaFacesAsNotFaces(
        personId: Long,
        mediaIds: List<Long>,
        suppressSimilar: Boolean = true
    ): Int = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            val validMediaIds = mediaIds.distinct()
            if (validMediaIds.isEmpty()) return@withContext 0
            ensureCacheLoaded()

            val targetFaces = ArrayList<FaceEntity>()
            validMediaIds.chunked(500).forEach { chunk ->
                targetFaces.addAll(faceDao.getFacesForMediaAndPerson(chunk, personId))
            }
            if (targetFaces.isEmpty()) return@withContext 0

            val cropPathsToDelete = targetFaces.mapNotNull { it.cropCachePath?.takeIf { p -> p.isNotBlank() } }.toSet()

            if (suppressSimilar) {
                val candidates = targetFaces
                    .sortedByDescending { it.qualityScore }
                    .map { it.embedding }
                    .filter { !isZeroVector(it) }
                val chosen = selectDiverseExemplarsToIgnore(
                    candidates = candidates,
                    existingIgnored = ignoredExemplarsCache,
                    maxToAdd = MAX_IGNORED_EXEMPLARS_PER_CLUSTER
                )
                if (chosen.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    faceDao.insertIgnoredFaceExemplars(
                        chosen.map { IgnoredFaceExemplarEntity(embedding = it, createdAt = now) }
                    )
                    faceDao.pruneIgnoredFaceExemplars(MAX_IGNORED_EXEMPLARS_TOTAL)
                }
            }

            faceDao.deleteFacesForMediaAndPersonChunked(validMediaIds, personId)

            for (path in cropPathsToDelete) {
                runCatching {
                    val f = File(path)
                    if (f.exists()) f.delete()
                }
            }

            val remainingFaces = faceDao.getFacesByPersonId(personId)
            val remainingCount = faceDao.countDistinctMediaForPerson(personId)
            if (remainingCount == 0 || remainingFaces.isEmpty()) {
                faceDao.deleteMergeSuggestionsForPerson(personId)
                faceDao.deletePerson(personId)
            } else {
                val person = faceDao.getPersonById(personId)
                if (person != null) {
                    val updatedCentroid = computeCentroidFromFaces(remainingFaces) ?: person.centroid
                    val updatedCover = if (person.coverCropPath != null && person.coverCropPath !in cropPathsToDelete) {
                        person.coverCropPath
                    } else {
                        remainingFaces.maxByOrNull { it.qualityScore }?.cropCachePath
                    }
                    faceDao.updatePerson(
                        person.copy(
                            centroid = updatedCentroid,
                            faceCount = remainingCount,
                            coverCropPath = updatedCover
                        )
                    )
                }
            }

            isCacheInitialized = false
            ensureCacheLoaded()
            Log.d(TAG, "Dismissed ${targetFaces.size} face detection(s) on ${validMediaIds.size} photo(s) from person $personId as not faces")
            validMediaIds.size
        }
    }

    suspend fun clearIgnoredFaces() = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            faceDao.clearIgnoredFaceExemplars()
            ignoredExemplarsCache.clear()
        }
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
                            threshold = threshold,
                            isHiddenA = c1.isHidden,
                            isHiddenB = c2.isHidden
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

            computeAndSaveMergeSuggestionsLocked(
                minThreshold = SUGGESTION_MIN_THRESHOLD,
                includeSinglePhotoPairs = false
            )

            mergeCount
        }
    }

    /**
     * Finds pairs of clusters with high-confidence ambiguous similarity (>= 0.48) and saves them
     * as PENDING merge suggestions.
     */
    suspend fun computeAndSaveMergeSuggestions(): Int = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            isCacheInitialized = false
            computeAndSaveMergeSuggestionsLocked(
                minThreshold = SUGGESTION_MIN_THRESHOLD,
                includeSinglePhotoPairs = false
            )
        }
    }

    /**
     * One-shot broader merge discovery pass triggered on demand via "Find more matches".
     * Surfaces harder matches (e.g., sunglasses, side profiles, or 1-photo clusters) down to
     * [BROAD_SUGGESTION_MIN_THRESHOLD] without cluttering normal day-to-day suggestions.
     */
    suspend fun findMoreMergeSuggestions(): Int = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            isCacheInitialized = false
            computeAndSaveMergeSuggestionsLocked(
                minThreshold = BROAD_SUGGESTION_MIN_THRESHOLD,
                includeSinglePhotoPairs = true
            )
        }
    }

    /**
     * Computes pairwise face similarity between [personId] and every other visible cluster using
     * both centroids and exemplars, used to rank look-alikes at the top of the Merge picker.
     */
    suspend fun computeSimilarityScoresForPerson(personId: Long): Map<Long, Float> = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensureCacheLoaded()
            val target = clusterCache.firstOrNull { it.personId == personId } ?: return@withContext emptyMap()
            val scores = HashMap<Long, Float>(clusterCache.size)
            for (other in clusterCache) {
                if (other.personId == personId || other.isHidden) continue
                scores[other.personId] = computeClusterPairSimilarity(target, other)
            }
            scores
        }
    }

    private fun computeClusterPairSimilarity(c1: ClusterCacheItem, c2: ClusterCacheItem): Float {
        var maxSim = FaceRecognitionEngine.cosineSimilarity(c1.centroid, c2.centroid)
        for (e1 in c1.exemplars) {
            for (e2 in c2.exemplars) {
                val s = FaceRecognitionEngine.cosineSimilarity(e1, e2)
                if (s > maxSim) maxSim = s
            }
        }
        return maxSim
    }

    private suspend fun computeAndSaveMergeSuggestionsLocked(
        minThreshold: Float,
        includeSinglePhotoPairs: Boolean
    ): Int {
        ensureCacheLoaded()

        val suggestions = mutableListOf<PersonMergeSuggestionEntity>()

        for (i in 0 until clusterCache.size) {
            val c1 = clusterCache[i]
            for (j in i + 1 until clusterCache.size) {
                val c2 = clusterCache[j]

                val maxSim = computeClusterPairSimilarity(c1, c2)

                if (shouldSuggestMerge(
                    faceCountA = c1.faceCount,
                    nameA = c1.name,
                    isHiddenA = c1.isHidden,
                    faceCountB = c2.faceCount,
                    nameB = c2.name,
                    isHiddenB = c2.isHidden,
                    similarity = maxSim,
                    minThreshold = minThreshold,
                    includeSinglePhotoPairs = includeSinglePhotoPairs
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
            Log.d(TAG, "Generated ${suggestions.size} new merge suggestions at >=${(minThreshold * 100).toInt()}%")
        }
        return suggestions.size
    }
}
