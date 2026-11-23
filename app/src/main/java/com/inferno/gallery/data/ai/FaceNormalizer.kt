package com.inferno.gallery.data.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 2D point representation independent of Android framework stubs.
 */
data class FacePoint(val x: Float, val y: Float)

/**
 * Named facial landmarks + 3D head pose angles in **image-space (viewer perspective)**:
 * - [leftEye] / [mouthLeft] are on the viewer's LEFT side of an upright face (`CANONICAL_POINTS[0]` & `[3]`),
 *   which correspond to ML Kit's `FaceLandmark.RIGHT_EYE` and `FaceLandmark.MOUTH_RIGHT`.
 * - [rightEye] / [mouthRight] are on the viewer's RIGHT side of an upright face (`CANONICAL_POINTS[1]` & `[4]`),
 *   which correspond to ML Kit's `FaceLandmark.LEFT_EYE` and `FaceLandmark.MOUTH_LEFT`.
 *
 * Keeping landmarks explicitly keyed avoids index-shift bugs when a profile occludes one organ.
 */
data class DetectedFaceLandmarks(
    val leftEye: FacePoint? = null,
    val rightEye: FacePoint? = null,
    val nose: FacePoint? = null,
    val mouthLeft: FacePoint? = null,
    val mouthRight: FacePoint? = null,
    val yawDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f
) {
    val count: Int
        get() {
            var c = 0
            if (leftEye != null) c++
            if (rightEye != null) c++
            if (nose != null) c++
            if (mouthLeft != null) c++
            if (mouthRight != null) c++
            return c
        }
}

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
 * Normalizes detected faces into a canonical 112×112 RGB frame for MobileFaceNet (`w600k_mbf`).
 *
 * - Frontal / near-frontal faces (`|yaw| <= 22°`): standard 5-point Umeyama similarity transform
 *   with anatomical landmark pairing (missing landmarks never shift subsequent indices).
 * - 3/4 and side profiles (`|yaw| >= 32°`, smoothly blended over `22°..32°`): midline vertical
 *   alignment using the yaw-invariant eye-midpoint to mouth-midpoint axis, preventing horizontal
 *   foreshortening scale collapse and protruding-nose roll distortion.
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

    private const val CANONICAL_EYE_Y = 51.6963f
    private const val CANONICAL_MOUTH_Y = 92.3655f
    private const val CANONICAL_EYE_TO_MOUTH = CANONICAL_MOUTH_Y - CANONICAL_EYE_Y // 40.6692f
    private const val CANONICAL_CENTER_X = 56.02f
    private const val CANONICAL_MID_Y = (CANONICAL_EYE_Y + CANONICAL_MOUTH_Y) * 0.5f // 72.0309f

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
        val pairs = ArrayList<Pair<FacePoint, FacePoint>>(n)
        for (i in 0 until n) {
            pairs.add(srcPoints[i] to dstPoints[i])
        }
        return computePairedTransform(pairs)
    }

    private fun computePairedTransform(pairs: List<Pair<FacePoint, FacePoint>>): SimilarityTransform {
        if (pairs.size < 2) return SimilarityTransform(1f, 0f, 0f, 0f)
        val n = pairs.size

        var srcMeanX = 0f
        var srcMeanY = 0f
        var dstMeanX = 0f
        var dstMeanY = 0f
        for ((s, d) in pairs) {
            srcMeanX += s.x
            srcMeanY += s.y
            dstMeanX += d.x
            dstMeanY += d.y
        }
        srcMeanX /= n
        srcMeanY /= n
        dstMeanX /= n
        dstMeanY /= n

        var srcVar = 0.0
        var covXX = 0.0
        var covXY = 0.0

        for ((s, d) in pairs) {
            val sx = (s.x - srcMeanX).toDouble()
            val sy = (s.y - srcMeanY).toDouble()
            val dx = (d.x - dstMeanX).toDouble()
            val dy = (d.y - dstMeanY).toDouble()

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
     * Rotation-invariant handedness guard: for any valid image-space landmark set at ANY roll angle,
     * the 2D cross product `(imageRight - imageLeft) × (lowerAnchor - upperAnchor)` in screen
     * coordinates (`+X` right, `+Y` down) is strictly positive. If negative, left/right pairs were
     * passed in subject-perspective order (e.g., raw ML Kit LEFT/RIGHT) and are swapped into
     * canonical image-space order so similarity alignment never collapses scale.
     */
    fun canonicalizeLandmarkHandedness(landmarks: DetectedFaceLandmarks): DetectedFaceLandmarks {
        val eyeMid = when {
            landmarks.leftEye != null && landmarks.rightEye != null -> FacePoint(
                (landmarks.leftEye.x + landmarks.rightEye.x) * 0.5f,
                (landmarks.leftEye.y + landmarks.rightEye.y) * 0.5f
            )
            landmarks.leftEye != null -> landmarks.leftEye
            else -> landmarks.rightEye
        }
        val mouthMid = when {
            landmarks.mouthLeft != null && landmarks.mouthRight != null -> FacePoint(
                (landmarks.mouthLeft.x + landmarks.mouthRight.x) * 0.5f,
                (landmarks.mouthLeft.y + landmarks.mouthRight.y) * 0.5f
            )
            landmarks.mouthLeft != null -> landmarks.mouthLeft
            else -> landmarks.mouthRight
        }
        val downAnchor = mouthMid ?: landmarks.nose
        val upAnchor = eyeMid ?: landmarks.nose

        var swappedEyes = false
        var swappedMouth = false
        if (upAnchor != null && downAnchor != null && upAnchor !== downAnchor) {
            val ux = downAnchor.x - upAnchor.x
            val uy = downAnchor.y - upAnchor.y
            val uLen = hypot(ux.toDouble(), uy.toDouble()).toFloat()
            if (uLen >= 3f) {
                if (landmarks.leftEye != null && landmarks.rightEye != null) {
                    val ex = landmarks.rightEye.x - landmarks.leftEye.x
                    val ey = landmarks.rightEye.y - landmarks.leftEye.y
                    val cross = ex * uy - ey * ux
                    if (cross < -1e-2f) swappedEyes = true
                }
                if (landmarks.mouthLeft != null && landmarks.mouthRight != null) {
                    val mx = landmarks.mouthRight.x - landmarks.mouthLeft.x
                    val my = landmarks.mouthRight.y - landmarks.mouthLeft.y
                    val cross = mx * uy - my * ux
                    if (cross < -1e-2f) swappedMouth = true
                }
            }
        }
        if (!swappedEyes && !swappedMouth) return landmarks
        return landmarks.copy(
            leftEye = if (swappedEyes) landmarks.rightEye else landmarks.leftEye,
            rightEye = if (swappedEyes) landmarks.leftEye else landmarks.rightEye,
            mouthLeft = if (swappedMouth) landmarks.mouthRight else landmarks.mouthLeft,
            mouthRight = if (swappedMouth) landmarks.mouthLeft else landmarks.mouthRight
        )
    }

    /**
     * Pose-aware alignment transform that preserves exact 5-point frontal accuracy while keeping
     * side-profile scale and upright orientation stable.
     */
    fun computePoseAwareTransform(rawLandmarks: DetectedFaceLandmarks): SimilarityTransform {
        val landmarks = canonicalizeLandmarkHandedness(rawLandmarks)
        val pairs = ArrayList<Pair<FacePoint, FacePoint>>(5)
        landmarks.leftEye?.let { pairs += it to CANONICAL_POINTS[0] }
        landmarks.rightEye?.let { pairs += it to CANONICAL_POINTS[1] }
        landmarks.nose?.let { pairs += it to CANONICAL_POINTS[2] }
        landmarks.mouthLeft?.let { pairs += it to CANONICAL_POINTS[3] }
        landmarks.mouthRight?.let { pairs += it to CANONICAL_POINTS[4] }

        val eyeMid = when {
            landmarks.leftEye != null && landmarks.rightEye != null -> FacePoint(
                (landmarks.leftEye.x + landmarks.rightEye.x) * 0.5f,
                (landmarks.leftEye.y + landmarks.rightEye.y) * 0.5f
            )
            landmarks.leftEye != null -> landmarks.leftEye
            else -> landmarks.rightEye
        }
        val mouthMid = when {
            landmarks.mouthLeft != null && landmarks.mouthRight != null -> FacePoint(
                (landmarks.mouthLeft.x + landmarks.mouthRight.x) * 0.5f,
                (landmarks.mouthLeft.y + landmarks.mouthRight.y) * 0.5f
            )
            landmarks.mouthLeft != null -> landmarks.mouthLeft
            else -> landmarks.mouthRight
        }

        val eyeDist = if (landmarks.leftEye != null && landmarks.rightEye != null) {
            hypot(
                (landmarks.rightEye.x - landmarks.leftEye.x).toDouble(),
                (landmarks.rightEye.y - landmarks.leftEye.y).toDouble()
            ).toFloat()
        } else 0f

        val eyeToMouthDist = if (eyeMid != null && mouthMid != null) {
            hypot(
                (mouthMid.x - eyeMid.x).toDouble(),
                (mouthMid.y - eyeMid.y).toDouble()
            ).toFloat()
        } else 0f

        val isForeshortenedProfile = eyeToMouthDist >= 4f && (
            abs(landmarks.yawDeg) > 22f ||
                landmarks.leftEye == null ||
                landmarks.rightEye == null ||
                eyeDist < 0.42f * eyeToMouthDist
            )

        val pairedTransform = if (pairs.size >= 2) computePairedTransform(pairs) else null
        if (!isForeshortenedProfile && pairedTransform != null) {
            return pairedTransform
        }

        val profileTransform = computeMidlineProfileTransform(eyeMid, mouthMid, landmarks.nose)
            ?: return pairedTransform ?: SimilarityTransform(1f, 0f, 0f, 0f)

        // Smooth blend between 22° and 32° yaw when >= 4 landmarks are available
        val absYaw = abs(landmarks.yawDeg)
        if (pairedTransform != null && pairs.size >= 4 && absYaw in 22f..32f && eyeDist >= 0.35f * eyeToMouthDist) {
            val t = ((absYaw - 22f) / 10f).coerceIn(0f, 1f)
            return SimilarityTransform(
                a = pairedTransform.a * (1f - t) + profileTransform.a * t,
                b = pairedTransform.b * (1f - t) + profileTransform.b * t,
                tx = pairedTransform.tx * (1f - t) + profileTransform.tx * t,
                ty = pairedTransform.ty * (1f - t) + profileTransform.ty * t
            )
        }

        return profileTransform
    }

    private fun computeMidlineProfileTransform(
        eyeMid: FacePoint?,
        mouthMid: FacePoint?,
        nose: FacePoint?
    ): SimilarityTransform? {
        if (eyeMid != null && mouthMid != null) {
            val vx = mouthMid.x - eyeMid.x
            val vy = mouthMid.y - eyeMid.y
            val vLen = hypot(vx.toDouble(), vy.toDouble()).toFloat()
            if (vLen >= 4f) {
                val scale = CANONICAL_EYE_TO_MOUTH / vLen
                // Angle of head tilt from vertical midline (eyeMid -> mouthMid)
                val rawRollRad = atan2((-vx).toDouble(), vy.toDouble()).toFloat()
                val eightDegRad = 0.14f
                val dampedRollRad = if (abs(rawRollRad) <= eightDegRad) {
                    rawRollRad * 0.5f
                } else {
                    rawRollRad
                }
                val a = scale * cos(dampedRollRad.toDouble()).toFloat()
                val b = -scale * sin(dampedRollRad.toDouble()).toFloat()

                val anchorX = if (nose != null) {
                    (eyeMid.x + mouthMid.x + nose.x) / 3f
                } else {
                    (eyeMid.x + mouthMid.x) * 0.5f
                }
                val anchorY = (eyeMid.y + mouthMid.y) * 0.5f

                val tx = CANONICAL_CENTER_X - (a * anchorX - b * anchorY)
                val ty = CANONICAL_MID_Y - (b * anchorX + a * anchorY)
                return SimilarityTransform(a, b, tx, ty)
            }
        }

        if (eyeMid != null && nose != null) {
            val vx = nose.x - eyeMid.x
            val vy = nose.y - eyeMid.y
            val vLen = hypot(vx.toDouble(), vy.toDouble()).toFloat()
            if (vLen >= 3f) {
                val scale = (20.0403f / vy.coerceAtLeast(3f)).coerceIn(0.1f, 10f)
                val anchorX = (eyeMid.x + nose.x) * 0.5f
                val anchorY = (eyeMid.y + nose.y) * 0.5f
                return SimilarityTransform(
                    a = scale,
                    b = 0f,
                    tx = CANONICAL_CENTER_X - scale * anchorX,
                    ty = 61.7f - scale * anchorY
                )
            }
        }

        return null
    }

    /**
     * Warps the source bitmap using the pose-aware similarity transform into a 112×112 Bitmap.
     */
    fun alignFacePoseAware(
        source: Bitmap,
        landmarks: DetectedFaceLandmarks
    ): Bitmap? {
        if (source.isRecycled || landmarks.count < 2) return null
        return warpWithTransform(source, computePoseAwareTransform(landmarks))
    }

    /**
     * Warps the source bitmap using the similarity transform into a new 112×112 Bitmap.
     * The caller is responsible for recycling the returned bitmap when finished.
     */
    fun alignFace(
        source: Bitmap,
        landmarks: List<FacePoint>
    ): Bitmap? {
        if (source.isRecycled) return null
        if (landmarks.size < 2) return null
        return try {
            warpWithTransform(source, computeTransform(landmarks))
        } catch (_: Throwable) {
            null
        }
    }

    private fun warpWithTransform(source: Bitmap, transform: SimilarityTransform): Bitmap? {
        return try {
            if (transform.a.isNaN() || transform.b.isNaN() || transform.tx.isNaN() || transform.ty.isNaN() ||
                transform.a.isInfinite() || transform.b.isInfinite() || transform.tx.isInfinite() || transform.ty.isInfinite()
            ) {
                return null
            }
            val scale = hypot(transform.a.toDouble(), transform.b.toDouble())
            if (scale < 1e-3 || scale > 50.0) return null

            val matrix = transform.toMatrix()
            val safeSource = if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            } ?: return null
            if (safeSource.isRecycled) return null

            val aligned = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(aligned)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(safeSource, matrix, paint)
            if (safeSource !== source && !safeSource.isRecycled) {
                safeSource.recycle()
            }
            aligned
        } catch (_: Throwable) {
            null
        }
    }
}
