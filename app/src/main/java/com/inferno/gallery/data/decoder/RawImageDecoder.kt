package com.inferno.gallery.data.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
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
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

internal data class RawOrientationMetadata(
    val exifOrientation: Int = ExifInterface.ORIENTATION_NORMAL,
    val rawWidth: Int = 0,
    val rawHeight: Int = 0
)

/**
 * Helper and Coil 3 Fetcher for high-performance camera RAW image decoding:
 * - Native Android ImageDecoder for DNG / RAW (Android 12+, minSdk 31) with explicit EXIF/MediaStore
 *   orientation correction (Android's SkRawCodec frequently leaves SubIFD RAW buffers unrotated)
 * - ExifInterface TIFF/IFD embedded preview JPEG extraction for proprietary
 *   DSLR/mirrorless formats (Canon CR2/CR3, Nikon NEF/NRW, Sony ARW, Fuji RAF, Olympus ORF, etc.)
 * - BitmapFactory stream fallback for non-ImageDecoder TIFF/JPEG previews
 */
object RawImageHelper {

    private val RAW_EXTENSIONS = setOf(
        "dng", "raw", "cr2", "cr3", "nef", "nrw", "arw", "orf", "raf", "rw2", "pef", "srw",
        "3fr", "rwl", "mrw", "x3f", "sr2", "srf"
    )

    private val RAW_MIME_TYPES = setOf(
        "image/x-adobe-dng",
        "image/dng",
        "image/x-canon-cr2",
        "image/x-canon-cr3",
        "image/x-nikon-nef",
        "image/x-nikon-nrw",
        "image/x-sony-arw",
        "image/x-sony-sr2",
        "image/x-sony-srf",
        "image/x-olympus-orf",
        "image/x-fuji-raf",
        "image/x-panasonic-rw2",
        "image/x-pentax-pef",
        "image/x-samsung-srw",
        "image/x-hasselblad-3fr",
        "image/x-leica-rwl",
        "image/x-minolta-mrw",
        "image/x-sigma-x3f",
        "image/x-dcraw",
        "image/x-raw"
    )

    private val rawUriCache = android.util.LruCache<Uri, Boolean>(4096)

    internal fun isRawExtension(ext: String): Boolean {
        if (ext.isBlank()) return false
        return RAW_EXTENSIONS.contains(ext.lowercase(Locale.ROOT).trim())
    }

    internal fun isRawFilename(nameOrPath: String): Boolean {
        if (nameOrPath.isBlank()) return false
        val clean = nameOrPath.substringBefore('?').substringBefore('#')
        val fileName = clean.substringAfterLast('/')
        val ext = fileName.substringAfterLast('.', "")
        return ext != fileName && isRawExtension(ext)
    }

    internal fun isRawMimeType(mime: String?): Boolean {
        if (mime.isNullOrBlank()) return false
        val normalized = mime.lowercase(Locale.ROOT).trim()
        return RAW_MIME_TYPES.contains(normalized)
    }

    internal fun degreesToExifOrientation(degrees: Int): Int {
        val normalized = ((degrees % 360) + 360) % 360
        return when (normalized) {
            90 -> ExifInterface.ORIENTATION_ROTATE_90
            180 -> ExifInterface.ORIENTATION_ROTATE_180
            270 -> ExifInterface.ORIENTATION_ROTATE_270
            else -> ExifInterface.ORIENTATION_NORMAL
        }
    }

    internal fun swapsDimensions(exifOrientation: Int): Boolean = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_ROTATE_270,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE -> true
        else -> false
    }

    /**
     * Determines whether [bitmapWidth] x [bitmapHeight] still needs [metadata.exifOrientation]
     * applied. When the orientation swaps X/Y axes and unrotated sensor dimensions are known,
     * only rotates if the decoded bitmap still matches the unrotated aspect orientation, avoiding
     * double-rotation on platform decoders that already applied EXIF orientation.
     */
    internal fun shouldApplyExifOrientation(
        decodedWidth: Int,
        decodedHeight: Int,
        metadata: RawOrientationMetadata
    ): Boolean {
        val orient = metadata.exifOrientation
        if (orient == ExifInterface.ORIENTATION_UNDEFINED || orient == ExifInterface.ORIENTATION_NORMAL) {
            return false
        }
        if (decodedWidth <= 0 || decodedHeight <= 0) return false

        if (swapsDimensions(orient) &&
            metadata.rawWidth > 0 &&
            metadata.rawHeight > 0 &&
            metadata.rawWidth != metadata.rawHeight &&
            decodedWidth != decodedHeight
        ) {
            val rawIsLandscape = metadata.rawWidth > metadata.rawHeight
            val decodedIsLandscape = decodedWidth > decodedHeight
            return decodedIsLandscape == rawIsLandscape
        }
        return true
    }

    fun isRawUri(context: Context, uri: Uri): Boolean {
        rawUriCache.get(uri)?.let { return it }

        val isRaw = checkIsRaw(context, uri)
        rawUriCache.put(uri, isRaw)
        return isRaw
    }

    /**
     * Fast suspend check optimized for grid thumbnail fetching: checks [rawUriCache] first,
     * then looks up local `content://media/...` rows in Room by ID before falling back to
     * [checkIsRaw] (zero extra Binder IPCs for normal JPEG/HEIC grid cells).
     */
    suspend fun isRawUriSuspend(context: Context, uri: Uri): Boolean {
        rawUriCache.get(uri)?.let { return it }

        if (uri.scheme == "file") {
            val isRaw = isRawFilename(uri.path ?: "")
            rawUriCache.put(uri, isRaw)
            return isRaw
        }

        if (uri.scheme == "content" && uri.authority == "media") {
            val mediaId = uri.lastPathSegment?.toLongOrNull()
            if (mediaId != null) {
                try {
                    val entity = DatabaseProvider.getDatabase(context).mediaDao().getMediaById(mediaId)
                    if (entity != null) {
                        val isRaw = isRawMimeType(entity.mimeType) ||
                            isRawFilename(entity.name) ||
                            isRawFilename(entity.filePath)
                        rawUriCache.put(uri, isRaw)
                        return isRaw
                    }
                } catch (_: Throwable) {
                }
            }
        }

        val isRaw = checkIsRaw(context, uri)
        rawUriCache.put(uri, isRaw)
        return isRaw
    }

    private fun checkIsRaw(context: Context, uri: Uri): Boolean {
        if (isRawFilename(uri.path ?: "")) return true

        if (uri.scheme == "content") {
            try {
                val mime = context.contentResolver.getType(uri)
                if (isRawMimeType(mime)) return true

                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0) ?: ""
                        if (isRawFilename(name)) return true
                    }
                }
            } catch (_: Exception) { }
        }
        return false
    }

    private fun readRawOrientationMetadata(context: Context, uri: Uri): RawOrientationMetadata {
        var exif: ExifInterface? = null
        try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                exif = ExifInterface(File(uri.path!!))
            } else {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        exif = ExifInterface(pfd.fileDescriptor)
                    }
                } catch (_: Throwable) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        exif = ExifInterface(stream)
                    }
                }
            }
        } catch (_: Throwable) {
        }

        var orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        ) ?: ExifInterface.ORIENTATION_UNDEFINED
        var rawWidth = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) ?: 0
        var rawHeight = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) ?: 0

        if (uri.scheme == "content" &&
            (orientation == ExifInterface.ORIENTATION_UNDEFINED ||
                orientation == ExifInterface.ORIENTATION_NORMAL ||
                rawWidth <= 0 ||
                rawHeight <= 0)
        ) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Images.Media.ORIENTATION,
                        MediaStore.Images.Media.WIDTH,
                        MediaStore.Images.Media.HEIGHT
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        if (orientation == ExifInterface.ORIENTATION_UNDEFINED ||
                            orientation == ExifInterface.ORIENTATION_NORMAL
                        ) {
                            val oriCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
                            if (oriCol >= 0 && !cursor.isNull(oriCol)) {
                                orientation = degreesToExifOrientation(cursor.getInt(oriCol))
                            }
                        }
                        if (rawWidth <= 0 || rawHeight <= 0) {
                            val wCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                            val hCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                            if (wCol >= 0 && hCol >= 0) {
                                val w = cursor.getInt(wCol)
                                val h = cursor.getInt(hCol)
                                if (w > 0 && h > 0) {
                                    rawWidth = w
                                    rawHeight = h
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            orientation = ExifInterface.ORIENTATION_NORMAL
        }
        return RawOrientationMetadata(
            exifOrientation = orientation,
            rawWidth = rawWidth,
            rawHeight = rawHeight
        )
    }

    /**
     * Decode a RAW image bitmap with explicit orientation correction and graceful fallbacks:
     * 1. Native ImageDecoder (API 31+) with post-decode EXIF orientation check
     * 2. ExifInterface embedded preview (full resolution or scaled)
     * 3. BitmapFactory stream fallback with EXIF orientation check
     */
    fun decodeRawBitmap(context: Context, uri: Uri, targetDim: Int? = null): Bitmap? {
        val orientationMetadata = readRawOrientationMetadata(context, uri)

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
                return if (shouldApplyExifOrientation(bitmap.width, bitmap.height, orientationMetadata)) {
                    rotateBitmapIfNeeded(bitmap, orientationMetadata.exifOrientation)
                } else {
                    bitmap
                }
            }
        } catch (_: Throwable) {
            // Proprietary camera RAW format not recognized by native ImageDecoder -> fallback to preview extraction
        }

        // Strategy 2: Embedded Preview Extraction via ExifInterface
        decodeExifEmbeddedPreview(context, uri, targetDim, orientationMetadata)?.let { return it }

        // Strategy 3: BitmapFactory stream fallback for non-ImageDecoder formats
        return decodeStreamFallback(context, uri, targetDim, orientationMetadata)
    }

    private fun decodeExifEmbeddedPreview(
        context: Context,
        uri: Uri,
        targetDim: Int?,
        orientationMetadata: RawOrientationMetadata
    ): Bitmap? {
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
        val thumbBytes = try {
            if (parsedExif.hasThumbnail()) parsedExif.thumbnailBytes else null
        } catch (_: Throwable) { null }

        if (thumbBytes != null && thumbBytes.isNotEmpty()) {
            return decodeSampledBitmapFromByteArray(thumbBytes, targetDim, orientationMetadata)
        }

        return null
    }

    private fun decodeSampledBitmapFromByteArray(
        data: ByteArray,
        targetDim: Int?,
        orientationMetadata: RawOrientationMetadata
    ): Bitmap? {
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
            if (shouldApplyExifOrientation(rawBitmap.width, rawBitmap.height, orientationMetadata)) {
                rotateBitmapIfNeeded(rawBitmap, orientationMetadata.exifOrientation)
            } else {
                rawBitmap
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeStreamFallback(
        context: Context,
        uri: Uri,
        targetDim: Int?,
        orientationMetadata: RawOrientationMetadata
    ): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val outW = boundsOpts.outWidth
            val outH = boundsOpts.outHeight
            if (outW <= 0 || outH <= 0) return null

            var sampleSize = 1
            if (targetDim != null && targetDim > 0) {
                val maxDim = maxOf(outW, outH)
                while ((maxDim / (sampleSize * 2)) >= targetDim) {
                    sampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            if (shouldApplyExifOrientation(rawBitmap.width, rawBitmap.height, orientationMetadata)) {
                rotateBitmapIfNeeded(rawBitmap, orientationMetadata.exifOrientation)
            } else {
                rawBitmap
            }
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
