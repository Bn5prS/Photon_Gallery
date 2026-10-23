package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
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
}
