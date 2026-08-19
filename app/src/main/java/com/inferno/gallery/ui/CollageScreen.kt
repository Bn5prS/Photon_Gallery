@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inferno.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.workers.MediaSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


// ── Data classes ──────────────────────────────────────────────────────────────

data class CollageLayout(
    val name: String,
    val bounds: List<Rect>
)

data class SlotState(
    val uri: Uri,
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

enum class CollageEditTab(val label: String, @androidx.annotation.DrawableRes val iconRes: Int) {
    LAYOUT("Layout", R.drawable.ic_ms_grid_view),
    BORDERS("Borders", R.drawable.ic_ms_border_all),
    BACKGROUND("Background", R.drawable.ic_ms_palette),
    RATIO("Ratio", R.drawable.ic_ms_aspect_ratio)
}

// ── Layout preset builder ─────────────────────────────────────────────────────

fun getLayoutPresets(itemCount: Int): List<CollageLayout> {
    val list = mutableListOf<CollageLayout>()
    if (itemCount <= 0) return list

    // Preset 1: Columns
    val columns = (0 until itemCount).map { idx ->
        val w = 1f / itemCount
        Rect(idx * w, 0f, (idx + 1) * w, 1f)
    }
    list.add(CollageLayout("Columns", columns))

    // Preset 2: Rows
    val rows = (0 until itemCount).map { idx ->
        val h = 1f / itemCount
        Rect(0f, idx * h, 1f, (idx + 1) * h)
    }
    list.add(CollageLayout("Rows", rows))

    // Preset 3: Grid
    when (itemCount) {
        4 -> list.add(CollageLayout("2x2 Grid", listOf(
            Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
            Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
        )))
        6 -> list.add(CollageLayout("2x3 Grid", listOf(
            Rect(0f, 0f, 0.5f, 0.333f), Rect(0.5f, 0f, 1f, 0.333f),
            Rect(0f, 0.333f, 0.5f, 0.666f), Rect(0.5f, 0.333f, 1f, 0.666f),
            Rect(0f, 0.666f, 0.5f, 1f), Rect(0.5f, 0.666f, 1f, 1f)
        )))
        8 -> list.add(CollageLayout("2x4 Grid", listOf(
            Rect(0f, 0f, 0.5f, 0.25f), Rect(0.5f, 0f, 1f, 0.25f),
            Rect(0f, 0.25f, 0.5f, 0.5f), Rect(0.5f, 0.25f, 1f, 0.5f),
            Rect(0f, 0.5f, 0.5f, 0.75f), Rect(0.5f, 0.5f, 1f, 0.75f),
            Rect(0f, 0.75f, 0.5f, 1f), Rect(0.5f, 0.75f, 1f, 1f)
        )))
        else -> {
            val cols = if (itemCount <= 3) itemCount else if (itemCount <= 6) 3 else 4
            val r = (itemCount + cols - 1) / cols
            val grid = (0 until itemCount).map { idx ->
                val c = idx % cols; val rowIdx = idx / cols
                Rect(c.toFloat() / cols, rowIdx.toFloat() / r, (c + 1).toFloat() / cols, (rowIdx + 1).toFloat() / r)
            }
            list.add(CollageLayout("Grid", grid))
        }
    }

    // Preset 4: Big Left
    if (itemCount > 1) {
        val remaining = itemCount - 1
        val right = (0 until remaining).map { idx ->
            Rect(0.5f, idx.toFloat() / remaining, 1f, (idx + 1).toFloat() / remaining)
        }
        list.add(CollageLayout("Big Left", listOf(Rect(0f, 0f, 0.5f, 1f)) + right))
    }

    // Preset 5: Big Top
    if (itemCount > 1) {
        val remaining = itemCount - 1
        val bottom = (0 until remaining).map { idx ->
            Rect(idx.toFloat() / remaining, 0.5f, (idx + 1).toFloat() / remaining, 1f)
        }
        list.add(CollageLayout("Big Top", listOf(Rect(0f, 0f, 1f, 0.5f)) + bottom))
    }

    // Preset 6: Split Rows
    when (itemCount) {
        3 -> list.add(CollageLayout("1 Top, 2 Bottom", listOf(
            Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
        )))
        5 -> list.add(CollageLayout("2 Top, 3 Bottom", listOf(
            Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
            Rect(0f, 0.5f, 0.333f, 1f), Rect(0.333f, 0.5f, 0.666f, 1f), Rect(0.666f, 0.5f, 1f, 1f)
        )))
        7 -> list.add(CollageLayout("3 Top, 4 Bottom", listOf(
            Rect(0f, 0f, 0.333f, 0.5f), Rect(0.333f, 0f, 0.666f, 0.5f), Rect(0.666f, 0f, 1f, 0.5f),
            Rect(0f, 0.5f, 0.25f, 1f), Rect(0.25f, 0.5f, 0.5f, 1f),
            Rect(0.5f, 0.5f, 0.75f, 1f), Rect(0.75f, 0.5f, 1f, 1f)
        )))
        else -> {
            val topHalf = itemCount / 2
            val bottomHalf = itemCount - topHalf
            val topBounds = (0 until topHalf).map { i -> Rect(i.toFloat() / topHalf, 0f, (i + 1).toFloat() / topHalf, 0.5f) }
            val bottomBounds = (0 until bottomHalf).map { i -> Rect(i.toFloat() / bottomHalf, 0.5f, (i + 1).toFloat() / bottomHalf, 1f) }
            list.add(CollageLayout("Split Rows", topBounds + bottomBounds))
        }
    }

    while (list.size < 6) {
        list.add(CollageLayout("Columns Alt ${list.size}", columns))
    }
    return list
}

// ── CollageScreen ─────────────────────────────────────────────────────────────

@Composable
fun CollageScreen(
    initialUris: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var slotStates by remember { mutableStateOf(initialUris.map { SlotState(Uri.parse(it)) }) }
    val presets = remember(slotStates.size) { getLayoutPresets(slotStates.size) }

    var selectedLayoutIndex by remember { mutableStateOf(0) }
    var selectedSlotIndex by remember { mutableStateOf(-1) }

    var spacing by remember { mutableStateOf(4f) }
    var cornerRadius by remember { mutableStateOf(8f) }
    var selectedBgColor by remember { mutableStateOf(Color.White) }
    var selectedRatioLabel by remember { mutableStateOf("1:1") }
    var ratioValue by remember { mutableStateOf(1f) }
    var activeTab by remember { mutableStateOf(CollageEditTab.LAYOUT) }
    var isSaving by remember { mutableStateOf(false) }
    var showGalleryChooser by remember { mutableStateOf(false) }

    val database = remember { DatabaseProvider.getDatabase(context) }
    val allMediaList by remember { database.mediaDao().observeAllMedia() }.collectAsState(initial = emptyList())
    val imageMedia = remember(allMediaList) { allMediaList.filter { !it.isVideo && it.bucketName != "Trash" } }

    BackHandler {
        when {
            showGalleryChooser -> showGalleryChooser = false
            selectedSlotIndex != -1 -> selectedSlotIndex = -1
            else -> onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Collage Canvas Area ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 72.dp, start = 12.dp, end = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                        Text(
                            text = "Generating collage…",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val parentWidth = maxWidth
                        val parentHeight = maxHeight
                        val parentRatio = parentWidth.value / parentHeight.value
                        val (canvasWidth, canvasHeight) = if (parentRatio > ratioValue) {
                            Pair(parentHeight * ratioValue, parentHeight)
                        } else {
                            Pair(parentWidth, parentWidth / ratioValue)
                        }

                        Box(
                            modifier = Modifier
                                .size(width = canvasWidth, height = canvasHeight)
                                .clip(RoundedCornerShape(cornerRadius.dp))
                                .background(selectedBgColor)
                                .padding(spacing.dp)
                        ) {
                            val currentLayout = presets.getOrNull(selectedLayoutIndex) ?: presets[0]
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val innerWidth = maxWidth
                                val innerHeight = maxHeight
                                currentLayout.bounds.forEachIndexed { index, rect ->
                                    val slot = slotStates.getOrNull(index) ?: return@forEachIndexed
                                    val left = innerWidth * rect.left
                                    val top = innerHeight * rect.top
                                    val width = innerWidth * (rect.right - rect.left)
                                    val height = innerHeight * (rect.bottom - rect.top)
                                    val isSlotSelected = selectedSlotIndex == index

                                    Box(
                                        modifier = Modifier
                                            .offset(x = left, y = top)
                                            .size(width = width, height = height)
                                            .padding(spacing.dp / 2)
                                            .clip(RoundedCornerShape(cornerRadius.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .border(
                                                width = if (isSlotSelected) 2.5.dp else 0.dp,
                                                color = if (isSlotSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(cornerRadius.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedSlotIndex = if (isSlotSelected) -1 else index
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(slot.uri)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .run {
                                                    if (isSlotSelected) {
                                                        pointerInput(index) {
                                                            detectTransformGestures { _, pan, zoom, _ ->
                                                                val updated = slotStates.toMutableList()
                                                                val curr = updated[index]
                                                                val newScale = (curr.scale * zoom).coerceIn(1f, 10f)
                                                                val newOffsetX = curr.offsetX + (pan.x / size.width.toFloat())
                                                                val newOffsetY = curr.offsetY + (pan.y / size.height.toFloat())
                                                                updated[index] = curr.copy(
                                                                    scale = newScale,
                                                                    offsetX = newOffsetX,
                                                                    offsetY = newOffsetY
                                                                )
                                                                slotStates = updated
                                                            }
                                                        }
                                                    } else {
                                                        this
                                                    }
                                                }
                                                .graphicsLayer {
                                                    rotationZ = slot.rotation.toFloat()
                                                    scaleX = (if (slot.flipHorizontal) -1f else 1f) * slot.scale
                                                    scaleY = (if (slot.flipVertical) -1f else 1f) * slot.scale
                                                    translationX = slot.offsetX * size.width
                                                    translationY = slot.offsetY * size.height
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Floating Slot Action Toolbar ─────────────────────────
                    if (selectedSlotIndex != -1) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val updated = slotStates.toMutableList()
                                        val curr = updated[selectedSlotIndex]
                                        updated[selectedSlotIndex] = curr.copy(rotation = (curr.rotation + 90) % 360)
                                        slotStates = updated
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_rotate_right), contentDescription = "Rotate", modifier = Modifier.size(20.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val updated = slotStates.toMutableList()
                                        val curr = updated[selectedSlotIndex]
                                        updated[selectedSlotIndex] = curr.copy(flipHorizontal = !curr.flipHorizontal)
                                        slotStates = updated
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz), contentDescription = "Flip Horizontal", modifier = Modifier.size(20.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val updated = slotStates.toMutableList()
                                        val curr = updated[selectedSlotIndex]
                                        updated[selectedSlotIndex] = curr.copy(flipVertical = !curr.flipVertical)
                                        slotStates = updated
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                        contentDescription = "Flip Vertical",
                                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 90f }
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val updated = slotStates.toMutableList()
                                        val curr = updated[selectedSlotIndex]
                                        updated[selectedSlotIndex] = curr.copy(scale = 1f, offsetX = 0f, offsetY = 0f)
                                        slotStates = updated
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_refresh), contentDescription = "Reset Zoom/Pan", modifier = Modifier.size(20.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showGalleryChooser = true
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = "Replace", modifier = Modifier.size(20.dp))
                                }

                                FilledTonalIconButton(
                                    onClick = { selectedSlotIndex = -1 },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_close), contentDescription = "Close", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom Studio Container ──────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    // Tool panel content
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            (slideInVertically(MotionTokens.snappySpring()) { it / 3 } + fadeIn(MotionTokens.snappySpring())) togetherWith
                                    (slideOutVertically(MotionTokens.snappySpring()) { -it / 3 } + fadeOut(MotionTokens.snappySpring()))
                        },
                        label = "collageTabContent"
                    ) { tab ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (tab) {
                                CollageEditTab.LAYOUT -> {
                                    LazyRow(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(presets) { idx, layout ->
                                            val isSelected = selectedLayoutIndex == idx
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedLayoutIndex = idx
                                                    selectedSlotIndex = -1
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(ShapeLarge)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                                        )
                                                        .border(
                                                            width = if (isSelected) 2.5.dp else 0.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = ShapeLarge
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    LayoutPreviewIcon(layout = layout, isSelected = isSelected)
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = layout.name,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                CollageEditTab.BORDERS -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Spacing row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                "Spacing",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.width(60.dp)
                                            )
                                            Slider(
                                                value = spacing,
                                                onValueChange = { spacing = it },
                                                valueRange = 0f..24f,
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
                                                    "${spacing.roundToInt()}dp",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // Radius row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                "Radius",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.width(60.dp)
                                            )
                                            Slider(
                                                value = cornerRadius,
                                                onValueChange = { cornerRadius = it },
                                                valueRange = 0f..24f,
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
                                                    "${cornerRadius.roundToInt()}dp",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                CollageEditTab.BACKGROUND -> {
                                    val bgColors = listOf(
                                        Color.White, Color.Black, Color(0xFF1E1E1E), Color(0xFFE0E0E0),
                                        Color(0xFFEEDC82), Color(0xFFFFB6C1), Color(0xFFADD8E6), Color(0xFF90EE90)
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(bgColors) { _, color ->
                                            val isSelected = selectedBgColor == color
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedBgColor = color
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                                        contentDescription = null,
                                                        tint = if (color == Color.White || color == Color(0xFFE0E0E0) || color == Color(0xFFEEDC82) || color == Color(0xFFFFB6C1) || color == Color(0xFFADD8E6) || color == Color(0xFF90EE90)) Color.Black else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                CollageEditTab.RATIO -> {
                                    val ratios = listOf(
                                        "1:1" to 1f, "4:3" to 4f / 3f, "3:4" to 3f / 4f,
                                        "16:9" to 16f / 9f, "9:16" to 9f / 16f
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(ratios) { _, (label, value) ->
                                            val isSelected = selectedRatioLabel == label
                                            Surface(
                                                shape = ShapeFull,
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                shadowElevation = 0.dp,
                                                modifier = Modifier
                                                    .clip(ShapeFull)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedRatioLabel = label
                                                        ratioValue = value
                                                    }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Main Tool Selector Strip ──────────────────────────
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(CollageEditTab.entries.toList()) { _, tab ->
                            val isSelected = activeTab == tab
                            CollageToolChip(
                                label = tab.label,
                                icon = ImageVector.vectorResource(tab.iconRes),
                                isSelected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeTab = tab
                                }
                            )
                        }
                    }
                }
            }
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
                        "Collage",
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
                            "${slotStates.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    FilledTonalButton(
                        enabled = !isSaving,
                        onClick = {
                            if (isSaving) return@FilledTonalButton
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSaving = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val currentLayout = presets.getOrNull(selectedLayoutIndex) ?: presets[0]
                                    saveCollageToDevice(
                                        context = context,
                                        slotStates = slotStates,
                                        layout = currentLayout,
                                        ratio = ratioValue,
                                        borderSpacingDp = spacing,
                                        cornerRadiusDp = cornerRadius,
                                        bgColor = selectedBgColor
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Collage saved to gallery!", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                        onBack()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        shape = ShapeFull,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_check), contentDescription = "Save", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // ── Local Gallery Chooser overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = showGalleryChooser,
            enter = fadeIn(MotionTokens.gentleSpring()) + slideInVertically(MotionTokens.gentleSpring()) { it },
            exit = fadeOut(MotionTokens.gentleSpring()) + slideOutVertically(MotionTokens.gentleSpring()) { it }
        ) {
            LocalGalleryChooser(
                mediaList = imageMedia,
                maxSelection = slotStates.size,
                onDismiss = { showGalleryChooser = false },
                onConfirm = { chosenUris: List<Uri> ->
                    val updated = slotStates.toMutableList()
                    chosenUris.forEachIndexed { index: Int, uri: Uri ->
                        val targetIndex = (selectedSlotIndex.coerceAtLeast(0) + index) % updated.size
                        updated[targetIndex] = SlotState(uri = uri, rotation = 0, flipHorizontal = false, flipVertical = false)
                    }
                    slotStates = updated
                    selectedSlotIndex = -1
                    showGalleryChooser = false
                }
            )
        }
    }
}

// ── Tool Chip ────────────────────────────────────────────────────────────────

@Composable
private fun CollageToolChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = ShapeFull,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(40.dp)
            .scale(if (isPressed) 0.94f else 1f)
            .clip(ShapeFull)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ── Layout Preview Icon ───────────────────────────────────────────────────────

@Composable
fun LayoutPreviewIcon(
    layout: CollageLayout,
    isSelected: Boolean
) {
    val color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        layout.bounds.forEach { rect ->
            drawRect(
                color = color,
                topLeft = Offset(rect.left * w, rect.top * h),
                size = Size((rect.right - rect.left) * w, (rect.bottom - rect.top) * h),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

// ── Local Gallery Chooser ─────────────────────────────────────────────────────

@Composable
fun LocalGalleryChooser(
    mediaList: List<CoreMediaEntity>,
    maxSelection: Int,
    onDismiss: () -> Unit,
    onConfirm: (List<Uri>) -> Unit
) {
    var selectedUris by remember { mutableStateOf(emptyList<Uri>()) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Scrim
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back), contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Select Photo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Select up to $maxSelection photo${if (maxSelection > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(selectedUris)
                    },
                    enabled = selectedUris.isNotEmpty(),
                    shape = ShapeFull,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        "Replace (${selectedUris.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Photo Grid
            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photos found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mediaList) { item ->
                        val uri = Uri.parse(item.uriString)
                        val isSelected = selectedUris.contains(uri)
                        val selectIndex = selectedUris.indexOf(uri)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(ShapeLarge)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = ShapeLarge
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isSelected) {
                                        selectedUris = selectedUris - uri
                                    } else if (selectedUris.size < maxSelection) {
                                        selectedUris = selectedUris + uri
                                    } else {
                                        Toast.makeText(context, "Max $maxSelection images.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .size(300, 300)
                                    .precision(coil3.size.Precision.EXACT)
                                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    border = BorderStroke(1.5.dp, Color.White),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(3.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (selectIndex + 1).toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            softWrap = false
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

// ── Bitmap save logic ─────────────────────────────────────────────────────────

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun saveCollageToDevice(
    context: Context,
    slotStates: List<SlotState>,
    layout: CollageLayout,
    ratio: Float,
    borderSpacingDp: Float,
    cornerRadiusDp: Float,
    bgColor: Color
) = withContext(Dispatchers.IO) {
    val scaleFactor = 4f
    val baseWidth = (600 * scaleFactor).roundToInt()
    val baseHeight = (baseWidth / ratio).roundToInt()
    val borderSpacing = borderSpacingDp * scaleFactor
    val cornerRadius = cornerRadiusDp * scaleFactor

    val bitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(bgColor.toArgb())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    layout.bounds.forEachIndexed { index, rect ->
        val slot = slotStates.getOrNull(index) ?: return@forEachIndexed
        val l = rect.left * baseWidth + borderSpacing / 2f
        val t = rect.top * baseHeight + borderSpacing / 2f
        val r = rect.right * baseWidth - borderSpacing / 2f
        val b = rect.bottom * baseHeight - borderSpacing / 2f
        val w = r - l
        val h = b - t
        if (w <= 0 || h <= 0) return@forEachIndexed

        val path = Path()
        path.addRoundRect(RectF(l, t, r, b), cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)

        val decoded = decodeSampledBitmap(context, slot.uri, w.roundToInt(), h.roundToInt())
        if (decoded != null) {
            val matrix = Matrix()
            val flipX = if (slot.flipHorizontal) -1f else 1f
            val flipY = if (slot.flipVertical) -1f else 1f
            matrix.postScale(flipX, flipY, decoded.width / 2f, decoded.height / 2f)
            matrix.postRotate(slot.rotation.toFloat(), decoded.width / 2f, decoded.height / 2f)

            val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val srcW = transformed.width.toFloat()
            val srcH = transformed.height.toFloat()
            val scale = minOf(w / srcW, h / srcH)
            val dx = (w - srcW * scale) / 2f
            val dy = (h - srcH * scale) / 2f
            val drawMatrix = Matrix()
            drawMatrix.postScale(scale, scale)
            drawMatrix.postTranslate(dx, dy)
            drawMatrix.postScale(slot.scale, slot.scale, w / 2f, h / 2f)
            drawMatrix.postTranslate(slot.offsetX * w, slot.offsetY * h)
            drawMatrix.postTranslate(l, t)
            canvas.drawBitmap(transformed, drawMatrix, paint)
            if (transformed != decoded) transformed.recycle()
            decoded.recycle()
        }
        canvas.restore()
    }

    val filename = "collage_${System.currentTimeMillis()}.jpg"
    val values = android.content.ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/PhotonGallery_Collages")
    }
    val resolver = context.contentResolver
    val outputUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw Exception("Failed to create MediaStore entry")
    resolver.openOutputStream(outputUri)?.use { stream ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
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
