package com.inferno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.utils.tick

/**
 * Separate rectangular filter chip with slightly curved corners (ShapeSmall / 8.dp)
 * adhering strictly to Material 3 surface color hierarchy and non-bouncy spring physics.
 */
@Composable
fun MorphingVariableTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    fontSize: TextUnit? = null,
    lineHeight: TextUnit? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth spring physics with strict NO BOUNCE (DampingRatioNoBouncy)
    val noBounceSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    val noBounceColorSpring = remember {
        spring<Color>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    // Material 3 Surface Color Hierarchy:
    // Selected: secondaryContainer (elevated tonal surface) + onSecondaryContainer
    // Unselected: surfaceContainer (tier 2 resting card surface) + onSurfaceVariant + outlineVariant border
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = noBounceColorSpring,
        label = "ChipContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = noBounceColorSpring,
        label = "ChipContentColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = noBounceColorSpring,
        label = "ChipBorderColor"
    )

    // Press scale interaction with non-bouncy spring
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = noBounceSpring,
        label = "ChipPressScale"
    )

    Surface(
        onClick = {
            if (!isSelected) {
                haptic.tick()
                onClick()
            }
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .minimumInteractiveComponentSize(),
        shape = ShapeSmall, // Rectangular with slightly curved corners (8.dp)
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.let { base ->
                    base.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = fontSize ?: base.fontSize,
                        lineHeight = lineHeight ?: base.lineHeight
                    )
                },
                color = contentColor
            )
        }
    }
}

/**
 * A row of separate rectangular chips (ShapeSmall / 8.dp) for the main gallery screen,
 * replacing the pill-inside-pill design with independent, cleanly spaced chips.
 */
@Composable
fun MorphingVariableTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit? = null,
    lineHeight: TextUnit? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val icon = when (title) {
                "Camera" -> ImageVector.vectorResource(R.drawable.ic_ms_photo_camera)
                "All Photos" -> ImageVector.vectorResource(R.drawable.ic_ms_image)
                else -> null
            }
            MorphingVariableTab(
                text = title,
                isSelected = index == selectedTabIndex,
                onClick = { onTabSelected(index) },
                leadingIcon = icon,
                fontSize = fontSize,
                lineHeight = lineHeight
            )
        }
    }
}
