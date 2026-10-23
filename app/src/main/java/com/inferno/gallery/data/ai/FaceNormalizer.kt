package com.inferno.gallery.data.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

/**
 * 2D point representation independent of Android framework stubs.
 */
data class FacePoint(val x: Float, val y: Float)

/**
 * 2D Similarity Transform:
 *   u = a * x - b * y + tx
 *   v = b * x + a * y + ty
 *
 * Where:
 *   scale = sqrt(a^2 + b^2)
 *   rotation = atan2(b, a)
 */
data class SimilarityTransform(
    val a: Float,
    val b: Float,
    val tx: Float,
    val ty: Float
) {
    fun toMatrix(): Matrix {
        val matrix = Matrix()
        matrix.setValues(floatArrayOf(
            a, -b, tx,
            b,  a, ty,
            0f, 0f, 1f
        ))
        return matrix
    }

    fun map(x: Float, y: Float): FacePoint {
        return FacePoint(
            x = a * x - b * y + tx,
            y = b * x + a * y + ty
        )
    }
}

/**
 * Normalizes detected faces into a canonical 112×112 RGB frame using 5-point
 * similarity transform (Umeyama/Procrustes 2D).
 *
 * Maps (Left Eye, Right Eye, Nose Tip, Left Mouth, Right Mouth) onto standard
 * ArcFace canonical coordinates to eliminate pose/tilt variations before embedding extraction.
 */
object FaceNormalizer {

    const val CROP_SIZE = 112

    // Canonical ArcFace 112×112 coordinates:
    // Left Eye, Right Eye, Nose Tip, Left Mouth Corner, Right Mouth Corner
    val CANONICAL_POINTS = arrayOf(
        FacePoint(38.2946f, 51.6963f),
        FacePoint(73.5318f, 51.6963f),
        FacePoint(56.0252f, 71.7366f),
        FacePoint(41.5493f, 92.3655f),
        FacePoint(70.7299f, 92.3655f)
    )

    /**
     * Computes the 2D Similarity Transform mapping source landmarks to canonical
     * target points using closed-form least-squares.
     */
    fun computeTransform(
        srcPoints: List<FacePoint>,
        dstPoints: Array<FacePoint> = CANONICAL_POINTS
    ): SimilarityTransform {
        require(srcPoints.size >= 2) { "At least 2 landmark points required for alignment" }
        val n = minOf(srcPoints.size, dstPoints.size)

        // 1. Compute centroids
        var srcMeanX = 0f
        var srcMeanY = 0f
        var dstMeanX = 0f
        var dstMeanY = 0f
        for (i in 0 until n) {
            srcMeanX += srcPoints[i].x
            srcMeanY += srcPoints[i].y
            dstMeanX += dstPoints[i].x
            dstMeanY += dstPoints[i].y
        }
        srcMeanX /= n
        srcMeanY /= n
        dstMeanX /= n
        dstMeanY /= n

        // 2. Compute variance and cross-covariances
        var srcVar = 0.0
        var covXX = 0.0
        var covXY = 0.0

        for (i in 0 until n) {
            val sx = (srcPoints[i].x - srcMeanX).toDouble()
            val sy = (srcPoints[i].y - srcMeanY).toDouble()
            val dx = (dstPoints[i].x - dstMeanX).toDouble()
            val dy = (dstPoints[i].y - dstMeanY).toDouble()

            srcVar += sx * sx + sy * sy
            covXX += sx * dx + sy * dy
            covXY += sx * dy - sy * dx
        }

        if (srcVar < 1e-6) {
            return SimilarityTransform(1f, 0f, 0f, 0f)
        }

        val a = (covXX / srcVar).toFloat()
        val b = (covXY / srcVar).toFloat()
        val tx = dstMeanX - (a * srcMeanX - b * srcMeanY)
        val ty = dstMeanY - (b * srcMeanX + a * srcMeanY)

        return SimilarityTransform(a, b, tx, ty)
    }

    /**
     * Warps the source bitmap using the similarity transform into a new 112×112 Bitmap.
     * The caller is responsible for recycling the returned bitmap when finished.
     */
    fun alignFace(
        source: Bitmap,
        landmarks: List<FacePoint>
    ): Bitmap? {
        if (landmarks.size < 2) return null
        return try {
            val transform = computeTransform(landmarks)
            val matrix = transform.toMatrix()
            val aligned = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(aligned)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(source, matrix, paint)
            aligned
        } catch (_: Exception) {
            null
        }
    }
}
