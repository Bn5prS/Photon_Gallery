package com.inferno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.googleSansFlex
import com.inferno.gallery.ui.utils.pressScale
import com.inferno.gallery.ui.utils.tick

/**
 * A variable-font tab that morphs its width ('wdth') and weight ('wght') axes
 * dynamically inside an expressive tonal pill container.
 */
@Composable
fun MorphingVariableTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 20.sp
) {
    val haptic = LocalHapticFeedback.current

    // Animate pill container background with responsive spring
    val animatedPillColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "MorphingTabPillColor"
    )

    // Fluid, organic width axis morph: 105f (expanded/readable) <-> 48f (sleek condensed)
    // Damping ratio 0.76f with StiffnessMediumLow provides a fluid, refined settle without sluggish wobble
    val animatedWidth by animateFloatAsState(
        targetValue = if (isSelected) 105f else 48f,
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MorphingTabWidth"
    )

    // Snappy weight axis: 600 (bold expressive title) <-> 550 (clean unselected weight)
    val animatedWeight by animateFloatAsState(
        targetValue = if (isSelected) 600f else 550f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MorphingTabWeight"
    )

    // Animate text color
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "MorphingTabTextColor"
    )

    val variableFamily = remember(animatedWidth.toInt(), animatedWeight.toInt()) {
        googleSansFlex(
            weight = animatedWeight.toInt(),
            width = animatedWidth,
            opticalSize = fontSize.value,
            roundness = 0f
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .pressScale(pressedScale = 0.95f, interactionSource = interactionSource)
            .clip(ShapeFull)
            .background(animatedPillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isSelected) {
                        haptic.tick()
                        onClick()
                    }
                }
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = text,
            fontFamily = variableFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            letterSpacing = if (isSelected) 0.sp else (-0.1).sp,
            color = animatedTextColor
        )
    }
}

/**
 * A horizontal segmented pill container displaying fluid width-morphing variable font tabs.
 */
@Composable
fun MorphingVariableTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 15.sp,
    lineHeight: TextUnit = 20.sp
) {
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                MorphingVariableTab(
                    text = title,
                    isSelected = index == selectedTabIndex,
                    onClick = { onTabSelected(index) },
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )
            }
        }
    }
}
