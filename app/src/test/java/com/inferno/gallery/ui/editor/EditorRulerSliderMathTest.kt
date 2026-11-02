package com.inferno.gallery.ui.editor

import androidx.compose.ui.geometry.Rect
import com.inferno.gallery.ui.editor.components.CornerHandle
import com.inferno.gallery.ui.editor.components.EditorCornerBracketsMath
import com.inferno.gallery.ui.editor.components.EditorRulerSliderMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorRulerSliderMathTest {

    @Test
    fun testValueNormalizationAndDenormalization() {
        val range = -100f..100f

        // Normalization
        assertEquals(0.0f, EditorRulerSliderMath.normalize(-100f, range), 0.001f)
        assertEquals(0.25f, EditorRulerSliderMath.normalize(-50f, range), 0.001f)
        assertEquals(0.5f, EditorRulerSliderMath.normalize(0f, range), 0.001f)
        assertEquals(0.75f, EditorRulerSliderMath.normalize(50f, range), 0.001f)
        assertEquals(1.0f, EditorRulerSliderMath.normalize(100f, range), 0.001f)

        // Beyond range clamping during normalization
        assertEquals(0.0f, EditorRulerSliderMath.normalize(-150f, range), 0.001f)
        assertEquals(1.0f, EditorRulerSliderMath.normalize(150f, range), 0.001f)

        // Single point range safety
        assertEquals(0.0f, EditorRulerSliderMath.normalize(5f, 5f..5f), 0.001f)

        // Denormalization
        assertEquals(-100f, EditorRulerSliderMath.denormalize(0.0f, range), 0.001f)
        assertEquals(0f, EditorRulerSliderMath.denormalize(0.5f, range), 0.001f)
        assertEquals(100f, EditorRulerSliderMath.denormalize(1.0f, range), 0.001f)
        assertEquals(50f, EditorRulerSliderMath.denormalize(0.75f, range), 0.001f)

        // Out of bounds fraction clamping
        assertEquals(-100f, EditorRulerSliderMath.denormalize(-0.2f, range), 0.001f)
        assertEquals(100f, EditorRulerSliderMath.denormalize(1.5f, range), 0.001f)
    }

    @Test
    fun testDeltaToValueMapping() {
        val pxPerUnit = 10f

        // Inverted = true (tape ruler physics: dragging left moves tape left, bringing higher values to center)
        assertEquals(5f, EditorRulerSliderMath.deltaToValue(dragDeltaPx = -50f, pxPerUnit = pxPerUnit, inverted = true), 0.001f)
        assertEquals(-5f, EditorRulerSliderMath.deltaToValue(dragDeltaPx = 50f, pxPerUnit = pxPerUnit, inverted = true), 0.001f)
        assertEquals(0f, EditorRulerSliderMath.deltaToValue(dragDeltaPx = 0f, pxPerUnit = pxPerUnit, inverted = true), 0.001f)

        // Inverted = false (direct mapping: dragging right increases value)
        assertEquals(5f, EditorRulerSliderMath.deltaToValue(dragDeltaPx = 50f, pxPerUnit = pxPerUnit, inverted = false), 0.001f)
        assertEquals(-5f, EditorRulerSliderMath.deltaToValue(dragDeltaPx = -50f, pxPerUnit = pxPerUnit, inverted = false), 0.001f)
    }

    @Test
    fun testClampingToRange() {
        val range = -45f..45f

        assertEquals(0f, EditorRulerSliderMath.clamp(0f, range), 0.001f)
        assertEquals(45f, EditorRulerSliderMath.clamp(45f, range), 0.001f)
        assertEquals(-45f, EditorRulerSliderMath.clamp(-45f, range), 0.001f)
        assertEquals(45f, EditorRulerSliderMath.clamp(60f, range), 0.001f)
        assertEquals(-45f, EditorRulerSliderMath.clamp(-100f, range), 0.001f)
    }

    @Test
    fun testSteppingAndQuantize() {
        // Step = 1.0f
        assertEquals(14f, EditorRulerSliderMath.quantize(14.3f, step = 1f), 0.001f)
        assertEquals(15f, EditorRulerSliderMath.quantize(14.7f, step = 1f), 0.001f)
        assertEquals(-3f, EditorRulerSliderMath.quantize(-3.2f, step = 1f), 0.001f)
        assertEquals(-4f, EditorRulerSliderMath.quantize(-3.8f, step = 1f), 0.001f)

        // Step = 5.0f
        assertEquals(10f, EditorRulerSliderMath.quantize(12f, step = 5f), 0.001f)
        assertEquals(15f, EditorRulerSliderMath.quantize(13f, step = 5f), 0.001f)
        assertEquals(-20f, EditorRulerSliderMath.quantize(-18f, step = 5f), 0.001f)

        // Step = 0.5f
        assertEquals(1.0f, EditorRulerSliderMath.quantize(1.2f, step = 0.5f), 0.001f)
        assertEquals(1.5f, EditorRulerSliderMath.quantize(1.3f, step = 0.5f), 0.001f)
        assertEquals(2.0f, EditorRulerSliderMath.quantize(1.8f, step = 0.5f), 0.001f)

        // Step <= 0 returns value unquantized
        assertEquals(7.33f, EditorRulerSliderMath.quantize(7.33f, step = 0f), 0.001f)
    }

    @Test
    fun testSnapToDefaultLogic() {
        val defaultValue = 0f
        val snapThreshold = 2.0f

        // Within snap threshold -> snaps to default
        assertEquals(0f, EditorRulerSliderMath.snapToDefault(1.5f, defaultValue, snapThreshold), 0.001f)
        assertEquals(0f, EditorRulerSliderMath.snapToDefault(-1.8f, defaultValue, snapThreshold), 0.001f)
        assertEquals(0f, EditorRulerSliderMath.snapToDefault(2.0f, defaultValue, snapThreshold), 0.001f)

        // Outside snap threshold -> remains original value
        assertEquals(2.5f, EditorRulerSliderMath.snapToDefault(2.5f, defaultValue, snapThreshold), 0.001f)
        assertEquals(-3.0f, EditorRulerSliderMath.snapToDefault(-3.0f, defaultValue, snapThreshold), 0.001f)

        // Non-zero default
        val nonZeroDefault = 50f
        assertEquals(50f, EditorRulerSliderMath.snapToDefault(51.5f, nonZeroDefault, snapThreshold), 0.001f)
        assertEquals(50f, EditorRulerSliderMath.snapToDefault(48.5f, nonZeroDefault, snapThreshold), 0.001f)
        assertEquals(55f, EditorRulerSliderMath.snapToDefault(55f, nonZeroDefault, snapThreshold), 0.001f)
    }

    @Test
    fun testApplyDeltaCombined() {
        val range = -100f..100f
        val pxPerUnit = 10f

        // Drag left by 200px -> delta is +20
        val v1 = EditorRulerSliderMath.applyDelta(
            currentValue = 0f,
            dragDeltaPx = -200f,
            pxPerUnit = pxPerUnit,
            range = range,
            step = 1f,
            snapThreshold = 1.5f,
            defaultValue = 0f
        )
        assertEquals(20f, v1, 0.001f)

        // Drag right from 0 by 1500px -> should clamp to -100
        val v2 = EditorRulerSliderMath.applyDelta(
            currentValue = 0f,
            dragDeltaPx = 1500f,
            pxPerUnit = pxPerUnit,
            range = range,
            step = 1f,
            snapThreshold = 1.5f,
            defaultValue = 0f
        )
        assertEquals(-100f, v2, 0.001f)

        // Small drag near 0 -> snaps to default 0
        val v3 = EditorRulerSliderMath.applyDelta(
            currentValue = 2f,
            dragDeltaPx = 10f, // delta = -1 -> 2 - 1 = 1f -> within 1.5 threshold of 0
            pxPerUnit = pxPerUnit,
            range = range,
            step = 1f,
            snapThreshold = 1.5f,
            defaultValue = 0f
        )
        assertEquals(0f, v3, 0.001f)
    }

    @Test
    fun testMajorTickIdentification() {
        // Step 1f, major interval 5f
        assertTrue(EditorRulerSliderMath.isMajorTick(value = 0f, majorInterval = 5f))
        assertTrue(EditorRulerSliderMath.isMajorTick(value = 5f, majorInterval = 5f))
        assertTrue(EditorRulerSliderMath.isMajorTick(value = -10f, majorInterval = 5f))
        assertTrue(EditorRulerSliderMath.isMajorTick(value = 25f, majorInterval = 5f))

        assertFalse(EditorRulerSliderMath.isMajorTick(value = 1f, majorInterval = 5f))
        assertFalse(EditorRulerSliderMath.isMajorTick(value = 3f, majorInterval = 5f))
        assertFalse(EditorRulerSliderMath.isMajorTick(value = -7f, majorInterval = 5f))
    }

    @Test
    fun testCornerHitTest() {
        val cropRect = Rect(0.2f, 0.2f, 0.8f, 0.8f)
        val canvasW = 1000f
        val canvasH = 1000f
        val touchRadius = 40f

        // Top-left: (200, 200)
        assertEquals(
            CornerHandle.TOP_LEFT,
            EditorCornerBracketsMath.hitTest(205f, 205f, cropRect, canvasW, canvasH, touchRadius)
        )

        // Top-right: (800, 200)
        assertEquals(
            CornerHandle.TOP_RIGHT,
            EditorCornerBracketsMath.hitTest(795f, 205f, cropRect, canvasW, canvasH, touchRadius)
        )

        // Bottom-left: (200, 800)
        assertEquals(
            CornerHandle.BOTTOM_LEFT,
            EditorCornerBracketsMath.hitTest(205f, 795f, cropRect, canvasW, canvasH, touchRadius)
        )

        // Bottom-right: (800, 800)
        assertEquals(
            CornerHandle.BOTTOM_RIGHT,
            EditorCornerBracketsMath.hitTest(795f, 795f, cropRect, canvasW, canvasH, touchRadius)
        )

        // Inside center: (500, 500)
        assertEquals(
            CornerHandle.INSIDE,
            EditorCornerBracketsMath.hitTest(500f, 500f, cropRect, canvasW, canvasH, touchRadius)
        )

        // Far outside: (50, 50)
        assertEquals(
            CornerHandle.NONE,
            EditorCornerBracketsMath.hitTest(50f, 50f, cropRect, canvasW, canvasH, touchRadius)
        )
    }

    @Test
    fun testCornerFreeformClamping() {
        val initial = Rect(0.2f, 0.2f, 0.8f, 0.8f)
        val canvasW = 1000f
        val canvasH = 1000f

        // Drag bottom-right outwards
        val expanded = EditorCornerBracketsMath.updateCropRect(
            currentRect = initial,
            handle = CornerHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.1f,
            deltaYNormalized = 0.1f,
            aspectRatio = null,
            canvasWidth = canvasW,
            canvasHeight = canvasH
        )
        assertEquals(0.2f, expanded.left, 0.001f)
        assertEquals(0.2f, expanded.top, 0.001f)
        assertEquals(0.9f, expanded.right, 0.001f)
        assertEquals(0.9f, expanded.bottom, 0.001f)

        // Drag past boundary (1.0f)
        val clampedBoundary = EditorCornerBracketsMath.updateCropRect(
            currentRect = expanded,
            handle = CornerHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.5f,
            deltaYNormalized = 0.5f,
            aspectRatio = null,
            canvasWidth = canvasW,
            canvasHeight = canvasH
        )
        assertEquals(1.0f, clampedBoundary.right, 0.001f)
        assertEquals(1.0f, clampedBoundary.bottom, 0.001f)

        // Drag top-left past minimum size
        val clampedMin = EditorCornerBracketsMath.updateCropRect(
            currentRect = initial,
            handle = CornerHandle.TOP_LEFT,
            deltaXNormalized = 0.75f,
            deltaYNormalized = 0.75f,
            aspectRatio = null,
            canvasWidth = canvasW,
            canvasHeight = canvasH,
            minDimensionNormalized = 0.1f
        )
        assertEquals(0.7f, clampedMin.left, 0.001f)
        assertEquals(0.7f, clampedMin.top, 0.001f)
    }

    @Test
    fun testCornerAspectRatioPreservation() {
        val initial = Rect(0.2f, 0.2f, 0.6f, 0.6f) // 1:1 on 1000x1000
        val canvasW = 1000f
        val canvasH = 1000f
        val aspect1to1 = 1.0f

        // Drag bottom-right corner with 1:1 aspect ratio
        val updated = EditorCornerBracketsMath.updateCropRect(
            currentRect = initial,
            handle = CornerHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.2f,
            deltaYNormalized = 0.1f, // Dominant delta is X (0.2)
            aspectRatio = aspect1to1,
            canvasWidth = canvasW,
            canvasHeight = canvasH
        )
        assertEquals(0.2f, updated.left, 0.001f)
        assertEquals(0.2f, updated.top, 0.001f)
        assertEquals(0.8f, updated.right, 0.001f)
        assertEquals(0.8f, updated.bottom, 0.001f) // Height adapted to match width
        assertEquals(updated.width, updated.height, 0.001f)
    }

    @Test
    fun testCornerInsideTranslation() {
        val initial = Rect(0.2f, 0.2f, 0.5f, 0.5f)
        val canvasW = 1000f
        val canvasH = 1000f

        val moved = EditorCornerBracketsMath.updateCropRect(
            currentRect = initial,
            handle = CornerHandle.INSIDE,
            deltaXNormalized = 0.1f,
            deltaYNormalized = 0.05f,
            aspectRatio = null,
            canvasWidth = canvasW,
            canvasHeight = canvasH
        )
        assertEquals(0.3f, moved.left, 0.001f)
        assertEquals(0.25f, moved.top, 0.001f)
        assertEquals(0.6f, moved.right, 0.001f)
        assertEquals(0.55f, moved.bottom, 0.001f)
        assertEquals(initial.width, moved.width, 0.001f)
        assertEquals(initial.height, moved.height, 0.001f)
    }

    @Test
    fun testSnapThresholdAllowsPlusMinusOne() {
        val step = 1f
        val snapThreshold = step * 0.4f
        val defaultValue = 0f

        // Near 0 (<= 0.4) -> snaps to 0
        assertEquals(0f, EditorRulerSliderMath.snapToDefault(0.35f, defaultValue, snapThreshold), 0.001f)
        assertEquals(0f, EditorRulerSliderMath.snapToDefault(-0.35f, defaultValue, snapThreshold), 0.001f)

        // Exact +1 and -1 (> 0.4) are preserved and NOT snapped to 0
        assertEquals(1f, EditorRulerSliderMath.snapToDefault(1f, defaultValue, snapThreshold), 0.001f)
        assertEquals(-1f, EditorRulerSliderMath.snapToDefault(-1f, defaultValue, snapThreshold), 0.001f)
        assertEquals(2f, EditorRulerSliderMath.snapToDefault(2f, defaultValue, snapThreshold), 0.001f)
        assertEquals(-2f, EditorRulerSliderMath.snapToDefault(-2f, defaultValue, snapThreshold), 0.001f)
    }

    @Test
    fun testCornerFreeformBoundaryDegenerateRange() {
        // When r is smaller than minDimensionNormalized, maxL shouldn't throw IllegalArgumentException
        val degenerateRect = Rect(0.01f, 0.01f, 0.02f, 0.02f)
        val result = EditorCornerBracketsMath.updateCropRect(
            currentRect = degenerateRect,
            handle = CornerHandle.TOP_LEFT,
            deltaXNormalized = -0.05f,
            deltaYNormalized = -0.05f,
            aspectRatio = null,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            minDimensionNormalized = 0.05f
        )
        assertTrue(result.left >= 0f)
        assertTrue(result.top >= 0f)
    }
}
