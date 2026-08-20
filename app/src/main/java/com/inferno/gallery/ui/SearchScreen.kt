package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.components.PhotonSectionHeader
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import com.inferno.gallery.ui.people.PeopleCarousel
import androidx.compose.ui.graphics.vector.ImageVector


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onAlbumClick: (String) -> Unit = {},
    onPersonClick: (Long) -> Unit = {},
    onViewAllPeopleClick: () -> Unit = {}
) {
    val query by viewModel.searchQuery.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val ftsResults by viewModel.ftsSearchResults.collectAsState()
    val smartResults by viewModel.smartSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val haptic = LocalHapticFeedback.current

    val hasAnyResults = ftsResults.isNotEmpty() || smartResults.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Active Search Bar (Filled Tonal Container) ──────────────────────────
        val focusRequester = remember { FocusRequester() }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MagicSearchIcon(
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search photos, dates, places, text…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateSearchQuery("")
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ── Results Area ────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = Triple(query.isBlank(), isSearching, hasAnyResults),
            transitionSpec = {
                (fadeIn(MotionTokens.gentleSpring()) +
                    scaleIn(MotionTokens.gentleSpring(), initialScale = 0.95f))
                    .togetherWith(fadeOut(MotionTokens.gentleSpring()) +
                        scaleOut(MotionTokens.gentleSpring(), targetScale = 0.95f))
            },
            label = "searchContentTransition",
            modifier = Modifier.fillMaxSize()
        ) { (isBlank, searching, hasResults) ->
            when {
                query.isBlank() -> EmptySearchState(
                    recentSearches = recentSearches,
                    viewModel = viewModel,
                    onPersonClick = onPersonClick,
                    onViewAllPeopleClick = onViewAllPeopleClick
                )
                searching -> SearchingState()
                !hasResults -> NoResultsState(query = query, onClear = { viewModel.updateSearchQuery("") })
                else -> SearchResultsList(
                    ftsResults = ftsResults,
                    smartResults = smartResults,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    onAlbumClick = onAlbumClick,
                    query = query,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    recentSearches: List<String>,
    viewModel: GalleryViewModel,
    onPersonClick: (Long) -> Unit = {},
    onViewAllPeopleClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val personClusters by viewModel.personClusters.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── People Carousel ──
        if (personClusters.isNotEmpty()) {
            item {
                PeopleCarousel(
                    clusters = personClusters,
                    onPersonClick = onPersonClick,
                    onViewAllClick = onViewAllPeopleClick
                )
            }
        }

        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearRecentSearches()
                        },
                        shape = ShapeFull,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(recentSearches.size) { index ->
                val recentQuery = recentSearches[index]
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.updateSearchQuery(recentQuery)
                    },
                    shape = ShapeLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_history),
                            contentDescription = "Recent",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = recentQuery,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSearchQuery(recentQuery)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_north_west),
                                contentDescription = "Insert query",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else if (personClusters.isEmpty()) {
            item {
                PhotonEmptyState(
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_search),
                    title = "Search photos & videos",
                    subtitle = "Search by date, place, text inside photos, or describe any scene.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContainedLoadingIndicator(
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Searching…",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(query: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = ShapeExtraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "No results for “$query”",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Try different keywords, dates, or visual scene descriptions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            FilledTonalButton(
                onClick = onClear,
                shape = ShapeFull,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = "Clear search",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SearchResultsList(
    ftsResults: List<GalleryItem>,
    smartResults: List<GalleryItem>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    onAlbumClick: (String) -> Unit,
    query: String,
    viewModel: GalleryViewModel
) {
    val haptic = LocalHapticFeedback.current
    val onFtsClick = remember(onPhotoClick, query, viewModel) {
        { item: GalleryItem ->
            viewModel.setInitialDetailItem(item)
            onPhotoClick(item.id, "search_text", query)
        }
    }
    val onSmartClick = remember(onPhotoClick, query, viewModel) {
        { item: GalleryItem ->
            viewModel.setInitialDetailItem(item)
            onPhotoClick(item.id, "search_smart", query)
        }
    }

    val lazyGridState = rememberLazyGridState()

    LaunchedEffect(lazyGridState) {
        var previousIndex = 0
        var previousScrollOffset = 0
        snapshotFlow {
            lazyGridState.firstVisibleItemIndex to lazyGridState.firstVisibleItemScrollOffset
        }
        .collect { (index, offset) ->
            if (lazyGridState.isScrollInProgress) {
                when {
                    index > previousIndex              -> viewModel.setScrollDockVisible(false)
                    index < previousIndex              -> viewModel.setScrollDockVisible(true)
                    offset > previousScrollOffset + 15 -> viewModel.setScrollDockVisible(false)
                    offset < previousScrollOffset - 15 -> viewModel.setScrollDockVisible(true)
                }
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    val totalMatches = ftsResults.size + smartResults.size

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Total Matches Header Banner (Single Dominant Count Indicator)
        item(span = { GridItemSpan(4) }) {
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "$totalMatches",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (totalMatches == 1) "Result found for “$query”" else "Results found for “$query”",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 1. Text Matches Section (OCR)
        if (ftsResults.isNotEmpty()) {
            item(span = { GridItemSpan(4) }) {
                PhotonSectionHeader(
                    title = "Text matches"
                )
            }
            items(
                items = ftsResults.take(8),
                key = { "fts_grid_${it.id}" }
            ) { item ->
                com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                    modifier = Modifier.animateItem(),
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onFtsClick
                )
            }
            if (ftsResults.size > 8) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAlbumClick("search_text")
                            },
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Show all text matches",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }

        // Section separator spacer
        if (ftsResults.isNotEmpty() && smartResults.isNotEmpty()) {
            item(span = { GridItemSpan(4) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 2. Semantic Matches Section (Smart Search)
        if (smartResults.isNotEmpty()) {
            item(span = { GridItemSpan(4) }) {
                PhotonSectionHeader(
                    title = "Semantic matches"
                )
            }
            items(
                items = smartResults.take(8),
                key = { "smart_grid_${it.id}" }
            ) { item ->
                com.inferno.gallery.ui.components.OptimizedThumbnailCell(
                    modifier = Modifier.animateItem(),
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = onSmartClick
                )
            }
            if (smartResults.size > 8) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAlbumClick("search_smart")
                            },
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Show all semantic matches",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}
