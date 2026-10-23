package com.inferno.gallery.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.inferno.gallery.data.ai.FaceClusteringManager
import com.inferno.gallery.data.ai.FaceNormalizer
import com.inferno.gallery.data.ai.FacePoint
import com.inferno.gallery.data.ai.FaceRecognitionEngine
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max

class FaceIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "FaceIndexWorker"
        private const val NOTIFICATION_ID = 57
        private const val CHANNEL_ID = "face_indexing_channel"
        private const val BATCH_SIZE = 10
        private const val MAX_THUMBNAIL_DIM = 1024
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Preparing face scanning…")

    private fun createForegroundInfo(progress: String, current: Int = 0, total: Int = 0): ForegroundInfo {
        val context = applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Face Recognition Indexing",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows progress while scanning and organizing faces in your gallery" }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Face Recognition")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)

        if (total > 0) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, builder.build())
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val database = DatabaseProvider.getDatabase(context)
        val faceDao = database.faceDao()
        val engine = FaceRecognitionEngine.getInstance(context)
        val clusteringManager = FaceClusteringManager.getInstance(context)

        // 1. Ensure engine is supported and model is available
        if (!FaceRecognitionEngine.isSupported()) {
            Log.w(TAG, "Device architecture does not support ONNX Runtime face inference.")
            return@withContext Result.success()
        }

        if (!engine.isModelDownloaded()) {
            Log.w(TAG, "Face model is not yet downloaded. Aborting indexing.")
            return@withContext Result.failure()
        }
        engine.loadModel()

        // If no faces are stored in the database yet, clear any prior stale scan statuses to ensure a complete fresh index
        if (faceDao.getFaceCount() == 0) {
            Log.d(TAG, "Zero faces in database: clearing obsolete scan statuses to re-index properly.")
            faceDao.clearAllFaceStatuses()
        }

        // 2. Query strictly non-vault, unindexed media
        val unindexedIds = faceDao.getUnindexedMediaIds()
        if (unindexedIds.isEmpty()) {
            Log.d(TAG, "All media faces are up to date.")
            setProgress(workDataOf("processed" to 0, "total" to 0))
            return@withContext Result.success()
        }

        val totalPhotos = unindexedIds.size
        var processedCount = 0
        setProgress(workDataOf("processed" to 0, "total" to totalPhotos))

        try {
            setForeground(createForegroundInfo("Scanning faces: 0 / $totalPhotos", 0, totalPhotos))
        } catch (_: Exception) {}

        // 3. Initialize detector with high accuracy, landmarks, and sensible minFaceSize (0.05 = 5% of photo)
        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.05f)
            .build()
        val detector = FaceDetection.getClient(detectorOptions)

        val faceCropDir = File(context.filesDir, "face_crops").apply {
            if (!exists()) mkdirs()
        }

        try {
            for (batch in unindexedIds.chunked(BATCH_SIZE)) {
                if (isStopped) {
                    Log.d(TAG, "Face indexing worker stopped by system or user.")
                    return@withContext Result.retry()
                }

                val mediaEntities = database.mediaDao().getMediaByIdsList(batch)
                for (media in mediaEntities) {
                    if (isStopped) return@withContext Result.retry()

                    val uri = Uri.parse(media.uriString)
                    val sampledBitmap = decodeSampledBitmap(context, media.filePath, uri, MAX_THUMBNAIL_DIM)
                    if (sampledBitmap == null || sampledBitmap.width < 32 || sampledBitmap.height < 32) {
                        faceDao.insertFaceStatus(
                            com.inferno.gallery.data.db.MediaFaceStatusEntity(
                                mediaId = media.id,
                                faceCount = 0,
                                scannedAt = System.currentTimeMillis()
                            )
                        )
                        sampledBitmap?.recycle()
                        processedCount++
                        continue
                    }

                    try {
                        val inputImage = InputImage.fromBitmap(sampledBitmap, 0)
                        val detectedFaces = Tasks.await(detector.process(inputImage))
                        var savedFaceCount = 0

                        for ((faceIndex, face) in detectedFaces.withIndex()) {
                            // Pose & quality filters
                            val box = face.boundingBox
                            if (box.width() < 28 || box.height() < 28) continue
                            if (abs(face.headEulerAngleY) > 50f || abs(face.headEulerAngleX) > 40f) continue

                            // 5-Point landmarks
                            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                            val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                            val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                            val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                            val aligned112: Bitmap? = if (leftEye != null && rightEye != null) {
                                val landmarks = mutableListOf(
                                    FacePoint(leftEye.x, leftEye.y),
                                    FacePoint(rightEye.x, rightEye.y)
                                )
                                if (nose != null) landmarks.add(FacePoint(nose.x, nose.y))
                                if (mouthLeft != null) landmarks.add(FacePoint(mouthLeft.x, mouthLeft.y))
                                if (mouthRight != null) landmarks.add(FacePoint(mouthRight.x, mouthRight.y))
                                FaceNormalizer.alignFace(sampledBitmap, landmarks) ?: cropFaceBoundingBox(sampledBitmap, box)
                            } else {
                                cropFaceBoundingBox(sampledBitmap, box)
                            }
                            if (aligned112 == null) continue

                            try {
                                val embedding = engine.extractEmbedding(aligned112) ?: continue

                                // Save 160×160 avatar crop for UI
                                val cropFile = File(faceCropDir, "face_${media.id}_${faceIndex}.webp")
                                val avatarBitmap = Bitmap.createScaledBitmap(aligned112, 160, 160, true)
                                try {
                                    FileOutputStream(cropFile).use { out ->
                                        avatarBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                                    }
                                } finally {
                                    if (avatarBitmap != aligned112) avatarBitmap.recycle()
                                }

                                val normBboxLeft = (box.left.toFloat() / sampledBitmap.width).coerceIn(0f, 1f)
                                val normBboxTop = (box.top.toFloat() / sampledBitmap.height).coerceIn(0f, 1f)
                                val normBboxRight = (box.right.toFloat() / sampledBitmap.width).coerceIn(0f, 1f)
                                val normBboxBottom = (box.bottom.toFloat() / sampledBitmap.height).coerceIn(0f, 1f)

                                val poseScore = 1f - (abs(face.headEulerAngleY) / 50f * 0.5f + abs(face.headEulerAngleX) / 40f * 0.5f)
                                val qualityScore = poseScore.coerceIn(0.1f, 1.0f)

                                clusteringManager.assignFaceToCluster(
                                    mediaId = media.id,
                                    embedding = embedding,
                                    bboxLeft = normBboxLeft,
                                    bboxTop = normBboxTop,
                                    bboxRight = normBboxRight,
                                    bboxBottom = normBboxBottom,
                                    qualityScore = qualityScore,
                                    cropCachePath = cropFile.absolutePath
                                )
                                savedFaceCount++
                            } finally {
                                aligned112.recycle()
                            }
                        }

                        faceDao.insertFaceStatus(
                            com.inferno.gallery.data.db.MediaFaceStatusEntity(
                                mediaId = media.id,
                                faceCount = savedFaceCount,
                                scannedAt = System.currentTimeMillis()
                            )
                        )
                        if (savedFaceCount > 0) {
                            Log.i(TAG, "Indexed media ${media.id}: found $savedFaceCount faces")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error processing media ${media.id}: ${e.message}")
                        faceDao.insertFaceStatus(
                            com.inferno.gallery.data.db.MediaFaceStatusEntity(
                                mediaId = media.id,
                                faceCount = 0,
                                scannedAt = System.currentTimeMillis()
                            )
                        )
                    } finally {
                        sampledBitmap.recycle()
                    }

                    processedCount++
                    if (processedCount % 5 == 0) {
                        setProgress(workDataOf("processed" to processedCount, "total" to totalPhotos))
                    }
                }

                // Update progress and yield CPU to prevent thermal throttling
                setProgress(workDataOf("processed" to processedCount, "total" to totalPhotos))
                try {
                    setForeground(createForegroundInfo("Scanning faces: $processedCount / $totalPhotos", processedCount, totalPhotos))
                } catch (_: Exception) {}

                delay(50)
            }

            Result.success()
        } finally {
            detector.close()
        }
    }

    private fun cropFaceBoundingBox(source: Bitmap, box: Rect): Bitmap? {
        return try {
            val marginX = (box.width() * 0.15f).toInt()
            val marginY = (box.height() * 0.15f).toInt()
            val left = (box.left - marginX).coerceIn(0, source.width - 1)
            val top = (box.top - marginY).coerceIn(0, source.height - 1)
            val right = (box.right + marginX).coerceIn(left + 1, source.width)
            val bottom = (box.bottom + marginY).coerceIn(top + 1, source.height)
            val cropWidth = right - left
            val cropHeight = bottom - top
            if (cropWidth < 32 || cropHeight < 32) return null
            val cropped = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
            val scaled = Bitmap.createScaledBitmap(cropped, FaceNormalizer.CROP_SIZE, FaceNormalizer.CROP_SIZE, true)
            if (scaled != cropped) cropped.recycle()
            scaled
        } catch (_: Exception) {
            null
        }
    }

    private fun openInputStream(context: Context, filePath: String, uri: Uri): InputStream? {
        if (filePath.isNotBlank()) {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                return file.inputStream()
            }
        }
        return context.contentResolver.openInputStream(uri)
    }

    private fun readExifOrientation(context: Context, filePath: String, uri: Uri): Int {
        return try {
            openInputStream(context, filePath, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
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
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun decodeSampledBitmap(context: Context, filePath: String, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val orientation = readExifOrientation(context, filePath, uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openInputStream(context, filePath, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            val maxOriginal = max(origWidth, origHeight)
            while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = openInputStream(context, filePath, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            applyExifOrientation(decoded, orientation)
        } catch (_: Exception) {
            null
        }
    }
}
