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
}
