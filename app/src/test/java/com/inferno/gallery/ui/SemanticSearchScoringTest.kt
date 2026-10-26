package com.inferno.gallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSearchScoringTest {

    companion object {
        const val MIN_CONFIDENCE = 0.235f
        const val MARGIN = 0.055f

        fun filterMatches(scored: List<Pair<Long, Float>>): List<Pair<Long, Float>> {
            val topScore = scored.firstOrNull()?.second ?: return emptyList()
            if (topScore < MIN_CONFIDENCE) return emptyList()

            val adaptiveThreshold = maxOf(MIN_CONFIDENCE, topScore - MARGIN)
            return scored.filter { it.second >= adaptiveThreshold }
        }

        fun calculateDisplayPercentage(score: Float): Int {
            return (((score - 0.235f) / (0.330f - 0.235f)) * 35f + 60f).coerceIn(60f, 99f).toInt()
        }
    }

    @Test
    fun testRejectsNoiseWhenConceptAbsent() {
        // When searching for an absent concept in a gallery,
        // background noise sits around ~0.15-0.22. Top score 0.225 must be rejected cleanly.
        val noiseScores = listOf(
            1L to 0.225f,
            2L to 0.210f,
            3L to 0.190f,
            4L to 0.170f
        )
        val result = filterMatches(noiseScores)
        assertTrue("Must return empty list when top score is below 0.235 noise floor", result.isEmpty())
    }

    @Test
    fun testRetainsHighConfidenceMatchesWithinMargin() {
        // When a real concept exists, e.g. topScore = 0.295 ("dog"), adaptiveThreshold = maxOf(0.235, 0.295 - 0.055) = 0.240
        val matches = listOf(
            1L to 0.295f,
            2L to 0.275f,
            3L to 0.255f,
            4L to 0.242f,
            5L to 0.220f  // Below 0.240 threshold -> rejected
        )
        val result = filterMatches(matches)
        assertEquals(4, result.size)
        assertEquals(1L, result[0].first)
        assertEquals(2L, result[1].first)
        assertEquals(3L, result[2].first)
        assertEquals(4L, result[3].first)
    }

    @Test
    fun testCalibratedDisplayPercentage() {
        // 0.235 threshold starts at 60%
        assertEquals(60, calculateDisplayPercentage(0.235f))
        // 0.278 score maps to ~75%
        assertEquals(75, calculateDisplayPercentage(0.278f))
        // 0.330+ hero score maps to 95%+
        assertEquals(95, calculateDisplayPercentage(0.330f))
        assertEquals(99, calculateDisplayPercentage(0.350f))
    }
}
