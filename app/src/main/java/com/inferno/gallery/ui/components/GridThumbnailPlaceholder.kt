package com.inferno.gallery.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.inferno.gallery.ui.GridDesignTokens

/**
 * Spring-driven shimmer placeholder for grid thumbnails.
 *
 * Uses an infiniteRepeatable spring animation (StiffnessVeryLow) to pace the shimmer
 * sweep — no tween(), no easing curves. Fully compliant with the M3 Expressive motion rules.
 *
 * The shape and aspect ratio are sourced from [GridDesignTokens] so any future design
 * token change automatically propagates here.
 */
@Composable
fun GridThumbnailPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_placeholder")

    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = GridDesignTokens.shimmerAnimSpec(),
        label = "shimmerProgress"
    )

    val surfaceLow  = MaterialTheme.colorScheme.surfaceContainerLow
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh

    // Map 0f..1f progress to a left→right sweep across ~800px (covers any screen width).
    val sweepStart = Offset(x = (shimmerProgress - 0.4f) * 1200f, y = 0f)
    val sweepEnd   = Offset(x = (shimmerProgress + 0.4f) * 1200f, y = 0f)

    Box(
        modifier = modifier
            .aspectRatio(GridDesignTokens.AspectRatio)
            .clip(GridDesignTokens.ThumbnailShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(surfaceLow, surfaceHigh, surfaceLow),
                    start  = sweepStart,
                    end    = sweepEnd
                )
            )
    )
}

/**
 * Error state placeholder shown when Coil fails to decode a thumbnail.
 * Matches the shimmer's shape/aspect so the grid layout never shifts.
 */
@Composable
fun GridThumbnailError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(GridDesignTokens.AspectRatio)
            .clip(GridDesignTokens.ThumbnailShape)
            .background(MaterialTheme.colorScheme.errorContainer)
    )
}
