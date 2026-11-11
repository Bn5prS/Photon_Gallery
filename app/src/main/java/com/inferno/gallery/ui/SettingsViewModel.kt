package com.inferno.gallery.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.model.ExifStripAction
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.data.MemoriesCarouselStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import com.inferno.gallery.workers.OcrIndexWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.inferno.gallery.data.db.DatabaseProvider
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.inferno.gallery.update.UpdateManager
import com.inferno.gallery.update.UpdateState
import com.inferno.gallery.workers.UpdateCheckWorker

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository.getInstance(application)
    private val db = DatabaseProvider.getDatabase(application)


    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.migrateThemeDefaultsIfNeeded()
            repository.onboardingV210CompletedFlow.first()
            repository.onboardingCompletedFlow.first()
            val autoClean = repository.autoCleanTrashEnabledFlow.first()
            val days = repository.autoCleanTrashDaysFlow.first()
            setupAutoCleanTrashWorker(autoClean, days)
            // Pre-warm theme preferences so UI doesn't experience cold-start theme flips
            _useAmoledBlack.value = repository.useAmoledBlackFlow.first()
            repository.themeModeFlow.first()
            repository.useMaterialYouFlow.first()
            repository.colorPresetNameFlow.first()
            repository.appSeedColorFlow.first()
            repository.motionStyleFlow.first()
            repository.dateHeaderAlignmentFlow.first()
            _isLoading.value = false
        }

        viewModelScope.launch {
            repository.useAmoledBlackFlow.collect {
                _useAmoledBlack.value = it
            }
        }

        viewModelScope.launch {
            WorkManager.getInstance(application)
                .getWorkInfosForUniqueWorkFlow("ModelDownloadWorker")
                .collect { workInfos ->
                    val workInfo = workInfos.firstOrNull()
                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        repository.updateSmartSearchAutoIndex(true)
                    }
                }
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

    val timelineLayoutMode: StateFlow<com.inferno.gallery.data.TimelineLayoutMode> = repository.timelineLayoutModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID
    )

    fun setTimelineLayoutMode(mode: com.inferno.gallery.data.TimelineLayoutMode) {
        viewModelScope.launch {
            repository.updateTimelineLayoutMode(mode)
        }
    }

    val typographyStyle: StateFlow<String> = repository.typographyStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "expressive"
    )

    fun setTypographyStyle(style: String) {
        viewModelScope.launch {
            repository.updateTypographyStyle(style)
        }
    }

    val useSystemFont: StateFlow<Boolean> = repository.useSystemFontFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setUseSystemFont(use: Boolean) {
        viewModelScope.launch {
            repository.updateUseSystemFont(use)
        }
    }

    val fontScale: StateFlow<Float> = repository.fontScaleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            repository.updateFontScale(scale)
        }
    }

    val secureRecentsEnabled: StateFlow<Boolean> = repository.secureRecentsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setSecureRecentsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSecureRecentsEnabled(enabled)
        }
    }

    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHapticsEnabled(enabled)
        }
    }

    val hapticsStrength: StateFlow<Float> = repository.hapticsStrengthFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.5f
    )

    fun setHapticsStrength(strength: Float) {
        viewModelScope.launch {
            repository.updateHapticsStrength(strength)
        }
    }

    val viewerToolbarActions: StateFlow<List<String>> = repository.viewerToolbarActionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.ui.model.ViewerToolbarAction.DEFAULTS
    )

    val selectionDockActions: StateFlow<List<String>> = repository.selectionDockActionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.ui.model.SelectionDockAction.DEFAULTS
    )

    fun updateViewerToolbarActions(actions: List<String>) {
        viewModelScope.launch {
            repository.updateViewerToolbarActions(actions)
        }
    }

    fun updateSelectionDockActions(actions: List<String>) {
        viewModelScope.launch {
            repository.updateSelectionDockActions(actions)
        }
    }

    fun resetViewerToolbarActions() {
        viewModelScope.launch {
            repository.resetViewerToolbarActions()
        }
    }

    fun resetSelectionDockActions() {
        viewModelScope.launch {
            repository.resetSelectionDockActions()
        }
    }

    val useMaterialYou: StateFlow<Boolean> = repository.useMaterialYouFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )

    fun setUseMaterialYou(use: Boolean) {
        viewModelScope.launch {
            repository.updateUseMaterialYou(use)
        }
    }

    val colorPresetName: StateFlow<String> = repository.colorPresetNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "default_blue"
    )

    fun setColorPresetName(name: String) {
        viewModelScope.launch {
            repository.updateColorPresetName(name)
        }
    }

    private val _useAmoledBlack = MutableStateFlow(false)
    val useAmoledBlack: StateFlow<Boolean> = _useAmoledBlack.asStateFlow()

    fun setUseAmoledBlack(use: Boolean) {
        _useAmoledBlack.value = use
        viewModelScope.launch {
            repository.updateUseAmoledBlack(use)
        }
    }

    val motionStyle: StateFlow<String> = repository.motionStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "expressive"
    )

    fun setMotionStyle(style: String) {
        viewModelScope.launch {
            repository.updateMotionStyle(style)
        }
    }

    val dateHeaderAlignment: StateFlow<String> = repository.dateHeaderAlignmentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "LEFT"
    )

    fun setDateHeaderAlignment(alignment: String) {
        viewModelScope.launch {
            repository.updateDateHeaderAlignment(alignment)
        }
    }

    val appSeedColor: StateFlow<Int> = repository.appSeedColorFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0xFF0A6EFF.toInt() // Default Photon Blue
    )

    fun setAppSeedColor(color: Int) {
        viewModelScope.launch {
            repository.updateAppSeedColor(color)
        }
    }

    val themePaletteStyle: StateFlow<String> = repository.themePaletteStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "TonalSpot"
    )

    fun setThemePaletteStyle(style: String) {
        viewModelScope.launch {
            repository.updateThemePaletteStyle(style)
        }
    }

    val themeContrastLevel: StateFlow<Float> = repository.themeContrastLevelFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    fun setThemeContrastLevel(level: Float) {
        viewModelScope.launch {
            repository.updateThemeContrastLevel(level)
        }
    }

    val invertThemeColors: StateFlow<Boolean> = repository.invertThemeColorsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setInvertThemeColors(invert: Boolean) {
        viewModelScope.launch {
            repository.updateInvertThemeColors(invert)
        }
    }


    val animateThemeTransitions: StateFlow<Boolean> = repository.animateThemeTransitionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setAnimateThemeTransitions(animate: Boolean) {
        viewModelScope.launch { repository.updateAnimateThemeTransitions(animate) }
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

    val showHiddenAlbums: StateFlow<Boolean> = repository.showHiddenAlbumsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setShowHiddenAlbums(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowHiddenAlbums(show)
        }
    }

    val showEmptyAlbums: StateFlow<Boolean> = repository.showEmptyAlbumsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setShowEmptyAlbums(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowEmptyAlbums(show)
        }
    }

    val showMemories: StateFlow<Boolean> = repository.showMemoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setShowMemories(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowMemories(show)
        }
    }

    val memoriesDismissed: StateFlow<Boolean> = repository.memoriesDismissedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun restoreMemoriesBanner() {
        viewModelScope.launch {
            repository.updateMemoriesDismissed(false)
        }
    }

    val memoriesCarouselStyle: StateFlow<MemoriesCarouselStyle> = repository.memoriesCarouselStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MemoriesCarouselStyle.SPOTLIGHT
    )

    fun setMemoriesCarouselStyle(style: MemoriesCarouselStyle) {
        viewModelScope.launch {
            repository.updateMemoriesCarouselStyle(style)
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

    val showVideosInDock: StateFlow<Boolean> = repository.showVideosInDockFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setShowVideosInDock(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowVideosInDock(show)
        }
    }

    val thumbnailCornerRadius: StateFlow<Float> = repository.thumbnailCornerRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    fun setThumbnailCornerRadius(radius: Float) {
        viewModelScope.launch {
            repository.updateThumbnailCornerRadius(radius.coerceIn(0f, 16f))
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

    val ocrIndexingEnabled: StateFlow<Boolean> = repository.ocrIndexingEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setOcrIndexingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateOcrIndexingEnabled(enabled)
            if (enabled) {
                val hasUnindexed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    db.mediaDao().getUnindexedImageCount() > 0
                }
                if (hasUnindexed) {
                    startOcrIndexing()
                }
            }
        }
    }

    val ocrIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("OcrIndexWorker")
        .map { it.firstOrNull() }

    fun startOcrIndexing() {
        viewModelScope.launch {
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun stopOcrIndexing() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("OcrIndexWorker")
        }
    }

    fun rebuildOcrIndex() {
        viewModelScope.launch {
            db.mediaDao().resetOcrIndexStatus()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL("DELETE FROM image_fts")
                db.mediaDao().clearDocumentClassifications()
            }
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.REPLACE, request)
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

    val exifStripAction: StateFlow<ExifStripAction> = repository.exifStripActionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExifStripAction.ASK_EVERY_TIME
    )

    fun setExifStripAction(action: ExifStripAction) {
        viewModelScope.launch {
            repository.setExifStripAction(action)
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
            if (enabled) {
                val shouldIndex = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication())
                    searchEngine.isModelDownloaded() && db.embeddingDao().getUnindexedMediaIds().isNotEmpty()
                }
                if (shouldIndex) {
                    startSmartSearchIndexing()
                }
            }
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
            delay(10000)
        }
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
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

    fun cancelModelDownload() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("ModelDownloadWorker")
    }

    fun startSmartSearchIndexing() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.embeddingStatusDao().clearAllStatuses()
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.SmartSearchIndexWorker>()
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "SmartSearchIndexWorker",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun stopSmartSearchIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartSearchIndexWorker")
    }

    fun clearSmartSearchEmbeddings() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("SmartSearchIndexWorker")
            db.embeddingDao().clearAllEmbeddings()
            db.embeddingStatusDao().clearAllStatuses()
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

    // ── Face Recognition Integration ──

    val faceRecognitionEnabled: StateFlow<Boolean> = repository.faceRecognitionEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setFaceRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateFaceRecognitionEnabled(enabled)
            if (enabled) {
                val engine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
                if (engine.isModelDownloaded()) {
                    startFaceIndexing()
                } else {
                    startFaceModelDownload()
                }
            } else {
                stopFaceIndexing()
            }
        }
    }

    val unindexedFacesCount: StateFlow<Int> = db.faceDao().observeUnindexedCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val faceModelDownloaded: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        val engine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
        while (true) {
            emit(engine.isModelDownloaded())
            delay(5000)
        }
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val faceProgress = com.inferno.gallery.data.IndexingProgressManager.faceProgress

    val faceAutoIndex: StateFlow<Boolean> = repository.faceAutoIndexFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setFaceAutoIndex(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateFaceAutoIndex(enabled)
            if (enabled) {
                val shouldIndex = withContext(Dispatchers.IO) {
                    val faceEngine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
                    faceEngine.isModelDownloaded() && db.faceDao().getUnindexedMediaIds().isNotEmpty()
                }
                if (shouldIndex) {
                    startFaceIndexing()
                }
            }
        }
    }

    val faceIndexWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceIndexWorker")
        .map { it.firstOrNull() }

    val faceModelDownloadWorkInfo: Flow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceModelDownloadWorker")
        .map { it.firstOrNull() }

    fun startFaceModelDownload() {
        val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceModelDownloadWorker>()
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "FaceModelDownloadWorker",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelFaceModelDownload() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceModelDownloadWorker")
    }

    fun startFaceIndexing(forceRescan: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val engine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
            if (!engine.isModelDownloaded()) {
                startFaceModelDownload()
                return@launch
            }
            if (forceRescan) {
                db.faceDao().clearAllFaceStatuses()
            }
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceIndexWorker>()
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "FaceIndexWorker",
                if (forceRescan) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun consolidateFaceClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            com.inferno.gallery.data.ai.FaceClusteringManager.getInstance(getApplication()).consolidateClusters()
        }
    }

    fun stopFaceIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceIndexWorker")
    }

    fun clearFaceIndex() {
        viewModelScope.launch(Dispatchers.IO) {
            stopFaceIndexing()
            db.faceDao().clearAllFaceStatuses()
            db.faceDao().clearAllFaces()
            db.faceDao().clearAllPersons()
            val cropDir = java.io.File(getApplication<Application>().filesDir, "face_crops")
            if (cropDir.exists()) {
                cropDir.deleteRecursively()
            }
        }
    }

    fun deleteFaceModel() {
        viewModelScope.launch(Dispatchers.IO) {
            stopFaceIndexing()
            val engine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
            val modelFile = engine.getModelFile()
            if (modelFile.exists()) {
                modelFile.delete()
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

    val viewerBlurEffect: StateFlow<Boolean> = repository.viewerBlurEffectFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setViewerBlurEffect(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateViewerBlurEffect(enabled)
        }
    }


    val viewerFilmstripEnabled: StateFlow<Boolean> = repository.viewerFilmstripEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setViewerFilmstripEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateViewerFilmstripEnabled(enabled)
        }
    }

    val viewerZoomMinimapEnabled: StateFlow<Boolean> = repository.viewerZoomMinimapEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setViewerZoomMinimapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateViewerZoomMinimapEnabled(enabled)
        }
    }

    val hdrDisplayEnabled: StateFlow<Boolean> = repository.hdrDisplayEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setHdrDisplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateHdrDisplayEnabled(enabled)
        }
    }



    val showAlbumSize: StateFlow<Boolean> = repository.showAlbumSizeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setShowAlbumSize(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowAlbumSize(show)
        }
    }

    val onboardingV210Completed: StateFlow<Boolean> = repository.onboardingV210CompletedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun setOnboardingV210Completed(completed: Boolean) {
        viewModelScope.launch {
            repository.updateOnboardingV210Completed(completed)
        }
    }

    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompletedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Eagerly so NavigationGraph gets the true state quickly
            initialValue = false
        )

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            repository.updateOnboardingCompleted(completed)
        }
    }

    val autoCheckUpdates: StateFlow<Boolean> = repository.autoCheckUpdatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoCheckUpdates(enabled)
            if (enabled) {
                UpdateCheckWorker.schedulePeriodic(getApplication())
            } else {
                UpdateCheckWorker.cancel(getApplication())
            }
        }
    }

    val updateState: StateFlow<UpdateState> = UpdateManager.state

    fun checkForUpdates() {
        UpdateManager.checkForUpdates(viewModelScope, manual = true, context = getApplication())
    }
}


