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
import com.inferno.gallery.workers.ClipIndexWorker
import com.inferno.gallery.workers.OcrIndexWorker
import kotlinx.coroutines.flow.Flow
import com.inferno.gallery.data.db.DatabaseProvider

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val db = DatabaseProvider.getDatabase(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    val unindexedClipImagesCount: StateFlow<Int> = db.mediaDao().observeUnindexedClipImageCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val unindexedOcrImagesCount: StateFlow<Int> = db.mediaDao().observeUnindexedOcrImageCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val clipIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("ClipIndexWorker")
        .map { it.firstOrNull() }

    val ocrIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("OcrIndexWorker")
        .map { it.firstOrNull() }

    fun startClipIndexing() {
        viewModelScope.launch {
            repository.updateClipIndexingEnabled(true)
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.ClipIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("ClipIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun stopClipIndexing() {
        viewModelScope.launch {
            repository.updateClipIndexingEnabled(false)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("ClipIndexWorker")
        }
    }

    fun rebuildClipIndex() {
        viewModelScope.launch {
            repository.updateClipIndexingEnabled(true)
            db.mediaDao().resetClipIndexStatus()
            db.searchDao().clearAllVectors()
            // In rebuild, we enqueue directly or call startClipIndexing. Calling startClipIndexing
            // is perfect since it already launches and updates datastore.
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.ClipIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("ClipIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun startOcrIndexing() {
        viewModelScope.launch {
            repository.updateOcrIndexingEnabled(true)
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.KEEP, request)
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
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }
}
