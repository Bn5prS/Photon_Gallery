@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme

/**
 * Photon Gallery motion facade — a thin wrapper over the official Material 3
 * Expressive motion system.
 *
 * Spring specs delegate to [MotionScheme.expressive()] (the same scheme
 * `MaterialExpressiveTheme` installs in [Theme.kt]); easings and durations are
 * the official M3 tokens from
 * https://m3.material.io/styles/motion/easing-and-duration/tokens.
 *
 * Nothing in this file may introduce a hand-tuned curve, duration, or spring
 * that can't be traced back to that tokens page or the MotionScheme API.
 */
object MotionTokens {

    /** The official expressive motion scheme — same one the theme installs. */
    private val scheme = MotionScheme.expressive()

    // ── Official easing tokens ─────────────────────────────────────────
    // https://m3.material.io/styles/motion/easing-and-duration/tokens

    /** Emphasized `cubic-bezier(0.2, 0, 0, 1)` — begin and end on screen. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized decelerate `cubic-bezier(0.05, 0.7, 0.1, 1)` — enter the screen. */
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized accelerate `cubic-bezier(0.3, 0, 0.8, 0.15)` — exit the screen. */
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard `cubic-bezier(0.2, 0, 0, 1)` — utility, begin and end on screen. */
    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // ── Spring specs — delegate to the official MotionScheme ───────────

    /**
     * Playful press feedback, selection, icon reactions — the official
     * expressive scheme's fast spatial spec (bouncy spring).
     */
    fun <T> bouncySpring(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()

    /**
     * UI chrome and crisp transitions — the official expressive scheme's
     * default spatial spec.
     */
    fun <T> snappySpring(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()

    /**
     * Content-area changes, modal sheets, container expansion — the official
     * expressive scheme's slow spatial spec.
     */
    fun <T> gentleSpring(): FiniteAnimationSpec<T> = scheme.slowSpatialSpec()

    /**
     * Shared-element/container transforms. M3 defines no dedicated token for
     * shared bounds; springs are the official mechanism, and zero bounce
     * prevents wobbly container morphs per the Compose shared-elements
     * guidance (developer.android.com/develop/ui/compose/animation/shared-elements).
     */
    fun <T> sharedElementSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // ── Official duration tokens ───────────────────────────────────────
    // md.sys.motion.duration: short1..4 = 50/100/150/200ms,
    // medium1..4 = 250/300/350/400ms, long1..4 = 450/500/550/600ms.

    object Durations {
        /** short3 (150ms) — icon reactions, quick toggles, crossfades. */
        const val Short = 150

        /** medium1 (250ms) — chips, button state changes, enter fades. */
        const val Medium = 250

        /** medium3 (350ms) — sheets, navigation panes, modal entries. */
        const val Long = 350

        /** long2 (500ms) — complex container morphs, hero transforms. */
        const val XLong = 500
    }

    /** Crossfades use short3 (150ms) — the fade duration shared-element
     * destinations expect so they don't fight the bounds morph. */
    const val CrossfadeDuration = Durations.Short
}
