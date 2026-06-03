package com.inferno.gallery.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.inferno.gallery.data.ai.ONNXImageEncoder
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.MediaVectorEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.work.workDataOf
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Background WorkManager worker that runs after [MediaSyncWorker].
 *
 * For each unindexed image in the Room SSOT:
 *  1. Loads a downsampled thumbnail from MediaStore via ContentResolver.
 *  2. Runs it through the INT8 MobileCLIP-S2 ONNX Image Encoder.
 *  3. Stores the resulting L2-normalised float embedding in [MediaVectorEntity].
 *  4. Marks the item as indexed in [core_media].
 *
 * Processes images in batches of 50 to keep peak memory low.
 * Video items are skipped (CLIP is not a video model).
 * Runs as a foreground service so it survives backgrounding.
 */
class AIIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AIIndexWorker"
        private const val BATCH_SIZE = 50
        // BitmapFactory sampling — loads a ~256px thumbnail for CLIP; avoids OOM on large RAW files
        private const val SAMPLE_SIZE = 4
        private const val NOTIFICATION_ID = 42
        private const val CHANNEL_ID = "ai_indexing_channel"
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val context = applicationContext

        // Create notification channel (required for API 26+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Indexing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress while indexing photos for smart search"
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Indexing photos for Smart Search")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            // Promote to foreground service so we survive backgrounding
            setForeground(createForegroundInfo("Starting…"))

            val db = DatabaseProvider.getDatabase(applicationContext)
            val imageEncoder = ONNXImageEncoder(applicationContext)
            imageEncoder.initialize()

            Log.d(TAG, "AI indexing started.")
            val totalImageCount = db.mediaDao().getTotalImageCount()
            val unindexedCount = db.mediaDao().getUnindexedClipImageCount()
            val alreadyIndexed = totalImageCount - unindexedCount
            var totalIndexed = 0
            val recentUris = ArrayDeque<String>(5)
            
            setProgress(workDataOf(
                "progress" to alreadyIndexed, 
                "total" to totalImageCount,
                "recent_uris" to recentUris.toTypedArray()
            ))

            // Process in batches until all items are indexed
            while (true) {
                val batch = db.mediaDao().getUnindexedClipMedia().filter { !it.isVideo }
                if (batch.isEmpty()) break

                for (entity in batch) {
                    if (isStopped) break
                    try {
                        val uri = Uri.parse(entity.uriString)
                        val bitmap = applicationContext.contentResolver.openInputStream(uri)?.use {
                            val opts = BitmapFactory.Options().apply { inSampleSize = SAMPLE_SIZE }
                            BitmapFactory.decodeStream(it, null, opts)
                        } ?: run {
                            Log.w(TAG, "Could not open stream for ${entity.name}, skipping.")
                            db.mediaDao().updateClipIndexStatus(entity.id, true)
                            totalIndexed++
                            setProgress(workDataOf("progress" to (alreadyIndexed + totalIndexed), "total" to totalImageCount))
                            continue
                        }

                        val embedding = imageEncoder.encodeImage(bitmap)
                        bitmap.recycle()

                        // Serialize FloatArray → ByteArray (little-endian)
                        val byteBuffer = ByteBuffer.allocate(embedding.size * 4)
                            .order(ByteOrder.LITTLE_ENDIAN)
                        embedding.forEach { byteBuffer.putFloat(it) }

                        db.searchDao().insertVector(
                            MediaVectorEntity(
                                mediaId = entity.id,
                                clipVector = byteBuffer.array()
                            )
                        )
                        db.mediaDao().updateClipIndexStatus(entity.id, true)
                        totalIndexed++
                        
                        recentUris.addLast(uri.toString())
                        if (recentUris.size > 5) recentUris.removeFirst()
                        
                        // Update UI after every single image
                        val currentProgress = alreadyIndexed + totalIndexed
                        setProgress(workDataOf(
                            "progress" to currentProgress, 
                            "total" to totalImageCount,
                            "recent_uris" to recentUris.toTypedArray()
                        ))
                        // Update foreground notification
                        setForeground(createForegroundInfo("$currentProgress / $totalImageCount images"))

                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to index ${entity.name}: ${e.message}")
                        db.mediaDao().updateClipIndexStatus(entity.id, true)
                        totalIndexed++
                        setProgress(workDataOf(
                            "progress" to (alreadyIndexed + totalIndexed), 
                            "total" to totalImageCount,
                            "recent_uris" to recentUris.toTypedArray()
                        ))
                    }
                }
            }

            Log.d(TAG, "AI indexing complete. Indexed $totalIndexed images.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "AIIndexWorker failed fatally: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}
