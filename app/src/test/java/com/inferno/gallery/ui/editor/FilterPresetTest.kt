package com.inferno.gallery.ui.editor

import androidx.compose.ui.graphics.ColorMatrix
import com.inferno.gallery.ui.editor.model.FilterPreset
import com.inferno.gallery.ui.editor.model.interpolate
import com.inferno.gallery.ui.editor.studios.AutoSuggestionType
import com.inferno.gallery.ui.editor.studios.AutoSuggestionsEngine
import com.inferno.gallery.ui.editor.studios.ImageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterPresetTest {

    private val epsilon = 0.001f

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun assertMatrixEquals(expected: FloatArray, actual: FloatArray, delta: Float = epsilon) {
        assertEquals("Matrix length mismatch", 20, actual.size)
        for (i in 0 until 20) {
            assertEquals(
                "Matrix mismatch at index $i (row ${i / 5}, col ${i % 5})",
                expected[i].toDouble(),
                actual[i].toDouble(),
                delta.toDouble()
            )
        }
    }

    @Test
    fun testAllFourteenPresetsDefinedInOrder() {
        val presets = FilterPreset.entries
        assertEquals("Must contain exactly 14 filters", 14, presets.size)

        val expectedNames = listOf(
            "Original",
            "Lite",
            "Playa",
            "Honey",
            "Isla",
            "Desert",
            "Clay",
            "Palma",
            "Metro",
            "Eiffel",
            "Modena",
            "Breeze",
            "Vogue",
            "Onyx"
        )
        val actualNames = presets.map { it.label }
        assertEquals(expectedNames, actualNames)
    }

    @Test
    fun testEveryPresetHasValid4x5Matrix() {
        for (preset in FilterPreset.entries) {
            assertEquals("Matrix for ${preset.label} must have 20 elements", 20, preset.matrixArray.size)
            // Alpha row: indices 15, 16, 17, 18, 19
            assertEquals("Alpha from R must be 0 for ${preset.label}", 0.0, preset.matrixArray[15].toDouble(), epsilon.toDouble())
            assertEquals("Alpha from G must be 0 for ${preset.label}", 0.0, preset.matrixArray[16].toDouble(), epsilon.toDouble())
            assertEquals("Alpha from B must be 0 for ${preset.label}", 0.0, preset.matrixArray[17].toDouble(), epsilon.toDouble())
            assertEquals("Alpha scale must be 1 for ${preset.label}", 1.0, preset.matrixArray[18].toDouble(), epsilon.toDouble())
            assertEquals("Alpha offset must be 0 for ${preset.label}", 0.0, preset.matrixArray[19].toDouble(), epsilon.toDouble())
        }
    }

    @Test
    fun testOriginalIsStrictIdentityMatrix() {
        val original = FilterPreset.ORIGINAL
        assertMatrixEquals(identity, original.matrixArray)
        assertMatrixEquals(identity, original.colorMatrix.values)
    }

    @Test
    fun testInterpolateAtZeroStrengthReturnsIdentity() {
        for (preset in FilterPreset.entries) {
            val resultMethod = preset.interpolate(0f)
            assertMatrixEquals(identity, resultMethod.values)

            val resultTopLevel = interpolate(preset.colorMatrix, 0f)
            assertMatrixEquals(identity, resultTopLevel.values)

            val resultCompanion = FilterPreset.interpolate(preset.colorMatrix, 0f)
            assertMatrixEquals(identity, resultCompanion.values)
        }
    }

    @Test
    fun testInterpolateAt100StrengthReturnsTargetMatrix() {
        for (preset in FilterPreset.entries) {
            val result = preset.interpolate(100f)
            assertMatrixEquals(preset.matrixArray, result.values)
        }
    }

    @Test
    fun testInterpolateAt50StrengthReturnsExactMidpoint() {
        for (preset in FilterPreset.entries) {
            val result = preset.interpolate(50f)
            for (i in 0 until 20) {
                val expected = (identity[i] * 0.5f) + (preset.matrixArray[i] * 0.5f)
                assertEquals(
                    "Midpoint mismatch for ${preset.label} at index $i",
                    expected.toDouble(),
                    result.values[i].toDouble(),
                    epsilon.toDouble()
                )
            }
        }
    }

    @Test
    fun testInterpolateClampingBelowZeroAndAbove100() {
        val playa = FilterPreset.PLAYA
        val belowZero = playa.interpolate(-25f)
        assertMatrixEquals(identity, belowZero.values)

        val above100 = playa.interpolate(150f)
        assertMatrixEquals(playa.matrixArray, above100.values)
    }

    @Test
    fun testMonochromePresetsVogueAndOnyx() {
        // Vogue: Monochrome B&W high fashion contrast
        val vogue = FilterPreset.VOGUE.matrixArray
        for (c in 0..4) {
            assertEquals("Vogue R and G row must match at col $c", vogue[0 * 5 + c].toDouble(), vogue[1 * 5 + c].toDouble(), epsilon.toDouble())
            assertEquals("Vogue G and B row must match at col $c", vogue[1 * 5 + c].toDouble(), vogue[2 * 5 + c].toDouble(), epsilon.toDouble())
        }

        // Onyx: Deep cinematic B&W
        val onyx = FilterPreset.ONYX.matrixArray
        for (c in 0..4) {
            assertEquals("Onyx R and G row must match at col $c", onyx[0 * 5 + c].toDouble(), onyx[1 * 5 + c].toDouble(), epsilon.toDouble())
            assertEquals("Onyx G and B row must match at col $c", onyx[1 * 5 + c].toDouble(), onyx[2 * 5 + c].toDouble(), epsilon.toDouble())
        }
    }

    @Test
    fun testAutoSuggestionsEnginePresetPayloads() {
        val suggestions = AutoSuggestionsEngine.generateSuggestions(null)
        assertEquals("Must generate exactly 4 suggestions", 4, suggestions.size)

        val types = suggestions.map { it.type }
        assertEquals(
            listOf(
                AutoSuggestionType.ENHANCE,
                AutoSuggestionType.DYNAMIC,
                AutoSuggestionType.WARM,
                AutoSuggestionType.COOL
            ),
            types
        )

        // Dynamic check: Shadow lift (+35), highlight recovery (-20), saturation (+25), tone (+30)
        val dynamic = AutoSuggestionsEngine.computeDynamicTune()
        assertEquals(35f, dynamic.shadows, epsilon)
        assertEquals(-20f, dynamic.highlights, epsilon)
        assertEquals(25f, dynamic.saturation, epsilon)
        assertEquals(30f, dynamic.tone, epsilon)
        assertEquals(10f, dynamic.contrast, epsilon)

        // Warm check: Warmth (+40), saturation (+15)
        val warm = AutoSuggestionsEngine.computeWarmTune()
        assertEquals(40f, warm.warmth, epsilon)
        assertEquals(15f, warm.saturation, epsilon)

        // Cool check: Cool tint (-30), contrast (+10), blueTone (+15)
        val cool = AutoSuggestionsEngine.computeCoolTune()
        assertEquals(-30f, cool.warmth, epsilon)
        assertEquals(10f, cool.contrast, epsilon)
        assertEquals(15f, cool.blueTone, epsilon)
    }

    @Test
    fun testAdaptiveEnhanceExposureAndTemperatureCorrections() {
        // Underexposed photo
        val darkStats = ImageStats(
            meanLuminance = 60f,
            p5Luminance = 5f,
            p95Luminance = 150f,
            meanRed = 60f,
            meanGreen = 60f,
            meanBlue = 60f
        )
        val darkEnhance = AutoSuggestionsEngine.computeEnhanceTune(darkStats)
        assertTrue("Dark photo should have positive brightness boost", darkEnhance.brightness > 15f)
        assertTrue("Dark photo with low dynamic range should boost contrast", darkEnhance.contrast > 15f)
        assertTrue("Dark photo with low blacks should lift shadows", darkEnhance.shadows >= 20f)

        // Overexposed photo
        val brightStats = ImageStats(
            meanLuminance = 190f,
            p5Luminance = 100f,
            p95Luminance = 250f,
            meanRed = 190f,
            meanGreen = 190f,
            meanBlue = 190f
        )
        val brightEnhance = AutoSuggestionsEngine.computeEnhanceTune(brightStats)
        assertTrue("Overexposed photo should reduce brightness", brightEnhance.brightness < 0f)
        assertTrue("Overexposed photo should recover highlights", brightEnhance.highlights <= -15f)

        // Warm color cast (too much red)
        val warmStats = ImageStats(
            meanLuminance = 128f,
            meanRed = 180f,
            meanGreen = 128f,
            meanBlue = 100f
        )
        val warmEnhance = AutoSuggestionsEngine.computeEnhanceTune(warmStats)
        assertTrue("Warm cast should apply negative warmth correction (cooling)", warmEnhance.warmth < 0f)

        // Cool color cast (too much blue)
        val coolStats = ImageStats(
            meanLuminance = 128f,
            meanRed = 90f,
            meanGreen = 128f,
            meanBlue = 180f
        )
        val coolEnhance = AutoSuggestionsEngine.computeEnhanceTune(coolStats)
        assertTrue("Cool cast should apply positive warmth correction (warming)", coolEnhance.warmth > 0f)
    }
}
