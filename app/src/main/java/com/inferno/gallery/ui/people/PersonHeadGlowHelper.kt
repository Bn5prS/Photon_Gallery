package com.inferno.gallery.ui.people

import androidx.compose.ui.geometry.Rect
import kotlin.math.min

/**
 * Utility helper to project normalized face bounding boxes into viewport coordinates
 * with natural head and hair framing for the glowing hairline animation.
 */
object PersonHeadGlowHelper {

    /**
     * Calculates the projected head bounds in viewport coordinates from normalized face coordinates [0f..1f].
     *
     * Expands the face bounding box upward by 28% (hair volume) and horizontally by 14% to gracefully
     * enclose the whole head and hair silhouette.
     * Maps through ContentScale.Fit logic (fitScale, dx, dy) to screen/viewport space.
     * Returns null if inputs are invalid or non-finite.
     */
    fun calculateHeadBounds(
        bboxLeft: Float,
        bboxTop: Float,
        bboxRight: Float,
        bboxBottom: Float,
        imageWidth: Float,
        imageHeight: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Rect? {
        if (imageWidth <= 0f || imageHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) return null
        if (!imageWidth.isFinite() || !imageHeight.isFinite() || !viewportWidth.isFinite() || !viewportHeight.isFinite()) return null
        if (bboxRight <= bboxLeft || bboxBottom <= bboxTop) return null

        val fitScale = min(viewportWidth / imageWidth, viewportHeight / imageHeight)
        val dx = (viewportWidth - imageWidth * fitScale) / 2f
        val dy = (viewportHeight - imageHeight * fitScale) / 2f

        val faceW = (bboxRight - bboxLeft) * imageWidth * fitScale
        val faceH = (bboxBottom - bboxTop) * imageHeight * fitScale
        val faceX = dx + (bboxLeft * imageWidth * fitScale)
        val faceY = dy + (bboxTop * imageHeight * fitScale)

        // Head & hair expansion
        val padTop = faceH * 0.28f
        val padSide = faceW * 0.14f
        val padBottom = faceH * 0.10f

        val left = (faceX - padSide).coerceAtLeast(0f)
        val top = (faceY - padTop).coerceAtLeast(0f)
        val right = (faceX + faceW + padSide).coerceAtMost(viewportWidth)
        val bottom = (faceY + faceH + padBottom).coerceAtMost(viewportHeight)

        if (right <= left || bottom <= top) return null
        return Rect(left, top, right, bottom)
    }
}
