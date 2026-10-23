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
import com.inferno.gallery.data.ai.FaceModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class FaceModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "FaceModelDownloadWorker"
        private const val NOTIFICATION_ID = 56
        private const val CHANNEL_ID = "face_model_download_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Preparing face recognition model download…")

    private fun createForegroundInfo(progress: String, progressPercent: Int = -1): ForegroundInfo {
        val context = applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Face Model Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows progress while downloading face recognition models" }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Face Recognition Setup")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)

        if (progressPercent in 0..100) {
            builder.setProgress(100, progressPercent, false)
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
        try {
            setForeground(createForegroundInfo("Connecting…"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set foreground info: ${e.message}")
        }
        setProgress(workDataOf("progress" to 0))

        val targetDir = File(context.filesDir, FaceModelRegistry.MODEL_DIR).apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(targetDir, FaceModelRegistry.MODEL_NAME)

        // 1. Check if model already exists and is valid
        if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024L) {
            val valid = verifyChecksum(targetFile, FaceModelRegistry.MODEL_SHA256)
            if (valid) {
                Log.d(TAG, "Face model already downloaded and verified.")
                setProgress(workDataOf("progress" to 100))
                return@withContext Result.success()
            } else {
                Log.w(TAG, "Existing face model corrupt, redownloading.")
                targetFile.delete()
            }
        }

        // 2. Check bundled models in assets or project directory if present
        try {
            val assetList = context.assets.list("models") ?: emptyArray()
            if (assetList.contains(FaceModelRegistry.MODEL_NAME)) {
                context.assets.open("models/${FaceModelRegistry.MODEL_NAME}").use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (verifyChecksum(targetFile, FaceModelRegistry.MODEL_SHA256)) {
                    Log.d(TAG, "Copied face model from assets.")
                    setProgress(workDataOf("progress" to 100))
                    return@withContext Result.success()
                }
            }
        } catch (_: Exception) {}

        // 3. Download from CDN
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        val tempFile = File(targetDir, "${FaceModelRegistry.MODEL_NAME}.tmp")
        try {
            val request = Request.Builder()
                .url(FaceModelRegistry.MODEL_URL)
                .header("User-Agent", "PhotonGallery-ModelDownloader/2.1.3")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with HTTP ${response.code}")
                return@withContext Result.retry()
            }

            val body = response.body ?: return@withContext Result.retry()
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else FaceModelRegistry.MODEL_SIZE

            var downloadedBytes = 0L
            val buffer = ByteArray(32 * 1024)

            body.byteStream().use { input: InputStream ->
                BufferedOutputStream(FileOutputStream(tempFile)).use { output ->
                    var bytesRead: Int
                    var lastUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            tempFile.delete()
                            return@withContext Result.failure()
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 500) {
                            lastUpdate = now
                            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            setProgress(workDataOf("progress" to percent))
                            try {
                                setForeground(createForegroundInfo("Downloading MobileFaceNet: $percent%", percent))
                            } catch (_: Exception) {}
                        }
                    }
                    output.flush()
                }
            }

            if (!verifyChecksum(tempFile, FaceModelRegistry.MODEL_SHA256)) {
                Log.e(TAG, "SHA-256 verification failed for downloaded face model.")
                tempFile.delete()
                return@withContext Result.retry()
            }

            if (tempFile.renameTo(targetFile)) {
                Log.d(TAG, "Face model downloaded and verified successfully.")
                Result.success()
            } else {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face model download failed: ${e.message}", e)
            tempFile.delete()
            Result.retry()
        }
    }

    private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(expectedSha256, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
