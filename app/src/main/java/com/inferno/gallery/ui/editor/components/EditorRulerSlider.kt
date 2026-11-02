package com.inferno.gallery.ui.editor.components

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Signature horizontal capsule ruler wheel slider.
 *
 * Encapsulated in a dark, rounded capsule container with:
 * - Left: Tool icon or value text in accent/peach color.
 * - Center: Horizontal tick-mark ruler canvas with a primary vertical needle indicator,
 *   drag gesture tracking, edge fading vignette, and tactile haptic feedback.
 * - Right: Reset button (curved refresh icon) enabled when value != defaultValue.
 * - Bottom: Prominent Material 3 Expressive pill "Done" button.
 */
@Composable
fun EditorRulerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float> = -100f..100f,
    step: Float = 1f,
    defaultValue: Float = 0f,
    @DrawableRes iconRes: Int? = null,
    title: String? = null,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    onReset: () -> Unit = { onValueChange(defaultValue) },
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(range)
    val currentStep by rememberUpdatedState(step)
    val currentDefaultValue by rememberUpdatedState(defaultValue)
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnDone by rememberUpdatedState(onDone)

    val view = LocalView.current
    val density = LocalDensity.current
    val isNonDefault = abs(value - defaultValue) > (step * 0.4f)
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest

    // Tick spacing in pixels
    val tickSpacingPx = with(density) { 10.dp.toPx() }
    val pxPerUnit = if (step > 0f) tickSpacingPx / step else tickSpacingPx

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Optional Title (e.g. "Vignette", "Playa", "Straighten")
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Horizontal Ruler Capsule Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp),
            shape = CircleShape,
            color = containerColor,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Slot: Tool icon or formatted value
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconRes != null && !isNonDefault) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = valueFormatter(value),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isNonDefault) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                // Center Slot: Horizontal Interactive Ruler Canvas
                var rawValue by remember { mutableFloatStateOf(value) }
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val primaryColor = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    rawValue = currentValue
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val activeStep = currentStep
                                    val activePx = if (activeStep > 0f) tickSpacingPx / activeStep else tickSpacingPx
                                    val deltaUnits = EditorRulerSliderMath.deltaToValue(
                                        dragDeltaPx = dragAmount.x,
                                        pxPerUnit = activePx,
                                        inverted = true
                                    )
                                    val oldStepped = EditorRulerSliderMath.quantize(rawValue, activeStep)
                                    val candidate = rawValue + deltaUnits
                                    val clamped = EditorRulerSliderMath.clamp(candidate, currentRange)
                                    rawValue = clamped

                                    val snapped = EditorRulerSliderMath.snapToDefault(
                                        value = clamped,
                                        defaultValue = currentDefaultValue,
                                        snapThreshold = activeStep * 0.4f
                                    )
                                    val newStepped = EditorRulerSliderMath.quantize(snapped, activeStep)

                                    if (newStepped != oldStepped) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        currentOnValueChange(newStepped)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        // Determine visible tick values
                        val halfWidth = size.width / 2f
                        val maxDeltaUnits = if (pxPerUnit > 0f) (halfWidth / pxPerUnit) + (step * 2) else 10f
                        val minVisibleVal = (value - maxDeltaUnits).coerceAtLeast(range.start)
                        val maxVisibleVal = (value + maxDeltaUnits).coerceAtMost(range.endInclusive)

                        val firstTickIndex = ceil(minVisibleVal / step).toLong()
                        val lastTickIndex = floor(maxVisibleVal / step).toLong()

                        // Draw ticks
                        for (i in firstTickIndex..lastTickIndex) {
                            val tickVal = (i * step).toFloat()
                            val tickX = centerX + (tickVal - value) * pxPerUnit

                            if (tickX in -10f..(size.width + 10f)) {
                                val isMajor = EditorRulerSliderMath.isMajorTick(tickVal, majorInterval = 5f * step)
                                val isZero = abs(tickVal - defaultValue) < 0.001f

                                val tickHalfHeight = when {
                                    isMajor -> 11.dp.toPx()
                                    else -> 5.dp.toPx()
                                }
                                val strokeW = when {
                                    isMajor -> 2.dp.toPx()
                                    else -> 1.25.dp.toPx()
                                }
                                val tickColor = when {
                                    isZero -> primaryColor.copy(alpha = 0.8f)
                                    isMajor -> onSurfaceVariant.copy(alpha = 0.7f)
                                    else -> onSurfaceVariant.copy(alpha = 0.35f)
                                }

                                drawLine(
                                    color = tickColor,
                                    start = Offset(tickX, centerY - tickHalfHeight),
                                    end = Offset(tickX, centerY + tickHalfHeight),
                                    strokeWidth = strokeW,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Center needle indicator
                        val needleHalfHeight = 16.dp.toPx()
                        drawLine(
                            color = primaryColor,
                            start = Offset(centerX, centerY - needleHalfHeight),
                            end = Offset(centerX, centerY + needleHalfHeight),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Vignette Edge Fade Left
                        val fadeWidth = 28.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(containerColor, Color.Transparent),
                                startX = 0f,
                                endX = fadeWidth
                            ),
                            topLeft = Offset.Zero,
                            size = Size(fadeWidth, size.height)
                        )

                        // Vignette Edge Fade Right
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, containerColor),
                                startX = size.width - fadeWidth,
                                endX = size.width
                            ),
                            topLeft = Offset(size.width - fadeWidth, 0f),
                            size = Size(fadeWidth, size.height)
                        )
                    }
                }

                // Right Slot: Reset Button
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = currentOnReset,
                        enabled = isNonDefault,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_refresh),
                            contentDescription = "Reset value to default",
                            tint = if (isNonDefault) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Pill: "Done" Action Button
        FilledTonalButton(
            onClick = currentOnDone,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

