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

        private const val TEXT_MODEL_URL = "https://raw.githubusercontent.com/Bn5prS/Photon_Gallery/master/models/text_model_quantized.onnx"
        private const val VISION_MODEL_URL = "https://raw.githubusercontent.com/Bn5prS/Photon_Gallery/master/models/vision_model_quantized.onnx"
        private const val FACE_MODEL_URL = "https://raw.githubusercontent.com/Bn5prS/Photon_Gallery/master/models/ghostfacenet_v1.onnx"
        private const val TOKENIZER_URL = "https://raw.githubusercontent.com/Bn5prS/Photon_Gallery/master/models/tokenizer.json"

        private const val TEXT_MODEL_NAME = "text_model_quantized.onnx"
        private const val VISION_MODEL_NAME = "vision_model_quantized.onnx"
        private const val FACE_MODEL_NAME = "ghostfacenet_v1.onnx"
        private const val TOKENIZER_NAME = "tokenizer.json"

        private const val TEXT_MODEL_SHA256 = "9106b51e6c663a56b99182ec617c2b3d53577b037e7e24a7717eb78048a0c97a"
        private const val VISION_MODEL_SHA256 = "44eece4fe5fe4e0359a88268a327adf758633a1aade3917690b952bef1501f96"
        private const val FACE_MODEL_SHA256 = "f72f85aa3ec2e8cfa5746606aab4589f325792e20c8fb3e51e7d8d6ba4134ae2"
        private const val TOKENIZER_SHA256 = "72ed5c96db5729294468543e4bc75fce14ca63f58e37300290189ba1c1e52b85"
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
        try {
            setForeground(createForegroundInfo("Connecting…"))

            val modelDir = File(applicationContext.filesDir, "smart_search_model_v2")
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
                DownloadTarget(TOKENIZER_URL, TOKENIZER_NAME, 2_224_081L, TOKENIZER_SHA256),
                DownloadTarget(TEXT_MODEL_URL, TEXT_MODEL_NAME, 64_504_507L, TEXT_MODEL_SHA256),
                DownloadTarget(VISION_MODEL_URL, VISION_MODEL_NAME, 87_461_602L, VISION_MODEL_SHA256)
            )

            val totalBytesExpected = downloadTargets.sumOf { it.expectedSize }
            var totalBytesDownloaded = 0L
            var lastReportedPercent = -1

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

                                val overallPercent = ((totalBytesDownloaded.toFloat() / totalBytesExpected.toFloat()) * 100).toInt().coerceIn(0, 100)
                                if (overallPercent > lastReportedPercent) {
                                    lastReportedPercent = overallPercent
                                    setProgress(workDataOf("progress" to overallPercent))
                                    try {
                                        setForeground(createForegroundInfo("Downloaded $overallPercent% ($fileName)"))
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to set foreground info: ${e.message}")
                                    }
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
                com.inferno.gallery.data.SettingsRepository.getInstance(applicationContext)
                    .smartSearchAutoIndexFlow
                    .first() && db.embeddingDao().getUnindexedMediaIds().isNotEmpty()
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
