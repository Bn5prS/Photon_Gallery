package com.inferno.gallery.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine

class OcrIndexWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "OcrIndexWorker"
        private const val NOTIFICATION_ID = 44
        private const val CHANNEL_ID = "ocr_indexing_channel"
        private const val WRITE_BATCH_SIZE = 1
        private const val LOAD_CHANNEL_CAPACITY = 8
        private const val EMBED_CHANNEL_CAPACITY = 8
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Preparing…")

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val context = applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Text Indexing",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows progress while indexing photos for text search" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Text (OCR) Indexing")
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
        val entity: CoreMediaEntity,
        val uri: Uri
    )

    private data class EmbeddedImage(
        val entity: CoreMediaEntity,
        val extractedText: String?
    )

    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo("Starting…"))

            val db = DatabaseProvider.getDatabase(applicationContext)

            val unindexed = withContext(Dispatchers.IO) {
                db.mediaDao().getUnindexedOcrMedia()
            }

            if (unindexed.isEmpty()) {
                Log.d(TAG, "Nothing to index.")
                return Result.success()
            }

            val totalImageCount = withContext(Dispatchers.IO) {
                db.mediaDao().getTotalImageCount()
            }
            val alreadyIndexed = totalImageCount - unindexed.size
            var processedCount = 0
            val recentUris = ArrayDeque<String>(5)

            Log.d(TAG, "OCR indexing started. Unindexed: ${unindexed.size}, total: $totalImageCount")

            setProgress(workDataOf(
                "progress" to alreadyIndexed,
                "total" to totalImageCount,
                "recent_uris" to emptyArray<String>()
            ))

            val loadChannel  = Channel<LoadedImage>(LOAD_CHANNEL_CAPACITY)
            val embedChannel = Channel<EmbeddedImage>(EMBED_CHANNEL_CAPACITY)

            coroutineScope {
                launch(Dispatchers.IO) {
                    for (entity in unindexed) {
                        if (isStopped) break
                        try {
                            val uri = Uri.parse(entity.uriString)
                            // Proactively check if stream is openable, skip if invalid/corrupt
                            applicationContext.contentResolver.openInputStream(uri)?.use {} 
                                ?: throw java.io.FileNotFoundException("Could not open input stream")

                            loadChannel.send(LoadedImage(entity, uri))

                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            Log.e(TAG, "Stage1 load failed for ${entity.name}: ${e.message}")
                            if (e is java.io.FileNotFoundException) {
                                runCatching { 
                                    db.mediaDao().updateOcrIndexStatus(entity.id, true)
                                }
                            }
                        }
                    }
                    loadChannel.close()
                }

                launch(Dispatchers.Default) {
                    val textRecognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                        com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
                    )

                    for (loaded in loadChannel) {
                        if (isStopped) break
                        try {
                            val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(
                                applicationContext,
                                loaded.uri
                            )
                            val extractedText = suspendCancellableCoroutine { cont ->
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        cont.resumeWith(kotlin.Result.success(visionText.text))
                                    }
                                    .addOnFailureListener {
                                        cont.resumeWith(kotlin.Result.success(null))
                                    }
                            }

                            embedChannel.send(EmbeddedImage(loaded.entity, extractedText))

                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            Log.e(TAG, "Stage2 inference failed for ${loaded.entity.name}: ${e.message}")
                            // Do not mark as true, allowing it to be retried in the next worker pass
                        }
                    }
                    embedChannel.close()
                }

                launch(Dispatchers.IO) {
                    val ocrIndexedBatch  = mutableListOf<Long>()

                    suspend fun flush() {
                        if (ocrIndexedBatch.isNotEmpty()) {
                            db.mediaDao().markOcrIndexed(ocrIndexedBatch.toList())
                        }
                        ocrIndexedBatch.clear()
                    }

                    for (embedded in embedChannel) {
                        if (isStopped) break

                        if (!embedded.extractedText.isNullOrBlank()) {
                            DatabaseProvider.insertFtsRow(db, embedded.entity.id, embedded.extractedText, "")
                            ocrIndexedBatch.add(embedded.entity.id)
                        } else if (embedded.extractedText != null || !embedded.entity.isIndexedOcr) {
                            ocrIndexedBatch.add(embedded.entity.id)
                        }

                        processedCount++
                        recentUris.addLast(embedded.entity.uriString)
                        if (recentUris.size > 5) recentUris.removeFirst()

                        if (ocrIndexedBatch.size >= WRITE_BATCH_SIZE) {
                            flush()
                        }

                        val current = alreadyIndexed + processedCount
                        setProgress(workDataOf(
                            "progress" to current,
                            "total" to totalImageCount,
                            "recent_uris" to recentUris.toTypedArray()
                        ))
                        setForeground(createForegroundInfo("$current / $totalImageCount images"))
                    }

                    flush()
                }
            } 

            Log.d(TAG, "OCR indexing complete. Indexed $processedCount images.")
            return Result.success()

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "OcrIndexWorker failed fatally: ${e.message}")
            e.printStackTrace()
            return Result.retry()
        }
    }
}
