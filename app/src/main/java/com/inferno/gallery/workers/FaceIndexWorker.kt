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
import com.inferno.gallery.data.ai.FaceScanHelper
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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


        // 2. Query strictly non-vault, unindexed media
        val unindexedIds = faceDao.getUnindexedMediaIds()
        val totalPhotos = faceDao.getTotalNonVaultMediaCount().coerceAtLeast(unindexedIds.size)
        if (unindexedIds.isEmpty()) {
            Log.d(TAG, "All media faces are up to date.")
            com.inferno.gallery.data.IndexingProgressManager.updateFaceProgress(
                isIndexing = false,
                progress = totalPhotos,
                total = totalPhotos
            )
            setProgress(workDataOf("processed" to totalPhotos, "total" to totalPhotos))
            return@withContext Result.success()
        }

        val alreadyIndexed = (totalPhotos - unindexedIds.size).coerceAtLeast(0)
        var processedCount = 0

        com.inferno.gallery.data.IndexingProgressManager.updateFaceProgress(
            isIndexing = true,
            progress = alreadyIndexed,
            total = totalPhotos
        )
        setProgress(workDataOf("processed" to alreadyIndexed, "total" to totalPhotos))

        try {
            setForeground(createForegroundInfo("Scanning faces: $alreadyIndexed / $totalPhotos", alreadyIndexed, totalPhotos))
        } catch (_: Exception) {}

        // 3. Initialize detector using FaceScanHelper options (0.02f minFaceSize)
        val detector = FaceDetection.getClient(FaceScanHelper.createDetectorOptions())

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
                    val savedFaceCount = FaceScanHelper.scanSingleMedia(
                        context = context,
                        mediaId = media.id,
                        filePath = media.filePath,
                        uri = uri,
                        database = database,
                        engine = engine,
                        clusteringManager = clusteringManager,
                        detector = detector
                    )
                    if (savedFaceCount > 0) {
                        Log.i(TAG, "Indexed media ${media.id}: found $savedFaceCount faces")
                    }

                    processedCount++
                    val currentProgress = alreadyIndexed + processedCount
                    val photoName = media.name.ifBlank { media.filePath.substringAfterLast('/') }
                    com.inferno.gallery.data.IndexingProgressManager.updateFaceProgress(
                        isIndexing = true,
                        progress = currentProgress,
                        total = totalPhotos,
                        currentImageName = photoName
                    )
                    if (processedCount % 5 == 0 || processedCount == unindexedIds.size) {
                        setProgress(workDataOf("processed" to currentProgress, "total" to totalPhotos))
                    }
                }

                val currentBatchProgress = alreadyIndexed + processedCount
                setProgress(workDataOf("processed" to currentBatchProgress, "total" to totalPhotos))
                try {
                    setForeground(createForegroundInfo("Scanning faces: $currentBatchProgress / $totalPhotos", currentBatchProgress, totalPhotos))
                } catch (_: Exception) {}

                // Periodically consolidate clusters every 100 photos to keep identities grouped
                if (processedCount > 0 && processedCount % 100 == 0) {
                    runCatching { clusteringManager.consolidateClusters() }
                }

                delay(50)
            }

            // Final consolidation pass after all photos are processed
            if (processedCount > 0) {
                runCatching { clusteringManager.consolidateClusters() }
            }
            Result.success()
        } finally {
            val total = runCatching { faceDao.getTotalNonVaultMediaCount() }.getOrDefault(0)
            val unindexed = runCatching { faceDao.getUnindexedMediaIds().size }.getOrDefault(0)
            com.inferno.gallery.data.IndexingProgressManager.updateFaceProgress(
                isIndexing = false,
                progress = (total - unindexed).coerceAtLeast(0),
                total = total
            )
            if (processedCount > 0) {
                runCatching { clusteringManager.consolidateClusters() }
            }
            runCatching { faceDao.recalculateAllPersonFaceCounts() }
            detector.close()
        }
    }
}
