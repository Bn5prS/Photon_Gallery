package com.inferno.gallery.ui.editor

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorMatrix
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.engine.ImageProcessingEngine
import com.inferno.gallery.ui.editor.model.AdjustTool
import com.inferno.gallery.ui.editor.model.TuneValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageProcessingEngineTest {

    private val epsilon = 0.001f

    private fun assertMatrixEquals(expected: FloatArray, actual: FloatArray, delta: Float = epsilon) {
        assertEquals("Matrix length mismatch", 20, actual.size)
        for (i in 0 until 20) {
            assertEquals("Matrix mismatch at index $i (row ${i / 5}, col ${i % 5})", expected[i], actual[i], delta)
        }
    }

    @Test
    fun testAdjustToolEnumCompletenessAndOrder() {
        val tools = AdjustTool.entries
        assertEquals("Must have exactly 13 adjustment tools", 13, tools.size)

        val expectedOrder = listOf(
            AdjustTool.BRIGHTNESS,
            AdjustTool.CONTRAST,
            AdjustTool.TONE,
            AdjustTool.WHITE_POINT,
            AdjustTool.HIGHLIGHTS,
            AdjustTool.SHADOWS,
            AdjustTool.BLACK_POINT,
            AdjustTool.SATURATION,
            AdjustTool.WARMTH,
            AdjustTool.TINT,
            AdjustTool.SKIN_TONE,
            AdjustTool.BLUE_TONE,
            AdjustTool.VIGNETTE
        )
        assertEquals(expectedOrder, tools)

        // Verify bounds and defaults
        for (tool in tools) {
            if (tool == AdjustTool.VIGNETTE) {
                assertEquals(0f, tool.minValue, epsilon)
                assertEquals(100f, tool.maxValue, epsilon)
                assertEquals(0f, tool.defaultValue, epsilon)
            } else {
                assertEquals(-100f, tool.minValue, epsilon)
                assertEquals(100f, tool.maxValue, epsilon)
                assertEquals(0f, tool.defaultValue, epsilon)
            }
            assertTrue("Label must not be empty", tool.label.isNotBlank())
            assertTrue("Description must not be empty", tool.description.isNotBlank())
            assertTrue("Icon resource must be valid", tool.iconRes != 0)
        }
    }

    @Test
    fun testTuneValuesAccessorsAndImmutability() {
        val defaultTune = TuneValues()
        assertTrue(defaultTune.isDefault())

        for (tool in AdjustTool.entries) {
            assertFalse(defaultTune.isToolModified(tool))
            assertEquals(tool.defaultValue, defaultTune.getValue(tool), epsilon)
        }

        // Test withValue
        val modified = defaultTune.withValue(AdjustTool.BRIGHTNESS, 45f)
        assertFalse(modified.isDefault())
        assertTrue(modified.isToolModified(AdjustTool.BRIGHTNESS))
        assertEquals(45f, modified.getValue(AdjustTool.BRIGHTNESS), epsilon)
        assertFalse(modified.isToolModified(AdjustTool.CONTRAST))

        // Test reset single tool
        val resetBrightness = modified.reset(AdjustTool.BRIGHTNESS)
        assertTrue(resetBrightness.isDefault())
        assertEquals(0f, resetBrightness.getValue(AdjustTool.BRIGHTNESS), epsilon)

        // Test resetAll
        val multiModified = defaultTune
            .withValue(AdjustTool.SATURATION, -50f)
            .withValue(AdjustTool.WARMTH, 30f)
        assertFalse(multiModified.isDefault())
        val allReset = multiModified.resetAll()
        assertTrue(allReset.isDefault())
    }

    @Test
    fun testDefaultTuneValuesProducesIdentityMatrix() {
        val defaultTune = TuneValues()
        val matrix = ImageProcessingEngine.buildColorMatrix(defaultTune)

        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        assertMatrixEquals(identity, matrix.values)
    }

    @Test
    fun testBrightnessPositiveAndNegative() {
        val bright = TuneValues(brightness = 50f)
        val brightMatrix = ImageProcessingEngine.buildColorMatrix(bright)

        // Brightness offsets R, G, B translation columns (indices 4, 9, 14)
        assertTrue("Red offset should be positive", brightMatrix.values[4] > 0f)
        assertTrue("Green offset should be positive", brightMatrix.values[9] > 0f)
        assertTrue("Blue offset should be positive", brightMatrix.values[14] > 0f)
        assertEquals("Alpha offset should remain 0", 0f, brightMatrix.values[19], epsilon)

        val dark = TuneValues(brightness = -50f)
        val darkMatrix = ImageProcessingEngine.buildColorMatrix(dark)
        assertTrue("Red offset should be negative", darkMatrix.values[4] < 0f)
        assertTrue("Green offset should be negative", darkMatrix.values[9] < 0f)
        assertTrue("Blue offset should be negative", darkMatrix.values[14] < 0f)
    }

    @Test
    fun testContrastScalingAndMidGrayOffset() {
        val highContrast = TuneValues(contrast = 50f)
        val highMatrix = ImageProcessingEngine.buildColorMatrix(highContrast)

        // Scale should be > 1.0 on diagonal
        assertTrue("Red scale should exceed 1.0", highMatrix.values[0] > 1.0f)
        assertTrue("Green scale should exceed 1.0", highMatrix.values[6] > 1.0f)
        assertTrue("Blue scale should exceed 1.0", highMatrix.values[12] > 1.0f)
        // Mid-gray offset should be negative to anchor 127.5
        assertTrue("Offset should be negative", highMatrix.values[4] < 0f)

        val lowContrast = TuneValues(contrast = -50f)
        val lowMatrix = ImageProcessingEngine.buildColorMatrix(lowContrast)
        assertTrue("Scale should be less than 1.0", lowMatrix.values[0] < 1.0f)
        assertTrue("Offset should be positive", lowMatrix.values[4] > 0f)
    }

    @Test
    fun testSaturationRec709Grayscale() {
        val desaturated = TuneValues(saturation = -100f)
        val desatMatrix = ImageProcessingEngine.buildColorMatrix(desaturated)

        // At saturation -100, R, G, B channels should sum with Rec. 709 weights
        val expectedLumR = 0.2126f
        val expectedLumG = 0.7152f
        val expectedLumB = 0.0722f

        assertEquals(expectedLumR, desatMatrix.values[0], 0.01f)
        assertEquals(expectedLumG, desatMatrix.values[1], 0.01f)
        assertEquals(expectedLumB, desatMatrix.values[2], 0.01f)

        assertEquals(expectedLumR, desatMatrix.values[5], 0.01f)
        assertEquals(expectedLumG, desatMatrix.values[6], 0.01f)
        assertEquals(expectedLumB, desatMatrix.values[7], 0.01f)

        assertEquals(expectedLumR, desatMatrix.values[10], 0.01f)
        assertEquals(expectedLumG, desatMatrix.values[11], 0.01f)
        assertEquals(expectedLumB, desatMatrix.values[12], 0.01f)
    }

    @Test
    fun testWarmthShift() {
        val warm = TuneValues(warmth = 50f)
        val warmMatrix = ImageProcessingEngine.buildColorMatrix(warm)

        // Warmth boosts Red and reduces Blue
        assertTrue("Red channel offset should increase", warmMatrix.values[4] > 0f)
        assertTrue("Blue channel offset should decrease", warmMatrix.values[14] < 0f)
        assertEquals("Green channel offset should be unchanged by warmth alone", 0f, warmMatrix.values[9], epsilon)

        val cool = TuneValues(warmth = -50f)
        val coolMatrix = ImageProcessingEngine.buildColorMatrix(cool)
        assertTrue("Red channel offset should decrease", coolMatrix.values[4] < 0f)
        assertTrue("Blue channel offset should increase", coolMatrix.values[14] > 0f)
    }

    @Test
    fun testTintShift() {
        val tintMagenta = TuneValues(tint = 50f)
        val magentaMatrix = ImageProcessingEngine.buildColorMatrix(tintMagenta)

        // Magenta boosts Red + Blue, decreases Green
        assertTrue("Red offset should increase", magentaMatrix.values[4] > 0f)
        assertTrue("Green offset should decrease", magentaMatrix.values[9] < 0f)
        assertTrue("Blue offset should increase", magentaMatrix.values[14] > 0f)

        val tintGreen = TuneValues(tint = -50f)
        val greenMatrix = ImageProcessingEngine.buildColorMatrix(tintGreen)
        assertTrue("Red offset should decrease", greenMatrix.values[4] < 0f)
        assertTrue("Green offset should increase", greenMatrix.values[9] > 0f)
        assertTrue("Blue offset should decrease", greenMatrix.values[14] < 0f)
    }

    @Test
    fun testWhitePointAndBlackPoint() {
        val wp = TuneValues(whitePoint = 40f)
        val wpMatrix = ImageProcessingEngine.buildColorMatrix(wp)
        assertTrue("White point scale should be > 1.0", wpMatrix.values[0] > 1.0f)

        val bp = TuneValues(blackPoint = 40f)
        val bpMatrix = ImageProcessingEngine.buildColorMatrix(bp)
        assertTrue("Black point should shift pedestal offset", bpMatrix.values[4] > 0f)
    }

    @Test
    fun testHighlightsAndShadows() {
        val hl = TuneValues(highlights = 50f)
        val hlMatrix = ImageProcessingEngine.buildColorMatrix(hl)
        assertTrue("Highlights should scale upper luminance", hlMatrix.values[0] > 1.0f)

        val sh = TuneValues(shadows = 40f)
        val shMatrix = ImageProcessingEngine.buildColorMatrix(sh)
        assertTrue("Shadows should lift lower luminance offset", shMatrix.values[4] > 0f)
    }

    @Test
    fun testSkinToneAndBlueTone() {
        val skin = TuneValues(skinTone = 50f)
        val skinMatrix = ImageProcessingEngine.buildColorMatrix(skin)
        assertTrue("Skin tone should warm red/yellow", skinMatrix.values[4] > 0f)

        val blue = TuneValues(blueTone = 50f)
        val blueMatrix = ImageProcessingEngine.buildColorMatrix(blue)
        assertTrue("Blue tone should boost blue", blueMatrix.values[14] > 0f || blueMatrix.values[12] > 1.0f)
    }

    @Test
    fun testToneHDRDynamicRange() {
        val tone = TuneValues(tone = 50f)
        val toneMatrix = ImageProcessingEngine.buildColorMatrix(tone)
        assertTrue("Tone should adjust contrast curve scale", toneMatrix.values[0] > 1.0f)
    }

    @Test
    fun testFilterMatrixPostConcatenation() {
        val tune = TuneValues(brightness = 20f)
        // Invert filter matrix: diagonal -1, offset 255
        val invertFilter = ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        ))

        val combined = ImageProcessingEngine.buildCombinedMatrix(tune, invertFilter)
        // With inversion, diagonal should be negative
        assertTrue("Red diagonal should be negative after inversion", combined.values[0] < 0f)
        assertTrue("Green diagonal should be negative after inversion", combined.values[6] < 0f)
        assertTrue("Blue diagonal should be negative after inversion", combined.values[12] < 0f)
    }

    @Test
    fun testVignetteBrushGeneration() {
        val zeroVignette = ImageProcessingEngine.createVignetteBrush(Size(1000f, 1000f), 0f)
        assertNull("Zero vignette must return null brush", zeroVignette)

        val activeVignette = ImageProcessingEngine.createVignetteBrush(Size(1000f, 1000f), 50f)
        assertNotNull("Positive vignette must produce brush", activeVignette)
    }
}
