package com.inferno.gallery.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.inferno.gallery.ui.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import kotlin.math.min

class TileCache(maxSizeKb: Int = 32 * 1024) : LruCache<String, Bitmap>(maxSizeKb) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount / 1024
    }

    override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
        if (evicted && !oldValue.isRecycled) {
            oldValue.recycle()
        }
    }
}

class DeepZoomController(
    private val context: Context,
    private val item: GalleryItem
) {
    var decoder: BitmapRegionDecoder? = null
        private set

    var imageWidth: Int = 0
        private set
    var imageHeight: Int = 0
        private set

    var isEligible: Boolean = false
        private set

    var rotationDegrees: Int = 0
        private set

    private val decoderLock = Any()
    val tileCache = TileCache()
    val activeJobKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext false

        try {
            val file = if (item.path.isNotBlank()) File(item.path) else null
            val createdDecoder = if (file != null && file.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(file.absolutePath)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(file.absolutePath, false)
                }
            } else {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        BitmapRegionDecoder.newInstance(stream)
                    } else {
                        @Suppress("DEPRECATION")
                        BitmapRegionDecoder.newInstance(stream, false)
                    }
                }
            }

            val exifOrientation = try {
                if (file != null && file.exists()) {
                    val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
                    exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
                } else {
                    context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        val exif = androidx.exifinterface.media.ExifInterface(stream)
                        exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
                    } ?: androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                }
            } catch (e: Exception) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            }

            rotationDegrees = when (exifOrientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (createdDecoder != null) {
                decoder = createdDecoder
                if (rotationDegrees == 90 || rotationDegrees == 270) {
                    imageWidth = createdDecoder.height
                    imageHeight = createdDecoder.width
                } else {
                    imageWidth = createdDecoder.width
                    imageHeight = createdDecoder.height
                }
                val totalPixels = imageWidth.toLong() * imageHeight.toLong()
                // Activate for photos larger than 2048px in either dimension or > 6 megapixels with 0 rotation
                isEligible = (imageWidth > 2048 || imageHeight > 2048 || totalPixels > 6_000_000L) && rotationDegrees == 0
                return@withContext isEligible
            }
        } catch (e: Exception) {
            android.util.Log.e("DeepZoomController", "Failed to create region decoder for ${item.name}", e)
        }
        return@withContext false
    }

    fun decodeTile(
        key: String,
        rect: Rect,
        sampleSize: Int,
        onDecoded: (Bitmap) -> Unit
    ) {
        val cached = tileCache.get(key)
        if (cached != null && !cached.isRecycled) {
            onDecoded(cached)
            return
        }

        synchronized(decoderLock) {
            val active = decoder ?: return
            if (active.isRecycled) return

            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = active.decodeRegion(rect, options)
                if (bitmap != null) {
                    tileCache.put(key, bitmap)
                    onDecoded(bitmap)
                }
            } catch (e: Exception) {
                android.util.Log.e("DeepZoomController", "Error decoding tile $key", e)
            }
        }
    }

    fun release() {
        activeJobKeys.clear()
        synchronized(decoderLock) {
            decoder?.recycle()
            decoder = null
        }
        tileCache.evictAll()
    }
}

@Composable
fun TileBasedDeepZoomOverlay(
    item: GalleryItem,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    pagerScale: Float = 1f,
    pagerAlpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    if (item.isVideo || scale <= 1.25f) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val controller = remember(item.id) { DeepZoomController(context, item) }
    var isReady by remember { mutableStateOf(false) }

    // Map of loaded tile bitmaps: key -> Bitmap
    val loadedTiles = remember { mutableStateMapOf<String, Bitmap>() }
    // Revision counter to trigger canvas redraws when tiles arrive
    var redrawTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.id) {
        val initialized = controller.initialize()
        isReady = initialized
    }

    DisposableEffect(item.id) {
        onDispose {
            controller.release()
            loadedTiles.clear()
        }
    }

    if (!isReady || controller.decoder == null || !controller.isEligible) {
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale * pagerScale
                scaleY = scale * pagerScale
                alpha = pagerAlpha
                translationX = offsetX
                translationY = offsetY
            }
    ) {
        // Read the trigger so canvas recomposes when new tiles finish decoding
        @Suppress("UNUSED_VARIABLE")
        val trigger = redrawTrigger

        val composableW = size.width
        val composableH = size.height
        val imgW = controller.imageWidth
        val imgH = controller.imageHeight

        if (composableW <= 0f || composableH <= 0f || imgW <= 0 || imgH <= 0) return@Canvas

        val fitScale = min(composableW / imgW.toFloat(), composableH / imgH.toFloat())
        val fitW = imgW * fitScale
        val fitH = imgH * fitScale
        val fitLeft = (composableW - fitW) / 2f
        val fitTop = (composableH - fitH) / 2f

        // Visible bounds in unscaled composable coordinates
        val visibleLeft = (0f - offsetX - composableW / 2f) / scale + composableW / 2f
        val visibleTop = (0f - offsetY - composableH / 2f) / scale + composableH / 2f
        val visibleRight = (composableW - offsetX - composableW / 2f) / scale + composableW / 2f
        val visibleBottom = (composableH - offsetY - composableH / 2f) / scale + composableH / 2f

        // Convert to raw image pixel coordinates
        val rawVisibleLeft = ((visibleLeft - fitLeft) / fitScale).toInt().coerceIn(0, imgW)
        val rawVisibleTop = ((visibleTop - fitTop) / fitScale).toInt().coerceIn(0, imgH)
        val rawVisibleRight = ((visibleRight - fitLeft) / fitScale).toInt().coerceIn(0, imgW)
        val rawVisibleBottom = ((visibleBottom - fitTop) / fitScale).toInt().coerceIn(0, imgH)

        if (rawVisibleRight <= rawVisibleLeft || rawVisibleBottom <= rawVisibleTop) return@Canvas

        // Determine inSampleSize based on current display density
        val effectiveScale = fitScale * scale
        val sampleSize = when {
            effectiveScale >= 0.70f -> 1
            effectiveScale >= 0.35f -> 2
            effectiveScale >= 0.18f -> 4
            else -> 8
        }

        val rawTileSize = 1024
        val startCol = rawVisibleLeft / rawTileSize
        val endCol = rawVisibleRight / rawTileSize
        val startRow = rawVisibleTop / rawTileSize
        val endRow = rawVisibleBottom / rawTileSize

        val currentVisibleKeys = mutableSetOf<String>()

        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                val tileLeft = col * rawTileSize
                val tileTop = row * rawTileSize
                val tileRight = min(tileLeft + rawTileSize, imgW)
                val tileBottom = min(tileTop + rawTileSize, imgH)

                if (tileRight <= tileLeft || tileBottom <= tileTop) continue

                val tileKey = "${item.id}_${col}_${row}_$sampleSize"
                currentVisibleKeys.add(tileKey)

                val cachedBitmap = controller.tileCache.get(tileKey)
                if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                    val drawLeft = fitLeft + tileLeft * fitScale
                    val drawTop = fitTop + tileTop * fitScale
                    val drawWidth = (tileRight - tileLeft) * fitScale
                    val drawHeight = (tileBottom - tileTop) * fitScale

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawBitmap(
                            cachedBitmap,
                            null,
                            android.graphics.RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight),
                            null
                        )
                    }
                } else {
                    // Queue tile for background decoding (deduplicated)
                    if (controller.activeJobKeys.add(tileKey)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val rect = Rect(tileLeft, tileTop, tileRight, tileBottom)
                                controller.decodeTile(tileKey, rect, sampleSize) { bitmap ->
                                    loadedTiles[tileKey] = bitmap
                                    redrawTrigger++
                                }
                            } finally {
                                controller.activeJobKeys.remove(tileKey)
                            }
                        }
                    }
                }
            }
        }
    }
}
