package com.inferno.gallery.ui.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.inferno.gallery.data.MediaSyncEngine
import com.inferno.gallery.ui.utils.ImageCacheEvictor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

enum class FrameExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val compressFormat: Bitmap.CompressFormat,
    val quality: Int
) {
    JPEG("JPEG", "jpg", "image/jpeg", Bitmap.CompressFormat.JPEG, 100),
    PNG("PNG", "png", "image/png", Bitmap.CompressFormat.PNG, 100)
}

enum class VideoExportResolution(
    val label: String,
    val maxShortSide: Int?
) {
    ORIGINAL("Original", null),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480)
}

/**
 * Pure Kotlin math and formatting helpers for offline video trimming & quick editing.
 * Kept free of Android framework classes so every edge case is unit-testable.
 */
object VideoTrimMath {
    const val MIN_TRIM_DURATION_MS = 500L

    val SPEED_PRESETS = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f)

    fun effectiveMinDuration(totalDurationMs: Long, minDurationMs: Long = MIN_TRIM_DURATION_MS): Long {
        if (totalDurationMs <= 0L) return 0L
        return minOf(minDurationMs.coerceAtLeast(1L), totalDurationMs)
    }

    fun clampRange(
        startMs: Long,
        endMs: Long,
        totalDurationMs: Long,
        minDurationMs: Long = MIN_TRIM_DURATION_MS
    ): Pair<Long, Long> {
        if (totalDurationMs <= 0L) return 0L to 0L
        val minGap = effectiveMinDuration(totalDurationMs, minDurationMs)
        val clampedStart = startMs.coerceIn(0L, (totalDurationMs - minGap).coerceAtLeast(0L))
        val clampedEnd = endMs.coerceIn((clampedStart + minGap).coerceAtMost(totalDurationMs), totalDurationMs)
        return clampedStart to clampedEnd
    }

    fun dragStartHandle(
        newStartMs: Long,
        currentEndMs: Long,
        totalDurationMs: Long,
        minDurationMs: Long = MIN_TRIM_DURATION_MS
    ): Long {
        if (totalDurationMs <= 0L) return 0L
        val minGap = effectiveMinDuration(totalDurationMs, minDurationMs)
        val safeEnd = currentEndMs.coerceIn(minGap, totalDurationMs)
        return newStartMs.coerceIn(0L, (safeEnd - minGap).coerceAtLeast(0L))
    }

    fun dragEndHandle(
        currentStartMs: Long,
        newEndMs: Long,
        totalDurationMs: Long,
        minDurationMs: Long = MIN_TRIM_DURATION_MS
    ): Long {
        if (totalDurationMs <= 0L) return 0L
        val minGap = effectiveMinDuration(totalDurationMs, minDurationMs)
        val safeStart = currentStartMs.coerceIn(0L, (totalDurationMs - minGap).coerceAtLeast(0L))
        return newEndMs.coerceIn((safeStart + minGap).coerceAtMost(totalDurationMs), totalDurationMs)
    }

    fun computeOutputDurationMs(trimmedDurationMs: Long, speed: Float): Long {
        if (trimmedDurationMs <= 0L) return 0L
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        return (trimmedDurationMs.toDouble() / safeSpeed.toDouble()).toLong().coerceAtLeast(1L)
    }

    fun canApplyResolution(
        sourceWidth: Int,
        sourceHeight: Int,
        preset: VideoExportResolution
    ): Boolean {
        val targetShort = preset.maxShortSide ?: return true
        if (sourceWidth <= 0 || sourceHeight <= 0) return true
        val shortSide = minOf(sourceWidth, sourceHeight)
        return shortSide > targetShort
    }

    /**
     * Computes even-aligned target `(width, height)` after applying [rotationDegreesCw] and
     * downscaling to [preset]. Returns `null` if no scaling is needed (original or source is already smaller).
     */
    fun computeTargetResolution(
        sourceWidth: Int,
        sourceHeight: Int,
        rotationDegreesCw: Int,
        preset: VideoExportResolution
    ): Pair<Int, Int>? {
        val targetShort = preset.maxShortSide ?: return null
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val normalizedRot = ((rotationDegreesCw % 360) + 360) % 360
        val effectiveW = if (normalizedRot == 90 || normalizedRot == 270) sourceHeight else sourceWidth
        val effectiveH = if (normalizedRot == 90 || normalizedRot == 270) sourceWidth else sourceHeight
        val shortSide = minOf(effectiveW, effectiveH)
        if (shortSide <= targetShort) return null

        val scale = targetShort.toDouble() / shortSide.toDouble()
        val outW = roundToEven((effectiveW * scale).roundToInt())
        val outH = roundToEven((effectiveH * scale).roundToInt())
        return outW to outH
    }

    private fun roundToEven(value: Int): Int {
        val clamped = value.coerceAtLeast(2)
        return if (clamped % 2 == 0) clamped else clamped - 1
    }

    /**
     * Converts clockwise UI rotation degrees (0, 90, 180, 270) to Media3 OpenGL counterclockwise degrees.
     */
    fun toMedia3RotationCcw(rotationDegreesCw: Int): Float {
        val normalizedCw = ((rotationDegreesCw % 360) + 360) % 360
        return ((360 - normalizedCw) % 360).toFloat()
    }

    fun estimateTrimmedSizeBytes(
        originalSizeBytes: Long,
        trimmedDurationMs: Long,
        totalDurationMs: Long
    ): Long {
        if (originalSizeBytes <= 0L || totalDurationMs <= 0L || trimmedDurationMs <= 0L) return 0L
        val ratio = (trimmedDurationMs.toDouble() / totalDurationMs.toDouble()).coerceIn(0.0, 1.0)
        return (originalSizeBytes * ratio).toLong().coerceAtLeast(1L)
    }

    fun estimateEditedSizeBytes(
        originalSizeBytes: Long,
        trimmedDurationMs: Long,
        totalDurationMs: Long,
        speed: Float = 1.0f,
        sourceWidth: Int = 0,
        sourceHeight: Int = 0,
        rotationDegreesCw: Int = 0,
        resolution: VideoExportResolution = VideoExportResolution.ORIGINAL,
        muteAudio: Boolean = false
    ): Long {
        if (originalSizeBytes <= 0L || totalDurationMs <= 0L || trimmedDurationMs <= 0L) return 0L
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        val durationRatio = (trimmedDurationMs.toDouble() / totalDurationMs.toDouble()).coerceIn(0.0, 1.0)
        val speedFactor = 1.0 / safeSpeed.toDouble()
        val targetRes = computeTargetResolution(sourceWidth, sourceHeight, rotationDegreesCw, resolution)
        val resolutionFactor = if (targetRes != null && sourceWidth > 0 && sourceHeight > 0) {
            val srcPixels = sourceWidth.toDouble() * sourceHeight.toDouble()
            val dstPixels = targetRes.first.toDouble() * targetRes.second.toDouble()
            (dstPixels / srcPixels).coerceIn(0.05, 1.0).pow(0.75)
        } else {
            1.0
        }
        val audioFactor = if (muteAudio) 0.95 else 1.0
        return (originalSizeBytes * durationRatio * speedFactor * resolutionFactor * audioFactor)
            .toLong()
            .coerceAtLeast(1L)
    }

    fun buildTrimmedDisplayName(
        originalName: String?,
        timestampMs: Long = System.currentTimeMillis()
    ): String {
        val trimmedOrig = originalName?.trim().orEmpty()
        if (trimmedOrig.isEmpty()) return "VID_TRIM_$timestampMs.mp4"
        val base = if (trimmedOrig.contains('.')) trimmedOrig.substringBeforeLast('.') else trimmedOrig
        val cleanBase = base.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifEmpty { "VID" }
        val suffixBase = if (cleanBase.endsWith("_trimmed")) cleanBase else "${cleanBase}_trimmed"
        return "${suffixBase}_$timestampMs.mp4"
    }

    fun formatTrimTime(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val tenths = (safeMs % 1000L) / 100L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
        } else {
            String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
        }
    }

    fun formatSpeedLabel(speed: Float): String {
        return when {
            abs(speed - 0.5f) < 0.01f -> "0.5×"
            abs(speed - 0.75f) < 0.01f -> "0.75×"
            abs(speed - 1.0f) < 0.01f -> "1×"
            abs(speed - 1.5f) < 0.01f -> "1.5×"
            abs(speed - 2.0f) < 0.01f -> "2×"
            else -> String.format(Locale.US, "%.2g×", speed)
        }
    }

    fun isTrimModified(
        startMs: Long,
        endMs: Long,
        totalDurationMs: Long,
        muteAudio: Boolean
    ): Boolean {
        if (muteAudio) return true
        if (totalDurationMs <= 0L) return false
        return startMs > 50L || (totalDurationMs - endMs) > 50L
    }

    fun isEditModified(
        startMs: Long,
        endMs: Long,
        totalDurationMs: Long,
        muteAudio: Boolean,
        speed: Float = 1.0f,
        rotationDegreesCw: Int = 0,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        resolution: VideoExportResolution = VideoExportResolution.ORIGINAL,
        sourceWidth: Int = 0,
        sourceHeight: Int = 0
    ): Boolean {
        if (isTrimModified(startMs, endMs, totalDurationMs, muteAudio)) return true
        if (abs(speed - 1.0f) > 0.01f) return true
        if (((rotationDegreesCw % 360) + 360) % 360 != 0) return true
        if (flipHorizontal || flipVertical) return true
        if (resolution != VideoExportResolution.ORIGINAL &&
            computeTargetResolution(sourceWidth, sourceHeight, rotationDegreesCw, resolution) != null
        ) {
            return true
        }
        return false
    }
}

/**
 * Shared full-resolution offline frame extraction helper used by the Frame Extractor screen.
 */
object VideoFrameExportHelper {

    suspend fun exportFrameAtPosition(
        context: Context,
        videoUri: Uri,
        positionMs: Long,
        format: FrameExportFormat = FrameExportFormat.JPEG
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            val fullBitmap = try {
                retriever.setDataSource(context, videoUri)
                retriever.getFrameAtTime(
                    positionMs.coerceAtLeast(0L) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                ) ?: retriever.getFrameAtTime(
                    positionMs.coerceAtLeast(0L) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            } ?: throw IllegalStateException("Could not decode frame at ${positionMs}ms")

            try {
                val nowMs = System.currentTimeMillis()
                val fileName = "FRAME_$nowMs.${format.extension}"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Photon Gallery")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.DATE_ADDED, nowMs / 1000L)
                    put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
                }

                val insertUri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw IllegalStateException("MediaStore insert failed")

                val outputStream = context.contentResolver.openOutputStream(insertUri)
                    ?: throw IllegalStateException("Failed to open output stream for $insertUri")
                outputStream.use { out ->
                    if (!fullBitmap.compress(format.compressFormat, format.quality, out)) {
                        throw IllegalStateException("Bitmap compression failed")
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(insertUri, contentValues, null, null)

                // Immediately pull the new frame into Room via the fast delta lane
                try {
                    MediaSyncEngine.getInstance(context.applicationContext).syncDelta()
                } catch (_: Throwable) {}

                insertUri
            } finally {
                if (!fullBitmap.isRecycled) fullBitmap.recycle()
            }
        }
    }

    /**
     * Decodes a full-resolution frame at [positionMs] into an app-private cache file suitable for
     * sharing via FileProvider without cluttering the user's gallery.
     */
    suspend fun exportFrameToCacheFile(
        context: Context,
        videoUri: Uri,
        positionMs: Long,
        format: FrameExportFormat = FrameExportFormat.JPEG
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            val fullBitmap = try {
                retriever.setDataSource(context, videoUri)
                retriever.getFrameAtTime(
                    positionMs.coerceAtLeast(0L) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                ) ?: retriever.getFrameAtTime(
                    positionMs.coerceAtLeast(0L) * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            } ?: throw IllegalStateException("Could not decode frame at ${positionMs}ms")

            try {
                val shareDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                try {
                    val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                    shareDir.listFiles()?.forEach { old ->
                        if (old.name.startsWith("frame_") && old.lastModified() < oneDayAgo) {
                            old.delete()
                        }
                    }
                } catch (_: Throwable) {}

                val outFile = File(shareDir, "frame_${System.currentTimeMillis()}.${format.extension}")
                FileOutputStream(outFile).use { out ->
                    if (!fullBitmap.compress(format.compressFormat, format.quality, out)) {
                        throw IllegalStateException("Bitmap compression failed")
                    }
                }
                outFile
            } finally {
                if (!fullBitmap.isRecycled) fullBitmap.recycle()
            }
        }
    }
}

data class VideoSourceMeta(
    val durationMs: Long,
    val sizeBytes: Long,
    val displayName: String?,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 30f
)

/**
 * Hardware-accelerated offline video trimming & quick-edit engine powered by Media3 Transformer.
 * Supports non-destructive "Save as Copy" and in-place "Replace Original", plus speed, rotation,
 * flip, audio mute, and resolution downscaling.
 */
object VideoTrimEngine {

    suspend fun inspectSource(context: Context, uri: Uri): VideoSourceMeta = withContext(Dispatchers.IO) {
        var durationMs = 0L
        var sizeBytes = 0L
        var displayName: String? = null
        var width = 0
        var height = 0
        var frameRate = 30f

        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val durIdx = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                        val sizeIdx = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                        val nameIdx = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                        if (durIdx >= 0) durationMs = cursor.getLong(durIdx)
                        if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Throwable) {}
        } else if (uri.scheme == "file" || uri.scheme == null) {
            val file = File(uri.path ?: uri.toString())
            if (file.exists()) {
                sizeBytes = file.length()
                displayName = file.name
            }
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            if (durationMs <= 0L) {
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) {
                width = rawH
                height = rawW
            } else {
                width = rawW
                height = rawH
            }
            val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            val frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull()
            val computedFps = if (frameCount != null && frameCount > 0L && durationMs > 0L) {
                frameCount.toFloat() / (durationMs / 1000f)
            } else null
            val detected = captureFps?.takeIf { it in 1f..240f }
                ?: computedFps?.takeIf { it in 1f..240f }
            if (detected != null) frameRate = detected
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }

        if (sizeBytes <= 0L) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    if (afd.length > 0L) sizeBytes = afd.length
                }
            } catch (_: Throwable) {}
        }

        VideoSourceMeta(
            durationMs = durationMs.coerceAtLeast(0L),
            sizeBytes = sizeBytes.coerceAtLeast(0L),
            displayName = displayName,
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            frameRate = frameRate.coerceIn(1f, 240f)
        )
    }

    @OptIn(UnstableApi::class)
    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        muteAudio: Boolean = false,
        speed: Float = 1.0f,
        rotationDegreesCw: Int = 0,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        resolution: VideoExportResolution = VideoExportResolution.ORIGINAL,
        sourceWidth: Int = 0,
        sourceHeight: Int = 0,
        overwrite: Boolean = false,
        originalDisplayName: String? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<Uri> {
        val appContext = context.applicationContext
        val tempFile = File(appContext.cacheDir, "photon_trim_${System.currentTimeMillis()}.mp4")
        val outputDurationMs = VideoTrimMath.computeOutputDurationMs(
            trimmedDurationMs = (endMs - startMs).coerceAtLeast(0L),
            speed = speed
        )
        return try {
            // 1. Execute hardware-accelerated trim + effects via Media3 Transformer on Main Looper
            runTransformerOnMain(
                context = appContext,
                sourceUri = sourceUri,
                startMs = startMs,
                endMs = endMs,
                muteAudio = muteAudio,
                speed = speed,
                rotationDegreesCw = rotationDegreesCw,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                resolution = resolution,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                outputPath = tempFile.absolutePath,
                onProgress = onProgress
            )

            // 2. Commit output MP4 into MediaStore (Save as Copy or Replace Original)
            val destUri = withContext(Dispatchers.IO) {
                commitTrimmedFile(
                    context = appContext,
                    tempFile = tempFile,
                    sourceUri = sourceUri,
                    overwrite = overwrite,
                    originalDisplayName = originalDisplayName,
                    trimmedDurationMs = outputDurationMs
                )
            }
            Result.success(destUri)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Throwable) {}
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun runTransformerOnMain(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        muteAudio: Boolean,
        speed: Float,
        rotationDegreesCw: Int,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        resolution: VideoExportResolution,
        sourceWidth: Int,
        sourceHeight: Int,
        outputPath: String,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { cont ->
            var progressJob: kotlinx.coroutines.Job? = null
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        progressJob?.cancel()
                        onProgress(1f)
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        progressJob?.cancel()
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                })
                .build()

            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs.coerceAtLeast(0L))
                .setEndPositionMs(endMs.coerceAtLeast(startMs + 1L))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(sourceUri)
                .setClippingConfiguration(clipping)
                .build()

            val videoEffects = mutableListOf<Effect>()
            val audioProcessors = mutableListOf<AudioProcessor>()

            // 1. Rotate / Flip (GPU matrix transformation)
            val normalizedCw = ((rotationDegreesCw % 360) + 360) % 360
            if (normalizedCw != 0 || flipHorizontal || flipVertical) {
                val scaleX = if (flipHorizontal) -1f else 1f
                val scaleY = if (flipVertical) -1f else 1f
                val ccwDegrees = VideoTrimMath.toMedia3RotationCcw(normalizedCw)
                videoEffects.add(
                    ScaleAndRotateTransformation.Builder()
                        .setScale(scaleX, scaleY)
                        .setRotationDegrees(ccwDegrees)
                        .build()
                )
            }

            // 2. Resolution downscale (never upscales; preserves aspect ratio with even dimensions)
            val targetRes = VideoTrimMath.computeTargetResolution(
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                rotationDegreesCw = normalizedCw,
                preset = resolution
            )
            if (targetRes != null) {
                videoEffects.add(
                    Presentation.createForWidthAndHeight(
                        targetRes.first,
                        targetRes.second,
                        Presentation.LAYOUT_SCALE_TO_FIT
                    )
                )
            }

            // 3. Playback speed (video timestamp scaling + pitch-preserved Sonic audio)
            val safeSpeed = speed.coerceIn(0.25f, 4.0f)
            if (abs(safeSpeed - 1.0f) > 0.01f) {
                videoEffects.add(SpeedChangeEffect(safeSpeed))
                if (!muteAudio) {
                    val sonic = SonicAudioProcessor().apply {
                        setSpeed(safeSpeed)
                        setPitch(1.0f)
                    }
                    audioProcessors.add(sonic)
                }
            }

            val effects = if (videoEffects.isEmpty() && audioProcessors.isEmpty()) {
                Effects.EMPTY
            } else {
                Effects(audioProcessors, videoEffects)
            }

            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(muteAudio)
                .setEffects(effects)
                .build()

            cont.invokeOnCancellation {
                progressJob?.cancel()
                try {
                    transformer.cancel()
                } catch (_: Throwable) {}
            }

            transformer.start(editedItem, outputPath)

            val holder = ProgressHolder()
            progressJob = launch(Dispatchers.Main) {
                while (isActive && cont.isActive) {
                    val state = transformer.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((holder.progress / 100f).coerceIn(0f, 0.99f))
                    }
                    delay(100L)
                }
            }
        }
    }

    private suspend fun commitTrimmedFile(
        context: Context,
        tempFile: File,
        sourceUri: Uri,
        overwrite: Boolean,
        originalDisplayName: String?,
        trimmedDurationMs: Long
    ): Uri {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            throw IllegalStateException("Trimmed output file is empty")
        }

        if (overwrite) {
            val outStream = if (sourceUri.scheme == "file" || sourceUri.scheme == null) {
                FileOutputStream(File(sourceUri.path ?: sourceUri.toString()), false)
            } else {
                try {
                    context.contentResolver.openOutputStream(sourceUri, "wt")
                } catch (_: Exception) {
                    context.contentResolver.openOutputStream(sourceUri, "w")
                }
            } ?: throw IllegalStateException("Cannot open destination for overwrite: $sourceUri")

            outStream.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }

            if (sourceUri.scheme == "content") {
                try {
                    val cv = ContentValues().apply {
                        put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
                        put(MediaStore.Video.Media.DURATION, trimmedDurationMs)
                    }
                    context.contentResolver.update(sourceUri, cv, null, null)
                } catch (_: Throwable) {}
            }

            ImageCacheEvictor.evictMedia(context, sourceUri)
            try {
                MediaSyncEngine.getInstance(context).syncDelta()
            } catch (_: Throwable) {}
            return sourceUri
        } else {
            val nowMs = System.currentTimeMillis()
            val displayName = VideoTrimMath.buildTrimmedDisplayName(originalDisplayName, nowMs)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Photon Gallery")
                put(MediaStore.Video.Media.DURATION, trimmedDurationMs)
                put(MediaStore.Video.Media.DATE_ADDED, nowMs / 1000L)
                put(MediaStore.Video.Media.DATE_TAKEN, nowMs)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val destUri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("MediaStore video insert failed")

            val outStream = context.contentResolver.openOutputStream(destUri)
                ?: throw IllegalStateException("Cannot open output stream for $destUri")
            outStream.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(destUri, values, null, null)

            try {
                MediaSyncEngine.getInstance(context).syncDelta()
            } catch (_: Throwable) {}
            return destUri
        }
    }
}
