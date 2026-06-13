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
import coil3.fetch.ImageFetchResult
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import coil3.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * A custom Coil Fetcher that retrieves pre-generated thumbnails from the Android system's
 * MediaStore provider using ContentResolver.loadThumbnail.
 *
 * It features a high-performance local file-based cache to avoid expensive binder IPC / DB lookups,
 * and a robust fallback utilizing MediaMetadataRetriever to extract and cache video frames if
 * system-level loadThumbnail fails.
 */
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val settings = SettingsRepository(context)
        val cacheEnabled = try {
            settings.cacheThumbnailsEnabledFlow.first()
        } catch (e: Exception) {
            true
        }

        val cacheDir = context.cacheDir.resolve("media_store_thumbnails")
        if (cacheEnabled && !cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val cacheKey = getCacheKey(uri)
        val cacheFile = File(cacheDir, "$cacheKey.jpg")

        // 1. Try custom disk cache first (extremely fast, no binder IPC, no video decoding)
        if (cacheEnabled && cacheFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    return@withContext ImageFetchResult(
                        image = bitmap.asImage(),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (e: Exception) {
                // If corrupted, delete and fall back
                cacheFile.delete()
            }
        }

        // 2. Try loading from MediaStore ContentResolver (normally fast, but involves IPC)
        var bitmap: Bitmap? = null
        val isVideo = uri.toString().contains("video", ignoreCase = true)

        try {
            // Force 512x512 to ensure Android OS hits the pre-generated MINI_KIND cache
            // instead of synchronously spinning up a video decoder for a custom size (e.g. 384x384)
            bitmap = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
        } catch (e: Exception) {
            android.util.Log.w("MediaStoreFetcher", "loadThumbnail failed for uri: $uri, attempting fallback")
        }

        // 3. Fallback specifically for videos if loadThumbnail fails/misses
        if (bitmap == null && isVideo) {
            bitmap = getVideoFrameFallback(context, uri)
        }

        if (bitmap != null) {
            // 4. Save to custom disk cache if enabled
            if (cacheEnabled) {
                try {
                    cacheFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaStoreFetcher", "Failed to write thumbnail cache for: $uri", e)
                }
            }

            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } else {
            null
        }
    }

    private fun getCacheKey(uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(uri.toString().toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            uri.toString().hashCode().toString()
        }
    }

    private fun getVideoFrameFallback(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 512, 512)
            } else {
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                    Bitmap.createScaledBitmap(it, 512, 512, true)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaStoreFetcher", "Fallback video frame extraction failed for uri: $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle local media content URIs (scheme = content, authority = media)
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null
            
            // Bypass this fetcher for high-res requests so Coil can decode the full image
            val width = (options.size.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
            if (width > 1024) return null
            
            return MediaStoreThumbnailFetcher(data, options, context)
        }
    }
}
