@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.inferno.gallery.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.inferno.gallery.R
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.ui.editor.components.EditorRulerSlider
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.PhotonStudioTheme
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLarge
import com.inferno.gallery.workers.MediaSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

// ── Data classes & Studio Models ──────────────────────────────────────────────

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
    val offsetY: Float = 0f,
    val isCropFill: Boolean = true
)

enum class CollageStyle(
    val label: String,
    val subtitle: String,
    val defaultGap: Float,
    val defaultMargin: Float,
    val defaultCorner: Float,
    val defaultBorderWidth: Float
) {
    MODERN("Bento", "Clean rounded", 5f, 10f, 16f, 0f),
    POLAROID("Polaroid", "Instant frame", 8f, 14f, 6f, 6f),
    FILM("35mm Film", "Cinema strip", 6f, 18f, 4f, 0f),
    SCRAPBOOK("Scrapbook", "Tilted cards", 10f, 16f, 10f, 4f),
    ARCH("Arch", "Editorial dome", 6f, 12f, 16f, 0f),
    CAPSULE("Capsule", "Oval cutouts", 6f, 12f, 28f, 2f),
    DIAMOND("Geometric", "Octagon cut", 6f, 12f, 12f, 0f)
}

enum class FrameParameter(val label: String, val min: Float, val max: Float, val defaultVal: Float) {
    GAP("Gap", 0f, 28f, 5f),
    MARGIN("Margin", 0f, 32f, 10f),
    CORNERS("Corners", 0f, 32f, 16f),
    BORDER("Stroke", 0f, 12f, 0f)
}

sealed class CollageBackground(val label: String) {
    data class Solid(val color: Color, val name: String) : CollageBackground(name)
    data class Gradient(val start: Color, val end: Color, val name: String) : CollageBackground(name)
    data class Pattern(val baseColor: Color, val dotColor: Color, val isGrid: Boolean, val name: String) : CollageBackground(name)
}

enum class CollageEditTab(val label: String, @param:androidx.annotation.DrawableRes val iconRes: Int) {
    STYLE("Style", R.drawable.ic_ms_auto_fix_high),
    LAYOUT("Layout", R.drawable.ic_ms_grid_view),
    FRAME("Frame", R.drawable.ic_ms_thin_tune),
    BACKGROUND("Backdrop", R.drawable.ic_ms_thin_palette),
    RATIO("Ratio", R.drawable.ic_ms_thin_aspect_ratio)
}

// ── Custom Slot Shapes (Compose + Android Path) ───────────────────────────────

private fun buildSlotShape(style: CollageStyle, cornerPx: Float): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    when (style) {
        CollageStyle.ARCH -> {
            val archRadius = min(w / 2f, h * 0.45f)
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(0f, 0f, w, h),
                    topLeft = CornerRadius(archRadius, archRadius),
                    topRight = CornerRadius(archRadius, archRadius),
                    bottomLeft = CornerRadius(cornerPx * 0.5f, cornerPx * 0.5f),
                    bottomRight = CornerRadius(cornerPx * 0.5f, cornerPx * 0.5f)
                )
            )
        }
        CollageStyle.CAPSULE -> {
            val r = min(w, h) / 2f
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(0f, 0f, w, h),
                    cornerRadius = CornerRadius(r, r)
                )
            )
        }
        CollageStyle.DIAMOND -> {
            val cut = min(w, h) * 0.22f
            moveTo(cut, 0f)
            lineTo(w - cut, 0f)
            lineTo(w, cut)
            lineTo(w, h - cut)
            lineTo(w - cut, h)
            lineTo(cut, h)
            lineTo(0f, h - cut)
            lineTo(0f, cut)
            close()
        }
        else -> {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(0f, 0f, w, h),
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            )
        }
    }
}

private fun buildAndroidSlotPath(style: CollageStyle, rect: RectF, cornerPx: Float): Path {
    val path = Path()
    val w = rect.width()
    val h = rect.height()
    when (style) {
        CollageStyle.ARCH -> {
            val archR = min(w / 2f, h * 0.45f)
            val botR = cornerPx * 0.5f
            val radii = floatArrayOf(archR, archR, archR, archR, botR, botR, botR, botR)
            path.addRoundRect(rect, radii, Path.Direction.CW)
        }
        CollageStyle.CAPSULE -> {
            val r = min(w, h) / 2f
            path.addRoundRect(rect, r, r, Path.Direction.CW)
        }
        CollageStyle.DIAMOND -> {
            val cut = min(w, h) * 0.22f
            path.moveTo(rect.left + cut, rect.top)
            path.lineTo(rect.right - cut, rect.top)
            path.lineTo(rect.right, rect.top + cut)
            path.lineTo(rect.right, rect.bottom - cut)
            path.lineTo(rect.right - cut, rect.bottom)
            path.lineTo(rect.left + cut, rect.bottom)
            path.lineTo(rect.left, rect.bottom - cut)
            path.lineTo(rect.left, rect.top + cut)
            path.close()
        }
        else -> {
            path.addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW)
        }
    }
    return path
}

private fun scrapbookTiltForIndex(index: Int): Float = when (index % 5) {
    0 -> -2.4f
    1 -> 2.1f
    2 -> 1.6f
    3 -> -1.9f
    else -> 1.2f
}

// ── Layout preset builder ─────────────────────────────────────────────────────

fun getLayoutPresets(itemCount: Int): List<CollageLayout> {
    val list = mutableListOf<CollageLayout>()
    if (itemCount <= 0) return list

    if (itemCount == 1) {
        list.add(CollageLayout("Full", listOf(Rect(0f, 0f, 1f, 1f))))
        list.add(CollageLayout("Poster", listOf(Rect(0.08f, 0.06f, 0.92f, 0.88f))))
        return list
    }

    when (itemCount) {
        2 -> {
            list.add(CollageLayout("Split", listOf(Rect(0f, 0f, 0.5f, 1f), Rect(0.5f, 0f, 1f, 1f))))
            list.add(CollageLayout("Stacked", listOf(Rect(0f, 0f, 1f, 0.5f), Rect(0f, 0.5f, 1f, 1f))))
            list.add(CollageLayout("Golden", listOf(Rect(0f, 0f, 0.62f, 1f), Rect(0.62f, 0f, 1f, 1f))))
            list.add(CollageLayout("Cinema", listOf(Rect(0f, 0f, 1f, 0.64f), Rect(0f, 0.64f, 1f, 1f))))
            list.add(CollageLayout("Duo Offset", listOf(Rect(0.02f, 0.02f, 0.58f, 0.78f), Rect(0.42f, 0.22f, 0.98f, 0.98f))))
            list.add(CollageLayout("PIP Corner", listOf(Rect(0f, 0f, 1f, 1f), Rect(0.60f, 0.60f, 0.96f, 0.96f))))
        }
        3 -> {
            list.add(CollageLayout("Hero Left", listOf(Rect(0f, 0f, 0.56f, 1f), Rect(0.56f, 0f, 1f, 0.5f), Rect(0.56f, 0.5f, 1f, 1f))))
            list.add(CollageLayout("Hero Top", listOf(Rect(0f, 0f, 1f, 0.55f), Rect(0f, 0.55f, 0.5f, 1f), Rect(0.5f, 0.55f, 1f, 1f))))
            list.add(CollageLayout("Bento", listOf(Rect(0f, 0f, 0.6f, 0.62f), Rect(0.6f, 0f, 1f, 1f), Rect(0f, 0.62f, 0.6f, 1f))))
            list.add(CollageLayout("2 Top, 1", listOf(Rect(0f, 0f, 0.5f, 0.45f), Rect(0.5f, 0f, 1f, 0.45f), Rect(0f, 0.45f, 1f, 1f))))
            list.add(CollageLayout("Triptych", listOf(Rect(0f, 0f, 0.333f, 1f), Rect(0.333f, 0f, 0.666f, 1f), Rect(0.666f, 0f, 1f, 1f))))
            list.add(CollageLayout("Filmstrip", listOf(Rect(0f, 0f, 1f, 0.333f), Rect(0f, 0.333f, 1f, 0.666f), Rect(0f, 0.666f, 1f, 1f))))
            list.add(CollageLayout("Center Stage", listOf(Rect(0f, 0.12f, 0.32f, 0.88f), Rect(0.32f, 0f, 0.68f, 1f), Rect(0.68f, 0.12f, 1f, 0.88f))))
        }
        4 -> {
            list.add(CollageLayout("2x2 Grid", listOf(
                Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
                Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f)
            )))
            list.add(CollageLayout("Bento Box", listOf(
                Rect(0f, 0f, 0.58f, 0.62f), Rect(0.58f, 0f, 1f, 0.38f),
                Rect(0f, 0.62f, 0.58f, 1f), Rect(0.58f, 0.38f, 1f, 1f)
            )))
            list.add(CollageLayout("Pinwheel", listOf(
                Rect(0f, 0f, 0.62f, 0.38f), Rect(0.62f, 0f, 1f, 0.62f),
                Rect(0f, 0.38f, 0.38f, 1f), Rect(0.38f, 0.62f, 1f, 1f)
            )))
            list.add(CollageLayout("Hero Left", listOf(
                Rect(0f, 0f, 0.56f, 1f), Rect(0.56f, 0f, 1f, 0.333f),
                Rect(0.56f, 0.333f, 1f, 0.666f), Rect(0.56f, 0.666f, 1f, 1f)
            )))
            list.add(CollageLayout("Hero Top", listOf(
                Rect(0f, 0f, 1f, 0.56f), Rect(0f, 0.56f, 0.333f, 1f),
                Rect(0.333f, 0.56f, 0.666f, 1f), Rect(0.666f, 0.56f, 1f, 1f)
            )))
            list.add(CollageLayout("Columns", (0 until 4).map { Rect(it * 0.25f, 0f, (it + 1) * 0.25f, 1f) }))
            list.add(CollageLayout("Rows", (0 until 4).map { Rect(0f, it * 0.25f, 1f, (it + 1) * 0.25f) }))
        }
        5 -> {
            list.add(CollageLayout("Bento 5", listOf(
                Rect(0f, 0f, 0.6f, 0.6f), Rect(0.6f, 0f, 1f, 0.3f), Rect(0.6f, 0.3f, 1f, 0.6f),
                Rect(0f, 0.6f, 0.5f, 1f), Rect(0.5f, 0.6f, 1f, 1f)
            )))
            list.add(CollageLayout("2 Top, 3", listOf(
                Rect(0f, 0f, 0.5f, 0.52f), Rect(0.5f, 0f, 1f, 0.52f),
                Rect(0f, 0.52f, 0.333f, 1f), Rect(0.333f, 0.52f, 0.666f, 1f), Rect(0.666f, 0.52f, 1f, 1f)
            )))
            list.add(CollageLayout("Hero + 4", listOf(
                Rect(0f, 0f, 0.52f, 1f),
                Rect(0.52f, 0f, 0.76f, 0.5f), Rect(0.76f, 0f, 1f, 0.5f),
                Rect(0.52f, 0.5f, 0.76f, 1f), Rect(0.76f, 0.5f, 1f, 1f)
            )))
            list.add(CollageLayout("Center Focus", listOf(
                Rect(0f, 0f, 0.5f, 0.5f), Rect(0.5f, 0f, 1f, 0.5f),
                Rect(0f, 0.5f, 0.5f, 1f), Rect(0.5f, 0.5f, 1f, 1f),
                Rect(0.24f, 0.24f, 0.76f, 0.76f)
            )))
            list.add(CollageLayout("Columns", (0 until 5).map { Rect(it * 0.2f, 0f, (it + 1) * 0.2f, 1f) }))
            list.add(CollageLayout("Rows", (0 until 5).map { Rect(0f, it * 0.2f, 1f, (it + 1) * 0.2f) }))
        }
        6 -> {
            list.add(CollageLayout("2x3 Grid", listOf(
                Rect(0f, 0f, 0.5f, 0.333f), Rect(0.5f, 0f, 1f, 0.333f),
                Rect(0f, 0.333f, 0.5f, 0.666f), Rect(0.5f, 0.333f, 1f, 0.666f),
                Rect(0f, 0.666f, 0.5f, 1f), Rect(0.5f, 0.666f, 1f, 1f)
            )))
            list.add(CollageLayout("3x2 Grid", listOf(
                Rect(0f, 0f, 0.333f, 0.5f), Rect(0.333f, 0f, 0.666f, 0.5f), Rect(0.666f, 0f, 1f, 0.5f),
                Rect(0f, 0.5f, 0.333f, 1f), Rect(0.333f, 0.5f, 0.666f, 1f), Rect(0.666f, 0.5f, 1f, 1f)
            )))
            list.add(CollageLayout("Hero Bento", listOf(
                Rect(0f, 0f, 0.666f, 0.666f), Rect(0.666f, 0f, 1f, 0.333f), Rect(0.666f, 0.333f, 1f, 0.666f),
                Rect(0f, 0.666f, 0.333f, 1f), Rect(0.333f, 0.666f, 0.666f, 1f), Rect(0.666f, 0.666f, 1f, 1f)
            )))
            list.add(CollageLayout("1-2-3", listOf(
                Rect(0f, 0f, 1f, 0.36f),
                Rect(0f, 0.36f, 0.5f, 0.68f), Rect(0.5f, 0.36f, 1f, 0.68f),
                Rect(0f, 0.68f, 0.333f, 1f), Rect(0.333f, 0.68f, 0.666f, 1f), Rect(0.666f, 0.68f, 1f, 1f)
            )))
            list.add(CollageLayout("Columns", (0 until 6).map { Rect(it / 6f, 0f, (it + 1) / 6f, 1f) }))
        }
        else -> {
            val cols = if (itemCount <= 8) 4 else 3
            val rows = (itemCount + cols - 1) / cols
            val grid = (0 until itemCount).map { idx ->
                val c = idx % cols
                val r = idx / cols
                val rowItems = if (r == rows - 1 && itemCount % cols != 0) itemCount % cols else cols
                Rect(c.toFloat() / rowItems, r.toFloat() / rows, (c + 1).toFloat() / rowItems, (r + 1).toFloat() / rows)
            }
            list.add(CollageLayout("Grid", grid))

            val remaining = itemCount - 1
            val rightCols = 2
            val rightRows = (remaining + rightCols - 1) / rightCols
            val heroLeft = listOf(Rect(0f, 0f, 0.45f, 1f)) + (0 until remaining).map { idx ->
                val c = idx % rightCols
                val r = idx / rightCols
                Rect(
                    0.45f + c * (0.55f / rightCols),
                    r.toFloat() / rightRows,
                    0.45f + (c + 1) * (0.55f / rightCols),
                    (r + 1).toFloat() / rightRows
                )
            }
            list.add(CollageLayout("Hero Left", heroLeft))

            val topHalf = itemCount / 2
            val bottomHalf = itemCount - topHalf
            val topBounds = (0 until topHalf).map { i -> Rect(i.toFloat() / topHalf, 0f, (i + 1).toFloat() / topHalf, 0.5f) }
            val bottomBounds = (0 until bottomHalf).map { i -> Rect(i.toFloat() / bottomHalf, 0.5f, (i + 1).toFloat() / bottomHalf, 1f) }
            list.add(CollageLayout("Split Rows", topBounds + bottomBounds))

            list.add(CollageLayout("Columns", (0 until itemCount).map { idx ->
                val w = 1f / itemCount
                Rect(idx * w, 0f, (idx + 1) * w, 1f)
            }))
        }
    }
    return list
}

private data class CollageSnapshot(
    val slots: List<SlotState>,
    val layoutIndex: Int,
    val style: CollageStyle
)

private enum class ChooserMode { REPLACE_SLOT, ADD_PHOTOS }

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

    var selectedStyle by remember { mutableStateOf(CollageStyle.MODERN) }
    var selectedLayoutIndex by remember { mutableIntStateOf(0) }
    val safeLayoutIndex = selectedLayoutIndex.coerceIn(0, (presets.size - 1).coerceAtLeast(0))

    var selectedSlotIndex by remember { mutableIntStateOf(-1) }
    var swapSourceIndex by remember { mutableIntStateOf(-1) }
    val undoStack = remember { mutableStateListOf<CollageSnapshot>() }
    val redoStack = remember { mutableStateListOf<CollageSnapshot>() }

    fun pushUndo() {
        undoStack.add(CollageSnapshot(slotStates.map { it.copy() }, safeLayoutIndex, selectedStyle))
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
    }

    var innerGap by remember { mutableFloatStateOf(CollageStyle.MODERN.defaultGap) }
    var outerMargin by remember { mutableFloatStateOf(CollageStyle.MODERN.defaultMargin) }
    var cornerRadius by remember { mutableFloatStateOf(CollageStyle.MODERN.defaultCorner) }
    var borderWidth by remember { mutableFloatStateOf(CollageStyle.MODERN.defaultBorderWidth) }
    var activeFrameParam by remember { mutableStateOf(FrameParameter.GAP) }

    val defaultBg = remember { CollageBackground.Solid(Color(0xFFF6F2EA), "Paper") }
    var selectedBackground by remember { mutableStateOf<CollageBackground>(defaultBg) }

    var selectedRatioLabel by remember { mutableStateOf("1:1") }
    var ratioValue by remember { mutableFloatStateOf(1f) }
    var activeTab by remember { mutableStateOf(CollageEditTab.STYLE) }
    var isSaving by remember { mutableStateOf(false) }
    var chooserMode by remember { mutableStateOf<ChooserMode?>(null) }

    fun applyStylePreset(style: CollageStyle) {
        pushUndo()
        selectedStyle = style
        innerGap = style.defaultGap
        outerMargin = style.defaultMargin
        cornerRadius = style.defaultCorner
        borderWidth = style.defaultBorderWidth
        selectedBackground = when (style) {
            CollageStyle.FILM -> CollageBackground.Solid(Color(0xFF101113), "Cinema")
            CollageStyle.POLAROID -> CollageBackground.Solid(Color(0xFFF4EFE6), "Warm Paper")
            CollageStyle.SCRAPBOOK -> CollageBackground.Pattern(Color(0xFFF7F3EB), Color(0x26000000), false, "Dot Grid")
            else -> selectedBackground
        }
    }

    val animatedRatio by animateFloatAsState(
        targetValue = ratioValue,
        animationSpec = MotionTokens.gentleSpring(),
        label = "collageRatioAnim"
    )
    val animatedGap by animateDpAsState(
        targetValue = innerGap.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "collageGapAnim"
    )
    val animatedMargin by animateDpAsState(
        targetValue = outerMargin.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "collageMarginAnim"
    )
    val animatedCorner by animateDpAsState(
        targetValue = cornerRadius.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "collageCornerAnim"
    )

    val database = remember { DatabaseProvider.getDatabase(context) }
    val allMediaList by remember { database.mediaDao().observeAllMedia() }.collectAsState(initial = emptyList())
    val imageMedia = remember(allMediaList) { allMediaList.filter { !it.isVideo && it.bucketName != "Trash" } }

    BackHandler {
        when {
            chooserMode != null -> chooserMode = null
            swapSourceIndex != -1 -> swapSourceIndex = -1
            selectedSlotIndex != -1 -> selectedSlotIndex = -1
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
            // ── Top Studio Bar (Matches PhotonEditorScreen) ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Close circle + Connected Undo / Redo M3 Expressive pills + Shuffle
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
                                    redoStack.add(CollageSnapshot(slotStates.map { it.copy() }, safeLayoutIndex, selectedStyle))
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    slotStates = prev.slots
                                    selectedLayoutIndex = prev.layoutIndex
                                    selectedStyle = prev.style
                                    selectedSlotIndex = -1
                                    swapSourceIndex = -1
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
                                    undoStack.add(CollageSnapshot(slotStates.map { it.copy() }, safeLayoutIndex, selectedStyle))
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    slotStates = next.slots
                                    selectedLayoutIndex = next.layoutIndex
                                    selectedStyle = next.style
                                    selectedSlotIndex = -1
                                    swapSourceIndex = -1
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

                    if (slotStates.size > 1) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pushUndo()
                                slotStates = slotStates.shuffled()
                                selectedSlotIndex = -1
                                swapSourceIndex = -1
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_thin_refresh),
                                contentDescription = "Shuffle photos",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Right: + Add Photo & Prominent Save Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (slotStates.size < 9) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                chooserMode = ChooserMode.ADD_PHOTOS
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
                                    contentDescription = "Add Photo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${slotStates.size}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (isSaving) return@Surface
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSaving = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val currentLayout = presets.getOrNull(safeLayoutIndex) ?: presets[0]
                                    saveCollageToDevice(
                                        context = context,
                                        slotStates = slotStates,
                                        layout = currentLayout,
                                        style = selectedStyle,
                                        ratio = ratioValue,
                                        innerGapDp = innerGap,
                                        outerMarginDp = outerMargin,
                                        cornerRadiusDp = cornerRadius,
                                        borderWidthDp = borderWidth,
                                        background = selectedBackground
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
                        enabled = !isSaving,
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
                                contentDescription = "Save",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // ── Collage Canvas Stage ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                        Text(
                            text = "Rendering studio collage…",
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
                        val parentRatio = parentWidth.value / parentHeight.value.coerceAtLeast(1f)
                        val (canvasWidth, canvasHeight) = if (parentRatio > animatedRatio) {
                            Pair(parentHeight * animatedRatio, parentHeight)
                        } else {
                            Pair(parentWidth, parentWidth / animatedRatio)
                        }

                        val safeMargin = animatedMargin.coerceAtLeast(0.dp)
                        val safeGap = animatedGap.coerceAtLeast(0.dp)
                        val safeCorner = animatedCorner.coerceAtLeast(0.dp)

                        Box(
                            modifier = Modifier
                                .size(width = canvasWidth, height = canvasHeight)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), clip = false)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            // Backdrop Layer (Solid / Gradient / Pattern / 35mm Film Sprockets)
                            CollageBackdropCanvas(
                                background = selectedBackground,
                                isFilmStyle = selectedStyle == CollageStyle.FILM,
                                modifier = Modifier.fillMaxSize()
                            )

                            val currentLayout = presets.getOrNull(safeLayoutIndex) ?: presets.firstOrNull()
                            val extraFilmPad = if (selectedStyle == CollageStyle.FILM) 12.dp else 0.dp
                            if (currentLayout != null) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = safeMargin,
                                            end = safeMargin,
                                            top = safeMargin + extraFilmPad,
                                            bottom = safeMargin + extraFilmPad
                                        )
                                ) {
                                    val innerWidth = maxWidth
                                    val innerHeight = maxHeight
                                    val density = androidx.compose.ui.platform.LocalDensity.current
                                    val cornerPx = with(density) { safeCorner.toPx() }
                                    val slotShape = remember(selectedStyle, cornerPx) {
                                        buildSlotShape(selectedStyle, cornerPx)
                                    }

                                    currentLayout.bounds.forEachIndexed { index, rect ->
                                        val slot = slotStates.getOrNull(index) ?: return@forEachIndexed
                                        val left = innerWidth * rect.left
                                        val top = innerHeight * rect.top
                                        val width = innerWidth * (rect.right - rect.left)
                                        val height = innerHeight * (rect.bottom - rect.top)
                                        val isSlotSelected = selectedSlotIndex == index
                                        val isSwapSource = swapSourceIndex == index
                                        val tiltDeg = if (selectedStyle == CollageStyle.SCRAPBOOK) scrapbookTiltForIndex(index) else 0f
                                        val isPolaroid = selectedStyle == CollageStyle.POLAROID

                                        Box(
                                            modifier = Modifier
                                                .offset(x = left, y = top)
                                                .size(width = width, height = height)
                                                .padding(safeGap / 2)
                                                .graphicsLayer { rotationZ = tiltDeg }
                                                .then(
                                                    if (isPolaroid || selectedStyle == CollageStyle.SCRAPBOOK) {
                                                        Modifier
                                                            .shadow(4.dp, RoundedCornerShape(safeCorner), clip = false)
                                                            .background(Color.White, RoundedCornerShape(safeCorner))
                                                            .padding(
                                                                start = borderWidth.coerceAtLeast(3f).dp,
                                                                top = borderWidth.coerceAtLeast(3f).dp,
                                                                end = borderWidth.coerceAtLeast(3f).dp,
                                                                bottom = if (isPolaroid) (borderWidth.coerceAtLeast(4f) * 2.8f).dp else borderWidth.coerceAtLeast(3f).dp
                                                            )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clip(slotShape)
                                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                                .then(
                                                    if (!isPolaroid && selectedStyle != CollageStyle.SCRAPBOOK && borderWidth > 0.2f) {
                                                        Modifier.border(borderWidth.dp, Color.White, slotShape)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .border(
                                                    width = when {
                                                        isSwapSource -> 3.dp
                                                        isSlotSelected -> 2.5.dp
                                                        else -> 0.dp
                                                    },
                                                    color = when {
                                                        isSwapSource -> MaterialTheme.colorScheme.tertiary
                                                        isSlotSelected -> MaterialTheme.colorScheme.primary
                                                        else -> Color.Transparent
                                                    },
                                                    shape = slotShape
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (swapSourceIndex != -1) {
                                                        if (swapSourceIndex != index) {
                                                            pushUndo()
                                                            val updated = slotStates.toMutableList()
                                                            val a = updated[swapSourceIndex]
                                                            updated[swapSourceIndex] = updated[index]
                                                            updated[index] = a
                                                            slotStates = updated
                                                            selectedSlotIndex = index
                                                        }
                                                        swapSourceIndex = -1
                                                    } else {
                                                        selectedSlotIndex = if (isSlotSelected) -1 else index
                                                    }
                                                }
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(slot.uri)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = if (slot.isCropFill) ContentScale.Crop else ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .run {
                                                        if (isSlotSelected && swapSourceIndex == -1) {
                                                            pointerInput(index, slot.uri) {
                                                                detectTransformGestures { _, pan, zoom, _ ->
                                                                    val updated = slotStates.toMutableList()
                                                                    val curr = updated.getOrNull(index) ?: return@detectTransformGestures
                                                                    val newScale = (curr.scale * zoom).coerceIn(1f, 8f)
                                                                    val newOffsetX = curr.offsetX + (pan.x / size.width.toFloat().coerceAtLeast(1f))
                                                                    val newOffsetY = curr.offsetY + (pan.y / size.height.toFloat().coerceAtLeast(1f))
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
                    }

                    // ── Swap Mode Prompt Pill ─────────────────────────────────
                    androidx.compose.animation.AnimatedVisibility(
                        visible = swapSourceIndex != -1,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut() + slideOutVertically { -it / 2 },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_swap_horiz),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Tap another photo to swap",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                IconButton(onClick = { swapSourceIndex = -1 }, modifier = Modifier.size(28.dp)) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_ms_close), contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // ── Floating Slot Action Pill ─────────────────────────────
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedSlotIndex in slotStates.indices && swapSourceIndex == -1,
                        enter = fadeIn(MotionTokens.snappySpring()) + slideInVertically(MotionTokens.snappySpring()) { it / 2 },
                        exit = fadeOut(MotionTokens.snappySpring()) + slideOutVertically(MotionTokens.snappySpring()) { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        val activeSlot = slotStates.getOrNull(selectedSlotIndex)
                        if (activeSlot != null) {
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
                                            val updated = slotStates.toMutableList()
                                            updated[selectedSlotIndex] = activeSlot.copy(rotation = (activeSlot.rotation + 90) % 360)
                                            slotStates = updated
                                        }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_rotate_right), contentDescription = "Rotate", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pushUndo()
                                            val updated = slotStates.toMutableList()
                                            updated[selectedSlotIndex] = activeSlot.copy(flipHorizontal = !activeSlot.flipHorizontal)
                                            slotStates = updated
                                        }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_flip), contentDescription = "Flip", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pushUndo()
                                            val updated = slotStates.toMutableList()
                                            updated[selectedSlotIndex] = activeSlot.copy(isCropFill = !activeSlot.isCropFill, scale = 1f, offsetX = 0f, offsetY = 0f)
                                            slotStates = updated
                                        }) {
                                            Icon(
                                                painter = painterResource(if (activeSlot.isCropFill) R.drawable.ic_ms_thin_crop else R.drawable.ic_ms_thin_open_in_full),
                                                contentDescription = "Fit or Fill",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (slotStates.size > 1) {
                                        item {
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                swapSourceIndex = selectedSlotIndex
                                            }) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_ms_thin_unfold_more),
                                                    contentDescription = "Swap",
                                                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 90f }
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            pushUndo()
                                            val updated = slotStates.toMutableList()
                                            updated[selectedSlotIndex] = activeSlot.copy(scale = 1f, offsetX = 0f, offsetY = 0f)
                                            slotStates = updated
                                        }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_refresh), contentDescription = "Reset Pan/Zoom", modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    item {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            chooserMode = ChooserMode.REPLACE_SLOT
                                        }) {
                                            Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = "Replace", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (slotStates.size > 2) {
                                        item {
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pushUndo()
                                                val updated = slotStates.toMutableList()
                                                updated.removeAt(selectedSlotIndex)
                                                slotStates = updated
                                                selectedSlotIndex = -1
                                                selectedLayoutIndex = 0
                                            }) {
                                                Icon(painter = painterResource(R.drawable.ic_ms_thin_delete), contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    item {
                                        IconButton(onClick = { selectedSlotIndex = -1 }) {
                                            Icon(painter = painterResource(R.drawable.ic_ms_thin_close), contentDescription = "Done", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Studio Tool Deck + Google Photos Pitch-Black Tab Dock ──
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
                    label = "collageStudioDeck"
                ) { tab ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (tab) {
                            CollageEditTab.STYLE -> {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(CollageStyle.entries) { style ->
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
                                                StyleMiniPreview(style = style, isSelected = isSelected, modifier = Modifier.size(44.dp))
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

                            CollageEditTab.LAYOUT -> {
                                LazyRow(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(presets) { idx, layout ->
                                        val isSelected = safeLayoutIndex == idx
                                        val bg by animateColorAsState(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            label = "layoutTileBg"
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (safeLayoutIndex != idx) {
                                                    pushUndo()
                                                    selectedLayoutIndex = idx
                                                }
                                                selectedSlotIndex = -1
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(66.dp)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(bg)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                        shape = RoundedCornerShape(18.dp)
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
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            CollageEditTab.FRAME -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Parameter Sub-Tool Pills
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FrameParameter.entries.forEach { param ->
                                            val isSelected = activeFrameParam == param
                                            val curVal = when (param) {
                                                FrameParameter.GAP -> innerGap
                                                FrameParameter.MARGIN -> outerMargin
                                                FrameParameter.CORNERS -> cornerRadius
                                                FrameParameter.BORDER -> borderWidth
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
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                                        text = "${curVal.roundToInt()}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Signature Tactile Ruler Wheel Slider (from PhotonEditorScreen)
                                    val activeValue = when (activeFrameParam) {
                                        FrameParameter.GAP -> innerGap
                                        FrameParameter.MARGIN -> outerMargin
                                        FrameParameter.CORNERS -> cornerRadius
                                        FrameParameter.BORDER -> borderWidth
                                    }
                                    EditorRulerSlider(
                                        value = activeValue,
                                        onValueChange = { v ->
                                            when (activeFrameParam) {
                                                FrameParameter.GAP -> innerGap = v
                                                FrameParameter.MARGIN -> outerMargin = v
                                                FrameParameter.CORNERS -> cornerRadius = v
                                                FrameParameter.BORDER -> borderWidth = v
                                            }
                                        },
                                        range = activeFrameParam.min..activeFrameParam.max,
                                        step = 1f,
                                        defaultValue = activeFrameParam.defaultVal,
                                        valueFormatter = { "${it.roundToInt()}dp" },
                                        showDoneButton = false
                                    )
                                }
                            }

                            CollageEditTab.BACKGROUND -> {
                                val bgOptions = listOf(
                                    CollageBackground.Solid(Color(0xFFF6F2EA), "Paper"),
                                    CollageBackground.Solid(Color.White, "White"),
                                    CollageBackground.Solid(Color(0xFF101113), "Cinema"),
                                    CollageBackground.Solid(Color.Black, "Black"),
                                    CollageBackground.Pattern(Color(0xFFF7F3EB), Color(0x28000000), false, "Dots"),
                                    CollageBackground.Pattern(Color(0xFF181A1F), Color(0x28FFFFFF), true, "Blueprint"),
                                    CollageBackground.Solid(MaterialTheme.colorScheme.primaryContainer, "Primary"),
                                    CollageBackground.Solid(MaterialTheme.colorScheme.secondaryContainer, "Tonal"),
                                    CollageBackground.Solid(MaterialTheme.colorScheme.tertiaryContainer, "Accent"),
                                    CollageBackground.Gradient(Color(0xFFFF8A65), Color(0xFFFFD54F), "Sunset"),
                                    CollageBackground.Gradient(Color(0xFF4FC3F7), Color(0xFF81C784), "Lagoon"),
                                    CollageBackground.Gradient(Color(0xFFCE93D8), Color(0xFF90CAF9), "Aurora"),
                                    CollageBackground.Gradient(Color(0xFF1A237E), Color(0xFF4A148C), "Cosmic"),
                                    CollageBackground.Gradient(Color(0xFF2E7D32), Color(0xFF80CBC4), "Moss")
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
                                                CollageBackdropCanvas(background = bg, isFilmStyle = false, modifier = Modifier.fillMaxSize())
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

                            CollageEditTab.RATIO -> {
                                val ratios = listOf(
                                    "1:1" to 1f,
                                    "4:5" to 4f / 5f,
                                    "4:3" to 4f / 3f,
                                    "3:4" to 3f / 4f,
                                    "3:2" to 3f / 2f,
                                    "2:3" to 2f / 3f,
                                    "16:9" to 16f / 9f,
                                    "9:16" to 9f / 16f
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(ratios) { _, (label, value) ->
                                        val isSelected = selectedRatioLabel == label
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedRatioLabel = label
                                                ratioValue = value
                                            }
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                val boxW = if (value >= 1f) 22.dp else (22f * value).coerceAtLeast(10f).dp
                                                val boxH = if (value <= 1f) 22.dp else (22f / value).coerceAtLeast(10f).dp
                                                Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = boxW, height = boxH)
                                                            .border(
                                                                width = 1.6.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                shape = RoundedCornerShape(3.dp)
                                                            )
                                                    )
                                                }
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
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
                        items(CollageEditTab.entries) { tab ->
                            val isSelected = tab == activeTab
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF1C191D) else Color.Transparent,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "collageTabBg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "collageTabBorder"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "collageTabFg"
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
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
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

        // ── Local Gallery Chooser overlay ────────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = chooserMode != null,
            enter = fadeIn(MotionTokens.gentleSpring()) + slideInVertically(MotionTokens.gentleSpring()) { it },
            exit = fadeOut(MotionTokens.gentleSpring()) + slideOutVertically(MotionTokens.gentleSpring()) { it }
        ) {
            val isAddMode = chooserMode == ChooserMode.ADD_PHOTOS
            val maxPick = if (isAddMode) (9 - slotStates.size).coerceAtLeast(1) else slotStates.size
            LocalGalleryChooser(
                mediaList = imageMedia,
                maxSelection = maxPick,
                confirmLabelPrefix = if (isAddMode) "Add" else "Replace",
                onDismiss = { chooserMode = null },
                onConfirm = { chosenUris: List<Uri> ->
                    pushUndo()
                    if (isAddMode) {
                        slotStates = slotStates + chosenUris.map { SlotState(uri = it) }
                        selectedLayoutIndex = 0
                    } else {
                        val updated = slotStates.toMutableList()
                        chosenUris.forEachIndexed { index: Int, uri: Uri ->
                            val targetIndex = (selectedSlotIndex.coerceAtLeast(0) + index) % updated.size
                            updated[targetIndex] = SlotState(uri = uri)
                        }
                        slotStates = updated
                    }
                    selectedSlotIndex = -1
                    chooserMode = null
                }
            )
        }
    }
    }
}

// ── Backdrop & Style Mini Previews ───────────────────────────────────────────

@Composable
private fun CollageBackdropCanvas(
    background: CollageBackground,
    isFilmStyle: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (background) {
            is CollageBackground.Solid -> drawRect(color = background.color)
            is CollageBackground.Gradient -> drawRect(
                brush = Brush.linearGradient(colors = listOf(background.start, background.end))
            )
            is CollageBackground.Pattern -> {
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
            val holeW = 8.dp.toPx()
            val holeH = 5.dp.toPx()
            val stepX = 16.dp.toPx()
            val topY = 5.dp.toPx()
            val botY = h - 5.dp.toPx() - holeH
            var x = 10.dp.toPx()
            while (x + holeW < w - 6.dp.toPx()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.82f),
                    topLeft = Offset(x, topY),
                    size = Size(holeW, holeH),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.82f),
                    topLeft = Offset(x, botY),
                    size = Size(holeW, holeH),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
                x += stepX
            }
        }
    }
}

@Composable
private fun StyleMiniPreview(
    style: CollageStyle,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val fg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
        when (style) {
            CollageStyle.MODERN -> {
                drawRoundRect(fg, Offset(2f, 2f), Size(w * 0.48f, h - 4f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.56f, 2f), Size(w * 0.42f, h * 0.44f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.56f, h * 0.54f), Size(w * 0.42f, h * 0.44f), CornerRadius(6f, 6f), style = stroke)
            }
            CollageStyle.POLAROID -> {
                drawRoundRect(fg, Offset(w * 0.14f, 2f), Size(w * 0.72f, h - 4f), CornerRadius(5f, 5f), style = stroke)
                drawRect(fg.copy(alpha = 0.35f), Offset(w * 0.22f, h * 0.14f), Size(w * 0.56f, h * 0.54f))
            }
            CollageStyle.FILM -> {
                drawRoundRect(fg, Offset(2f, 4f), Size(w - 4f, h - 8f), CornerRadius(4f, 4f), style = stroke)
                for (i in 0..3) {
                    val x = 6f + i * ((w - 16f) / 3.5f)
                    drawCircle(fg, radius = 2f, center = Offset(x, 9f))
                    drawCircle(fg, radius = 2f, center = Offset(x, h - 9f))
                }
            }
            CollageStyle.SCRAPBOOK -> {
                drawRoundRect(fg.copy(alpha = 0.6f), Offset(4f, 6f), Size(w * 0.55f, h * 0.65f), CornerRadius(5f, 5f), style = stroke)
                drawRoundRect(fg, Offset(w * 0.36f, h * 0.24f), Size(w * 0.56f, h * 0.65f), CornerRadius(5f, 5f), style = stroke)
            }
            CollageStyle.ARCH -> {
                val r = w * 0.34f
                drawRoundRect(fg, Offset(w * 0.16f, 2f), Size(w * 0.68f, h - 4f), CornerRadius(r, r), style = stroke)
            }
            CollageStyle.CAPSULE -> {
                val r = w * 0.5f
                drawCircle(fg, radius = min(w, h) * 0.28f, center = Offset(w * 0.34f, h * 0.5f), style = stroke)
                drawCircle(fg, radius = min(w, h) * 0.28f, center = Offset(w * 0.68f, h * 0.5f), style = stroke)
            }
            CollageStyle.DIAMOND -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, 3f)
                    lineTo(w - 3f, h * 0.5f)
                    lineTo(w * 0.5f, h - 3f)
                    lineTo(3f, h * 0.5f)
                    close()
                }
                drawPath(path, fg, style = stroke)
            }
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
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val w = size.width
        val h = size.height
        val gap = 1.5.dp.toPx()
        layout.bounds.forEach { rect ->
            val l = rect.left * w + gap / 2f
            val t = rect.top * h + gap / 2f
            val rw = ((rect.right - rect.left) * w - gap).coerceAtLeast(2f)
            val rh = ((rect.bottom - rect.top) * h - gap).coerceAtLeast(2f)
            drawRoundRect(
                color = color,
                topLeft = Offset(l, t),
                size = Size(rw, rh),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
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
    confirmLabelPrefix: String = "Replace",
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
                            maxLines = 1
                        )
                        Text(
                            text = "Select up to $maxSelection photo${if (maxSelection > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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
                        "$confirmLabelPrefix (${selectedUris.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }

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
                                            maxLines = 1
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

// ── High-Res Studio Export Logic ──────────────────────────────────────────────

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
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val exifMatrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> exifMatrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> exifMatrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> exifMatrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> exifMatrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> exifMatrix.postScale(1f, -1f)
            else -> return raw
        }
        val oriented = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, exifMatrix, true)
        if (oriented != raw) raw.recycle()
        oriented
    } catch (e: Exception) {
        null
    }
}

private suspend fun saveCollageToDevice(
    context: Context,
    slotStates: List<SlotState>,
    layout: CollageLayout,
    style: CollageStyle,
    ratio: Float,
    innerGapDp: Float,
    outerMarginDp: Float,
    cornerRadiusDp: Float,
    borderWidthDp: Float,
    background: CollageBackground
) = withContext(Dispatchers.IO) {
    val scaleFactor = 4f
    val baseWidth = (600 * scaleFactor).roundToInt()
    val baseHeight = (baseWidth / ratio).roundToInt()
    val innerGap = innerGapDp * scaleFactor
    val outerMargin = outerMarginDp * scaleFactor
    val cornerRadius = cornerRadiusDp * scaleFactor
    val borderWidth = borderWidthDp * scaleFactor

    val bitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 1. Draw Backdrop
    when (background) {
        is CollageBackground.Solid -> canvas.drawColor(background.color.toArgb())
        is CollageBackground.Gradient -> {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, baseWidth.toFloat(), baseHeight.toFloat(),
                    background.start.toArgb(), background.end.toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, baseWidth.toFloat(), baseHeight.toFloat(), bgPaint)
        }
        is CollageBackground.Pattern -> {
            canvas.drawColor(background.baseColor.toArgb())
            val patPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = background.dotColor.toArgb()
                strokeWidth = 1f * scaleFactor
            }
            val step = 16f * scaleFactor
            if (background.isGrid) {
                var x = step
                while (x < baseWidth) {
                    canvas.drawLine(x, 0f, x, baseHeight.toFloat(), patPaint)
                    x += step
                }
                var y = step
                while (y < baseHeight) {
                    canvas.drawLine(0f, y, baseWidth.toFloat(), y, patPaint)
                    y += step
                }
            } else {
                var x = step / 2f
                while (x < baseWidth) {
                    var y = step / 2f
                    while (y < baseHeight) {
                        canvas.drawCircle(x, y, 1.6f * scaleFactor, patPaint)
                        y += step
                    }
                    x += step
                }
            }
        }
    }

    // 2. Film sprockets if 35mm Film style
    val extraFilmPad = if (style == CollageStyle.FILM) 12f * scaleFactor else 0f
    if (style == CollageStyle.FILM) {
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(210, 255, 255, 255)
        }
        val holeW = 8f * scaleFactor
        val holeH = 5f * scaleFactor
        val stepX = 16f * scaleFactor
        val topY = 5f * scaleFactor
        val botY = baseHeight - 5f * scaleFactor - holeH
        var x = 10f * scaleFactor
        while (x + holeW < baseWidth - 6f * scaleFactor) {
            canvas.drawRoundRect(RectF(x, topY, x + holeW, topY + holeH), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
            canvas.drawRoundRect(RectF(x, botY, x + holeW, botY + holeH), 1.5f * scaleFactor, 1.5f * scaleFactor, holePaint)
            x += stepX
        }
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val contentLeft = outerMargin
    val contentTop = outerMargin + extraFilmPad
    val contentWidth = (baseWidth - outerMargin * 2f).coerceAtLeast(1f)
    val contentHeight = (baseHeight - (outerMargin + extraFilmPad) * 2f).coerceAtLeast(1f)

    layout.bounds.forEachIndexed { index, rect ->
        val slot = slotStates.getOrNull(index) ?: return@forEachIndexed
        val outerL = contentLeft + rect.left * contentWidth + innerGap / 2f
        val outerT = contentTop + rect.top * contentHeight + innerGap / 2f
        val outerR = contentLeft + rect.right * contentWidth - innerGap / 2f
        val outerB = contentTop + rect.bottom * contentHeight - innerGap / 2f
        if (outerR <= outerL || outerB <= outerT) return@forEachIndexed

        canvas.save()
        if (style == CollageStyle.SCRAPBOOK) {
            canvas.rotate(scrapbookTiltForIndex(index), (outerL + outerR) / 2f, (outerT + outerB) / 2f)
        }

        val isPolaroid = style == CollageStyle.POLAROID
        val isScrapbook = style == CollageStyle.SCRAPBOOK
        val photoRect = if (isPolaroid || isScrapbook) {
            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                setShadowLayer(4f * scaleFactor, 0f, 2f * scaleFactor, android.graphics.Color.argb(65, 0, 0, 0))
            }
            canvas.drawRoundRect(RectF(outerL, outerT, outerR, outerB), cornerRadius, cornerRadius, framePaint)
            val pad = borderWidth.coerceAtLeast(3f * scaleFactor)
            val botPad = if (isPolaroid) borderWidth.coerceAtLeast(4f * scaleFactor) * 2.8f else pad
            RectF(outerL + pad, outerT + pad, outerR - pad, outerB - botPad)
        } else {
            RectF(outerL, outerT, outerR, outerB)
        }

        val w = photoRect.width()
        val h = photoRect.height()
        if (w <= 0f || h <= 0f) {
            canvas.restore()
            return@forEachIndexed
        }

        val slotPath = buildAndroidSlotPath(style, photoRect, cornerRadius)
        canvas.save()
        canvas.clipPath(slotPath)

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
            val scale = if (slot.isCropFill) maxOf(w / srcW, h / srcH) else minOf(w / srcW, h / srcH)
            val dx = (w - srcW * scale) / 2f
            val dy = (h - srcH * scale) / 2f
            val drawMatrix = Matrix()
            drawMatrix.postScale(scale, scale)
            drawMatrix.postTranslate(dx, dy)
            drawMatrix.postScale(slot.scale, slot.scale, w / 2f, h / 2f)
            drawMatrix.postTranslate(slot.offsetX * w, slot.offsetY * h)
            drawMatrix.postTranslate(photoRect.left, photoRect.top)
            canvas.drawBitmap(transformed, drawMatrix, paint)
            if (transformed != decoded) transformed.recycle()
            decoded.recycle()
        }
        canvas.restore()

        if (!isPolaroid && !isScrapbook && borderWidth > 0.5f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = android.graphics.Color.WHITE
            }
            canvas.drawPath(slotPath, strokePaint)
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
