package com.inferno.gallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.getSegmentShape

object LayeredToolPillDefaults {
    val Height: Dp = 64.dp
    val Padding: Dp = 6.dp
    val SegmentGap: Dp = 6.dp
    val OuterCorner: Dp = 26.dp
    val InnerCorner: Dp = 6.dp
    val IconSize: Dp = 22.dp

    fun segmentShape(index: Int, total: Int): Shape =
        getSegmentShape(index = index, total = total, outerCorner = OuterCorner, innerCorner = InnerCorner)
}

/**
 * Shared concentric layered capsule dock (matching the fullscreen viewer pill):
 * stadium outer [CircleShape] shell with [surfaceContainer] fill and inner connected tiles.
 */
@Composable
fun LayeredToolPill(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    shadowElevation: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = shadowElevation,
        modifier = modifier.height(LayeredToolPillDefaults.Height)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(LayeredToolPillDefaults.Padding),
            horizontalArrangement = Arrangement.spacedBy(LayeredToolPillDefaults.SegmentGap),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Connected segment tile for [LayeredToolPill] with independent bouncy press scale
 * and bounded ripple feedback. Supports icon-only or icon + concise label modes.
 */
@Composable
fun LayeredToolTile(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconModifier: Modifier = Modifier,
    tileShape: Shape = RoundedCornerShape(LayeredToolPillDefaults.InnerCorner),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "layeredToolTileScale"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(tileShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = tint.copy(alpha = 0.15f)),
                onClick = onClick
            ),
        shape = tileShape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
        border = null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val resolvedTint = if (enabled) tint else tint.copy(alpha = 0.38f)
            if (label != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = resolvedTint,
                        modifier = iconModifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = resolvedTint,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = resolvedTint,
                    modifier = iconModifier.size(LayeredToolPillDefaults.IconSize)
                )
            }
        }
    }
}
