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
    EDGE_TOP,
    EDGE_BOTTOM,
    EDGE_LEFT,
    EDGE_RIGHT,
    INSIDE,
    NONE
}

/**
 * Mathematical calculations and hit-testing for crop corner brackets.
 */
object EditorCornerBracketsMath {

    /**
     * Hit tests the touch coordinates against the 4 corner handles, 4 edge lines, and inside of [cropRect].
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

        // Priority 1: 4 Corners (highest precedence)
        if (hypot(touchX - l, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_LEFT
        if (hypot(touchX - r, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_RIGHT
        if (hypot(touchX - l, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_LEFT
        if (hypot(touchX - r, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_RIGHT

        // Priority 2: 4 Edges / lines (touch near boundary lines)
        val edgeMargin = touchRadiusPx.coerceAtLeast(24f)
        val isNearHorizSpan = touchX >= (l - edgeMargin) && touchX <= (r + edgeMargin)
        val isNearVertSpan = touchY >= (t - edgeMargin) && touchY <= (b + edgeMargin)

        if (isNearHorizSpan && abs(touchY - t) <= edgeMargin) return CornerHandle.EDGE_TOP
        if (isNearHorizSpan && abs(touchY - b) <= edgeMargin) return CornerHandle.EDGE_BOTTOM
        if (isNearVertSpan && abs(touchX - l) <= edgeMargin) return CornerHandle.EDGE_LEFT
        if (isNearVertSpan && abs(touchX - r) <= edgeMargin) return CornerHandle.EDGE_RIGHT

        // Priority 3: Inside area
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
                CornerHandle.EDGE_TOP -> {
                    val maxT = (b - minDimensionNormalized).coerceAtLeast(0f)
                    t = (t + deltaYNormalized).coerceIn(0f, maxT)
                }
                CornerHandle.EDGE_BOTTOM -> {
                    val minB = (t + minDimensionNormalized).coerceAtMost(1f)
                    b = (b + deltaYNormalized).coerceIn(minB, 1f)
                }
                CornerHandle.EDGE_LEFT -> {
                    val maxL = (r - minDimensionNormalized).coerceAtLeast(0f)
                    l = (l + deltaXNormalized).coerceIn(0f, maxL)
                }
                CornerHandle.EDGE_RIGHT -> {
                    val minR = (l + minDimensionNormalized).coerceAtMost(1f)
                    r = (r + deltaXNormalized).coerceIn(minR, 1f)
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
            CornerHandle.EDGE_TOP -> {
                val candH = (currentRect.bottom - (currentRect.top + deltaYNormalized)).coerceAtLeast(minH)
                val maxAllowedH = currentRect.bottom.coerceAtLeast(minH)
                val maxH = min(maxAllowedH, if (rNorm > 0f) 1f / rNorm else maxAllowedH)
                val newH = candH.coerceIn(minH, maxH)
                val newW = (newH * rNorm).coerceIn(minW, 1f)
                val newTop = currentRect.bottom - newH
                val cx = (currentRect.left + currentRect.right) / 2f
                val newLeft = (cx - newW / 2f).coerceIn(0f, (1f - newW).coerceAtLeast(0f))
                val newRight = (newLeft + newW).coerceIn(0f, 1f)
                Rect(newLeft, newTop, newRight, currentRect.bottom)
            }
            CornerHandle.EDGE_BOTTOM -> {
                val candH = ((currentRect.bottom + deltaYNormalized) - currentRect.top).coerceAtLeast(minH)
                val maxAllowedH = (1f - currentRect.top).coerceAtLeast(minH)
                val maxH = min(maxAllowedH, if (rNorm > 0f) 1f / rNorm else maxAllowedH)
                val newH = candH.coerceIn(minH, maxH)
                val newW = (newH * rNorm).coerceIn(minW, 1f)
                val newBottom = currentRect.top + newH
                val cx = (currentRect.left + currentRect.right) / 2f
                val newLeft = (cx - newW / 2f).coerceIn(0f, (1f - newW).coerceAtLeast(0f))
                val newRight = (newLeft + newW).coerceIn(0f, 1f)
                Rect(newLeft, currentRect.top, newRight, newBottom)
            }
            CornerHandle.EDGE_LEFT -> {
                val candW = (currentRect.right - (currentRect.left + deltaXNormalized)).coerceAtLeast(minW)
                val maxAllowedW = currentRect.right.coerceAtLeast(minW)
                val maxW = min(maxAllowedW, if (rNorm > 0f) 1f * rNorm else maxAllowedW)
                val newW = candW.coerceIn(minW, maxW)
                val newH = (if (rNorm > 0f) newW / rNorm else newW).coerceIn(minH, 1f)
                val newLeft = currentRect.right - newW
                val cy = (currentRect.top + currentRect.bottom) / 2f
                val newTop = (cy - newH / 2f).coerceIn(0f, (1f - newH).coerceAtLeast(0f))
                val newBottom = (newTop + newH).coerceIn(0f, 1f)
                Rect(newLeft, newTop, currentRect.right, newBottom)
            }
            CornerHandle.EDGE_RIGHT -> {
                val candW = ((currentRect.right + deltaXNormalized) - currentRect.left).coerceAtLeast(minW)
                val maxAllowedW = (1f - currentRect.left).coerceAtLeast(minW)
                val maxW = min(maxAllowedW, if (rNorm > 0f) 1f * rNorm else maxAllowedW)
                val newW = candW.coerceIn(minW, maxW)
                val newH = (if (rNorm > 0f) newW / rNorm else newW).coerceIn(minH, 1f)
                val newRight = currentRect.left + newW
                val cy = (currentRect.top + currentRect.bottom) / 2f
                val newTop = (cy - newH / 2f).coerceIn(0f, (1f - newH).coerceAtLeast(0f))
                val newBottom = (newTop + newH).coerceIn(0f, 1f)
                Rect(currentRect.left, newTop, newRight, newBottom)
            }
            else -> currentRect
        }
    }

    /**
     * Updates [currentRect] for multi-touch pinch-to-zoom and pan operations.
     *
     * @param zoomFactor Multiplier > 1f to zoom into image (reducing crop area), < 1f to zoom out.
     * @param panDeltaXNorm Horizontal drag delta in normalized canvas coordinates.
     * @param panDeltaYNorm Vertical drag delta in normalized canvas coordinates.
     * @param centroidXNorm Center of pinch in normalized coordinates.
     * @param centroidYNorm Center of pinch in normalized coordinates.
     * @param aspectRatio Optional aspect ratio constraint (width / height).
     */
    fun zoomAndPanCropRect(
        currentRect: Rect,
        zoomFactor: Float,
        panDeltaXNorm: Float,
        panDeltaYNorm: Float,
        centroidXNorm: Float,
        centroidYNorm: Float,
        aspectRatio: Float?,
        minDimensionNormalized: Float = 0.05f
    ): Rect {
        if (zoomFactor == 1f && panDeltaXNorm == 0f && panDeltaYNorm == 0f) return currentRect

        // Zoom: Pinching out (zoomFactor > 1) zooms INTO the photo, so the visible crop rect shrinks
        val clampedZoom = zoomFactor.coerceIn(0.2f, 5.0f)
        var newW = (currentRect.width / clampedZoom).coerceIn(minDimensionNormalized, 1f)
        var newH = (currentRect.height / clampedZoom).coerceIn(minDimensionNormalized, 1f)

        if (aspectRatio != null && aspectRatio > 0f) {
            val candidateW = newH * aspectRatio
            if (candidateW <= 1f) {
                newW = candidateW.coerceAtLeast(minDimensionNormalized)
                newH = newW / aspectRatio
            } else {
                newW = 1f
                newH = (1f / aspectRatio).coerceIn(minDimensionNormalized, 1f)
            }
        }

        // Relative anchor in current rect
        val fx = if (currentRect.width > 0f) {
            ((centroidXNorm - currentRect.left) / currentRect.width).coerceIn(0f, 1f)
        } else 0.5f
        val fy = if (currentRect.height > 0f) {
            ((centroidYNorm - currentRect.top) / currentRect.height).coerceIn(0f, 1f)
        } else 0.5f

        // Compute new top-left around centroid
        var newL = centroidXNorm - fx * newW
        var newT = centroidYNorm - fy * newH

        // Pan: Dragging finger right/down moves photo right/down, meaning crop window shifts opposite
        newL -= panDeltaXNorm
        newT -= panDeltaYNorm

        // Clamp to normalized canvas boundaries
        val maxL = (1f - newW).coerceAtLeast(0f)
        val maxT = (1f - newH).coerceAtLeast(0f)
        val clampedL = newL.coerceIn(0f, maxL)
        val clampedT = newT.coerceIn(0f, maxT)

        return Rect(
            left = clampedL,
            top = clampedT,
            right = (clampedL + newW).coerceIn(0f, 1f),
            bottom = (clampedT + newH).coerceIn(0f, 1f)
        )
    }
}

