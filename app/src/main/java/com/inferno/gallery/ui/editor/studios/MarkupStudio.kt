package com.inferno.gallery.ui.editor.studios

import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.markup.MarkupPalette
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.markup.MarkupText
import com.inferno.gallery.ui.editor.markup.MarkupTool
import com.inferno.gallery.ui.editor.markup.RedactMode
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.utils.pressScale
import kotlin.math.roundToInt

/**
 * Markup & Privacy Redact Studio.
 *
 * Implements:
 * 1. Main Markup Carousel:
 *    - Squircle tiles: Pen, Highlighter, Redact (privacy mosaic/pixelation tool), and Text.
 *    - Modified dot indicators when drawings or text overlays exist.
 * 2. Active Drawing Mode (Pen / Highlighter / Redact):
 *    - Header: Tool name and sub-mode toggle (Mosaic vs. Blackout for Redact).
 *    - Stroke width capsule slider: Min dot (left) to Max dot (right) with draggable thumb.
 *    - Color palette row: 8 curated round swatches with selected ring indicator.
 *    - Bottom row: Cancel `X` (discards strokes drawn during session), "Markup" label, Apply `✓` (confirms strokes).
 */
@Composable
fun MarkupStudio(
    markupState: MarkupState,
    onMarkupStateChange: (MarkupState) -> Unit,
    modifier: Modifier = Modifier,
    activeTool: MarkupTool? = null,
    onActiveToolChange: ((MarkupTool?) -> Unit)? = null,
    selectedColor: Color = MarkupPalette.COLORS[0],
    onColorChange: ((Color) -> Unit)? = null,
    penStrokeWidth: Float = MarkupPalette.DEFAULT_PEN_WIDTH,
    onPenStrokeWidthChange: ((Float) -> Unit)? = null,
    highlighterStrokeWidth: Float = MarkupPalette.DEFAULT_HIGHLIGHTER_WIDTH,
    onHighlighterStrokeWidthChange: ((Float) -> Unit)? = null,
    redactStrokeWidth: Float = MarkupPalette.DEFAULT_REDACT_WIDTH,
    onRedactStrokeWidthChange: ((Float) -> Unit)? = null,
    redactMode: RedactMode = RedactMode.MOSAIC,
    onRedactModeChange: ((RedactMode) -> Unit)? = null,
    selectedTextId: Long? = null,
    onSelectTextId: ((Long?) -> Unit)? = null,
    onEditText: ((MarkupText) -> Unit)? = null,
    onOpenTextDialog: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null
) {
    var internalActiveTool by rememberSaveable { mutableStateOf<MarkupTool?>(null) }
    val effectiveActiveTool = activeTool ?: internalActiveTool
    val setTool: (MarkupTool?) -> Unit = { tool ->
        internalActiveTool = tool
        onActiveToolChange?.invoke(tool)
    }

    var internalColor by remember { mutableStateOf(selectedColor) }
    val effectiveColor = if (onColorChange != null) selectedColor else internalColor
    val setColor: (Color) -> Unit = { color ->
        internalColor = color
        onColorChange?.invoke(color)
    }

    var internalPenWidth by remember { mutableFloatStateOf(penStrokeWidth) }
    val effectivePenWidth = if (onPenStrokeWidthChange != null) penStrokeWidth else internalPenWidth
    val setPenWidth: (Float) -> Unit = { width ->
        internalPenWidth = width
        onPenStrokeWidthChange?.invoke(width)
    }

    var internalHighlighterWidth by remember { mutableFloatStateOf(highlighterStrokeWidth) }
    val effectiveHighlighterWidth = if (onHighlighterStrokeWidthChange != null) highlighterStrokeWidth else internalHighlighterWidth
    val setHighlighterWidth: (Float) -> Unit = { width ->
        internalHighlighterWidth = width
        onHighlighterStrokeWidthChange?.invoke(width)
    }

    var internalRedactWidth by remember { mutableFloatStateOf(redactStrokeWidth) }
    val effectiveRedactWidth = if (onRedactStrokeWidthChange != null) redactStrokeWidth else internalRedactWidth
    val setRedactWidth: (Float) -> Unit = { width ->
        internalRedactWidth = width
        onRedactStrokeWidthChange?.invoke(width)
    }

    var internalRedactMode by remember { mutableStateOf(redactMode) }
    val effectiveRedactMode = if (onRedactModeChange != null) redactMode else internalRedactMode
    val setRedactMode: (RedactMode) -> Unit = { mode ->
        internalRedactMode = mode
        onRedactModeChange?.invoke(mode)
    }

    // Keep snapshot of state upon entering active drawing mode so Cancel reverts cleanly
    var entryMarkupState by remember { mutableStateOf(markupState) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = effectiveActiveTool,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 4 })
                } else if (targetState == null && initialState != null) {
                    (fadeIn() + slideInHorizontally { -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { it / 4 })
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "MarkupStudioModeTransition"
        ) { tool ->
            if (tool == null) {
                // 1. Main Markup Carousel View: 4 Squircle buttons
                MarkupCarouselView(
                    markupState = markupState,
                    onSelectTool = { selected ->
                        if (selected == MarkupTool.TEXT) {
                            entryMarkupState = markupState
                            setTool(MarkupTool.TEXT)
                            onOpenTextDialog?.invoke()
                        } else {
                            entryMarkupState = markupState
                            setTool(selected)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (tool == MarkupTool.TEXT) {
                // 2. Active Text Mode View (Move, Scale, Rotate, Add, Edit, Delete)
                ActiveTextModeView(
                    selectedText = markupState.texts.find { it.id == selectedTextId },
                    onAddText = { onOpenTextDialog?.invoke() },
                    onEditText = { textToEdit -> onEditText?.invoke(textToEdit) },
                    onDeleteText = { textToDelete ->
                        onMarkupStateChange(markupState.withRemovedText(textToDelete.id))
                        onSelectTextId?.invoke(null)
                    },
                    onCancel = {
                        onMarkupStateChange(entryMarkupState)
                        setTool(null)
                    },
                    onApply = {
                        setTool(null)
                        onDone?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 3. Active Drawing Mode View (Pen, Highlighter, Redact)
                val (currentWidth, onWidthChange, widthRange) = when (tool) {
                    MarkupTool.HIGHLIGHTER -> Triple(effectiveHighlighterWidth, setHighlighterWidth, 0.020f..0.080f)
                    MarkupTool.REDACT -> Triple(effectiveRedactWidth, setRedactWidth, 0.020f..0.090f)
                    else -> Triple(effectivePenWidth, setPenWidth, 0.004f..0.030f)
                }

                ActiveDrawingModeView(
                    activeTool = tool,
                    currentStrokeWidth = currentWidth,
                    strokeWidthRange = widthRange,
                    onStrokeWidthChange = onWidthChange,
                    selectedColor = effectiveColor,
                    onColorChange = setColor,
                    redactMode = effectiveRedactMode,
                    onRedactModeChange = setRedactMode,
                    onCancel = {
                        // Discard strokes made during this session
                        onMarkupStateChange(entryMarkupState)
                        setTool(null)
                    },
                    onApply = {
                        // Confirm strokes and return
                        setTool(null)
                        onDone?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Main carousel view with Squircle tool tiles: Pen, Highlighter, Redact, and Text.
 */
@Composable
private fun MarkupCarouselView(
    markupState: MarkupState,
    onSelectTool: (MarkupTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val tools = listOf(
        MarkupToolTileData(MarkupTool.PEN, "Pen", R.drawable.ic_ms_edit),
        MarkupToolTileData(MarkupTool.HIGHLIGHTER, "Highlighter", R.drawable.ic_ms_auto_fix_high),
        MarkupToolTileData(MarkupTool.REDACT, "Redact", R.drawable.ic_ms_visibility_off),
        MarkupToolTileData(MarkupTool.TEXT, "Text", R.drawable.ic_ms_text_fields)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (item in tools) {
            val isModified = when (item.tool) {
                MarkupTool.PEN -> markupState.strokes.any { it.tool == MarkupTool.PEN }
                MarkupTool.HIGHLIGHTER -> markupState.strokes.any { it.tool == MarkupTool.HIGHLIGHTER }
                MarkupTool.REDACT -> markupState.strokes.any { it.tool == MarkupTool.REDACT }
                MarkupTool.TEXT -> markupState.texts.isNotEmpty()
                else -> false
            }

            MarkupSquircleItem(
                label = item.label,
                iconRes = item.iconRes,
                isModified = isModified,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onSelectTool(item.tool)
                }
            )
        }
    }
}

private data class MarkupToolTileData(
    val tool: MarkupTool,
    val label: String,
    @param:DrawableRes val iconRes: Int
)

/**
 * Individual squircle button with expressive styling:
 * - Label on top (e.g. "Pen", "Redact")
 * - Rounded squircle container with centered icon
 * - Active dot indicator underneath when modified
 */
@Composable
private fun MarkupSquircleItem(
    label: String,
    @DrawableRes iconRes: Int,
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
            .width(68.dp)
            .pressScale()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            tonalElevation = if (isModified) 4.dp else 1.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

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
 * Active Drawing Mode View:
 * 1. Tool name header + Redact mode toggle (Mosaic vs Blackout).
 * 2. Signature capsule stroke slider (min dot to max dot with draggable thumb).
 * 3. 8-swatch round color palette.
 * 4. Bottom action row: Cancel `X`, "Markup" title, Apply `✓`.
 */
@Composable
private fun ActiveDrawingModeView(
    activeTool: MarkupTool,
    currentStrokeWidth: Float,
    strokeWidthRange: ClosedFloatingPointRange<Float>,
    onStrokeWidthChange: (Float) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    redactMode: RedactMode,
    onRedactModeChange: (RedactMode) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1. Header Row: Active tool name + Redact Mode Toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getMarkupToolLabel(activeTool),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activeTool == MarkupTool.REDACT) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RedactModePill(
                        label = "Mosaic",
                        iconRes = R.drawable.ic_ms_grid_view,
                        isSelected = redactMode == RedactMode.MOSAIC,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onRedactModeChange(RedactMode.MOSAIC)
                        }
                    )
                    RedactModePill(
                        label = stringResource(R.string.editor_redact_blur),
                        iconRes = R.drawable.ic_ms_blur_on,
                        isSelected = redactMode == RedactMode.BLUR,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onRedactModeChange(RedactMode.BLUR)
                        }
                    )
                    RedactModePill(
                        label = stringResource(R.string.editor_redact_blackout),
                        iconRes = R.drawable.ic_ms_visibility_off,
                        isSelected = redactMode == RedactMode.BLACKOUT,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onRedactModeChange(RedactMode.BLACKOUT)
                        }
                    )
                }
            }
        }

        // ── 2. Signature Capsule Stroke Slider ──
        StrokeWidthCapsuleSlider(
            value = currentStrokeWidth,
            onValueChange = onStrokeWidthChange,
            range = strokeWidthRange,
            activeColor = when {
                activeTool == MarkupTool.REDACT && redactMode == RedactMode.BLACKOUT -> Color.Black
                activeTool == MarkupTool.REDACT && redactMode == RedactMode.BLUR -> MaterialTheme.colorScheme.primary
                activeTool == MarkupTool.REDACT -> MaterialTheme.colorScheme.primary
                activeTool == MarkupTool.HIGHLIGHTER -> selectedColor.copy(alpha = 0.65f)
                else -> selectedColor
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        // ── 3. Color Swatches Row (White, Black, Coral/Red, Green, Blue, Yellow, Orange, Purple) ──
        val palette = remember {
            listOf(
                Color(0xFFFFFFFF), // White
                Color(0xFF000000), // Black
                Color(0xFFE53935), // Red / Coral
                Color(0xFF43A047), // Green
                Color(0xFF1E88E5), // Blue
                Color(0xFFFFD600), // Yellow
                Color(0xFFFB8C00), // Orange
                Color(0xFF8E24AA)  // Purple
            )
        }

        if (activeTool != MarkupTool.REDACT) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                items(palette) { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .pressScale()
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onColorChange(color)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            val checkTint = if (color == Color.White || color == Color(0xFFFFD600)) Color.Black else Color.White
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_check),
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // In Redact Mosaic or Blur mode: show privacy guidance pill
            val guidanceText = if (redactMode == RedactMode.BLUR) {
                "Draw over faces or sensitive info to smoothly blur"
            } else {
                "Draw over faces, IDs, numbers or sensitive info to pixelate"
            }
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (redactMode == RedactMode.BLUR) R.drawable.ic_ms_blur_on else R.drawable.ic_ms_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = guidanceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── 4. Bottom Action Row: Cancel `X`, "Markup" title, Apply `✓` ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onCancel()
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_close),
                    contentDescription = "Cancel",
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Markup",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            FilledIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onApply()
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_check),
                    contentDescription = "Apply",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveTextModeView(
    selectedText: MarkupText?,
    onAddText: () -> Unit,
    onEditText: (MarkupText) -> Unit,
    onDeleteText: (MarkupText) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Text Action Row: Add Text, Edit Text (if selected), Delete Text (if selected)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Add Text" Button
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .pressScale()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAddText()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                        contentDescription = "Add Text",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Add Text",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            if (selectedText != null) {
                Spacer(modifier = Modifier.width(12.dp))

                // "Edit" Button
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .pressScale()
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onEditText(selectedText)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_edit),
                            contentDescription = "Edit Text",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // "Delete" Button
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .pressScale()
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onDeleteText(selectedText)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                            contentDescription = "Delete Text",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // 2. Bottom Action Row: Cancel `X`, "Text", Apply `✓`
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onCancel()
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_close),
                    contentDescription = "Cancel",
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Text",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            FilledIconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onApply()
                },
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_check),
                    contentDescription = "Apply",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Capsule button toggle for Redact modes (Mosaic vs Blackout).
 */
@Composable
private fun RedactModePill(
    label: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ShapeFull,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .pressScale(0.94f)
            .clip(ShapeFull)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Signature stroke width capsule slider:
 * - Rounded capsule pill track
 * - Min dot indicator on the left
 * - Max dot indicator on the right
 * - Smoothly draggable thumb between min and max with haptic feedback
 */
@Composable
fun StrokeWidthCapsuleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .clip(ShapeFull)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(range) {
                val padPx = with(density) { 24.dp.toPx() }
                val maxWPx = size.width.toFloat()
                val trackWPx = (maxWPx - 2 * padPx).coerceAtLeast(1f)

                fun updateFromPosition(touchX: Float) {
                    val newFraction = ((touchX - padPx) / trackWPx).coerceIn(0f, 1f)
                    val newValue = range.start + newFraction * (range.endInclusive - range.start)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onValueChange(newValue)
                }

                detectTapGestures { offset ->
                    updateFromPosition(offset.x)
                }
            }
            .pointerInput(range) {
                val padPx = with(density) { 24.dp.toPx() }
                val maxWPx = size.width.toFloat()
                val trackWPx = (maxWPx - 2 * padPx).coerceAtLeast(1f)

                detectDragGestures { change, _ ->
                    change.consume()
                    val touchX = change.position.x
                    val newFraction = ((touchX - padPx) / trackWPx).coerceIn(0f, 1f)
                    val newValue = range.start + newFraction * (range.endInclusive - range.start)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onValueChange(newValue)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val padDp = 24.dp
        val padPx = with(density) { padDp.toPx() }
        val maxWPx = constraints.maxWidth.toFloat()
        val trackWPx = (maxWPx - 2 * padPx).coerceAtLeast(1f)
        val thumbXPx = padPx + fraction * trackWPx

        // 1. Min dot on left
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                .align(Alignment.CenterStart)
        )

        // 2. Center connecting subtle track
        Box(
            modifier = Modifier
                .padding(horizontal = padDp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .align(Alignment.Center)
        )

        // 3. Max dot on right
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                .align(Alignment.CenterEnd)
        )

        // 4. Draggable thumb
        val thumbRadiusPx = with(density) { 14.dp.toPx() }
        val offsetXPx = (thumbXPx - thumbRadiusPx).roundToInt()

        Surface(
            shape = CircleShape,
            color = activeColor,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.95f)
            ),
            modifier = Modifier
                .offset { IntOffset(offsetXPx, 0) }
                .size(28.dp)
                .align(Alignment.CenterStart)
        ) {}
    }
}

@Composable
fun getMarkupToolLabel(tool: MarkupTool): String {
    return when (tool) {
        MarkupTool.PEN -> stringResource(R.string.editor_markup_pen)
        MarkupTool.HIGHLIGHTER -> stringResource(R.string.editor_markup_highlighter)
        MarkupTool.REDACT -> stringResource(R.string.editor_markup_redact)
        MarkupTool.TEXT -> stringResource(R.string.editor_markup_text)
        else -> tool.label
    }
}
