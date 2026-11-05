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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.graphics.RectangleShape
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
import com.inferno.gallery.ui.utils.pressScale
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                        bgColor = bgColor,
                        alignment = alignment,
                        scaleMode = scaleMode
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
    bgColor: Color,
    alignment: StitchAlignment = StitchAlignment.CENTER,
    scaleMode: StitchScaleMode = StitchScaleMode.FILL
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Decode aspect ratios for instant, shift-free layout measurements
    val itemAspectRatios = remember { mutableStateMapOf<Uri, Float>() }
    LaunchedEffect(items) {
        withContext(Dispatchers.IO) {
            items.forEach { item ->
                if (!itemAspectRatios.containsKey(item.uri)) {
                    val ratio = try {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(item.uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, opts)
                        }
                        if (opts.outWidth > 0 && opts.outHeight > 0) {
                            opts.outWidth.toFloat() / opts.outHeight.toFloat()
                        } else 1f
                    } catch (e: Exception) {
                        1f
                    }
                    itemAspectRatios[item.uri] = ratio
                }
            }
        }
    }

    var contentWidthPx by remember { mutableFloatStateOf(0f) }
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    // Padding around the stitched canvas when fitted to screen
    val fitMarginPx = with(density) { 32.dp.toPx() }
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

    // Auto-fit initial view or adapt when orientation / items change while fitted
    LaunchedEffect(fitScale, orientation, items.size) {
        if (fitScale > 0.001f && fitScale < 1.0f) {
            if (!hasInitializedFit || kotlin.math.abs(scale - fitScale) < 0.05f) {
                scale = fitScale
                offsetX = 0f
                offsetY = 0f
                hasInitializedFit = true
            }
        } else if (!hasInitializedFit && fitScale >= 1.0f) {
            hasInitializedFit = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pointerInput(items, orientation, fitScale) {
                detectTapGestures(
                    onDoubleTap = { centroid ->
                        val isNearFit = kotlin.math.abs(scale - fitScale) < 0.05f
                        if (isNearFit || scale < 0.95f) {
                            // Zoom to 1.0f focused around centroid
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

                            val clampedX = if (scaledW <= viewportWidthPx) 0f else targetX.coerceIn(-maxX, maxX)
                            val clampedY = if (scaledH <= viewportHeightPx) 0f else targetY.coerceIn(-maxY, maxY)
                            animateTo(targetScale, clampedX, clampedY)
                        } else {
                            // Zoom out to full fit
                            animateTo(fitScale, 0f, 0f)
                        }
                    }
                )
            }
            .pointerInput(items, orientation, fitScale, contentWidthPx, contentHeightPx) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
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
                            val zoomMotion = kotlin.math.abs(1 - accumulatedZoom) * centroidSize
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

                            val rubberX = if (scaledW <= viewportWidthPx) 0f else newX.coerceIn(-maxX - 80f, maxX + 80f)
                            val rubberY = if (scaledH <= viewportHeightPx) 0f else newY.coerceIn(-maxY - 80f, maxY + 80f)

                            scale = newScale
                            offsetX = rubberX
                            offsetY = rubberY

                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })

                    // Gesture ended: snap back rubber-band or fling
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
                            val targetFlingX = (offsetX + velocity.x * flingFactor).coerceIn(-maxX, maxX)
                            val targetFlingY = (offsetY + velocity.y * flingFactor).coerceIn(-maxY, maxY)
                            animateTo(scale, targetFlingX, targetFlingY)
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

        // ── Canvas Artboard ──────────────────────────────────────────────────
        if (orientation == StitchOrientation.VERTICAL) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .width(maxWidth)
                    .wrapContentHeight(unbounded = true)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(2.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .background(bgColor)
                    .onSizeChanged {
                        if (it.width > 0) contentWidthPx = it.width.toFloat()
                        if (it.height > 0) contentHeightPx = it.height.toFloat()
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacingDp.dp),
                    horizontalAlignment = when (alignment) {
                        StitchAlignment.START -> Alignment.Start
                        StitchAlignment.CENTER -> Alignment.CenterHorizontally
                        StitchAlignment.END -> Alignment.End
                    }
                ) {
                    items.forEach { item ->
                        val ratio = itemAspectRatios[item.uri] ?: 1f
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .size(1080, 2400)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            onSuccess = { state ->
                                val w = state.painter.intrinsicSize.width
                                val h = state.painter.intrinsicSize.height
                                if (w > 0f && h > 0f) {
                                    itemAspectRatios[item.uri] = w / h
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .height(maxHeight)
                    .wrapContentWidth(unbounded = true)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(2.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .background(bgColor)
                    .onSizeChanged {
                        if (it.width > 0) contentWidthPx = it.width.toFloat()
                        if (it.height > 0) contentHeightPx = it.height.toFloat()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(spacingDp.dp),
                    verticalAlignment = when (alignment) {
                        StitchAlignment.START -> Alignment.Top
                        StitchAlignment.CENTER -> Alignment.CenterVertically
                        StitchAlignment.END -> Alignment.Bottom
                    }
                ) {
                    items.forEach { item ->
                        val ratio = itemAspectRatios[item.uri] ?: 1f
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .size(2400, 1080)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            onSuccess = { state ->
                                val w = state.painter.intrinsicSize.width
                                val h = state.painter.intrinsicSize.height
                                if (w > 0f && h > 0f) {
                                    itemAspectRatios[item.uri] = w / h
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                        )
                    }
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
                        .clip(RectangleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (isDragging) 2.dp else 0.dp,
                            color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RectangleShape
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                    )

                    // Order number badge
                    Surface(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .size(20.dp)
                            .align(Alignment.TopStart),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                        shadowElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f))
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

            // Add more button
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RectangleShape)
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
            // ── Row 1: Direction Selection ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Direction",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(68.dp)
                )
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier.width(220.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StitchPillOption(
                            selected = orientation == StitchOrientation.VERTICAL,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_unfold_more),
                            label = "Vertical",
                            onClick = { onOrientationChange(StitchOrientation.VERTICAL) },
                            modifier = Modifier.weight(1f)
                        )
                        StitchPillOption(
                            selected = orientation == StitchOrientation.HORIZONTAL,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                            label = "Horizontal",
                            onClick = { onOrientationChange(StitchOrientation.HORIZONTAL) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Row 2: Scale Mode (Same container width & pill size as Direction) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Scale",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(68.dp)
                )
                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 0.dp,
                    modifier = Modifier.width(220.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StitchPillOption(
                            selected = scaleMode == StitchScaleMode.FILL,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_crop),
                            label = "Fill",
                            onClick = { onScaleModeChange(StitchScaleMode.FILL) },
                            modifier = Modifier.weight(1f)
                        )
                        StitchPillOption(
                            selected = scaleMode == StitchScaleMode.FIT,
                            icon = ImageVector.vectorResource(R.drawable.ic_ms_aspect_ratio),
                            label = "Fit",
                            onClick = { onScaleModeChange(StitchScaleMode.FIT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Row 3: Alignment (Aligned with Direction & Scale, Vertical Only) ──
            AnimatedVisibility(
                visible = orientation == StitchOrientation.VERTICAL,
                enter = fadeIn(MotionTokens.gentleSpring()) + expandVertically(MotionTokens.gentleSpring()),
                exit = fadeOut(MotionTokens.gentleSpring()) + shrinkVertically(MotionTokens.gentleSpring())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Align",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(68.dp)
                    )
                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 0.dp,
                        modifier = Modifier.width(220.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StitchIconPillOption(
                                selected = alignment == StitchAlignment.START,
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_format_align_left),
                                contentDescription = "Align Left",
                                onClick = { onAlignmentChange(StitchAlignment.START) },
                                modifier = Modifier.weight(1f)
                            )
                            StitchIconPillOption(
                                selected = alignment == StitchAlignment.CENTER,
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_format_align_center),
                                contentDescription = "Align Center",
                                onClick = { onAlignmentChange(StitchAlignment.CENTER) },
                                modifier = Modifier.weight(1f)
                            )
                            StitchIconPillOption(
                                selected = alignment == StitchAlignment.END,
                                icon = ImageVector.vectorResource(R.drawable.ic_ms_format_align_right),
                                contentDescription = "Align Right",
                                onClick = { onAlignmentChange(StitchAlignment.END) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Row 4: Gap spacing slider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Gap",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(68.dp)
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
                    shadowElevation = 0.dp,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${spacingDp.roundToInt()} dp",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Row 5: Background Color Palette ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(68.dp)
                )
                val presetColors = listOf(
                    Color.Black,
                    Color.White,
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.errorContainer
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(presetColors) { _, color ->
                        val isSelected = bgColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .pressScale(0.94f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBgColorChange(color)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val isLight = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.5f
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                    contentDescription = "Selected",
                                    tint = if (isLight) Color.Black else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        shadowElevation = 0.dp,
        modifier = modifier
            .height(32.dp)
            .pressScale(0.96f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                softWrap = false
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
        shadowElevation = 0.dp,
        modifier = modifier
            .height(32.dp)
            .pressScale(0.96f)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp)
            )
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
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Throwable) {
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

    var (canvasWidth, canvasHeight) = if (orientation == StitchOrientation.VERTICAL) {
        val primaryWidth = minOf(boundsList.maxOf { it.first }, maxDimension)
        val scaledHeights = boundsList.map { (w, h) -> (primaryWidth.toFloat() / w * h).roundToInt() }
        val totalH = scaledHeights.sum() + totalSpacing.roundToInt()
        val finalH = minOf(totalH, maxDimension * 2)
        Pair(primaryWidth, finalH)
    } else {
        val primaryHeight = minOf(boundsList.maxOf { it.second }, maxDimension)
        val scaledWidths = boundsList.map { (w, h) -> (primaryHeight.toFloat() / h * w).roundToInt() }
        val totalW = scaledWidths.sum() + totalSpacing.roundToInt()
        val finalW = minOf(totalW, maxDimension * 2)
        Pair(finalW, primaryHeight)
    }

    // Safety guard: limit total pixels to 16M (64MB RAM at ARGB_8888) to prevent OOM
    val maxPixels = 16_777_216L
    val currentPixels = canvasWidth.toLong() * canvasHeight.toLong()
    if (currentPixels > maxPixels) {
        val scaleDown = kotlin.math.sqrt(maxPixels.toDouble() / currentPixels.toDouble()).toFloat()
        canvasWidth = (canvasWidth * scaleDown).roundToInt().coerceAtLeast(1)
        canvasHeight = (canvasHeight * scaleDown).roundToInt().coerceAtLeast(1)
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
