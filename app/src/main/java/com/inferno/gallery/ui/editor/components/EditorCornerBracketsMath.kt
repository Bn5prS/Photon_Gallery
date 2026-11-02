package com.inferno.gallery.ui.editor.components

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Handle type for corner crop drag operations.
 */
enum class CornerHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    INSIDE,
    NONE
}

/**
 * Mathematical calculations and hit-testing for crop corner brackets.
 */
object EditorCornerBracketsMath {

    /**
     * Hit tests the touch coordinates against the 4 corner handles and inside of [cropRect].
     */
    fun hitTest(
        touchX: Float,
        touchY: Float,
        cropRect: Rect,
        canvasWidth: Float,
        canvasHeight: Float,
        touchRadiusPx: Float
    ): CornerHandle {
        val l = cropRect.left * canvasWidth
        val t = cropRect.top * canvasHeight
        val r = cropRect.right * canvasWidth
        val b = cropRect.bottom * canvasHeight

        // Priority 1: 4 Corners
        if (hypot(touchX - l, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_LEFT
        if (hypot(touchX - r, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_RIGHT
        if (hypot(touchX - l, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_LEFT
        if (hypot(touchX - r, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_RIGHT

        // Priority 2: Inside area
        if (touchX in l..r && touchY in t..b) return CornerHandle.INSIDE

        return CornerHandle.NONE
    }

    /**
     * Updates [currentRect] given a drag delta in normalized coordinates, respecting
     * boundaries [0f..1f], minimum dimension, and [aspectRatio] (pixel width / height).
     */
    fun updateCropRect(
        currentRect: Rect,
        handle: CornerHandle,
        deltaXNormalized: Float,
        deltaYNormalized: Float,
        aspectRatio: Float?,
        canvasWidth: Float,
        canvasHeight: Float,
        minDimensionNormalized: Float = 0.05f
    ): Rect {
        if (handle == CornerHandle.NONE) return currentRect

        // Translation
        if (handle == CornerHandle.INSIDE) {
            val width = currentRect.width
            val height = currentRect.height
            val newLeft = (currentRect.left + deltaXNormalized).coerceIn(0f, (1f - width).coerceAtLeast(0f))
            val newTop = (currentRect.top + deltaYNormalized).coerceIn(0f, (1f - height).coerceAtLeast(0f))
            return Rect(
                left = newLeft,
                top = newTop,
                right = (newLeft + width).coerceIn(0f, 1f),
                bottom = (newTop + height).coerceIn(0f, 1f)
            )
        }

        // Freeform mode (null aspect ratio or zero canvas)
        if (aspectRatio == null || canvasWidth <= 0f || canvasHeight <= 0f) {
            var l = currentRect.left
            var t = currentRect.top
            var r = currentRect.right
            var b = currentRect.bottom

            when (handle) {
                CornerHandle.TOP_LEFT -> {
                    val maxL = (r - minDimensionNormalized).coerceAtLeast(0f)
                    val maxT = (b - minDimensionNormalized).coerceAtLeast(0f)
                    l = (l + deltaXNormalized).coerceIn(0f, maxL)
                    t = (t + deltaYNormalized).coerceIn(0f, maxT)
                }
                CornerHandle.TOP_RIGHT -> {
                    val minR = (l + minDimensionNormalized).coerceAtMost(1f)
                    val maxT = (b - minDimensionNormalized).coerceAtLeast(0f)
                    r = (r + deltaXNormalized).coerceIn(minR, 1f)
                    t = (t + deltaYNormalized).coerceIn(0f, maxT)
                }
                CornerHandle.BOTTOM_LEFT -> {
                    val maxL = (r - minDimensionNormalized).coerceAtLeast(0f)
                    val minB = (t + minDimensionNormalized).coerceAtMost(1f)
                    l = (l + deltaXNormalized).coerceIn(0f, maxL)
                    b = (b + deltaYNormalized).coerceIn(minB, 1f)
                }
                CornerHandle.BOTTOM_RIGHT -> {
                    val minR = (l + minDimensionNormalized).coerceAtMost(1f)
                    val minB = (t + minDimensionNormalized).coerceAtMost(1f)
                    r = (r + deltaXNormalized).coerceIn(minR, 1f)
                    b = (b + deltaYNormalized).coerceIn(minB, 1f)
                }
                else -> {}
            }
            return Rect(l, t, r, b)
        }

        // Fixed aspect ratio mode:
        // A = w_px / h_px = (w_norm * W) / (h_norm * H) => (w_norm / h_norm) = A / (W / H)
        val canvasAspect = canvasWidth / canvasHeight
        val rNorm = aspectRatio / canvasAspect

        val minH = max(minDimensionNormalized, minDimensionNormalized / rNorm)
        val minW = minH * rNorm

        return when (handle) {
            CornerHandle.BOTTOM_RIGHT -> {
                val anchorX = currentRect.left
                val anchorY = currentRect.top
                val maxAllowedW = (1f - anchorX).coerceAtLeast(0f)
                val maxAllowedH = (1f - anchorY).coerceAtLeast(0f)
                val maxH = min(maxAllowedH, if (rNorm > 0f) maxAllowedW / rNorm else maxAllowedH).coerceAtLeast(minH)

                val candW = currentRect.right + deltaXNormalized - anchorX
                val candH = currentRect.bottom + deltaYNormalized - anchorY
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX, anchorY, anchorX + chosenW, anchorY + chosenH)
            }
            CornerHandle.BOTTOM_LEFT -> {
                val anchorX = currentRect.right
                val anchorY = currentRect.top
                val maxAllowedW = anchorX.coerceAtLeast(0f)
                val maxAllowedH = (1f - anchorY).coerceAtLeast(0f)
                val maxH = min(maxAllowedH, if (rNorm > 0f) maxAllowedW / rNorm else maxAllowedH).coerceAtLeast(minH)

                val candW = anchorX - (currentRect.left + deltaXNormalized)
                val candH = currentRect.bottom + deltaYNormalized - anchorY
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX - chosenW, anchorY, anchorX, anchorY + chosenH)
            }
            CornerHandle.TOP_RIGHT -> {
                val anchorX = currentRect.left
                val anchorY = currentRect.bottom
                val maxAllowedW = (1f - anchorX).coerceAtLeast(0f)
                val maxAllowedH = anchorY.coerceAtLeast(0f)
                val maxH = min(maxAllowedH, if (rNorm > 0f) maxAllowedW / rNorm else maxAllowedH).coerceAtLeast(minH)

                val candW = currentRect.right + deltaXNormalized - anchorX
                val candH = anchorY - (currentRect.top + deltaYNormalized)
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX, anchorY - chosenH, anchorX + chosenW, anchorY)
            }
            CornerHandle.TOP_LEFT -> {
                val anchorX = currentRect.right
                val anchorY = currentRect.bottom
                val maxAllowedW = anchorX.coerceAtLeast(0f)
                val maxAllowedH = anchorY.coerceAtLeast(0f)
                val maxH = min(maxAllowedH, if (rNorm > 0f) maxAllowedW / rNorm else maxAllowedH).coerceAtLeast(minH)

                val candW = anchorX - (currentRect.left + deltaXNormalized)
                val candH = anchorY - (currentRect.top + deltaYNormalized)
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX - chosenW, anchorY - chosenH, anchorX, anchorY)
            }
            else -> currentRect
        }
    }
}

