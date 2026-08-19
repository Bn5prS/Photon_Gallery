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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository.getInstance(application)
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

    val appSeedColor: StateFlow<Int> = repository.appSeedColorFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0xFF6750A4.toInt() // Default M3 Purple
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

    // ── Advanced MaterialKolor Theming ──

    val colorPresetName: StateFlow<String> = repository.colorPresetNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Material You"
    )

    val secondaryColorOverride: StateFlow<Int> = repository.secondaryColorOverrideFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = -1
    )

    val tertiaryColorOverride: StateFlow<Int> = repository.tertiaryColorOverrideFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = -1
    )

    val contrastPreset: StateFlow<String> = repository.contrastPresetFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Default"
    )

    val animateThemeTransitions: StateFlow<Boolean> = repository.animateThemeTransitionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    /** Applies a named preset atomically — seed, style, and contrast all update together. */
    fun setColorPreset(preset: com.inferno.gallery.ui.theme.PhotonColorPreset) {
        viewModelScope.launch {
            repository.updateColorPresetName(preset.name)
            // Clear per-slot overrides so the preset drives everything
            repository.updateSecondaryColorOverride(-1)
            repository.updateTertiaryColorOverride(-1)
            repository.updateContrastPreset(preset.contrastPreset)
            if (preset.seedColor != null) {
                repository.updateAppSeedColor(preset.seedColor.toArgb())
                repository.updateUseMaterialYou(false)
            } else {
                // "Material You" preset → re-enable wallpaper extraction
                repository.updateUseMaterialYou(true)
            }
            repository.updateThemePaletteStyle(preset.style.name)
        }
    }

    fun setSecondaryColorOverride(colorArgb: Int) {
        viewModelScope.launch {
            repository.updateSecondaryColorOverride(colorArgb)
            repository.updateColorPresetName("Custom")
        }
    }

    fun setTertiaryColorOverride(colorArgb: Int) {
        viewModelScope.launch {
            repository.updateTertiaryColorOverride(colorArgb)
            repository.updateColorPresetName("Custom")
        }
    }

    fun clearSecondaryColorOverride() {
        viewModelScope.launch { repository.updateSecondaryColorOverride(-1) }
    }

    fun clearTertiaryColorOverride() {
        viewModelScope.launch { repository.updateTertiaryColorOverride(-1) }
    }

    fun setContrastPreset(preset: String) {
        viewModelScope.launch {
            repository.updateContrastPreset(preset)
        }
    }

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
}


