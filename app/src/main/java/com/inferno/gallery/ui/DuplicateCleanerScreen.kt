@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.inferno.gallery.ui

import android.app.Activity
import android.provider.MediaStore
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.inferno.gallery.R
import com.inferno.gallery.ui.components.ExpressiveFilledIconButton
import com.inferno.gallery.ui.components.PhotonEmptyState
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.WavyProgressTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CleanerTab(val label: String) {
    ALL("All"),
    SIMILAR("Similar"),
    EXACT("Exact"),
    BLURRY("Blurry")
}

private data class InspectPhotoTarget(
    val item: GalleryItem,
    val pixelCount: Long = 0L,
    val sharpness: Float = 0f,
    val badge: String? = null
)

@Composable
fun DuplicateCleanerScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val exactDuplicates by viewModel.duplicates.collectAsState()
    val similarGroups by viewModel.similarPhotoGroups.collectAsState()
    val blurryPhotos by viewModel.blurryPhotos.collectAsState()
    val totalReclaimableAvailable by viewModel.totalCleanupReclaimableBytes.collectAsState()
    val scanState by viewModel.duplicateScanState.collectAsState()

    var activeTab by remember { mutableStateOf(CleanerTab.ALL) }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var comparingGroup by remember { mutableStateOf<Pair<SimilarPhotoGroup, String?>?>(null) }
    var inspectingPhoto by remember { mutableStateOf<InspectPhotoTarget?>(null) }
    var showTrashConfirmDialog by remember { mutableStateOf(false) }

    // Start background exact + perceptual + sharpness scan on entry
    LaunchedEffect(Unit) {
        viewModel.scanForDuplicates()
    }

    val allKnownItemsById = remember(exactDuplicates, similarGroups, blurryPhotos) {
        val map = LinkedHashMap<String, GalleryItem>()
        exactDuplicates.forEach { g -> g.items.forEach { map[it.id] = it } }
        similarGroups.forEach { g -> g.items.forEach { map[it.item.id] = it.item } }
        blurryPhotos.forEach { map[it.item.id] = it.item }
        map
    }

    // Smart Select: keeps original in exact duplicates + keeps Best Shot in every similar/burst group
    fun smartSelectRedundant() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedItemIds.clear()
        val picked = LinkedHashSet<String>()
        if (activeTab == CleanerTab.ALL || activeTab == CleanerTab.EXACT) {
            exactDuplicates.forEach { group ->
                group.items.drop(1).forEach { picked.add(it.id) }
            }
        }
        if (activeTab == CleanerTab.ALL || activeTab == CleanerTab.SIMILAR) {
            similarGroups.forEach { group ->
                group.items.filterNot { it.isBestShot }.forEach { picked.add(it.item.id) }
            }
        }
        if (activeTab == CleanerTab.BLURRY) {
            blurryPhotos.forEach { picked.add(it.item.id) }
        }
        selectedItemIds.addAll(picked)
    }

    fun toggleSelection(id: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (id in selectedItemIds) selectedItemIds.remove(id) else selectedItemIds.add(id)
    }

    val selectedItemsList = remember(selectedItemIds.size, allKnownItemsById) {
        selectedItemIds.mapNotNull { allKnownItemsById[it] }
    }
    val selectedReclaimableBytes = remember(selectedItemsList) {
        selectedItemsList.sumOf { it.size }
    }

    var pendingCleanerTrashUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val toTrash = pendingCleanerTrashUris
            if (toTrash.isNotEmpty()) {
                viewModel.trashSelectedMedia(toTrash)
            }
            selectedItemIds.clear()
            comparingGroup = null
            inspectingPhoto = null
            viewModel.scanForDuplicates()
        }
        pendingCleanerTrashUris = emptyList()
    }

    fun executeTrashSelected() {
        val urisToDelete = selectedItemsList.map {
            com.inferno.gallery.data.MediaUriUtils.normalizeMediaUri(it.uri)
        }
        pendingCleanerTrashUris = urisToDelete.map { it.toString() }
        if (urisToDelete.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val trashIntent = MediaStore.createTrashRequest(
                        context.contentResolver,
                        urisToDelete,
                        true
                    )
                    val request = IntentSenderRequest.Builder(trashIntent.intentSender).build()
                    withContext(Dispatchers.Main) {
                        trashLauncher.launch(request)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val hasAnyFindings = exactDuplicates.isNotEmpty() || similarGroups.isNotEmpty() || blurryPhotos.isNotEmpty()
    val showSimilar = (activeTab == CleanerTab.ALL || activeTab == CleanerTab.SIMILAR) && similarGroups.isNotEmpty()
    val showExact = (activeTab == CleanerTab.ALL || activeTab == CleanerTab.EXACT) && exactDuplicates.isNotEmpty()
    val showBlurry = (activeTab == CleanerTab.ALL || activeTab == CleanerTab.BLURRY) && blurryPhotos.isNotEmpty()
    val visibleTabHasContent = showExact || showSimilar || showBlurry
    val multipleSectionsVisible = listOf(showSimilar, showExact, showBlurry).count { it } > 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.utility_clean_up),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasAnyFindings) {
                            Text(
                                text = "${Formatter.formatShortFileSize(context, totalReclaimableAvailable)} reclaimable",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    ExpressiveFilledIconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(IconSizeTokens.L)
                        )
                    }
                },
                actions = {
                    if (hasAnyFindings) {
                        Surface(
                            onClick = { smartSelectRedundant() },
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Smart Select",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (scanState is DuplicateScanState.Scanning) {
                        CircularWavyProgressIndicator(
                            wavelength = WavyProgressTokens.CircularWavelength,
                            waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(22.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ExpressiveFilledIconButton(
                            onClick = { viewModel.scanForDuplicates() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                                contentDescription = "Rescan",
                                modifier = Modifier.size(IconSizeTokens.L)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedItemIds.isNotEmpty(),
                enter = slideInVertically(MotionTokens.snappySpring()) { it } + fadeIn(),
                exit = slideOutVertically(MotionTokens.snappySpring()) { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedItemIds.clear()
                                    }
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                        contentDescription = "Clear selection",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${selectedItemIds.size} selected",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Free ${Formatter.formatShortFileSize(context, selectedReclaimableBytes)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showTrashConfirmDialog = true },
                                shape = ShapeFull,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    ImageVector.vectorResource(R.drawable.ic_ms_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.dialog_move_to_trash_confirm),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Subtle inline progress strip when rescanning over existing findings
            if (scanState is DuplicateScanState.Scanning && hasAnyFindings) {
                val state = scanState as DuplicateScanState.Scanning
                val progress = if (state.total > 0) (state.processed.toFloat() / state.total).coerceIn(0f, 1f) else 0f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.stage.ifBlank { "Analyzing library…" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.total > 0) {
                            Text(
                                text = "${state.processed}/${state.total}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.total > 0) {
                        LinearWavyProgressIndicator(
                            progress = { progress },
                            wavelength = WavyProgressTokens.LinearWavelength,
                            waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    } else {
                        LinearWavyProgressIndicator(
                            wavelength = WavyProgressTokens.LinearWavelength,
                            waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            // Minimal Borderless Category Strip
            if (hasAnyFindings) {
                val chipCounts = mapOf(
                    CleanerTab.ALL to (exactDuplicates.size + similarGroups.size + blurryPhotos.size),
                    CleanerTab.SIMILAR to similarGroups.size,
                    CleanerTab.EXACT to exactDuplicates.size,
                    CleanerTab.BLURRY to blurryPhotos.size
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CleanerTab.entries) { tab ->
                        val count = chipCounts[tab] ?: 0
                        val selected = activeTab == tab
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer,
                            label = "cleanerTabBg"
                        )
                        val fg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "cleanerTabFg"
                        )
                        Surface(
                            onClick = { activeTab = tab },
                            shape = ShapeFull,
                            color = bg,
                            contentColor = fg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                                if (count > 0) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = fg.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Content or Scanning / Empty State
            if (!visibleTabHasContent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    if (scanState is DuplicateScanState.Scanning) {
                        val state = scanState as DuplicateScanState.Scanning
                        val progress = if (state.total > 0) (state.processed.toFloat() / state.total).coerceIn(0f, 1f) else 0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularWavyProgressIndicator(
                                        wavelength = WavyProgressTokens.CircularWavelength,
                                        waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                                        modifier = Modifier.size(68.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_cleaning_services),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Scanning library…",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (state.total > 0) {
                                    "${state.stage.ifBlank { "Analyzing" }} (${state.processed} of ${state.total})"
                                } else {
                                    "Checking duplicates, burst shots, and blurry photos offline"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (state.total > 0) {
                                LinearWavyProgressIndicator(
                                    progress = { progress },
                                    wavelength = WavyProgressTokens.LinearWavelength,
                                    waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(8.dp)
                                )
                            } else {
                                LinearWavyProgressIndicator(
                                    wavelength = WavyProgressTokens.LinearWavelength,
                                    waveSpeed = WavyProgressTokens.CalmWaveSpeed,
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(8.dp)
                                )
                            }
                        }
                    } else {
                        PhotonEmptyState(
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_check_circle),
                            title = if (hasAnyFindings) "None in ${activeTab.label}" else "Library looks clean",
                            subtitle = if (hasAnyFindings) {
                                "No ${activeTab.label.lowercase()} items detected."
                            } else {
                                "No duplicate, burst, or blurry shots found."
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = paddingValues.calculateBottomPadding() + if (selectedItemIds.isNotEmpty()) 96.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showSimilar) {
                        if (multipleSectionsVisible) {
                            item(key = "section_similar") {
                                QuietSectionHeader(
                                    title = "Similar & burst",
                                    trailing = "${similarGroups.size} groups"
                                )
                            }
                        }
                        items(similarGroups, key = { it.id }) { group ->
                            SimilarGroupSection(
                                group = group,
                                selectedItemIds = selectedItemIds,
                                onToggleSelection = { toggleSelection(it.id) },
                                onCompareClick = { focusId -> comparingGroup = group to focusId }
                            )
                        }
                    }

                    if (showExact) {
                        if (multipleSectionsVisible) {
                            item(key = "section_exact") {
                                QuietSectionHeader(
                                    title = "Exact copies",
                                    trailing = "${exactDuplicates.size} sets"
                                )
                            }
                        }
                        items(exactDuplicates, key = { it.fileHash }) { group ->
                            ExactDuplicateSection(
                                group = group,
                                selectedItemIds = selectedItemIds,
                                onToggleSelection = { toggleSelection(it.id) },
                                onInspect = { item, isKeep ->
                                    inspectingPhoto = InspectPhotoTarget(
                                        item = item,
                                        badge = if (isKeep) "Original copy" else "Exact copy"
                                    )
                                }
                            )
                        }
                    }

                    if (showBlurry) {
                        if (multipleSectionsVisible) {
                            item(key = "section_blurry") {
                                QuietSectionHeader(
                                    title = "Blurry shots",
                                    trailing = "${blurryPhotos.size} photos"
                                )
                            }
                        }
                        val blurryRows = blurryPhotos.chunked(3)
                        items(
                            items = blurryRows,
                            key = { row -> "blurry_row_${row.first().item.id}" }
                        ) { rowItems ->
                            BlurryPhotoRow(
                                rowItems = rowItems,
                                selectedItemIds = selectedItemIds,
                                onToggleSelection = { toggleSelection(it.id) },
                                onInspect = { entry ->
                                    inspectingPhoto = InspectPhotoTarget(
                                        item = entry.item,
                                        pixelCount = entry.pixelCount,
                                        sharpness = entry.sharpness,
                                        badge = "Low sharpness"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Side-by-Side Comparison Sheet for Similar / Burst groups
    comparingGroup?.let { (group, initialCandidateId) ->
        SimilarComparisonSheet(
            group = group,
            initialCandidateId = initialCandidateId,
            selectedItemIds = selectedItemIds,
            onToggleSelection = { toggleSelection(it.id) },
            onInspectFull = { entry ->
                inspectingPhoto = InspectPhotoTarget(
                    item = entry.item,
                    pixelCount = entry.pixelCount,
                    sharpness = entry.sharpness,
                    badge = if (entry.isBestShot) "Best Shot" else null
                )
            },
            onDismiss = { comparingGroup = null }
        )
    }

    // Quick Pinch-to-Zoom Inspect Sheet (long-press any photo to judge sharpness/details)
    inspectingPhoto?.let { target ->
        CleanupInspectSheet(
            target = target,
            isSelectedForTrash = target.item.id in selectedItemIds,
            onToggleTrash = { toggleSelection(target.item.id) },
            onDismiss = { inspectingPhoto = null }
        )
    }

    // Confirmation dialog before trashing selected cleanup items
    if (showTrashConfirmDialog && selectedItemIds.isNotEmpty()) {
        val count = selectedItemIds.size
        val sizeStr = Formatter.formatShortFileSize(context, selectedReclaimableBytes)
        AlertDialog(
            onDismissRequest = { showTrashConfirmDialog = false },
            shape = ShapeExtraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            icon = {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = if (count == 1) "Move 1 photo to Trash?" else "Move $count photos to Trash?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This will free up $sizeStr. You can still restore these items from Trash later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTrashConfirmDialog = false
                        executeTrashSelected()
                    },
                    shape = ShapeFull,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.dialog_move_to_trash_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTrashConfirmDialog = false },
                    shape = ShapeFull
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun QuietSectionHeader(
    title: String,
    trailing: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = trailing,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SimilarGroupSection(
    group: SimilarPhotoGroup,
    selectedItemIds: List<String>,
    onToggleSelection: (GalleryItem) -> Unit,
    onCompareClick: (focusId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reclaimableBytes = remember(group) {
        group.items.filterNot { it.isBestShot }.sumOf { it.item.size }
    }

    Surface(
        shape = ShapeLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Clean 1-line header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (group.isBurst) "Burst · ${group.items.size}" else "${group.items.size} similar",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "· ${Formatter.formatShortFileSize(context, reclaimableBytes)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { onCompareClick(null) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_compare),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Compare",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clean horizontal strip with details BELOW each photo
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(group.items, key = { it.item.id }) { entry ->
                    val item = entry.item
                    val isSelected = item.id in selectedItemIds
                    val sharpLabel = formatSharpnessWord(entry.sharpness)
                    val mpLabel = formatMegapixels(entry.pixelCount)
                    val line1 = listOf(
                        Formatter.formatShortFileSize(context, item.size),
                        sharpLabel
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    val line2 = listOf(mpLabel, item.bucketName.ifBlank { null })
                        .filterNotNull()
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")

                    CleanupPhotoColumnTile(
                        uri = item.resolvedUri,
                        contentDescription = item.name,
                        isSelectedForTrash = isSelected,
                        topBadge = if (entry.isBestShot && !isSelected) "Best" else null,
                        isPrimaryBadge = entry.isBestShot,
                        primaryLine = line1,
                        secondaryLine = line2,
                        onClick = { onToggleSelection(item) },
                        onLongClick = { onCompareClick(if (entry.isBestShot) null else item.id) },
                        modifier = Modifier.width(142.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExactDuplicateSection(
    group: DuplicateGroup,
    selectedItemIds: List<String>,
    onToggleSelection: (GalleryItem) -> Unit,
    onInspect: (GalleryItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reclaimableBytes = remember(group) {
        group.items.drop(1).sumOf { it.size }
    }

    Surface(
        shape = ShapeLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${group.items.size} exact copies",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Free ${Formatter.formatShortFileSize(context, reclaimableBytes)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(group.items, key = { it.id }) { item ->
                    val isSelected = item.id in selectedItemIds
                    val isOriginal = item == group.items.first()
                    val folderLabel = item.bucketName.ifBlank { "Device" }
                    val dateLabel = formatShortDate(item.dateAdded)
                    val metaLine = listOf(
                        Formatter.formatShortFileSize(context, item.size),
                        dateLabel
                    ).filter { it.isNotBlank() }.joinToString(" · ")

                    CleanupPhotoColumnTile(
                        uri = item.resolvedUri,
                        contentDescription = item.name,
                        isSelectedForTrash = isSelected,
                        topBadge = if (isOriginal && !isSelected) "Keep" else null,
                        isPrimaryBadge = isOriginal,
                        primaryLine = folderLabel,
                        secondaryLine = metaLine,
                        onClick = { onToggleSelection(item) },
                        onLongClick = { onInspect(item, isOriginal) },
                        modifier = Modifier.width(142.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlurryPhotoRow(
    rowItems: List<BlurryPhotoItem>,
    selectedItemIds: List<String>,
    onToggleSelection: (GalleryItem) -> Unit,
    onInspect: (BlurryPhotoItem) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rowItems.forEach { entry ->
            val item = entry.item
            val isSelected = item.id in selectedItemIds
            CleanupPhotoColumnTile(
                uri = item.resolvedUri,
                contentDescription = item.name,
                isSelectedForTrash = isSelected,
                topBadge = null,
                isPrimaryBadge = false,
                primaryLine = Formatter.formatShortFileSize(context, item.size),
                secondaryLine = item.bucketName.ifBlank { formatShortDate(item.dateAdded) },
                aspectRatio = 0.92f,
                onClick = { onToggleSelection(item) },
                onLongClick = { onInspect(entry) },
                modifier = Modifier.weight(1f)
            )
        }
        repeat(3 - rowItems.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Clean photography-first tile: photo stays unobstructed with only a minimal corner selection ring
 * (and optional tiny Best/Keep tag); file size, sharpness, resolution, and folder live neatly below.
 */
@Composable
private fun CleanupPhotoColumnTile(
    uri: android.net.Uri,
    contentDescription: String,
    isSelectedForTrash: Boolean,
    topBadge: String?,
    isPrimaryBadge: Boolean,
    primaryLine: String,
    secondaryLine: String,
    aspectRatio: Float = 0.88f,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageScale by animateFloatAsState(
        targetValue = if (isSelectedForTrash) 0.96f else 1f,
        animationSpec = MotionTokens.snappySpring(),
        label = "cleanupTileScale"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelectedForTrash -> MaterialTheme.colorScheme.error
            isPrimaryBadge && topBadge != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "cleanupTileBorder"
    )

    Column(
        modifier = modifier
            .clip(ShapeMedium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .graphicsLayer {
                    scaleX = imageScale
                    scaleY = imageScale
                }
                .clip(ShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (borderColor != Color.Transparent) {
                        Modifier.border(2.dp, borderColor, ShapeMedium)
                    } else Modifier
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .size(320)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle red wash when queued for Trash so kept vs trashed is unmistakable
            if (isSelectedForTrash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.22f))
                )
            }

            // Minimal Best / Keep pill (top-start only, never cluttered)
            if (topBadge != null) {
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = topBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Clean selection circle (top-end)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelectedForTrash) MaterialTheme.colorScheme.error
                        else Color.Black.copy(alpha = 0.34f)
                    )
                    .border(
                        width = if (isSelectedForTrash) 0.dp else 1.5.dp,
                        color = if (isSelectedForTrash) Color.Transparent else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectedForTrash) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                        contentDescription = "Marked for Trash",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Clean metadata strictly BELOW the photo (unobstructed image + full decision clarity)
        Column(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = primaryLine,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (isSelectedForTrash) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondaryLine.isNotBlank()) {
                Text(
                    text = secondaryLine,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SimilarComparisonSheet(
    group: SimilarPhotoGroup,
    initialCandidateId: String?,
    selectedItemIds: List<String>,
    onToggleSelection: (GalleryItem) -> Unit,
    onInspectFull: (SimilarPhotoItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bestShot = remember(group) { group.items.firstOrNull { it.isBestShot } ?: group.items.first() }
    val others = remember(group) { group.items.filter { it.item.id != bestShot.item.id } }
    var activeCandidate by remember(group, initialCandidateId) {
        mutableStateOf(
            others.firstOrNull { it.item.id == initialCandidateId }
                ?: others.firstOrNull()
                ?: bestShot
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Compare shots",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap a photo to zoom · ${group.items.size} in group",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Two-column Side-by-Side Comparison Panels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ComparePhotoPanel(
                    entry = bestShot,
                    peer = activeCandidate,
                    label = "Best Shot",
                    isHighlightedBest = true,
                    isSelectedForTrash = bestShot.item.id in selectedItemIds,
                    onToggleTrash = { onToggleSelection(bestShot.item) },
                    onZoomInspect = { onInspectFull(bestShot) },
                    modifier = Modifier.weight(1f)
                )
                ComparePhotoPanel(
                    entry = activeCandidate,
                    peer = bestShot,
                    label = "Candidate",
                    isHighlightedBest = false,
                    isSelectedForTrash = activeCandidate.item.id in selectedItemIds,
                    onToggleTrash = { onToggleSelection(activeCandidate.item) },
                    onZoomInspect = { onInspectFull(activeCandidate) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Candidate filmstrip when 3+ shots in the group
            if (others.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(others, key = { it.item.id }) { candidate ->
                        val isFocused = candidate.item.id == activeCandidate.item.id
                        val isMarked = candidate.item.id in selectedItemIds
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(ShapeMedium)
                                .border(
                                    width = if (isFocused) 2.5.dp else 1.dp,
                                    color = if (isFocused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = ShapeMedium
                                )
                                .clickable { activeCandidate = candidate }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(candidate.item.resolvedUri)
                                    .size(160)
                                    .build(),
                                contentDescription = candidate.item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isMarked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ComparePhotoPanel(
    entry: SimilarPhotoItem,
    peer: SimilarPhotoItem,
    label: String,
    isHighlightedBest: Boolean,
    isSelectedForTrash: Boolean,
    onToggleTrash: () -> Unit,
    onZoomInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mpText = formatMegapixels(entry.pixelCount).ifBlank { "—" }
    val sharpnessWord = formatSharpnessWord(entry.sharpness).ifBlank { "—" }
    val isSharper = entry.sharpness > peer.sharpness + 1.5f
    val isHigherRes = entry.pixelCount > peer.pixelCount

    Surface(
        shape = ShapeLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(ShapeMedium)
                    .background(Color.Black)
                    .clickable(onClick = onZoomInspect)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(entry.item.resolvedUri)
                        .size(720)
                        .build(),
                    contentDescription = entry.item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = ShapeFull,
                    color = if (isHighlightedBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                    else Color(0xA6121216),
                    contentColor = if (isHighlightedBest) MaterialTheme.colorScheme.onPrimaryContainer
                    else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Crisp comparison specs (winning metrics highlighted subtly in primary)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                CompareSpecRow(
                    key = "Size",
                    value = Formatter.formatShortFileSize(context, entry.item.size),
                    highlight = false
                )
                CompareSpecRow(
                    key = "Detail",
                    value = "$mpText · $sharpnessWord",
                    highlight = isSharper || isHigherRes
                )
                CompareSpecRow(
                    key = "Folder",
                    value = entry.item.bucketName.ifBlank { "Device" },
                    highlight = false
                )
            }

            FilledTonalButton(
                onClick = onToggleTrash,
                shape = ShapeFull,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSelectedForTrash) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isSelectedForTrash) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (isSelectedForTrash) R.drawable.ic_ms_delete else R.drawable.ic_ms_check_circle
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isSelectedForTrash) "Trash" else "Keep",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CompareSpecRow(
    key: String,
    value: String,
    highlight: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Pinch-to-zoom full photo inspection sheet (opened by long-pressing any cleanup tile or tapping
 * a photo inside Compare) so the user can check sharpness, details, and folder before trashing.
 */
@Composable
private fun CleanupInspectSheet(
    target: InspectPhotoTarget,
    isSelectedForTrash: Boolean,
    onToggleTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoomScale by remember(target.item.id) { mutableFloatStateOf(1f) }
    var panX by remember(target.item.id) { mutableFloatStateOf(0f) }
    var panY by remember(target.item.id) { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.item.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildList {
                        add(target.item.bucketName.ifBlank { "Device" })
                        add(Formatter.formatShortFileSize(context, target.item.size))
                        formatMegapixels(target.pixelCount).takeIf { it.isNotBlank() }?.let { add(it) }
                        formatSharpnessWord(target.sharpness).takeIf { it.isNotBlank() }?.let { add(it) }
                        formatShortDate(target.item.dateAdded).takeIf { it.isNotBlank() }?.let { add(it) }
                    }.joinToString(" · ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            // Pinch-to-zoom preview frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f)
                    .clip(ShapeLarge)
                    .clipToBounds()
                    .background(Color.Black)
                    .pointerInput(target.item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (zoomScale * zoom).coerceIn(1f, 4f)
                            zoomScale = nextScale
                            if (nextScale > 1.02f) {
                                val maxX = (size.width * (nextScale - 1f)) / 2f
                                val maxY = (size.height * (nextScale - 1f)) / 2f
                                panX = (panX + pan.x).coerceIn(-maxX, maxX)
                                panY = (panY + pan.y).coerceIn(-maxY, maxY)
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    }
                    .pointerInput(target.item.id) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomScale > 1.15f) {
                                    zoomScale = 1f
                                    panX = 0f
                                    panY = 0f
                                } else {
                                    zoomScale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(target.item.resolvedUri)
                        .size(1440)
                        .build(),
                    contentDescription = target.item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panX
                            translationY = panY
                        }
                )

                if (target.badge != null) {
                    Surface(
                        shape = ShapeFull,
                        color = Color(0xB8121216),
                        contentColor = Color.White,
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = target.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (zoomScale > 1.05f) {
                    Surface(
                        onClick = {
                            zoomScale = 1f
                            panX = 0f
                            panY = 0f
                        },
                        shape = ShapeFull,
                        color = Color(0xB8121216),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f× · Reset", zoomScale),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Button(
                onClick = onToggleTrash,
                shape = ShapeFull,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelectedForTrash) MaterialTheme.colorScheme.surfaceContainerHighest
                    else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (isSelectedForTrash) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (isSelectedForTrash) R.drawable.ic_ms_check_circle else R.drawable.ic_ms_delete
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSelectedForTrash) "Keep this photo" else "Mark for Trash",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun formatMegapixels(pixelCount: Long): String {
    if (pixelCount <= 0L) return ""
    return String.format(Locale.US, "%.1f MP", pixelCount / 1_000_000.0)
}

private fun formatSharpnessWord(sharpness: Float): String = when {
    sharpness >= 32f -> "Crisp"
    sharpness >= 16f -> "Good"
    sharpness > 0f -> "Soft"
    else -> ""
}

private fun formatShortDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    return try {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochSeconds * 1000L))
    } catch (_: Throwable) {
        ""
    }
}
