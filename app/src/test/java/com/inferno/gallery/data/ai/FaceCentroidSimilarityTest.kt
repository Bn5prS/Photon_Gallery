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
}
