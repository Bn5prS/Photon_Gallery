@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.inferno.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.exifinterface.media.ExifInterface
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.inferno.gallery.R
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.editor.components.EditorRulerSlider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.PhotonStudioTheme
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.workers.MediaSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Studio Data Models ────────────────────────────────────────────────────────

data class StitchItem(
    val uri: Uri,
    val id: String = "${uri}_${System.nanoTime()}",
    val rotation: Int = 0,
    val flipHorizontal: Boolean = false,
    val trimStart: Float = 0f,
    val trimEnd: Float = 0f
)

enum class StitchOrientation { VERTICAL, HORIZONTAL }
enum class StitchScaleMode { FILL, FIT }
enum class StitchAlignment { START, CENTER, END }

enum class StitchStyle(
    val label: String,
    val subtitle: String,
    val defaultGap: Float,
    val defaultMargin: Float,
    val defaultCorner: Float,
    val defaultBorderWidth: Float
) {
    SEAMLESS("Seamless", "Zero-gap scroll", 0f, 0f, 0f, 0f),
    MODERN("Bento Strip", "Clean rounded", 6f, 12f, 14f, 0f),
    FILM("35mm Film", "Cinema sprocket", 8f, 18f, 4f, 0f),
    POLAROID("Instant Strip", "White frame", 10f, 16f, 6f, 6f),
    SHADOW_CARDS("Elevated", "Floating cards", 12f, 16f, 16f, 2f)
}

enum class StitchFrameParam(val label: String, val min: Float, val max: Float, val defaultVal: Float) {
    GAP("Gap", 0f, 40f, 0f),
    MARGIN("Margin", 0f, 32f, 0f),
    CORNERS("Corners", 0f, 32f, 0f),
    BORDER("Stroke", 0f, 12f, 0f),
    TRIM_BARS("Trim Bars", 0f, 25f, 0f)
}

sealed class StitchBackground(val label: String) {
    data class Solid(val color: Color, val name: String) : StitchBackground(name)
    data class Gradient(val start: Color, val end: Color, val name: String) : StitchBackground(name)
    data class Pattern(val baseColor: Color, val dotColor: Color, val isGrid: Boolean, val name: String) : StitchBackground(name)
}

enum class StitchStudioTab(val label: String, @param:androidx.annotation.DrawableRes val iconRes: Int) {
    CLIPS("Clips", R.drawable.ic_ms_image),
    STYLE("Style", R.drawable.ic_ms_auto_fix_high),
    LAYOUT("Layout", R.drawable.ic_ms_thin_open_in_full),
    FRAME("Frame", R.drawable.ic_ms_thin_tune),
    BACKDROP("Backdrop", R.drawable.ic_ms_thin_palette)
}

private data class StitchSnapshot(
    val items: List<StitchItem>,
    val orientation: StitchOrientation,
    val spacingDp: Float,
    val outerMarginDp: Float,
    val cornerRadiusDp: Float,
    val borderWidthDp: Float,
    val style: StitchStyle,
    val alignment: StitchAlignment,
    val scaleMode: StitchScaleMode,
    val background: StitchBackground
)

// ── StitchScreen Studio ───────────────────────────────────────────────────────

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
    var selectedStyle by remember { mutableStateOf(StitchStyle.SEAMLESS) }
    var spacingDp by remember { mutableFloatStateOf(StitchStyle.SEAMLESS.defaultGap) }
    var outerMarginDp by remember { mutableFloatStateOf(StitchStyle.SEAMLESS.defaultMargin) }
    var cornerRadiusDp by remember { mutableFloatStateOf(StitchStyle.SEAMLESS.defaultCorner) }
    var borderWidthDp by remember { mutableFloatStateOf(StitchStyle.SEAMLESS.defaultBorderWidth) }
    var alignment by remember { mutableStateOf(StitchAlignment.CENTER) }
    var scaleMode by remember { mutableStateOf(StitchScaleMode.FILL) }
    var selectedBackground by remember { mutableStateOf<StitchBackground>(StitchBackground.Solid(Color.Black, "Black")) }

    // ── Studio UI & history state ────────────────────────────────────────────
    var activeTab by remember { mutableStateOf(StitchStudioTab.STYLE) }
    var activeFrameParam by remember { mutableStateOf(StitchFrameParam.GAP) }
    var selectedClipIndex by remember { mutableIntStateOf(-1) }
    var isSaving by remember { mutableStateOf(false) }
    var showGalleryPicker by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }

    val undoStack = remember { mutableStateListOf<StitchSnapshot>() }
    val redoStack = remember { mutableStateListOf<StitchSnapshot>() }

    fun currentSnapshot() = StitchSnapshot(
        items = stitchItems.map { it.copy() },
        orientation = orientation,
        spacingDp = spacingDp,
        outerMarginDp = outerMarginDp,
        cornerRadiusDp = cornerRadiusDp,
        borderWidthDp = borderWidthDp,
        style = selectedStyle,
        alignment = alignment,
        scaleMode = scaleMode,
        background = selectedBackground
    )

    fun restoreSnapshot(s: StitchSnapshot) {
        stitchItems = s.items
        orientation = s.orientation
        spacingDp = s.spacingDp
        outerMarginDp = s.outerMarginDp
        cornerRadiusDp = s.cornerRadiusDp
        borderWidthDp = s.borderWidthDp
        selectedStyle = s.style
        alignment = s.alignment
        scaleMode = s.scaleMode
        selectedBackground = s.background
        if (selectedClipIndex !in stitchItems.indices) selectedClipIndex = -1
    }

    fun pushUndo() {
        undoStack.add(currentSnapshot())
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun applyStylePreset(style: StitchStyle) {
        pushUndo()
        selectedStyle = style
        spacingDp = style.defaultGap
        outerMarginDp = style.defaultMargin
        cornerRadiusDp = style.defaultCorner
        borderWidthDp = style.defaultBorderWidth
        selectedBackground = when (style) {
            StitchStyle.FILM -> StitchBackground.Solid(Color(0xFF101113), "Cinema")
            StitchStyle.POLAROID -> StitchBackground.Solid(Color(0xFFF4EFE6), "Warm Paper")
            StitchStyle.SHADOW_CARDS -> StitchBackground.Pattern(Color(0xFFF7F3EB), Color(0x26000000), false, "Dot Grid")
            else -> selectedBackground
        }
    }

    val allTrimmed = remember(stitchItems) {
        stitchItems.isNotEmpty() && stitchItems.all { it.trimStart > 0.005f || it.trimEnd > 0.005f }
    }

    // ── Gallery media for Add-more picker ──────────────────────────────────
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
            selectedClipIndex != -1 -> selectedClipIndex = -1
            else -> onBack()
        }
    }

    PhotonStudioTheme {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Studio Bar (Matches PhotonEditorScreen & CollageScreen) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Close + Connected Undo / Redo + Reverse Order
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_thin_close),
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val canUndo = undoStack.isNotEmpty()
                        val canRedo = redoStack.isNotEmpty()
                        Surface(
                            onClick = {
                                if (canUndo) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    redoStack.add(currentSnapshot())
                                    restoreSnapshot(undoStack.removeAt(undoStack.lastIndex))
                                }
                            },
                            enabled = canUndo,
                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(width = 42.dp, height = 40.dp)
                                .alpha(if (canUndo) 1f else 0.38f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_thin_undo),
                                    contentDescription = "Undo",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                if (canRedo) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    undoStack.add(currentSnapshot())
                                    restoreSnapshot(redoStack.removeAt(redoStack.lastIndex))
                                }
                            },
                            enabled = canRedo,
                            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(width = 42.dp, height = 40.dp)
                                .alpha(if (canRedo) 1f else 0.38f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_thin_redo),
                                    contentDescription = "Redo",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (stitchItems.size > 1) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pushUndo()
                                stitchItems = stitchItems.reversed()
                                if (selectedClipIndex in stitchItems.indices) {
                                    selectedClipIndex = stitchItems.lastIndex - selectedClipIndex
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                contentDescription = "Reverse order",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        rotationZ = if (orientation == StitchOrientation.VERTICAL) 90f else 0f
                                    }
                            )
                        }
                    }
                }

                // Right: +N Add Photo Pill & Export Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showGalleryPicker = true
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_add),
                                contentDescription = "Add photos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${stitchItems.size}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (!isSaving && stitchItems.size >= 2) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSaveSheet = true
                            }
                        },
                        enabled = !isSaving && stitchItems.size >= 2,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_thin_save),
                                contentDescription = "Export",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Export",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // ── Interactive Stitch Canvas Stage ───────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                        Text(
                            text = "Stitching high-res studio strip…",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    StitchPreview(
                        items = stitchItems,
                        orientation = orientation,
                        style = selectedStyle,
                        spacingDp = spacingDp,
                        outerMarginDp = outerMarginDp,
                        cornerRadiusDp = cornerRadiusDp,
                        borderWidthDp = borderWidthDp,
                        background = selectedBackground,
                        alignment = alignment,
                        scaleMode = scaleMode,
                        selectedClipIndex = selectedClipIndex,
                        onSelectClip = { idx ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedClipIndex = if (selectedClipIndex == idx) -1 else idx
                        }
                    )

                    // ── Floating Per-Clip Action Pill Toolbar ────────────────
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedClipIndex in stitchItems.indices,
                        enter = fadeIn(MotionTokens.snappySpring()) + slideInVertically(MotionTokens.snappySpring()) { it / 2 },
                        exit = fadeOut(MotionTokens.snappySpring()) + slideOutVertically(MotionTokens.snappySpring()) { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        val clip = stitchItems.getOrNull(selectedClipIndex)
                        if (clip != null) {
                            val isTrimmed = clip.trimStart > 0.005f || clip.trimEnd > 0.005f
                            Surface(
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                tonalElevation = 4.dp
                            ) {
                                LazyRow(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pushUndo()
                                            val updated = stitchItems.toMutableList()
                                            updated[selectedClipIndex] = clip.copy(rotation = (clip.rotation + 90) % 360)
                                            stitchItems = updated
                                        }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_rotate_right), contentDescription = "Rotate 90°", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pushUndo()
                                            val updated = stitchItems.toMutableList()
                                            updated[selectedClipIndex] = clip.copy(flipHorizontal = !clip.flipHorizontal)
                                            stitchItems = updated
                                        }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_flip), contentDescription = "Flip Horizontal", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pushUndo()
                                            val updated = stitchItems.toMutableList()
                                            updated[selectedClipIndex] = if (isTrimmed) {
                                                clip.copy(trimStart = 0f, trimEnd = 0f)
                                            } else {
                                                clip.copy(trimStart = 0.045f, trimEnd = 0.055f)
                                            }
                                            stitchItems = updated
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_ms_thin_crop),
                                                contentDescription = "Trim screenshot bars",
                                                tint = if (isTrimmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (selectedClipIndex > 0) {
                                        item {
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                pushUndo()
                                                val updated = stitchItems.toMutableList()
                                                val moved = updated.removeAt(selectedClipIndex)
                                                val newIdx = selectedClipIndex - 1
                                                updated.add(newIdx, moved)
                                                stitchItems = updated
                                                selectedClipIndex = newIdx
                                            }) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_arrow_back),
                                                    contentDescription = "Move earlier",
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            rotationZ = if (orientation == StitchOrientation.VERTICAL) 90f else 0f
                                                        }
                                                )
                                            }
                                        }
                                    }
                                    if (selectedClipIndex < stitchItems.lastIndex) {
                                        item {
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                pushUndo()
                                                val updated = stitchItems.toMutableList()
                                                val moved = updated.removeAt(selectedClipIndex)
                                                val newIdx = selectedClipIndex + 1
                                                updated.add(newIdx, moved)
                                                stitchItems = updated
                                                selectedClipIndex = newIdx
                                            }) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_chevron_right),
                                                    contentDescription = "Move later",
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .graphicsLayer {
                                                            rotationZ = if (orientation == StitchOrientation.VERTICAL) 90f else 0f
                                                        }
                                                )
                                            }
                                        }
                                    }
                                    if (stitchItems.size > 2) {
                                        item {
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pushUndo()
                                                val updated = stitchItems.toMutableList()
                                                updated.removeAt(selectedClipIndex)
                                                stitchItems = updated
                                                selectedClipIndex = -1
                                            }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_ms_thin_delete),
                                                    contentDescription = "Remove clip",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        IconButton(onClick = { selectedClipIndex = -1 }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_close), contentDescription = "Done", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Bottom Studio Deck + Google Photos Pitch-Black Tab Dock ─
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .navigationBarsPadding()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (slideInVertically(MotionTokens.snappySpring()) { it / 3 } + fadeIn(MotionTokens.snappySpring())) togetherWith
                            (slideOutVertically(MotionTokens.snappySpring()) { -it / 3 } + fadeOut(MotionTokens.snappySpring()))
                    },
                    label = "stitchStudioDeck"
                ) { tab ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (tab) {
                            StitchStudioTab.CLIPS -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Hold & drag to reorder • Tap to edit clip",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                pushUndo()
                                                stitchItems = if (allTrimmed) {
                                                    stitchItems.map { it.copy(trimStart = 0f, trimEnd = 0f) }
                                                } else {
                                                    stitchItems.map { it.copy(trimStart = 0.045f, trimEnd = 0.055f) }
                                                }
                                            },
                                            shape = CircleShape,
                                            color = if (allTrimmed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_ms_thin_crop),
                                                    contentDescription = null,
                                                    tint = if (allTrimmed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (allTrimmed) "Restore Bars" else "Trim All Bars",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (allTrimmed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    ReorderableThumbnailStrip(
                                        items = stitchItems,
                                        selectedIndex = selectedClipIndex,
                                        onSelect = { idx ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedClipIndex = if (selectedClipIndex == idx) -1 else idx
                                        },
                                        onReorder = { from, to ->
                                            pushUndo()
                                            stitchItems = stitchItems.toMutableList().also {
                                                it.add(to, it.removeAt(from))
                                            }
                                            selectedClipIndex = to
                                        },
                                        onRemove = { index ->
                                            if (stitchItems.size > 2) {
                                                pushUndo()
                                                stitchItems = stitchItems.toMutableList().also { it.removeAt(index) }
                                                selectedClipIndex = -1
                                            } else {
                                                Toast.makeText(context, "Need at least 2 images", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onAddMore = { showGalleryPicker = true }
                                    )
                                }
                            }

                            StitchStudioTab.STYLE -> {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(StitchStyle.entries) { style ->
                                        val isSelected = selectedStyle == style
                                        Surface(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                applyStylePreset(style)
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier.width(104.dp).height(104.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                StitchStyleMiniPreview(style = style, isSelected = isSelected, modifier = Modifier.size(44.dp))
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = style.label,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = style.subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            StitchStudioTab.LAYOUT -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Direction + Scale Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = ShapeFull,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                StitchPillOption(
                                                    selected = orientation == StitchOrientation.VERTICAL,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_unfold_more),
                                                    label = "Vertical",
                                                    onClick = {
                                                        pushUndo()
                                                        orientation = StitchOrientation.VERTICAL
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StitchPillOption(
                                                    selected = orientation == StitchOrientation.HORIZONTAL,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                                    label = "Horizontal",
                                                    onClick = {
                                                        pushUndo()
                                                        orientation = StitchOrientation.HORIZONTAL
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = ShapeFull,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.weight(0.78f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                StitchPillOption(
                                                    selected = scaleMode == StitchScaleMode.FILL,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_crop),
                                                    label = "Fill",
                                                    onClick = {
                                                        pushUndo()
                                                        scaleMode = StitchScaleMode.FILL
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StitchPillOption(
                                                    selected = scaleMode == StitchScaleMode.FIT,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_aspect_ratio),
                                                    label = "Fit",
                                                    onClick = {
                                                        pushUndo()
                                                        scaleMode = StitchScaleMode.FIT
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // Cross-axis Alignment Row (Active when Fit Original is used)
                                    val alignEnabled = scaleMode == StitchScaleMode.FIT
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (alignEnabled) 1f else 0.45f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (alignEnabled) "Cross-axis Align" else "Align (Fit mode only)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = ShapeFull,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.width(180.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                StitchIconPillOption(
                                                    selected = alignment == StitchAlignment.START,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_thin_format_align_left),
                                                    contentDescription = "Align Start",
                                                    onClick = {
                                                        if (alignEnabled) {
                                                            pushUndo()
                                                            alignment = StitchAlignment.START
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StitchIconPillOption(
                                                    selected = alignment == StitchAlignment.CENTER,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_thin_format_align_center),
                                                    contentDescription = "Align Center",
                                                    onClick = {
                                                        if (alignEnabled) {
                                                            pushUndo()
                                                            alignment = StitchAlignment.CENTER
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StitchIconPillOption(
                                                    selected = alignment == StitchAlignment.END,
                                                    icon = ImageVector.vectorResource(R.drawable.ic_ms_thin_format_align_right),
                                                    contentDescription = "Align End",
                                                    onClick = {
                                                        if (alignEnabled) {
                                                            pushUndo()
                                                            alignment = StitchAlignment.END
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            StitchStudioTab.FRAME -> {
                                val targetIndex = if (selectedClipIndex in stitchItems.indices) selectedClipIndex else -1
                                val currentTrimPct = if (targetIndex >= 0) {
                                    (stitchItems[targetIndex].trimStart * 100f).roundToInt().toFloat()
                                } else {
                                    ((stitchItems.firstOrNull()?.trimStart ?: 0f) * 100f).roundToInt().toFloat()
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(StitchFrameParam.entries) { param ->
                                            val isSelected = activeFrameParam == param
                                            val displayVal = when (param) {
                                                StitchFrameParam.GAP -> "${spacingDp.roundToInt()}"
                                                StitchFrameParam.MARGIN -> "${outerMarginDp.roundToInt()}"
                                                StitchFrameParam.CORNERS -> "${cornerRadiusDp.roundToInt()}"
                                                StitchFrameParam.BORDER -> "${borderWidthDp.roundToInt()}"
                                                StitchFrameParam.TRIM_BARS -> "${currentTrimPct.roundToInt()}%"
                                            }
                                            Surface(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    activeFrameParam = param
                                                },
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = param.label,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = displayVal,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val activeVal = when (activeFrameParam) {
                                        StitchFrameParam.GAP -> spacingDp
                                        StitchFrameParam.MARGIN -> outerMarginDp
                                        StitchFrameParam.CORNERS -> cornerRadiusDp
                                        StitchFrameParam.BORDER -> borderWidthDp
                                        StitchFrameParam.TRIM_BARS -> currentTrimPct
                                    }
                                    EditorRulerSlider(
                                        value = activeVal,
                                        onValueChange = { v ->
                                            when (activeFrameParam) {
                                                StitchFrameParam.GAP -> spacingDp = v
                                                StitchFrameParam.MARGIN -> outerMarginDp = v
                                                StitchFrameParam.CORNERS -> cornerRadiusDp = v
                                                StitchFrameParam.BORDER -> borderWidthDp = v
                                                StitchFrameParam.TRIM_BARS -> {
                                                    val frac = (v / 100f).coerceIn(0f, 0.25f)
                                                    stitchItems = if (targetIndex >= 0) {
                                                        stitchItems.toMutableList().also {
                                                            it[targetIndex] = it[targetIndex].copy(trimStart = frac, trimEnd = frac)
                                                        }
                                                    } else {
                                                        stitchItems.map { it.copy(trimStart = frac, trimEnd = frac) }
                                                    }
                                                }
                                            }
                                        },
                                        range = activeFrameParam.min..activeFrameParam.max,
                                        step = 1f,
                                        defaultValue = activeFrameParam.defaultVal,
                                        valueFormatter = { v ->
                                            if (activeFrameParam == StitchFrameParam.TRIM_BARS) "${v.roundToInt()}%" else "${v.roundToInt()}dp"
                                        },
                                        showDoneButton = false
                                    )
                                }
                            }

                            StitchStudioTab.BACKDROP -> {
                                val bgOptions = listOf(
                                    StitchBackground.Solid(Color.Black, "Black"),
                                    StitchBackground.Solid(Color.White, "White"),
                                    StitchBackground.Solid(Color(0xFFF6F2EA), "Paper"),
                                    StitchBackground.Solid(Color(0xFF101113), "Cinema"),
                                    StitchBackground.Pattern(Color(0xFFF7F3EB), Color(0x28000000), false, "Dots"),
                                    StitchBackground.Pattern(Color(0xFF181A1F), Color(0x28FFFFFF), true, "Blueprint"),
                                    StitchBackground.Solid(MaterialTheme.colorScheme.primaryContainer, "Primary"),
                                    StitchBackground.Solid(MaterialTheme.colorScheme.secondaryContainer, "Tonal"),
                                    StitchBackground.Solid(MaterialTheme.colorScheme.tertiaryContainer, "Accent"),
                                    StitchBackground.Gradient(Color(0xFFFF8A65), Color(0xFFFFD54F), "Sunset"),
                                    StitchBackground.Gradient(Color(0xFF4FC3F7), Color(0xFF81C784), "Lagoon"),
                                    StitchBackground.Gradient(Color(0xFFCE93D8), Color(0xFF90CAF9), "Aurora"),
                                    StitchBackground.Gradient(Color(0xFF1A237E), Color(0xFF4A148C), "Cosmic"),
                                    StitchBackground.Gradient(Color(0xFF2E7D32), Color(0xFF80CBC4), "Moss")
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(bgOptions) { _, bg ->
                                        val isSelected = selectedBackground == bg
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                pushUndo()
                                                selectedBackground = bg
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StitchBackdropCanvas(
                                                    background = bg,
                                                    isFilmStyle = false,
                                                    orientation = orientation,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.ic_ms_thin_check),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Text(
                                                text = bg.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Google Photos Pitch-Black Tab Dock ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(StitchStudioTab.entries) { tab ->
                            val isSelected = tab == activeTab
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF1C191D) else Color.Transparent,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "stitchTabBg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "stitchTabBorder"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "stitchTabFg"
                            )

                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeTab = tab
                                },
                                shape = CircleShape,
                                color = containerColor,
                                border = if (isSelected) BorderStroke(1.dp, borderColor) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = tab.iconRes),
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = contentColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Save Options Sheet ────────────────────────────────────────────────
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
                                style = selectedStyle,
                                spacingPx = spacingDp * context.resources.displayMetrics.density,
                                outerMarginPx = outerMarginDp * context.resources.displayMetrics.density,
                                cornerRadiusPx = cornerRadiusDp * context.resources.displayMetrics.density,
                                borderWidthPx = borderWidthDp * context.resources.displayMetrics.density,
                                alignment = alignment,
                                scaleMode = scaleMode,
                                background = selectedBackground,
                                format = format
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Saved to gallery!", Toast.LENGTH_SHORT).show()
                                isSaving = false
                                onBack()
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error saving: ${e.message ?: "Out of memory"}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isSaving = false
                            }
                        }
                    }
                }
            )
        }

        // ── Add-More Gallery Chooser ──────────────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = showGalleryPicker,
            enter = fadeIn(MotionTokens.gentleSpring()) + slideInVertically(MotionTokens.gentleSpring()) { it },
            exit = fadeOut(MotionTokens.gentleSpring()) + slideOutVertically(MotionTokens.gentleSpring()) { it }
        ) {
            LocalGalleryChooser(
                mediaList = imageMedia,
                maxSelection = 30,
                confirmLabelPrefix = "Add",
                onDismiss = { showGalleryPicker = false },
                onConfirm = { chosenUris ->
                    pushUndo()
                    stitchItems = stitchItems + chosenUris.map { StitchItem(it) }
                    showGalleryPicker = false
                }
            )
        }
    }
    }
}

// ── EXIF Dimensions Helper ────────────────────────────────────────────────────

private fun readExifOrientedDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        val rawW = opts.outWidth
        val rawH = opts.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        val exifOrientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> Pair(rawH, rawW)
            else -> Pair(rawW, rawH)
        }
    } catch (_: Exception) {
        null
    }
}

// ── StitchPreview ─────────────────────────────────────────────────────────────

@Composable
private fun StitchPreview(
    items: List<StitchItem>,
    orientation: StitchOrientation,
    style: StitchStyle,
    spacingDp: Float,
    outerMarginDp: Float,
    cornerRadiusDp: Float,
    borderWidthDp: Float,
    background: StitchBackground,
    alignment: StitchAlignment,
    scaleMode: StitchScaleMode,
    selectedClipIndex: Int,
    onSelectClip: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Decode EXIF-corrected natural dimensions for every URI
    val itemBaseDims = remember { mutableStateMapOf<Uri, Pair<Int, Int>>() }
    LaunchedEffect(items) {
        withContext(Dispatchers.IO) {
            items.forEach { item ->
                if (!itemBaseDims.containsKey(item.uri)) {
                    itemBaseDims[item.uri] = readExifOrientedDimensions(context, item.uri) ?: Pair(1000, 1000)
                }
            }
        }
    }

    val safeGap by animateDpAsState(spacingDp.dp, MotionTokens.snappySpring(), label = "stitchGap")
    val safeMargin by animateDpAsState(outerMarginDp.dp, MotionTokens.snappySpring(), label = "stitchMargin")
    val safeCorner by animateDpAsState(cornerRadiusDp.dp, MotionTokens.snappySpring(), label = "stitchCorner")

    var contentWidthPx by remember { mutableFloatStateOf(0f) }
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    val fitMarginPx = with(density) { 24.dp.toPx() }
    val availW = (viewportWidthPx - fitMarginPx).coerceAtLeast(10f)
    val availH = (viewportHeightPx - fitMarginPx).coerceAtLeast(10f)

    val fitScale = remember(contentWidthPx, contentHeightPx, availW, availH) {
        if (contentWidthPx > 0f && contentHeightPx > 0f) {
            minOf(availW / contentWidthPx, availH / contentHeightPx, 1.0f)
        } else 1.0f
    }

    val minAllowedScale = minOf(fitScale * 0.4f, 0.05f)
    val maxAllowedScale = 5.0f

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var hasInitializedFit by remember { mutableStateOf(false) }

    val animScope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun animateTo(targetScale: Float, targetX: Float = 0f, targetY: Float = 0f) {
        animJob?.cancel()
        animJob = animScope.launch {
            launch {
                androidx.compose.animation.core.animate(
                    initialValue = scale,
                    targetValue = targetScale,
                    animationSpec = MotionTokens.gentleSpring()
                ) { v, _ -> scale = v }
            }
            launch {
                androidx.compose.animation.core.animate(
                    initialValue = offsetX,
                    targetValue = targetX,
                    animationSpec = MotionTokens.gentleSpring()
                ) { v, _ -> offsetX = v }
            }
            launch {
                androidx.compose.animation.core.animate(
                    initialValue = offsetY,
                    targetValue = targetY,
                    animationSpec = MotionTokens.gentleSpring()
                ) { v, _ -> offsetY = v }
            }
        }
    }

    LaunchedEffect(fitScale, orientation, items.size) {
        if (fitScale > 0.001f && fitScale < 1.0f) {
            if (!hasInitializedFit || abs(scale - fitScale) < 0.05f) {
                scale = fitScale
                offsetX = 0f
                offsetY = 0f
                hasInitializedFit = true
            }
        } else if (!hasInitializedFit && fitScale >= 1.0f) {
            hasInitializedFit = true
        }
    }

    // Precompute effective rotated dimensions per clip for FIT scaling
    val effectiveDims = remember(items, itemBaseDims.toMap()) {
        items.map { item ->
            val (bw, bh) = itemBaseDims[item.uri] ?: Pair(1000, 1000)
            if (item.rotation % 180 != 0) Pair(bh, bw) else Pair(bw, bh)
        }
    }
    val maxCrossDim = remember(effectiveDims, orientation) {
        if (orientation == StitchOrientation.VERTICAL) {
            (effectiveDims.maxOfOrNull { it.first } ?: 1000).coerceAtLeast(1)
        } else {
            (effectiveDims.maxOfOrNull { it.second } ?: 1000).coerceAtLeast(1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pointerInput(items, orientation, fitScale) {
                detectTapGestures(
                    onDoubleTap = { centroid ->
                        val isNearFit = abs(scale - fitScale) < 0.05f
                        if (isNearFit || scale < 0.95f) {
                            val targetScale = 1.0f
                            val focalX = centroid.x - viewportWidthPx / 2f
                            val focalY = centroid.y - viewportHeightPx / 2f
                            val effectiveZoom = if (scale > 0f) targetScale / scale else 1f
                            val targetX = (offsetX - focalX) * effectiveZoom + focalX
                            val targetY = (offsetY - focalY) * effectiveZoom + focalY

                            val scaledW = contentWidthPx * targetScale
                            val scaledH = contentHeightPx * targetScale
                            val maxX = maxOf(0f, (scaledW - viewportWidthPx) / 2f)
                            val maxY = maxOf(0f, (scaledH - viewportHeightPx) / 2f)

                            animateTo(
                                targetScale,
                                if (scaledW <= viewportWidthPx) 0f else targetX.coerceIn(-maxX, maxX),
                                if (scaledH <= viewportHeightPx) 0f else targetY.coerceIn(-maxY, maxY)
                            )
                        } else {
                            animateTo(fitScale, 0f, 0f)
                        }
                    }
                )
            }
            .pointerInput(items, orientation, fitScale, contentWidthPx, contentHeightPx) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var accumulatedZoom = 1f
                    var accumulatedPan = Offset.Zero
                    val touchSlop = viewConfiguration.touchSlop
                    var pastTouchSlop = false
                    val velocityTracker = VelocityTracker()
                    var pointerCount = 0

                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        pointerCount = maxOf(pointerCount, pressed.size)

                        if (pressed.size == 1) {
                            velocityTracker.addPointerInputChange(pressed[0])
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (!pastTouchSlop) {
                            accumulatedZoom *= zoomChange
                            accumulatedPan += panChange
                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - accumulatedZoom) * centroidSize
                            val panMotion = accumulatedPan.getDistance()
                            if (zoomMotion > touchSlop || panMotion > touchSlop || pressed.size >= 2) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero || pressed.size >= 2)) {
                            animJob?.cancel()
                            val centroid = event.calculateCentroid(useCurrent = false)
                            val oldScale = scale
                            val newScale = (oldScale * zoomChange).coerceIn(minAllowedScale, maxAllowedScale)
                            val effectiveZoom = if (oldScale > 0f) newScale / oldScale else 1f

                            val focalX = centroid.x - viewportWidthPx / 2f
                            val focalY = centroid.y - viewportHeightPx / 2f

                            val newX = (offsetX - focalX) * effectiveZoom + focalX + panChange.x
                            val newY = (offsetY - focalY) * effectiveZoom + focalY + panChange.y

                            val scaledW = contentWidthPx * newScale
                            val scaledH = contentHeightPx * newScale
                            val maxX = maxOf(0f, (scaledW - viewportWidthPx) / 2f)
                            val maxY = maxOf(0f, (scaledH - viewportHeightPx) / 2f)

                            scale = newScale
                            offsetX = if (scaledW <= viewportWidthPx) 0f else newX.coerceIn(-maxX - 80f, maxX + 80f)
                            offsetY = if (scaledH <= viewportHeightPx) 0f else newY.coerceIn(-maxY - 80f, maxY + 80f)

                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    if (scale < fitScale * 0.98f) {
                        animateTo(fitScale, 0f, 0f)
                    } else if (scale > maxAllowedScale) {
                        animateTo(maxAllowedScale, offsetX, offsetY)
                    } else {
                        val scaledW = contentWidthPx * scale
                        val scaledH = contentHeightPx * scale
                        val maxX = maxOf(0f, (scaledW - viewportWidthPx) / 2f)
                        val maxY = maxOf(0f, (scaledH - viewportHeightPx) / 2f)

                        if (pointerCount == 1 && (scaledW > viewportWidthPx || scaledH > viewportHeightPx)) {
                            val velocity = velocityTracker.calculateVelocity()
                            val flingFactor = 0.25f
                            animateTo(
                                scale,
                                (offsetX + velocity.x * flingFactor).coerceIn(-maxX, maxX),
                                (offsetY + velocity.y * flingFactor).coerceIn(-maxY, maxY)
                            )
                        } else {
                            val clampedX = if (scaledW <= viewportWidthPx) 0f else offsetX.coerceIn(-maxX, maxX)
                            val clampedY = if (scaledH <= viewportHeightPx) 0f else offsetY.coerceIn(-maxY, maxY)
                            if (clampedX != offsetX || clampedY != offsetY) {
                                animateTo(scale, clampedX, clampedY)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        viewportWidthPx = with(density) { maxWidth.toPx() }
        viewportHeightPx = with(density) { maxHeight.toPx() }

        val filmSidePad = if (style == StitchStyle.FILM && orientation == StitchOrientation.VERTICAL) 16.dp else 0.dp
        val filmVertPad = if (style == StitchStyle.FILM && orientation == StitchOrientation.HORIZONTAL) 16.dp else 0.dp
        val clampedGap = safeGap.coerceAtLeast(0.dp)
        val clampedMargin = safeMargin.coerceAtLeast(0.dp)
        val clampedCorner = safeCorner.coerceAtLeast(0.dp)

        val artboardBaseModifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
            .let { mod ->
                if (orientation == StitchOrientation.VERTICAL) {
                    mod.width(maxWidth).wrapContentHeight(unbounded = true)
                } else {
                    mod.height(maxHeight).wrapContentWidth(unbounded = true)
                }
            }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .onSizeChanged {
                if (it.width > 0) contentWidthPx = it.width.toFloat()
                if (it.height > 0) contentHeightPx = it.height.toFloat()
            }

        Box(modifier = artboardBaseModifier) {
            StitchBackdropCanvas(
                background = background,
                isFilmStyle = style == StitchStyle.FILM,
                orientation = orientation,
                modifier = Modifier.matchParentSize()
            )

            val contentPadding = PaddingValues(
                start = clampedMargin + filmSidePad,
                end = clampedMargin + filmSidePad,
                top = clampedMargin + filmVertPad,
                bottom = clampedMargin + filmVertPad
            )

            if (orientation == StitchOrientation.VERTICAL) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(clampedGap),
                    horizontalAlignment = when (alignment) {
                        StitchAlignment.START -> Alignment.Start
                        StitchAlignment.CENTER -> Alignment.CenterHorizontally
                        StitchAlignment.END -> Alignment.End
                    }
                ) {
                    items.forEachIndexed { idx, item ->
                        val (effW, effH) = effectiveDims[idx]
                        val crossFraction = if (scaleMode == StitchScaleMode.FIT) {
                            (effW.toFloat() / maxCrossDim.toFloat()).coerceIn(0.25f, 1f)
                        } else 1f
                        StitchPreviewClipCard(
                            item = item,
                            effWidth = effW,
                            effHeight = effH,
                            orientation = orientation,
                            style = style,
                            cornerRadiusDp = clampedCorner,
                            borderWidthDp = borderWidthDp,
                            isSelected = idx == selectedClipIndex,
                            onClick = { onSelectClip(idx) },
                            modifier = Modifier.fillMaxWidth(crossFraction)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(clampedGap),
                    verticalAlignment = when (alignment) {
                        StitchAlignment.START -> Alignment.Top
                        StitchAlignment.CENTER -> Alignment.CenterVertically
                        StitchAlignment.END -> Alignment.Bottom
                    }
                ) {
                    items.forEachIndexed { idx, item ->
                        val (effW, effH) = effectiveDims[idx]
                        val crossFraction = if (scaleMode == StitchScaleMode.FIT) {
                            (effH.toFloat() / maxCrossDim.toFloat()).coerceIn(0.25f, 1f)
                        } else 1f
                        StitchPreviewClipCard(
                            item = item,
                            effWidth = effW,
                            effHeight = effH,
                            orientation = orientation,
                            style = style,
                            cornerRadiusDp = clampedCorner,
                            borderWidthDp = borderWidthDp,
                            isSelected = idx == selectedClipIndex,
                            onClick = { onSelectClip(idx) },
                            modifier = Modifier.fillMaxHeight(crossFraction)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchPreviewClipCard(
    item: StitchItem,
    effWidth: Int,
    effHeight: Int,
    orientation: StitchOrientation,
    style: StitchStyle,
    cornerRadiusDp: androidx.compose.ui.unit.Dp,
    borderWidthDp: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadiusDp)
    val keepFrac = (1f - item.trimStart - item.trimEnd).coerceIn(0.4f, 1f)
    val rotRatio = effWidth.toFloat() / effHeight.coerceAtLeast(1).toFloat()
    val trimmedRatio = if (orientation == StitchOrientation.VERTICAL) {
        (rotRatio / keepFrac).coerceAtLeast(0.1f)
    } else {
        (rotRatio * keepFrac).coerceAtLeast(0.1f)
    }

    val isPolaroid = style == StitchStyle.POLAROID
    val isElevated = style == StitchStyle.SHADOW_CARDS

    val outerMod = modifier
        .then(
            if (isElevated || isPolaroid) {
                Modifier
                    .shadow(if (isElevated) 8.dp else 4.dp, shape, clip = false)
                    .background(Color.White, shape)
                    .padding(
                        start = borderWidthDp.coerceAtLeast(if (isPolaroid) 4f else 0f).dp,
                        top = borderWidthDp.coerceAtLeast(if (isPolaroid) 4f else 0f).dp,
                        end = borderWidthDp.coerceAtLeast(if (isPolaroid) 4f else 0f).dp,
                        bottom = if (isPolaroid) (borderWidthDp.coerceAtLeast(4f) * 2.8f).dp else borderWidthDp.coerceAtLeast(0f).dp
                    )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .then(
            if (!isPolaroid && !isElevated && borderWidthDp > 0.2f) {
                Modifier.border(borderWidthDp.dp, Color.White, shape)
            } else Modifier
        )
        .border(
            width = if (isSelected) 2.5.dp else 0.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = shape
        )
        .clickable(onClick = onClick)

    Box(
        modifier = outerMod.then(
            if (orientation == StitchOrientation.VERTICAL) {
                Modifier.aspectRatio(trimmedRatio)
            } else {
                Modifier.aspectRatio(trimmedRatio, matchHeightConstraintsFirst = true)
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        val trimShiftFraction = (item.trimEnd - item.trimStart) * 0.5f
        val scaleForTrim = 1f / keepFrac
        val swapsAxes = (item.rotation % 180) != 0

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(1200, 2400)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .layout { measurable, constraints ->
                    val w = constraints.maxWidth
                    val h = constraints.maxHeight
                    val untrimmedW = if (orientation == StitchOrientation.HORIZONTAL) (w * scaleForTrim).roundToInt() else w
                    val untrimmedH = if (orientation == StitchOrientation.VERTICAL) (h * scaleForTrim).roundToInt() else h
                    val childW = if (swapsAxes) untrimmedH else untrimmedW
                    val childH = if (swapsAxes) untrimmedW else untrimmedH
                    val placeable = measurable.measure(Constraints.fixed(childW.coerceAtLeast(1), childH.coerceAtLeast(1)))
                    layout(w, h) {
                        val shiftX = if (orientation == StitchOrientation.HORIZONTAL) (trimShiftFraction * untrimmedW) else 0f
                        val shiftY = if (orientation == StitchOrientation.VERTICAL) (trimShiftFraction * untrimmedH) else 0f
                        placeable.placeRelativeWithLayer(
                            x = (w - placeable.width) / 2,
                            y = (h - placeable.height) / 2
                        ) {
                            rotationZ = item.rotation.toFloat()
                            scaleX = if (item.flipHorizontal) -1f else 1f
                            translationX = shiftX
                            translationY = shiftY
                        }
                    }
                }
        )
    }
}

// ── Backdrop & Style Mini-Preview Canvases ────────────────────────────────────

@Composable
private fun StitchBackdropCanvas(
    background: StitchBackground,
    isFilmStyle: Boolean,
    orientation: StitchOrientation,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (background) {
            is StitchBackground.Solid -> drawRect(color = background.color)
            is StitchBackground.Gradient -> drawRect(
                brush = Brush.linearGradient(colors = listOf(background.start, background.end))
            )
            is StitchBackground.Pattern -> {
                drawRect(color = background.baseColor)
                val step = 16.dp.toPx()
                if (background.isGrid) {
                    var x = step
                    while (x < w) {
                        drawLine(background.dotColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                        x += step
                    }
                    var y = step
                    while (y < h) {
                        drawLine(background.dotColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                        y += step
                    }
                } else {
                    var x = step / 2f
                    while (x < w) {
                        var y = step / 2f
                        while (y < h) {
                            drawCircle(background.dotColor, radius = 1.6.dp.toPx(), center = Offset(x, y))
                            y += step
                        }
                        x += step
                    }
                }
            }
        }

        if (isFilmStyle) {
            val holeLen = 8.dp.toPx()
            val holeThick = 5.dp.toPx()
            val step = 16.dp.toPx()
            val edgeMargin = 5.dp.toPx()
            if (orientation == StitchOrientation.VERTICAL) {
                // Sprockets along Left & Right borders
                val leftX = edgeMargin
                val rightX = w - edgeMargin - holeThick
                var y = 10.dp.toPx()
                while (y + holeLen < h - 6.dp.toPx()) {
                    drawRoundRect(Color.White.copy(alpha = 0.82f), Offset(leftX, y), Size(holeThick, holeLen), CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = 0.82f), Offset(rightX, y), Size(holeThick, holeLen), CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()))
                    y += step
                }
            } else {
                // Sprockets along Top & Bottom borders
                val topY = edgeMargin
                val botY = h - edgeMargin - holeThick
                var x = 10.dp.toPx()
                while (x + holeLen < w - 6.dp.toPx()) {
                    drawRoundRect(Color.White.copy(alpha = 0.82f), Offset(x, topY), Size(holeLen, holeThick), CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = 0.82f), Offset(x, botY), Size(holeLen, holeThick), CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()))
                    x += step
                }
            }
        }
    }
}

@Composable
private fun StitchStyleMiniPreview(
    style: StitchStyle,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val fg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
        when (style) {
            StitchStyle.SEAMLESS -> {
                drawRect(fg, Offset(w * 0.25f, 3f), Size(w * 0.5f, h - 6f), style = stroke)
                drawLine(fg.copy(alpha = 0.5f), Offset(w * 0.25f, h * 0.5f), Offset(w * 0.75f, h * 0.5f), strokeWidth = 1.4.dp.toPx())
            }
            StitchStyle.MODERN -> {
                drawRoundRect(fg, Offset(w * 0.22f, 2f), Size(w * 0.56f, h * 0.42f), CornerRadius(7f, 7f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.22f, h * 0.54f), Size(w * 0.56f, h * 0.42f), CornerRadius(7f, 7f), style = stroke)
            }
            StitchStyle.FILM -> {
                drawRoundRect(fg, Offset(4f, 2f), Size(w - 8f, h - 4f), CornerRadius(4f, 4f), style = stroke)
                for (i in 0..3) {
                    val y = 8f + i * ((h - 16f) / 3.2f)
                    drawCircle(fg, radius = 2f, center = Offset(9f, y))
                    drawCircle(fg, radius = 2f, center = Offset(w - 9f, y))
                }
            }
            StitchStyle.POLAROID -> {
                drawRoundRect(fg, Offset(w * 0.2f, 2f), Size(w * 0.6f, h - 4f), CornerRadius(5f, 5f), style = stroke)
                drawRect(fg.copy(alpha = 0.35f), Offset(w * 0.28f, h * 0.14f), Size(w * 0.44f, h * 0.28f))
                drawRect(fg.copy(alpha = 0.35f), Offset(w * 0.28f, h * 0.52f), Size(w * 0.44f, h * 0.28f))
            }
            StitchStyle.SHADOW_CARDS -> {
                drawRoundRect(fg.copy(alpha = 0.35f), Offset(w * 0.24f + 4f, 6f), Size(w * 0.52f, h * 0.38f), CornerRadius(8f, 8f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.22f, 3f), Size(w * 0.52f, h * 0.38f), CornerRadius(8f, 8f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.22f, h * 0.54f), Size(w * 0.52f, h * 0.38f), CornerRadius(8f, 8f), style = stroke)
            }
        }
    }
}

// ── ReorderableThumbnailStrip ─────────────────────────────────────────────────

@Composable
private fun ReorderableThumbnailStrip(
    items: List<StitchItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onAddMore: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(0) }

    val itemCentersX = remember { mutableStateMapOf<Int, Float>() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isDragging = draggingIndex == index
            val isSelected = index == selectedIndex

            val liftElevation by animateDpAsState(
                targetValue = if (isDragging) 10.dp else 0.dp,
                animationSpec = MotionTokens.snappySpring(),
                label = "stitchThumbElevation"
            )
            val liftScale by animateFloatAsState(
                targetValue = if (isDragging) 1.1f else 1f,
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
                    .size(72.dp)
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
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = if (isDragging || isSelected) 2.5.dp else 0.dp,
                        color = if (isDragging || isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !isDragging) { onSelect(index) }
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
                        .size(160, 160)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Clip ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp)
                        .size(20.dp)
                        .align(Alignment.TopStart),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 4.dp)
                        .size(20.dp)
                        .graphicsLayer { alpha = removeBadgeAlpha }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f))
                        .clickable(enabled = !isDragging) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRemove(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
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
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ── Layout Control Pills ──────────────────────────────────────────────────────

@Composable
private fun StitchPillOption(
    selected: Boolean,
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = ShapeFull,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StitchIconPillOption(
    selected: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = ShapeFull,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
                text = "Export Stitched Studio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose an export format for your stitched media.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                description = "Multi-page document — printing & archiving",
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
                    maxLines = 1
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.80f),
                    maxLines = 1,
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

// ── EXIF-Aware Decode + Clip Transform & Render Export ────────────────────────

private fun decodeTransformedClipBitmap(
    context: Context,
    item: StitchItem,
    reqWidth: Int,
    reqHeight: Int,
    orientation: StitchOrientation
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sample = 1
        while (origW / (sample * 2) >= reqWidth && origH / (sample * 2) >= reqHeight) {
            sample *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = context.contentResolver.openInputStream(item.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val exifOrientation = context.contentResolver.openInputStream(item.uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (item.rotation != 0) {
            matrix.postRotate(item.rotation.toFloat())
        }
        if (item.flipHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        val oriented = if (!matrix.isIdentity) {
            val bmp = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (bmp != raw) raw.recycle()
            bmp
        } else raw

        // Apply start/end screenshot bar trim
        val hasTrim = item.trimStart > 0.002f || item.trimEnd > 0.002f
        if (!hasTrim) return oriented

        val w = oriented.width
        val h = oriented.height
        val cropRect = if (orientation == StitchOrientation.VERTICAL) {
            val topPx = (h * item.trimStart).roundToInt().coerceIn(0, h - 10)
            val botPx = (h * (1f - item.trimEnd)).roundToInt().coerceIn(topPx + 10, h)
            Bitmap.createBitmap(oriented, 0, topPx, w, (botPx - topPx).coerceAtLeast(1))
        } else {
            val leftPx = (w * item.trimStart).roundToInt().coerceIn(0, w - 10)
            val rightPx = (w * (1f - item.trimEnd)).roundToInt().coerceIn(leftPx + 10, w)
            Bitmap.createBitmap(oriented, leftPx, 0, (rightPx - leftPx).coerceAtLeast(1), h)
        }
        if (cropRect != oriented) oriented.recycle()
        cropRect
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

private fun drawStitchBackdropToCanvas(
    canvas: android.graphics.Canvas,
    width: Int,
    height: Int,
    background: StitchBackground,
    isFilmStyle: Boolean,
    orientation: StitchOrientation,
    scaleFactor: Float
) {
    when (background) {
        is StitchBackground.Solid -> canvas.drawColor(background.color.toArgb())
        is StitchBackground.Gradient -> {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    background.start.toArgb(), background.end.toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        }
        is StitchBackground.Pattern -> {
            canvas.drawColor(background.baseColor.toArgb())
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = background.dotColor.toArgb()
                strokeWidth = 1f * scaleFactor
            }
            val step = 16f * scaleFactor
            if (background.isGrid) {
                var x = step
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), p)
                    x += step
                }
                var y = step
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, p)
                    y += step
                }
            } else {
                var x = step / 2f
                while (x < width) {
                    var y = step / 2f
                    while (y < height) {
                        canvas.drawCircle(x, y, 1.6f * scaleFactor, p)
                        y += step
                    }
                    x += step
                }
            }
        }
    }

    if (isFilmStyle) {
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(210, 255, 255, 255)
        }
        val holeLen = 8f * scaleFactor
        val holeThick = 5f * scaleFactor
        val step = 16f * scaleFactor
        val edge = 5f * scaleFactor
        if (orientation == StitchOrientation.VERTICAL) {
            val leftX = edge
            val rightX = width - edge - holeThick
            var y = 10f * scaleFactor
            while (y + holeLen < height - 6f * scaleFactor) {
                canvas.drawRoundRect(RectF(leftX, y, leftX + holeThick, y + holeLen), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
                canvas.drawRoundRect(RectF(rightX, y, rightX + holeThick, y + holeLen), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
                y += step
            }
        } else {
            val topY = edge
            val botY = height - edge - holeThick
            var x = 10f * scaleFactor
            while (x + holeLen < width - 6f * scaleFactor) {
                canvas.drawRoundRect(RectF(x, topY, x + holeLen, topY + holeThick), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
                canvas.drawRoundRect(RectF(x, botY, x + holeLen, botY + holeThick), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
                x += step
            }
        }
    }
}

private suspend fun saveStitchedImage(
    context: Context,
    items: List<StitchItem>,
    orientation: StitchOrientation,
    style: StitchStyle,
    spacingPx: Float,
    outerMarginPx: Float,
    cornerRadiusPx: Float,
    borderWidthPx: Float,
    alignment: StitchAlignment,
    scaleMode: StitchScaleMode,
    background: StitchBackground,
    format: String
) = withContext(Dispatchers.IO) {
    val maxDimension = 4096
    val density = context.resources.displayMetrics.density

    val boundsList = items.map { item ->
        val (bw, bh) = readExifOrientedDimensions(context, item.uri) ?: return@map null
        val (rw, rh) = if (item.rotation % 180 != 0) Pair(bh, bw) else Pair(bw, bh)
        val keep = (1f - item.trimStart - item.trimEnd).coerceIn(0.4f, 1f)
        if (orientation == StitchOrientation.VERTICAL) {
            Pair(rw, (rh * keep).roundToInt().coerceAtLeast(1))
        } else {
            Pair((rw * keep).roundToInt().coerceAtLeast(1), rh)
        }
    }
    val validBounds = boundsList.filterNotNull()
    if (validBounds.isEmpty()) throw Exception("Could not read image dimensions")

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
                drawStitchBackdropToCanvas(page.canvas, pageW, pageH, background, false, orientation, 1f)

                val bmp = decodeTransformedClipBitmap(context, item, pageW, pageH, orientation)
                if (bmp != null) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    page.canvas.drawBitmap(
                        bmp,
                        android.graphics.Rect(0, 0, bmp.width, bmp.height),
                        android.graphics.Rect(0, 0, pageW, pageH),
                        paint
                    )
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

    // Export scale multiplier relative to preview dp -> high-res pixels (~1200..3000px)
    val exportScale = (maxOf(
        if (orientation == StitchOrientation.VERTICAL) validBounds.maxOf { it.first } else validBounds.maxOf { it.second },
        1080
    ).coerceAtMost(maxDimension) / (360f * density)).coerceAtLeast(1f)

    val scaledGap = spacingPx * exportScale
    val scaledMargin = outerMarginPx * exportScale
    val scaledCorner = cornerRadiusPx * exportScale
    val scaledBorder = borderWidthPx * exportScale
    val filmSidePad = if (style == StitchStyle.FILM && orientation == StitchOrientation.VERTICAL) 16f * density * exportScale else 0f
    val filmVertPad = if (style == StitchStyle.FILM && orientation == StitchOrientation.HORIZONTAL) 16f * density * exportScale else 0f

    val padH = scaledMargin + filmSidePad
    val padV = scaledMargin + filmVertPad
    val totalGap = scaledGap * (validBounds.size - 1).coerceAtLeast(0)

    val isPolaroid = style == StitchStyle.POLAROID
    val isElevated = style == StitchStyle.SHADOW_CARDS

    val framePadL = if (isPolaroid || isElevated) maxOf(scaledBorder, if (isPolaroid) 4f * density * exportScale else 0f) else 0f
    val framePadB = if (isPolaroid) maxOf(scaledBorder, 4f * density * exportScale) * 2.8f else framePadL

    var (canvasWidth, canvasHeight) = if (orientation == StitchOrientation.VERTICAL) {
        val innerW = minOf(validBounds.maxOf { it.first }, maxDimension)
        val slotHeights = items.mapIndexedNotNull { idx, _ ->
            val (iw, ih) = boundsList[idx] ?: return@mapIndexedNotNull null
            val drawW = if (scaleMode == StitchScaleMode.FIT) minOf(iw, innerW) else innerW
            val picW = (drawW - framePadL * 2f).coerceAtLeast(1f)
            val picH = picW / iw.toFloat() * ih.toFloat()
            (picH + framePadL + framePadB).roundToInt()
        }
        val finalW = (innerW + padH * 2f).roundToInt().coerceAtLeast(1)
        val finalH = minOf((slotHeights.sum() + totalGap + padV * 2f).roundToInt(), maxDimension * 2).coerceAtLeast(1)
        Pair(finalW, finalH)
    } else {
        val innerH = minOf(validBounds.maxOf { it.second }, maxDimension)
        val slotWidths = items.mapIndexedNotNull { idx, _ ->
            val (iw, ih) = boundsList[idx] ?: return@mapIndexedNotNull null
            val drawH = if (scaleMode == StitchScaleMode.FIT) minOf(ih, innerH) else innerH
            val picH = (drawH - framePadL - framePadB).coerceAtLeast(1f)
            val picW = picH / ih.toFloat() * iw.toFloat()
            (picW + framePadL * 2f).roundToInt()
        }
        val finalW = minOf((slotWidths.sum() + totalGap + padH * 2f).roundToInt(), maxDimension * 2).coerceAtLeast(1)
        val finalH = (innerH + padV * 2f).roundToInt().coerceAtLeast(1)
        Pair(finalW, finalH)
    }

    val maxPixels = 16_777_216L
    val currentPixels = canvasWidth.toLong() * canvasHeight.toLong()
    val downscale = if (currentPixels > maxPixels) {
        kotlin.math.sqrt(maxPixels.toDouble() / currentPixels.toDouble()).toFloat()
    } else 1f
    canvasWidth = (canvasWidth * downscale).roundToInt().coerceAtLeast(1)
    canvasHeight = (canvasHeight * downscale).roundToInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawStitchBackdropToCanvas(canvas, canvasWidth, canvasHeight, background, style == StitchStyle.FILM, orientation, density * exportScale * downscale)

    val effGap = scaledGap * downscale
    val effPadH = padH * downscale
    val effPadV = padV * downscale
    val effCorner = scaledCorner * downscale
    val effBorder = scaledBorder * downscale
    val effFramePadL = framePadL * downscale
    val effFramePadB = framePadB * downscale

    val innerContentW = (canvasWidth - effPadH * 2f).coerceAtLeast(1f)
    val innerContentH = (canvasHeight - effPadV * 2f).coerceAtLeast(1f)
    val maxValidCross = if (orientation == StitchOrientation.VERTICAL) validBounds.maxOf { it.first } else validBounds.maxOf { it.second }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var cursor = if (orientation == StitchOrientation.VERTICAL) effPadV else effPadH

    items.forEachIndexed { index, item ->
        val (origW, origH) = boundsList.getOrNull(index) ?: return@forEachIndexed

        val outerRect: RectF
        val picRect: RectF
        if (orientation == StitchOrientation.VERTICAL) {
            val slotW = if (scaleMode == StitchScaleMode.FIT) {
                innerContentW * (origW.toFloat() / maxValidCross.toFloat()).coerceIn(0.25f, 1f)
            } else innerContentW
            val picW = (slotW - effFramePadL * 2f).coerceAtLeast(1f)
            val picH = picW / origW.toFloat() * origH.toFloat()
            val slotH = picH + effFramePadL + effFramePadB

            val left = effPadH + when (alignment) {
                StitchAlignment.START -> 0f
                StitchAlignment.CENTER -> (innerContentW - slotW) / 2f
                StitchAlignment.END -> innerContentW - slotW
            }
            outerRect = RectF(left, cursor, left + slotW, cursor + slotH)
            picRect = RectF(left + effFramePadL, cursor + effFramePadL, left + slotW - effFramePadL, cursor + effFramePadL + picH)
            cursor += slotH + effGap
        } else {
            val slotH = if (scaleMode == StitchScaleMode.FIT) {
                innerContentH * (origH.toFloat() / maxValidCross.toFloat()).coerceIn(0.25f, 1f)
            } else innerContentH
            val picH = (slotH - effFramePadL - effFramePadB).coerceAtLeast(1f)
            val picW = picH / origH.toFloat() * origW.toFloat()
            val slotW = picW + effFramePadL * 2f

            val top = effPadV + when (alignment) {
                StitchAlignment.START -> 0f
                StitchAlignment.CENTER -> (innerContentH - slotH) / 2f
                StitchAlignment.END -> innerContentH - slotH
            }
            outerRect = RectF(cursor, top, cursor + slotW, top + slotH)
            picRect = RectF(cursor + effFramePadL, top + effFramePadL, cursor + effFramePadL + picW, top + effFramePadL + picH)
            cursor += slotW + effGap
        }

        if (isPolaroid || isElevated) {
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                setShadowLayer(
                    (if (isElevated) 8f else 4f) * density * exportScale * downscale,
                    0f,
                    2f * density * exportScale * downscale,
                    android.graphics.Color.argb(65, 0, 0, 0)
                )
            }
            canvas.drawRoundRect(outerRect, effCorner, effCorner, cardPaint)
        }

        val clipPath = Path().apply {
            addRoundRect(picRect, effCorner, effCorner, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        val bmp = decodeTransformedClipBitmap(context, item, picRect.width().roundToInt().coerceAtLeast(1), picRect.height().roundToInt().coerceAtLeast(1), orientation)
        if (bmp != null) {
            canvas.drawBitmap(
                bmp,
                android.graphics.Rect(0, 0, bmp.width, bmp.height),
                RectF(picRect),
                paint
            )
            bmp.recycle()
        }
        canvas.restore()

        if (!isPolaroid && !isElevated && effBorder > 0.5f) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = effBorder
                color = android.graphics.Color.WHITE
            }
            canvas.drawRoundRect(picRect, effCorner, effCorner, borderPaint)
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
