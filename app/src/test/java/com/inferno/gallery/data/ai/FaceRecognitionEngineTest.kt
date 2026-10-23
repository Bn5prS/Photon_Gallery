package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FaceRecognitionEngineTest {

    @Test
    fun cosineSimilarity_identicalVectors_returnsOne() {
        val v1 = floatArrayOf(0.5f, -0.2f, 0.8f, 0.1f)
        val similarity = FaceRecognitionEngine.cosineSimilarity(v1, v1)
        assertEquals(1.0f, similarity, 0.001f)
    }

    @Test
    fun cosineDistance_identicalVectors_returnsZero() {
        val v1 = floatArrayOf(0.5f, -0.2f, 0.8f, 0.1f)
        val distance = FaceRecognitionEngine.cosineDistance(v1, v1)
        assertEquals(0.0f, distance, 0.001f)
    }

    @Test
    fun cosineSimilarity_orthogonalVectors_returnsZero() {
        val v1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        val similarity = FaceRecognitionEngine.cosineSimilarity(v1, v2)
        assertEquals(0.0f, similarity, 0.001f)
    }

    @Test
    fun cosineSimilarity_oppositeVectors_returnsMinusOne() {
        val v1 = floatArrayOf(1.0f, 2.0f, -3.0f)
        val v2 = floatArrayOf(-1.0f, -2.0f, 3.0f)
        val similarity = FaceRecognitionEngine.cosineSimilarity(v1, v2)
        assertEquals(-1.0f, similarity, 0.001f)
    }

    @Test
    fun normalizeL2_producesUnitLengthVector() {
        val v = floatArrayOf(3.0f, 4.0f) // norm = 5
        val normV = FaceRecognitionEngine.normalizeL2(v)

        assertEquals(0.6f, normV[0], 0.001f)
        assertEquals(0.8f, normV[1], 0.001f)

        var sumSq = 0.0
        for (x in normV) sumSq += x * x
        val length = sqrt(sumSq).toFloat()
        assertEquals(1.0f, length, 0.001f)
    }
}
