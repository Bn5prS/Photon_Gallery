package com.inferno.gallery.ui.editor

import androidx.compose.ui.res.stringResource

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import java.io.InputStream
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.components.EditorBottomTabs
import com.inferno.gallery.ui.editor.components.EditorCornerBrackets
import com.inferno.gallery.ui.editor.components.EditorCornerBracketsMath
import com.inferno.gallery.ui.editor.components.EditorOverflowSheet
import com.inferno.gallery.ui.editor.components.EditorTopBar
import com.inferno.gallery.ui.editor.crop.CropGeometryHelper
import com.inferno.gallery.ui.editor.engine.ImageExportWorker
import com.inferno.gallery.ui.editor.engine.ImageProcessingEngine
import com.inferno.gallery.ui.editor.markup.MarkupCanvasOverlay
import com.inferno.gallery.ui.editor.markup.MarkupPalette
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.markup.MarkupText
import com.inferno.gallery.ui.editor.markup.MarkupTextEditorDialog
import com.inferno.gallery.ui.editor.markup.MarkupTool
import com.inferno.gallery.ui.editor.markup.RedactMode
import com.inferno.gallery.ui.editor.model.AdjustTool
import com.inferno.gallery.ui.editor.model.CropState
import com.inferno.gallery.ui.editor.model.EditorHistoryState
import com.inferno.gallery.ui.editor.model.EditorTab
import com.inferno.gallery.ui.editor.model.FilterPreset
import com.inferno.gallery.ui.editor.model.TuneValues
import com.inferno.gallery.ui.editor.studios.AdjustStudio
import com.inferno.gallery.ui.editor.studios.AutoSuggestionType
import com.inferno.gallery.ui.editor.studios.AutoSuggestionsEngine
import com.inferno.gallery.ui.editor.studios.AutoStudio
import com.inferno.gallery.ui.editor.studios.CropStudio
import com.inferno.gallery.ui.editor.studios.CropSubMode
import com.inferno.gallery.ui.editor.studios.FiltersStudio
import com.inferno.gallery.ui.editor.studios.MarkupStudio
import com.inferno.gallery.ui.editor.studios.MoreStudio
import com.inferno.gallery.ui.editor.studios.ToolSearchSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Immutable snapshot container of all editing parameters for history stack undo/redo operations.
 */
data class EditorSnapshot(
    val cropState: CropState = CropState(),
    val tuneValues: TuneValues = TuneValues(),
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val filterStrength: Float = 100f,
    val markupState: MarkupState = MarkupState()
) {
    val isModified: Boolean
        get() = cropState.isModified ||
                !tuneValues.isDefault() ||
                filterPreset != FilterPreset.ORIGINAL ||
                markupState.strokes.isNotEmpty() ||
                markupState.texts.isNotEmpty()
}

/**
 * Flagship photo editor screen providing comprehensive editing workflow,
 * studios, animations, and lossless export engine.
 *
 * @param imageUri MediaStore content URI of the photo to edit.
 * @param onBack Invoked when user exits the editor.
 * @param onSaved Invoked with the newly created copy URI when export succeeds.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GooglePhotosEditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── 1. Bitmap Loading State ──────────────────────────────────────────
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUri) {
        isLoadingBitmap = true
        loadError = null
        originalBitmap = withContext(Dispatchers.IO) {
            try {
                fun openStream(): InputStream? {
                    return try {
                        context.contentResolver.openInputStream(imageUri)
                    } catch (e: Exception) {
                        null
                    } ?: try {
                        val path = imageUri.path ?: imageUri.toString()
                        val file = java.io.File(path)
                        if (file.exists() && file.canRead()) file.inputStream() else null
                    } catch (e: Exception) {
                        null
                    }
                }

                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                openStream()?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }

                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                val targetMax = 2560
                if (maxDim > 0) {
                    while (maxDim / (sampleSize * 2) >= targetMax) {
                        sampleSize *= 2
                    }
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBmp = openStream()?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return@withContext null

                val exifOrientation = try {
                    openStream()?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                } catch (e: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }

                applyExifOrientation(rawBmp, exifOrientation)
            } catch (e: Exception) {
                android.util.Log.e("GooglePhotosEditor", "Failed to load bitmap from $imageUri", e)
                null
            }
        }
        isLoadingBitmap = false
        if (originalBitmap == null) {
            loadError = "Unable to load image"
        }
    }

    val imageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }

    // ── 2. History & Editor State Management ──────────────────────────────
    var historyState by remember {
        mutableStateOf(EditorHistoryState(current = EditorSnapshot()))
    }
    val currentSnapshot = historyState.current

    val commitSnapshot: (EditorSnapshot) -> Unit = { newSnapshot ->
        if (newSnapshot != historyState.current) {
            historyState = historyState.push(newSnapshot)
        }
    }

    val updateLiveSnapshot: (EditorSnapshot) -> Unit = { newSnapshot ->
        historyState = historyState.copy(current = newSnapshot)
    }

    // ── 3. Navigation & Studio Selection ─────────────────────────────────
    var selectedTab by rememberSaveable { mutableStateOf(EditorTab.AUTO) }
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var savingMessage by rememberSaveable { mutableStateOf("Saving changes\u2026") }
    var isComparingOriginal by remember { mutableStateOf(false) }

    // Studio-specific active sub-modes
    var activeAdjustTool by rememberSaveable { mutableStateOf<AdjustTool?>(null) }
    var cropSubMode by rememberSaveable { mutableStateOf(CropSubMode.CAROUSEL) }
    var showAspectPicker by rememberSaveable { mutableStateOf(false) }
    var isCropDragging by remember { mutableStateOf(false) }
    var isDraggingCropHandle by remember { mutableStateOf(false) }
    var dragStartCropSnapshot by remember { mutableStateOf<EditorSnapshot?>(null) }

    // Markup studio settings
    var activeMarkupTool by rememberSaveable { mutableStateOf<MarkupTool?>(null) }
    var markupColor by remember { mutableStateOf(MarkupPalette.COLORS[0]) }
    var penStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_PEN_WIDTH) }
    var highlighterStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_HIGHLIGHTER_WIDTH) }
    var redactStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_REDACT_WIDTH) }
    var redactMode by remember { mutableStateOf(RedactMode.MOSAIC) }
    var selectedMarkupTextId by remember { mutableStateOf<Long?>(null) }
    var editingTextItem by remember { mutableStateOf<MarkupText?>(null) }
    var showTextDialog by rememberSaveable { mutableStateOf(false) }

    // Markup pinch-to-zoom & pan state for pinpoint drawing accuracy
    var markupZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var markupPanX by rememberSaveable { mutableFloatStateOf(0f) }
    var markupPanY by rememberSaveable { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != EditorTab.MARKUP) {
            markupZoom = 1f
            markupPanX = 0f
            markupPanY = 0f
        }
    }

    val hasModifications = historyState.canUndo || currentSnapshot.isModified

    val handleSave: (Boolean) -> Unit = { overwrite ->
        if (!isSaving) {
            isSaving = true
            savingMessage = if (overwrite) "Saving changes\u2026" else "Saving copy\u2026"
            scope.launch {
                val result = ImageExportWorker.exportImage(
                    context = context,
                    sourceUri = imageUri,
                    cropState = currentSnapshot.cropState,
                    tuneValues = currentSnapshot.tuneValues,
                    filterPreset = currentSnapshot.filterPreset,
                    filterStrength = currentSnapshot.filterStrength,
                    markupState = currentSnapshot.markupState,
                    overwrite = overwrite
                )
                isSaving = false
                result.onSuccess { savedUri ->
                    Toast.makeText(
                        context,
                        if (overwrite) "Saved changes to photo" else "Saved copy to photos",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSaved(savedUri)
                    onBack()
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        message = error.localizedMessage ?: "Failed to save photo"
                    )
                }
            }
        }
    }

    // ── 4. System Back Handling ──────────────────────────────────────────
    BackHandler(enabled = true) {
        when {
            isSearchOpen -> isSearchOpen = false
            cropSubMode != CropSubMode.CAROUSEL -> cropSubMode = CropSubMode.CAROUSEL
            activeAdjustTool != null -> activeAdjustTool = null
            selectedTab == EditorTab.MARKUP && markupZoom > 1.05f -> {
                markupZoom = 1f
                markupPanX = 0f
                markupPanY = 0f
            }
            hasModifications -> showDiscardDialog = true
            else -> onBack()
        }
    }

    // ── 5. ColorMatrix & Shader Computation ──────────────────────────────
    val filterMatrix = remember(currentSnapshot.filterPreset, currentSnapshot.filterStrength) {
        if (currentSnapshot.filterPreset != FilterPreset.ORIGINAL && currentSnapshot.filterStrength > 0f) {
            currentSnapshot.filterPreset.interpolate(currentSnapshot.filterStrength)
        } else null
    }

    val combinedMatrix = remember(currentSnapshot.tuneValues, filterMatrix) {
        ImageProcessingEngine.buildCombinedMatrix(
            tune = currentSnapshot.tuneValues,
            filterMatrix = filterMatrix
        )
    }

    val activeColorFilter = remember(isComparingOriginal, combinedMatrix) {
        if (isComparingOriginal) null else ColorFilter.colorMatrix(combinedMatrix)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorTopBar(
                onCancel = {
                    if (hasModifications) {
                        showDiscardDialog = true
                    } else {
                        onBack()
                    }
                },
                onUndo = {
                    if (historyState.canUndo) {
                        historyState = historyState.undo()
                    }
                },
                onRedo = {
                    if (historyState.canRedo) {
                        historyState = historyState.redo()
                    }
                },
                canUndo = historyState.canUndo,
                canRedo = historyState.canRedo,
                hasModifications = hasModifications,
                onSave = { handleSave(true) },
                onSaveAsCopy = { handleSave(false) },
                onMoreOptions = { showOverflowMenu = true }
            )
        },
        bottomBar = {
            EditorBottomTabs(
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    selectedTab = tab
                    // Reset sub-mode tools when switching tabs
                    if (tab != EditorTab.ADJUST) activeAdjustTool = null
                    if (tab != EditorTab.CROP) cropSubMode = CropSubMode.CAROUSEL
                    if (tab != EditorTab.MARKUP) activeMarkupTool = null
                },
                onOpenSearch = { isSearchOpen = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── CENTER CANVAS VIEWPORT ───────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.TopStart
            ) {
                val bmp = originalBitmap
                val imgBmp = imageBitmap

                if (bmp != null && imgBmp != null) {
                    val rawAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val effectiveAspect = if (isComparingOriginal) {
                        rawAspect
                    } else {
                        CropGeometryHelper.effectiveAspectRatio(rawAspect, currentSnapshot.cropState.rotation)
                    }

                    val maxW = constraints.maxWidth.toFloat()
                    val maxH = constraints.maxHeight.toFloat()
                    val containerAspect = if (maxH > 0f) maxW / maxH else 1f

                    val density = LocalDensity.current

                    val canTouchImageToCompare = selectedTab != EditorTab.CROP &&
                            !isCropDragging &&
                            (selectedTab != EditorTab.MARKUP || activeMarkupTool == null)
                    val currentCanTouchToCompare by rememberUpdatedState(canTouchImageToCompare)

                    // ── CROP VIEWPORT GEOMETRY (CENTERED VIEWPORT MODEL) ──────
                    val isCropMode = selectedTab == EditorTab.CROP && !isComparingOriginal

                    val targetGeo = remember(maxW, maxH, effectiveAspect, currentSnapshot.cropState.cropRect) {
                        EditorCornerBracketsMath.calculateViewportGeometry(
                            viewWidth = maxW,
                            viewHeight = maxH,
                            photoAspect = effectiveAspect,
                            cropRect = currentSnapshot.cropState.cropRect
                        )
                    }

                    // Release-to-center transition animation
                    val animProgress = remember { androidx.compose.animation.core.Animatable(1f) }
                    var frozenScale by remember { mutableFloatStateOf(1f) }
                    var frozenTransX by remember { mutableFloatStateOf(0f) }
                    var frozenTransY by remember { mutableFloatStateOf(0f) }

                    val targetScale = if (isCropMode) targetGeo.scale else 1f
                    val targetTransX = if (isCropMode) targetGeo.translationX else 0f
                    val targetTransY = if (isCropMode) targetGeo.translationY else 0f

                    val progress = animProgress.value
                    val liveScale = when {
                        isDraggingCropHandle -> frozenScale
                        progress < 1f -> frozenScale + (targetScale - frozenScale) * progress
                        else -> targetScale
                    }
                    val liveTransX = when {
                        isDraggingCropHandle -> frozenTransX
                        progress < 1f -> frozenTransX + (targetTransX - frozenTransX) * progress
                        else -> targetTransX
                    }
                    val liveTransY = when {
                        isDraggingCropHandle -> frozenTransY
                        progress < 1f -> frozenTransY + (targetTransY - frozenTransY) * progress
                        else -> targetTransY
                    }

                    val centerX = maxW / 2f
                    val centerY = maxH / 2f
                    val baseW = targetGeo.baseW
                    val baseH = targetGeo.baseH
                    val baseLeft = targetGeo.baseLeft
                    val baseTop = targetGeo.baseTop

                    val photoScreenW = baseW * liveScale
                    val photoScreenH = baseH * liveScale
                    val photoScreenLeft = centerX + liveTransX - photoScreenW / 2f
                    val photoScreenTop = centerY + liveTransY - photoScreenH / 2f

                    // Screen-space Crop Box
                    val cRect = currentSnapshot.cropState.cropRect
                    val currentCropBoxOnScreen = if (isCropMode) {
                        val boxL = photoScreenLeft + cRect.left * photoScreenW
                        val boxT = photoScreenTop + cRect.top * photoScreenH
                        val boxR = photoScreenLeft + cRect.right * photoScreenW
                        val boxB = photoScreenTop + cRect.bottom * photoScreenH
                        Rect(boxL, boxT, boxR, boxB)
                    } else {
                        Rect(photoScreenLeft, photoScreenTop, photoScreenLeft + photoScreenW, photoScreenTop + photoScreenH)
                    }

                    val isQuarterRotated = !isComparingOriginal && (currentSnapshot.cropState.rotation % 180 != 0)
                    val unrotatedBaseW = if (isQuarterRotated) baseH else baseW
                    val unrotatedBaseH = if (isQuarterRotated) baseW else baseH
                    val boxWDp = with(density) { unrotatedBaseW.toDp() }
                    val boxHDp = with(density) { unrotatedBaseH.toDp() }

                    // Image container with live geometry transforms applied via graphicsLayer
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = boxWDp, height = boxHDp)
                            .graphicsLayer {
                                val isMarkupActive = selectedTab == EditorTab.MARKUP
                                translationX = liveTransX + (if (isMarkupActive) markupPanX else 0f)
                                translationY = liveTransY + (if (isMarkupActive) markupPanY else 0f)
                                scaleX = liveScale * (if (isMarkupActive) markupZoom else 1f)
                                scaleY = liveScale * (if (isMarkupActive) markupZoom else 1f)
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                if (!isComparingOriginal) {
                                    val crop = currentSnapshot.cropState
                                    rotationZ = crop.rotation.toFloat() + crop.straightenAngle
                                    scaleX *= if (crop.flipH) -1f else 1f
                                    scaleY *= if (crop.flipV) -1f else 1f
                                    rotationX = crop.verticalTilt
                                    rotationY = -crop.horizontalTilt
                                    cameraDistance = 16f * density.density
                                }
                            }
                            .then(
                                if (canTouchImageToCompare) {
                                    Modifier.pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            if (!currentCanTouchToCompare) return@awaitEachGesture

                                            val timeoutMs = (viewConfiguration.longPressTimeoutMillis).coerceIn(300L, 500L)
                                            val upOrCancel = withTimeoutOrNull(timeoutMs) {
                                                waitForUpOrCancellation()
                                            }

                                            if (upOrCancel == null && currentCanTouchToCompare) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                isComparingOriginal = true
                                                try {
                                                    waitForUpOrCancellation()
                                                } finally {
                                                    isComparingOriginal = false
                                                }
                                            }
                                        }
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Live Rendered Image
                        Image(
                            bitmap = imgBmp,
                            contentDescription = "Edited photo",
                            colorFilter = activeColorFilter,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 2. Real-time Vignette Radial Gradient Overlay
                        if (!isComparingOriginal && currentSnapshot.tuneValues.vignette > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val radius = max(cx, cy) * 1.2f
                                val alpha = ((currentSnapshot.tuneValues.vignette / 100f) * 0.78f).coerceIn(0f, 1f)
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = alpha)),
                                        center = Offset(cx, cy),
                                        radius = radius
                                    ),
                                    size = size
                                )
                            }
                        }

                        // 3. Markup Overlay (interactive in Markup tab, persistent display elsewhere)
                        if (!isComparingOriginal && (selectedTab == EditorTab.MARKUP || currentSnapshot.markupState.strokes.isNotEmpty() || currentSnapshot.markupState.texts.isNotEmpty())) {
                            val markupActive = selectedTab == EditorTab.MARKUP
                            MarkupCanvasOverlay(
                                markupState = currentSnapshot.markupState,
                                activeTool = activeMarkupTool,
                                currentColor = markupColor,
                                currentStrokeWidth = when (activeMarkupTool) {
                                    MarkupTool.PEN -> penStrokeWidth
                                    MarkupTool.HIGHLIGHTER -> highlighterStrokeWidth
                                    MarkupTool.REDACT -> redactStrokeWidth
                                    else -> penStrokeWidth
                                },
                                redactMode = redactMode,
                                selectedTextId = selectedMarkupTextId,
                                onSelectText = { selectedMarkupTextId = it },
                                onStateChanged = { updatedMarkup ->
                                    commitSnapshot(currentSnapshot.copy(markupState = updatedMarkup))
                                },
                                onEditTextRequest = { textItem ->
                                    editingTextItem = textItem
                                    showTextDialog = true
                                },
                                onZoomPan = { zoomChange, panChange ->
                                    val newZoom = (markupZoom * zoomChange).coerceIn(1f, 5f)
                                    markupZoom = newZoom
                                    if (newZoom > 1f) {
                                        val maxPanX = (maxW * (newZoom - 1f)) / 2f
                                        val maxPanY = (maxH * (newZoom - 1f)) / 2f
                                        markupPanX = (markupPanX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                        markupPanY = (markupPanY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                    } else {
                                        markupPanX = 0f
                                        markupPanY = 0f
                                    }
                                },
                                isInteractive = markupActive,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 4. Corner Brackets & Grid (active in Crop tab, placed in viewport space)
                    if (!isComparingOriginal && selectedTab == EditorTab.CROP) {
                        EditorCornerBrackets(
                            cropRect = currentSnapshot.cropState.cropRect,
                            onCropRectChange = { newRect ->
                                val updatedCrop = currentSnapshot.cropState.copy(cropRect = newRect)
                                updateLiveSnapshot(currentSnapshot.copy(cropState = updatedCrop))
                            },
                            aspectRatio = currentSnapshot.cropState.aspectOption.resolveTargetRatio(effectiveAspect),
                            isDragging = isCropDragging,
                            onDragHandleStateChange = { dragging ->
                                if (dragging) {
                                    frozenScale = liveScale
                                    frozenTransX = liveTransX
                                    frozenTransY = liveTransY
                                    isDraggingCropHandle = true
                                    isCropDragging = true
                                    dragStartCropSnapshot = currentSnapshot
                                } else {
                                    isDraggingCropHandle = false
                                    isCropDragging = false
                                    scope.launch {
                                        animProgress.snapTo(0f)
                                        animProgress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    dragStartCropSnapshot?.let { startSnap ->
                                        if (startSnap != currentSnapshot) {
                                            historyState = EditorHistoryState(
                                                past = historyState.past + startSnap,
                                                current = currentSnapshot,
                                                future = emptyList()
                                            )
                                        }
                                    }
                                    dragStartCropSnapshot = null
                                }
                            },
                            cropBoxOnScreen = currentCropBoxOnScreen,
                            photoWidth = photoScreenW,
                            photoHeight = photoScreenH,
                            photoLeft = photoScreenLeft,
                            photoTop = photoScreenTop,
                            onPhotoPanZoom = { newRect ->
                                val updatedCrop = currentSnapshot.cropState.copy(cropRect = newRect)
                                updateLiveSnapshot(currentSnapshot.copy(cropState = updatedCrop))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!isComparingOriginal && currentSnapshot.cropState.cropRect != Rect(0f, 0f, 1f, 1f)) {
                        // Subtle non-interactive boundary crop scrim when previewing outside Crop tab
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rect = currentSnapshot.cropState.cropRect
                            val w = size.width
                            val h = size.height
                            val l = photoScreenLeft + rect.left * photoScreenW
                            val t = photoScreenTop + rect.top * photoScreenH
                            val r = photoScreenLeft + rect.right * photoScreenW
                            val b = photoScreenTop + rect.bottom * photoScreenH
                            val scrim = Color.Black.copy(alpha = 0.65f)
                            if (t > 0f) drawRect(scrim, Offset.Zero, Size(w, t))
                            if (b < h) drawRect(scrim, Offset(0f, b), Size(w, h - b))
                            if (l > 0f) drawRect(scrim, Offset(0f, t), Size(l, b - t))
                            if (r < w) drawRect(scrim, Offset(r, t), Size(w - r, b - t))
                        }
                    }

                    // 5. Hold-to-compare "Original" pill badge
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isComparingOriginal,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(120)),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.72f),
                            tonalElevation = 6.dp
                        ) {
                            Text(
                                text = stringResource(R.string.editor_action_original),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // 6. Floating View Original / Compare button in top-right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    shape = CircleShape
                                ),
                            shape = CircleShape,
                            color = if (isComparingOriginal) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
                            },
                            tonalElevation = 4.dp,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isComparingOriginal = true
                                            waitForUpOrCancellation()
                                            isComparingOriginal = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_compare),
                                    contentDescription = stringResource(R.string.editor_action_original),
                                    tint = if (isComparingOriginal) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 7. Floating Markup Zoom Reset Pill (top-start corner)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedTab == EditorTab.MARKUP && markupZoom > 1.05f,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 12.dp, start = 12.dp)
                    ) {
                        Surface(
                            onClick = {
                                markupZoom = 1f
                                markupPanX = 0f
                                markupPanY = 0f
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                            tonalElevation = 4.dp,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_refresh),
                                    contentDescription = "Reset zoom",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${(markupZoom * 100).toInt()}% \u2022 Reset",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (isLoadingBitmap) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = loadError ?: "Failed to open image",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── BOTTOM STUDIOS CONTAINER ──────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                when (selectedTab) {
                    EditorTab.AUTO -> {
                        AutoStudio(
                            previewBitmap = originalBitmap,
                            onApplyTuneValues = { tune ->
                                commitSnapshot(currentSnapshot.copy(tuneValues = tune))
                            }
                        )
                    }

                    EditorTab.CROP -> {
                        val rawAspect = originalBitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
                        CropStudio(
                            cropState = currentSnapshot.cropState,
                            onCropStateChange = { newCrop ->
                                commitSnapshot(currentSnapshot.copy(cropState = newCrop))
                            },
                            imageAspect = rawAspect,
                            showAspectPicker = showAspectPicker,
                            onDismissAspectPicker = { showAspectPicker = false },
                            initialSubMode = cropSubMode,
                            onStraightenDone = { cropSubMode = CropSubMode.CAROUSEL },
                            onReset = {
                                commitSnapshot(currentSnapshot.copy(cropState = CropState()))
                            }
                        )
                    }

                    EditorTab.ADJUST -> {
                        AdjustStudio(
                            tuneValues = currentSnapshot.tuneValues,
                            onTuneValuesChange = { newTune ->
                                commitSnapshot(currentSnapshot.copy(tuneValues = newTune))
                            },
                            initialTool = activeAdjustTool,
                            onDone = { activeAdjustTool = null }
                        )
                    }

                    EditorTab.MARKUP -> {
                        MarkupStudio(
                            markupState = currentSnapshot.markupState,
                            onMarkupStateChange = { newMarkup ->
                                commitSnapshot(currentSnapshot.copy(markupState = newMarkup))
                            },
                            activeTool = activeMarkupTool,
                            onActiveToolChange = { activeMarkupTool = it },
                            selectedColor = markupColor,
                            onColorChange = { markupColor = it },
                            penStrokeWidth = penStrokeWidth,
                            onPenStrokeWidthChange = { penStrokeWidth = it },
                            highlighterStrokeWidth = highlighterStrokeWidth,
                            onHighlighterStrokeWidthChange = { highlighterStrokeWidth = it },
                            redactStrokeWidth = redactStrokeWidth,
                            onRedactStrokeWidthChange = { redactStrokeWidth = it },
                            redactMode = redactMode,
                            onRedactModeChange = { redactMode = it },
                            selectedTextId = selectedMarkupTextId,
                            onSelectTextId = { selectedMarkupTextId = it },
                            onEditText = { textToEdit ->
                                editingTextItem = textToEdit
                                showTextDialog = true
                            },
                            onOpenTextDialog = {
                                editingTextItem = null
                                showTextDialog = true
                            },
                            onDone = { activeMarkupTool = null }
                        )
                    }

                    EditorTab.FILTERS -> {
                        FiltersStudio(
                            previewBitmap = originalBitmap,
                            selectedFilter = currentSnapshot.filterPreset,
                            filterStrength = currentSnapshot.filterStrength,
                            onSelectFilter = { preset ->
                                commitSnapshot(currentSnapshot.copy(filterPreset = preset))
                            },
                            onFilterStrengthChange = { strength ->
                                commitSnapshot(currentSnapshot.copy(filterStrength = strength))
                            },
                            onDone = {}
                        )
                    }

                    EditorTab.MORE -> {
                        MoreStudio(imageUri = imageUri)
                    }
                }
            }
        }
    }

    // ── 6. Tool Search Sheet Modal ───────────────────────────────────────
    if (isSearchOpen) {
        ToolSearchSheet(
            onSelectTool = { tab, toolId ->
                isSearchOpen = false
                selectedTab = tab
                when (tab) {
                    EditorTab.ADJUST -> {
                        val tool = AdjustTool.entries.find {
                            it.name.equals(toolId, ignoreCase = true) ||
                                    it.name.replace("_", "").equals(toolId?.replace("_", ""), ignoreCase = true)
                        } ?: when (toolId?.lowercase()) {
                            "brightness" -> AdjustTool.BRIGHTNESS
                            "contrast" -> AdjustTool.CONTRAST
                            "tone" -> AdjustTool.TONE
                            "white_point", "white point" -> AdjustTool.WHITE_POINT
                            "highlights" -> AdjustTool.HIGHLIGHTS
                            "shadows" -> AdjustTool.SHADOWS
                            "black_point", "black point" -> AdjustTool.BLACK_POINT
                            "saturation" -> AdjustTool.SATURATION
                            "warmth" -> AdjustTool.WARMTH
                            "tint" -> AdjustTool.TINT
                            "skin_tone", "skin tone" -> AdjustTool.SKIN_TONE
                            "blue_tone", "blue tone" -> AdjustTool.BLUE_TONE
                            "vignette" -> AdjustTool.VIGNETTE
                            else -> null
                        }
                        activeAdjustTool = tool
                    }

                    EditorTab.CROP -> {
                        when (toolId?.lowercase()) {
                            "straighten" -> cropSubMode = CropSubMode.FRAMING
                            "perspective" -> cropSubMode = CropSubMode.PERSPECTIVE
                            "crop_aspect", "aspect" -> showAspectPicker = true
                            "rotate" -> {
                                val rawAspect = originalBitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
                                val newRotation = CropGeometryHelper.rotateCounterClockwise(currentSnapshot.cropState.rotation)
                                val newEffAspect = CropGeometryHelper.effectiveAspectRatio(rawAspect, newRotation)
                                val old = currentSnapshot.cropState.cropRect
                                val newRect = if (currentSnapshot.cropState.aspectOption != com.inferno.gallery.ui.editor.model.AspectRatioOption.FREEFORM) {
                                    CropGeometryHelper.calculateInitialAspectRect(currentSnapshot.cropState.aspectOption, newEffAspect)
                                } else if (old != Rect(0f, 0f, 1f, 1f)) {
                                    Rect(
                                        left = old.top,
                                        top = (1f - old.right).coerceIn(0f, 1f),
                                        right = old.bottom,
                                        bottom = (1f - old.left).coerceIn(0f, 1f)
                                    )
                                } else {
                                    old
                                }
                                commitSnapshot(
                                    currentSnapshot.copy(
                                        cropState = currentSnapshot.cropState.copy(
                                            rotation = newRotation,
                                            cropRect = newRect
                                        )
                                    )
                                )
                            }
                            "reset_crop", "reset" -> {
                                commitSnapshot(currentSnapshot.copy(cropState = CropState()))
                            }
                            else -> cropSubMode = CropSubMode.CAROUSEL
                        }
                    }

                    EditorTab.MARKUP -> {
                        when (toolId?.lowercase()) {
                            "markup_pen", "pen" -> activeMarkupTool = MarkupTool.PEN
                            "markup_highlighter", "highlighter" -> activeMarkupTool = MarkupTool.HIGHLIGHTER
                            "markup_redact", "redact" -> activeMarkupTool = MarkupTool.REDACT
                            "markup_text", "text" -> {
                                activeMarkupTool = MarkupTool.TEXT
                                showTextDialog = true
                            }
                            "markup_arrow", "arrow" -> activeMarkupTool = MarkupTool.ARROW
                            "markup_eraser", "eraser" -> activeMarkupTool = MarkupTool.ERASER
                            else -> activeMarkupTool = MarkupTool.PEN
                        }
                    }

                    EditorTab.FILTERS -> {
                        val preset = FilterPreset.entries.find {
                            it.name.equals(toolId, ignoreCase = true) || it.label.equals(toolId, ignoreCase = true)
                        }
                        if (preset != null) {
                            commitSnapshot(currentSnapshot.copy(filterPreset = preset, filterStrength = 100f))
                        }
                    }

                    EditorTab.AUTO -> {
                        val suggestions = AutoSuggestionsEngine.generateSuggestions(originalBitmap)
                        val tuned = when (toolId?.lowercase()) {
                            "auto_enhance", "enhance" -> suggestions.find { it.type == AutoSuggestionType.ENHANCE }?.tuneValues
                            "auto_dynamic", "dynamic" -> suggestions.find { it.type == AutoSuggestionType.DYNAMIC }?.tuneValues
                            "auto_warm", "warm" -> suggestions.find { it.type == AutoSuggestionType.WARM }?.tuneValues
                            "auto_cool", "cool" -> suggestions.find { it.type == AutoSuggestionType.COOL }?.tuneValues
                            else -> null
                        }
                        if (tuned != null) {
                            commitSnapshot(currentSnapshot.copy(tuneValues = tuned))
                        }
                    }

                    EditorTab.MORE -> {
                        // Tab switch suffices
                    }
                }
            },
            onDismiss = { isSearchOpen = false }
        )
    }

    // ── 7. Overflow Bottom Sheet ─────────────────────────────────────────
    if (showOverflowMenu) {
        EditorOverflowSheet(
            onDismissRequest = { showOverflowMenu = false },
            canRevert = hasModifications,
            onSave = { handleSave(true) },
            onSaveAsCopy = { handleSave(false) },
            onRevertEdits = {
                commitSnapshot(EditorSnapshot())
                activeAdjustTool = null
                cropSubMode = CropSubMode.CAROUSEL
                activeMarkupTool = null
                Toast.makeText(context, "Reverted all edits", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ── 8. Markup Text Editor Dialog ─────────────────────────────────────
    if (showTextDialog) {
        MarkupTextEditorDialog(
            initialText = editingTextItem,
            onDismissRequest = { showTextDialog = false },
            onConfirm = { updatedOrNewText ->
                showTextDialog = false
                markupColor = updatedOrNewText.color
                val updatedMarkup = if (editingTextItem != null) {
                    currentSnapshot.markupState.withUpdatedText(updatedOrNewText)
                } else {
                    currentSnapshot.markupState.withAddedText(updatedOrNewText)
                }
                commitSnapshot(currentSnapshot.copy(markupState = updatedMarkup))
                selectedMarkupTextId = updatedOrNewText.id
            },
            onDelete = { idToDelete ->
                showTextDialog = false
                val updatedMarkup = currentSnapshot.markupState.withRemovedText(idToDelete)
                commitSnapshot(currentSnapshot.copy(markupState = updatedMarkup))
                selectedMarkupTextId = null
            }
        )
    }

    // ── 9. Discard Changes Confirmation Dialog ───────────────────────────
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(text = stringResource(R.string.dialog_discard_edits_title)) },
            text = { Text(text = stringResource(R.string.dialog_discard_edits_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text(text = stringResource(R.string.action_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(text = stringResource(R.string.action_keep_editing))
                }
            }
        )
    }

    // ── 10. Saving Copy Modal Progress Overlay ───────────────────────────
    if (isSaving) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss while saving */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = savingMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Normalizes bitmap orientation according to EXIF rotation and flip tags.
 */
private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = android.graphics.Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.preRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.preRotate(270f)
            matrix.preScale(-1f, 1f)
        }
        else -> return bitmap
    }
    val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (transformed != bitmap && !bitmap.isRecycled) {
        bitmap.recycle()
    }
    return transformed
}
