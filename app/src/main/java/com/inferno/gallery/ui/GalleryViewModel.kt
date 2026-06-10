package com.inferno.gallery.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.TelegramBackupEntity
import com.inferno.gallery.data.db.CloudMediaItem
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.data.FavoritesManager
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.inferno.gallery.workers.OcrIndexWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

import androidx.compose.runtime.Immutable

@Immutable
data class GalleryItem(
    val id: String,
    val uri: Uri,
    val bucketName: String,
    val dateAdded: Long,
    val size: Long,
    val name: String,
    val dateModified: Long,
    val path: String,
    val isVideo: Boolean = false,
    val durationMs: Long? = null,
    val searchScore: Float? = null
)

enum class SortOrder {
    NewToOld,
    OldToNew,
    SmallToBig,
    BigToSmall,
    NameAsc
}

enum class ViewMode {
    Immersive,
    Grouped
}

@Immutable
data class AlbumBucket(
    val bucketName: String,
    val coverUri: Uri,
    val itemCount: Int,
    val totalSizeBytes: Long = 0L,
    val maxDate: Long = 0L
)

enum class SearchMode { SMART, FTS }

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application.contentResolver)
    private val settingsRepository = SettingsRepository(application)
    private val favoritesManager = FavoritesManager(application)
    private val database = DatabaseProvider.getDatabase(application)


    val favoriteIds: StateFlow<Set<String>> = favoritesManager.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val gridAutoPlay: StateFlow<Boolean> = settingsRepository.gridAutoPlayFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun toggleGridAutoPlay() {
        viewModelScope.launch {
            settingsRepository.toggleGridAutoPlay(gridAutoPlay.value)
        }
    }

    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.onboardingCompletedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.updateOnboardingCompleted(true)
        }
    }



    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            favoritesManager.toggleFavorite(id)
        }
    }

    val allMedia: StateFlow<List<GalleryItem>> = database.mediaDao().observeAllMedia().map { entities ->
        entities.map { entity ->
            GalleryItem(
                id = entity.id.toString(),
                uri = Uri.parse(entity.uriString),
                bucketName = entity.bucketName,
                dateAdded = entity.dateAdded,
                size = entity.size,
                name = entity.name,
                dateModified = entity.dateModified,
                path = entity.filePath,
                isVideo = entity.isVideo,
                durationMs = entity.durationMs
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cloudMedia: StateFlow<List<CloudMediaItem>> = database.telegramBackupDao().observeCloudMedia()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cloudMediaIds: StateFlow<Set<String>> = database.telegramBackupDao().observeCloudMedia().map { list ->
        list.map { it.id.toString() }.toSet()
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptySet()
    )

    val pendingBackupsCount: StateFlow<Int> = database.telegramBackupDao().observePendingBackupsCount()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    suspend fun getTelegramFileUrl(fileId: String): String? {
        if (fileId.isBlank()) return null
        val token = settingsRepository.telegramBotTokenFlow.first()
        val chatId = settingsRepository.telegramChatIdFlow.first()
        if (token.isBlank() || chatId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                com.inferno.gallery.data.network.TelegramClient(token, chatId).getFileUrl(fileId)
            }.getOrNull()
        }
    }

    val sortOrder: StateFlow<SortOrder> = settingsRepository.sortOrderFlow.map {
        try {
            SortOrder.valueOf(it)
        } catch (e: Exception) {
            SortOrder.NewToOld
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortOrder.NewToOld
    )

    val albumSortOrder: StateFlow<SortOrder> = settingsRepository.albumSortOrderFlow.map {
        try {
            SortOrder.valueOf(it)
        } catch (e: Exception) {
            SortOrder.NameAsc
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortOrder.NameAsc
    )

    val viewMode: StateFlow<ViewMode> = settingsRepository.viewModeFlow.map {
        try {
            ViewMode.valueOf(it)
        } catch (e: Exception) {
            ViewMode.Immersive
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ViewMode.Immersive
    )

    val dockStyle: StateFlow<DockStyle> = settingsRepository.dockStyleFlow.map {
        try {
            DockStyle.valueOf(it)
        } catch (e: Exception) {
            DockStyle.PILL
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockStyle.PILL
    )

    private val _currentBucket = MutableStateFlow<String?>(null)

    fun setBucket(bucket: String?) {
        _currentBucket.value = bucket
    }

    val selectedFilterIndex: StateFlow<Int> = settingsRepository.selectedFilterIndexFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _gridCellsCount = MutableStateFlow(4)
    val gridCellsCount: StateFlow<Int> = _gridCellsCount.asStateFlow()

    private var saveGridCellsJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            _gridCellsCount.value = settingsRepository.gridCellsCountFlow.first()
        }
    }

    val thumbnailCornerRadius: StateFlow<Float> = settingsRepository.thumbnailCornerRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    fun setThumbnailCornerRadius(radius: Float) {
        viewModelScope.launch {
            settingsRepository.updateThumbnailCornerRadius(radius)
        }
    }

    val images: StateFlow<List<GalleryItem>> = combine(
        allMedia, sortOrder, _currentBucket, selectedFilterIndex
    ) { raw, order, bucket, filterIndex ->
        if (bucket == "search_text") {
            ftsSearchResults.value.filter { searchItem ->
                raw.any { it.id == searchItem.id && it.bucketName != "Trash" }
            }
        } else {
            val folderName = when (filterIndex) {
                1 -> "Camera"
                2 -> "Screenshots"
                else -> null
            }
            val filtered = if (bucket == "All") {
                raw.filter { it.bucketName != "Trash" }
            } else if (bucket == "telegram_cloud") {
                raw.filter { cloudMediaIds.value.contains(it.id) }
            } else if (bucket == "Videos") {
                raw.filter { it.isVideo && it.bucketName != "Trash" }
            } else if (bucket != null) {
                raw.filter { it.bucketName == bucket }
            } else if (folderName != null) {
                raw.filter { it.bucketName == folderName }
            } else {
                raw.filter { it.bucketName != "Trash" }
            }

            when (order) {
                SortOrder.NewToOld -> filtered.sortedByDescending { it.dateAdded }
                SortOrder.OldToNew -> filtered.sortedBy { it.dateAdded }
                SortOrder.SmallToBig -> filtered.sortedBy { it.size }
                SortOrder.BigToSmall -> filtered.sortedByDescending { it.size }
                SortOrder.NameAsc -> filtered.sortedBy { it.name }
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groupedImages: StateFlow<Map<String, List<GalleryItem>>> = images.map { sortedList ->
        val fullFormat = SimpleDateFormat("EEEE, dd-MM-yyyy", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        
        sortedList.groupBy { item ->
            val timeMs = item.dateAdded * 1000L
            if (timeMs < oneWeekAgo) {
                dateFormat.format(Date(timeMs))
            } else {
                fullFormat.format(Date(timeMs))
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris: StateFlow<Set<String>> = _selectedUris.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedUris.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleSelection(uri: String) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    fun addSelection(uri: String) {
        val current = _selectedUris.value.toMutableSet()
        current.add(uri)
        _selectedUris.value = current
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    fun selectAll() {
        _selectedUris.value = images.value.map { it.uri.toString() }.toSet()
    }

    fun selectRange(startUri: String, endUri: String) {
        val list = images.value
        val startIndex = list.indexOfFirst { it.uri.toString() == startUri }
        val endIndex = list.indexOfFirst { it.uri.toString() == endUri }
        if (startIndex != -1 && endIndex != -1) {
            val min = minOf(startIndex, endIndex)
            val max = maxOf(startIndex, endIndex)
            val range = list.subList(min, max + 1).map { it.uri.toString() }
            val current = _selectedUris.value.toMutableSet()
            current.addAll(range)
            _selectedUris.value = current
        }
    }

    val allAlbums: StateFlow<List<AlbumBucket>> = combine(allMedia, albumSortOrder) { mediaList, order ->
        val excludedBuckets = setOf("Camera", "Screenshots")
        
        val buckets = mediaList.groupBy { it.bucketName }
            .filterKeys { !excludedBuckets.contains(it) && !it.contains("Screenrecordings", ignoreCase = true) }
            .map { (bucketName, items) ->
            val totalSize = items.sumOf { it.size }
            val maxDate = items.maxOfOrNull { it.dateAdded } ?: 0L
            AlbumBucket(
                bucketName = bucketName,
                coverUri = items.first().uri,
                itemCount = items.size,
                totalSizeBytes = totalSize,
                maxDate = maxDate
            )
        }
        
        val sortedBuckets = when (order) {
            SortOrder.NewToOld -> buckets.sortedByDescending { it.maxDate }
            SortOrder.OldToNew -> buckets.sortedBy { it.maxDate }
            SortOrder.SmallToBig -> buckets.sortedBy { it.totalSizeBytes }
            SortOrder.BigToSmall -> buckets.sortedByDescending { it.totalSizeBytes }
            SortOrder.NameAsc -> buckets.sortedBy { it.bucketName }
        }
        
        sortedBuckets
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedAlbums: StateFlow<List<AlbumBucket>> = allMedia.map { mediaList ->
        val allBucket = AlbumBucket(
            bucketName = "All",
            coverUri = mediaList.firstOrNull()?.uri ?: Uri.EMPTY,
            itemCount = mediaList.size,
            totalSizeBytes = mediaList.sumOf { it.size },
            maxDate = mediaList.maxOfOrNull { it.dateAdded } ?: 0L
        )
        
        val videos = mediaList.filter { it.isVideo }
        val videosBucket = AlbumBucket(
            bucketName = "Videos",
            coverUri = videos.firstOrNull()?.uri ?: Uri.EMPTY,
            itemCount = videos.size,
            totalSizeBytes = videos.sumOf { it.size },
            maxDate = videos.maxOfOrNull { it.dateAdded } ?: 0L
        )

        val cameraItems = mediaList.filter { it.bucketName == "Camera" }
        val cameraBucket = AlbumBucket(
            bucketName = "Camera",
            coverUri = cameraItems.firstOrNull()?.uri ?: Uri.EMPTY,
            itemCount = cameraItems.size,
            totalSizeBytes = cameraItems.sumOf { it.size },
            maxDate = cameraItems.maxOfOrNull { it.dateAdded } ?: 0L
        )

        val screenshotsItems = mediaList.filter { it.bucketName == "Screenshots" }
        val screenshotsBucket = AlbumBucket(
            bucketName = "Screenshots",
            coverUri = screenshotsItems.firstOrNull()?.uri ?: Uri.EMPTY,
            itemCount = screenshotsItems.size,
            totalSizeBytes = screenshotsItems.sumOf { it.size },
            maxDate = screenshotsItems.maxOfOrNull { it.dateAdded } ?: 0L
        )

        val screenrecordingsItems = mediaList.filter { it.bucketName.contains("Screenrecordings", ignoreCase = true) }
        val screenrecordingsBucket = AlbumBucket(
            bucketName = "Screenrecordings",
            coverUri = screenrecordingsItems.firstOrNull()?.uri ?: Uri.EMPTY,
            itemCount = screenrecordingsItems.size,
            totalSizeBytes = screenrecordingsItems.sumOf { it.size },
            maxDate = screenrecordingsItems.maxOfOrNull { it.dateAdded } ?: 0L
        )

        listOf(allBucket, cameraBucket, videosBucket, screenshotsBucket, screenrecordingsBucket)
            .filter { it.itemCount > 0 }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Old FTS results (filename/bucket matching) kept for backward compat
    val searchResults: StateFlow<List<GalleryItem>> = combine(
        images, _searchQuery
    ) { items, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            items.filter {
                it.bucketName.contains(query, ignoreCase = true) ||
                it.uri.toString().contains(query, ignoreCase = true)
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var searchJob: kotlinx.coroutines.Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _ftsSearchResults.value = emptyList()
            _isSearching.value = false
            searchJob?.cancel()
            return
        }
        // Debounce: wait 500ms after user stops typing, then fire searches
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            performUnifiedSearch(query)
        }
    }

    private val _ftsSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val ftsSearchResults: StateFlow<List<GalleryItem>> = _ftsSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private fun performUnifiedSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Run FTS text-in-image search
                val ftsResults = withContext(Dispatchers.IO) {
                    try {
                        val ftsEntities = if (query.isNotBlank()) {
                            DatabaseProvider.searchFts(database, query)
                        } else {
                            emptyList()
                        }
                        ftsEntities.map { entity ->
                            GalleryItem(
                                id = entity.id.toString(),
                                uri = Uri.parse(entity.uriString),
                                bucketName = entity.bucketName,
                                dateAdded = entity.dateAdded,
                                size = entity.size,
                                name = entity.name,
                                dateModified = entity.dateModified,
                                path = entity.filePath,
                                isVideo = entity.isVideo,
                                durationMs = entity.durationMs
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GalleryViewModel", "FTS search failed: ${e.message}")
                        emptyList()
                    }
                }

                _ftsSearchResults.value = ftsResults
            } finally {
                _isSearching.value = false
            }
        }
    }



    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.updateSortOrder(order.name)
        }
    }

    fun setAlbumSortOrder(order: SortOrder) {
        viewModelScope.launch {
            settingsRepository.updateAlbumSortOrder(order.name)
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch {
            settingsRepository.updateViewMode(mode.name)
        }
    }



    fun setFilter(index: Int) {
        viewModelScope.launch {
            settingsRepository.updateSelectedFilterIndex(index)
        }
    }

    fun setGridCellsCount(count: Int) {
        _gridCellsCount.value = count
        saveGridCellsJob?.cancel()
        saveGridCellsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            settingsRepository.updateGridCellsCount(count)
        }
    }

    private val _uiEvents = kotlinx.coroutines.channels.Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun removeMediaOptimistically(uriString: String) {
        viewModelScope.launch {
            // Instantly move to Trash bin so the UI updates
            database.mediaDao().updateBucketByUri(uriString, "Trash")
            _uiEvents.send(UiEvent.DeleteSuccess)
        }
    }

    val ocrIndexWorkInfo = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("OcrIndexWorker")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val unindexedOcrImagesCount: kotlinx.coroutines.flow.StateFlow<Int> = database.mediaDao().observeUnindexedOcrImageCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalImagesCount: kotlinx.coroutines.flow.StateFlow<Int> = database.mediaDao().observeTotalImageCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val backupStatuses: StateFlow<Map<Long, String>> = database.telegramBackupDao().observeAllBackups().map { backups ->
        backups.associate { it.mediaId to it.backupStatus }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun startOcrIndexing() {
        viewModelScope.launch {
            settingsRepository.updateOcrIndexingEnabled(true)
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun stopOcrIndexing() {
        viewModelScope.launch {
            settingsRepository.updateOcrIndexingEnabled(false)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("OcrIndexWorker")
        }
    }

    fun clearOcrIndexAndReindex() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateOcrIndexingEnabled(true)
            WorkManager.getInstance(getApplication()).cancelUniqueWork("OcrIndexWorker")
            database.openHelper.writableDatabase.execSQL("DELETE FROM image_fts")
            val allIds = database.mediaDao().getAllMediaIds()
            allIds.chunked(500).forEach { chunk ->
                chunk.forEach { id -> database.mediaDao().updateOcrIndexStatus(id, false) }
            }
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    private val _isCloudRefreshing = MutableStateFlow(false)
    val isCloudRefreshing: StateFlow<Boolean> = _isCloudRefreshing.asStateFlow()

    fun refreshCloudBackups() {
        if (_isCloudRefreshing.value) return
        _isCloudRefreshing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupDao = database.telegramBackupDao()
                var backups = backupDao.observeAllBackups().first()
                var successBackups = backups.filter { it.backupStatus == "SUCCESS" }
                
                val botTokens = settingsRepository.telegramBotTokensFlow.first()
                var currentChatId = settingsRepository.telegramChatIdFlow.first()
                
                if (botTokens.isNotEmpty() && currentChatId.isNotBlank()) {
                    val activeToken = botTokens.first()
                    
                    // If no successful backups exist locally, try restoring from Telegram manifest
                    if (successBackups.isEmpty()) {
                        Log.d("GalleryViewModel", "Local backup list is empty. Attempting manifest recovery from Telegram.")
                        val restored = com.inferno.gallery.data.SyncManifestManager.restoreFromManifest(
                            getApplication(),
                            activeToken,
                            currentChatId
                        )
                        if (restored) {
                            backups = backupDao.observeAllBackups().first()
                            successBackups = backups.filter { it.backupStatus == "SUCCESS" }
                        }
                    }
                    
                    if (successBackups.isNotEmpty()) {
                        var client = com.inferno.gallery.data.network.TelegramClient(activeToken, currentChatId)
                        var changesMade = false
                        
                        for (backup in successBackups) {
                            val messageId = backup.telegramMessageId
                            val fileId = backup.telegramFileId
                            
                            try {
                                if (messageId != null) {
                                    val exists = client.checkMessageExists(messageId)
                                    if (!exists) {
                                        Log.d("GalleryViewModel", "Backup message $messageId was deleted from Telegram. Wiping association.")
                                        backupDao.deleteBackup(backup.mediaId)
                                        changesMade = true
                                    }
                                } else if (fileId != null) {
                                    // Fallback for legacy backups
                                    client.getFileUrl(fileId)
                                }
                                // Sleep 250ms to prevent rate limits during verification
                                kotlinx.coroutines.delay(250)
                            } catch (e: Exception) {
                                if (e is com.inferno.gallery.data.network.TelegramMigrationException) {
                                    Log.i("GalleryViewModel", "Group chat migrated during verification! Updating chat ID to ${e.newChatId}")
                                    settingsRepository.updateTelegramChatId(e.newChatId.toString())
                                    currentChatId = e.newChatId.toString()
                                    client = com.inferno.gallery.data.network.TelegramClient(activeToken, currentChatId)
                                    // Retry checking the current item once with new chat ID
                                    try {
                                        if (messageId != null) {
                                            val exists = client.checkMessageExists(messageId)
                                            if (!exists) {
                                                backupDao.deleteBackup(backup.mediaId)
                                                changesMade = true
                                            }
                                        } else if (fileId != null) {
                                            client.getFileUrl(fileId)
                                        }
                                    } catch (retryEx: Exception) {
                                        if (retryEx is com.inferno.gallery.data.network.TelegramApiException) {
                                            if (retryEx.description.contains("file_id is invalid", ignoreCase = true) || 
                                                retryEx.description.contains("file not found", ignoreCase = true)) {
                                                Log.d("GalleryViewModel", "Backup file $fileId was deleted from Telegram. Wiping association.")
                                                backupDao.deleteBackup(backup.mediaId)
                                                changesMade = true
                                            }
                                        }
                                    }
                                } else if (e is com.inferno.gallery.data.network.TelegramApiException) {
                                    // Delete legacy backups only if we get explicit file not found error
                                    if (messageId == null && fileId != null) {
                                        if (e.description.contains("file_id is invalid", ignoreCase = true) || 
                                            e.description.contains("file not found", ignoreCase = true)) {
                                            Log.d("GalleryViewModel", "Backup file $fileId was deleted from Telegram. Wiping association.")
                                            backupDao.deleteBackup(backup.mediaId)
                                            changesMade = true
                                        } else {
                                            Log.w("GalleryViewModel", "API error checking legacy file $fileId: ${e.message}")
                                        }
                                    } else {
                                        Log.w("GalleryViewModel", "API error checking message $messageId: ${e.message}")
                                    }
                                } else if (e is com.inferno.gallery.data.network.RateLimitException) {
                                    // Wait longer if we hit rate limits during check
                                    val waitTime = e.retryAfterSeconds + 2
                                    Log.w("GalleryViewModel", "Rate limit hit during cloud refresh verification. Waiting $waitTime seconds.")
                                    kotlinx.coroutines.delay(waitTime * 1000L)
                                } else {
                                    // Generic network or socket exceptions - DO NOT delete backup
                                    Log.w("GalleryViewModel", "Transient error checking cloud backup: ${e.message}")
                                }
                            }
                        }
                        
                        // If any records were deleted, update the pinned sync manifest
                        if (changesMade) {
                            com.inferno.gallery.data.SyncManifestManager.updateManifest(
                                getApplication(),
                                activeToken,
                                currentChatId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error verifying cloud backups: ${e.message}", e)
            } finally {
                _isCloudRefreshing.value = false
            }
        }
    }

    fun deleteCloudBackup(mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupDao = database.telegramBackupDao()
                val backup = backupDao.getBackupForMedia(mediaId)
                if (backup != null) {
                    val messageId = backup.telegramMessageId
                    val botTokens = settingsRepository.telegramBotTokensFlow.first()
                    val chatId = settingsRepository.telegramChatIdFlow.first()
                    
                    if (botTokens.isNotEmpty() && chatId.isNotBlank()) {
                        val token = botTokens.first()
                        
                        // 1. Delete from Telegram chat if messageId is available
                        if (messageId != null) {
                            try {
                                com.inferno.gallery.data.network.TelegramClient(token, chatId).deleteMessage(messageId)
                                Log.d("GalleryViewModel", "Deleted message $messageId from Telegram.")
                            } catch (e: Exception) {
                                Log.e("GalleryViewModel", "Failed to delete message $messageId from Telegram: ${e.message}")
                            }
                        }
                        
                        // 2. Delete local database association
                        backupDao.deleteBackup(mediaId)
                        
                        // 3. Update pinned manifest
                        com.inferno.gallery.data.SyncManifestManager.updateManifest(
                            getApplication(),
                            token,
                            chatId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error deleting cloud backup $mediaId: ${e.message}", e)
            }
        }
    }

    fun backupSelectedMedia() {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val allMediaList = database.mediaDao().getAllMedia()
            val backupDao = database.telegramBackupDao()
            
            selected.forEach { uriStr ->
                val mediaItem = allMediaList.firstOrNull { it.uriString == uriStr }
                if (mediaItem != null) {
                    backupDao.insertOrUpdate(
                        TelegramBackupEntity(
                            mediaId = mediaItem.id,
                            telegramFileId = null,
                            telegramThumbFileId = null,
                            backupStatus = "PENDING",
                            backupTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            withContext(Dispatchers.Main) {
                clearSelection()
            }
            
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.TelegramBackupWorker>()
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "TelegramBackupWorker",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    sealed class UiEvent {
        object DeleteSuccess : UiEvent()
    }

}
