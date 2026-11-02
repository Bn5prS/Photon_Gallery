package com.inferno.gallery.ui.editor.studios

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.components.EditorRulerSlider
import com.inferno.gallery.ui.editor.model.FilterPreset

/**
 * Filters Studio for comprehensive filter workflow:
 *
 * 1. Browsing Mode:
 *    - Horizontal carousel of squircle thumbnails displaying live user photo with filter applied.
 *    - Filter name below thumbnail (e.g. "Original", "Playa").
 *    - Vertical divider between "Lite" and "Playa".
 *    - Selected filter shows checkmark or current strength number (e.g. "100") inside the card.
 *    - Bottom "Done" pill button.
 *
 * 2. Scrubbing Mode (opened when tapping selected filter again):
 *    - Header with chevron `<` collapse button and filter title.
 *    - [EditorRulerSlider] (0..100) with tactile haptics, reset button, and Done action.
 */
@Composable
fun FiltersStudio(
    previewBitmap: Bitmap?,
    selectedFilter: FilterPreset,
    filterStrength: Float,
    onSelectFilter: (FilterPreset) -> Unit,
    onFilterStrengthChange: (Float) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isScrubbing by rememberSaveable { mutableStateOf(false) }
    val presets = remember { FilterPreset.entries }
    val listState = rememberLazyListState()
    val view = LocalView.current

    // Scroll selected filter into view when returning to carousel
    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) {
            val index = presets.indexOf(selectedFilter).coerceAtLeast(0)
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
            targetState = isScrubbing,
            transitionSpec = {
                if (targetState) {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 4 })
                } else {
                    (fadeIn() + slideInHorizontally { -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { it / 4 })
                }
            },
            label = "FiltersStudioModeTransition"
        ) { scrubbing ->
            if (!scrubbing) {
                // Mode 1: Browsing Carousel View
                FilterBrowsingView(
                    presets = presets,
                    previewBitmap = previewBitmap,
                    selectedPreset = selectedFilter,
                    filterStrength = filterStrength,
                    onSelectPreset = { preset ->
                        if (preset == selectedFilter && preset != FilterPreset.ORIGINAL) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            isScrubbing = true
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onSelectFilter(preset)
                        }
                    },
                    onDone = onDone,
                    listState = listState,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Mode 2: Scrubbing View with Ruler Slider & Chevron collapse button
                FilterScrubbingView(
                    selectedPreset = selectedFilter,
                    filterStrength = filterStrength,
                    onFilterStrengthChange = onFilterStrengthChange,
                    onCollapse = { isScrubbing = false },
                    onDone = {
                        isScrubbing = false
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Overload for index-based callers:
 * `FiltersStudio(previewBitmap, selectedFilterIndex, filterStrength, onSelectFilter, onFilterStrengthChange, onDone)`
 */
@Composable
fun FiltersStudio(
    previewBitmap: Bitmap?,
    selectedFilterIndex: Int,
    filterStrength: Float,
    onSelectFilter: (Int) -> Unit,
    onFilterStrengthChange: (Float) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember { FilterPreset.entries }
    val currentPreset = presets.getOrElse(selectedFilterIndex) { FilterPreset.ORIGINAL }
    FiltersStudio(
        previewBitmap = previewBitmap,
        selectedFilter = currentPreset,
        filterStrength = filterStrength,
        onSelectFilter = { preset ->
            onSelectFilter(presets.indexOf(preset).coerceAtLeast(0))
        },
        onFilterStrengthChange = onFilterStrengthChange,
        onDone = onDone,
        modifier = modifier
    )
}

/**
 * Horizontal browsing carousel of filter thumbnails with divider between Lite and Playa.
 */
@Composable
private fun FilterBrowsingView(
    presets: List<FilterPreset>,
    previewBitmap: Bitmap?,
    selectedPreset: FilterPreset,
    filterStrength: Float,
    onSelectPreset: (FilterPreset) -> Unit,
    onDone: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(presets, key = { _, preset -> preset.name }) { _, preset ->
                if (preset == FilterPreset.PLAYA) {
                    // Distinct divider between Lite and Playa
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(1.dp)
                            .height(56.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                }

                FilterThumbnailItem(
                    preset = preset,
                    previewBitmap = previewBitmap,
                    isSelected = preset == selectedPreset,
                    filterStrength = if (preset == selectedPreset) filterStrength else 100f,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }

        // Bottom Done Pill Button
        FilledTonalButton(
            onClick = onDone,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Done",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * Individual squircle filter thumbnail with live preview and selection badge.
 */
@Composable
private fun FilterThumbnailItem(
    preset: FilterPreset,
    previewBitmap: Bitmap?,
    isSelected: Boolean,
    filterStrength: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matrix = remember(preset, filterStrength, isSelected) {
        if (isSelected) {
            preset.interpolate(filterStrength)
        } else {
            preset.colorMatrix
        }
    }

    Column(
        modifier = modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Squircle Thumbnail Container
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                            RoundedCornerShape(18.dp)
                        )
                    } else {
                        Modifier.border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            RoundedCornerShape(18.dp)
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = preset.label,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(matrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            }

            // Selection Badge inside Card
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                ) {
                    if (preset == FilterPreset.ORIGINAL || filterStrength >= 99.5f) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_check),
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    } else {
                        Text(
                            text = filterStrength.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Preset Label Below Thumbnail
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Filter intensity scrubbing view with title header, collapse chevron, and Ruler Slider.
 */
@Composable
private fun FilterScrubbingView(
    selectedPreset: FilterPreset,
    filterStrength: Float,
    onFilterStrengthChange: (Float) -> Unit,
    onCollapse: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header with Collapse Chevron and Filter Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_arrow_back),
                    contentDescription = "Collapse intensity slider",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = selectedPreset.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            // Balance spacer matching chevron button size
            Spacer(modifier = Modifier.size(36.dp))
        }

        // Dedicated 0..100 Ruler Slider
        EditorRulerSlider(
            value = filterStrength,
            onValueChange = onFilterStrengthChange,
            range = 0f..100f,
            step = 1f,
            defaultValue = 100f,
            iconRes = R.drawable.ic_ms_palette,
            title = null,
            onReset = { onFilterStrengthChange(100f) },
            onDone = onDone
        )
    }
}
