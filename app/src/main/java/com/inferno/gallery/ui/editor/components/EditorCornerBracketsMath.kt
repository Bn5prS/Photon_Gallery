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

data class CropViewportGeometry(
    val baseW: Float,
    val baseH: Float,
    val baseLeft: Float,
    val baseTop: Float,
    val targetFrame: Rect,
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
    val cropBoxOnScreen: Rect,
    val photoScreenLeft: Float,
    val photoScreenTop: Float,
    val photoScreenWidth: Float,
    val photoScreenHeight: Float
)

/**
 * Mathematical calculations and hit-testing for crop corner brackets.
 */
object EditorCornerBracketsMath {

    /**
     * Calculates the viewport geometry for photo layout and centered crop frame.
     * Scale is always uniform (never stretches).
     */
    fun calculateViewportGeometry(
        viewWidth: Float,
        viewHeight: Float,
        photoAspect: Float,
        cropRect: Rect,
        framingFraction: Float = 0.84f
    ): CropViewportGeometry {
        if (viewWidth <= 0f || viewHeight <= 0f || photoAspect <= 0f) {
            val r = Rect(0f, 0f, viewWidth, viewHeight)
            return CropViewportGeometry(
                baseW = viewWidth,
                baseH = viewHeight,
                baseLeft = 0f,
                baseTop = 0f,
                targetFrame = r,
                scale = 1f,
                translationX = 0f,
                translationY = 0f,
                cropBoxOnScreen = r,
                photoScreenLeft = 0f,
                photoScreenTop = 0f,
                photoScreenWidth = viewWidth,
                photoScreenHeight = viewHeight
            )
        }

        val framingW = viewWidth * framingFraction
        val framingH = viewHeight * framingFraction

        val (baseW, baseH) = if (photoAspect > (framingW / framingH)) {
            framingW to (framingW / photoAspect)
        } else {
            (framingH * photoAspect) to framingH
        }

        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        val baseLeft = centerX - baseW / 2f
        val baseTop = centerY - baseH / 2f

        val normW = cropRect.width.coerceIn(0.02f, 1f)
        val normH = cropRect.height.coerceIn(0.02f, 1f)
        val cropAspect = (normW / normH) * photoAspect

        val (frameW, frameH) = if (cropAspect > (framingW / framingH)) {
            framingW to (framingW / cropAspect)
        } else {
            (framingH * cropAspect) to maxOf(1f, framingH)
        }

        val frameLeft = centerX - frameW / 2f
        val frameTop = centerY - frameH / 2f
        val targetFrame = Rect(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH)

        val scale = frameW / (normW * baseW)
        val cropCenterX = (cropRect.left + cropRect.right) / 2f
        val cropCenterY = (cropRect.top + cropRect.bottom) / 2f
        val translationX = (0.5f - cropCenterX) * baseW * scale
        val translationY = (0.5f - cropCenterY) * baseH * scale

        val photoScreenWidth = baseW * scale
        val photoScreenHeight = baseH * scale
        val photoScreenLeft = centerX + translationX - photoScreenWidth / 2f
        val photoScreenTop = centerY + translationY - photoScreenHeight / 2f

        val boxL = photoScreenLeft + cropRect.left * photoScreenWidth
        val boxT = photoScreenTop + cropRect.top * photoScreenHeight
        val boxR = photoScreenLeft + cropRect.right * photoScreenWidth
        val boxB = photoScreenTop + cropRect.bottom * photoScreenHeight

        return CropViewportGeometry(
            baseW = baseW,
            baseH = baseH,
            baseLeft = baseLeft,
            baseTop = baseTop,
            targetFrame = targetFrame,
            scale = scale,
            translationX = translationX,
            translationY = translationY,
            cropBoxOnScreen = Rect(boxL, boxT, boxR, boxB),
            photoScreenLeft = photoScreenLeft,
            photoScreenTop = photoScreenTop,
            photoScreenWidth = photoScreenWidth,
            photoScreenHeight = photoScreenHeight
        )
    }

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
     * Hit tests screen coordinates against the 4 corner handles, 4 edge lines, and inside of [boxRect].
     */
    fun hitTestScreen(
        touchX: Float,
        touchY: Float,
        boxRect: Rect,
        touchRadiusPx: Float
    ): CornerHandle {
        val l = boxRect.left
        val t = boxRect.top
        val r = boxRect.right
        val b = boxRect.bottom

        // Priority 1: 4 Corners
        if (hypot(touchX - l, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_LEFT
        if (hypot(touchX - r, touchY - t) <= touchRadiusPx) return CornerHandle.TOP_RIGHT
        if (hypot(touchX - l, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_LEFT
        if (hypot(touchX - r, touchY - b) <= touchRadiusPx) return CornerHandle.BOTTOM_RIGHT

        // Priority 2: 4 Edges
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

    /**
     * Pans the photo behind the locked crop frame in response to 1-finger swipe or 2-finger drag.
     * Dragging right reveals more left content (shifts crop left); dragging down reveals more top content.
     */
    fun panPhotoInCrop(
        currentRect: Rect,
        dragDeltaXPx: Float,
        dragDeltaYPx: Float,
        photoWidthPx: Float,
        photoHeightPx: Float
    ): Rect {
        if (photoWidthPx <= 0f || photoHeightPx <= 0f) return currentRect
        val normW = currentRect.width
        val normH = currentRect.height

        val deltaNormX = -dragDeltaXPx / photoWidthPx
        val deltaNormY = -dragDeltaYPx / photoHeightPx

        val maxL = (1f - normW).coerceAtLeast(0f)
        val maxT = (1f - normH).coerceAtLeast(0f)

        val newL = (currentRect.left + deltaNormX).coerceIn(0f, maxL)
        val newT = (currentRect.top + deltaNormY).coerceIn(0f, maxT)

        return Rect(newL, newT, (newL + normW).coerceIn(0f, 1f), (newT + normH).coerceIn(0f, 1f))
    }

    /**
     * Zooms the photo behind the locked crop frame in response to multi-touch pinch.
     * Pinching out (zoomFactor > 1) enlarges the photo, reducing the normalized crop window.
     * Pinching in (zoomFactor < 1) shrinks the photo, expanding the normalized crop window.
     *
     * In both freeform and aspect-constrained modes, zooming the photo scales the crop window
     * uniformly so the aspect ratio of the crop box is 100% preserved. The crop window is
     * strictly clamped so it can never exceed 1.0 in either dimension (photo can never shrink
     * smaller than the crop box), and its boundaries are clamped strictly inside [0..1].
     */
    fun zoomPhotoInCrop(
        currentRect: Rect,
        zoomFactor: Float,
        centroidXPx: Float,
        centroidYPx: Float,
        photoLeftPx: Float,
        photoTopPx: Float,
        photoWidthPx: Float,
        photoHeightPx: Float,
        aspectRatio: Float? = null,
        minDimensionNormalized: Float = 0.05f
    ): Rect {
        if (photoWidthPx <= 0f || photoHeightPx <= 0f) return currentRect
        val clampedZoom = zoomFactor.coerceIn(0.2f, 5.0f)
        if (clampedZoom == 1.0f) return currentRect

        val oldW = currentRect.width
        val oldH = currentRect.height
        if (oldW <= 0f || oldH <= 0f) return currentRect

        val cropScale = 1f / clampedZoom

        // The crop window can NEVER expand beyond 1.0 in either width or height
        val maxCropScale = minOf(1f / oldW, 1f / oldH).coerceAtLeast(1f)
        val minCropScale = maxOf(minDimensionNormalized / oldW, minDimensionNormalized / oldH).coerceAtMost(1f)

        val effectiveScale = cropScale.coerceIn(minCropScale, maxCropScale)
        val newW = (oldW * effectiveScale).coerceIn(minDimensionNormalized, 1f)
        val newH = (oldH * effectiveScale).coerceIn(minDimensionNormalized, 1f)

        val focusNormX = ((centroidXPx - photoLeftPx) / photoWidthPx).coerceIn(0f, 1f)
        val focusNormY = ((centroidYPx - photoTopPx) / photoHeightPx).coerceIn(0f, 1f)

        val newL = focusNormX - (focusNormX - currentRect.left) * effectiveScale
        val newT = focusNormY - (focusNormY - currentRect.top) * effectiveScale

        val maxL = (1f - newW).coerceAtLeast(0f)
        val maxT = (1f - newH).coerceAtLeast(0f)

        val clampedL = newL.coerceIn(0f, maxL)
        val clampedT = newT.coerceIn(0f, maxT)

        return Rect(clampedL, clampedT, clampedL + newW, clampedT + newH)
    }
}

