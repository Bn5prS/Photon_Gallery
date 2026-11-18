package com.inferno.gallery.ui

import com.inferno.gallery.data.ai.SemanticSearchScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSearchScoringTest {

    @Test
    fun testRejectsFlatNoiseClusterWhenConceptAbsent() {
        // Simulated absent concept across 16 photos: flat background noise around median 0.140,
        // peaking at 0.168 without genuine separation.
        val scores = listOf(
            1L to 0.168f,
            2L to 0.161f,
            3L to 0.155f,
            4L to 0.150f,
            5L to 0.147f,
            6L to 0.145f,
            7L to 0.143f,
            8L to 0.141f,
            9L to 0.140f,
            10L to 0.137f,
            11L to 0.135f,
            12L to 0.133f,
            13L to 0.131f,
            14L to 0.128f,
            15L to 0.124f,
            16L to 0.119f
        )
        val result = SemanticSearchScorer.filterMatches(scores, "flamingo")
        assertTrue("Flat background noise cluster must be rejected cleanly", result.matches.isEmpty())
    }

    @Test
    fun testRejectsSubFloorNoiseInSmallGallery() {
        val smallNoise = listOf(
            1L to 0.166f,
            2L to 0.155f,
            3L to 0.140f,
            4L to 0.130f
        )
        val result = SemanticSearchScorer.filterMatches(smallNoise, "sunset")
        assertTrue("Small gallery below 0.172 noise gate must reject weak matches", result.matches.isEmpty())
    }

    @Test
    fun testPreventsOutlierCollapseWhenOneHeroPhotoScoresHigh() {
        // 1 studio-quality hero match (0.295) plus genuine cluster (0.256..0.222) and noise <= 0.150.
        val scored = mutableListOf(
            1L to 0.295f,
            2L to 0.256f,
            3L to 0.250f,
            4L to 0.242f,
            5L to 0.231f,
            6L to 0.222f
        )
        for (i in 7..24) {
            scored += i.toLong() to (0.150f - (i - 7) * 0.002f)
        }

        val result = SemanticSearchScorer.filterMatches(scored, "cat")
        val ids = result.matches.map { it.mediaId }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), ids)
    }

    @Test
    fun testRetainsCommonHighPrevalenceConceptWithoutOverRaisingFloor() {
        // Simulate a broad concept ("person") appearing in >50% of 16 indexed photos.
        val scored = listOf(
            1L to 0.255f,
            2L to 0.248f,
            3L to 0.242f,
            4L to 0.236f,
            5L to 0.230f,
            6L to 0.224f,
            7L to 0.219f,
            8L to 0.212f,
            9L to 0.206f,
            10L to 0.198f,
            11L to 0.176f,
            12L to 0.165f,
            13L to 0.142f,
            14L to 0.135f,
            15L to 0.128f,
            16L to 0.120f
        )
        val result = SemanticSearchScorer.filterMatches(scored, "person")
        assertTrue("Common concepts must not be erased by elevated median", result.matches.size >= 8)
        assertTrue("Background photos below noise floor must remain excluded", result.matches.none { it.mediaId >= 13L })
    }

    @Test
    fun testSpecificMultiWordQueryTightensMarginVsSingleWord() {
        val scored = mutableListOf(
            1L to 0.252f,
            2L to 0.246f,
            3L to 0.242f,
            4L to 0.212f // Borderline 0.030 below anchor: kept by broad single-word, dropped by 3-word
        )
        for (i in 5..20) {
            scored += i.toLong() to 0.135f
        }

        val broadResult = SemanticSearchScorer.filterMatches(scored, "car")
        val specificResult = SemanticSearchScorer.filterMatches(scored, "red sports car")

        assertTrue(broadResult.matches.any { it.mediaId == 4L })
        assertTrue(specificResult.matches.none { it.mediaId == 4L })
    }

    @Test
    fun testCalibratedDisplayPercentageIsMonotonicAndBounded() {
        val lowPct = SemanticSearchScorer.calculateDisplayPercentage(0.160f, topScore = 0.260f, floor = 0.158f)
        val midPct = SemanticSearchScorer.calculateDisplayPercentage(0.215f, topScore = 0.260f, floor = 0.158f)
        val highPct = SemanticSearchScorer.calculateDisplayPercentage(0.285f, topScore = 0.285f, floor = 0.158f)

        assertTrue(lowPct in 60..75)
        assertTrue(midPct in (lowPct + 5)..94)
        assertEquals(99, highPct)
    }
}
