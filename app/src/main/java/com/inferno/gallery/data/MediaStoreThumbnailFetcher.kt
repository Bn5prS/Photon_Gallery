package com.inferno.gallery.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

private val mediaProviderSemaphore = Semaphore(4) // Max concurrent system ContentResolver.loadThumbnail IPCs — each is CPU-heavy in MediaProvider, so keep headroom for the UI thread
private val videoDecodeSemaphore = Semaphore(2)   // Max 2 concurrent video frame extractions
private val diskCacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

private data class CacheTask(val cacheDir: File, val cacheFile: File, val bitmap: Bitmap)
private val diskCacheChannel = Channel<CacheTask>(
    capacity = 16,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

private val diskCacheWorker = diskCacheScope.launch {
    for (task in diskCacheChannel) {
        try {
            if (task.cacheFile.exists() && task.cacheFile.length() > 0) continue
            if (!task.cacheDir.exists()) task.cacheDir.mkdirs()
            val tempFile = File(task.cacheDir, "${task.cacheFile.name}.tmp")
            val softwareBitmap = if (task.bitmap.config == Bitmap.Config.HARDWARE) {
                task.bitmap.copy(Bitmap.Config.RGB_565, false) ?: task.bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                task.bitmap
            }
            if (softwareBitmap != null) {
                FileOutputStream(tempFile).use { out ->
                    softwareBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, out)
                    out.flush()
                }
                if (softwareBitmap != task.bitmap) {
                    softwareBitmap.recycle()
                }
                if (tempFile.length() > 0) {
                    tempFile.renameTo(task.cacheFile)
                } else {
                    tempFile.delete()
                }
            }
        } catch (_: Exception) {
        }
    }
}

/**
 * A high-performance custom Coil Fetcher that retrieves thumbnails with a 2-tier caching system:
 * 1. App-local fast WebP disk cache (`thumb_cache`) to completely eliminate redundant system Binder IPCs.
 * 2. MediaStore ContentResolver.loadThumbnail with strict concurrency control (Semaphore) to avoid
 *    saturating system MediaProvider Binder thread pools during rapid scrolling.
 */
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context,
    private val cacheEnabled: Boolean = true
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val isVideo = uri.toString().contains("video", ignoreCase = true)
        
        // Dynamically resolve target dimension based on Coil request size (clamped 128..512)
        val reqWidth = (options.size.width as? Dimension.Pixels)?.px
        val reqHeight = (options.size.height as? Dimension.Pixels)?.px
        val targetDim = maxOf(reqWidth ?: 256, reqHeight ?: 256).coerceIn(128, 512)
        val targetSize = Size(targetDim, targetDim)

        val uriId = try {
            ContentUris.parseId(uri).toString()
        } catch (_: Exception) {
            uri.lastPathSegment ?: uri.hashCode().toString()
        }

        val cacheDir = File(context.cacheDir, "thumb_cache")
        val cacheKey = "${if (isVideo) "v" else "i"}_${uriId}_${targetDim}.webp"
        val cacheFile = File(cacheDir, cacheKey)

        // Tier 1: Check fast local file cache (no IPC, <1ms load)
        if (cacheEnabled && cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val decodeOpts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.HARDWARE
                }
                val cachedBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, decodeOpts)
                if (cachedBitmap != null) {
                    return@withContext ImageFetchResult(
                        image = cachedBitmap.asImage(),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                cacheFile.delete()
            }
        }

        coroutineContext.ensureActive()

        // Tier 2: Fetch from system MediaProvider / MediaMetadataRetriever under concurrency limit
        var isFromSystem = false
        val bitmap: Bitmap? = if (isVideo) {
            var tempBitmap: Bitmap? = mediaProviderSemaphore.withPermit {
                coroutineContext.ensureActive()
                try {
                    context.contentResolver.loadThumbnail(uri, targetSize, null)
                } catch (e: Exception) {
                    null
                }
            }

            if (tempBitmap == null) {
                coroutineContext.ensureActive()
                tempBitmap = videoDecodeSemaphore.withPermit {
                    coroutineContext.ensureActive()
                    getVideoFrameFallback(context, uri, targetDim)
                }
            } else {
                isFromSystem = true
            }
            tempBitmap
        } else {
            var tempBitmap: Bitmap? = mediaProviderSemaphore.withPermit {
                coroutineContext.ensureActive()
                try {
                    context.contentResolver.loadThumbnail(uri, targetSize, null)
                } catch (e: Exception) {
                    null
                }
            }
            if (tempBitmap != null) {
                isFromSystem = true
            }
            tempBitmap
        }

        coroutineContext.ensureActive()

        if (bitmap != null) {
            // Save to local disk cache asynchronously
            if (cacheEnabled) {
                saveThumbnailToDiskCache(cacheDir, cacheFile, bitmap)
            }

            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = true,
                dataSource = if (isFromSystem) DataSource.DISK else DataSource.MEMORY
            )
        } else {
            // Downsampled fallback decode to prevent loading huge unscaled images in memory
            val fallbackBitmap = decodeDownsampledBitmap(context, uri, targetDim)
            if (fallbackBitmap != null) {
                if (cacheEnabled) {
                    saveThumbnailToDiskCache(cacheDir, cacheFile, fallbackBitmap)
                }
                ImageFetchResult(
                    image = fallbackBitmap.asImage(),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
        }
    }

    private fun decodeDownsampledBitmap(context: Context, uri: Uri, targetDim: Int): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val outWidth = boundsOpts.outWidth
            val outHeight = boundsOpts.outHeight
            if (outWidth <= 0 || outHeight <= 0) return null

            var inSampleSize = 1
            val maxDim = maxOf(outWidth, outHeight)
            while ((maxDim / (inSampleSize * 2)) >= targetDim) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.HARDWARE
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveThumbnailToDiskCache(cacheDir: File, cacheFile: File, bitmap: Bitmap) {
        if (cacheFile.exists() && cacheFile.length() > 0) return
        diskCacheChannel.trySend(CacheTask(cacheDir, cacheFile, bitmap))
    }

    private fun getVideoFrameFallback(context: Context, uri: Uri, targetDim: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetDim, targetDim)
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreFetcher", "Fallback video frame extraction failed for uri: $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }


    class Factory(private val context: Context) : Fetcher.Factory<coil3.Uri> {

        private val cacheEnabled = AtomicBoolean(true)

        init {
            val settings = SettingsRepository.getInstance(context)
            diskCacheScope.launch {
                try {
                    cacheEnabled.set(settings.cacheThumbnailsEnabledFlow.first())
                } catch (_: Exception) { }
            }
            settings.cacheThumbnailsEnabledFlow
                .onEach { cacheEnabled.set(it) }
                .launchIn(diskCacheScope)
        }

        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null

            val width = (options.size.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
            if (width > 1024) return null

            val androidUri = android.net.Uri.parse(data.toString())
            return MediaStoreThumbnailFetcher(androidUri, options, context, cacheEnabled.get())
        }
    }

    class AndroidUriFactory(private val context: Context) : Fetcher.Factory<android.net.Uri> {

        private val cacheEnabled = AtomicBoolean(true)

        init {
            val settings = SettingsRepository.getInstance(context)
            diskCacheScope.launch {
                try {
                    cacheEnabled.set(settings.cacheThumbnailsEnabledFlow.first())
                } catch (_: Exception) { }
            }
            settings.cacheThumbnailsEnabledFlow
                .onEach { cacheEnabled.set(it) }
                .launchIn(diskCacheScope)
        }

        override fun create(data: android.net.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null

            val width = (options.size.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
            if (width > 1024) return null

            return MediaStoreThumbnailFetcher(data, options, context, cacheEnabled.get())
        }
    }
}


