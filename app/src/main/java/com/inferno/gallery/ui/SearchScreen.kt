package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.tween
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val query by viewModel.searchQuery.collectAsState()
    val semanticResults by viewModel.semanticSearchResults.collectAsState()
    val ftsResults by viewModel.ftsSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val clipIndexWorkInfo by viewModel.clipIndexWorkInfo.collectAsState(initial = null)
    val ocrIndexWorkInfo by viewModel.ocrIndexWorkInfo.collectAsState(initial = null)
    val unindexedClipCount by viewModel.unindexedClipImagesCount.collectAsState()
    val unindexedOcrCount by viewModel.unindexedOcrImagesCount.collectAsState()
    val totalImagesCount by viewModel.totalImagesCount.collectAsState()

    val hasAnyResults = semanticResults.isNotEmpty() || ftsResults.isNotEmpty()

    var focusedList by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        focusedList = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // ── Search Bar ──────────────────────────────────────────────────────────
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { /* Auto-search handles this via debounce */ },
                    expanded = false,
                    onExpandedChange = {},
                    leadingIcon = {
                        MagicSearchIcon(modifier = Modifier.size(24.dp))
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    placeholder = {
                        Text("Search photos, scenes, or text…")
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        // ── Results Area ────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = Triple(query.isBlank(), isSearching, hasAnyResults),
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.95f))
                    .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleOut(spring(stiffness = Spring.StiffnessMediumLow), targetScale = 0.95f))
            },
            modifier = Modifier.fillMaxSize()
        ) { (isBlank, searching, hasResults) ->
            when {
                isBlank -> EmptySearchState(
                    clipIndexWorkInfo = clipIndexWorkInfo,
                    ocrIndexWorkInfo = ocrIndexWorkInfo,
                    unindexedClipCount = unindexedClipCount,
                    unindexedOcrCount = unindexedOcrCount,
                    totalCount = totalImagesCount,
                    onStartClip = { viewModel.startClipIndexing() },
                    onStopClip = { viewModel.stopClipIndexing() },
                    onClearClip = { viewModel.clearClipIndexAndReindex() },
                    onStartOcr = { viewModel.startOcrIndexing() },
                    onStopOcr = { viewModel.stopOcrIndexing() },
                    onClearOcr = { viewModel.clearOcrIndexAndReindex() }
                )
                searching -> SearchingState()
                !hasResults -> NoResultsState(query)
                else -> UnifiedSearchResults(
                    semanticResults = semanticResults,
                    ftsResults = ftsResults,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onPhotoClick = onPhotoClick,
                    query = query,
                    focusedList = focusedList,
                    onFocusedListChange = { focusedList = it }
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    clipIndexWorkInfo: WorkInfo?,
    ocrIndexWorkInfo: WorkInfo?,
    unindexedClipCount: Int,
    unindexedOcrCount: Int,
    totalCount: Int,
    onStartClip: () -> Unit,
    onStopClip: () -> Unit,
    onClearClip: () -> Unit,
    onStartOcr: () -> Unit,
    onStopOcr: () -> Unit,
    onClearOcr: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "Find anything",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Describe a scene, a feeling,\nor text you remember seeing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndexingCard(
                    title = "Visual Search",
                    workInfo = clipIndexWorkInfo,
                    unindexedCount = unindexedClipCount,
                    totalCount = totalCount,
                    onStart = onStartClip,
                    onStop = onStopClip,
                    onClearAndReindex = onClearClip,
                    modifier = Modifier.weight(1f)
                )
                IndexingCard(
                    title = "Text Search",
                    workInfo = ocrIndexWorkInfo,
                    unindexedCount = unindexedOcrCount,
                    totalCount = totalCount,
                    onStart = onStartOcr,
                    onStop = onStopOcr,
                    onClearAndReindex = onClearOcr,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(160.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = "Thinking…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No results for",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "\u201c$query\u201d",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Try different words or descriptions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UnifiedSearchResults(
    semanticResults: List<GalleryItem>,
    ftsResults: List<GalleryItem>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPhotoClick: (String, String?, String?) -> Unit,
    query: String,
    focusedList: String?,
    onFocusedListChange: (String?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (focusedList == "text") {
            // Header for focused text search results
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onFocusedListChange(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Images containing text \"$query\"",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${ftsResults.size} images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // All items
            items(
                items = ftsResults,
                key = { "fts_focused_${it.id}" }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { onPhotoClick(item.id, "search_text", query) }
                )
            }
        } else if (focusedList == "visual") {
            // Header for focused visual search results
            item(span = { GridItemSpan(3) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onFocusedListChange(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Visual Matches",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${semanticResults.size} images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // All items
            items(
                items = semanticResults,
                key = { "ai_focused_${it.id}" }
            ) { item ->
                GalleryGridItem(
                    item = item,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { onPhotoClick(item.id, "search_visual", query) }
                )
            }
        } else {
            // ── Text Matches (FTS/OCR) ── horizontal filmstrip at top
            if (ftsResults.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        // Header 1: [T] Images containing text "query"      count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFocusedListChange("text") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Images containing text \"$query\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${ftsResults.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Header 2: count images     View all >
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${ftsResults.size} images",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onFocusedListChange("text") }
                            ) {
                                Text(
                                    text = "View all",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Grid of images
                val displayResults = ftsResults.take(6)
                items(
                    items = displayResults,
                    key = { "fts_grid_${it.id}" }
                ) { item ->
                    GalleryGridItem(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onPhotoClick(item.id, "search_text", query) }
                    )
                }

                // Text recognition bottom card
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                        Text(
                            text = "Text recognition",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onFocusedListChange("text") },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ftsResults.first().uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = query,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${ftsResults.size}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Visual Matches (AI Semantic) ── standard grid
            if (semanticResults.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFocusedListChange("visual") }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ImageSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Visual Matches",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${semanticResults.size} images",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(
                    items = semanticResults,
                    key = { "ai_${it.id}" }
                ) { item ->
                    GalleryGridItem(
                        item = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onPhotoClick(item.id, "search_visual", query) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IndexingCard(
    title: String,
    workInfo: WorkInfo?,
    unindexedCount: Int,
    totalCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearAndReindex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED
    val progress = workInfo?.progress
    val dbIndexed = totalCount - unindexedCount
    val workerIndexed = progress?.getInt("progress", 0) ?: 0
    val indexed = maxOf(dbIndexed, workerIndexed)
    val total = if (totalCount > 0) totalCount else (progress?.getInt("total", 0) ?: 0)

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isRunning) {
                FilledTonalButton(onClick = onStop, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("Stop", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(onClick = onStart, modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("Start", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            val progressFloat = if (total > 0) indexed.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$indexed / $total", 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClearAndReindex,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentPadding = PaddingValues(0.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Text("Reindex", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
