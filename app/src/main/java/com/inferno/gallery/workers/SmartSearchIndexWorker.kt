package com.inferno.gallery.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inferno.gallery.data.ai.SmartSearchEngine
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.MediaEmbeddingEntity
import com.inferno.gallery.data.db.MediaEmbeddingStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class SmartSearchIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SmartSearchIndexWorker"
        private const val NOTIFICATION_ID = 66
        private const val CHANNEL_ID = "smart_search_indexing_channel"
        private const val LOAD_CHANNEL_CAPACITY = 4
        private const val EMBED_CHANNEL_CAPACITY = 4
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Preparing image indexing…")

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val context = applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Smart Search Indexing",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows progress while scanning images for semantic contents" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Smart Search Indexing")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private data class LoadedImage(
        val mediaId: Long,
        val bitmap: Bitmap,
        val name: String,
        val dateModified: Long,
        val size: Long
    )

    private data class EmbeddedImage(
        val mediaId: Long,
        val embedding: FloatArray,
        val name: String,
        val dateModified: Long,
        val size: Long
    )

    private fun openInputStream(context: Context, filePath: String, uriString: String): InputStream {
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(uriString))
            if (input != null) return input
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "ContentResolver failed to open Uri $uriString: ${e.message}")
        }

        val file = java.io.File(filePath)
        if (!file.exists() || !file.canRead()) {
            throw java.io.FileNotFoundException("File does not exist or cannot be read: $filePath")
        }
        return file.inputStream()
    }

    private fun readExifOrientation(context: Context, filePath: String, uriString: String): Int {
        return try {
            openInputStream(context, filePath, uriString).use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unable to read EXIF orientation for $uriString: ${e.message}")
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EXIF orientation: ${e.message}")
            bitmap
        }
    }

    private fun decodeDownsampledBitmap(context: Context, filePath: String, uriString: String): Bitmap? {
        var input: InputStream? = null
        try {
            val orientation = readExifOrientation(context, filePath, uriString)
            input = openInputStream(context, filePath, uriString)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            var sampleSize = 1
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / sampleSize >= 224 && halfWidth / sampleSize >= 224) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            input = openInputStream(context, filePath, uriString)
            val decoded = BitmapFactory.decodeStream(input, null, decodeOptions)
            return decoded?.let { applyExifOrientation(it, orientation) }
        } finally {
            try {
                input?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun doWork(): Result {
        com.inferno.gallery.data.IndexingProgressManager.updateClipProgress(
            isIndexing = true,
            progress = 0,
            total = 0,
            currentImageName = "Initializing..."
        )
        val searchEngine = SmartSearchEngine.getInstance(applicationContext)
        try {
            if (!searchEngine.isModelDownloaded()) {
                Log.e(TAG, "Models not downloaded. Cannot index.")
                return Result.failure()
            }

            setForeground(createForegroundInfo("Loading AI model…"))
            searchEngine.loadModel()

            val db = DatabaseProvider.getDatabase(applicationContext)

            // Clean up any zero-vector embeddings so they can be re-indexed
            withContext(Dispatchers.IO) {
                val allEmbeddings = db.embeddingDao().getAllEmbeddings()
                val zeroEmbeddings = allEmbeddings.filter { it.embedding.all { value -> value == 0f } }
                if (zeroEmbeddings.isNotEmpty()) {
                    Log.d(TAG, "Cleaning up ${zeroEmbeddings.size} zero-vector embeddings from DB...")
                    zeroEmbeddings.forEach { db.embeddingDao().deleteEmbedding(it.mediaId) }
                }
            }

            val unindexedIds = withContext(Dispatchers.IO) {
                db.embeddingDao().getUnindexedMediaIds()
            }

            if (unindexedIds.isEmpty()) {
                Log.d(TAG, "No unindexed images found.")
                return Result.success()
            }

            val totalCount = withContext(Dispatchers.IO) {
                db.mediaDao().getTotalImageCount()
            }
            val alreadyIndexed = totalCount - unindexedIds.size
            var processedCount = 0

            Log.d(TAG, "Smart Search indexing started. Unindexed: ${unindexedIds.size}, total: $totalCount")

            com.inferno.gallery.data.IndexingProgressManager.updateClipProgress(
                isIndexing = true,
                progress = alreadyIndexed,
                total = totalCount,
                currentImageName = "Starting..."
            )

            setProgress(workDataOf(
                "progress" to alreadyIndexed,
                "total" to totalCount,
                "current_image" to ""
            ))

            val loadChannel = Channel<LoadedImage>(LOAD_CHANNEL_CAPACITY)
            val embedChannel = Channel<EmbeddedImage>(EMBED_CHANNEL_CAPACITY)

            // Limited parallelism dispatcher for AI execution (isolated CPU thread context)
            val aiDispatcher = Dispatchers.Default.limitedParallelism(1)

            coroutineScope {
                // 1. Loading Stage (IO thread)
                launch(Dispatchers.IO) {
                    for (mediaId in unindexedIds) {
                        if (isStopped) break
                        var entity: com.inferno.gallery.data.db.CoreMediaEntity? = null
                        try {
                            entity = db.mediaDao().getMediaById(mediaId)
                            if (entity == null) {
                                // Deleted in background, skip
                                continue
                            }
                            val bitmap = decodeDownsampledBitmap(applicationContext, entity.filePath, entity.uriString)
                            if (bitmap != null) {
                                loadChannel.send(
                                    LoadedImage(
                                        mediaId = mediaId,
                                        bitmap = bitmap,
                                        name = entity.name,
                                        dateModified = entity.dateModified,
                                        size = entity.size
                                    )
                                )
                            } else {
                                // Invalid file format / corrupt image, insert zero-vector to avoid retrying endlessly
                                Log.w(TAG, "Bitmap decoding returned null for ${entity.name}. Marking with zero vector.")
                                db.embeddingStatusDao().upsertStatus(
                                    MediaEmbeddingStatusEntity(
                                        mediaId = mediaId,
                                        status = MediaEmbeddingEntity.STATUS_FAILED_PERMANENT,
                                        dateModified = entity.dateModified,
                                        size = entity.size,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        } catch (e: java.io.FileNotFoundException) {
                            Log.e(TAG, "FileNotFoundException loading $mediaId (${e.message}). Marking with zero vector.")
                            // File genuinely deleted or inaccessible on disk, mark with zero vector to avoid infinite retries
                            val fallback = entity
                            if (fallback != null) {
                                db.embeddingStatusDao().upsertStatus(
                                    MediaEmbeddingStatusEntity(
                                        mediaId = mediaId,
                                        status = MediaEmbeddingEntity.STATUS_FAILED_PERMANENT,
                                        dateModified = fallback.dateModified,
                                        size = fallback.size,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException loading $mediaId: ${e.message}. Marking as transient failure.")
                            val fallback = entity
                            if (fallback != null) {
                                db.embeddingStatusDao().upsertStatus(
                                    MediaEmbeddingStatusEntity(
                                        mediaId = mediaId,
                                        status = MediaEmbeddingEntity.STATUS_FAILED_TRANSIENT,
                                        dateModified = fallback.dateModified,
                                        size = fallback.size,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed loading $mediaId: ${e.message}. Marking as transient failure.")
                            val fallback = entity
                            if (fallback != null) {
                                db.embeddingStatusDao().upsertStatus(
                                    MediaEmbeddingStatusEntity(
                                        mediaId = mediaId,
                                        status = MediaEmbeddingEntity.STATUS_FAILED_TRANSIENT,
                                        dateModified = fallback.dateModified,
                                        size = fallback.size,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    }
                    loadChannel.close()
                }

                // 2. Inference Stage (Isolated CPU/GPU thread)
                launch(aiDispatcher) {
                    for (loaded in loadChannel) {
                        if (isStopped) {
                            loaded.bitmap.recycle()
                            break
                        }
                        try {
                            val embedding = searchEngine.encodeImage(loaded.bitmap)
                            embedChannel.send(
                                EmbeddedImage(
                                    mediaId = loaded.mediaId,
                                    embedding = embedding,
                                    name = loaded.name,
                                    dateModified = loaded.dateModified,
                                    size = loaded.size
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Stage2 embedding failed for ${loaded.name}: ${e.message}")
                        } finally {
                            loaded.bitmap.recycle()
                        }
                    }
                    embedChannel.close()
                }

                // 3. Database Writing Stage (IO thread)
                launch(Dispatchers.IO) {
                    for (embedded in embedChannel) {
                        if (isStopped) break
                        try {
                            db.embeddingDao().insertEmbedding(
                                MediaEmbeddingEntity(
                                    mediaId = embedded.mediaId,
                                    embedding = embedded.embedding,
                                    dateModified = embedded.dateModified,
                                    size = embedded.size
                                )
                            )
                            db.embeddingStatusDao().deleteStatus(embedded.mediaId)
                            processedCount++

                            val sample = embedded.embedding.take(5).joinToString(", ")
                            val nonZero = embedded.embedding.count { it != 0f }
                            Log.d(TAG, "Inserted embedding for ${embedded.name} (ID: ${embedded.mediaId}). Size: ${embedded.embedding.size}, Non-Zero count: $nonZero, Sample: [$sample]")

                            val current = alreadyIndexed + processedCount
                            com.inferno.gallery.data.IndexingProgressManager.updateClipProgress(
                                isIndexing = true,
                                progress = current,
                                total = totalCount,
                                currentImageName = embedded.name
                            )
                            setProgress(workDataOf(
                                "progress" to current,
                                "total" to totalCount,
                                "current_image" to embedded.name
                            ))
                            setForeground(createForegroundInfo("$current / $totalCount images scanned"))
                        } catch (e: Exception) {
                            Log.e(TAG, "Stage3 db insert failed for media ID ${embedded.mediaId}: ${e.message}")
                        }
                    }
                }
            }

            Log.d(TAG, "Smart Search indexing complete. Indexed $processedCount images.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "SmartSearchIndexWorker failed fatally: ${e.message}", e)
            return Result.retry()
        } finally {
            val db = DatabaseProvider.getDatabase(applicationContext)
            val total = runCatching { db.mediaDao().getTotalImageCount() }.getOrDefault(0)
            val unindexed = runCatching { db.embeddingDao().getUnindexedMediaIds().size }.getOrDefault(0)
            com.inferno.gallery.data.IndexingProgressManager.updateClipProgress(
                isIndexing = false,
                progress = total - unindexed,
                total = total,
                currentImageName = null
            )
        }
    }
}
