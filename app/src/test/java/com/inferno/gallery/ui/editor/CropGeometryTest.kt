package com.inferno.gallery.ui.editor

import androidx.compose.ui.geometry.Rect
import com.inferno.gallery.ui.editor.crop.CropGeometryHelper
import com.inferno.gallery.ui.editor.model.AspectRatioOption
import com.inferno.gallery.ui.editor.model.CropState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {

    @Test
    fun testAllElevenAspectRatioOptionsPresent() {
        val options = AspectRatioOption.entries
        assertEquals(11, options.size)

        assertEquals("Freeform", AspectRatioOption.FREEFORM.label)
        assertNull(AspectRatioOption.FREEFORM.ratio)

        assertEquals("Original", AspectRatioOption.ORIGINAL.label)
        assertEquals(-1f, AspectRatioOption.ORIGINAL.ratio)

        assertEquals("Square", AspectRatioOption.SQUARE.label)
        assertEquals(1f, AspectRatioOption.SQUARE.ratio)

        assertEquals("5:4", AspectRatioOption.RATIO_5_4.label)
        assertEquals(5f / 4f, AspectRatioOption.RATIO_5_4.ratio)

        assertEquals("4:3", AspectRatioOption.RATIO_4_3.label)
        assertEquals(4f / 3f, AspectRatioOption.RATIO_4_3.ratio)

        assertEquals("3:2", AspectRatioOption.RATIO_3_2.label)
        assertEquals(3f / 2f, AspectRatioOption.RATIO_3_2.ratio)

        assertEquals("16:9", AspectRatioOption.RATIO_16_9.label)
        assertEquals(16f / 9f, AspectRatioOption.RATIO_16_9.ratio)

        assertEquals("4:5", AspectRatioOption.RATIO_4_5.label)
        assertEquals(4f / 5f, AspectRatioOption.RATIO_4_5.ratio)

        assertEquals("3:4", AspectRatioOption.RATIO_3_4.label)
        assertEquals(3f / 4f, AspectRatioOption.RATIO_3_4.ratio)

        assertEquals("2:3", AspectRatioOption.RATIO_2_3.label)
        assertEquals(2f / 3f, AspectRatioOption.RATIO_2_3.ratio)

        assertEquals("9:16", AspectRatioOption.RATIO_9_16.label)
        assertEquals(9f / 16f, AspectRatioOption.RATIO_9_16.ratio)
    }

    @Test
    fun testAspectRatioTargetResolution() {
        val effectiveAspect = 16f / 9f

        // Freeform is unconstrained -> null
        assertNull(AspectRatioOption.FREEFORM.resolveTargetRatio(effectiveAspect))
        assertNull(CropGeometryHelper.targetAspectRatio(AspectRatioOption.FREEFORM, effectiveAspect))

        // Original matches effective aspect
        assertEquals(
            effectiveAspect,
            AspectRatioOption.ORIGINAL.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            effectiveAspect,
            CropGeometryHelper.targetAspectRatio(AspectRatioOption.ORIGINAL, effectiveAspect)!!,
            0.001f
        )

        // Square -> 1f
        assertEquals(
            1f,
            AspectRatioOption.SQUARE.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )

        // Landscape ratios
        assertEquals(
            1.25f,
            AspectRatioOption.RATIO_5_4.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            4f / 3f,
            AspectRatioOption.RATIO_4_3.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            1.5f,
            AspectRatioOption.RATIO_3_2.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            16f / 9f,
            AspectRatioOption.RATIO_16_9.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )

        // Portrait ratios
        assertEquals(
            0.8f,
            AspectRatioOption.RATIO_4_5.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            0.75f,
            AspectRatioOption.RATIO_3_4.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            2f / 3f,
            AspectRatioOption.RATIO_2_3.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
        assertEquals(
            9f / 16f,
            AspectRatioOption.RATIO_9_16.resolveTargetRatio(effectiveAspect)!!,
            0.001f
        )
    }

    @Test
    fun testCenteredRectCalculations() {
        // Freeform defaults to full bounds
        val freeRect = CropGeometryHelper.calculateInitialAspectRect(AspectRatioOption.FREEFORM, 1f)
        assertEquals(Rect(0f, 0f, 1f, 1f), freeRect)

        // Target matches image aspect exactly -> full bounds (0,0,1,1)
        val squareOnSquare = CropGeometryHelper.calculateInitialAspectRect(AspectRatioOption.SQUARE, 1f)
        assertEquals(Rect(0f, 0f, 1f, 1f), squareOnSquare)

        val wideOnWide = CropGeometryHelper.calculateInitialAspectRect(AspectRatioOption.RATIO_16_9, 16f / 9f)
        assertEquals(0f, wideOnWide.left, 0.001f)
        assertEquals(0f, wideOnWide.top, 0.001f)
        assertEquals(1f, wideOnWide.right, 0.001f)
        assertEquals(1f, wideOnWide.bottom, 0.001f)

        // Target is wider than image (e.g. 16:9 on 1:1 image)
        // Image aspect = 1.0, Target aspect = 16/9 = 1.7778
        // r = 1.7778 / 1.0 = 1.7778 > 1
        // h = 1 / 1.7778 = 9/16 = 0.5625
        // Centered top = (1 - 0.5625) / 2 = 0.21875
        val wideOnSquare = CropGeometryHelper.calculateInitialAspectRect(AspectRatioOption.RATIO_16_9, 1f)
        assertEquals(0f, wideOnSquare.left, 0.001f)
        assertEquals(1f, wideOnSquare.right, 0.001f)
        assertEquals(0.21875f, wideOnSquare.top, 0.001f)
        assertEquals(0.78125f, wideOnSquare.bottom, 0.001f)
        assertEquals(0.5625f, wideOnSquare.height, 0.001f)

        // Target is taller than image (e.g. 1:1 on 16:9 image)
        // Image aspect = 16/9 = 1.7778, Target aspect = 1.0
        // r = 1.0 / 1.7778 = 9/16 = 0.5625 <= 1
        // Centered left = (1 - 0.5625) / 2 = 0.21875
        val squareOnWide = CropGeometryHelper.calculateInitialAspectRect(AspectRatioOption.SQUARE, 16f / 9f)
        assertEquals(0f, squareOnWide.top, 0.001f)
        assertEquals(1f, squareOnWide.bottom, 0.001f)
        assertEquals(0.21875f, squareOnWide.left, 0.001f)
        assertEquals(0.78125f, squareOnWide.right, 0.001f)
        assertEquals(0.5625f, squareOnWide.width, 0.001f)

        // Verify boundary safety: all coordinates within 0f..1f
        assertTrue(wideOnSquare.left in 0f..1f)
        assertTrue(wideOnSquare.top in 0f..1f)
        assertTrue(wideOnSquare.right in 0f..1f)
        assertTrue(wideOnSquare.bottom in 0f..1f)
    }

    @Test
    fun testRotationMath() {
        // Normalization to 0, 90, 180, 270
        assertEquals(0, CropGeometryHelper.normalizeRotation(0))
        assertEquals(90, CropGeometryHelper.normalizeRotation(90))
        assertEquals(180, CropGeometryHelper.normalizeRotation(180))
        assertEquals(270, CropGeometryHelper.normalizeRotation(270))
        assertEquals(0, CropGeometryHelper.normalizeRotation(360))
        assertEquals(90, CropGeometryHelper.normalizeRotation(450))
        assertEquals(270, CropGeometryHelper.normalizeRotation(-90))
        assertEquals(180, CropGeometryHelper.normalizeRotation(-180))
        assertEquals(0, CropGeometryHelper.normalizeRotation(-360))

        // Counter-clockwise 90° steps (Counter-clockwise rotate button behavior)
        assertEquals(270, CropGeometryHelper.rotateCounterClockwise(0))
        assertEquals(180, CropGeometryHelper.rotateCounterClockwise(270))
        assertEquals(90, CropGeometryHelper.rotateCounterClockwise(180))
        assertEquals(0, CropGeometryHelper.rotateCounterClockwise(90))

        // Clockwise 90° steps
        assertEquals(90, CropGeometryHelper.rotateClockwise(0))
        assertEquals(180, CropGeometryHelper.rotateClockwise(90))
        assertEquals(270, CropGeometryHelper.rotateClockwise(180))
        assertEquals(0, CropGeometryHelper.rotateClockwise(270))

        // Effective aspect ratio under rotation
        val originalAspect = 4f / 3f
        assertEquals(originalAspect, CropGeometryHelper.effectiveAspectRatio(originalAspect, 0), 0.001f)
        assertEquals(3f / 4f, CropGeometryHelper.effectiveAspectRatio(originalAspect, 90), 0.001f)
        assertEquals(originalAspect, CropGeometryHelper.effectiveAspectRatio(originalAspect, 180), 0.001f)
        assertEquals(3f / 4f, CropGeometryHelper.effectiveAspectRatio(originalAspect, 270), 0.001f)
    }

    @Test
    fun testTiltAndStraightenClamping() {
        // Straighten angle is clamped to -45f..+45f
        assertEquals(0f, CropGeometryHelper.clampStraighten(0f), 0.001f)
        assertEquals(15f, CropGeometryHelper.clampStraighten(15f), 0.001f)
        assertEquals(-30f, CropGeometryHelper.clampStraighten(-30f), 0.001f)
        assertEquals(45f, CropGeometryHelper.clampStraighten(50f), 0.001f)
        assertEquals(-45f, CropGeometryHelper.clampStraighten(-60f), 0.001f)

        // Vertical tilt clamped to -30f..+30f
        assertEquals(0f, CropGeometryHelper.clampTilt(0f), 0.001f)
        assertEquals(20f, CropGeometryHelper.clampTilt(20f), 0.001f)
        assertEquals(-25f, CropGeometryHelper.clampTilt(-25f), 0.001f)
        assertEquals(30f, CropGeometryHelper.clampTilt(35f), 0.001f)
        assertEquals(-30f, CropGeometryHelper.clampTilt(-45f), 0.001f)
    }

    @Test
    fun testCropStateDefaultsAndModifications() {
        val defaultState = CropState()
        assertEquals(0, defaultState.rotation)
        assertEquals(0f, defaultState.straightenAngle, 0.001f)
        assertFalse(defaultState.flipH)
        assertFalse(defaultState.flipV)
        assertEquals(Rect(0f, 0f, 1f, 1f), defaultState.cropRect)
        assertEquals(AspectRatioOption.FREEFORM, defaultState.aspectOption)
        assertEquals(0f, defaultState.verticalTilt, 0.001f)
        assertEquals(0f, defaultState.horizontalTilt, 0.001f)
        assertFalse(defaultState.isModified)

        // Rotate CCW
        val rotatedCcw = defaultState.rotate90Ccw()
        assertEquals(270, rotatedCcw.rotation)
        assertTrue(rotatedCcw.isModified)

        // Rotate CW
        val rotatedCw = defaultState.rotate90Cw()
        assertEquals(90, rotatedCw.rotation)
        assertTrue(rotatedCw.isModified)

        // Straighten clamping
        val straightened = defaultState.withStraighten(55f)
        assertEquals(45f, straightened.straightenAngle, 0.001f)
        assertTrue(straightened.isModified)

        // Vertical and Horizontal tilt clamping
        val tiltedV = defaultState.withVerticalTilt(-40f)
        assertEquals(-30f, tiltedV.verticalTilt, 0.001f)
        assertTrue(tiltedV.isModified)

        val tiltedH = defaultState.withHorizontalTilt(40f)
        assertEquals(30f, tiltedH.horizontalTilt, 0.001f)
        assertTrue(tiltedH.isModified)

        // Flips
        val flipH = defaultState.toggleFlipH()
        assertTrue(flipH.flipH)
        assertTrue(flipH.isModified)

        val flipV = defaultState.toggleFlipV()
        assertTrue(flipV.flipV)
        assertTrue(flipV.isModified)

        // Reset restores pristine default
        val resetState = tiltedV.reset()
        assertEquals(defaultState, resetState)
        assertFalse(resetState.isModified)
    }
}
