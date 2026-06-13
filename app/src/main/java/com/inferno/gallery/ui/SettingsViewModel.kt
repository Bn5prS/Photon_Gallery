package com.inferno.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.DockStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import com.inferno.gallery.workers.OcrIndexWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val db = DatabaseProvider.getDatabase(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.onboardingCompletedFlow.first()
            val autoClean = repository.autoCleanTrashEnabledFlow.first()
            val days = repository.autoCleanTrashDaysFlow.first()
            setupAutoCleanTrashWorker(autoClean, days)
            _isLoading.value = false
        }
    }

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow.map { modeString ->
        try {
            ThemeMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode.name)
        }
    }

    val useMaterialYou: StateFlow<Boolean> = repository.useMaterialYouFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setUseMaterialYou(use: Boolean) {
        viewModelScope.launch {
            repository.updateUseMaterialYou(use)
        }
    }

    val useAmoledBlack: StateFlow<Boolean> = repository.useAmoledBlackFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setUseAmoledBlack(use: Boolean) {
        viewModelScope.launch {
            repository.updateUseAmoledBlack(use)
        }
    }

    val useFullScreen: StateFlow<Boolean> = repository.useFullScreenFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setUseFullScreen(use: Boolean) {
        viewModelScope.launch {
            repository.updateUseFullScreen(use)
        }
    }

    val dockStyle: StateFlow<DockStyle> = repository.dockStyleFlow.map { modeString ->
        try {
            DockStyle.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            DockStyle.PILL
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockStyle.PILL
    )

    fun setDockStyle(style: DockStyle) {
        viewModelScope.launch {
            repository.updateDockStyle(style)
        }
    }

    val thumbnailCornerRadius: StateFlow<Float> = repository.thumbnailCornerRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    fun setThumbnailCornerRadius(radius: Float) {
        viewModelScope.launch {
            repository.updateThumbnailCornerRadius(radius)
        }
    }

    val totalImagesCount: StateFlow<Int> = db.mediaDao().observeTotalImageCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val unindexedOcrImagesCount: StateFlow<Int> = db.mediaDao().observeUnindexedOcrImageCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val ocrProgress = com.inferno.gallery.data.IndexingProgressManager.ocrProgress

    val ocrIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("OcrIndexWorker")
        .map { it.firstOrNull() }

    fun startOcrIndexing() {
        viewModelScope.launch {
            repository.updateOcrIndexingEnabled(true)
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun stopOcrIndexing() {
        viewModelScope.launch {
            repository.updateOcrIndexingEnabled(false)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("OcrIndexWorker")
        }
    }

    fun rebuildOcrIndex() {
        viewModelScope.launch {
            repository.updateOcrIndexingEnabled(true)
            db.mediaDao().resetOcrIndexStatus()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL("DELETE FROM image_fts")
            }
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.REPLACE, request)
        }
    }

    val telegramBotToken: StateFlow<String> = repository.telegramBotTokenFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val telegramBotTokens: StateFlow<List<String>> = repository.telegramBotTokensFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val telegramChatId: StateFlow<String> = repository.telegramChatIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val telegramBackupEnabled: StateFlow<Boolean> = repository.telegramBackupEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val telegramStripLocation: StateFlow<Boolean> = repository.telegramStripLocationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val telegramAutoBackupFolders: StateFlow<Set<String>> = repository.telegramAutoBackupFoldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val telegramAutoBackupWifiOnly: StateFlow<Boolean> = repository.telegramAutoBackupWifiOnlyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val telegramAutoBackupBatteryLowPause: StateFlow<Boolean> = repository.telegramAutoBackupBatteryLowPauseFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val allBucketNames: StateFlow<List<String>> = db.mediaDao().observeAllBucketNames().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTelegramBotToken(token: String) {
        viewModelScope.launch {
            repository.updateTelegramBotToken(token)
        }
    }

    fun setTelegramBotTokens(tokens: List<String>) {
        viewModelScope.launch {
            repository.updateTelegramBotTokens(tokens)
        }
    }

    fun saveTelegramCredentials(tokens: List<String>, chatId: String) {
        viewModelScope.launch {
            repository.updateTelegramBotTokens(tokens)
            repository.updateTelegramChatId(chatId)
            val primaryToken = tokens.firstOrNull()
            if (!primaryToken.isNullOrBlank() && chatId.isNotBlank()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        android.util.Log.i("SettingsViewModel", "Auto-restoring cloud backups on credentials update...")
                        com.inferno.gallery.data.SyncManifestManager.restoreFromManifest(getApplication(), primaryToken, chatId)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsViewModel", "Failed to auto-restore from manifest: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun setTelegramChatId(chatId: String) {
        viewModelScope.launch {
            repository.updateTelegramChatId(chatId)
            val tokens = repository.telegramBotTokensFlow.first()
            val primaryToken = tokens.firstOrNull()
            if (!primaryToken.isNullOrBlank() && chatId.isNotBlank()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        android.util.Log.i("SettingsViewModel", "Auto-restoring cloud backups on credentials update...")
                        com.inferno.gallery.data.SyncManifestManager.restoreFromManifest(getApplication(), primaryToken, chatId)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsViewModel", "Failed to auto-restore from manifest: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun setTelegramStripLocation(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateTelegramStripLocation(enabled)
        }
    }

    fun setTelegramAutoBackupFolders(folders: Set<String>) {
        viewModelScope.launch {
            repository.updateTelegramAutoBackupFolders(folders)
            syncAutoBackupQueue()
        }
    }

    fun setTelegramAutoBackupWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            repository.updateTelegramAutoBackupWifiOnly(wifiOnly)
            if (telegramBackupEnabled.value) {
                rescheduleTelegramBackup(wifiOnly)
            }
        }
    }

    fun setTelegramAutoBackupBatteryLowPause(pause: Boolean) {
        viewModelScope.launch {
            repository.updateTelegramAutoBackupBatteryLowPause(pause)
        }
    }

    private fun syncAutoBackupQueue() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val enabled = repository.telegramBackupEnabledFlow.first()
            val folders = repository.telegramAutoBackupFoldersFlow.first()
            val backupDao = db.telegramBackupDao()
            val allMedia = db.mediaDao().getAllMedia()
            val mediaMap = allMedia.associateBy { it.id }

            // 1. Clean up pending backups that are no longer selected
            val allBackups = backupDao.observeAllBackups().first()
            val pendingBackups = allBackups.filter { it.backupStatus == "PENDING" }
            for (pending in pendingBackups) {
                val media = mediaMap[pending.mediaId]
                if (media == null) {
                    backupDao.deleteBackup(pending.mediaId)
                    continue
                }
                if (!enabled || !folders.contains(media.bucketName)) {
                    android.util.Log.d("SettingsViewModel", "Removing cancelled auto-backup: ${media.name} (folder: ${media.bucketName})")
                    backupDao.deleteBackup(pending.mediaId)
                }
            }

            if (!enabled || folders.isEmpty()) return@launch

            // 2. Queue existing backups for newly selected folders (skipping files > 50MB limit)
            val updatedBackups = backupDao.observeAllBackups().first().associateBy { it.mediaId }
            val toQueue = allMedia.filter { media ->
                folders.contains(media.bucketName) && !updatedBackups.containsKey(media.id) && media.size <= 50 * 1024 * 1024L
            }

            if (toQueue.isNotEmpty()) {
                android.util.Log.d("SettingsViewModel", "Retroactively queueing ${toQueue.size} items for auto backup.")
                for (item in toQueue) {
                    backupDao.insertOrUpdate(
                        com.inferno.gallery.data.db.TelegramBackupEntity(
                            mediaId = item.id,
                            telegramFileId = null,
                            telegramThumbFileId = null,
                            telegramMessageId = null,
                            backupStatus = "PENDING",
                            backupTimestamp = System.currentTimeMillis()
                        )
                    )
                }

                val wifiOnly = repository.telegramAutoBackupWifiOnlyFlow.first()
                rescheduleTelegramBackup(wifiOnly)
            }
        }
    }

    private fun rescheduleTelegramBackup(wifiOnly: Boolean) {
        val constraints = androidx.work.Constraints.Builder().apply {
            if (wifiOnly) {
                setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
            } else {
                setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            }
        }.build()
        val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.TelegramBackupWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "TelegramBackupWorker",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun setTelegramBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateTelegramBackupEnabled(enabled)
            if (enabled) {
                syncAutoBackupQueue()
                val wifiOnly = repository.telegramAutoBackupWifiOnlyFlow.first()
                rescheduleTelegramBackup(wifiOnly)
            } else {
                syncAutoBackupQueue()
                WorkManager.getInstance(getApplication()).cancelUniqueWork("TelegramBackupWorker")
            }
        }
    }

    private val _connectionTestState = MutableStateFlow<ConnectionTestResult?>(null)
    val connectionTestState: StateFlow<ConnectionTestResult?> = _connectionTestState.asStateFlow()

    fun testTelegramConnection(token: String, chatId: String) {
        if (token.isBlank() || chatId.isBlank()) {
            _connectionTestState.value = ConnectionTestResult.Error("Credentials are empty")
            return
        }

        _connectionTestState.value = ConnectionTestResult.Testing
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient()
                    val requestBody = okhttp3.MultipartBody.Builder()
                        .setType(okhttp3.MultipartBody.FORM)
                        .addFormDataPart("chat_id", chatId)
                        .addFormDataPart("text", "Photon Gallery connection test successful!")
                        .build()
                    val request = okhttp3.Request.Builder()
                        .url("https://api.telegram.org/bot$token/sendMessage")
                        .header("User-Agent", "PhotonGalleryApp/1.0 (Android; Jetpack Compose)")
                        .post(requestBody)
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            val json = try { org.json.JSONObject(bodyString) } catch (e: Exception) { null }
                            val description = json?.optString("description") ?: ""
                            val migrateToChatId = json?.optJSONObject("parameters")?.optLong("migrate_to_chat_id", 0L) ?: 0L
                            if (migrateToChatId != 0L) {
                                repository.updateTelegramChatId(migrateToChatId.toString())
                                ConnectionTestResult.Migrated(migrateToChatId.toString())
                            } else {
                                val errorDetail = if (description.isNotBlank()) description else "HTTP error: ${response.code}"
                                ConnectionTestResult.Error(errorDetail)
                            }
                        } else {
                            val json = org.json.JSONObject(bodyString)
                            if (json.getBoolean("ok")) {
                                ConnectionTestResult.Success
                            } else {
                                ConnectionTestResult.Error(json.optString("description", "Unknown error"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    ConnectionTestResult.Error(e.message ?: "Connection failed")
                }
            }
            _connectionTestState.value = result
        }
    }

    fun clearConnectionTestResult() {
        _connectionTestState.value = null
    }

    fun restoreFromManifest(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val token = repository.telegramBotTokensFlow.first().firstOrNull()
                val chatId = repository.telegramChatIdFlow.first()
                if (token.isNullOrBlank() || chatId.isBlank()) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Please configure credentials first.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Restoring from cloud...", android.widget.Toast.LENGTH_SHORT).show()
                }
                val success = com.inferno.gallery.data.SyncManifestManager.restoreFromManifest(context, token, chatId)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (success) {
                        android.widget.Toast.makeText(context, "Restore successful!", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "No manifest found or restore failed.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Restore error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val stripMetadataOnShare: StateFlow<Boolean> = repository.stripMetadataOnShareFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setStripMetadataOnShare(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateStripMetadataOnShare(enabled)
        }
    }

    // ── Smart Search Integration ──

    val smartSearchAutoIndex: StateFlow<Boolean> = repository.smartSearchAutoIndexFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setSmartSearchAutoIndex(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSmartSearchAutoIndex(enabled)
        }
    }

    val smartSearchThreshold: StateFlow<Float> = repository.smartSearchThresholdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.20f
    )

    fun setSmartSearchThreshold(threshold: Float) {
        viewModelScope.launch {
            repository.updateSmartSearchThreshold(threshold)
        }
    }

    val unindexedSmartSearchCount: StateFlow<Int> = db.embeddingDao().observeUnindexedCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val smartSearchModelDownloaded: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication())
        while (true) {
            emit(searchEngine.isModelDownloaded())
            kotlinx.coroutines.delay(2000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(application).isModelDownloaded()
    )

    val clipProgress = com.inferno.gallery.data.IndexingProgressManager.clipProgress

    val smartSearchIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("SmartSearchIndexWorker")
        .map { it.firstOrNull() }

    val modelDownloadWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("ModelDownloadWorker")
        .map { it.firstOrNull() }

    fun startModelDownload() {
        val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.ModelDownloadWorker>()
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "ModelDownloadWorker",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun startSmartSearchIndexing() {
        val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.SmartSearchIndexWorker>()
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SmartSearchIndexWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun stopSmartSearchIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartSearchIndexWorker")
    }

    fun clearSmartSearchEmbeddings() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartSearchIndexWorker")
            db.embeddingDao().clearAllEmbeddings()
        }
    }

    fun deleteSmartSearchModel() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartSearchIndexWorker")
            val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication())
            searchEngine.close()
            val dir = searchEngine.getModelDir()
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }

    val confirmDeleteEnabled: StateFlow<Boolean> = repository.confirmDeleteEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setConfirmDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateConfirmDeleteEnabled(enabled)
        }
    }

    val autoplayWithSoundEnabled: StateFlow<Boolean> = repository.autoplayWithSoundEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setAutoplayWithSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoplayWithSoundEnabled(enabled)
        }
    }

    val autoCleanTrashEnabled: StateFlow<Boolean> = repository.autoCleanTrashEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setAutoCleanTrashEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoCleanTrashEnabled(enabled)
            setupAutoCleanTrashWorker(enabled, autoCleanTrashDays.value)
        }
    }

    val autoCleanTrashDays: StateFlow<Int> = repository.autoCleanTrashDaysFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 30
    )

    fun setAutoCleanTrashDays(days: Int) {
        viewModelScope.launch {
            repository.updateAutoCleanTrashDays(days)
            if (autoCleanTrashEnabled.value) {
                setupAutoCleanTrashWorker(true, days)
            }
        }
    }

    private fun setupAutoCleanTrashWorker(enabled: Boolean, days: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        if (enabled) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()
            
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.inferno.gallery.workers.AutoCleanTrashWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                "AutoCleanTrashWorker",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork("AutoCleanTrashWorker")
        }
    }

    val cacheThumbnailsEnabled: StateFlow<Boolean> = repository.cacheThumbnailsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setCacheThumbnailsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCacheThumbnailsEnabled(enabled)
            if (enabled) {
                val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.PrecacheThumbnailsWorker>()
                    .build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "PrecacheThumbnailsWorker",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } else {
                WorkManager.getInstance(getApplication()).cancelUniqueWork("PrecacheThumbnailsWorker")
            }
        }
    }

    val maxBrightnessEnabled: StateFlow<Boolean> = repository.maxBrightnessEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setMaxBrightnessEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMaxBrightnessEnabled(enabled)
        }
    }
}

sealed class ConnectionTestResult {
    object Testing : ConnectionTestResult()
    object Success : ConnectionTestResult()
    data class Migrated(val newChatId: String) : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}
