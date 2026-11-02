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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.components.EditorRulerSlider
import com.inferno.gallery.ui.editor.crop.CropGeometryHelper
import com.inferno.gallery.ui.editor.model.AspectRatioOption
import com.inferno.gallery.ui.editor.model.CropState

/**
 * Sub-modes within the Crop studio.
 */
enum class CropSubMode {
    CAROUSEL,
    FRAMING,
    PERSPECTIVE
}

/**
 * Active axis for 2-axis perspective keystone adjustment.
 */
enum class PerspectiveAxis {
    VERTICAL,
    HORIZONTAL
}

/**
 * Crop, Framing & Perspective Studio.
 *
 * Implements:
 * 1. Main Crop Carousel:
 *    - Squircle tiles: Crop / Aspect, Straighten (Framing), Rotate 90° CCW, Perspective, Reset.
 *    - Active parameter dot indicators and state-aware styling.
 * 2. Framing (Straighten) Sub-mode:
 *    - Header: "Framing" with degree readout.
 *    - [EditorRulerSlider] (-45°..+45°, 0° center, 1° step) with degree formatter.
 *    - Action buttons: Cancel `X` (reverts to entry angle) and Apply `✓` (confirms angle).
 * 3. Perspective Sub-mode:
 *    - Keystone axis toggle: "Vertical" and "Horizontal".
 *    - [EditorRulerSlider] (-30°..+30°, 0° center, 1° step) with degree formatter.
 *    - Action buttons: Cancel `X` (reverts tilts) and Apply `✓` (confirms tilts).
 * 4. Aspect Ratio Picker:
 *    - Modal bottom sheet with all 11 canonical aspect ratio options,
 *      ratio preview glyphs, and active option checkmark indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropStudio(
    cropState: CropState,
    onCropStateChange: (CropState) -> Unit,
    modifier: Modifier = Modifier,
    imageAspect: Float = 1f,
    showAspectPicker: Boolean = false,
    onDismissAspectPicker: (() -> Unit)? = null,
    onStraightenDone: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
    initialSubMode: CropSubMode = CropSubMode.CAROUSEL
) {
    var subMode by rememberSaveable { mutableStateOf(initialSubMode) }
    LaunchedEffect(initialSubMode) {
        subMode = initialSubMode
    }
    var perspectiveAxis by rememberSaveable { mutableStateOf(PerspectiveAxis.VERTICAL) }
    var isInternalAspectSheetOpen by rememberSaveable { mutableStateOf(false) }

    // Saved states for Cancel action in sub-modes
    var savedStraightenAngle by remember { mutableFloatStateOf(cropState.straightenAngle) }
    var savedVerticalTilt by remember { mutableFloatStateOf(cropState.verticalTilt) }
    var savedHorizontalTilt by remember { mutableFloatStateOf(cropState.horizontalTilt) }

    val view = LocalView.current
    val effectiveAspect = remember(imageAspect, cropState.rotation) {
        CropGeometryHelper.effectiveAspectRatio(imageAspect, cropState.rotation)
    }

    val isAspectPickerVisible = showAspectPicker || isInternalAspectSheetOpen

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = subMode,
            transitionSpec = {
                if (targetState != CropSubMode.CAROUSEL && initialState == CropSubMode.CAROUSEL) {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 4 })
                } else if (targetState == CropSubMode.CAROUSEL && initialState != CropSubMode.CAROUSEL) {
                    (fadeIn() + slideInHorizontally { -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { it / 4 })
                } else {
                    fadeIn() togetherWith fadeOut()
                }
            },
            label = "cropSubModeTransition"
        ) { currentMode ->
            when (currentMode) {
                CropSubMode.CAROUSEL -> {
                    CropCarouselView(
                        cropState = cropState,
                        onOpenAspect = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            isInternalAspectSheetOpen = true
                        },
                        onOpenStraighten = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            savedStraightenAngle = cropState.straightenAngle
                            subMode = CropSubMode.FRAMING
                        },
                        onRotate90 = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            val newRotation = CropGeometryHelper.rotateCounterClockwise(cropState.rotation)
                            val newEffectiveAspect = CropGeometryHelper.effectiveAspectRatio(imageAspect, newRotation)
                            val newRect = if (cropState.aspectOption != AspectRatioOption.FREEFORM) {
                                CropGeometryHelper.calculateInitialAspectRect(cropState.aspectOption, newEffectiveAspect)
                            } else {
                                cropState.cropRect
                            }
                            onCropStateChange(cropState.copy(rotation = newRotation, cropRect = newRect))
                        },
                        onOpenPerspective = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            savedVerticalTilt = cropState.verticalTilt
                            savedHorizontalTilt = cropState.horizontalTilt
                            subMode = CropSubMode.PERSPECTIVE
                        },
                        onResetCrop = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onCropStateChange(CropState())
                            onReset?.invoke()
                        }
                    )
                }

                CropSubMode.FRAMING -> {
                    FramingSubModeView(
                        straightenAngle = cropState.straightenAngle,
                        onStraightenChange = { angle ->
                            onCropStateChange(cropState.withStraighten(angle))
                        },
                        onCancel = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onCropStateChange(cropState.withStraighten(savedStraightenAngle))
                            subMode = CropSubMode.CAROUSEL
                        },
                        onApply = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            subMode = CropSubMode.CAROUSEL
                            onStraightenDone?.invoke()
                        }
                    )
                }

                CropSubMode.PERSPECTIVE -> {
                    PerspectiveSubModeView(
                        verticalTilt = cropState.verticalTilt,
                        horizontalTilt = cropState.horizontalTilt,
                        activeAxis = perspectiveAxis,
                        onAxisChange = { newAxis ->
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            perspectiveAxis = newAxis
                        },
                        onTiltChange = { value ->
                            val updated = if (perspectiveAxis == PerspectiveAxis.VERTICAL) {
                                cropState.withVerticalTilt(value)
                            } else {
                                cropState.withHorizontalTilt(value)
                            }
                            onCropStateChange(updated)
                        },
                        onCancel = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onCropStateChange(
                                cropState.copy(
                                    verticalTilt = savedVerticalTilt,
                                    horizontalTilt = savedHorizontalTilt
                                )
                            )
                            subMode = CropSubMode.CAROUSEL
                        },
                        onApply = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            subMode = CropSubMode.CAROUSEL
                        }
                    )
                }
            }
        }
    }

    // Modal Aspect Ratio Selector Sheet
    if (isAspectPickerVisible) {
        AspectRatioPickerSheet(
            currentOption = cropState.aspectOption,
            onOptionSelected = { option ->
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                val newRect = CropGeometryHelper.calculateInitialAspectRect(option, effectiveAspect)
                onCropStateChange(cropState.copy(aspectOption = option, cropRect = newRect))
                isInternalAspectSheetOpen = false
                onDismissAspectPicker?.invoke()
            },
            onDismissRequest = {
                isInternalAspectSheetOpen = false
                onDismissAspectPicker?.invoke()
            }
        )
    }
}

/**
 * Main Crop bottom squircle tools carousel row.
 */
@Composable
private fun CropCarouselView(
    cropState: CropState,
    onOpenAspect: () -> Unit,
    onOpenStraighten: () -> Unit,
    onRotate90: () -> Unit,
    onOpenPerspective: () -> Unit,
    onResetCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Crop / Aspect ratio
        val hasCustomAspect = cropState.aspectOption != AspectRatioOption.FREEFORM ||
                cropState.cropRect != androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
        CropToolTile(
            label = cropState.aspectOption.label,
            iconRes = R.drawable.ic_ms_crop,
            isActive = hasCustomAspect,
            hasActiveDot = hasCustomAspect,
            onClick = onOpenAspect
        )

        // 2. Straighten (Framing)
        val hasStraighten = cropState.straightenAngle != 0f
        CropToolTile(
            label = if (hasStraighten) "${cropState.straightenAngle.toInt()}°" else "Straighten",
            iconRes = R.drawable.ic_ms_straighten,
            isActive = hasStraighten,
            hasActiveDot = hasStraighten,
            onClick = onOpenStraighten
        )

        // 3. Rotate 90° CCW
        val hasRotation = cropState.rotation != 0
        CropToolTile(
            label = if (hasRotation) "${cropState.rotation}°" else "Rotate",
            iconRes = R.drawable.ic_ms_rotate_90_degrees_ccw,
            isActive = hasRotation,
            hasActiveDot = hasRotation,
            onClick = onRotate90
        )

        // 4. Perspective
        val hasPerspective = cropState.verticalTilt != 0f || cropState.horizontalTilt != 0f
        CropToolTile(
            label = "Perspective",
            iconRes = R.drawable.ic_ms_perspective,
            isActive = hasPerspective,
            hasActiveDot = hasPerspective,
            onClick = onOpenPerspective
        )

        // 5. Reset
        CropToolTile(
            label = "Reset",
            iconRes = R.drawable.ic_ms_refresh,
            isActive = false,
            isEnabled = cropState.isModified,
            hasActiveDot = false,
            onClick = onResetCrop
        )
    }
}

/**
 * Individual squircle tool button for editor design.
 */
@Composable
private fun CropToolTile(
    label: String,
    @DrawableRes iconRes: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    hasActiveDot: Boolean = false
) {
    Column(
        modifier = modifier
            .width(62.dp)
            .clickable(enabled = isEnabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (!isEnabled) {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f)
                    } else if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (!isEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                } else if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            // Active dot in corner
            if (hasActiveDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 5.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (!isEnabled) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            } else if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Straighten / Framing sub-mode view with horizontal angle ruler slider and Cancel `X` / Apply `✓` buttons.
 */
@Composable
private fun FramingSubModeView(
    straightenAngle: Float,
    onStraightenChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-mode Header: "Framing"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Framing",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${straightenAngle.toInt()}°",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Horizontal Ruler Slider
        EditorRulerSlider(
            value = straightenAngle,
            onValueChange = onStraightenChange,
            range = -45f..45f,
            step = 1f,
            defaultValue = 0f,
            iconRes = R.drawable.ic_ms_straighten,
            valueFormatter = { "${it.toInt()}°" },
            onReset = { onStraightenChange(0f) },
            showDoneButton = false
        )

        // Action Buttons Row: Cancel X and Apply ✓
        SubModeActionButtons(
            onCancel = onCancel,
            onApply = onApply
        )
    }
}

/**
 * 2-Axis Perspective Keystone sub-mode view with Vertical / Horizontal toggle, ruler dial, and Cancel/Apply buttons.
 */
@Composable
private fun PerspectiveSubModeView(
    verticalTilt: Float,
    horizontalTilt: Float,
    activeAxis: PerspectiveAxis,
    onAxisChange: (PerspectiveAxis) -> Unit,
    onTiltChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVal = if (activeAxis == PerspectiveAxis.VERTICAL) verticalTilt else horizontalTilt

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-mode Header: "Perspective"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perspective",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${activeVal.toInt()}°",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Toggle between Vertical and Horizontal keystone axis
        PerspectiveAxisToggle(
            activeAxis = activeAxis,
            verticalTilt = verticalTilt,
            horizontalTilt = horizontalTilt,
            onAxisChange = onAxisChange
        )

        // Horizontal Ruler Slider
        EditorRulerSlider(
            value = activeVal,
            onValueChange = onTiltChange,
            range = -30f..30f,
            step = 1f,
            defaultValue = 0f,
            iconRes = R.drawable.ic_ms_perspective,
            valueFormatter = { "${it.toInt()}°" },
            onReset = { onTiltChange(0f) },
            showDoneButton = false
        )

        // Action Buttons Row: Cancel X and Apply ✓
        SubModeActionButtons(
            onCancel = onCancel,
            onApply = onApply
        )
    }
}

/**
 * Axis selector capsule row for Perspective transforms.
 */
@Composable
private fun PerspectiveAxisToggle(
    activeAxis: PerspectiveAxis,
    verticalTilt: Float,
    horizontalTilt: Float,
    onAxisChange: (PerspectiveAxis) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Vertical axis pill
            val isVert = activeAxis == PerspectiveAxis.VERTICAL
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isVert) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onAxisChange(PerspectiveAxis.VERTICAL) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Vertical",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isVert) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isVert) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (verticalTilt != 0f) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${verticalTilt.toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Horizontal axis pill
            val isHoriz = activeAxis == PerspectiveAxis.HORIZONTAL
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isHoriz) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onAxisChange(PerspectiveAxis.HORIZONTAL) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Horizontal",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isHoriz) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isHoriz) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (horizontalTilt != 0f) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${horizontalTilt.toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sub-mode action buttons: Cancel `X` and Apply `✓`.
 */
@Composable
private fun SubModeActionButtons(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel `X` button
        FilledTonalIconButton(
            onClick = onCancel,
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

        // Apply `✓` button
        FilledIconButton(
            onClick = onApply,
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

/**
 * Modal bottom sheet presenting all 11 canonical aspect ratio options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun AspectRatioPickerSheet(
    currentOption: AspectRatioOption,
    onOptionSelected: (AspectRatioOption) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember { AspectRatioOption.entries }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Aspect ratio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose crop dimensions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onDismissRequest,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(text = "Done", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 11 Aspect Ratio Options List
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options) { option ->
                    val isSelected = option == currentOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                            }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 24.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ratio visual glyph box
                        AspectRatioGlyph(
                            option = option,
                            isSelected = isSelected
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Label and description
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                            val subtitle = when (option) {
                                AspectRatioOption.FREEFORM -> "Unconstrained freeform"
                                AspectRatioOption.ORIGINAL -> "Match photo original aspect"
                                AspectRatioOption.SQUARE -> "1:1 Square"
                                else -> "Fixed ${option.label}"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Checkmark indicator
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_check),
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miniature geometric aspect ratio glyph preview box.
 */
@Composable
private fun AspectRatioGlyph(
    option: AspectRatioOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val glyphColor = if (isSelected) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.onSurfaceVariant

    val boxWidth: Float
    val boxHeight: Float

    when (option) {
        AspectRatioOption.FREEFORM -> {
            boxWidth = 22f
            boxHeight = 16f
        }
        AspectRatioOption.ORIGINAL -> {
            boxWidth = 20f
            boxHeight = 16f
        }
        AspectRatioOption.SQUARE -> {
            boxWidth = 18f
            boxHeight = 18f
        }
        AspectRatioOption.RATIO_16_9 -> {
            boxWidth = 24f
            boxHeight = 13.5f
        }
        AspectRatioOption.RATIO_9_16 -> {
            boxWidth = 13.5f
            boxHeight = 24f
        }
        AspectRatioOption.RATIO_4_3 -> {
            boxWidth = 22f
            boxHeight = 16.5f
        }
        AspectRatioOption.RATIO_3_4 -> {
            boxWidth = 16.5f
            boxHeight = 22f
        }
        AspectRatioOption.RATIO_3_2 -> {
            boxWidth = 22f
            boxHeight = 14.6f
        }
        AspectRatioOption.RATIO_2_3 -> {
            boxWidth = 14.6f
            boxHeight = 22f
        }
        AspectRatioOption.RATIO_5_4 -> {
            boxWidth = 20f
            boxHeight = 16f
        }
        AspectRatioOption.RATIO_4_5 -> {
            boxWidth = 16f
            boxHeight = 20f
        }
    }

    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = boxWidth.dp, height = boxHeight.dp)
                .border(
                    width = 1.5.dp,
                    color = glyphColor,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}
