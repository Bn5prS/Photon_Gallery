package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SmartSearchEngineTest {

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumOfSquares = 0.0f
        for (v in vector) {
            sumOfSquares += v * v
        }
        val norm = sqrt(sumOfSquares)
        if (norm == 0.0f) return vector
        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    private fun calculateLetterboxDimensions(sourceWidth: Int, sourceHeight: Int, targetSize: Int = 224): Pair<Int, Int> {
        val scale = targetSize.toFloat() / maxOf(sourceWidth, sourceHeight).toFloat()
        val scaledWidth = maxOf(1, (sourceWidth * scale).roundToInt())
        val scaledHeight = maxOf(1, (sourceHeight * scale).roundToInt())
        return Pair(scaledWidth, scaledHeight)
    }

    @Test
    fun testVectorNormalizationUnitLength() {
        val vec = FloatArray(512) { 1.0f }
        val normalized = l2Normalize(vec)

        var sumSq = 0f
        for (v in normalized) sumSq += v * v
        val length = sqrt(sumSq)

        assertEquals(1.0f, length, 1e-4f)
    }

    @Test
    fun testLetterboxDimensionsPortraitPhonePhoto() {
        // Typical portrait 9:16 phone photo: 1080 x 2400
        val (w, h) = calculateLetterboxDimensions(1080, 2400, 224)
        assertEquals(224, h)
        // 1080 * (224 / 2400) = 100.8 -> 101
        assertEquals(101, w)
        assertTrue(w <= 224)
        assertTrue(h <= 224)
    }

    @Test
    fun testLetterboxDimensionsLandscapePhoto() {
        // Typical landscape 16:9 photo: 2400 x 1080
        val (w, h) = calculateLetterboxDimensions(2400, 1080, 224)
        assertEquals(224, w)
        assertEquals(101, h)
        assertTrue(w <= 224)
        assertTrue(h <= 224)
    }

    @Test
    fun testLetterboxDimensionsSquarePhoto() {
        val (w, h) = calculateLetterboxDimensions(500, 500, 224)
        assertEquals(224, w)
        assertEquals(224, h)
    }

    @Test
    fun testPromptEnsemblingMath() {
        val e1 = l2Normalize(FloatArray(512) { 0.5f })
        val e2 = l2Normalize(FloatArray(512) { 0.8f })

        val sum = FloatArray(512) { i -> e1[i] + e2[i] }
        val ensembled = l2Normalize(sum)

        var sumSq = 0f
        for (v in ensembled) sumSq += v * v
        val length = sqrt(sumSq)

        assertEquals(1.0f, length, 1e-4f)
    }

    private fun calculateCenterCropDimensions(sourceWidth: Int, sourceHeight: Int, targetSize: Int = 224): Triple<Int, Int, Pair<Int, Int>> {
        val scale = targetSize.toFloat() / minOf(sourceWidth, sourceHeight).toFloat()
        val scaledWidth = maxOf(targetSize, (sourceWidth * scale).roundToInt())
        val scaledHeight = maxOf(targetSize, (sourceHeight * scale).roundToInt())
        val xOffset = (scaledWidth - targetSize) / 2
        val yOffset = (scaledHeight - targetSize) / 2
        return Triple(scaledWidth, scaledHeight, Pair(xOffset, yOffset))
    }

    @Test
    fun testCenterCropDimensionsPortraitPhoto() {
        // Typical portrait 9:16 phone photo: 1080 x 2400
        val (scaledW, scaledH, offsets) = calculateCenterCropDimensions(1080, 2400, 224)
        assertEquals(224, scaledW)
        // 2400 * (224 / 1080) = 497.77 -> 498
        assertEquals(498, scaledH)
        val (xOffset, yOffset) = offsets
        assertEquals(0, xOffset)
        assertEquals((498 - 224) / 2, yOffset)
    }

    @Test
    fun testCenterCropDimensionsLandscapePhoto() {
        // Typical landscape 16:9 photo: 2400 x 1080
        val (scaledW, scaledH, offsets) = calculateCenterCropDimensions(2400, 1080, 224)
        assertEquals(498, scaledW)
        assertEquals(224, scaledH)
        val (xOffset, yOffset) = offsets
        assertEquals((498 - 224) / 2, xOffset)
        assertEquals(0, yOffset)
    }

    @Test
    fun testCenterCropDimensionsSquarePhoto() {
        val (scaledW, scaledH, offsets) = calculateCenterCropDimensions(500, 500, 224)
        assertEquals(224, scaledW)
        assertEquals(224, scaledH)
        assertEquals(Pair(0, 0), offsets)
    }

    @Test
    fun testMultiTemplatePromptEnsemblingMath() {
        val e1 = l2Normalize(FloatArray(512) { 0.2f })
        val e2 = l2Normalize(FloatArray(512) { 0.4f })
        val e3 = l2Normalize(FloatArray(512) { 0.6f })
        val e4 = l2Normalize(FloatArray(512) { 0.8f })

        val sum = FloatArray(512) { i -> e1[i] + e2[i] + e3[i] + e4[i] }
        val ensembled = l2Normalize(sum)

        var sumSq = 0f
        for (v in ensembled) sumSq += v * v
        val length = sqrt(sumSq)

        assertEquals(1.0f, length, 1e-4f)
    }

    @Test
    fun testNormalizeQueryExpansions() {
        assertEquals("clothes iron steam iron", SmartSearchEngine.normalizeQuery("iron box"))
        assertEquals("blue refrigerator fridge", SmartSearchEngine.normalizeQuery("blue fridge"))
        assertEquals("person wearing eyeglasses sunglasses spectacles", SmartSearchEngine.normalizeQuery("person wearing specs"))
        assertEquals("dog puppy playing", SmartSearchEngine.normalizeQuery("doggy playing"))
        assertEquals("red car", SmartSearchEngine.normalizeQuery("red car"))
    }
}
