package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ExpressiveShapes
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.PresetThemePalette

/**
 * Renders a scalloped 4-quadrant color swatch representing a Material 3 palette preset.
 *
 * Each quadrant displays one of the 4 key tones:
 * - Top-Left: Primary tone
 * - Top-Right: Secondary tone
 * - Bottom-Left: Tertiary tone
 * - Bottom-Right: Container / Deep Anchor tone
 *
 * When selected, displays an outer animated primary accent ring and a crisp centered checkmark.
 */
@Composable
fun FourQuadrantColorSwatch(
    palette: PresetThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val quadrantColors = palette.quadrantColors

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = MotionTokens.snappySpring(),
        label = "swatch_scale"
    )

    Column(
        modifier = modifier
            .width(72.dp)
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
                this.contentDescription = "${palette.name} color palette"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Outer selection boundary box
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            // Animated outer selection ring
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = ExpressiveShapes.Scallop
                        )
                )
            }

            // 4-Quadrant scalloped canvas
            Box(
                modifier = Modifier
                    .size(if (isSelected) 46.dp else 48.dp)
                    .clip(ExpressiveShapes.Scallop),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(if (isSelected) 46.dp else 48.dp)) {
                    val halfWidth = size.width / 2f
                    val halfHeight = size.height / 2f
                    val quadrantSize = Size(halfWidth, halfHeight)

                    // Top-Left quadrant
                    drawRect(
                        color = quadrantColors[0],
                        topLeft = Offset.Zero,
                        size = quadrantSize
                    )
                    // Top-Right quadrant
                    drawRect(
                        color = quadrantColors[1],
                        topLeft = Offset(halfWidth, 0f),
                        size = quadrantSize
                    )
                    // Bottom-Left quadrant
                    drawRect(
                        color = quadrantColors[2],
                        topLeft = Offset(0f, halfHeight),
                        size = quadrantSize
                    )
                    // Bottom-Right quadrant
                    drawRect(
                        color = quadrantColors[3],
                        topLeft = Offset(halfWidth, halfHeight),
                        size = quadrantSize
                    )
                }

                // Centered check badge when selected
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(MotionTokens.fastEffectsSpec()) + scaleIn(MotionTokens.bouncySpring()),
                    exit = fadeOut(MotionTokens.fastEffectsSpec()) + scaleOut(MotionTokens.bouncySpring())
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = ExpressiveShapes.Scallop,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // Palette Name Label
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
