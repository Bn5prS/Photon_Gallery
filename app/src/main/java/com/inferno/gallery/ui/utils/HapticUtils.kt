package com.inferno.gallery.ui.utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView

/**
 * Photon Gallery haptic feedback utilities.
 *
 * Uses Android View-level haptic constants for reliable tactile
 * feedback across devices. Compose's [HapticFeedbackType.TextHandleMove]
 * is imperceptible on many phones, so we bypass it.
 */

// ── Premium Haptic Manager ────────────────────────────────────────

object PremiumHapticsManager {
    var enabled: Boolean = true
    var strength: Float = 0.5f // 0.0f to 1.0f
}

// ── View-level haptic helpers ───────────────────────────────────────

/** Light tick — perceptible click for regular taps, toggles, small actions. */
fun View.tick() {
    if (!PremiumHapticsManager.enabled) return
    val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
    val vibrator = vibratorManager?.defaultVibrator
    
    if (vibrator != null && vibrator.hasVibrator()) {
        val attrs = android.os.VibrationAttributes.Builder().setUsage(android.os.VibrationAttributes.USAGE_TOUCH).build()
        if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_TICK)) {
            vibrator.vibrate(
                android.os.VibrationEffect.startComposition()
                    .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, PremiumHapticsManager.strength)
                    .compose(),
                attrs
            )
            return
        } else if (vibrator.hasAmplitudeControl()) {
            val amplitude = (PremiumHapticsManager.strength * 255).toInt().coerceIn(1, 255)
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(15, amplitude), attrs)
            return
        }
    }
    // Fallback if no advanced vibrator features
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/** Firm thud — strong feedback for long-press, destructive actions. */
fun View.thud() {
    if (!PremiumHapticsManager.enabled) return
    val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
    val vibrator = vibratorManager?.defaultVibrator
    
    if (vibrator != null && vibrator.hasVibrator()) {
        val attrs = android.os.VibrationAttributes.Builder().setUsage(android.os.VibrationAttributes.USAGE_TOUCH).build()
        if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_THUD)) {
            vibrator.vibrate(
                android.os.VibrationEffect.startComposition()
                    .addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, PremiumHapticsManager.strength)
                    .compose(),
                attrs
            )
            return
        } else if (vibrator.hasAmplitudeControl()) {
            val amplitude = (PremiumHapticsManager.strength * 255).toInt().coerceIn(1, 255)
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(40, amplitude), attrs)
            return
        }
    }
    // Fallback if no advanced vibrator features
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

// ── Compose-level wrappers (for use where View isn't readily available) ──

/**
 * Light tick via Compose [HapticFeedback].
 * NOTE: Compose path cannot fully replicate CLOCK_TICK — TextHandleMove is
 * the lightest available type. For true differentiation use [View.tick()] via
 * [haptickClickable] / [haptickCombinedClickable] which call the View system.
 */
fun HapticFeedback.tick() =
    performHapticFeedback(HapticFeedbackType.TextHandleMove)

/** Firm thud via Compose [HapticFeedback]. */
fun HapticFeedback.thud() =
    performHapticFeedback(HapticFeedbackType.LongPress)

// ── Haptic click modifiers ──────────────────────────────────────────

/**
 * Drop-in replacement for [Modifier.clickable] that triggers a light
 * haptic tick on every tap via the View system.
 */
fun Modifier.haptickClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.clickable(enabled = enabled) {
        view.tick()
        onClick()
    }
}

/**
 * Drop-in replacement for [Modifier.combinedClickable] with haptic
 * feedback on both tap (light tick) and long-press (firm thud).
 */
fun Modifier.haptickCombinedClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this.combinedClickable(
        enabled = enabled,
        onLongClick = if (onLongClick != null) {
            {
                view.thud()
                onLongClick()
            }
        } else null,
        onClick = {
            view.tick()
            onClick()
        }
    )
}

