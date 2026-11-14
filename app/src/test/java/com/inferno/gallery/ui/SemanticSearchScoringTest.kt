package com.inferno.gallery.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSearchScoringTest {

    companion object {
        const val MIN_CONFIDENCE = 0.040f
        const val MARGIN = 0.015f

        fun filterMatches(scored: List<Pair<Long, Float>>): List<Pair<Long, Float>> {
            val topScore = scored.firstOrNull()?.second ?: return emptyList()
            if (topScore < MIN_CONFIDENCE) return emptyList()

            val adaptiveThreshold = maxOf(MIN_CONFIDENCE, topScore - MARGIN)
            return scored.filter { it.second >= adaptiveThreshold }
        }

        fun calculateDisplayPercentage(score: Float): Int {
            return (((score - 0.040f) / (0.100f - 0.040f)) * 35f + 60f).coerceIn(60f, 99f).toInt()
        }
    }

    @Test
    fun testRejectsNoiseWhenConceptAbsent() {
        // When searching for an absent concept in a gallery with MobileCLIP-S2,
        // background noise sits below ~0.038. Top score 0.038 must be rejected cleanly.
        val noiseScores = listOf(
            1L to 0.038f,
            2L to 0.030f,
            3L to 0.020f,
            4L to 0.010f
        )
        val result = filterMatches(noiseScores)
        assertTrue("Must return empty list when top score is below 0.040 noise floor", result.isEmpty())
    }

    @Test
    fun testRetainsHighConfidenceMatchesWithinMargin() {
        // When a real concept exists, e.g. topScore = 0.080 ("dog"), adaptiveThreshold = maxOf(0.040, 0.080 - 0.015) = 0.065
        val matches = listOf(
            1L to 0.080f,
            2L to 0.075f,
            3L to 0.070f,
            4L to 0.066f,
            5L to 0.050f  // Below 0.065 threshold -> rejected
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
        // 0.040 threshold starts at 60%
        assertEquals(60, calculateDisplayPercentage(0.040f))
        // 0.065 score maps to ~74%
        assertEquals(74, calculateDisplayPercentage(0.065f))
        // 0.100+ hero score maps to 95%+
        assertEquals(95, calculateDisplayPercentage(0.100f))
        assertEquals(99, calculateDisplayPercentage(0.120f))
    }
}
