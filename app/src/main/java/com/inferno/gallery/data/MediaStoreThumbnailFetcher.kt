package com.inferno.gallery.data

import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.withContext

/**
 * A custom Coil Fetcher that retrieves pre-generated thumbnails from the Android system's
 * MediaStore provider using ContentResolver.loadThumbnail.
 *
 * This provides massive performance benefits for local galleries because the OS already has
 * hardware-accelerated, pre-rendered thumbnails stored on disk, eliminating the need to read
 * the full source files (e.g. 100MB+ videos or 20MB+ raw photos) and run expensive software
 * downsampling inside the app process.
 */
class MediaStoreThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        try {
            val width = when (val w = options.size.width) {
                is Dimension.Pixels -> w.px
                else -> 512
            }
            val height = when (val h = options.size.height) {
                is Dimension.Pixels -> h.px
                else -> 512
            }
            
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(width, height), null)
            
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            // If loadThumbnail fails (e.g. file deleted or corrupt), return null
            // so that Coil can fall back to its standard decoder pipeline.
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle local media content URIs (scheme = content, authority = media)
            val isLocalMedia = data.scheme == "content" && data.authority == "media"
            if (!isLocalMedia) return null
            return MediaStoreThumbnailFetcher(data, options, context)
        }
    }
}
