package com.inferno.gallery.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Environment
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.ShapeSmall
import com.inferno.gallery.ui.utils.thud
import com.inferno.gallery.ui.utils.tick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import com.inferno.gallery.ui.editor.crop.CropGeometryHelper
import com.inferno.gallery.ui.editor.crop.CropHandle
import com.inferno.gallery.ui.editor.markup.MarkupBottomDock
import com.inferno.gallery.ui.editor.markup.MarkupCanvasOverlay
import com.inferno.gallery.ui.editor.markup.MarkupPalette
import com.inferno.gallery.ui.editor.markup.MarkupRasterizer
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.markup.MarkupText
import com.inferno.gallery.ui.editor.markup.MarkupTextEditorDialog
import com.inferno.gallery.ui.editor.markup.MarkupTool
import com.inferno.gallery.ui.components.FloatingTipCard


// ── Data Models ──────────────────────────────────────────────────────────────

enum class EditorTool(val label: String, @androidx.annotation.DrawableRes val iconRes: Int) {
    SUGGESTIONS("Enhance", R.drawable.ic_ms_auto_fix_high),
    CROP("Crop", R.drawable.ic_ms_crop),
    ADJUST("Adjust", R.drawable.ic_ms_tune),
    FILTERS("Filters", R.drawable.ic_ms_palette),
    MARKUP("Markup", R.drawable.ic_ms_edit)
}

data class TuneValues(
    val brightness: Float = 0f,    // -100..100
    val contrast: Float = 0f,      // -100..100
    val highlights: Float = 0f,    // -100..100
    val shadows: Float = 0f,       // -100..100
    val whitePoint: Float = 0f,    // -100..100
    val blackPoint: Float = 0f,    // -100..100
    val saturation: Float = 0f,    // -100..100
    val warmth: Float = 0f,        // -100..100
    val tint: Float = 0f,          // -100..100 (green <-> magenta)
    val vignette: Float = 0f       // 0..100
)

data class CropState(
    val rotation: Int = 0,           // 0, 90, 180, 270
    val straightenAngle: Float = 0f, // -45f..+45f fine angle
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropRect: Rect? = null,      // Normalized 0..1
    val aspectLabel: String = "Free" // "Free", "Original", "1:1", "5:4", "4:3", "3:2", "16:9"
)

data class FilterPreset(
    val name: String,
    val matrix: ColorMatrix
)

data class EditorState(
    val tune: TuneValues = TuneValues(),
    val crop: CropState = CropState(),
    val filterIndex: Int = 0,
    val markup: MarkupState = MarkupState()
)

// ── Filter Presets ───────────────────────────────────────────────────────────

private fun buildFilterPresets(): List<FilterPreset> {
    val presets = mutableListOf<FilterPreset>()

    // Original
    presets.add(FilterPreset("Original", ColorMatrix()))

    // Vivid
    presets.add(FilterPreset("Vivid", ColorMatrix().apply {
        val s = ColorMatrix()
        s.setSaturation(1.5f)
        postConcat(s)
        val c = buildContrastMatrix(0.15f)
        postConcat(c)
    }))

    // Warm
    presets.add(FilterPreset("Warm", ColorMatrix(floatArrayOf(
        1.2f, 0f, 0f, 0f, 10f,
        0f, 1.05f, 0f, 0f, 5f,
        0f, 0f, 0.9f, 0f, -10f,
        0f, 0f, 0f, 1f, 0f
    ))))

    // Cool
    presets.add(FilterPreset("Cool", ColorMatrix(floatArrayOf(
        0.9f, 0f, 0f, 0f, -10f,
        0f, 1.0f, 0f, 0f, 0f,
        0f, 0f, 1.2f, 0f, 15f,
        0f, 0f, 0f, 1f, 0f
    ))))

    // B&W
    presets.add(FilterPreset("B&W", ColorMatrix().apply { setSaturation(0f) }))

    // Sepia
    presets.add(FilterPreset("Sepia", ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))))

    // Vintage
    presets.add(FilterPreset("Vintage", ColorMatrix(floatArrayOf(
        0.9f, 0.5f, 0.1f, 0f, 20f,
        0.3f, 0.8f, 0.1f, 0f, 10f,
        0.2f, 0.3f, 0.5f, 0f, 30f,
        0f, 0f, 0f, 1f, 0f
    ))))

    // Dramatic
    presets.add(FilterPreset("Dramatic", ColorMatrix().apply {
        setSaturation(0.6f)
        val c = buildContrastMatrix(0.3f)
        postConcat(c)
    }))

    // Fade
    presets.add(FilterPreset("Fade", ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, 30f,
        0f, 1f, 0f, 0f, 30f,
        0f, 0f, 1f, 0f, 30f,
        0f, 0f, 0f, 0.9f, 0f
    )).apply {
        val s = ColorMatrix()
        s.setSaturation(0.7f)
        postConcat(s)
    }))

    // Film
    presets.add(FilterPreset("Film", ColorMatrix(floatArrayOf(
        1.1f, 0.05f, 0.0f, 0f, 5f,
        0.0f, 1.05f, 0.05f, 0f, 0f,
        0.05f, 0.0f, 1.1f, 0f, 10f,
        0f, 0f, 0f, 1f, 0f
    )).apply {
        val c = buildContrastMatrix(0.1f)
        postConcat(c)
    }))

    return presets
}

private fun buildContrastMatrix(contrast: Float): ColorMatrix {
    val scale = 1f + contrast
    val offset = (-0.5f * scale + 0.5f) * 255f
    return ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, offset,
        0f, scale, 0f, 0f, offset,
        0f, 0f, scale, 0f, offset,
        0f, 0f, 0f, 1f, 0f
    ))
}

// ── Combined ColorMatrix builder ─────────────────────────────────────────────

private fun buildCombinedMatrix(tune: TuneValues, filterMatrix: ColorMatrix): ColorMatrix {
    val result = ColorMatrix()

    // 1. Brightness
    if (tune.brightness != 0f) {
        val b = tune.brightness * 1.5f
        result.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 2. Contrast
    if (tune.contrast != 0f) {
        val c = tune.contrast / 100f
        result.postConcat(buildContrastMatrix(c))
    }

    // 3. Saturation
    if (tune.saturation != 0f) {
        val s = ColorMatrix()
        s.setSaturation(1f + tune.saturation / 100f)
        result.postConcat(s)
    }

    // 4. Warmth (shift R/B)
    if (tune.warmth != 0f) {
        val w = tune.warmth * 0.8f
        result.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, w,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, -w,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 5. Tint (shift green vs magenta)
    if (tune.tint != 0f) {
        val t = tune.tint * 0.6f
        result.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, t,
            0f, 1f, 0f, 0f, -t,
            0f, 0f, 1f, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 6. Shadows (lift/darken dark tones)
    if (tune.shadows != 0f) {
        val sh = tune.shadows * 0.5f
        result.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, sh,
            0f, 1f, 0f, 0f, sh,
            0f, 0f, 1f, 0f, sh,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 7. Highlights
    if (tune.highlights != 0f) {
        val hl = tune.highlights * 0.3f
        val scale = 1f + hl / 255f
        result.postConcat(ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, 0f,
            0f, scale, 0f, 0f, 0f,
            0f, 0f, scale, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 8. White Point
    if (tune.whitePoint != 0f) {
        val wp = 1f + (tune.whitePoint / 200f)
        result.postConcat(ColorMatrix(floatArrayOf(
            wp, 0f, 0f, 0f, 0f,
            0f, wp, 0f, 0f, 0f,
            0f, 0f, wp, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 9. Black Point
    if (tune.blackPoint != 0f) {
        val bp = tune.blackPoint * 0.8f
        result.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bp,
            0f, 1f, 0f, 0f, bp,
            0f, 0f, 1f, 0f, bp,
            0f, 0f, 0f, 1f, 0f
        )))
    }

    // 10. Filter preset on top
    result.postConcat(filterMatrix)

    return result
}

// ── Main Editor Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // ── Load original bitmap ─────────────────────────────────────────────
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        originalBitmap = withContext(Dispatchers.IO) {
            try {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }

                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                val targetMax = 4096
                while (maxDim / (sampleSize * 2) >= targetMax) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
        }
    }

    val bmp = originalBitmap
    if (bmp == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Loading editor…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val imageAspect = bmp.width.toFloat() / bmp.height.toFloat()

    // ── Filter presets ───────────────────────────────────────────────────
    val filterPresets = remember { buildFilterPresets() }

    // ── Editor State + Undo/Redo Stacks ──────────────────────────────────
    var currentState by remember { mutableStateOf(EditorState()) }
    val undoStack = remember { mutableStateListOf<EditorState>() }
    val redoStack = remember { mutableStateListOf<EditorState>() }

    fun pushState(newState: EditorState) {
        undoStack.add(currentState)
        redoStack.clear()
        currentState = newState
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentState)
            currentState = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentState)
            currentState = next
        }
    }

    // ── Active Tool ──────────────────────────────────────────────────────
    var activeTool by remember { mutableStateOf(EditorTool.SUGGESTIONS) }
    var isComparingOriginal by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSaveMenu by remember { mutableStateOf(false) }

    // ── Markup Tool State ────────────────────────────────────────────────
    var activeMarkupTool by remember { mutableStateOf(MarkupTool.PEN) }
    var markupColor by remember { mutableStateOf(MarkupPalette.COLORS[0]) }
    var penStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_PEN_WIDTH) }
    var highlighterStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_HIGHLIGHTER_WIDTH) }
    var arrowStrokeWidth by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_ARROW_WIDTH) }
    var eraserRadius by remember { mutableFloatStateOf(MarkupPalette.DEFAULT_ERASER_RADIUS) }

    // Text Editor Dialog State
    var showTextDialog by remember { mutableStateOf(false) }
    var editingTextItem by remember { mutableStateOf<MarkupText?>(null) }
    var selectedMarkupTextId by remember { mutableStateOf<Long?>(null) }

    // Back handler
    BackHandler {
        onBack()
    }

    // ── Combined color matrix ────────────────────────────────────────────
    val combinedMatrix = remember(currentState.tune, currentState.filterIndex) {
        val filterMatrix = filterPresets.getOrNull(currentState.filterIndex)?.matrix ?: ColorMatrix()
        buildCombinedMatrix(currentState.tune, filterMatrix)
    }

    val combinedColorFilter = remember(combinedMatrix) {
        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(combinedMatrix.array)
        )
    }

    // ── Save function ────────────────────────────────────────────────────
    fun saveImage(overwrite: Boolean) {
        val b = originalBitmap ?: return
        isSaving = true
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val crop = currentState.crop
                var workBitmap = b

                // Apply markup onto high-res bitmap
                if (currentState.markup.strokes.isNotEmpty() || currentState.markup.texts.isNotEmpty()) {
                    val annotated = workBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val markupCanvas = Canvas(annotated)
                    MarkupRasterizer.rasterize(
                        canvas = markupCanvas,
                        bitmapWidth = annotated.width,
                        bitmapHeight = annotated.height,
                        markup = currentState.markup
                    )
                    workBitmap = annotated
                }

                // Apply rotation/flip/straighten
                val totalRotation = crop.rotation.toFloat() + crop.straightenAngle
                if (totalRotation != 0f || crop.flipH || crop.flipV) {
                    val matrix = android.graphics.Matrix()
                    if (crop.flipH) matrix.preScale(-1f, 1f)
                    if (crop.flipV) matrix.preScale(1f, -1f)
                    if (totalRotation != 0f) matrix.postRotate(totalRotation)
                    val rotated = Bitmap.createBitmap(workBitmap, 0, 0, workBitmap.width, workBitmap.height, matrix, true)
                    if (workBitmap != b) workBitmap.recycle()
                    workBitmap = rotated
                }

                // Apply crop rect
                val cropRect = crop.cropRect
                if (cropRect != null) {
                    val x = (cropRect.left * workBitmap.width).toInt().coerceIn(0, workBitmap.width - 1)
                    val y = (cropRect.top * workBitmap.height).toInt().coerceIn(0, workBitmap.height - 1)
                    val w = ((cropRect.right - cropRect.left) * workBitmap.width).toInt().coerceIn(1, workBitmap.width - x)
                    val h = ((cropRect.bottom - cropRect.top) * workBitmap.height).toInt().coerceIn(1, workBitmap.height - y)
                    val cropped = Bitmap.createBitmap(workBitmap, x, y, w, h)
                    if (workBitmap != b) workBitmap.recycle()
                    workBitmap = cropped
                }

                // Apply color matrix (tune + filter)
                val resultBitmap = Bitmap.createBitmap(workBitmap.width, workBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(combinedMatrix)
                }
                canvas.drawBitmap(workBitmap, 0f, 0f, paint)
                if (workBitmap != b) workBitmap.recycle()

                // Apply vignette
                if (currentState.tune.vignette > 0f) {
                    val cx = resultBitmap.width / 2f
                    val cy = resultBitmap.height / 2f
                    val radius = kotlin.math.max(cx, cy) * 1.2f
                    val alpha = (currentState.tune.vignette / 100f * 200).toInt().coerceIn(0, 200)
                    val vignettePaint = Paint().apply {
                        shader = RadialGradient(
                            cx, cy, radius,
                            intArrayOf(0x00000000, 0x00000000, android.graphics.Color.argb(alpha, 0, 0, 0)),
                            floatArrayOf(0f, 0.45f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, resultBitmap.width.toFloat(), resultBitmap.height.toFloat(), vignettePaint)
                }

                // Write to MediaStore
                withContext(Dispatchers.IO) {
                    if (overwrite) {
                        context.contentResolver.openOutputStream(imageUri, "w")?.use { out ->
                            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                    } else {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "Photon_Edit_${System.currentTimeMillis()}.jpg")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Photon Gallery")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val newUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (newUri != null) {
                            context.contentResolver.openOutputStream(newUri)?.use { out ->
                                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(newUri, values, null, null)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isSaving = false
                    view.thud()
                    Toast.makeText(context, if (overwrite) "Saved changes" else "Saved as copy", Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Image Preview Area ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                if (activeTool == EditorTool.CROP) {
                    CropPreview(
                        imageBitmap = imageBitmap,
                        imageAspect = imageAspect,
                        cropState = currentState.crop,
                        colorFilter = combinedColorFilter,
                        onCropRectChanged = { newRect ->
                            currentState = currentState.copy(crop = currentState.crop.copy(cropRect = newRect))
                        }
                    )
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxW = constraints.maxWidth.toFloat()
                        val maxH = constraints.maxHeight.toFloat()
                        val isRotated90 = currentState.crop.rotation % 180 != 0
                        val effectiveAspect = if (isRotated90) 1f / imageAspect else imageAspect

                        val (imgW, imgH) = if (effectiveAspect > maxW / maxH) {
                            maxW to (maxW / effectiveAspect)
                        } else {
                            (maxH * effectiveAspect) to maxH
                        }

                        val density = LocalDensity.current.density
                        val (drawW, drawH) = if (isRotated90) imgH to imgW else imgW to imgH

                        val totalRot = currentState.crop.rotation.toFloat() + currentState.crop.straightenAngle

                        val canTouchToCompare = activeTool != EditorTool.MARKUP && activeTool != EditorTool.CROP

                        Box(
                            modifier = Modifier
                                .size(
                                    width = (drawW / density).dp,
                                    height = (drawH / density).dp
                                )
                                .graphicsLayer {
                                    rotationZ = totalRot
                                    scaleX = if (currentState.crop.flipH) -1f else 1f
                                    scaleY = if (currentState.crop.flipV) -1f else 1f
                                }
                                .then(
                                    if (canTouchToCompare) {
                                        Modifier.pointerInput(Unit) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                isComparingOriginal = true
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                waitForUpOrCancellation()
                                                isComparingOriginal = false
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val effectiveColorFilter = if (isComparingOriginal) null else combinedColorFilter
                            val effectiveVignette = if (isComparingOriginal) 0f else currentState.tune.vignette

                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Preview",
                                contentScale = ContentScale.FillBounds,
                                colorFilter = effectiveColorFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (currentState.tune.vignette > 0f) {
                                            Modifier.drawWithContent {
                                                drawContent()
                                                val alpha = effectiveVignette / 100f * 0.8f
                                                drawRect(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = alpha)
                                                        ),
                                                        center = Offset(size.width / 2, size.height / 2),
                                                        radius = maxOf(size.width, size.height) * 0.6f
                                                    )
                                                )
                                            }
                                        } else Modifier
                                    )
                            )

                            val currentStrokeWidth = when (activeMarkupTool) {
                                MarkupTool.HIGHLIGHTER -> highlighterStrokeWidth
                                MarkupTool.ARROW -> arrowStrokeWidth
                                else -> penStrokeWidth
                            }
                            MarkupCanvasOverlay(
                                markupState = currentState.markup,
                                activeTool = activeMarkupTool,
                                currentColor = markupColor,
                                currentStrokeWidth = currentStrokeWidth,
                                eraserRadius = eraserRadius,
                                selectedTextId = selectedMarkupTextId,
                                onSelectText = { id ->
                                    selectedMarkupTextId = id
                                },
                                isInteractive = (activeTool == EditorTool.MARKUP),
                                onStateChanged = { newMarkup ->
                                    pushState(currentState.copy(markup = newMarkup))
                                },
                                onEditTextRequest = { textItem ->
                                    editingTextItem = textItem
                                    showTextDialog = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Saving overlay
                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Saving changes…",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ── Bottom Panel (Studio Tonal Container) ────────────────────
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
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 10.dp, bottom = 8.dp)
                ) {
                    // Active tool panel
                    AnimatedContent(
                        targetState = activeTool,
                        transitionSpec = {
                            (slideInVertically(MotionTokens.snappySpring()) { it / 3 } + fadeIn(MotionTokens.snappySpring())) togetherWith
                                    (slideOutVertically(MotionTokens.snappySpring()) { -it / 3 } + fadeOut(MotionTokens.snappySpring()))
                        },
                        label = "toolPanel"
                    ) { tool ->
                        when (tool) {
                            EditorTool.SUGGESTIONS -> SuggestionsPanel(
                                currentTune = currentState.tune,
                                onTuneChanged = { newTune ->
                                    pushState(currentState.copy(tune = newTune))
                                }
                            )
                            EditorTool.CROP -> CropToolPanel(
                                cropState = currentState.crop,
                                imageAspect = imageAspect,
                                onCropStateChanged = { newCrop ->
                                    pushState(currentState.copy(crop = newCrop))
                                }
                            )
                            EditorTool.ADJUST -> AdjustPanel(
                                values = currentState.tune,
                                onValuesChanged = { newValues ->
                                    pushState(currentState.copy(tune = newValues))
                                }
                            )
                            EditorTool.FILTERS -> FiltersPanel(
                                presets = filterPresets,
                                selectedIndex = currentState.filterIndex,
                                originalBitmap = bmp,
                                onFilterSelected = { index ->
                                    pushState(currentState.copy(filterIndex = index))
                                }
                            )
                            EditorTool.MARKUP -> {
                                val currentSelectedText = currentState.markup.texts.find { it.id == selectedMarkupTextId }
                                val settingsRepo = remember(context) { com.inferno.gallery.data.SettingsRepository.getInstance(context) }
                                val tipMarkupDismissed by settingsRepo.tipMarkupGesturesDismissedFlow.collectAsState(initial = true)

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (!tipMarkupDismissed) {
                                        FloatingTipCard(
                                            title = "Quick Tip",
                                            message = "Tap placed text or arrows to move, scale, and rotate freely with two fingers.",
                                            iconResId = R.drawable.ic_ms_touch_app,
                                            onDismiss = {
                                                coroutineScope.launch {
                                                    settingsRepo.dismissTip(com.inferno.gallery.data.SettingsRepository.TIP_MARKUP_GESTURES)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp)
                                        )
                                    }

                                    MarkupBottomDock(
                                        activeTool = activeMarkupTool,
                                        selectedColor = currentSelectedText?.color ?: markupColor,
                                        penStrokeWidth = penStrokeWidth,
                                        highlighterStrokeWidth = highlighterStrokeWidth,
                                        arrowStrokeWidth = arrowStrokeWidth,
                                        eraserRadius = eraserRadius,
                                        selectedText = currentSelectedText,
                                        onToolSelected = { selectedTool ->
                                            activeMarkupTool = selectedTool
                                            if (selectedTool == MarkupTool.TEXT) {
                                                if (currentState.markup.texts.isEmpty()) {
                                                    editingTextItem = null
                                                    showTextDialog = true
                                                } else if (selectedMarkupTextId == null) {
                                                    selectedMarkupTextId = currentState.markup.texts.last().id
                                                }
                                            }
                                        },
                                        onColorSelected = { newColor ->
                                            markupColor = newColor
                                            if (activeMarkupTool == MarkupTool.TEXT && currentSelectedText != null) {
                                                val updated = currentSelectedText.copy(color = newColor)
                                                pushState(currentState.copy(markup = currentState.markup.withUpdatedText(updated)))
                                            }
                                        },
                                        onPenStrokeWidthChanged = { penStrokeWidth = it },
                                        onHighlighterStrokeWidthChanged = { highlighterStrokeWidth = it },
                                        onArrowStrokeWidthChanged = { arrowStrokeWidth = it },
                                        onEraserRadiusChanged = { eraserRadius = it },
                                        onClearAllClick = {
                                            if (currentState.markup.strokes.isNotEmpty() || currentState.markup.texts.isNotEmpty()) {
                                                pushState(currentState.copy(markup = MarkupState()))
                                                selectedMarkupTextId = null
                                            }
                                        },
                                        onAddTextClick = {
                                            editingTextItem = null
                                            showTextDialog = true
                                        },
                                        onRotateTextClick = {
                                            if (currentSelectedText != null) {
                                                val newRotation = (currentSelectedText.rotation + 45f) % 360f
                                                val updated = currentSelectedText.copy(rotation = newRotation)
                                                pushState(currentState.copy(markup = currentState.markup.withUpdatedText(updated)))
                                            }
                                        },
                                        onDeleteTextClick = {
                                            if (currentSelectedText != null) {
                                                pushState(currentState.copy(markup = currentState.markup.withRemovedText(currentSelectedText.id)))
                                                selectedMarkupTextId = null
                                            }
                                        },
                                        onEditTextClick = {
                                            if (currentSelectedText != null) {
                                                editingTextItem = currentSelectedText
                                                showTextDialog = true
                                            }
                                        },
                                        onToggleBoxClick = {
                                            if (currentSelectedText != null) {
                                                val updated = currentSelectedText.copy(hasBackgroundBox = !currentSelectedText.hasBackgroundBox)
                                                pushState(currentState.copy(markup = currentState.markup.withUpdatedText(updated)))
                                            }
                                        },
                                        onTextScaleChanged = { newScale ->
                                            if (currentSelectedText != null) {
                                                val updated = currentSelectedText.copy(scale = newScale)
                                                pushState(currentState.copy(markup = currentState.markup.withUpdatedText(updated)))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Main Tool Selector Row (Google Photos Style Tabs) ──
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(EditorTool.entries.toList()) { _, tool ->
                            val isSelected = activeTool == tool
                            val hasModifications = when (tool) {
                                EditorTool.SUGGESTIONS -> currentState.tune != TuneValues()
                                EditorTool.CROP -> currentState.crop.rotation != 0 || currentState.crop.straightenAngle != 0f || currentState.crop.flipH || currentState.crop.flipV || currentState.crop.cropRect != null
                                EditorTool.ADJUST -> currentState.tune != TuneValues()
                                EditorTool.FILTERS -> currentState.filterIndex > 0
                                EditorTool.MARKUP -> currentState.markup.strokes.isNotEmpty() || currentState.markup.texts.isNotEmpty()
                            }
                            EditorToolChip(
                                label = tool.label,
                                icon = ImageVector.vectorResource(tool.iconRes),
                                isSelected = isSelected,
                                hasModifications = hasModifications,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeTool = tool
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

            // Right: Undo, Redo, Save
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp,
                modifier = Modifier.height(48.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Hold to compare with original
                    Box(
                        modifier = Modifier
                            .clip(ShapeFull)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    isComparingOriginal = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    waitForUpOrCancellation()
                                    isComparingOriginal = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                    ) {
                        FilledTonalIconButton(
                            onClick = {},
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (isComparingOriginal) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isComparingOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_ms_compare),
                                contentDescription = "Hold to compare with original"
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_undo), contentDescription = "Undo")
                    }

                    FilledTonalIconButton(
                        onClick = { redo() },
                        enabled = redoStack.isNotEmpty(),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_ms_redo), contentDescription = "Redo")
                    }

                    // Save dropdown
                    Box {
                        FilledTonalButton(
                            onClick = { showSaveMenu = true },
                            shape = ShapeFull,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                "Save",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        DropdownMenu(
                            expanded = showSaveMenu,
                            onDismissRequest = { showSaveMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save as Copy") },
                                onClick = {
                                    showSaveMenu = false
                                    saveImage(overwrite = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Overwrite Original") },
                                onClick = {
                                    showSaveMenu = false
                                    saveImage(overwrite = true)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating "ORIGINAL" pill indicator during compare
        AnimatedVisibility(
            visible = isComparingOriginal,
            enter = fadeIn(MotionTokens.snappySpring()) + slideInVertically(MotionTokens.snappySpring()) { -it },
            exit = fadeOut(MotionTokens.snappySpring()) + slideOutVertically(MotionTokens.snappySpring()) { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_ms_compare),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Text(
                        "ORIGINAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }
        }

        // Text editing dialog
        if (showTextDialog) {
            MarkupTextEditorDialog(
                initialText = editingTextItem,
                onDismissRequest = { showTextDialog = false },
                onConfirm = { updatedOrNewText ->
                    showTextDialog = false
                    markupColor = updatedOrNewText.color
                    if (editingTextItem != null) {
                        pushState(currentState.copy(markup = currentState.markup.withUpdatedText(updatedOrNewText)))
                    } else {
                        pushState(currentState.copy(markup = currentState.markup.withAddedText(updatedOrNewText)))
                        selectedMarkupTextId = updatedOrNewText.id
                    }
                },
                onDelete = { idToDelete ->
                    showTextDialog = false
                    pushState(currentState.copy(markup = currentState.markup.withRemovedText(idToDelete)))
                    selectedMarkupTextId = null
                }
            )
        }
    }
}

// ── Editor Tool Chip ─────────────────────────────────────────────────────────

@Composable
private fun EditorToolChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    hasModifications: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = ShapeFull,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 0.dp,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(ShapeFull)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
            if (hasModifications) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// ── Suggestions Panel ────────────────────────────────────────────────────────

@Composable
private fun SuggestionsPanel(
    currentTune: TuneValues,
    onTuneChanged: (TuneValues) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    data class SuggestionItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val tune: TuneValues
    )

    val suggestions = remember {
        listOf(
            SuggestionItem(
                id = "enhance",
                title = "Enhance",
                subtitle = "Auto balanced",
                tune = TuneValues(
                    brightness = 5f,
                    contrast = 15f,
                    highlights = -8f,
                    shadows = 18f,
                    saturation = 14f,
                    warmth = 4f
                )
            ),
            SuggestionItem(
                id = "warm",
                title = "Warm",
                subtitle = "Golden hour",
                tune = TuneValues(
                    warmth = 28f,
                    saturation = 10f,
                    contrast = 8f
                )
            ),
            SuggestionItem(
                id = "cool",
                title = "Cool",
                subtitle = "Crisp chill",
                tune = TuneValues(
                    warmth = -28f,
                    saturation = 6f,
                    contrast = 10f
                )
            ),
            SuggestionItem(
                id = "dynamic",
                title = "Dynamic",
                subtitle = "High pop",
                tune = TuneValues(
                    contrast = 24f,
                    highlights = -14f,
                    shadows = 20f,
                    saturation = 20f,
                    whitePoint = 10f,
                    blackPoint = -10f
                )
            )
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(suggestions) { _, item ->
            val isApplied = currentTune == item.tune
            Surface(
                shape = ShapeLarge,
                color = if (isApplied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isApplied) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = if (isApplied) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .clip(ShapeLarge)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isApplied) {
                            onTuneChanged(TuneValues())
                        } else {
                            onTuneChanged(item.tune)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isApplied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_auto_fix_high),
                            contentDescription = null,
                            tint = if (isApplied) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Adjust Panel (Full Suite) ────────────────────────────────────────────────

@Composable
private fun AdjustPanel(
    values: TuneValues,
    onValuesChanged: (TuneValues) -> Unit
) {
    data class AdjustParam(
        val label: String,
        val icon: ImageVector,
        val value: Float,
        val minVal: Float = -100f,
        val maxVal: Float = 100f,
        val setter: (Float) -> TuneValues
    )

    val params = listOf(
        AdjustParam("Brightness", ImageVector.vectorResource(R.drawable.ic_ms_light_mode), values.brightness) { values.copy(brightness = it) },
        AdjustParam("Contrast", ImageVector.vectorResource(R.drawable.ic_ms_contrast), values.contrast) { values.copy(contrast = it) },
        AdjustParam("Highlights", ImageVector.vectorResource(R.drawable.ic_ms_bolt), values.highlights) { values.copy(highlights = it) },
        AdjustParam("Shadows", ImageVector.vectorResource(R.drawable.ic_ms_dark_mode), values.shadows) { values.copy(shadows = it) },
        AdjustParam("White Point", ImageVector.vectorResource(R.drawable.ic_ms_circle), values.whitePoint) { values.copy(whitePoint = it) },
        AdjustParam("Black Point", ImageVector.vectorResource(R.drawable.ic_ms_contrast), values.blackPoint) { values.copy(blackPoint = it) },
        AdjustParam("Saturation", ImageVector.vectorResource(R.drawable.ic_ms_palette), values.saturation) { values.copy(saturation = it) },
        AdjustParam("Warmth", ImageVector.vectorResource(R.drawable.ic_ms_wb_twilight), values.warmth) { values.copy(warmth = it) },
        AdjustParam("Tint", ImageVector.vectorResource(R.drawable.ic_ms_tune), values.tint) { values.copy(tint = it) },
        AdjustParam("Vignette", ImageVector.vectorResource(R.drawable.ic_ms_center_focus_strong), values.vignette, minVal = 0f, maxVal = 100f) { values.copy(vignette = it) }
    )

    var selectedParam by remember { mutableIntStateOf(0) }
    val param = params[selectedParam.coerceIn(0, params.lastIndex)]
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Parameter selector chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(params) { index, p ->
                val isSelected = selectedParam == index
                val isModified = p.value != 0f
                Surface(
                    shape = ShapeFull,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isModified -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isModified -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(ShapeFull)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedParam = index
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(p.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            p.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        if (isModified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = ShapeFull,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    "${if (p.value > 0) "+" else ""}${p.value.roundToInt()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Centered Slider Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(54.dp)
            ) {
                Text(
                    text = "${if (param.value > 0) "+" else ""}${param.value.roundToInt()}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            var lastParamInt by remember(selectedParam) { mutableIntStateOf(param.value.roundToInt()) }

            Slider(
                value = param.value,
                onValueChange = { newValue ->
                    val rounded = newValue.roundToInt().toFloat()
                    val intVal = rounded.roundToInt()
                    if (intVal == 0 && lastParamInt != 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    lastParamInt = intVal
                    onValuesChanged(param.setter(rounded))
                },
                valueRange = param.minVal..param.maxVal,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.weight(1f)
            )

            // Reset button
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onValuesChanged(param.setter(0f))
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_ms_refresh), contentDescription = "Reset",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Crop Tool Panel ──────────────────────────────────────────────────────────

@Composable
private fun CropToolPanel(
    cropState: CropState,
    imageAspect: Float,
    onCropStateChanged: (CropState) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val aspects = listOf("Free", "Original", "1:1", "4:5", "5:4", "3:4", "4:3", "2:3", "3:2", "9:16", "16:9")
    val isRotated90 = cropState.rotation % 180 != 0
    val effectiveAspect = if (isRotated90) 1f / imageAspect else imageAspect

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Aspect ratio row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(aspects) { _, label ->
                val isSelected = cropState.aspectLabel == label
                Surface(
                    shape = ShapeFull,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(ShapeFull)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val newRect = CropGeometryHelper.calculateInitialAspectRect(label, effectiveAspect)
                            onCropStateChanged(cropState.copy(aspectLabel = label, cropRect = newRect))
                        }
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Straighten Angle Slider (-45°..+45°)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.width(58.dp)
            ) {
                Text(
                    text = "${if (cropState.straightenAngle > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", cropState.straightenAngle)}°",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            var lastAngleInt by remember { mutableIntStateOf(cropState.straightenAngle.roundToInt()) }
            Slider(
                value = cropState.straightenAngle,
                onValueChange = { angle ->
                    val rounded = (angle * 2f).roundToInt() / 2f
                    val intAngle = rounded.roundToInt()
                    if (intAngle == 0 && lastAngleInt != 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    lastAngleInt = intAngle
                    onCropStateChanged(cropState.copy(straightenAngle = rounded))
                },
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.weight(1f)
            )

            // Reset angle button
            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCropStateChanged(cropState.copy(straightenAngle = 0f))
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(48.dp)
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                    contentDescription = "Reset Angle",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Action buttons: Rotate, Flip H, Flip V, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newRotation = (cropState.rotation + 90) % 360
                        val newEffectiveAspect = if (newRotation % 180 != 0) 1f / imageAspect else imageAspect
                        val newRect = CropGeometryHelper.calculateInitialAspectRect(cropState.aspectLabel, newEffectiveAspect)
                        onCropStateChanged(cropState.copy(rotation = newRotation, cropRect = newRect))
                    },
                    shape = ShapeLarge,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_rotate_right), contentDescription = "Rotate")
                }
                Text("Rotate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCropStateChanged(cropState.copy(flipH = !cropState.flipH))
                    },
                    shape = ShapeLarge,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz), contentDescription = "Flip H")
                }
                Text("Flip H", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCropStateChanged(cropState.copy(flipV = !cropState.flipV))
                    },
                    shape = ShapeLarge,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                        contentDescription = "Flip V",
                        modifier = Modifier.graphicsLayer { rotationZ = 90f }
                    )
                }
                Text("Flip V", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCropStateChanged(CropState(cropRect = Rect(0f, 0f, 1f, 1f), aspectLabel = "Free"))
                    },
                    shape = ShapeLarge,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_refresh), contentDescription = "Reset")
                }
                Text("Reset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Crop Preview ─────────────────────────────────────────────────────────────

@Composable
private fun CropPreview(
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap,
    imageAspect: Float,
    cropState: CropState,
    colorFilter: androidx.compose.ui.graphics.ColorFilter?,
    onCropRectChanged: (Rect) -> Unit
) {
    val isRotated90 = cropState.rotation % 180 != 0
    val effectiveAspect = if (isRotated90) 1f / imageAspect else imageAspect
    var imageRect by remember { mutableStateOf(Rect.Zero) }
    val cropRect = cropState.cropRect ?: Rect(0f, 0f, 1f, 1f)
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }
    val haptic = LocalHapticFeedback.current

    val totalRotation = cropState.rotation.toFloat() + cropState.straightenAngle

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()

        val (imgW, imgH) = if (effectiveAspect > maxW / maxH) {
            maxW to maxW / effectiveAspect
        } else {
            maxH * effectiveAspect to maxH
        }

        val imgLeft = (maxW - imgW) / 2f
        val imgTop = (maxH - imgH) / 2f

        LaunchedEffect(imgW, imgH, imgLeft, imgTop) {
            imageRect = Rect(imgLeft, imgTop, imgLeft + imgW, imgTop + imgH)
        }

        val density = LocalDensity.current.density
        val (drawW, drawH) = if (isRotated90) imgH to imgW else imgW to imgH

        // Draw image
        Image(
            bitmap = imageBitmap,
            contentDescription = "Crop preview",
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = Modifier
                .size(
                    width = (drawW / density).dp,
                    height = (drawH / density).dp
                )
                .graphicsLayer {
                    rotationZ = totalRotation
                    scaleX = if (cropState.flipH) -1f else 1f
                    scaleY = if (cropState.flipV) -1f else 1f
                }
        )

        // Crop overlay
        if (imageRect != Rect.Zero) {
            val isDragging = activeHandle != CropHandle.NONE
            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val left = imageRect.left + cropRect.left * imageRect.width
                val top = imageRect.top + cropRect.top * imageRect.height
                val right = imageRect.left + cropRect.right * imageRect.width
                val bottom = imageRect.top + cropRect.bottom * imageRect.height

                // Dim area outside crop
                drawRect(Color.Black.copy(alpha = 0.55f), Offset.Zero, Size(size.width, top))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, bottom), Size(size.width, size.height - bottom))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, top), Size(left, bottom - top))
                drawRect(Color.Black.copy(alpha = 0.55f), Offset(right, top), Size(size.width - right, bottom - top))

                // Crop border
                drawRect(
                    Color.White,
                    Offset(left, top),
                    Size(right - left, bottom - top),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                )

                // Rule of thirds lines (brighter when dragging)
                val gridAlpha = if (isDragging) 0.65f else 0.25f
                val thirdW = (right - left) / 3f
                val thirdH = (bottom - top) / 3f
                for (i in 1..2) {
                    drawLine(Color.White.copy(alpha = gridAlpha), Offset(left + thirdW * i, top), Offset(left + thirdW * i, bottom), 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = gridAlpha), Offset(left, top + thirdH * i), Offset(right, top + thirdH * i), 1.dp.toPx())
                }

                // Corner bracket handles
                val handleLen = 22.dp.toPx()
                val handleThickness = 3.5.dp.toPx()
                val corners = listOf(
                    Triple(CropHandle.TOP_LEFT, Offset(left, top), Pair(1f, 1f)),
                    Triple(CropHandle.TOP_RIGHT, Offset(right, top), Pair(-1f, 1f)),
                    Triple(CropHandle.BOTTOM_LEFT, Offset(left, bottom), Pair(1f, -1f)),
                    Triple(CropHandle.BOTTOM_RIGHT, Offset(right, bottom), Pair(-1f, -1f))
                )
                corners.forEach { (handle, pt, dirs) ->
                    val isCornerActive = activeHandle == handle
                    val cornerColor = if (isCornerActive) primaryColor else Color.White
                    val thickness = if (isCornerActive) handleThickness * 1.5f else handleThickness
                    drawLine(cornerColor, pt, Offset(pt.x + handleLen * dirs.first, pt.y), thickness)
                    drawLine(cornerColor, pt, Offset(pt.x, pt.y + handleLen * dirs.second), thickness)
                }

                // Edge handles (for Free mode)
                if (cropState.aspectLabel == "Free") {
                    val edgeLen = 20.dp.toPx()
                    val edgeThickness = 3.dp.toPx()
                    val midX = (left + right) / 2f
                    val midY = (top + bottom) / 2f

                    // Top
                    val isTopActive = activeHandle == CropHandle.TOP
                    drawLine(
                        if (isTopActive) primaryColor else Color.White,
                        Offset(midX - edgeLen / 2, top),
                        Offset(midX + edgeLen / 2, top),
                        if (isTopActive) edgeThickness * 1.5f else edgeThickness
                    )
                    // Bottom
                    val isBottomActive = activeHandle == CropHandle.BOTTOM
                    drawLine(
                        if (isBottomActive) primaryColor else Color.White,
                        Offset(midX - edgeLen / 2, bottom),
                        Offset(midX + edgeLen / 2, bottom),
                        if (isBottomActive) edgeThickness * 1.5f else edgeThickness
                    )
                    // Left
                    val isLeftActive = activeHandle == CropHandle.LEFT
                    drawLine(
                        if (isLeftActive) primaryColor else Color.White,
                        Offset(left, midY - edgeLen / 2),
                        Offset(left, midY + edgeLen / 2),
                        if (isLeftActive) edgeThickness * 1.5f else edgeThickness
                    )
                    // Right
                    val isRightActive = activeHandle == CropHandle.RIGHT
                    drawLine(
                        if (isRightActive) primaryColor else Color.White,
                        Offset(right, midY - edgeLen / 2),
                        Offset(right, midY + edgeLen / 2),
                        if (isRightActive) edgeThickness * 1.5f else edgeThickness
                    )
                }
            }

            // Interactive Gesture detector
            val cornerRadiusPx = with(LocalDensity.current) { 44.dp.toPx() }
            val edgeMarginPx = with(LocalDensity.current) { 28.dp.toPx() }
            val minSizePx = with(LocalDensity.current) { 48.dp.toPx() }

            val currentCropRect by rememberUpdatedState(cropRect)
            val currentAspectLabel by rememberUpdatedState(cropState.aspectLabel)
            val currentEffectiveAspect by rememberUpdatedState(effectiveAspect)
            val currentOnCropRectChanged by rememberUpdatedState(onCropRectChanged)
            val currentImageRect by rememberUpdatedState(imageRect)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val imgRect = currentImageRect
                                val cRect = currentCropRect
                                val handle = CropGeometryHelper.hitTest(
                                    touchX = offset.x,
                                    touchY = offset.y,
                                    cropRect = cRect,
                                    imageRect = imgRect,
                                    cornerHitRadiusPx = cornerRadiusPx,
                                    edgeHitMarginPx = edgeMarginPx
                                )
                                activeHandle = handle
                                if (handle != CropHandle.NONE) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                activeHandle = CropHandle.NONE
                            },
                            onDragCancel = {
                                activeHandle = CropHandle.NONE
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val imgRect = currentImageRect
                                if (activeHandle != CropHandle.NONE && imgRect.width > 0 && imgRect.height > 0) {
                                    val dx = dragAmount.x / imgRect.width
                                    val dy = dragAmount.y / imgRect.height
                                    val minWNorm = minSizePx / imgRect.width
                                    val minHNorm = minSizePx / imgRect.height

                                    val newRect = CropGeometryHelper.updateCropRect(
                                        currentRect = currentCropRect,
                                        handle = activeHandle,
                                        deltaXNormalized = dx,
                                        deltaYNormalized = dy,
                                        aspectLabel = currentAspectLabel,
                                        effectiveAspect = currentEffectiveAspect,
                                        minWidthNormalized = minWNorm,
                                        minHeightNormalized = minHNorm
                                    )
                                    currentOnCropRectChanged(newRect)
                                }
                            }
                        )
                    }
            )
        }
    }
}

// ── Filters Panel ────────────────────────────────────────────────────────────

@Composable
private fun FiltersPanel(
    presets: List<FilterPreset>,
    selectedIndex: Int,
    originalBitmap: Bitmap,
    onFilterSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val thumbnails = remember(originalBitmap) {
        val thumbSize = 120
        val aspect = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val tw = if (aspect >= 1f) thumbSize else (thumbSize * aspect).toInt()
        val th = if (aspect >= 1f) (thumbSize / aspect).toInt() else thumbSize
        val smallBmp = Bitmap.createScaledBitmap(originalBitmap, tw, th, true)

        presets.map { preset ->
            val result = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(preset.matrix)
            }
            canvas.drawBitmap(smallBmp, 0f, 0f, paint)
            result.asImageBitmap()
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(presets) { index, preset ->
            val isSelected = selectedIndex == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterSelected(index)
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(ShapeLarge)
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = ShapeLarge
                        )
                ) {
                    thumbnails.getOrNull(index)?.let { thumb ->
                        Image(
                            bitmap = thumb,
                            contentDescription = preset.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            border = BorderStroke(1.5.dp, Color.White),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    preset.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



