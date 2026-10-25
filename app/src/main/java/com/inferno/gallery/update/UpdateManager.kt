package com.inferno.gallery.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.inferno.gallery.BuildConfig
import com.inferno.gallery.data.network.UpdateChecker
import com.inferno.gallery.data.network.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    data object Idle : UpdateState()
    data class Checking(val manual: Boolean) : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class Error(val message: String, val manual: Boolean) : UpdateState()
}

object UpdateManager {
    private const val TAG = "UpdateManager"

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var downloadJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkForUpdates(scope: CoroutineScope, manual: Boolean = false, context: Context? = null) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) {
            return
        }

        _state.value = UpdateState.Checking(manual = manual)

        scope.launch {
            try {
                val currentVersion = BuildConfig.VERSION_NAME
                val updateInfo = UpdateChecker.checkForUpdates(currentVersion)

                if (updateInfo != null && updateInfo.isAvailable) {
                    _state.value = UpdateState.UpdateAvailable(updateInfo)
                    if (context != null) {
                        try {
                            UpdateNotificationHelper.showUpdateNotification(context.applicationContext, updateInfo)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to post update notification: ${e.message}")
                        }
                    }
                } else if (updateInfo != null && !updateInfo.isAvailable) {
                    if (manual) {
                        _state.value = UpdateState.UpToDate(currentVersion)
                    } else {
                        _state.value = UpdateState.Idle
                    }
                } else {
                    if (manual) {
                        _state.value = UpdateState.Error("Could not check for updates. Check internet connection.", manual = true)
                    } else {
                        _state.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Check for updates error: ${e.message}", e)
                if (manual) {
                    _state.value = UpdateState.Error(e.message ?: "Failed to check for updates", manual = true)
                } else {
                    _state.value = UpdateState.Idle
                }
            }
        }
    }

    fun downloadAndInstall(context: Context, info: UpdateInfo, scope: CoroutineScope) {
        val downloadUrl = info.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            // Fallback: open release page in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releasePageUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open browser: ${e.message}")
            }
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val targetFileName = info.apkFileName ?: "PhotonGallery-${info.latestVersion}.apk"
                val apkFile = File(updatesDir, targetFileName)

                // If already downloaded and valid, skip download
                if (apkFile.exists() && info.apkSizeBytes > 0 && apkFile.length() == info.apkSizeBytes) {
                    withContext(Dispatchers.Main) {
                        _state.value = UpdateState.ReadyToInstall(info, apkFile)
                        installApk(context, apkFile)
                    }
                    return@launch
                }

                _state.value = UpdateState.Downloading(info, 0f, 0L, info.apkSizeBytes)

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "PhotonGallery-App")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Server returned HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IllegalStateException("Empty response body")
                    val totalBytes = if (body.contentLength() > 0) body.contentLength() else info.apkSizeBytes
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(apkFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            var lastUpdateMillis = System.currentTimeMillis()

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                val now = System.currentTimeMillis()
                                if (now - lastUpdateMillis > 150) {
                                    lastUpdateMillis = now
                                    val progress = if (totalBytes > 0) {
                                        (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                                    } else 0f

                                    _state.value = UpdateState.Downloading(
                                        info = info,
                                        progress = progress,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _state.value = UpdateState.ReadyToInstall(info, apkFile)
                        installApk(context, apkFile)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download update: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _state.value = UpdateState.Error("Download failed: ${e.message}", manual = true)
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "Cannot install: APK file does not exist at ${apkFile.absolutePath}")
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _state.value = UpdateState.Error("Failed to start installer: ${e.message}", manual = true)
        }
    }

    fun dismiss() {
        downloadJob?.cancel()
        _state.value = UpdateState.Idle
    }

    fun setUpdateAvailable(info: UpdateInfo) {
        _state.value = UpdateState.UpdateAvailable(info)
    }
}
