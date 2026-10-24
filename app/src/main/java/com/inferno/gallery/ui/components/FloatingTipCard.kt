@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.SpacingTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive one-time floating feature discovery tip card.
 *
 * Appears with a spring bounce after a short delay and provides a high-contrast
 * "Got it" action that smoothly dismisses the card and persists the dismissal.
 */
@Composable
fun FloatingTipCard(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Quick Tip",
    @DrawableRes iconResId: Int = R.drawable.ic_ms_auto_fix_high,
    delayMillis: Long = 400L
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            delay(delayMillis)
        }
        isVisible = true
    }

    val dismissAction: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        isVisible = false
        coroutineScope.launch {
            delay(250)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(MotionTokens.bouncySpring(), initialScale = 0.85f) + fadeIn(MotionTokens.fastEffectsSpec()),
        exit = scaleOut(MotionTokens.snappySpring(), targetScale = 0.85f) + fadeOut(MotionTokens.fastEffectsSpec()),
        modifier = modifier
    ) {
        Surface(
            shape = ShapeLargeIncreased,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.M)
        ) {
            Row(
                modifier = Modifier.padding(SpacingTokens.M),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SpacingTokens.M)
            ) {
                // Feature Icon Pill
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(ShapeFull)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeTokens.M),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Title and explanatory message
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action: "Got it" & Close icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    FilledTonalButton(
                        onClick = dismissAction,
                        shape = ShapeFull,
                        contentPadding = PaddingValues(horizontal = SpacingTokens.M, vertical = SpacingTokens.XS),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Got it",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = dismissAction,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(IconSizeTokens.S),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
