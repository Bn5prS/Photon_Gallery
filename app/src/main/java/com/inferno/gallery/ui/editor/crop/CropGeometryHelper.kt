package com.inferno.gallery.ui.editor.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class CropHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    INSIDE
}

object CropGeometryHelper {

    /**
     * Resolves aspect ratio as target width / target height in image pixel coordinates.
     */
    fun targetAspectRatio(aspectLabel: String, effectiveAspect: Float): Float? {
        return when (aspectLabel) {
            "Original" -> effectiveAspect
            "1:1" -> 1.0f
            "5:4" -> 5f / 4f
            "4:5" -> 4f / 5f
            "4:3" -> 4f / 3f
            "3:4" -> 3f / 4f
            "3:2" -> 3f / 2f
            "2:3" -> 2f / 3f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            else -> null // "Free" or unconstrained
        }
    }

    /**
     * Calculates default centered crop rect for a given aspect ratio within image bounds.
     */
    fun calculateInitialAspectRect(aspectLabel: String, effectiveAspect: Float): Rect {
        val targetRatio = targetAspectRatio(aspectLabel, effectiveAspect) ?: return Rect(0f, 0f, 1f, 1f)
        val r = targetRatio / effectiveAspect
        return if (r <= 1f) {
            val left = ((1f - r) / 2f).coerceIn(0f, 1f)
            Rect(left = left, top = 0f, right = (left + r).coerceIn(0f, 1f), bottom = 1f)
        } else {
            val h = 1f / r
            val top = ((1f - h) / 2f).coerceIn(0f, 1f)
            Rect(left = 0f, top = top, right = 1f, bottom = (top + h).coerceIn(0f, 1f))
        }
    }

    /**
     * Identifies which crop handle or region the touch point hits on canvas.
     */
    fun hitTest(
        touchX: Float,
        touchY: Float,
        cropRect: Rect,
        imageRect: Rect,
        cornerHitRadiusPx: Float,
        edgeHitMarginPx: Float
    ): CropHandle {
        val left = imageRect.left + cropRect.left * imageRect.width
        val top = imageRect.top + cropRect.top * imageRect.height
        val right = imageRect.left + cropRect.right * imageRect.width
        val bottom = imageRect.top + cropRect.bottom * imageRect.height

        // 1. Corners (highest priority)
        if (hypot(touchX - left, touchY - top) <= cornerHitRadiusPx) return CropHandle.TOP_LEFT
        if (hypot(touchX - right, touchY - top) <= cornerHitRadiusPx) return CropHandle.TOP_RIGHT
        if (hypot(touchX - left, touchY - bottom) <= cornerHitRadiusPx) return CropHandle.BOTTOM_LEFT
        if (hypot(touchX - right, touchY - bottom) <= cornerHitRadiusPx) return CropHandle.BOTTOM_RIGHT

        // 2. Edges
        val inHorizontalRange = touchX in (left - edgeHitMarginPx)..(right + edgeHitMarginPx)
        val inVerticalRange = touchY in (top - edgeHitMarginPx)..(bottom + edgeHitMarginPx)

        if (inHorizontalRange && abs(touchY - top) <= edgeHitMarginPx) return CropHandle.TOP
        if (inHorizontalRange && abs(touchY - bottom) <= edgeHitMarginPx) return CropHandle.BOTTOM
        if (inVerticalRange && abs(touchX - left) <= edgeHitMarginPx) return CropHandle.LEFT
        if (inVerticalRange && abs(touchX - right) <= edgeHitMarginPx) return CropHandle.RIGHT

        // 3. Inside crop area
        if (touchX in left..right && touchY in top..bottom) return CropHandle.INSIDE

        return CropHandle.NONE
    }

    /**
     * Updates crop rect based on active handle drag delta.
     */
    fun updateCropRect(
        currentRect: Rect,
        handle: CropHandle,
        deltaXNormalized: Float,
        deltaYNormalized: Float,
        aspectLabel: String,
        effectiveAspect: Float,
        minWidthNormalized: Float,
        minHeightNormalized: Float
    ): Rect {
        if (handle == CropHandle.NONE) return currentRect

        // Translation
        if (handle == CropHandle.INSIDE) {
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

        val targetRatio = targetAspectRatio(aspectLabel, effectiveAspect)

        // Freeform mode (no fixed aspect ratio)
        if (targetRatio == null) {
            var l = currentRect.left
            var t = currentRect.top
            var r = currentRect.right
            var b = currentRect.bottom

            when (handle) {
                CropHandle.TOP_LEFT -> {
                    l = (l + deltaXNormalized).coerceIn(0f, r - minWidthNormalized)
                    t = (t + deltaYNormalized).coerceIn(0f, b - minHeightNormalized)
                }
                CropHandle.TOP_RIGHT -> {
                    r = (r + deltaXNormalized).coerceIn(l + minWidthNormalized, 1f)
                    t = (t + deltaYNormalized).coerceIn(0f, b - minHeightNormalized)
                }
                CropHandle.BOTTOM_LEFT -> {
                    l = (l + deltaXNormalized).coerceIn(0f, r - minWidthNormalized)
                    b = (b + deltaYNormalized).coerceIn(t + minHeightNormalized, 1f)
                }
                CropHandle.BOTTOM_RIGHT -> {
                    r = (r + deltaXNormalized).coerceIn(l + minWidthNormalized, 1f)
                    b = (b + deltaYNormalized).coerceIn(t + minHeightNormalized, 1f)
                }
                CropHandle.LEFT -> {
                    l = (l + deltaXNormalized).coerceIn(0f, r - minWidthNormalized)
                }
                CropHandle.RIGHT -> {
                    r = (r + deltaXNormalized).coerceIn(l + minWidthNormalized, 1f)
                }
                CropHandle.TOP -> {
                    t = (t + deltaYNormalized).coerceIn(0f, b - minHeightNormalized)
                }
                CropHandle.BOTTOM -> {
                    b = (b + deltaYNormalized).coerceIn(t + minHeightNormalized, 1f)
                }
                else -> {}
            }
            return Rect(l, t, r, b)
        }

        // Fixed aspect ratio mode: normalized aspect ratio rNorm = width / height
        val rNorm = targetRatio / effectiveAspect
        val minH = max(minHeightNormalized, minWidthNormalized / rNorm)
        val minW = minH * rNorm

        return when (handle) {
            CropHandle.BOTTOM_RIGHT -> {
                // Anchor is top-left
                val anchorX = currentRect.left
                val anchorY = currentRect.top
                val maxAllowedW = 1f - anchorX
                val maxAllowedH = 1f - anchorY
                val maxH = min(maxAllowedH, maxAllowedW / rNorm)

                val candW = (currentRect.right + deltaXNormalized - anchorX)
                val candH = (currentRect.bottom + deltaYNormalized - anchorY)
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX, anchorY, anchorX + chosenW, anchorY + chosenH)
            }
            CropHandle.BOTTOM_LEFT -> {
                // Anchor is top-right
                val anchorX = currentRect.right
                val anchorY = currentRect.top
                val maxAllowedW = anchorX
                val maxAllowedH = 1f - anchorY
                val maxH = min(maxAllowedH, maxAllowedW / rNorm)

                val candW = (anchorX - (currentRect.left + deltaXNormalized))
                val candH = (currentRect.bottom + deltaYNormalized - anchorY)
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX - chosenW, anchorY, anchorX, anchorY + chosenH)
            }
            CropHandle.TOP_RIGHT -> {
                // Anchor is bottom-left
                val anchorX = currentRect.left
                val anchorY = currentRect.bottom
                val maxAllowedW = 1f - anchorX
                val maxAllowedH = anchorY
                val maxH = min(maxAllowedH, maxAllowedW / rNorm)

                val candW = (currentRect.right + deltaXNormalized - anchorX)
                val candH = (anchorY - (currentRect.top + deltaYNormalized))
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX, anchorY - chosenH, anchorX + chosenW, anchorY)
            }
            CropHandle.TOP_LEFT -> {
                // Anchor is bottom-right
                val anchorX = currentRect.right
                val anchorY = currentRect.bottom
                val maxAllowedW = anchorX
                val maxAllowedH = anchorY
                val maxH = min(maxAllowedH, maxAllowedW / rNorm)

                val candW = (anchorX - (currentRect.left + deltaXNormalized))
                val candH = (anchorY - (currentRect.top + deltaYNormalized))
                val chosenH = if (abs(deltaXNormalized) > abs(deltaYNormalized)) {
                    (candW / rNorm).coerceIn(minH, maxH)
                } else {
                    candH.coerceIn(minH, maxH)
                }
                val chosenW = chosenH * rNorm
                Rect(anchorX - chosenW, anchorY - chosenH, anchorX, anchorY)
            }
            CropHandle.LEFT, CropHandle.RIGHT -> {
                // Adjust width, scale height centered
                val dw = if (handle == CropHandle.RIGHT) deltaXNormalized else -deltaXNormalized
                val newW = (currentRect.width + dw).coerceIn(minW, min(1f, 1f * rNorm))
                val newH = (newW / rNorm).coerceIn(minH, 1f)
                val adjustedW = newH * rNorm

                val midY = currentRect.top + currentRect.height / 2f
                val newTop = (midY - newH / 2f).coerceIn(0f, 1f - newH)
                val newLeft = if (handle == CropHandle.RIGHT) {
                    currentRect.left.coerceIn(0f, 1f - adjustedW)
                } else {
                    (currentRect.right - adjustedW).coerceIn(0f, 1f - adjustedW)
                }
                Rect(newLeft, newTop, newLeft + adjustedW, newTop + newH)
            }
            CropHandle.TOP, CropHandle.BOTTOM -> {
                // Adjust height, scale width centered
                val dh = if (handle == CropHandle.BOTTOM) deltaYNormalized else -deltaYNormalized
                val newH = (currentRect.height + dh).coerceIn(minH, min(1f, 1f / rNorm))
                val newW = (newH * rNorm).coerceIn(minW, 1f)
                val adjustedH = newW / rNorm

                val midX = currentRect.left + currentRect.width / 2f
                val newLeft = (midX - newW / 2f).coerceIn(0f, 1f - newW)
                val newTop = if (handle == CropHandle.BOTTOM) {
                    currentRect.top.coerceIn(0f, 1f - adjustedH)
                } else {
                    (currentRect.bottom - adjustedH).coerceIn(0f, 1f - adjustedH)
                }
                Rect(newLeft, newTop, newLeft + newW, newTop + adjustedH)
            }
            else -> currentRect
        }
    }
}

