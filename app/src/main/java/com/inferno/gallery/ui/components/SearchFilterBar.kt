@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.inferno.gallery.R
import com.inferno.gallery.data.SearchDatePreset
import com.inferno.gallery.data.SearchDocumentTypeOption
import com.inferno.gallery.data.SearchFilterQueryBuilder
import com.inferno.gallery.data.SearchFilterState
import com.inferno.gallery.data.SearchMediaTypeFilter
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.ui.AlbumBucket
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import java.io.File

enum class SearchFilterSheetPage {
    MAIN, ALBUMS, LOCATIONS
}

/**
 * Active-only removable filter pills shown directly below the Search bar.
 * Stays completely hidden when no metadata filters are applied so the Search
 * screen remains uncluttered.
 */
@Composable
fun ActiveSearchFilterStrip(
    filters: SearchFilterState,
    availablePeople: List<PersonEntity>,
    onOpenSheet: (SearchFilterSheetPage) -> Unit,
    onUpdateFilters: ((SearchFilterState) -> SearchFilterState) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val peopleById = remember(availablePeople) { availablePeople.associateBy { it.id } }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filters.hasDateFilter) {
            item(key = "active_filter_date") {
                ActiveFilterPill(
                    label = filters.dateLabel ?: "Date",
                    iconRes = R.drawable.ic_ms_calendar_month,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.MAIN)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(dateStartSec = null, dateEndSec = null, dateLabel = null) }
                    }
                )
            }
        }

        if (filters.mediaType != SearchMediaTypeFilter.ALL) {
            item(key = "active_filter_media") {
                ActiveFilterPill(
                    label = filters.mediaType.label,
                    iconRes = R.drawable.ic_ms_image,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.MAIN)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(mediaType = SearchMediaTypeFilter.ALL) }
                    }
                )
            }
        }

        if (filters.personIds.isNotEmpty()) {
            item(key = "active_filter_person") {
                val names = filters.personIds.mapNotNull { id ->
                    peopleById[id]?.name?.takeIf { it.isNotBlank() } ?: "Person #$id"
                }
                val label = when (names.size) {
                    0 -> "People"
                    1 -> names.first()
                    else -> "${names.first()} +${names.size - 1}"
                }
                ActiveFilterPill(
                    label = label,
                    iconRes = R.drawable.ic_ms_person,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.MAIN)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(personIds = emptySet()) }
                    }
                )
            }
        }

        if (filters.documentCategories.isNotEmpty()) {
            item(key = "active_filter_document") {
                val names = SearchDocumentTypeOption.entries
                    .filter { it.categoryKey in filters.documentCategories }
                    .map { it.label }
                val label = when (names.size) {
                    0 -> "Document"
                    1 -> names.first()
                    else -> "${names.first()} +${names.size - 1}"
                }
                ActiveFilterPill(
                    label = label,
                    iconRes = R.drawable.ic_ms_description,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.MAIN)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(documentCategories = emptySet()) }
                    }
                )
            }
        }

        if (filters.albumNames.isNotEmpty()) {
            item(key = "active_filter_album") {
                val sortedNames = filters.albumNames.sorted()
                val label = if (sortedNames.size == 1) {
                    sortedNames.first()
                } else {
                    "${sortedNames.first()} +${sortedNames.size - 1}"
                }
                ActiveFilterPill(
                    label = label,
                    iconRes = R.drawable.ic_ms_photo_album,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.ALBUMS)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(albumNames = emptySet()) }
                    }
                )
            }
        }

        if (!filters.locationName.isNullOrBlank()) {
            item(key = "active_filter_location") {
                ActiveFilterPill(
                    label = filters.locationName,
                    iconRes = R.drawable.ic_ms_location_on,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSheet(SearchFilterSheetPage.LOCATIONS)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateFilters { it.copy(locationName = null) }
                    }
                )
            }
        }

        if (filters.activeCount >= 2) {
            item(key = "active_filter_clear_all") {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClearAll()
                    },
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "Clear all",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

/**
 * Unified Material 3 Expressive Filters bottom sheet combining Date Range, Media Type,
 * Document Type, People, Albums, and Location filters in one place (#53).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFiltersSheet(
    initialPage: SearchFilterSheetPage,
    filters: SearchFilterState,
    availablePeople: List<PersonEntity>,
    availableAlbums: List<AlbumBucket>,
    availableLocations: List<String>,
    onUpdateFilters: ((SearchFilterState) -> SearchFilterState) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var currentPage by remember(initialPage) { mutableStateOf(initialPage) }
    var showCustomDateRangePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(MotionTokens.gentleSpring()) togetherWith fadeOut(MotionTokens.gentleSpring())
            },
            label = "searchFiltersSheetPage"
        ) { page ->
            when (page) {
                SearchFilterSheetPage.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        // Sheet header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (filters.isActive) {
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onClearAll()
                                    },
                                    shape = ShapeFull
                                ) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        // Scrollable filter sections
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // 1. Date
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterSectionLabel("Date")
                                val isCustomDateActive = filters.hasDateFilter &&
                                    SearchDatePreset.entries.none { it.label == filters.dateLabel }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SearchDatePreset.entries.forEach { preset ->
                                        val isPicked = filters.dateLabel == preset.label
                                        SelectableFilterPill(
                                            label = preset.label,
                                            selected = isPicked,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isPicked) {
                                                    onUpdateFilters {
                                                        it.copy(dateStartSec = null, dateEndSec = null, dateLabel = null)
                                                    }
                                                } else {
                                                    val (startSec, endSec) = preset.computeEpochSecRange()
                                                    onUpdateFilters {
                                                        it.copy(
                                                            dateStartSec = startSec,
                                                            dateEndSec = endSec,
                                                            dateLabel = preset.label
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    SelectableFilterPill(
                                        label = if (isCustomDateActive) (filters.dateLabel ?: "Custom") else "Custom…",
                                        selected = isCustomDateActive,
                                        leadingIconRes = R.drawable.ic_ms_calendar_month,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showCustomDateRangePicker = true
                                        }
                                    )
                                }
                            }

                            // 2. Media type
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterSectionLabel("Media type")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SearchMediaTypeFilter.entries
                                        .filter { it != SearchMediaTypeFilter.ALL }
                                        .forEach { type ->
                                            val isPicked = filters.mediaType == type
                                            SelectableFilterPill(
                                                label = type.label,
                                                selected = isPicked,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onUpdateFilters {
                                                        it.copy(
                                                            mediaType = if (isPicked) SearchMediaTypeFilter.ALL else type
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                }
                            }

                            // 3. Document type
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterSectionLabel("Document type")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SearchDocumentTypeOption.entries.forEach { doc ->
                                        val isPicked = doc.categoryKey in filters.documentCategories
                                        SelectableFilterPill(
                                            label = doc.label,
                                            selected = isPicked,
                                            showCheckWhenSelected = true,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onUpdateFilters { state ->
                                                    val next = if (isPicked) {
                                                        state.documentCategories - doc.categoryKey
                                                    } else {
                                                        state.documentCategories + doc.categoryKey
                                                    }
                                                    state.copy(documentCategories = next)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. People
                            if (availablePeople.isNotEmpty()) {
                                val context = LocalContext.current
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterSectionLabel("People")
                                        if (filters.personIds.size > 1) {
                                            Text(
                                                text = "Together in photo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(availablePeople, key = { it.id }) { person ->
                                            val isPicked = person.id in filters.personIds
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .clip(ShapeLarge)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onUpdateFilters { state ->
                                                            val next = if (person.id in state.personIds) {
                                                                state.personIds - person.id
                                                            } else {
                                                                state.personIds + person.id
                                                            }
                                                            state.copy(personIds = next)
                                                        }
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            width = if (isPicked) 2.5.dp else 1.dp,
                                                            color = if (isPicked) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.outlineVariant,
                                                            shape = CircleShape
                                                        )
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val cropPath = person.coverCropPath
                                                    if (!cropPath.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(context)
                                                                .data(File(cropPath))
                                                                .size(160)
                                                                .build(),
                                                            contentDescription = person.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    if (isPicked) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = person.name?.takeIf { it.isNotBlank() } ?: "Person",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isPicked) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Album & Location grouped drill-in rows
                            val hasAlbums = availableAlbums.isNotEmpty() || filters.albumNames.isNotEmpty()
                            val hasLocations = availableLocations.isNotEmpty() || !filters.locationName.isNullOrBlank()
                            if (hasAlbums || hasLocations) {
                                Surface(
                                    shape = ShapeLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        if (hasAlbums) {
                                            val sortedAlbums = filters.albumNames.sorted()
                                            val albumSummary = when (sortedAlbums.size) {
                                                0 -> "Any"
                                                1 -> sortedAlbums.first()
                                                else -> "${sortedAlbums.first()} +${sortedAlbums.size - 1}"
                                            }
                                            FilterDrillInRow(
                                                iconRes = R.drawable.ic_ms_photo_album,
                                                title = "Album",
                                                value = albumSummary,
                                                hasValue = filters.albumNames.isNotEmpty(),
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    currentPage = SearchFilterSheetPage.ALBUMS
                                                },
                                                onClear = if (filters.albumNames.isNotEmpty()) {
                                                    {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onUpdateFilters { it.copy(albumNames = emptySet()) }
                                                    }
                                                } else null
                                            )
                                        }
                                        if (hasAlbums && hasLocations) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 14.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            )
                                        }
                                        if (hasLocations) {
                                            val hasLocation = !filters.locationName.isNullOrBlank()
                                            FilterDrillInRow(
                                                iconRes = R.drawable.ic_ms_location_on,
                                                title = "Location",
                                                value = if (hasLocation) filters.locationName!! else "Any",
                                                hasValue = hasLocation,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    currentPage = SearchFilterSheetPage.LOCATIONS
                                                },
                                                onClear = if (hasLocation) {
                                                    {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onUpdateFilters { it.copy(locationName = null) }
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismiss,
                            shape = ShapeFull,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (filters.isActive) "Show results" else "Done",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                SearchFilterSheetPage.ALBUMS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubPageHeader(
                            title = "Albums",
                            hasSelection = filters.albumNames.isNotEmpty(),
                            onBack = { currentPage = SearchFilterSheetPage.MAIN },
                            onClear = { onUpdateFilters { it.copy(albumNames = emptySet()) } }
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableAlbums, key = { it.bucketName }) { album ->
                                val isPicked = album.bucketName in filters.albumNames
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateFilters { state ->
                                            val next = if (isPicked) {
                                                state.albumNames - album.bucketName
                                            } else {
                                                state.albumNames + album.bucketName
                                            }
                                            state.copy(albumNames = next)
                                        }
                                    },
                                    shape = ShapeLarge,
                                    color = if (isPicked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = album.bucketName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isPicked) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${album.itemCount}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isPicked) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (initialPage == SearchFilterSheetPage.ALBUMS) onDismiss()
                                else currentPage = SearchFilterSheetPage.MAIN
                            },
                            shape = ShapeFull,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                SearchFilterSheetPage.LOCATIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SubPageHeader(
                            title = "Location",
                            hasSelection = !filters.locationName.isNullOrBlank(),
                            onBack = { currentPage = SearchFilterSheetPage.MAIN },
                            onClear = {
                                onUpdateFilters { it.copy(locationName = null) }
                                if (initialPage == SearchFilterSheetPage.LOCATIONS) onDismiss()
                                else currentPage = SearchFilterSheetPage.MAIN
                            }
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableLocations, key = { it }) { place ->
                                val isPicked = filters.locationName == place
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateFilters {
                                            it.copy(locationName = if (isPicked) null else place)
                                        }
                                        if (initialPage == SearchFilterSheetPage.LOCATIONS) onDismiss()
                                        else currentPage = SearchFilterSheetPage.MAIN
                                    },
                                    shape = ShapeLarge,
                                    color = if (isPicked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_location_on),
                                            contentDescription = null,
                                            tint = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = place,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isPicked) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isPicked) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
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
    }

    // Native M3 DateRangePicker Dialog (UTC-midnight converted to local full-day range)
    if (showCustomDateRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = filters.dateStartSec?.times(1000L),
            initialSelectedEndDateMillis = filters.dateEndSec?.times(1000L)
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMs = pickerState.selectedStartDateMillis
                        val endMs = pickerState.selectedEndDateMillis ?: startMs
                        if (startMs != null && endMs != null) {
                            val (startSec, endSec) = SearchFilterQueryBuilder
                                .utcPickerRangeToLocalEpochSec(startMs, endMs)
                            val label = SearchFilterQueryBuilder
                                .formatUtcPickerRangeLabel(startMs, endMs)
                            onUpdateFilters {
                                it.copy(
                                    dateStartSec = startSec,
                                    dateEndSec = endSec,
                                    dateLabel = label
                                )
                            }
                        }
                        showCustomDateRangePicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                modifier = Modifier.height(480.dp)
            )
        }
    }
}

@Composable
private fun ActiveFilterPill(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                    contentDescription = "Clear $label",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectableFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIconRes: Int? = null,
    showCheckWhenSelected: Boolean = false
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        label = "selectableFilterPillBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "selectableFilterPillFg"
    )
    Surface(
        onClick = onClick,
        shape = ShapeFull,
        color = bg,
        contentColor = fg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val iconToShow = when {
                selected && showCheckWhenSelected -> R.drawable.ic_ms_check
                leadingIconRes != null -> leadingIconRes
                else -> null
            }
            if (iconToShow != null) {
                Icon(
                    imageVector = ImageVector.vectorResource(iconToShow),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun FilterDrillInRow(
    iconRes: Int,
    title: String,
    value: String,
    hasValue: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = if (hasValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (hasValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (hasValue && onClear != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                    contentDescription = "Clear $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SubPageHeader(
    title: String,
    hasSelection: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                contentDescription = "Back to filters",
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (hasSelection) {
            TextButton(onClick = onClear, shape = ShapeFull) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
