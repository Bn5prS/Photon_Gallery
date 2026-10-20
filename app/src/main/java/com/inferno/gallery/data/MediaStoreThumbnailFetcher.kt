package com.inferno.gallery.data

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private val mediaProviderSemaphore = Semaphore(
    Runtime.getRuntime().availableProcessors().coerceIn(4, 6)
) // Balanced concurrent ContentResolver.loadThumbnail IPCs — leaves CPU headroom for UI 120Hz flings
private val videoDecodeSemaphore = Semaphore(2)   // Max 2 concurrent video frame extractions (CPU/codec-bound)

/**
 * A high-performance custom Coil Fetcher that retrieves thumbnails via MediaStore
 * ContentResolver.loadThumbnail with strict concurrency control and immediate CancellationSignal
 * to abort off-screen decodes during high-velocity flings.
 */
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val isVideo = uri.toString().contains("video", ignoreCase = true)
        
        // Dynamically resolve target dimension based on Coil request size (clamped 128..512)
        val reqWidth = (options.size.width as? Dimension.Pixels)?.px
        val reqHeight = (options.size.height as? Dimension.Pixels)?.px
        val targetDim = maxOf(reqWidth ?: 256, reqHeight ?: 256).coerceIn(128, 512)
        val targetSize = Size(targetDim, targetDim)

        coroutineContext.ensureActive()

        val signal = android.os.CancellationSignal()
        val job = coroutineContext[kotlinx.coroutines.Job]
        val completionHandle = job?.invokeOnCompletion {
            try {
                signal.cancel()
            } catch (_: Throwable) {}
        }

        // Fetch from system MediaProvider / MediaMetadataRetriever under concurrency limit
        var isFromSystem = false
        val bitmap: Bitmap? = try {
            if (isVideo) {
                var tempBitmap: Bitmap? = mediaProviderSemaphore.withPermit {
                    coroutineContext.ensureActive()
                    try {
                        context.contentResolver.loadThumbnail(uri, targetSize, signal)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: android.os.OperationCanceledException) {
                        null
                    } catch (_: Throwable) {
                        null
                    }
                }

                if (tempBitmap == null && !signal.isCanceled) {
                    coroutineContext.ensureActive()
                    tempBitmap = videoDecodeSemaphore.withPermit {
                        coroutineContext.ensureActive()
                        getVideoFrameFallback(context, uri, targetDim)
                    }
                } else if (tempBitmap != null) {
                    isFromSystem = true
                }
                tempBitmap
            } else {
                var tempBitmap: Bitmap? = mediaProviderSemaphore.withPermit {
                    coroutineContext.ensureActive()
                    try {
                        context.contentResolver.loadThumbnail(uri, targetSize, signal)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: android.os.OperationCanceledException) {
                        null
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (tempBitmap != null) {
                    isFromSystem = true
                }
                tempBitmap
            }
        } finally {
            completionHandle?.dispose()
        }

        coroutineContext.ensureActive()
        if (signal.isCanceled) return@withContext null

        if (bitmap != null) {
            val rightSized = rightSizeToTarget(bitmap, targetDim)
            rightSized.prepareToDraw()
            ImageFetchResult(
                image = rightSized.asImage(),
                isSampled = true,
                dataSource = if (isFromSystem) DataSource.DISK else DataSource.MEMORY
            )
        } else {
            // Downsampled fallback decode to prevent loading huge unscaled images in memory
            val fallbackBitmap = if (com.inferno.gallery.data.decoder.RawImageHelper.isRawUri(context, uri)) {
                com.inferno.gallery.data.decoder.RawImageHelper.decodeRawBitmap(context, uri, targetDim)
            } else {
                decodeDownsampledBitmap(context, uri, targetDim)
            }
            if (fallbackBitmap != null) {
                fallbackBitmap.prepareToDraw()
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

    /**
     * Downscales provider-returned bitmaps that exceed the requested bucket.
     *
     * MediaProvider routinely hands back ~512 px for a 160 px grid request. At 4 bytes/pixel
     * that is ~1 MB per cell instead of ~100 KB: GPU upload time per cell multiplies and the
     * LRU evicts hot entries far sooner, so the grid refetches the same thumbnails it just
     * dropped. HARDWARE bitmaps are already GPU-resident and cannot be cheaply reconfigured,
     * so they are passed through untouched.
     */
    private fun rightSizeToTarget(bitmap: Bitmap, targetPx: Int): Bitmap {
        if (bitmap.config == Bitmap.Config.HARDWARE) return bitmap
        val maxSide = maxOf(bitmap.width, bitmap.height)
        val budget = (targetPx * 1.25f).toInt()
        if (maxSide <= budget) return bitmap

        val scale = targetPx.toFloat() / maxSide
        val width = (bitmap.width * scale).toInt().coerceIn(1, bitmap.width)
        val height = (bitmap.height * scale).toInt().coerceIn(1, bitmap.height)
        return try {
            Bitmap.createScaledBitmap(bitmap, width, height, true) ?: bitmap
        } catch (_: Throwable) {
            bitmap
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private fun getVideoFrameFallback(context: Context, uri: Uri, targetDim: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetDim, targetDim)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
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
        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null

            val width = (options.size.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
            if (width > 1024) return null

            val androidUri = android.net.Uri.parse(data.toString())
            return MediaStoreThumbnailFetcher(androidUri, options, context)
        }
    }

    class AndroidUriFactory(private val context: Context) : Fetcher.Factory<android.net.Uri> {
        override fun create(data: android.net.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null

            val width = (options.size.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
            if (width > 1024) return null

            return MediaStoreThumbnailFetcher(data, options, context)
        }
    }
}


