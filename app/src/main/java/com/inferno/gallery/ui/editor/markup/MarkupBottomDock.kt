package com.inferno.gallery.ui.editor.markup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.IconButton
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
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MarkupBottomDock(
    activeTool: MarkupTool,
    selectedColor: Color,
    strokeWidth: Float,
    onToolSelected: (MarkupTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showWidthSlider by remember { mutableStateOf(false) }

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
            // ── Expandable Stroke Thickness Slider ──
            AnimatedVisibility(
                visible = showWidthSlider && activeTool != MarkupTool.TEXT && activeTool != MarkupTool.ERASER,
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
                        text = "Thickness",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(SpacingTokens.M))
                    Slider(
                        value = strokeWidth,
                        onValueChange = onStrokeWidthChanged,
                        valueRange = 0.005f..0.040f,
                        colors = SliderDefaults.colors(
                            thumbColor = selectedColor,
                            activeTrackColor = selectedColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.weight(1f)
                    )
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

            // ── Color Palette Row (Only shown when drawing or adding text) ──
            if (activeTool != MarkupTool.ERASER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stroke width quick toggle button
                    if (activeTool != MarkupTool.TEXT) {
                        Surface(
                            shape = CircleShape,
                            color = if (showWidthSlider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (showWidthSlider) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .pressScale()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showWidthSlider = !showWidthSlider
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    shape = CircleShape,
                                    color = selectedColor,
                                    modifier = Modifier.size((strokeWidth * 600f).coerceIn(6f, 22f).dp)
                                ) {}
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
