package com.inferno.gallery.ui.components

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated high-performance hardware-accelerated in-memory thumbnail cache for Photon Gallery.
 * Sized dynamically at 25% of runtime maximum heap to prevent GC thrashing during high-speed flings.
 */
object PhotonThumbnailCache {
    private val maxCacheKilobytes = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt()

    private val memoryLruCache = object : LruCache<String, Bitmap>(maxCacheKilobytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    /**
     * Synchronous memory-hit lookup. Returns immediately on frame 0 during composition.
     */
    fun get(mediaId: String): Bitmap? = memoryLruCache.get(mediaId)

    /**
     * Stores a prepared bitmap into the in-memory cache.
     */
    fun put(mediaId: String, bitmap: Bitmap) {
        memoryLruCache.put(mediaId, bitmap)
    }

    /**
     * Clears the thumbnail cache.
     */
    fun clear() {
        memoryLruCache.evictAll()
    }
}

/**
 * Shared thumbnail resolution buckets, derived from column count. Used by both
 * visible-cell loading and scroll-ahead prefetching so cache keys always match.
 */
fun thumbnailTargetPx(gridCellsCount: Int): Int = when {
    gridCellsCount <= 2 -> 512
    gridCellsCount == 3 -> 320
    gridCellsCount == 4 -> 240
    gridCellsCount <= 6 -> 160
    else -> 128
}

/**
 * Bounded decode lanes. Visible-cell loads get priority bandwidth; prefetches can never
 * starve them because they share neither thread quota nor queue ordering.
 */
private val cellDecodeDispatcher = Dispatchers.IO.limitedParallelism(4)
private val prefetchDispatcher = Dispatchers.IO.limitedParallelism(2)

/**
 * Coalesces concurrent requests for the same media+size into a single decode.
 * Prevents the classic fling pathology where a cell and the prefetcher decode the
 * same item twice, doubling CPU/GPU work and racing cache writes.
 */
private val inFlightLoads = ConcurrentHashMap<String, CompletableDeferred<Bitmap?>>()

/**
 * Loads a media thumbnail using Android's native hardware-accelerated thumbnail provider,
 * with fallbacks to file-based ThumbnailUtils, sampled BitmapFactory decoding, and video frame extraction.
 *
 * Guarantees:
 *  - At most [cellDecodeDispatcher] parallel decodes for visible cells, [prefetchDispatcher] for prefetches.
 *  - One decode per (mediaId, size) pair at any instant — duplicates coalesce via [inFlightLoads].
 *  - Cancelled callers abort between fallback strategies instead of finishing dead work.
 *  - Provider bitmaps larger than requested are downsampled to exact target before GPU upload.
 *
 * Asynchronously pre-uploads texture pixels to the GPU via [Bitmap.prepareToDraw].
 */
suspend fun loadNativeMediaThumbnail(
    context: Context,
    mediaUri: Uri,
    filePath: String? = null,
    mediaId: String,
    isVideo: Boolean,
    targetDimensionPx: Int = 256,
    forPrefetch: Boolean = false
): Bitmap? {
    // 1. Synchronous memory hit — resolves without touching any dispatcher
    PhotonThumbnailCache.get(mediaId)?.let { return it }

    val key = "${mediaId}@$targetDimensionPx"
    while (true) {
        val waiter = CompletableDeferred<Bitmap?>()
        val existing = inFlightLoads.putIfAbsent(key, waiter)
        if (existing != null) {
            // Another caller is already decoding this exact asset — ride along
            val result = runCatching { existing.await() }.getOrNull()
            currentCoroutineContext().ensureActive()
            PhotonThumbnailCache.get(mediaId)?.let { return it }
            if (result != null) return result
            continue // winner failed or was cancelled — become the new winner
        }

        return try {
            val dispatcher = if (forPrefetch) prefetchDispatcher else cellDecodeDispatcher
            val bitmap = withContext(dispatcher) {
                decodeMediaThumbnail(context, mediaUri, filePath, mediaId, isVideo, targetDimensionPx)
            }
            waiter.complete(bitmap)
            bitmap
        } catch (t: Throwable) {
            // Release co-waiters so they retry independently; rethrow preserves cancellation
            waiter.complete(null)
            throw t
        } finally {
            inFlightLoads.remove(key, waiter)
        }
    }
}

private suspend fun decodeMediaThumbnail(
    context: Context,
    mediaUri: Uri,
    filePath: String?,
    mediaId: String,
    isVideo: Boolean,
    targetDimensionPx: Int
): Bitmap? {
    val contentResolver: ContentResolver = context.contentResolver
    val requestSize = Size(targetDimensionPx, targetDimensionPx)
    // Dense grids (5-8 columns, ≤160px tiles): RGB_565 halves decode memory, GPU texture
    // upload and LRU footprint per cell. Alpha is irrelevant for opaque grid thumbnails.
    val preferRgb565 = targetDimensionPx <= 160

    // Ensure we have a valid MediaStore Content URI if available
    val contentUri: Uri = if (mediaUri.scheme == ContentResolver.SCHEME_CONTENT) {
        mediaUri
    } else {
        val numericId = mediaId.toLongOrNull() ?: -1L
        if (numericId > 0L) {
            ContentUris.withAppendedId(
                if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                numericId
            )
        } else {
            mediaUri
        }
    }

    var loadedBitmap: Bitmap? = null

    // Strategy 1: Native ContentResolver.loadThumbnail on Android Q+ (API 29+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && contentUri.scheme == ContentResolver.SCHEME_CONTENT) {
        try {
            loadedBitmap = rightSizeToTarget(
                contentResolver.loadThumbnail(contentUri, requestSize, null),
                targetDimensionPx,
                preferRgb565
            )
        } catch (_: Throwable) {
            // Proceed to next fallback strategy
        }
    }
    currentCoroutineContext().ensureActive()

    // Strategy 2: File-based ThumbnailUtils on Android Q+ (API 29+)
    if (loadedBitmap == null && !filePath.isNullOrEmpty()) {
        try {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    loadedBitmap = rightSizeToTarget(
                        if (isVideo) {
                            ThumbnailUtils.createVideoThumbnail(file, requestSize, null)
                        } else {
                            ThumbnailUtils.createImageThumbnail(file, requestSize, null)
                        },
                        targetDimensionPx,
                        preferRgb565
                    )
                }
            }
        } catch (_: Throwable) {
        }
    }
    currentCoroutineContext().ensureActive()

    // Strategy 3: Sampled BitmapFactory decoding from local file
    if (loadedBitmap == null && !isVideo && !filePath.isNullOrEmpty()) {
        loadedBitmap = decodeSampledBitmapFromFile(filePath, targetDimensionPx, targetDimensionPx, preferRgb565)
    }
    currentCoroutineContext().ensureActive()

    // Strategy 4: Sampled BitmapFactory decoding from ContentResolver InputStream
    if (loadedBitmap == null && !isVideo && contentUri.scheme == ContentResolver.SCHEME_CONTENT) {
        loadedBitmap = decodeSampledBitmapFromStream(context, contentUri, targetDimensionPx, targetDimensionPx, preferRgb565)
    }
    currentCoroutineContext().ensureActive()

    // Strategy 5: Legacy MediaStore thumbnail APIs on pre-Android 10
    if (loadedBitmap == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val numericId = mediaId.toLongOrNull() ?: try {
            ContentUris.parseId(contentUri)
        } catch (_: Throwable) {
            -1L
        }
        if (numericId > 0L) {
            try {
                @Suppress("DEPRECATION")
                loadedBitmap = rightSizeToTarget(
                    if (isVideo) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            contentResolver,
                            numericId,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    } else {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            contentResolver,
                            numericId,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                    },
                    targetDimensionPx,
                    preferRgb565
                )
            } catch (_: Throwable) {
            }
        }
    }
    currentCoroutineContext().ensureActive()

    // Strategy 6: Video frame extraction fallback
    if (loadedBitmap == null && isVideo) {
        val videoTargetUri = if (!filePath.isNullOrEmpty()) Uri.fromFile(File(filePath)) else contentUri
        loadedBitmap = rightSizeToTarget(
            extractVideoFrameFallback(context, videoTargetUri, targetDimensionPx),
            targetDimensionPx,
            preferRgb565
        )
    }
    currentCoroutineContext().ensureActive()

    // Pre-upload texture to GPU on background thread — eliminates UI-thread jank on first draw.
    // Skipped for cancelled requests so dead work never burns GPU bandwidth or evicts hot entries.
    loadedBitmap?.prepareToDraw()
    loadedBitmap?.let { PhotonThumbnailCache.put(mediaId, it) }

    return loadedBitmap
}

/**
 * Downscales provider-returned bitmaps that exceed the requested bucket. MediaStore often
 * hands back 512px thumbnails for a 160px request; oversized textures quadruple GPU upload
 * time and memory footprint per cell and accelerate LRU eviction during dense-grid flings.
 *
 * When [preferRgb565] is set, ARGB_8888 results are converted to RGB_565 (2 bytes/pixel
 * instead of 4). HARDWARE bitmaps are left untouched — they already live GPU-side and
 * cannot be cheaply reconfigured.
 */
private fun rightSizeToTarget(bitmap: Bitmap?, targetPx: Int, preferRgb565: Boolean = false): Bitmap? {
    if (bitmap == null || bitmap.isRecycled) return bitmap
    val isHardware = bitmap.config == Bitmap.Config.HARDWARE

    fun convertConfig(source: Bitmap): Bitmap {
        if (!preferRgb565 || isHardware || source.config == Bitmap.Config.RGB_565) return source
        return try {
            source.copy(Bitmap.Config.RGB_565, false) ?: source
        } catch (_: Throwable) {
            source
        }
    }

    val maxSide = maxOf(bitmap.width, bitmap.height)
    val budget = (targetPx * 1.25f).toInt()
    if (maxSide <= budget) return convertConfig(bitmap)

    val scale = targetPx.toFloat() / maxSide
    val width = (bitmap.width * scale).toInt().coerceIn(1, bitmap.width)
    val height = (bitmap.height * scale).toInt().coerceIn(1, bitmap.height)
    val scaled = try {
        Bitmap.createScaledBitmap(bitmap, width, height, true) ?: return convertConfig(bitmap)
    } catch (_: Throwable) {
        return bitmap
    }
    return convertConfig(scaled)
}

private fun decodeSampledBitmapFromFile(
    path: String,
    reqWidth: Int,
    reqHeight: Int,
    preferRgb565: Boolean = false
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = if (preferRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        BitmapFactory.decodeFile(path, options)
    } catch (_: Throwable) {
        null
    }
}

private fun decodeSampledBitmapFromStream(
    context: Context,
    uri: Uri,
    reqWidth: Int,
    reqHeight: Int,
    preferRgb565: Boolean = false
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = if (preferRgb565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (_: Throwable) {
        null
    }
}

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

/**
 * Fallback frame extractor for local video files when ContentResolver thumbnail generation is unavailable.
 */
private fun extractVideoFrameFallback(
    context: Context,
    videoUri: Uri,
    targetDimensionPx: Int
): Bitmap? {
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever().apply {
            setDataSource(context, videoUri)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                1_000_000L, // 1 second into video
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetDimensionPx,
                targetDimensionPx
            )
        } else {
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } catch (_: Throwable) {
        null
    } finally {
        try {
            retriever?.release()
        } catch (_: Throwable) {
        }
    }
}
