package com.inferno.gallery.ui.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import com.inferno.gallery.ui.theme.MotionTokens

/**
 * Unified press-scale feedback modifier.
 *
 * Applies a spring-animated scale-down on press and optional selection scale.
 * Replaces the inline [collectIsPressedAsState] + [animateFloatAsState] blocks
 * duplicated across Albums, AlbumCover, NavigationDock, and SettingsScreen.
 *
 * @param selectedScale  Scale when [isSelected] is true. Defaults to 0.88f.
 * @param pressedScale   Scale on pointer press. Defaults to 0.95f.
 * @param isSelected     Whether the element is in selected state.
 * @param interactionSource Optional — provide an existing [MutableInteractionSource]
 *   if the composable also wires it to indication/ripple. If null, a new one is
 *   created internally and used only for press detection.
 */
fun Modifier.pressScale(
    selectedScale: Float = 0.88f,
    pressedScale: Float = 0.95f,
    isSelected: Boolean = false,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val targetScale = when {
        isSelected -> selectedScale
        isPressed -> pressedScale
        else -> 1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MotionTokens.bouncySpring(),
        label = "pressScale"
    )

    this.scale(scale)
}
