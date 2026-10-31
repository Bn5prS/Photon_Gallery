package com.inferno.gallery.ui.editor.crop

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryHelperTest {

    @Test
    fun testHitTestCornersAndInside() {
        val imageRect = Rect(0f, 0f, 1000f, 1000f)
        val cropRect = Rect(0.2f, 0.2f, 0.8f, 0.8f) // Canvas pixel: 200..800
        val cornerRadius = 40f
        val edgeMargin = 20f

        // Near top-left (200, 200)
        assertEquals(
            CropHandle.TOP_LEFT,
            CropGeometryHelper.hitTest(205f, 205f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Near top-right (800, 200)
        assertEquals(
            CropHandle.TOP_RIGHT,
            CropGeometryHelper.hitTest(795f, 205f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Near bottom-left (200, 800)
        assertEquals(
            CropHandle.BOTTOM_LEFT,
            CropGeometryHelper.hitTest(205f, 795f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Near bottom-right (800, 800)
        assertEquals(
            CropHandle.BOTTOM_RIGHT,
            CropGeometryHelper.hitTest(795f, 795f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Near top edge (500, 200)
        assertEquals(
            CropHandle.TOP,
            CropGeometryHelper.hitTest(500f, 205f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Inside center (500, 500)
        assertEquals(
            CropHandle.INSIDE,
            CropGeometryHelper.hitTest(500f, 500f, cropRect, imageRect, cornerRadius, edgeMargin)
        )

        // Far outside (50, 50)
        assertEquals(
            CropHandle.NONE,
            CropGeometryHelper.hitTest(50f, 50f, cropRect, imageRect, cornerRadius, edgeMargin)
        )
    }

    @Test
    fun testFreeformResizeBoundsAndClamping() {
        val initial = Rect(0.2f, 0.2f, 0.8f, 0.8f)
        val minDim = 0.1f

        // Drag bottom right outward
        val expanded = CropGeometryHelper.updateCropRect(
            currentRect = initial,
            handle = CropHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.1f,
            deltaYNormalized = 0.1f,
            aspectLabel = "Free",
            effectiveAspect = 1f,
            minWidthNormalized = minDim,
            minHeightNormalized = minDim
        )
        assertEquals(0.2f, expanded.left, 0.001f)
        assertEquals(0.2f, expanded.top, 0.001f)
        assertEquals(0.9f, expanded.right, 0.001f)
        assertEquals(0.9f, expanded.bottom, 0.001f)

        // Drag beyond image boundary (1.0)
        val clampedBoundary = CropGeometryHelper.updateCropRect(
            currentRect = expanded,
            handle = CropHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.5f,
            deltaYNormalized = 0.5f,
            aspectLabel = "Free",
            effectiveAspect = 1f,
            minWidthNormalized = minDim,
            minHeightNormalized = minDim
        )
        assertEquals(1.0f, clampedBoundary.right, 0.001f)
        assertEquals(1.0f, clampedBoundary.bottom, 0.001f)

        // Drag bottom right inward past minimum size - must clamp
        val clampedMin = CropGeometryHelper.updateCropRect(
            currentRect = initial,
            handle = CropHandle.BOTTOM_RIGHT,
            deltaXNormalized = -0.75f,
            deltaYNormalized = -0.75f,
            aspectLabel = "Free",
            effectiveAspect = 1f,
            minWidthNormalized = minDim,
            minHeightNormalized = minDim
        )
        assertTrue("Width should be at least minDim", clampedMin.width >= minDim - 0.001f)
        assertTrue("Height should be at least minDim", clampedMin.height >= minDim - 0.001f)
    }

    @Test
    fun testInsideTranslation() {
        val initial = Rect(0.2f, 0.2f, 0.6f, 0.6f)
        val translated = CropGeometryHelper.updateCropRect(
            currentRect = initial,
            handle = CropHandle.INSIDE,
            deltaXNormalized = 0.1f,
            deltaYNormalized = 0.1f,
            aspectLabel = "Free",
            effectiveAspect = 1f,
            minWidthNormalized = 0.1f,
            minHeightNormalized = 0.1f
        )
        assertEquals(0.3f, translated.left, 0.001f)
        assertEquals(0.3f, translated.top, 0.001f)
        assertEquals(0.7f, translated.right, 0.001f)
        assertEquals(0.7f, translated.bottom, 0.001f)
    }

    @Test
    fun testFixedAspectRatioPreservation() {
        // Image aspect ratio 1:1, target aspect 1:1 -> normalized ratio = 1:1
        val initial = Rect(0.2f, 0.2f, 0.6f, 0.6f)
        val resized = CropGeometryHelper.updateCropRect(
            currentRect = initial,
            handle = CropHandle.BOTTOM_RIGHT,
            deltaXNormalized = 0.2f,
            deltaYNormalized = 0.1f,
            aspectLabel = "1:1",
            effectiveAspect = 1.0f,
            minWidthNormalized = 0.1f,
            minHeightNormalized = 0.1f
        )
        // Ratio in pixels: (width * effectiveAspect) / height == 1.0
        val pixelRatio = (resized.width * 1.0f) / resized.height
        assertEquals(1.0f, pixelRatio, 0.01f)
    }
}

