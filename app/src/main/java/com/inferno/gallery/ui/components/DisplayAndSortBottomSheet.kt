@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.R
import com.inferno.gallery.data.AlbumLayoutMode
import com.inferno.gallery.ui.SortOrder
import com.inferno.gallery.ui.ViewMode
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeMedium

/**
 * Half-height bottom sheet for controlling View Mode and Sort Order with Material 3 Expressive styling.
 */
@Composable
fun DisplayAndSortBottomSheet(
    isAlbumsScreen: Boolean,
    viewMode: ViewMode = ViewMode.Grouped,
    onViewModeChange: (ViewMode) -> Unit = {},
    albumLayoutMode: AlbumLayoutMode = AlbumLayoutMode.GRID,
    onAlbumLayoutModeChange: (AlbumLayoutMode) -> Unit = {},
    sortOrder: SortOrder = SortOrder.NewToOld,
    onSortOrderChange: (SortOrder) -> Unit = {},
    albumSortOrder: SortOrder = SortOrder.NameAsc,
    onAlbumSortOrderChange: (SortOrder) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = ShapeEdgeTop,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header with Title and Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.menu_view_mode) + " & " + stringResource(R.string.menu_sort_order),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                PhotonCloseButton(
                    onClick = onDismissRequest,
                    contentDescription = stringResource(R.string.action_close)
                )
            }

            // View Mode / Layout Section
            Text(
                text = stringResource(R.string.menu_view_mode).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAlbumsScreen) {
                    ViewModeToggle(
                        title = stringResource(R.string.layout_grid_view),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                        isSelected = albumLayoutMode == AlbumLayoutMode.GRID,
                        onClick = { onAlbumLayoutModeChange(AlbumLayoutMode.GRID) },
                        modifier = Modifier.weight(1f)
                    )
                    ViewModeToggle(
                        title = stringResource(R.string.layout_list_view),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_list),
                        isSelected = albumLayoutMode == AlbumLayoutMode.LIST,
                        onClick = { onAlbumLayoutModeChange(AlbumLayoutMode.LIST) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ViewModeToggle(
                        title = stringResource(R.string.view_mode_grouped),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                        isSelected = viewMode == ViewMode.Grouped,
                        onClick = { onViewModeChange(ViewMode.Grouped) },
                        modifier = Modifier.weight(1f)
                    )
                    ViewModeToggle(
                        title = stringResource(R.string.view_mode_immersive),
                        icon = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                        isSelected = viewMode == ViewMode.Immersive,
                        onClick = { onViewModeChange(ViewMode.Immersive) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sort Order Section
            Text(
                text = stringResource(R.string.menu_sort_order).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sortOptions: List<Pair<SortOrder, String>> = if (isAlbumsScreen) {
                listOf(
                    SortOrder.NameAsc to stringResource(R.string.sort_name_a_to_z),
                    SortOrder.NameDesc to stringResource(R.string.sort_name_z_to_a),
                    SortOrder.DateModifiedNewToOld to stringResource(R.string.sort_date_added_newest),
                    SortOrder.DateModifiedOldToNew to stringResource(R.string.sort_date_added_oldest),
                    SortOrder.NewToOld to stringResource(R.string.sort_date_taken_newest),
                    SortOrder.OldToNew to stringResource(R.string.sort_date_taken_oldest),
                    SortOrder.CountDesc to stringResource(R.string.sort_count_most),
                    SortOrder.CountAsc to stringResource(R.string.sort_count_least),
                    SortOrder.BigToSmall to stringResource(R.string.sort_size_big_to_small),
                    SortOrder.SmallToBig to stringResource(R.string.sort_size_small_to_big)
                )
            } else {
                listOf(
                    SortOrder.NewToOld to stringResource(R.string.sort_date_taken_newest),
                    SortOrder.OldToNew to stringResource(R.string.sort_date_taken_oldest),
                    SortOrder.DateModifiedNewToOld to stringResource(R.string.sort_date_added_newest),
                    SortOrder.DateModifiedOldToNew to stringResource(R.string.sort_date_added_oldest),
                    SortOrder.BigToSmall to stringResource(R.string.sort_size_big_to_small),
                    SortOrder.SmallToBig to stringResource(R.string.sort_size_small_to_big),
                    SortOrder.NameAsc to stringResource(R.string.sort_name_a_to_z),
                    SortOrder.NameDesc to stringResource(R.string.sort_name_z_to_a)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sortOptions) { (option, label) ->
                    val isSelected = if (isAlbumsScreen) albumSortOrder == option else sortOrder == option
                    SortOptionRow(
                        label = label,
                        isSelected = isSelected,
                        onClick = {
                            if (isAlbumsScreen) onAlbumSortOrderChange(option) else onSortOrderChange(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(),
        label = "viewModeToggleBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "viewModeToggleContent"
    )
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .clip(ShapeMedium)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = ShapeMedium,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SortOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "sortRowBg"
    )
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeMedium)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
        if (isSelected) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

