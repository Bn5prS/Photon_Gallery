package com.inferno.gallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSearchScoringTest {

    companion object {
        const val MIN_CONFIDENCE = 0.278f
        const val MARGIN = 0.030f

        fun filterMatches(scored: List<Pair<Long, Float>>): List<Pair<Long, Float>> {
            val topScore = scored.firstOrNull()?.second ?: return emptyList()
            if (topScore < MIN_CONFIDENCE) return emptyList()

            val adaptiveThreshold = maxOf(MIN_CONFIDENCE, topScore - MARGIN)
            return scored.filter { it.second >= adaptiveThreshold }
        }

        fun calculateDisplayPercentage(score: Float): Int {
            return (((score - 0.278f) / (0.330f - 0.278f)) * 35f + 65f).coerceIn(65f, 99f).toInt()
        }
    }

    @Test
    fun testRejectsNoiseWhenConceptAbsent() {
        // When searching for an absent concept or random queries in a thousands-photo gallery,
        // background noise reaches ~0.265-0.274. Top score 0.273 must be rejected cleanly.
        val noiseScores = listOf(
            1L to 0.273f,
            2L to 0.265f,
            3L to 0.240f,
            4L to 0.210f
        )
        val result = filterMatches(noiseScores)
        assertTrue("Must return empty list when top score is below 0.278 noise floor", result.isEmpty())
    }

    @Test
    fun testRetainsHighConfidenceMatchesWithinMargin() {
        // When a real concept exists, e.g. topScore = 0.312 ("red car"), adaptiveThreshold = maxOf(0.278, 0.312 - 0.030) = 0.282
        val matches = listOf(
            1L to 0.312f,
            2L to 0.295f,
            3L to 0.285f,
            4L to 0.275f, // Below 0.282 threshold -> rejected
            5L to 0.220f  // Background noise -> rejected
        )
        val result = filterMatches(matches)
        assertEquals(3, result.size)
        assertEquals(1L, result[0].first)
        assertEquals(2L, result[1].first)
        assertEquals(3L, result[2].first)
    }

    @Test
    fun testCalibratedDisplayPercentage() {
        // 0.278 threshold starts at 65%
        assertEquals(65, calculateDisplayPercentage(0.278f))
        // 0.300 score maps to ~80%
        assertEquals(79, calculateDisplayPercentage(0.300f))
        // 0.330+ hero score caps at 99%
        assertEquals(99, calculateDisplayPercentage(0.330f))
        assertEquals(99, calculateDisplayPercentage(0.450f))
    }
}
