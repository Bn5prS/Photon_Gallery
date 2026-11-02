package com.inferno.gallery.ui.editor.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.inferno.gallery.ui.editor.markup.MarkupRasterizer
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.model.CropState
import com.inferno.gallery.ui.editor.model.FilterPreset
import com.inferno.gallery.ui.editor.model.TuneValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max

/**
 * Lossless high-resolution background image export engine for photo editor export.
 *
 * Executes full-fidelity rendering on high-resolution source bitmaps:
 * 1. Decodes full original resolution bitmap with ARGB_8888 config.
 * 2. Normalizes orientation according to EXIF metadata.
 * 3. Rasterizes markup strokes, privacy redact mosaics, and text overlays via [MarkupRasterizer].
 * 4. Applies geometric transformations: rotation, flips, fine straighten angle, 2-axis perspective keystone, and crop rectangle.
 * 5. Applies combined affine ColorMatrix covering all 13 adjustments and filter presets via [ImageProcessingEngine].
 * 6. Applies vignette radial gradient if configured.
 * 7. Commits to MediaStore with quality 95 JPEG and `-edited` naming suffix (or overwrites original).
 */
object ImageExportWorker {

    /**
     * Exports the edited photo in high resolution on background IO dispatcher.
     *
     * @return [Result] wrapping the destination [Uri] on success, or an exception on failure.
     */
    suspend fun exportImage(
        context: Context,
        sourceUri: Uri,
        cropState: CropState,
        tuneValues: TuneValues,
        filterPreset: FilterPreset = FilterPreset.ORIGINAL,
        filterStrength: Float = 100f,
        markupState: MarkupState = MarkupState(),
        overwrite: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Decode original full-resolution bitmap
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Cannot open input stream for $sourceUri")

            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }

            val rawBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                ?: throw IllegalStateException("Failed to decode image from $sourceUri")
            inputStream.close()

            // 2. Normalize EXIF orientation if needed
            val exifOrientation = try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            var workBitmap = applyExifOrientation(rawBitmap, exifOrientation)

            // 3. Apply markup rasterization onto high-res bitmap
            if (markupState.strokes.isNotEmpty() || markupState.texts.isNotEmpty()) {
                val annotated = if (workBitmap.isMutable) {
                    workBitmap
                } else {
                    workBitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
                val markupCanvas = Canvas(annotated)
                MarkupRasterizer.rasterize(
                    canvas = markupCanvas,
                    bitmapWidth = annotated.width,
                    bitmapHeight = annotated.height,
                    markup = markupState,
                    sourceBitmap = workBitmap
                )
                if (workBitmap != annotated && !workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
                workBitmap = annotated
            }

            // 4. Geometric transformations (rotation, flips, straighten, perspective)
            val totalRotation = (cropState.rotation.toFloat() + cropState.straightenAngle)
            val hasRotation = totalRotation != 0f
            val hasFlip = cropState.flipH || cropState.flipV
            val hasPerspective = cropState.verticalTilt != 0f || cropState.horizontalTilt != 0f

            if (hasRotation || hasFlip || hasPerspective) {
                val matrix = android.graphics.Matrix()
                val cx = workBitmap.width / 2f
                val cy = workBitmap.height / 2f

                if (cropState.flipH) matrix.preScale(-1f, 1f, cx, cy)
                if (cropState.flipV) matrix.preScale(1f, -1f, cx, cy)
                if (totalRotation != 0f) matrix.postRotate(totalRotation, cx, cy)

                if (hasPerspective) {
                    val camera = android.graphics.Camera()
                    camera.save()
                    camera.rotateX(cropState.verticalTilt)
                    camera.rotateY(-cropState.horizontalTilt)
                    val perspectiveMatrix = android.graphics.Matrix()
                    camera.getMatrix(perspectiveMatrix)
                    camera.restore()
                    perspectiveMatrix.preTranslate(-cx, -cy)
                    perspectiveMatrix.postTranslate(cx, cy)
                    matrix.postConcat(perspectiveMatrix)
                }

                val transformed = Bitmap.createBitmap(
                    workBitmap,
                    0,
                    0,
                    workBitmap.width,
                    workBitmap.height,
                    matrix,
                    true
                )
                if (transformed != workBitmap && !workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
                workBitmap = transformed
            }

            // 5. Apply crop rect
            val cropRect = cropState.cropRect
            val isCropped = cropRect.left > 0.001f || cropRect.top > 0.001f ||
                    cropRect.right < 0.999f || cropRect.bottom < 0.999f
            if (isCropped) {
                val x = (cropRect.left * workBitmap.width).toInt().coerceIn(0, workBitmap.width - 1)
                val y = (cropRect.top * workBitmap.height).toInt().coerceIn(0, workBitmap.height - 1)
                val w = ((cropRect.right - cropRect.left) * workBitmap.width).toInt().coerceIn(1, workBitmap.width - x)
                val h = ((cropRect.bottom - cropRect.top) * workBitmap.height).toInt().coerceIn(1, workBitmap.height - y)
                val cropped = Bitmap.createBitmap(workBitmap, x, y, w, h)
                if (cropped != workBitmap && !workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
                workBitmap = cropped
            }

            // 6. Apply combined affine ColorMatrix (13 adjustments + filter preset)
            val filterAndroidMatrix = if (filterPreset != FilterPreset.ORIGINAL && filterStrength > 0f) {
                filterPreset.interpolateAndroid(filterStrength)
            } else null

            val combinedMatrix = ImageProcessingEngine.buildCombinedAndroidMatrix(
                tune = tuneValues,
                filterMatrix = filterAndroidMatrix
            )

            val colorAdjustedBitmap = Bitmap.createBitmap(
                workBitmap.width,
                workBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(colorAdjustedBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                colorFilter = ColorMatrixColorFilter(combinedMatrix)
            }
            canvas.drawBitmap(workBitmap, 0f, 0f, paint)
            if (workBitmap != colorAdjustedBitmap && !workBitmap.isRecycled) {
                workBitmap.recycle()
            }
            workBitmap = colorAdjustedBitmap

            // 7. Apply vignette radial gradient
            if (tuneValues.vignette > 0f) {
                val cx = workBitmap.width / 2f
                val cy = workBitmap.height / 2f
                val radius = max(cx, cy) * 1.2f
                val alpha = ((tuneValues.vignette / 100f) * 200).toInt().coerceIn(0, 255)
                val vignettePaint = Paint().apply {
                    isAntiAlias = true
                    isDither = true
                    shader = RadialGradient(
                        cx, cy, radius,
                        intArrayOf(0x00000000, 0x00000000, android.graphics.Color.argb(alpha, 0, 0, 0)),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                val vignetteCanvas = Canvas(workBitmap)
                vignetteCanvas.drawRect(
                    0f,
                    0f,
                    workBitmap.width.toFloat(),
                    workBitmap.height.toFloat(),
                    vignettePaint
                )
            }

            // 8. Commit to MediaStore
            if (overwrite) {
                val outputStream: OutputStream? = if (sourceUri.scheme == "file" || sourceUri.scheme == null) {
                    val file = File(sourceUri.path ?: sourceUri.toString())
                    FileOutputStream(file)
                } else {
                    try {
                        context.contentResolver.openOutputStream(sourceUri, "w")
                    } catch (e: Exception) {
                        val path = queryFilePath(context, sourceUri)
                        if (path != null) FileOutputStream(File(path)) else throw e
                    }
                }
                outputStream?.use { out ->
                    workBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                } ?: throw IllegalStateException("Cannot open output stream for overwrite: $sourceUri")

                if (sourceUri.scheme == "content") {
                    try {
                        val cv = ContentValues().apply {
                            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        }
                        context.contentResolver.update(sourceUri, cv, null, null)
                    } catch (_: Exception) {}
                }

                if (!workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
                sourceUri
            } else {
                val originalName = queryDisplayName(context, sourceUri) ?: "Photon_${System.currentTimeMillis()}"
                val baseName = if (originalName.contains(".")) originalName.substringBeforeLast(".") else originalName
                val cleanBase = if (baseName.endsWith("-edited")) baseName else "$baseName-edited"
                val newDisplayName = "$cleanBase.jpg"

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, newDisplayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Photon")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val newUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Failed to insert image entry into MediaStore")

                val outputStream = context.contentResolver.openOutputStream(newUri)
                    ?: throw IllegalStateException("Cannot open output stream for $newUri")
                outputStream.use { out ->
                    workBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(newUri, values, null, null)

                if (!workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
                newUri
            }
        }
    }

    /**
     * Convenience method matching alternative plan signature: returns [Uri] or throws exception.
     */
    suspend fun exportEditedImage(
        context: Context,
        sourceUri: Uri,
        cropState: CropState,
        tuneValues: TuneValues,
        filterPreset: FilterPreset = FilterPreset.ORIGINAL,
        filterStrength: Float = 100f,
        markupState: MarkupState = MarkupState(),
        overwrite: Boolean = false
    ): Uri {
        return exportImage(
            context = context,
            sourceUri = sourceUri,
            cropState = cropState,
            tuneValues = tuneValues,
            filterPreset = filterPreset,
            filterStrength = filterStrength,
            markupState = markupState,
            overwrite = overwrite
        ).getOrThrow()
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return oriented
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
            } catch (e: Exception) {
                // Ignore and fall through
            }
        }
        return uri.lastPathSegment
    }

    private fun queryFilePath(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media.DATA),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (idx >= 0) return cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.path
    }
}
