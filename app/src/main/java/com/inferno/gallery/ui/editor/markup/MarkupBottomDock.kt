package com.inferno.gallery.ui.editor.markup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarkupBottomDock(
    activeTool: MarkupTool,
    selectedColor: Color,
    penStrokeWidth: Float,
    highlighterStrokeWidth: Float,
    arrowStrokeWidth: Float,
    eraserRadius: Float,
    selectedText: MarkupText? = null,
    onToolSelected: (MarkupTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onPenStrokeWidthChanged: (Float) -> Unit,
    onHighlighterStrokeWidthChanged: (Float) -> Unit,
    onArrowStrokeWidthChanged: (Float) -> Unit,
    onEraserRadiusChanged: (Float) -> Unit,
    onClearAllClick: () -> Unit = {},
    onAddTextClick: () -> Unit = {},
    onRotateTextClick: () -> Unit = {},
    onDeleteTextClick: () -> Unit = {},
    onEditTextClick: () -> Unit = {},
    onToggleBoxClick: () -> Unit = {},
    onTextScaleChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showThicknessSlider by remember { mutableStateOf(false) }
    var showSizeSlider by remember { mutableStateOf(false) }

    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SpacingTokens.S, horizontal = SpacingTokens.S),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
        ) {
            // ── Expandable Stroke Thickness Row for Pen, Highlighter, Arrow ──
            val isDrawingTool = activeTool == MarkupTool.PEN || activeTool == MarkupTool.HIGHLIGHTER || activeTool == MarkupTool.ARROW
            AnimatedVisibility(
                visible = showThicknessSlider && isDrawingTool,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val (currentWidth, onWidthChange, range, presets) = when (activeTool) {
                    MarkupTool.PEN -> Quadruple(
                        penStrokeWidth,
                        onPenStrokeWidthChanged,
                        0.004f..0.030f,
                        listOf("Fine" to 0.006f, "Normal" to 0.010f, "Bold" to 0.018f, "Heavy" to 0.026f)
                    )
                    MarkupTool.HIGHLIGHTER -> Quadruple(
                        highlighterStrokeWidth,
                        onHighlighterStrokeWidthChanged,
                        0.020f..0.080f,
                        listOf("Thin" to 0.025f, "Normal" to 0.040f, "Wide" to 0.060f, "Max" to 0.080f)
                    )
                    MarkupTool.ARROW -> Quadruple(
                        arrowStrokeWidth,
                        onArrowStrokeWidthChanged,
                        0.006f..0.035f,
                        listOf("Fine" to 0.008f, "Normal" to 0.014f, "Bold" to 0.022f, "Heavy" to 0.030f)
                    )
                    else -> Quadruple(penStrokeWidth, onPenStrokeWidthChanged, 0.004f..0.030f, emptyList())
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.S),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (activeTool) {
                                MarkupTool.PEN -> "Pen Thickness"
                                MarkupTool.HIGHLIGHTER -> "Highlighter Width"
                                MarkupTool.ARROW -> "Arrow Thickness"
                                else -> "Thickness"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        // Live stroke sample line
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height((currentWidth * 600f).coerceIn(4f, 22f).dp)
                                .clip(ShapeFull)
                                .background(if (activeTool == MarkupTool.HIGHLIGHTER) selectedColor.copy(alpha = 0.40f) else selectedColor)
                        )
                    }

                    // Presets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for ((name, value) in presets) {
                            val isPresetSelected = kotlin.math.abs(currentWidth - value) < 0.0025f
                            Surface(
                                shape = ShapeFull,
                                color = if (isPresetSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (isPresetSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .pressScale(0.92f)
                                    .clip(ShapeFull)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onWidthChange(value)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Continuous Slider
                    Slider(
                        value = currentWidth,
                        onValueChange = onWidthChange,
                        valueRange = range,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = selectedColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Expandable Text Size Slider (Text Tool) ──
            AnimatedVisibility(
                visible = showSizeSlider && activeTool == MarkupTool.TEXT && selectedText != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size: ${((selectedText?.scale ?: 1.0f) * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(76.dp)
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.S))
                    Slider(
                        value = selectedText?.scale ?: 1.0f,
                        onValueChange = onTextScaleChanged,
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Eraser Dedicated Control Row ──
            if (activeTool == MarkupTool.ERASER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.S),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val eraserPresets = listOf(
                        "Small" to 0.025f,
                        "Medium" to 0.045f,
                        "Large" to 0.080f
                    )
                    for ((label, sizeVal) in eraserPresets) {
                        val isSelected = kotlin.math.abs(eraserRadius - sizeVal) < 0.008f
                        Surface(
                            shape = ShapeFull,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .pressScale(0.92f)
                                .clip(ShapeFull)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEraserRadiusChanged(sizeVal)
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Clear All action button
                    MarkupActionChip(
                        label = "Clear All",
                        iconRes = R.drawable.ic_ms_delete,
                        isSelected = false,
                        isDestructive = true,
                        onClick = onClearAllClick
                    )
                }
            }

            // ── Contextual Text Actions Row (When in TEXT mode) ──
            if (activeTool == MarkupTool.TEXT) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        MarkupActionChip(
                            label = "Add",
                            iconRes = R.drawable.ic_ms_add,
                            isSelected = false,
                            onClick = onAddTextClick
                        )
                    }

                    if (selectedText != null) {
                        item {
                            MarkupActionChip(
                                label = "Size",
                                iconRes = R.drawable.ic_ms_open_in_full,
                                isSelected = showSizeSlider,
                                onClick = { showSizeSlider = !showSizeSlider }
                            )
                        }

                        item {
                            MarkupActionChip(
                                label = "+45°",
                                iconRes = R.drawable.ic_ms_rotate_right,
                                isSelected = false,
                                onClick = onRotateTextClick
                            )
                        }

                        item {
                            MarkupActionChip(
                                label = if (selectedText.hasBackgroundBox) "Box On" else "Box Off",
                                iconRes = R.drawable.ic_ms_border_all,
                                isSelected = selectedText.hasBackgroundBox,
                                onClick = onToggleBoxClick
                            )
                        }

                        item {
                            MarkupActionChip(
                                label = "Edit",
                                iconRes = R.drawable.ic_ms_edit,
                                isSelected = false,
                                onClick = onEditTextClick
                            )
                        }

                        item {
                            MarkupActionChip(
                                label = "Delete",
                                iconRes = R.drawable.ic_ms_delete,
                                isSelected = false,
                                isDestructive = true,
                                onClick = onDeleteTextClick
                            )
                        }
                    }
                }
            }

            // ── Primary Tool Mode Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MarkupToolItem(
                    label = "Pen",
                    iconRes = R.drawable.ic_ms_edit,
                    isSelected = activeTool == MarkupTool.PEN,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(MarkupTool.PEN)
                    }
                )

                MarkupToolItem(
                    label = "Highlighter",
                    iconRes = R.drawable.ic_ms_auto_fix_high,
                    isSelected = activeTool == MarkupTool.HIGHLIGHTER,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(MarkupTool.HIGHLIGHTER)
                    }
                )

                MarkupToolItem(
                    label = "Arrow",
                    iconRes = R.drawable.ic_ms_north_west,
                    isSelected = activeTool == MarkupTool.ARROW,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(MarkupTool.ARROW)
                    }
                )

                MarkupToolItem(
                    label = "Eraser",
                    iconRes = R.drawable.ic_ms_cleaning_services,
                    isSelected = activeTool == MarkupTool.ERASER,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(MarkupTool.ERASER)
                    }
                )

                MarkupToolItem(
                    label = "Text",
                    iconRes = R.drawable.ic_ms_text_fields,
                    isSelected = activeTool == MarkupTool.TEXT,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(MarkupTool.TEXT)
                    }
                )
            }

            // ── Color Palette Row (Only shown when drawing or adding/editing text) ──
            if (activeTool != MarkupTool.ERASER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick toggle button for thickness/size
                    if (isDrawingTool) {
                        val activeThickness = when (activeTool) {
                            MarkupTool.HIGHLIGHTER -> highlighterStrokeWidth
                            MarkupTool.ARROW -> arrowStrokeWidth
                            else -> penStrokeWidth
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (showThicknessSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (showThicknessSlider) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .pressScale()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showThicknessSlider = !showThicknessSlider
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    shape = CircleShape,
                                    color = selectedColor,
                                    modifier = Modifier.size((activeThickness * 600f).coerceIn(6f, 22f).dp)
                                ) {}
                            }
                        }
                        Spacer(modifier = Modifier.width(SpacingTokens.S))
                    } else if (activeTool == MarkupTool.TEXT && selectedText != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (showSizeSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (showSizeSlider) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .pressScale()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSizeSlider = !showSizeSlider
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_full),
                                    contentDescription = "Size",
                                    modifier = Modifier.size(IconSizeTokens.S)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(SpacingTokens.S))
                    }

                    // Color dots
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(MarkupPalette.COLORS) { color ->
                            val isSelected = color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .pressScale()
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onColorSelected(color)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val checkTint = if (color == Color.White || color == Color(0xFFFFD600)) Color.Black else Color.White
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                        contentDescription = null,
                                        tint = checkTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun MarkupActionChip(
    label: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    isSelected: Boolean = false,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val containerColor = when {
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = when {
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = ShapeFull,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .pressScale(0.92f)
            .clip(ShapeFull)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SpacingTokens.M, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.XS)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(IconSizeTokens.S)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MarkupToolItem(
    label: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeFull,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .pressScale(0.92f)
            .clip(ShapeFull)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(IconSizeTokens.M)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
