package com.inferno.gallery.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalView

/**
 * Photon Gallery haptic feedback utilities.
 *
 * Provides crash-proof, multi-tiered tactile feedback across all Android
 * devices (API 31+ down to legacy) with robust fail-soft guarantees.
 */

// ── Premium Haptic Manager ────────────────────────────────────────

object PremiumHapticsManager {
    @Volatile
    var enabled: Boolean = true
    @Volatile
    var strength: Float = 0.5f // 0.0f to 1.0f
}

// ── View-level haptic helpers ───────────────────────────────────────

/** Light tick — perceptible click for regular taps, toggles, small actions. */
fun View.tick() {
    if (!PremiumHapticsManager.enabled) return
    try {
        // Tier 1: Modern Android 12+ (API 31+) VibratorManager with rich composition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    val strength = PremiumHapticsManager.strength.coerceIn(0.1f, 1.0f)
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()

                    if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                        val effect = VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, strength)
                            .compose()
                        safeVibrate(vibrator, effect, attrs)
                        return
                    } else if (vibrator.hasAmplitudeControl()) {
                        val amplitude = (strength * 255).toInt().coerceIn(1, 255)
                        val effect = VibrationEffect.createOneShot(12, amplitude)
                        safeVibrate(vibrator, effect, attrs)
                        return
                    }
                }
            } catch (_: Throwable) {
                // Ignore vendor VibratorManager HAL errors; proceed to Tier 2
            }
        }

        // Tier 2: Standard View.performHapticFeedback (safe on all OEM frameworks)
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            val performed = performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, flags)
            if (performed) return

            // If CLOCK_TICK is unmapped on this OEM, fallback to KEYBOARD_TAP or VIRTUAL_KEY
            if (performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, flags)) return
            if (performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, flags)) return
        } catch (_: Throwable) {
            // View may be detached or unsupported; proceed to Tier 3
        }

        // Tier 3: Classic Vibrator Service fallback
        try {
            @Suppress("DEPRECATION")
            val legacyVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (legacyVibrator != null && legacyVibrator.hasVibrator()) {
                val strength = PremiumHapticsManager.strength.coerceIn(0.1f, 1.0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = if (legacyVibrator.hasAmplitudeControl()) {
                        (strength * 255).toInt().coerceIn(1, 255)
                    } else {
                        VibrationEffect.DEFAULT_AMPLITUDE
                    }
                    legacyVibrator.vibrate(VibrationEffect.createOneShot(12, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    legacyVibrator.vibrate(12)
                }
            }
        } catch (_: Throwable) {
            // Complete fail-soft
        }
    } catch (_: Throwable) {
        // Guaranteed crash-proof: never let haptic issues crash the application
    }
}

private fun safeVibrate(vibrator: Vibrator, effect: VibrationEffect, attrs: VibrationAttributes) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(effect, attrs)
    } else {
        vibrator.vibrate(effect)
    }
}

/** Firm thud — strong feedback for long-press, destructive actions. */
fun View.thud() {
    if (!PremiumHapticsManager.enabled) return
    try {
        // Tier 1: Modern Android 12+ (API 31+) VibratorManager with rich composition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    val strength = PremiumHapticsManager.strength.coerceIn(0.1f, 1.0f)
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_TOUCH)
                        .build()

                    if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
                        val effect = VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, strength)
                            .compose()
                        safeVibrate(vibrator, effect, attrs)
                        return
                    } else if (vibrator.hasAmplitudeControl()) {
                        val amplitude = (strength * 255).toInt().coerceIn(1, 255)
                        val effect = VibrationEffect.createOneShot(35, amplitude)
                        safeVibrate(vibrator, effect, attrs)
                        return
                    }
                }
            } catch (_: Throwable) {
                // Ignore vendor VibratorManager HAL errors; proceed to Tier 2
            }
        }

        // Tier 2: Standard View.performHapticFeedback
        try {
            val flags = HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            val performed = performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, flags)
            if (performed) return
            if (performHapticFeedback(HapticFeedbackConstants.CONFIRM, flags)) return
        } catch (_: Throwable) {
            // View may be detached or unsupported; proceed to Tier 3
        }

        // Tier 3: Classic Vibrator Service fallback
        try {
            @Suppress("DEPRECATION")
            val legacyVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (legacyVibrator != null && legacyVibrator.hasVibrator()) {
                val strength = PremiumHapticsManager.strength.coerceIn(0.1f, 1.0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = if (legacyVibrator.hasAmplitudeControl()) {
                        (strength * 255).toInt().coerceIn(1, 255)
                    } else {
                        VibrationEffect.DEFAULT_AMPLITUDE
                    }
                    legacyVibrator.vibrate(VibrationEffect.createOneShot(35, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    legacyVibrator.vibrate(35)
                }
            }
        } catch (_: Throwable) {
            // Complete fail-soft
        }
    } catch (_: Throwable) {
        // Guaranteed crash-proof: never let haptic issues crash the application
    }
}

// ── Compose-level wrappers (crash-proof) ──

/**
 * Light tick via Compose [HapticFeedback].
 */
fun HapticFeedback.tick() {
    if (!PremiumHapticsManager.enabled) return
    try {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    } catch (_: Throwable) {
        // Complete fail-soft
    }
}

/** Firm thud via Compose [HapticFeedback]. */
fun HapticFeedback.thud() {
    if (!PremiumHapticsManager.enabled) return
    try {
        performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (_: Throwable) {
        // Complete fail-soft
    }
}

/** Generic safe perform for any HapticFeedbackType */
fun HapticFeedback.safePerform(type: HapticFeedbackType) {
    if (!PremiumHapticsManager.enabled) return
    try {
        performHapticFeedback(type)
    } catch (_: Throwable) {
        // Complete fail-soft
    }
}

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
