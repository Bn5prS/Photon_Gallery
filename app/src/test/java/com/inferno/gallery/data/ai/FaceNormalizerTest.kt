package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class FaceNormalizerTest {

    @Test
    fun computeTransform_identicalPoints_producesIdentity() {
        val src = FaceNormalizer.CANONICAL_POINTS.toList()
        val transform = FaceNormalizer.computeTransform(src)

        // Identity transform: a = 1, b = 0, tx = 0, ty = 0
        assertEquals(1.0f, transform.a, 0.001f)
        assertEquals(0.0f, transform.b, 0.001f)
        assertEquals(0.0f, transform.tx, 0.001f)
        assertEquals(0.0f, transform.ty, 0.001f)
    }

    @Test
    fun computeTransform_scaledAndTranslatedPoints_invertsCorrectly() {
        val scale = 2.0f
        val tx = 100f
        val ty = 200f

        // Apply scale & translation to canonical points
        val transformedSrc = FaceNormalizer.CANONICAL_POINTS.map { p ->
            FacePoint(p.x * scale + tx, p.y * scale + ty)
        }

        val transform = FaceNormalizer.computeTransform(transformedSrc)

        // Transform maps transformedSrc -> canonicalPoints.
        // Therefore, scale should be 1/2 = 0.5f and b = 0
        assertEquals(0.5f, transform.a, 0.005f)
        assertEquals(0.0f, transform.b, 0.005f)

        // Test mapping of all 5 points back to canonical points
        for (i in FaceNormalizer.CANONICAL_POINTS.indices) {
            val mapped = transform.map(transformedSrc[i].x, transformedSrc[i].y)
            assertEquals(FaceNormalizer.CANONICAL_POINTS[i].x, mapped.x, 0.05f)
            assertEquals(FaceNormalizer.CANONICAL_POINTS[i].y, mapped.y, 0.05f)
        }
    }

    @Test
    fun computeTransform_rotatedPoints_correctsRotation() {
        val angleRad = Math.toRadians(45.0).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        // Rotate canonical points by 45 degrees around origin
        val rotatedSrc = FaceNormalizer.CANONICAL_POINTS.map { p ->
            FacePoint(p.x * cosA - p.y * sinA, p.x * sinA + p.y * cosA)
        }

        val transform = FaceNormalizer.computeTransform(rotatedSrc)

        // Test mapping of all 5 points back to canonical coordinates
        for (i in FaceNormalizer.CANONICAL_POINTS.indices) {
            val mapped = transform.map(rotatedSrc[i].x, rotatedSrc[i].y)
            assertEquals(FaceNormalizer.CANONICAL_POINTS[i].x, mapped.x, 0.1f)
            assertEquals(FaceNormalizer.CANONICAL_POINTS[i].y, mapped.y, 0.1f)
        }
    }

    @Test
    fun computePoseAwareTransform_missingNose_mapsEyesAndMouthToTrueTargets() {
        val landmarks = DetectedFaceLandmarks(
            leftEye = FacePoint(38.3f, 51.7f),
            rightEye = FacePoint(73.5f, 51.7f),
            nose = null,
            mouthLeft = FacePoint(41.5f, 92.4f),
            mouthRight = FacePoint(70.7f, 92.4f),
            yawDeg = 10f
        )

        val transform = FaceNormalizer.computePoseAwareTransform(landmarks)
        val mappedMouthLeft = transform.map(41.5f, 92.4f)
        val mappedMouthRight = transform.map(70.7f, 92.4f)

        // Must land on canonical mouth row (~92.4), NOT canonical nose (~71.7)!
        assertEquals(FaceNormalizer.CANONICAL_POINTS[3].x, mappedMouthLeft.x, 1.0f)
        assertEquals(FaceNormalizer.CANONICAL_POINTS[3].y, mappedMouthLeft.y, 1.0f)
        assertEquals(FaceNormalizer.CANONICAL_POINTS[4].x, mappedMouthRight.x, 1.0f)
        assertEquals(FaceNormalizer.CANONICAL_POINTS[4].y, mappedMouthRight.y, 1.0f)
    }

    @Test
    fun computePoseAwareTransform_sideProfile_preservesScaleAndAvoidsFakeTilt() {
        // Upright 55-degree right-facing profile: eye separation compressed to 10px,
        // nose protruding to x=72, vertical eye-to-mouth span = 40px (~1.0x canonical 40.67px).
        val profile = DetectedFaceLandmarks(
            leftEye = FacePoint(45f, 50f),
            rightEye = FacePoint(55f, 50f),
            nose = FacePoint(72f, 70f),
            mouthLeft = FacePoint(48f, 90f),
            mouthRight = FacePoint(60f, 90f),
            yawDeg = 55f
        )

        val transform = FaceNormalizer.computePoseAwareTransform(profile)
        val scale = hypot(transform.a.toDouble(), transform.b.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(atan2(transform.b.toDouble(), transform.a.toDouble())).toFloat()

        // Scale should match vertical eye-to-mouth (~1.02), not collapse below 0.75 from foreshortening
        assertTrue("Expected un-foreshortened scale near 1.0, got $scale", scale in 0.90f..1.15f)
        assertTrue("Expected upright alignment without nose-induced roll, got $rollDeg deg", abs(rollDeg) < 8f)

        val mappedNose = transform.map(72f, 70f)
        val mappedLeftEye = transform.map(45f, 50f)
        assertTrue("Nose must stay well inside 112x112 crop, got ${mappedNose.x}", mappedNose.x in 20f..98f)
        assertTrue("Eye must stay well inside 112x112 crop, got ${mappedLeftEye.x}", mappedLeftEye.x in 15f..95f)
    }
}
