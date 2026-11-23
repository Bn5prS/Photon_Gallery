package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarPhotoAnalyzerTest {

    private fun gradientGrid(shift: Int = 0): IntArray {
        val arr = IntArray(72)
        for (y in 0 until 8) {
            for (x in 0 until 9) {
                // Decreasing left-to-right gradient with slight row variation
                arr[y * 9 + x] = (240 - x * 24 - y * 3 + shift).coerceIn(0, 255)
            }
        }
        return arr
    }

    @Test
    fun dHash_isInvariantToUniformBrightnessShift_andDistinctForReversedGradient() {
        val h1 = PerceptualHash.computeDHash(gradientGrid(0))
        val h2 = PerceptualHash.computeDHash(gradientGrid(15))
        assertEquals(0, PerceptualHash.hammingDistance(h1, h2))

        val reversed = IntArray(72) { idx ->
            val x = idx % 9
            x * 28
        }
        val hRev = PerceptualHash.computeDHash(reversed)
        assertTrue(PerceptualHash.hammingDistance(h1, hRev) > 50)
    }

    @Test
    fun laplacianSharpness_distinguishesSharpEdgesFromBlurryImage() {
        val w = 32
        val h = 32
        // High-contrast checkerboard -> very sharp
        val sharp = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            if ((x + y) % 2 == 0) 240 else 15
        }
        // Smooth flat gradient -> blurry / low high-frequency energy
        val blurry = IntArray(w * h) { i ->
            val x = i % w
            100 + x
        }
        val sharpScore = PerceptualHash.computeLaplacianSharpness(sharp, w, h)
        val blurryScore = PerceptualHash.computeLaplacianSharpness(blurry, w, h)
        assertTrue("Expected sharp ($sharpScore) >> blurry ($blurryScore)", sharpScore > 100f)
        assertTrue("Expected blurry ($blurryScore) < 5f", blurryScore < 5f)
    }

    @Test
    fun cluster_groupsBurstShots_picksSharpestBestShot_andSkipsPureExactDuplicates() {
        val baseHash = 0x5555_AAAA_3333_CCCCL
        // 3 burst photos taken 5 seconds apart, within Hamming distance <= 10
        val burstSoft = SimilarCandidate(1L, baseHash, 1000L, 2_000_000L, "h1", sharpness = 18f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)
        val burstSharp = SimilarCandidate(2L, baseHash xor 0b101101L, 1003L, 2_200_000L, "h2", sharpness = 62f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)
        val burstMid = SimilarCandidate(3L, baseHash xor 0b110001L, 1006L, 2_100_000L, "h3", sharpness = 35f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)

        // Pure exact duplicate pair sharing identical fileHash -> excluded from Similar tab
        val exactHash = 0x1234_5678_9ABC_DEF0L
        val exactA = SimilarCandidate(10L, exactHash, 5000L, 1_500_000L, "same_sha", sharpness = 40f, pixelCount = 8_000_000L, isScreenshotOrDownload = false)
        val exactB = SimilarCandidate(11L, exactHash, 5005L, 1_500_000L, "same_sha", sharpness = 40f, pixelCount = 8_000_000L, isScreenshotOrDownload = false)

        // Unrelated photo far away in hash space
        val unrelated = SimilarCandidate(20L, baseHash.inv(), 9000L, 3_000_000L, "h20", sharpness = 55f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)

        val clusters = SimilarPhotoClusterer.cluster(
            listOf(burstSoft, burstSharp, burstMid, exactA, exactB, unrelated)
        )

        assertEquals(1, clusters.size)
        val c = clusters.single()
        assertTrue(c.isBurst)
        assertEquals(2L, c.bestShotId)
        assertEquals(listOf(2L, 3L, 1L), c.memberIds)
        assertTrue(c.averageSimilarityPct >= 85)
    }

    @Test
    fun cluster_findsNonBurstNearIdenticalViaLshBands() {
        val h = 0x0F0F_F0F0_1111_2222L
        // Taken days apart (> 120s), differing by only 3 bits -> caught by 6x10-bit LSH
        val older = SimilarCandidate(100L, h, 10_000L, 1_000_000L, "shaA", sharpness = 30f, pixelCount = 4_000_000L, isScreenshotOrDownload = false)
        val newer = SimilarCandidate(101L, h xor 0b10101L, 500_000L, 4_000_000L, "shaB", sharpness = 50f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)

        val clusters = SimilarPhotoClusterer.cluster(listOf(older, newer))
        assertEquals(1, clusters.size)
        assertFalse(clusters.single().isBurst)
        assertEquals(101L, clusters.single().bestShotId)
    }

    @Test
    fun detectBlurry_ignoresScreenshotsAndSharpPhotos() {
        val sharpCam = SimilarCandidate(1L, 1L, 100L, 2_000_000L, null, sharpness = 42f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)
        val blurryCam = SimilarCandidate(2L, 2L, 101L, 2_000_000L, null, sharpness = 7.5f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)
        val blurryVeryBad = SimilarCandidate(3L, 3L, 102L, 2_000_000L, null, sharpness = 3.2f, pixelCount = 12_000_000L, isScreenshotOrDownload = false)
        val screenshot = SimilarCandidate(4L, 4L, 103L, 500_000L, null, sharpness = 6.0f, pixelCount = 2_000_000L, isScreenshotOrDownload = true)

        val blurryIds = SimilarPhotoClusterer.detectBlurry(listOf(sharpCam, blurryCam, blurryVeryBad, screenshot))
        assertEquals(listOf(3L, 2L), blurryIds)
    }
}

