package com.inferno.gallery.data.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale

/**
 * Helper and Coil 3 Fetcher for high-performance camera RAW image decoding:
 * - Native Android ImageDecoder for DNG / RAW (Android 12+, minSdk 31)
 * - ExifInterface TIFF/IFD embedded preview JPEG extraction for proprietary
 *   DSLR/mirrorless formats (Canon CR2/CR3, Nikon NEF/NRW, Sony ARW, Fuji RAF, Olympus ORF, etc.)
 */
object RawImageHelper {

    private val RAW_EXTENSIONS = setOf(
        "dng", "raw", "cr2", "cr3", "nef", "nrw", "arw", "orf", "raf", "rw2", "pef", "srw"
    )

    private val RAW_MIME_TYPES = setOf(
        "image/x-adobe-dng",
        "image/dng",
        "image/x-canon-cr2",
        "image/x-canon-cr3",
        "image/x-nikon-nef",
        "image/x-nikon-nrw",
        "image/x-sony-arw",
        "image/x-olympus-orf",
        "image/x-fuji-raf",
        "image/x-panasonic-rw2",
        "image/x-pentax-pef",
        "image/x-samsung-srw",
        "image/x-raw"
    )

    private val rawUriCache = android.util.LruCache<Uri, Boolean>(2000)

    fun isRawUri(context: Context, uri: Uri): Boolean {
        rawUriCache.get(uri)?.let { return it }

        val isRaw = checkIsRaw(context, uri)
        rawUriCache.put(uri, isRaw)
        return isRaw
    }

    private fun checkIsRaw(context: Context, uri: Uri): Boolean {
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""
        val ext = path.substringAfterLast('.', "")
        if (RAW_EXTENSIONS.contains(ext)) return true

        val scheme = uri.scheme
        if (scheme == "content") {
            try {
                val mime = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT)
                if (mime != null) {
                    return RAW_MIME_TYPES.contains(mime) || mime.startsWith("image/x-")
                }

                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)?.lowercase(Locale.ROOT) ?: ""
                        val nameExt = name.substringAfterLast('.', "")
                        if (RAW_EXTENSIONS.contains(nameExt)) return true
                    }
                }
            } catch (_: Exception) { }
        }
        return false
    }

    /**
     * Decode a RAW image bitmap with graceful fallbacks:
     * 1. Hardware ImageDecoder (API 31+)
     * 2. ExifInterface embedded preview (full resolution or scaled)
     */
    fun decodeRawBitmap(context: Context, uri: Uri, targetDim: Int? = null): Bitmap? {
        // Strategy 1: Native ImageDecoder
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                if (targetDim != null && targetDim > 0) {
                    val origWidth = info.size.width
                    val origHeight = info.size.height
                    val maxDim = maxOf(origWidth, origHeight)
                    if (maxDim > targetDim) {
                        val scale = targetDim.toFloat() / maxDim
                        val targetW = (origWidth * scale).toInt().coerceAtLeast(1)
                        val targetH = (origHeight * scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            if (bitmap.width > 0 && bitmap.height > 0) {
                return bitmap
            }
        } catch (_: Throwable) {
            // Proprietary camera RAW format not recognized by native ImageDecoder -> fallback to preview extraction
        }

        // Strategy 2: Embedded Preview Extraction via ExifInterface
        return decodeExifEmbeddedPreview(context, uri, targetDim)
    }

    private fun decodeExifEmbeddedPreview(context: Context, uri: Uri, targetDim: Int?): Bitmap? {
        var exif: ExifInterface? = null
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                exif = ExifInterface(pfd.fileDescriptor)
            }
        } catch (_: Throwable) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    exif = ExifInterface(stream)
                }
            } catch (_: Throwable) { }
        }

        val parsedExif = exif ?: return null
        val orientation = parsedExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Check if thumbnail/preview bytes exist
        val thumbBytes = try {
            if (parsedExif.hasThumbnail()) parsedExif.thumbnailBytes else null
        } catch (_: Throwable) { null }

        if (thumbBytes != null && thumbBytes.isNotEmpty()) {
            return decodeSampledBitmapFromByteArray(thumbBytes, targetDim, orientation)
        }

        return null
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, targetDim: Int?, orientation: Int): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, boundsOpts)

            var sampleSize = 1
            if (targetDim != null && targetDim > 0 && boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                while ((maxDim / (sampleSize * 2)) >= targetDim) {
                    sampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOpts) ?: return null
            rotateBitmapIfNeeded(rawBitmap, orientation)
        } catch (_: Throwable) {
            null
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Throwable) {
            bitmap
        }
    }
}

/**
 * Coil 3 Fetcher for camera RAW images (DNG, CR2, CR3, NEF, ARW, ORF, RAF, RW2).
 */
class RawImageFetcher(
    private val uri: Uri,
    private val options: Options,
    private val context: Context
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()

        val reqWidth = (options.size.width as? Dimension.Pixels)?.px
        val reqHeight = (options.size.height as? Dimension.Pixels)?.px
        val targetDim = if (reqWidth != null && reqHeight != null) {
            maxOf(reqWidth, reqHeight)
        } else {
            // For ORIGINAL or unbounded size requests, clamp to 3840px max to prevent OutOfMemoryError on 50MP RAW sensors
            3840
        }

        val bitmap = RawImageHelper.decodeRawBitmap(context, uri, targetDim) ?: return@withContext null
        coroutineContext.ensureActive()
        bitmap.prepareToDraw()

        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = targetDim < 3840,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<coil3.Uri> {
        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val androidUri = android.net.Uri.parse(data.toString())
            if (!RawImageHelper.isRawUri(context, androidUri)) return null
            return RawImageFetcher(androidUri, options, context)
        }
    }

    class AndroidUriFactory(private val context: Context) : Fetcher.Factory<android.net.Uri> {
        override fun create(data: android.net.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!RawImageHelper.isRawUri(context, data)) return null
            return RawImageFetcher(data, options, context)
        }
    }
}
