package com.inferno.gallery.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║         FROZEN DESIGN CONTRACT — GridDesignTokens           ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  DO NOT modify any value without a performance review.      ║
 * ║  All grid composables source their tokens ONLY from here.   ║
 * ║  Violating this contract reintroduces the perf regressions  ║
 * ║  that motivated the June 2026 reimplementation.             ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Decision log (locked 2026-06-01):
 *   - AspectRatio = 1f (square) — avoids StaggeredGrid layout overhead
 *   - ThumbnailShape = 16.dp — matches existing shared element bounds; avoids visual regression
 *   - SelectionScaleTarget = 0.85f — M3 Expressive standard motion press feedback
 *   - Memory cache = 25% — prevents GC storms during fast scroll (was 50%)
 */
object GridDesignTokens {

    // ── Shape ─────────────────────────────────────────────────────────────
    // Locked to RoundedCornerShape(16.dp) — do NOT increase; more complex clip paths
    // degrade GPU rasterization during fast scroll at 120 Hz.
    val ThumbnailShape         = RoundedCornerShape(16.dp)
    val BadgeShape             = RoundedCornerShape(4.dp)
    // SelectionOverlay always tracks the thumbnail shape so clips align during spring animation.
    val SelectionOverlayShape  = ThumbnailShape

    // ── Layout ────────────────────────────────────────────────────────────
    // AspectRatio = 1f (square). Changing to portrait-adaptive requires migrating to
    // LazyVerticalStaggeredGrid, which has higher measure overhead. Review before changing.
    const val AspectRatio             = 1f
    val CellSpacing                   = 4.dp
    val GridHorizontalPadding         = 8.dp

    // ── Spring Motion (M3 Expressive) ─────────────────────────────────────
    // ALL motion uses spring() — tween() is banned by project rules.

    /** Used for animateItem placement in LazyVerticalGrid. */
    fun itemPlacementSpec() = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessLow
    )

    /** Used for the selection scale animation on individual cells. */
    fun selectionScaleSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    /** Used for SharedTransitionScope.sharedElement boundsTransform. */
    fun sharedElementBoundsSpec() = spring<Rect>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessLow
    )

    /** Shimmer sweep animation for the loading placeholder.
     *  Uses keyframes to mimic spring pacing — slow start, peak mid, gentle end.
     *  Note: spring() cannot be used inside infiniteRepeatable (not DurationBasedAnimationSpec). */
    fun shimmerAnimSpec() = infiniteRepeatable<Float>(
        animation  = keyframes {
            durationMillis = 1400
            0.0f at 0
            0.3f at 350   // slow ramp-up
            0.7f at 700   // peak velocity
            1.0f at 1400  // decelerate
        },
        repeatMode = RepeatMode.Restart
    )

    // ── Selection ─────────────────────────────────────────────────────────
    const val SelectionScaleTarget  = 0.85f
    const val SelectionScaleNormal  = 1.0f
    const val SelectionOverlayAlpha = 0.30f

    // ── Video Overlay Gradient ────────────────────────────────────────────
    const val VideoGradientStartAlpha = 0f
    const val VideoGradientEndAlpha   = 0.6f

    // ── Coil Sampling Strategy ────────────────────────────────────────────
    // Always use Precision.INEXACT in grid context — EXACT forces synchronous decode.
    // thumbnailPx is calculated ONCE per GridCellsCount change, not per cell.
    // Memory cache: 25% of heap. Disk cache: 300 MB. Both set in MainActivity.
}
