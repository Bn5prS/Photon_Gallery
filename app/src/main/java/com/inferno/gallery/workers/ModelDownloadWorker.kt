package com.inferno.gallery.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ModelDownloadWorker"
        private const val NOTIFICATION_ID = 55
        private const val CHANNEL_ID = "model_download_channel"

        const val BASE_MODEL_URL = "https://huggingface.co/RuteNL/MobileCLIP2-S2-OpenCLIP-ONNX/resolve/main"
        const val TEXT_MODEL_URL = "$BASE_MODEL_URL/text.onnx"
        const val TEXT_DATA_URL = "$BASE_MODEL_URL/text.onnx.data"
        const val VISION_MODEL_URL = "$BASE_MODEL_URL/visual.onnx"
        const val VISION_DATA_URL = "$BASE_MODEL_URL/visual.onnx.data"
        const val TOKENIZER_URL = "$BASE_MODEL_URL/tokenizer.json"

        const val TEXT_MODEL_NAME = "text.onnx"
        const val TEXT_DATA_NAME = "text.onnx.data"
        const val VISION_MODEL_NAME = "visual.onnx"
        const val VISION_DATA_NAME = "visual.onnx.data"
        const val TOKENIZER_NAME = "tokenizer.json"

        const val VISION_MODEL_SIZE = 2_310_022L
        const val VISION_DATA_SIZE = 143_589_376L
        const val TEXT_MODEL_SIZE = 1_517_435L
        const val TEXT_DATA_SIZE = 253_755_392L
        const val TOKENIZER_SIZE = 3_642_035L

        const val TEXT_MODEL_SHA256 = "c7046e2a3c4078fd0b10865f3063bfa8e89c0808de3bd2fcaf89ba0d0a2fe90f"
        const val TEXT_DATA_SHA256 = "b0e6225b1a9e8b15b1abedcc430ebb369c9695831c9b0922acf9e83906e03e47"
        const val VISION_MODEL_SHA256 = "8bb691c4085f8767c8237e324816a430f8264b5c2d4542ec2d9f88d701faa823"
        const val VISION_DATA_SHA256 = "365f1b95476a0852aaf5b136432383ff9405eedc3deb5347c2b8398a3838f50c"
        const val TOKENIZER_SHA256 = "43d4281659ce30b6aca2c7485944b3d73925bb7f87c7234daf3b6b094a6d618c"
        const val MODEL_DIR = "smart_search_model_v7"
        const val SEMANTIC_MODEL_VERSION = 7
    }

    private data class DownloadTarget(
        val url: String,
        val fileName: String,
        val expectedSize: Long,
        val expectedSha256: String
    )

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Preparing high-speed model download…")

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val context = applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Smart Search Model Setup",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows progress while downloading AI model files" }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading AI Vision Models")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun verifySha256(file: File, expected: String): Boolean {
        if (expected.isBlank()) return true
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                var read = input.read(buffer)
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual.equals(expected, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Checksum verification failed for ${file.name}: ${e.message}")
            false
        }
    }

    override suspend fun doWork(): Result {
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(NOTIFICATION_ID)
        try {
            // Clean up any legacy model directories from previous versions
            listOf("smart_search_model_v1", "smart_search_model_v2", "smart_search_model_v3", "smart_search_model_v4", "smart_search_model_v5", "smart_search_model_v6").forEach { legacyDirName ->
                val legacyDir = File(applicationContext.filesDir, legacyDirName)
                if (legacyDir.exists()) {
                    try { legacyDir.deleteRecursively() } catch (_: Exception) {}
                }
            }

            val modelDir = File(applicationContext.filesDir, MODEL_DIR)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }

            // High-throughput OkHttpClient with connection pool
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build()

            val downloadTargets = listOf(
                DownloadTarget(TOKENIZER_URL, TOKENIZER_NAME, TOKENIZER_SIZE, TOKENIZER_SHA256),
                DownloadTarget(TEXT_MODEL_URL, TEXT_MODEL_NAME, TEXT_MODEL_SIZE, TEXT_MODEL_SHA256),
                DownloadTarget(TEXT_DATA_URL, TEXT_DATA_NAME, TEXT_DATA_SIZE, TEXT_DATA_SHA256),
                DownloadTarget(VISION_MODEL_URL, VISION_MODEL_NAME, VISION_MODEL_SIZE, VISION_MODEL_SHA256),
                DownloadTarget(VISION_DATA_URL, VISION_DATA_NAME, VISION_DATA_SIZE, VISION_DATA_SHA256)
            )

            val totalBytesExpected = downloadTargets.sumOf { it.expectedSize }
            var totalBytesDownloaded = 0L
            var lastReportedPercent = -1
            var lastSpeedTimestamp = System.currentTimeMillis()
            var lastSpeedBytes = 0L
            var currentSpeedBytesPerSec = 0L

            for (target in downloadTargets) {
                if (isStopped) break
                val url = target.url
                val fileName = target.fileName

                val finalFile = File(modelDir, fileName)
                // If it already exists and size matches, verify hash before skipping
                if (finalFile.exists() && finalFile.length() > target.expectedSize * 0.95) {
                    if (verifySha256(finalFile, target.expectedSha256)) {
                        totalBytesDownloaded += finalFile.length()
                        Log.d(TAG, "$fileName already exists and checksum matches, skipping.")
                        continue
                    } else {
                        Log.w(TAG, "$fileName checksum mismatch. Re-downloading.")
                        finalFile.delete()
                    }
                }

                val tempFile = File(modelDir, "$fileName.tmp")
                if (tempFile.exists()) tempFile.delete()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "PhotonGallery-ModelDownloader/2.0")
                    .build()

                val response = client.newCall(request).execute()
                response.use { res ->
                    if (!res.isSuccessful) {
                        throw java.io.IOException("Failed to download $fileName: HTTP code ${res.code}")
                    }

                    val body = res.body ?: throw java.io.IOException("Empty response body for $fileName")
                    val inputStream = body.byteStream()
                    val outputStream = BufferedOutputStream(FileOutputStream(tempFile), 131072) // 128KB buffer
                    val digest = MessageDigest.getInstance("SHA-256")

                    val buffer = ByteArray(131072) // 128KB high-speed buffer
                    var bytesRead: Int

                    inputStream.use { input ->
                        outputStream.use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (isStopped) {
                                    tempFile.delete()
                                    break
                                }
                                output.write(buffer, 0, bytesRead)
                                digest.update(buffer, 0, bytesRead)
                                totalBytesDownloaded += bytesRead

                                val now = System.currentTimeMillis()
                                val timeDelta = now - lastSpeedTimestamp
                                if (timeDelta >= 500L) {
                                    val bytesDelta = totalBytesDownloaded - lastSpeedBytes
                                    if (bytesDelta >= 0) {
                                        currentSpeedBytesPerSec = (bytesDelta * 1000L) / timeDelta
                                    }
                                    lastSpeedTimestamp = now
                                    lastSpeedBytes = totalBytesDownloaded
                                }

                                val overallPercent = ((totalBytesDownloaded.toFloat() / totalBytesExpected.toFloat()) * 100).toInt().coerceIn(0, 100)
                                if (overallPercent > lastReportedPercent) {
                                    lastReportedPercent = overallPercent
                                    setProgress(
                                        workDataOf(
                                            "progress" to overallPercent,
                                            "bytesDownloaded" to totalBytesDownloaded,
                                            "totalBytes" to totalBytesExpected,
                                            "speedBytesPerSec" to currentSpeedBytesPerSec
                                        )
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (!isStopped && tempFile.exists()) {
                        val downloadedSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                        if (target.expectedSha256.isNotBlank() && !downloadedSha256.equals(target.expectedSha256, ignoreCase = true)) {
                            tempFile.delete()
                            throw java.io.IOException("Checksum verification failed for $fileName: expected ${target.expectedSha256} but got $downloadedSha256")
                        }

                        if (finalFile.exists()) finalFile.delete()
                        tempFile.renameTo(finalFile)
                    }
                }
            }

            if (isStopped) {
                Log.d(TAG, "Download worker stopped before completion.")
                return Result.failure()
            }

            // Clear all old embeddings so they get re-calculated with the new model
            val db = DatabaseProvider.getDatabase(applicationContext)
            val shouldAutoIndex = withContext(Dispatchers.IO) {
                db.embeddingDao().clearAllEmbeddings()
                db.embeddingStatusDao().clearAllStatuses()
                val settingsRepo = com.inferno.gallery.data.SettingsRepository.getInstance(applicationContext)
                settingsRepo.updateSmartSearchAutoIndex(true)
                val (aiConfig, galleryExcluded) = com.inferno.gallery.data.ai.AiIndexingSupport.loadScope(applicationContext)
                settingsRepo.smartSearchAutoIndexFlow.first() &&
                    com.inferno.gallery.data.ai.AiIndexingSupport.getEligibleUnindexedSmartCount(
                        db,
                        aiConfig,
                        galleryExcluded
                    ) > 0
            }

            if (shouldAutoIndex) {
                val indexRequest = androidx.work.OneTimeWorkRequestBuilder<SmartSearchIndexWorker>().build()
                androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "SmartSearchIndexWorker",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    indexRequest
                )
            }

            Log.d(TAG, "High-speed model download completed successfully. Cleared old database embeddings for re-indexing.")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "ModelDownloadWorker failed fatally: ${e.message}", e)
            return Result.retry()
        }
    }
}
