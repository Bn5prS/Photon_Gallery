@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.inferno.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.workers.MediaSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


// ── Data models ───────────────────────────────────────────────────────────────

data class StitchItem(
    val uri: Uri,
    val id: String = "${uri}_${System.nanoTime()}"
)

enum class StitchOrientation { VERTICAL, HORIZONTAL }
enum class StitchScaleMode { FILL, FIT }
enum class StitchAlignment { START, CENTER, END }

// ── StitchScreen ─────────────────────────────────────────────────────────────

@Composable
fun StitchScreen(
    initialUris: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // ── Core editable state ─────────────────────────────────────────────────
    var stitchItems by remember {
        mutableStateOf(initialUris.map { StitchItem(Uri.parse(it)) })
    }
    var orientation by remember { mutableStateOf(StitchOrientation.VERTICAL) }
    var spacingDp by remember { mutableFloatStateOf(4f) }
    var alignment by remember { mutableStateOf(StitchAlignment.CENTER) }
    var scaleMode by remember { mutableStateOf(StitchScaleMode.FILL) }
    var bgColor by remember { mutableStateOf(Color.Black) }

    // ── UI state ─────────────────────────────────────────────────────────────
    var isSaving by remember { mutableStateOf(false) }
    var showGalleryPicker by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }

    // ── Gallery media for the add-more picker ─────────────────────────────
    val database = remember { DatabaseProvider.getDatabase(context) }
    val allMediaList by remember { database.mediaDao().observeAllMedia() }
        .collectAsState(initial = emptyList())
    val imageMedia = remember(allMediaList) {
        allMediaList.filter { !it.isVideo && it.bucketName != "Trash" }
    }

    BackHandler {
        when {
            showGalleryPicker -> showGalleryPicker = false
            showSaveSheet -> showSaveSheet = false
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Live Preview Area ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 68.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                        Text(
                            text = "Stitching photos…",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    StitchPreview(
                        items = stitchItems,
                        orientation = orientation,
                        spacingDp = spacingDp,
                        bgColor = bgColor
                    )
                }
            }

            // ── Reorderable thumbnail strip ───────────────────────────────
            ReorderableThumbnailStrip(
                items = stitchItems,
                onReorder = { from, to ->
                    stitchItems = stitchItems.toMutableList().also {
                        it.add(to, it.removeAt(from))
                    }
                },
                onRemove = { index ->
                    if (stitchItems.size > 2) {
                        stitchItems = stitchItems.toMutableList().also { it.removeAt(index) }
                    } else {
                        Toast.makeText(context, "Need at least 2 images", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddMore = { showGalleryPicker = true }
            )

            // ── Controls panel (Studio Tonal Container) ───────────────────
            StitchControlsPanel(
                orientation = orientation,
                spacingDp = spacingDp,
                alignment = alignment,
                scaleMode = scaleMode,
                bgColor = bgColor,
                onOrientationChange = { orientation = it },
                onSpacingChange = { spacingDp = it },
                onAlignmentChange = { alignment = it },
                onScaleModeChange = { scaleMode = it },
                onBgColorChange = { bgColor = it }
            )
        }

        // ── Top Floating Action Scrim Toolbar ────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                    }
                }
            }

            // Right: Header & Save Action Pill
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Stitch",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )

                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            "${stitchItems.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    FilledTonalButton(
                        enabled = !isSaving && stitchItems.size >= 2,
                        onClick = {
                            if (!isSaving) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSaveSheet = true
                            }
                        },
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_download), contentDescription = "Save", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Export",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // ── Save options bottom sheet ─────────────────────────────────────────
        if (showSaveSheet) {
            SaveOptionsSheet(
                onDismiss = { showSaveSheet = false },
                onSave = { format ->
                    showSaveSheet = false
                    isSaving = true
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            saveStitchedImage(
                                context = context,
                                items = stitchItems,
                                orientation = orientation,
                                spacingPx = spacingDp * context.resources.displayMetrics.density,
                                alignment = alignment,
                                scaleMode = scaleMode,
                                bgColor = bgColor,
                                format = format
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Saved to gallery!", Toast.LENGTH_SHORT).show()
                                isSaving = false
                                onBack()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error saving: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        // ── Add-more gallery picker overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = showGalleryPicker,
            enter = fadeIn(MotionTokens.gentleSpring()) + slideInVertically(MotionTokens.gentleSpring()) { it },
            exit = fadeOut(MotionTokens.gentleSpring()) + slideOutVertically(MotionTokens.gentleSpring()) { it }
        ) {
            LocalGalleryChooser(
                mediaList = imageMedia,
                maxSelection = 30,
                onDismiss = { showGalleryPicker = false },
                onConfirm = { chosenUris ->
                    stitchItems = stitchItems + chosenUris.map { StitchItem(it) }
                    showGalleryPicker = false
                }
            )
        }
    }
}

// ── StitchPreview ─────────────────────────────────────────────────────────────

@Composable
private fun StitchPreview(
    items: List<StitchItem>,
    orientation: StitchOrientation,
    spacingDp: Float,
    bgColor: Color
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (orientation == StitchOrientation.VERTICAL) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacingDp.dp)
            ) {
                items.forEach { item ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(900, 1800)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacingDp.dp)
            ) {
                items.forEach { item ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(1800, 900)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

// ── ReorderableThumbnailStrip ─────────────────────────────────────────────────

@Composable
private fun ReorderableThumbnailStrip(
    items: List<StitchItem>,
    onReorder: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onAddMore: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableStateOf(0) }

    val itemCentersX = remember { mutableStateMapOf<Int, Float>() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isDragging = draggingIndex == index

                val liftElevation by animateDpAsState(
                    targetValue = if (isDragging) 12.dp else 0.dp,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "stitchThumbElevation"
                )
                val liftScale by animateFloatAsState(
                    targetValue = if (isDragging) 1.12f else 1f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "stitchThumbScale"
                )
                val removeBadgeAlpha by animateFloatAsState(
                    targetValue = if (isDragging) 0f else 1f,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "removeBadgeAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .onGloballyPositioned { coords ->
                            itemCentersX[index] = coords.positionInRoot().x + coords.size.width / 2f
                        }
                        .graphicsLayer {
                            scaleX = liftScale
                            scaleY = liftScale
                            translationX = if (isDragging) dragOffsetX else 0f
                            shadowElevation = liftElevation.toPx()
                        }
                        .zIndex(if (isDragging) 10f else 0f)
                        .clip(ShapeLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (isDragging) 2.5.dp else 0.dp,
                            color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = ShapeLarge
                        )
                        .pointerInput(index, items.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingIndex = index
                                    dragOffsetX = 0f
                                    targetIndex = index
                                },
                                onDrag = { _, delta ->
                                    dragOffsetX += delta.x
                                    val currentCentreX = (itemCentersX[index] ?: 0f) + dragOffsetX
                                    val newTarget = itemCentersX.entries
                                        .minByOrNull { abs(it.value - currentCentreX) }
                                        ?.key ?: index
                                    if (newTarget != targetIndex) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        targetIndex = newTarget
                                    }
                                },
                                onDragEnd = {
                                    val from = draggingIndex
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                    if (from != null) {
                                        val to = targetIndex.coerceIn(0, items.size - 1)
                                        if (from != to) onReorder(from, to)
                                    }
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .size(144, 144)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Image ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Order number badge
                    Surface(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .size(20.dp)
                            .align(Alignment.TopStart),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Remove badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                            .size(20.dp)
                            .graphicsLayer {
                                alpha = removeBadgeAlpha
                            }
                            .border(BorderStroke(1.5.dp, Color.White), CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f))
                            .clickable(enabled = !isDragging) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onRemove(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Add more button
            Surface(
                shape = ShapeLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clip(ShapeLarge)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddMore()
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                        contentDescription = "Add images",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ── StitchControlsPanel ───────────────────────────────────────────────────────

@Composable
private fun StitchControlsPanel(
    orientation: StitchOrientation,
    spacingDp: Float,
    alignment: StitchAlignment,
    scaleMode: StitchScaleMode,
    bgColor: Color,
    onOrientationChange: (StitchOrientation) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onAlignmentChange: (StitchAlignment) -> Unit,
    onScaleModeChange: (StitchScaleMode) -> Unit,
    onBgColorChange: (Color) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = ShapeEdgeTop,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Row 1: Direction & Scale with ample horizontal scrolling ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Segmented Button
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direction",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = orientation == StitchOrientation.VERTICAL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOrientationChange(StitchOrientation.VERTICAL)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Text(
                                "Vertical",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        SegmentedButton(
                            selected = orientation == StitchOrientation.HORIZONTAL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOrientationChange(StitchOrientation.HORIZONTAL)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Text(
                                "Horizontal",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Scale mode Segmented Button
                Column(modifier = Modifier.weight(0.85f)) {
                    Text(
                        text = "Scale",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = scaleMode == StitchScaleMode.FILL,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScaleModeChange(StitchScaleMode.FILL)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {}
                        ) {
                            Text(
                                "Fill",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        SegmentedButton(
                            selected = scaleMode == StitchScaleMode.FIT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScaleModeChange(StitchScaleMode.FIT)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {}
                        ) {
                            Text(
                                "Fit",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // ── Row 2: Gap spacing slider ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Gap",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(36.dp)
                )
                Slider(
                    value = spacingDp,
                    onValueChange = onSpacingChange,
                    valueRange = 0f..40f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = "${spacingDp.roundToInt()}dp",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ── Row 3: Alignment (vertical only) + Background color ──────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = orientation == StitchOrientation.VERTICAL,
                    enter = fadeIn(MotionTokens.gentleSpring()) + scaleIn(MotionTokens.gentleSpring(), initialScale = 0.85f),
                    exit = fadeOut(MotionTokens.gentleSpring()) + scaleOut(MotionTokens.gentleSpring(), targetScale = 0.85f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = "Align",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = alignment == StitchAlignment.START,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAlignmentChange(StitchAlignment.START)
                                },
                                shape = SegmentedButtonDefaults.itemShape(0, 3),
                                icon = {}
                            ) {
                                Text(
                                    "Left",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            SegmentedButton(
                                selected = alignment == StitchAlignment.CENTER,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAlignmentChange(StitchAlignment.CENTER)
                                },
                                shape = SegmentedButtonDefaults.itemShape(1, 3),
                                icon = {}
                            ) {
                                Text(
                                    "Center",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            SegmentedButton(
                                selected = alignment == StitchAlignment.END,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAlignmentChange(StitchAlignment.END)
                                },
                                shape = SegmentedButtonDefaults.itemShape(2, 3),
                                icon = {}
                            ) {
                                Text(
                                    "Right",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Background colour picker
                Column(
                    modifier = if (orientation == StitchOrientation.VERTICAL) Modifier.wrapContentWidth() else Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Background",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    val presetColors = listOf(
                        Color.Black,
                        Color.White,
                        Color(0xFF1E1E1E),
                        Color(0xFF2D2D2D),
                        Color(0xFFF5F5F0),
                        Color(0xFFE8D5C4),
                        Color(0xFF0D1B2A),
                        Color(0xFF1B4332)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(presetColors) { _, color ->
                            val isSelected = bgColor == color
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBgColorChange(color)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                        contentDescription = null,
                                        tint = if (color == Color.White || color == Color(0xFFF5F5F0) || color == Color(0xFFE8D5C4)) Color.Black else Color.White,
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

// ── SaveOptionsSheet ──────────────────────────────────────────────────────────

@Composable
private fun SaveOptionsSheet(
    onDismiss: () -> Unit,
    onSave: (format: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = ShapeEdgeTop,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Save Stitched Image",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "Choose an export format for your stitched media.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            SaveFormatCard(
                label = "JPEG",
                description = "Best compatibility — photos & sharing",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave("JPEG")
                }
            )
            SaveFormatCard(
                label = "PNG",
                description = "Lossless quality — screenshots & graphics",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave("PNG")
                }
            )
            SaveFormatCard(
                label = "PDF",
                description = "Single-page document — printing & archiving",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave("PDF")
                }
            )
        }
    }
}

@Composable
private fun SaveFormatCard(
    label: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = ShapeLarge,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.96f else 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.80f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_download),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Render + Save pipeline ────────────────────────────────────────────────────

private fun decodeSampledBitmapForStitch(
    context: Context,
    uri: Uri,
    targetWidth: Int,
    targetHeight: Int
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sample = 1
        while (origW / (sample * 2) >= targetWidth && origH / (sample * 2) >= targetHeight) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private suspend fun saveStitchedImage(
    context: Context,
    items: List<StitchItem>,
    orientation: StitchOrientation,
    spacingPx: Float,
    alignment: StitchAlignment,
    scaleMode: StitchScaleMode,
    bgColor: Color,
    format: String
) = withContext(Dispatchers.IO) {
    val maxDimension = 4096
    val density = context.resources.displayMetrics.density

    val boundsList = items.mapNotNull { item ->
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            if (opts.outWidth > 0 && opts.outHeight > 0) Pair(opts.outWidth, opts.outHeight)
            else null
        } catch (e: Exception) {
            null
        }
    }
    if (boundsList.isEmpty()) throw Exception("Could not read image dimensions")

    val totalSpacing = spacingPx * (items.size - 1).coerceAtLeast(0)

    if (format == "PDF") {
        val pdfDocument = PdfDocument()
        try {
            items.forEachIndexed { pageIndex, item ->
                val (origW, origH) = boundsList.getOrNull(pageIndex) ?: return@forEachIndexed
                val scale = minOf(595f / origW, 842f / origH, 1f)
                val pageW = (origW * scale).roundToInt().coerceAtLeast(1)
                val pageH = (origH * scale).roundToInt().coerceAtLeast(1)

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val pageCanvas = page.canvas
                pageCanvas.drawColor(bgColor.toArgb())

                val bmp = decodeSampledBitmapForStitch(context, item.uri, pageW, pageH)
                if (bmp != null) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                    val dstRect = android.graphics.Rect(0, 0, pageW, pageH)
                    pageCanvas.drawBitmap(bmp, srcRect, dstRect, paint)
                    bmp.recycle()
                }
                pdfDocument.finishPage(page)
            }

            val filename = "stitch_${System.currentTimeMillis()}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/PhotonGallery_Stitch")
            }
            val resolver = context.contentResolver
            val outputUri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: throw Exception("Failed to create PDF MediaStore entry")
            resolver.openOutputStream(outputUri)?.use { stream ->
                pdfDocument.writeTo(stream)
            } ?: throw Exception("Failed to open PDF output stream")
        } finally {
            pdfDocument.close()
        }
        return@withContext
    }

    val (canvasWidth, canvasHeight) = if (orientation == StitchOrientation.VERTICAL) {
        val primaryWidth = minOf(boundsList.maxOf { it.first }, maxDimension)
        val scaledHeights = boundsList.map { (w, h) -> (primaryWidth.toFloat() / w * h).roundToInt() }
        val totalH = scaledHeights.sum() + totalSpacing.roundToInt()
        val finalH = minOf(totalH, maxDimension * 4)
        Pair(primaryWidth, finalH)
    } else {
        val primaryHeight = minOf(boundsList.maxOf { it.second }, maxDimension)
        val scaledWidths = boundsList.map { (w, h) -> (primaryHeight.toFloat() / h * w).roundToInt() }
        val totalW = scaledWidths.sum() + totalSpacing.roundToInt()
        val finalW = minOf(totalW, maxDimension * 4)
        Pair(finalW, primaryHeight)
    }

    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(bgColor.toArgb())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    var currentOffset = 0f

    items.forEachIndexed { index, item ->
        val (origW, origH) = boundsList.getOrNull(index) ?: return@forEachIndexed

        if (orientation == StitchOrientation.VERTICAL) {
            val slotH = (canvasWidth.toFloat() / origW * origH).roundToInt()
            val bmp = decodeSampledBitmapForStitch(context, item.uri, canvasWidth, slotH)
            if (bmp != null) {
                val dstLeft = when (alignment) {
                    StitchAlignment.START -> 0f
                    StitchAlignment.CENTER -> (canvasWidth - canvasWidth) / 2f
                    StitchAlignment.END -> (canvasWidth - canvasWidth).toFloat()
                }
                val dstTop = currentOffset
                val dstRight = dstLeft + canvasWidth
                val dstBottom = dstTop + slotH

                val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                val dstRect = android.graphics.Rect(dstLeft.roundToInt(), dstTop.roundToInt(), dstRight.roundToInt(), dstBottom.roundToInt())
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                bmp.recycle()
                currentOffset += slotH + spacingPx
            }
        } else {
            val slotW = (canvasHeight.toFloat() / origH * origW).roundToInt()
            val bmp = decodeSampledBitmapForStitch(context, item.uri, slotW, canvasHeight)
            if (bmp != null) {
                val dstLeft = currentOffset
                val dstTop = 0f
                val dstRight = dstLeft + slotW
                val dstBottom = canvasHeight.toFloat()

                val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                val dstRect = android.graphics.Rect(dstLeft.roundToInt(), dstTop.roundToInt(), dstRight.roundToInt(), dstBottom.roundToInt())
                canvas.drawBitmap(bmp, srcRect, dstRect, paint)
                bmp.recycle()
                currentOffset += slotW + spacingPx
            }
        }
    }

    val mimeType = if (format == "PNG") "image/png" else "image/jpeg"
    val extension = if (format == "PNG") "png" else "jpg"
    val compressFormat = if (format == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    val quality = if (format == "PNG") 100 else 95

    val filename = "stitch_${System.currentTimeMillis()}.$extension"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PhotonGallery_Stitch")
    }
    val resolver = context.contentResolver
    val outputUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw Exception("Failed to create MediaStore entry")
    resolver.openOutputStream(outputUri)?.use { stream ->
        if (!bitmap.compress(compressFormat, quality, stream)) {
            throw Exception("Failed to compress bitmap to output stream")
        }
    } ?: throw Exception("Failed to open output stream")
    bitmap.recycle()

    val syncWorkRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>().build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        "MediaSyncWorker",
        ExistingWorkPolicy.REPLACE,
        syncWorkRequest
    )
}
