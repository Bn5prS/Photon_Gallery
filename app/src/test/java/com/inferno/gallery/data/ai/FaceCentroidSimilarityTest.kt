package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceCentroidSimilarityTest {

    @Test
    fun testCosineSimilarityIdenticalVectors() {
        val vecA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecB = floatArrayOf(1.0f, 0.0f, 0.0f)
        val sim = FaceRecognitionEngine.cosineSimilarity(vecA, vecB)
        assertEquals(1.0f, sim, 0.0001f)
    }

    @Test
    fun testCosineSimilarityOrthogonalVectors() {
        val vecA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecB = floatArrayOf(0.0f, 1.0f, 0.0f)
        val sim = FaceRecognitionEngine.cosineSimilarity(vecA, vecB)
        assertEquals(0.0f, sim, 0.0001f)
    }

    @Test
    fun testCandidateThresholdClassification() {
        // Ambiguous window check
        val minThresh = FaceClusteringManager.SUGGESTION_MIN_THRESHOLD
        val maxThresh = FaceClusteringManager.SUGGESTION_MAX_THRESHOLD

        assertTrue(minThresh < maxThresh)
        assertTrue(FaceClusteringManager.isCandidateSimilarity((minThresh + maxThresh) / 2f))
        assertFalse(FaceClusteringManager.isCandidateSimilarity(minThresh - 0.05f))
        assertFalse(FaceClusteringManager.isCandidateSimilarity(maxThresh + 0.05f))
    }

    @Test
    fun testCanonicalPairOrdering() {
        val pair1 = FaceClusteringManager.toCanonicalPair(20L, 5L)
        assertEquals(5L, pair1.first)
        assertEquals(20L, pair1.second)

        val pair2 = FaceClusteringManager.toCanonicalPair(3L, 12L)
        assertEquals(3L, pair2.first)
        assertEquals(12L, pair2.second)
    }

    @Test
    fun testShouldSuggestMerge() {
        // Both single face unnamed: rejected (noise)
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = null, isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.50f
            )
        )

        // One cluster has 2+ faces: accepted
        assertTrue(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 3, nameA = null, isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.50f
            )
        )

        // One cluster is named: accepted
        assertTrue(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = "Alice", isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.50f
            )
        )

        // Either cluster is hidden: rejected
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 5, nameA = "Bob", isHiddenA = true,
                faceCountB = 3, nameB = null, isHiddenB = false,
                similarity = 0.50f
            )
        )

        // Similarity too low (< 0.48f): rejected
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 5, nameA = "Bob", isHiddenA = false,
                faceCountB = 3, nameB = null, isHiddenB = false,
                similarity = 0.42f
            )
        )
    }

    @Test
    fun testQualityWeightedCentroidPrefersCrispFrontalOverLowQualityProfile() {
        val frontal = FaceRecognitionEngine.normalizeL2(floatArrayOf(1.0f, 0.0f, 0.0f))
        val noisyProfile = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.4f, 0.9165f, 0.0f))

        val centroid = FaceClusteringManager.computeWeightedCentroid(
            embeddings = listOf(frontal, noisyProfile),
            weights = listOf(0.95f, 0.30f)
        )!!

        val simToFrontal = FaceRecognitionEngine.cosineSimilarity(centroid, frontal)
        val simToNoisy = FaceRecognitionEngine.cosineSimilarity(centroid, noisyProfile)
        assertTrue("Weighted centroid must stay closer to crisp frontal ($simToFrontal vs $simToNoisy)", simToFrontal > simToNoisy + 0.12f)
    }

    @Test
    fun testEvaluateClusterMatchAcceptsPoseBridgeAndBlocksOutlierChaining() {
        val frontalCentroid = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.90f, 0.10f, 0.0f, 0.0f))
        val frontalEx = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.94f, 0.05f, 0.0f, 0.0f))
        val threeQuarterEx = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.72f, 0.68f, 0.12f, 0.0f))
        // Genuine side profile: ~0.46 to frontal centroid, ~0.505 to 3/4 exemplar, ~0.48 to frontal
        val sideProfile = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.47f, 0.49f, 0.73f, 0.0f))

        assertTrue(
            "Side profile corroborated by both 3/4 exemplar and centroid should match",
            FaceClusteringManager.isClusterMatch(
                embedding = sideProfile,
                centroid = frontalCentroid,
                exemplars = listOf(frontalEx, threeQuarterEx)
            )
        )

        // Stranger who accidentally hits 0.53 against one outlier exemplar, but has 0.20 centroid similarity
        val corruptedOutlier = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.15f, 0.0f, 0.0f, 0.988f))
        val stranger = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.15f, 0.0f, 0.82f, 0.55f))
        assertFalse(
            "Outlier chaining with <0.38 centroid similarity must be rejected to preserve precision",
            FaceClusteringManager.isClusterMatch(
                embedding = stranger,
                centroid = frontalCentroid,
                exemplars = listOf(frontalEx, corruptedOutlier)
            )
        )
    }

    @Test
    fun testShouldConsolidateClustersProtectsNamedIdentitiesAndSupportsPoseBridges() {
        val cA = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.92f, 0.10f, 0.0f))
        val exA1 = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.95f, 0.05f, 0.0f))
        val exA2 = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.74f, 0.66f, 0.10f))

        val cB = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.46f, 0.52f, 0.72f))
        val exB1 = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.48f, 0.54f, 0.69f))

        // Two DIFFERENT user-assigned names must NEVER be auto-merged!
        assertFalse(
            FaceClusteringManager.shouldConsolidateClusters(
                centroidA = cA, exemplarsA = listOf(exA1, exA2), nameA = "Rahul",
                centroidB = cA, exemplarsB = listOf(exA1), nameB = "Rohan"
            )
        )

        // Unnamed side-profile cluster SHOULD consolidate into named frontal+3/4 cluster via pose bridge
        assertTrue(
            FaceClusteringManager.shouldConsolidateClusters(
                centroidA = cA, exemplarsA = listOf(exA1, exA2), nameA = "Rahul",
                centroidB = cB, exemplarsB = listOf(exB1), nameB = null
            )
        )
    }

    @Test
    fun testBroadSuggestionPassSurfacesSinglePhotoPairsWhileRespectingGuards() {
        val broadMin = FaceClusteringManager.BROAD_SUGGESTION_MIN_THRESHOLD

        // Two unnamed 1-photo clusters at 42% similarity are surfaced during "Find more matches"
        assertTrue(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = null, isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.42f,
                minThreshold = broadMin
            )
        )

        // High-similarity non-consolidated pairs (>= 54%) are also surfaced during "Find more matches"
        assertTrue(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 2, nameA = null, isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.58f,
                minThreshold = broadMin
            )
        )

        // Below broad 40% floor is still rejected
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = null, isHiddenA = false,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.39f,
                minThreshold = broadMin
            )
        )

        // Hidden or conflicting user-named clusters stay blocked even during broader discovery
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = "Alice", isHiddenA = false,
                faceCountB = 1, nameB = "Bob", isHiddenB = false,
                similarity = 0.45f,
                minThreshold = broadMin
            )
        )
        assertFalse(
            FaceClusteringManager.shouldSuggestMerge(
                faceCountA = 1, nameA = null, isHiddenA = true,
                faceCountB = 1, nameB = null, isHiddenB = false,
                similarity = 0.45f,
                minThreshold = broadMin
            )
        )
    }

    @Test
    fun testPartitionCandidatesBySimilarityRanksClosestMatchesFirst() {
        val candidates = listOf(1L, 2L, 3L, 4L, 5L)
        val scores = mapOf(
            1L to 0.22f,
            2L to 0.41f,
            3L to 0.51f,
            4L to 0.37f,
            5L to 0.15f
        )

        val (closest, others) = FaceClusteringManager.partitionCandidatesBySimilarity(
            candidates = candidates,
            idSelector = { it },
            similarityScores = scores
        )

        assertEquals(listOf(3L, 2L, 4L), closest)
        assertEquals(listOf(1L, 5L), others)
    }

    @Test
    fun testIgnoredExemplarMatchingSuppressesRecurringNonFacesWithoutSwallowingRealFaces() {
        // Two completely unrelated non-face detections (e.g., dark glare blob & wall poster)
        val junkA = FaceRecognitionEngine.normalizeL2(floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f))
        val junkB = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f))
        val ignored = listOf(junkA, junkB)

        // Recurring near-identical false detection matches junkA (>= 0.68f)
        val recurringJunkA = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.86f, 0.12f, 0.49f, 0.0f))
        assertTrue(
            "Recurring non-face artifact should be dropped",
            FaceClusteringManager.matchesIgnoredExemplar(recurringJunkA, ignored)
        )

        // Real face sitting midway between unrelated junkA and junkB (~0.50 sim to each)
        // Would have been swallowed if junkA + junkB were averaged into one centroid (~0.71 sim!),
        // but per-exemplar matching keeps real faces safe!
        val realFaceMidpoint = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.55f, 0.55f, 0.63f, 0.0f))
        assertFalse(
            "Real face between unrelated ignored objects must NOT be swallowed",
            FaceClusteringManager.matchesIgnoredExemplar(realFaceMidpoint, ignored)
        )
    }

    @Test
    fun testSelectDiverseExemplarsToIgnoreDeduplicatesAndCaps() {
        val existing = listOf(
            FaceRecognitionEngine.normalizeL2(floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f))
        )
        val nearDuplicateOfExisting = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.98f, 0.20f, 0.0f, 0.0f))
        val novelA = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f))
        val nearDuplicateOfNovelA = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.0f, 0.99f, 0.14f, 0.0f))
        val novelB = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f))
        val novelC = FaceRecognitionEngine.normalizeL2(floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))

        val picked = FaceClusteringManager.selectDiverseExemplarsToIgnore(
            candidates = listOf(nearDuplicateOfExisting, novelA, nearDuplicateOfNovelA, novelB, novelC),
            existingIgnored = existing,
            maxToAdd = 2
        )

        assertEquals(2, picked.size)
        assertTrue(FaceRecognitionEngine.cosineSimilarity(picked[0], novelA) > 0.99f)
        assertTrue(FaceRecognitionEngine.cosineSimilarity(picked[1], novelB) > 0.99f)
    }

    @Test
    fun testShouldConsolidateClustersBlocksHiddenVisibleMerges() {
        val emb = FaceRecognitionEngine.normalizeL2(floatArrayOf(1.0f, 0.0f, 0.0f))

        // Identical embeddings where one is hidden and one is visible must NEVER auto-consolidate
        assertFalse(
            FaceClusteringManager.shouldConsolidateClusters(
                centroidA = emb, exemplarsA = listOf(emb), nameA = null,
                centroidB = emb, exemplarsB = listOf(emb), nameB = null,
                isHiddenA = true, isHiddenB = false
            )
        )
        assertFalse(
            FaceClusteringManager.shouldConsolidateClusters(
                centroidA = emb, exemplarsA = listOf(emb), nameA = null,
                centroidB = emb, exemplarsB = listOf(emb), nameB = null,
                isHiddenA = false, isHiddenB = true
            )
        )
        // Both visible still consolidates normally
        assertTrue(
            FaceClusteringManager.shouldConsolidateClusters(
                centroidA = emb, exemplarsA = listOf(emb), nameA = null,
                centroidB = emb, exemplarsB = listOf(emb), nameB = null,
                isHiddenA = false, isHiddenB = false
            )
        )
    }
}
