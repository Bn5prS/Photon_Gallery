package com.inferno.gallery.ui.editor.studios

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.editor.components.EditorRulerSlider
import com.inferno.gallery.ui.editor.model.AdjustTool
import com.inferno.gallery.ui.editor.model.TuneValues
import kotlin.math.abs

/**
 * Adjust Studio for 13-adjustment workflow.
 *
 * Mode 1 (Idle): Horizontal carousel of 13 squircle buttons with tool labels on top,
 * squircle icons, and active dot indicators for modified parameters.
 *
 * Mode 2 (Scrubbing): Capsule ruler slider with active tool icon, center needle,
 * neighbor tool navigation header, haptic feedback, reset, and Done button.
 */
@Composable
fun AdjustStudio(
    tuneValues: TuneValues,
    onTuneValuesChange: (TuneValues) -> Unit,
    modifier: Modifier = Modifier,
    initialTool: AdjustTool? = null,
    onDone: (() -> Unit)? = null
) {
    var activeTool by rememberSaveable { mutableStateOf(initialTool) }
    val tools = remember { AdjustTool.entries }
    val listState = rememberLazyListState()
    val view = LocalView.current

    // Scroll selected tool into view when returning to carousel
    LaunchedEffect(activeTool) {
        if (activeTool == null && initialTool != null) {
            val index = tools.indexOf(initialTool).coerceAtLeast(0)
            listState.animateScrollToItem(index)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = activeTool,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 4 })
                } else if (targetState == null && initialState != null) {
                    (fadeIn() + slideInHorizontally { -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { it / 4 })
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "AdjustStudioModeTransition"
        ) { currentTool ->
            if (currentTool == null) {
                // Carousel View: 13 squircle buttons
                AdjustToolsCarousel(
                    tools = tools,
                    tuneValues = tuneValues,
                    onSelectTool = { tool ->
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        activeTool = tool
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Scrubbing View: Active tool with neighbor navigation and ruler slider
                AdjustToolScrubbingView(
                    activeTool = currentTool,
                    tools = tools,
                    tuneValues = tuneValues,
                    onTuneValuesChange = onTuneValuesChange,
                    onSelectTool = { newTool ->
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        activeTool = newTool
                    },
                    onDone = {
                        activeTool = null
                        onDone?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Horizontal carousel displaying all 13 adjustment tools.
 */
@Composable
private fun AdjustToolsCarousel(
    tools: List<AdjustTool>,
    tuneValues: TuneValues,
    onSelectTool: (AdjustTool) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        itemsIndexed(tools, key = { _, tool -> tool.name }) { _, tool ->
            val isModified = tuneValues.isToolModified(tool)
            AdjustToolSquircleItem(
                tool = tool,
                isModified = isModified,
                onClick = { onSelectTool(tool) }
            )
        }
    }
}

/**
 * Individual squircle tool button for editing tools:
 * - Label on top (e.g. "Brightness")
 * - Rounded squircle container with tool icon
 * - Active dot indicator underneath when modified
 */
@Composable
private fun AdjustToolSquircleItem(
    tool: AdjustTool,
    isModified: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isModified) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val iconColor = if (isModified) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = if (isModified) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .width(66.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label on top
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Rounded squircle container
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            tonalElevation = if (isModified) 4.dp else 1.dp
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = tool.iconRes),
                    contentDescription = tool.label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Active dot indicator
        Box(
            modifier = Modifier
                .height(6.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isModified) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Active scrubbing view displaying:
 * - Neighbor navigation bar (Previous tool, Active tool capsule pill, Next tool)
 * - Swiping horizontally switches between tools
 * - [EditorRulerSlider] with tick marks, center needle, reset, and Done button
 */
@Composable
private fun AdjustToolScrubbingView(
    activeTool: AdjustTool,
    tools: List<AdjustTool>,
    tuneValues: TuneValues,
    onTuneValuesChange: (TuneValues) -> Unit,
    onSelectTool: (AdjustTool) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndex = tools.indexOf(activeTool)
    val prevTool = if (currentIndex > 0) tools[currentIndex - 1] else null
    val nextTool = if (currentIndex < tools.size - 1) tools[currentIndex + 1] else null

    // Horizontal swipe gesture detection on navigation header to switch tools
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 90f

    val draggableState = rememberDraggableState { delta ->
        totalDrag += delta
        if (totalDrag > dragThreshold && prevTool != null) {
            totalDrag = 0f
            onSelectTool(prevTool)
        } else if (totalDrag < -dragThreshold && nextTool != null) {
            totalDrag = 0f
            onSelectTool(nextTool)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Neighbor navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { totalDrag = 0f }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous tool label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (prevTool != null) {
                    TextButton(
                        onClick = { onSelectTool(prevTool) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = prevTool.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Active Tool Pill Capsule
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = activeTool.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Next tool label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (nextTool != null) {
                    TextButton(
                        onClick = { onSelectTool(nextTool) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = nextTool.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Dedicated Capsule Ruler Slider
        val currentValue = tuneValues.getValue(activeTool)
        EditorRulerSlider(
            value = currentValue,
            onValueChange = { newValue ->
                onTuneValuesChange(tuneValues.withValue(activeTool, newValue))
            },
            range = activeTool.range,
            step = 1f,
            defaultValue = activeTool.defaultValue,
            iconRes = activeTool.iconRes,
            title = null,
            onReset = {
                onTuneValuesChange(tuneValues.reset(activeTool))
            },
            onDone = onDone
        )
    }
}
