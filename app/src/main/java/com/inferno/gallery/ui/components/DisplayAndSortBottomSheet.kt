@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
 * Compact bottom sheet for controlling View Mode and Sort Order with pure solid tonal layering.
 * Features an integrated View Mode segmented bar and balanced 4-row inline sort controls.
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
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
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

            // View Mode Section
            Text(
                text = stringResource(R.string.menu_view_mode).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val currentSort = if (isAlbumsScreen) albumSortOrder else sortOrder
            var activeSort by remember(currentSort) { mutableStateOf(currentSort) }
            val onSelectSort: (SortOrder) -> Unit = { order ->
                activeSort = order
                if (isAlbumsScreen) onAlbumSortOrderChange(order) else onSortOrderChange(order)
            }

            var activeViewMode by remember(viewMode) { mutableStateOf(viewMode) }
            val onSelectViewMode: (ViewMode) -> Unit = { mode ->
                activeViewMode = mode
                onViewModeChange(mode)
            }

            var activeAlbumLayout by remember(albumLayoutMode) { mutableStateOf(albumLayoutMode) }
            val onSelectAlbumLayout: (AlbumLayoutMode) -> Unit = { layout ->
                activeAlbumLayout = layout
                onAlbumLayoutModeChange(layout)
            }

            // Balanced Full-Width View Mode Segmented Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeMedium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAlbumsScreen) {
                        ViewModePill(
                            title = stringResource(R.string.layout_grid_view),
                            subtitle = null,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                            isSelected = activeAlbumLayout == AlbumLayoutMode.GRID,
                            onClick = { onSelectAlbumLayout(AlbumLayoutMode.GRID) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModePill(
                            title = stringResource(R.string.layout_list_view),
                            subtitle = null,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_list),
                            isSelected = activeAlbumLayout == AlbumLayoutMode.LIST,
                            onClick = { onSelectAlbumLayout(AlbumLayoutMode.LIST) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        ViewModePill(
                            title = stringResource(R.string.view_mode_grouped),
                            subtitle = stringResource(R.string.view_mode_with_dates),
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                            isSelected = activeViewMode == ViewMode.Grouped,
                            onClick = { onSelectViewMode(ViewMode.Grouped) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModePill(
                            title = stringResource(R.string.view_mode_immersive),
                            subtitle = stringResource(R.string.view_mode_clean_grid),
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                            isSelected = activeViewMode == ViewMode.Immersive,
                            onClick = { onSelectViewMode(ViewMode.Immersive) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAlbumsScreen) {
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_name),
                        option1Label = stringResource(R.string.sort_opt_a_to_z),
                        option1 = SortOrder.NameAsc,
                        option2Label = stringResource(R.string.sort_opt_z_to_a),
                        option2 = SortOrder.NameDesc,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_date_added),
                        option1Label = stringResource(R.string.sort_opt_newest),
                        option1 = SortOrder.DateModifiedNewToOld,
                        option2Label = stringResource(R.string.sort_opt_oldest),
                        option2 = SortOrder.DateModifiedOldToNew,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_date_taken),
                        option1Label = stringResource(R.string.sort_opt_newest),
                        option1 = SortOrder.NewToOld,
                        option2Label = stringResource(R.string.sort_opt_oldest),
                        option2 = SortOrder.OldToNew,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_count),
                        option1Label = stringResource(R.string.sort_opt_most),
                        option1 = SortOrder.CountDesc,
                        option2Label = stringResource(R.string.sort_opt_least),
                        option2 = SortOrder.CountAsc,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_size),
                        option1Label = stringResource(R.string.sort_opt_large_to_small),
                        option1 = SortOrder.BigToSmall,
                        option2Label = stringResource(R.string.sort_opt_small_to_large),
                        option2 = SortOrder.SmallToBig,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                } else {
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_date_taken),
                        option1Label = stringResource(R.string.sort_opt_newest),
                        option1 = SortOrder.NewToOld,
                        option2Label = stringResource(R.string.sort_opt_oldest),
                        option2 = SortOrder.OldToNew,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_date_added),
                        option1Label = stringResource(R.string.sort_opt_newest),
                        option1 = SortOrder.DateModifiedNewToOld,
                        option2Label = stringResource(R.string.sort_opt_oldest),
                        option2 = SortOrder.DateModifiedOldToNew,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_size),
                        option1Label = stringResource(R.string.sort_opt_large_to_small),
                        option1 = SortOrder.BigToSmall,
                        option2Label = stringResource(R.string.sort_opt_small_to_large),
                        option2 = SortOrder.SmallToBig,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                    InlineSortRow(
                        categoryTitle = stringResource(R.string.sort_cat_name),
                        option1Label = stringResource(R.string.sort_opt_a_to_z),
                        option1 = SortOrder.NameAsc,
                        option2Label = stringResource(R.string.sort_opt_z_to_a),
                        option2 = SortOrder.NameDesc,
                        selectedSort = activeSort,
                        onSortChange = onSelectSort
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModePill(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "viewModePillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "viewModePillContent"
    )
    val subtitleColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = spring(),
        label = "viewModePillSub"
    )

    Surface(
        onClick = onClick,
        shape = ShapeMedium,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor
                )
            }
        }
    }
}

@Composable
private fun InlineSortRow(
    categoryTitle: String,
    option1Label: String,
    option1: SortOrder,
    option2Label: String,
    option2: SortOrder,
    selectedSort: SortOrder,
    onSortChange: (SortOrder) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = categoryTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortPill(
                label = option1Label,
                isSelected = selectedSort == option1,
                onClick = { onSortChange(option1) }
            )
            SortPill(
                label = option2Label,
                isSelected = selectedSort == option2,
                onClick = { onSortChange(option2) }
            )
        }
    }
}

@Composable
private fun SortPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = spring(),
        label = "sortPillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "sortPillText"
    )

    Surface(
        onClick = onClick,
        shape = ShapeMedium,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
