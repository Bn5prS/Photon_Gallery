package com.inferno.gallery.data.ai

import kotlin.math.roundToInt

/**
 * Distribution-aware scoring and filtering for MobileCLIP2-S2 cosine similarities.
 *
 * Raw CLIP cosine similarities shift noticeably across queries (broad concepts such as "person"
 * have higher background medians than niche queries like "yellow bicycle"). Anchoring only on
 * `topScore - gap` drops legitimate matches when a single hero photo scores very high, while a
 * flat `0.145f` floor leaks up to 200 unrelated photos when the queried concept is absent.
 *
 * This scorer combines:
 * 1. An absolute floor ([MIN_ABSOLUTE_TOP_SCORE], [MIN_ITEM_FLOOR]).
 * 2. Lower-half median/quartile baseline estimation to reject flat noise clusters on absent
 *    concepts while staying safe when a common concept appears in a large share of the gallery.
 * 3. A clamped 3rd-order rank anchor to avoid collapsing the result set around one outlier.
 */
object SemanticSearchScorer {

    const val MIN_ABSOLUTE_TOP_SCORE = 0.162f
    const val STRONG_ABSOLUTE_SCORE = 0.205f
    const val MIN_ITEM_FLOOR = 0.158f
    const val MAX_RESULTS = 200

    data class ScoredMatch(
        val mediaId: Long,
        val score: Float,
        val confidencePercent: Int
    )

    data class ScoringResult(
        val matches: List<ScoredMatch>,
        val topScore: Float,
        val threshold: Float,
        val baselineMedian: Float
    )

    fun filterMatches(
        scored: List<Pair<Long, Float>>,
        query: String,
        maxResults: Int = MAX_RESULTS
    ): ScoringResult {
        if (scored.isEmpty() || maxResults <= 0) {
            return ScoringResult(emptyList(), 0f, MIN_ITEM_FLOOR, 0f)
        }

        val sorted = scored.sortedByDescending { it.second }
        val n = sorted.size
        val topScore = sorted[0].second
        val median = sorted[n / 2].second

        if (topScore < MIN_ABSOLUTE_TOP_SCORE) {
            return ScoringResult(emptyList(), topScore, MIN_ABSOLUTE_TOP_SCORE, median)
        }

        val minItemFloor: Float
        if (n >= 12) {
            val p25Index = ((n * 3) / 4).coerceIn(0, n - 1)
            val p10Index = ((n * 9) / 10).coerceIn(p25Index, n - 1)
            val p25 = sorted[p25Index].second
            val p10 = sorted[p10Index].second
            val tailSpread = (p25 - p10).coerceAtLeast(0.006f)
            val tailSigma = (tailSpread / 0.607f).coerceIn(0.010f, 0.024f)
            val lowerSpread = (median - p25).coerceAtLeast(0.008f)
            // When >50% of photos match a common concept ("person"), median sits inside the
            // positive cluster, so estimate noise sigma from the lower background tail alone.
            val noiseSigma = if (median > 0.175f) {
                tailSigma
            } else {
                minOf(lowerSpread / 0.6745f, tailSigma * 1.4f).coerceIn(0.010f, 0.028f)
            }

            if (topScore < STRONG_ABSOLUTE_SCORE) {
                val requiredLift = maxOf(0.028f, 2.3f * noiseSigma)
                val statisticalGate = maxOf(MIN_ABSOLUTE_TOP_SCORE, median + requiredLift)
                if (topScore < statisticalGate) {
                    return ScoringResult(emptyList(), topScore, statisticalGate, median)
                }
            }

            // Cap effective baseline so ultra-common concepts (e.g. "person") don't raise the cutoff.
            val effectiveBaseline = minOf(median, p25 + 0.015f).coerceAtMost(0.165f)
            minItemFloor = maxOf(MIN_ITEM_FLOOR, effectiveBaseline + maxOf(0.020f, 1.6f * noiseSigma))
        } else {
            val smallGalleryGate = if (n >= 4) 0.172f else 0.165f
            if (topScore < smallGalleryGate) {
                return ScoringResult(emptyList(), topScore, smallGalleryGate, median)
            }
            minItemFloor = MIN_ITEM_FLOOR
        }

        val tokenCount = query.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val adaptiveGap = when {
            tokenCount <= 1 -> 0.042f
            tokenCount == 2 -> 0.032f
            else -> 0.025f
        }
        val maxOutlierDrop = if (tokenCount <= 1) 0.045f else 0.035f
        val anchorIndex = minOf(2, sorted.lastIndex)
        val anchorScore = maxOf(sorted[anchorIndex].second, topScore - maxOutlierDrop)
        val finalThreshold = maxOf(minItemFloor, anchorScore - adaptiveGap)

        val matched = sorted.asSequence()
            .takeWhile { it.second >= finalThreshold }
            .take(maxResults)
            .map { (id, score) ->
                ScoredMatch(
                    mediaId = id,
                    score = score,
                    confidencePercent = calculateDisplayPercentage(
                        score = score,
                        topScore = topScore,
                        floor = finalThreshold
                    )
                )
            }
            .toList()

        return ScoringResult(
            matches = matched,
            topScore = topScore,
            threshold = finalThreshold,
            baselineMedian = median
        )
    }

    fun calculateDisplayPercentage(
        score: Float,
        topScore: Float = 0.275f,
        floor: Float = MIN_ITEM_FLOOR
    ): Int {
        val absNorm = ((score - 0.155f) / (0.275f - 0.155f)).coerceIn(0f, 1f)
        val relDenom = (topScore - floor).coerceAtLeast(0.025f)
        val relNorm = ((score - floor) / relDenom).coerceIn(0f, 1f)
        val combined = 0.55f * absNorm + 0.45f * relNorm
        return (60f + combined * 39f).roundToInt().coerceIn(60, 99)
    }
}
