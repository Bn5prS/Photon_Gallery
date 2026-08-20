@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.inferno.gallery.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.MediaEntity
import com.inferno.gallery.data.db.FaceEntity
import com.inferno.gallery.data.db.PersonClusterEntity
import com.inferno.gallery.data.IndexingProgressManager
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.data.FavoritesManager
import com.inferno.gallery.data.VaultAuthManager
import com.inferno.gallery.data.BucketNames
import com.inferno.gallery.data.MediaQueryBuilder
import com.inferno.gallery.data.ai.FaceClusteringManager
import android.util.Log

import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.paging.filter
import androidx.paging.insertSeparators
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
    val searchScore: Float? = null,
    /** Pre-computed on IO: whether the local file exists on disk. */
    val localExists: Boolean = true,
    /** Pre-computed on IO: the URI to use for thumbnail loading. */
    val resolvedUri: Uri = uri,
    val pHash: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fileHash: String? = null
)

@Immutable
data class SmartSearchStatus(
    val modelDownloaded: Boolean,
    val isIndexing: Boolean,
    val indexedCount: Int,
    val totalCount: Int
)

sealed class GalleryListItem {
    data class Item(val galleryItem: GalleryItem) : GalleryListItem()
    data class Header(val title: String) : GalleryListItem()
}

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
    val maxDate: Long = 0L,
    val coverUris: List<Uri> = emptyList()
)

@Immutable
data class DuplicateGroup(
    val fileHash: String,
    val items: List<GalleryItem>
)

sealed class DuplicateScanState {
    data object Idle : DuplicateScanState()
    data class Scanning(val processed: Int, val total: Int) : DuplicateScanState()
    data object Done : DuplicateScanState()
}

enum class SearchMode { SMART, FTS }

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application.contentResolver)
    val settingsRepository = SettingsRepository.getInstance(application)
    private val favoritesManager = FavoritesManager(application)
    private val database = DatabaseProvider.getDatabase(application)
    
    // UI State for Person Face Clusters
    val personClusters: StateFlow<List<PersonClusterEntity>> = database.faceDao().observeAllClusters()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unindexedFaceCount: StateFlow<Int> = database.faceDao().observeUnindexedFaceCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val faceIndexingProgress = IndexingProgressManager.faceProgress

    // UI State for Place Clusters
    private val _placesClusters = MutableStateFlow<List<com.inferno.gallery.data.db.BucketInfo>>(emptyList())
    val placesClusters: StateFlow<List<com.inferno.gallery.data.db.BucketInfo>> = _placesClusters.asStateFlow()

    // ── Duplicate Scan State ──
    private val _duplicateScanState = MutableStateFlow<DuplicateScanState>(DuplicateScanState.Idle)
    val duplicateScanState: StateFlow<DuplicateScanState> = _duplicateScanState.asStateFlow()

    // ── Toast Event Channel ──
    private val _toastEvent = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private fun showToast(message: String) {
        _toastEvent.trySend(message)
    }

    /** True while the initial fast sync is running — used to show loading UI on first launch. */
    private val _isInitialSyncRunning = MutableStateFlow(false)
    val isInitialSyncRunning: StateFlow<Boolean> = _isInitialSyncRunning.asStateFlow()

    private var mediaStoreObserverJob: kotlinx.coroutines.Job? = null

    private val mediaStoreObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            mediaStoreObserverJob?.cancel()
            mediaStoreObserverJob = viewModelScope.launch {
                kotlinx.coroutines.delay(1000) // Debounce rapid MediaStore changes
                Log.d("GalleryViewModel", "MediaStore change detected, enqueueing MediaSyncWorker...")
                val syncRequest = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "MediaSyncWorker_Foreground",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            }
        }
    }

    init {
        // Collect place clusters
        viewModelScope.launch(Dispatchers.IO) {
            database.placesDao().observePlaceClusters().collect { clusters ->
                _placesClusters.value = clusters
            }
        }

        // Fast initial sync: if Room is empty, bulk-insert MediaStore metadata
        // directly on IO. This populates the grid in ~1s instead of waiting
        // ~10s for WorkManager to schedule and execute MediaSyncWorker.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = database.mediaDao().getAllMedia().size
                if (count == 0) {
                    _isInitialSyncRunning.value = true
                    Log.d("GalleryViewModel", "Room empty — running fast initial sync")
                    val mediaList = repository.getImagesListForSync()
                    if (mediaList.isNotEmpty()) {
                        val vaultUris = database.vaultDao().getAllOriginalUris().toSet()
                        val vaultPaths = database.vaultDao().getAllOriginalPaths().toSet()

                        val entities = mediaList
                            .filter { !vaultUris.contains(it.uri.toString()) && !vaultPaths.contains(it.path) }
                            .map { media ->
                                com.inferno.gallery.data.db.CoreMediaEntity(
                                    id = media.id,
                                    uriString = media.uri.toString(),
                                    filePath = media.path,
                                    bucketName = media.bucketName,
                                    dateAdded = media.dateAdded,
                                    dateModified = media.dateModified,
                                    size = media.size,
                                    name = media.name,
                                    mimeType = null,
                                    isVideo = media.isVideo,
                                    durationMs = media.durationMs,
                                    isIndexedOcr = false,
                                    pHash = null,
                                    latitude = null,
                                    longitude = null,
                                    fileHash = null
                                )
                            }
                        if (entities.isNotEmpty()) {
                            database.mediaDao().insertAll(entities)
                        }
                        Log.d("GalleryViewModel", "Fast sync complete: ${entities.size} items inserted")
                    }
                    _isInitialSyncRunning.value = false
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Fast sync failed: ${e.message}")
                _isInitialSyncRunning.value = false
            }
        }

        getApplication<Application>().contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver
        )
        getApplication<Application>().contentResolver.registerContentObserver(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaStoreObserver
        )

        // Silently scan for duplicates in the background on startup
        scanForDuplicates()

    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
    }

    /**
     * Resolves localExists and resolvedUri for a GalleryItem.
     */
    private fun resolveFileUri(uri: Uri, path: String): Uri {
        return if (path.isNotEmpty()) {
            Uri.fromFile(java.io.File(path))
        } else {
            uri
        }
    }

    private fun resolveItemFields(
        uri: Uri,
        path: String
    ): Pair<Boolean, Uri> {
        return true to resolveFileUri(uri, path)
    }

    // ── Shared SQL Query Builder ──
    // Delegates to MediaQueryBuilder for testability
    private fun buildMediaConditions(
        bucket: String?,
        filterIndex: Int,
        excluded: Set<String> = emptySet(),
        favIds: Set<String> = emptySet(),
        ftsIds: List<String> = emptyList(),
        smartIds: List<String> = emptyList()
    ): MediaQueryBuilder.QueryConditions {
        return MediaQueryBuilder.buildMediaConditions(bucket, filterIndex, excluded, favIds, ftsIds, smartIds)
    }

    private fun buildWhereClause(qc: MediaQueryBuilder.QueryConditions): String {
        return MediaQueryBuilder.buildWhereClause(qc)
    }

    private fun buildOrderClause(order: SortOrder): String = MediaQueryBuilder.buildOrderClause(order.name)

    private fun buildOrderClause(order: SortOrder, bucket: String?, smartIds: List<String>): String =
        MediaQueryBuilder.buildOrderClause(order.name, bucket, smartIds)


    // ── Excluded Folders ──
    val excludedFolders: StateFlow<Set<String>> = settingsRepository.excludedFoldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleExcludedFolder(bucketName: String) {
        viewModelScope.launch {
            val current = excludedFolders.value.toMutableSet()
            if (current.contains(bucketName)) current.remove(bucketName) else current.add(bucketName)
            settingsRepository.updateExcludedFolders(current)
        }
    }

    // ── Private Space ──
    val vaultAuthManager = VaultAuthManager()
    private val vaultRepository = com.inferno.gallery.data.VaultRepository(application, database.vaultDao())

    val vaultItems: StateFlow<List<com.inferno.gallery.data.db.VaultMediaEntity>> = vaultRepository.vaultItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val vaultItemCount: StateFlow<Int> = vaultRepository.vaultCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val isVaultUnlocked: StateFlow<Boolean> = vaultAuthManager.isAuthenticated

    fun hideMedia(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = vaultRepository.hideMedia(uris).size
            showToast("$count item${if (count != 1) "s" else ""} hidden")
        }
    }

    fun unhideMedia(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = vaultRepository.unhideMedia(ids)
            showToast("$count item${if (count != 1) "s" else ""} restored")
        }
    }

    fun deleteFromVault(ids: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            vaultRepository.deleteFromVault(ids)
            showToast("${ids.size} item${if (ids.size != 1) "s" else ""} permanently deleted")
        }
    }

    fun getVaultFileUri(entity: com.inferno.gallery.data.db.VaultMediaEntity): Uri {
        return vaultRepository.getVaultFileUri(entity)
    }

    private val _isScrollDockVisible = MutableStateFlow(true)
    val isScrollDockVisible: StateFlow<Boolean> = _isScrollDockVisible.asStateFlow()

    fun setScrollDockVisible(visible: Boolean) {
        _isScrollDockVisible.value = visible
    }


    val favoriteIds: StateFlow<Set<String>> = favoritesManager.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )







    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompletedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.updateOnboardingCompleted(true)
        }
    }

    val trashCount: StateFlow<Int> = database.mediaDao().observeTrashCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val showAlbumSize: StateFlow<Boolean> = settingsRepository.showAlbumSizeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            favoritesManager.toggleFavorite(id)
        }
    }

    val favoriteMedia: StateFlow<List<GalleryItem>> = combine(
        favoritesManager.favoritesFlow,
        excludedFolders
    ) { favs, excluded ->
        Pair(favs, excluded)
    }.flatMapLatest { (favs, excluded) ->
        if (favs.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else database.mediaDao().observeMediaByIds(favs.mapNotNull { it.toLongOrNull() })
            .map { entities ->
                entities
                    .filter { !excluded.contains(it.bucketName) && it.bucketName != "Trash" }
                    .map { entity ->
                        val uri = Uri.parse(entity.uriString)
                        val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                        GalleryItem(
                            id = entity.id.toString(),
                            uri = uri,
                            bucketName = entity.bucketName,
                            dateAdded = entity.dateAdded,
                            size = entity.size,
                            name = entity.name,
                            dateModified = entity.dateModified,
                            path = entity.filePath,
                            isVideo = entity.isVideo,
                            durationMs = entity.durationMs,
                            localExists = exists,
                            resolvedUri = resolved
                        )
                    }
            }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detailMedia = MutableStateFlow<List<GalleryItem>>(emptyList())
    val detailMedia: StateFlow<List<GalleryItem>> = _detailMedia.asStateFlow()

    private val _initialDetailItem = MutableStateFlow<GalleryItem?>(null)
    val initialDetailItem: StateFlow<GalleryItem?> = _initialDetailItem.asStateFlow()

    fun setInitialDetailItem(item: GalleryItem) {
        _initialDetailItem.value = item
        if (_detailMedia.value.none { it.id == item.id }) {
            _detailMedia.value = listOf(item)
        }
    }

    fun loadDetailMedia(mediaId: String, bucketName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (bucketName == BucketNames.SEARCH_TEXT) {
                _detailMedia.value = _ftsSearchResults.value
                return@launch
            }
            if (bucketName == BucketNames.SEARCH_SMART) {
                _detailMedia.value = _smartSearchResults.value
                return@launch
            }
            if (bucketName == BucketNames.FAVORITES) {
                _detailMedia.value = favoriteMedia.value
                return@launch
            }
            if (bucketName == "geotagged") {
                val excluded = excludedFolders.value
                val excludedClause = if (excluded.isNotEmpty()) {
                    val placeholders = excluded.joinToString(",") { "?" }
                    " AND cm.bucketName NOT IN ($placeholders)"
                } else ""
                val queryString = "SELECT cm.* FROM core_media cm WHERE cm.latitude IS NOT NULL AND cm.longitude IS NOT NULL AND cm.bucketName != 'Trash' AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)$excludedClause ORDER BY cm.dateAdded DESC"
                val query = androidx.sqlite.db.SimpleSQLiteQuery(queryString, excluded.toTypedArray())
                val entities = try {
                    database.mediaDao().getMediaRaw(query)
                } catch (e: Exception) {
                    emptyList()
                }
                val items = entities.map { entity ->
                    val uri = Uri.parse(entity.uriString)
                    val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                    GalleryItem(
                        id = entity.id.toString(),
                        uri = uri,
                        bucketName = entity.bucketName,
                        dateAdded = entity.dateAdded,
                        size = entity.size,
                        name = entity.name,
                        dateModified = entity.dateModified,
                        path = entity.filePath,
                        isVideo = entity.isVideo,
                        durationMs = entity.durationMs,
                        localExists = exists,
                        resolvedUri = resolved,
                        latitude = entity.latitude,
                        longitude = entity.longitude
                    )
                }
                
                _detailMedia.value = items
                return@launch
            }

            // Build the SQLite query for the bucket
            val filterIndex = selectedFilterIndex.value
            val order = sortOrder.value

            val smartIds = smartSearchResults.value.map { it.id }
            val qc = buildMediaConditions(
                bucket = bucketName, 
                filterIndex = filterIndex, 
                excluded = excludedFolders.value,
                favIds = favoriteIds.value,
                smartIds = smartIds
            )
            val queryString = "SELECT cm.* FROM core_media cm " +
                buildWhereClause(qc) + buildOrderClause(order, bucketName, smartIds)

            val query = androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
            val entities = try {
                database.mediaDao().getMediaRaw(query)
            } catch (e: Exception) {
                emptyList()
            }
            val items = entities.map { entity ->
                val uri = Uri.parse(entity.uriString)
                val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                GalleryItem(
                    id = entity.id.toString(),
                    uri = uri,
                    bucketName = entity.bucketName,
                    dateAdded = entity.dateAdded,
                    size = entity.size,
                    name = entity.name,
                    dateModified = entity.dateModified,
                    path = entity.filePath,
                    isVideo = entity.isVideo,
                    durationMs = entity.durationMs,
                    localExists = exists,
                    resolvedUri = resolved
                )
            }

            val finalItems = items

            val containsTarget = finalItems.any { it.id == mediaId }
            if (finalItems.isNotEmpty() && containsTarget) {
                _detailMedia.value = finalItems
            } else {
                // Fetch the clicked item as a fallback
                val idLong = mediaId.toLongOrNull()
                val entity = idLong?.let { database.mediaDao().getMediaById(it) }
                if (entity != null) {
                    val uri = Uri.parse(entity.uriString)
                    val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                    val fallbackItem = GalleryItem(
                        id = entity.id.toString(),
                        uri = uri,
                        bucketName = entity.bucketName,
                        dateAdded = entity.dateAdded,
                        size = entity.size,
                        name = entity.name,
                        dateModified = entity.dateModified,
                        path = entity.filePath,
                        isVideo = entity.isVideo,
                        durationMs = entity.durationMs,
                        localExists = exists,
                        resolvedUri = resolved
                    )
                    _detailMedia.value = listOf(fallbackItem)
                } else {
                    _detailMedia.value = emptyList()
                }
            }
        }
    }

    fun recordMediaView(mediaId: String) {
        // No-op for offline gallery
    }

    fun renameMedia(
        context: android.content.Context,
        item: GalleryItem,
        newName: String,
        onSecurityException: (android.app.PendingIntent) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = item.uri
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newName)
                }

                try {
                    val rows = context.contentResolver.update(uri, values, null, null)
                    if (rows > 0) {
                        database.mediaDao().updateMediaName(item.id.toLong(), newName)

                        val currentDetailMedia = _detailMedia.value
                        val updatedList = currentDetailMedia.map {
                            if (it.id == item.id) {
                                it.copy(name = newName)
                            } else {
                                it
                            }
                        }
                        _detailMedia.value = updatedList

                        withContext(Dispatchers.Main) {
                            showToast("Renamed to $newName")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Failed to rename: 0 rows updated")
                        }
                    }
                } catch (securityException: SecurityException) {
                    val recoverable = securityException as? android.app.RecoverableSecurityException
                    if (recoverable != null) {
                        withContext(Dispatchers.Main) {
                            onSecurityException(recoverable.userAction.actionIntent)
                        }
                    } else {
                        val pendingIntent = android.provider.MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                        withContext(Dispatchers.Main) {
                            onSecurityException(pendingIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error renaming: ${e.message}")
                }
            }
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

    val timelineLayoutMode: StateFlow<com.inferno.gallery.data.TimelineLayoutMode> = settingsRepository.timelineLayoutModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.TimelineLayoutMode.STANDARD_GRID
    )

    fun setTimelineLayoutMode(mode: com.inferno.gallery.data.TimelineLayoutMode) {
        viewModelScope.launch {
            settingsRepository.updateTimelineLayoutMode(mode)
        }
    }

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
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(application)
                if (searchEngine.isModelDownloaded()) {
                    Log.d("GalleryViewModel", "Pre-warming Smart Search Engine...")
                    searchEngine.loadModel()
                    Log.d("GalleryViewModel", "Smart Search Engine pre-warmed successfully.")
                }
            } catch (e: Exception) {
                Log.w("GalleryViewModel", "Failed to pre-warm Smart Search Engine: ${e.message}")
            }
        }
    }

    val thumbnailCornerRadius: StateFlow<Float> = settingsRepository.thumbnailCornerRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    val cacheThumbnailsEnabled: StateFlow<Boolean> = settingsRepository.cacheThumbnailsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setThumbnailCornerRadius(radius: Float) {
        viewModelScope.launch {
            settingsRepository.updateThumbnailCornerRadius(radius)
        }
    }


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

    private var baseSelectedUris = emptySet<String>()
    private val _draggedUris = MutableStateFlow<Set<String>>(emptySet())
    private var dragSelecting = true

    fun startDragSelection(initialUri: String, isSelecting: Boolean) {
        baseSelectedUris = _selectedUris.value
        dragSelecting = isSelecting
        _draggedUris.value = setOf(initialUri)
        updateSelectionState()
    }

    fun updateDragSelection(uris: Set<String>) {
        _draggedUris.value = uris
        updateSelectionState()
    }

    fun endDragSelection() {
        _selectedUris.value = getMergedSelection()
        _draggedUris.value = emptySet()
        baseSelectedUris = emptySet()
    }

    private fun getMergedSelection(): Set<String> {
        return if (dragSelecting) {
            baseSelectedUris + _draggedUris.value
        } else {
            baseSelectedUris - _draggedUris.value
        }
    }

    private fun updateSelectionState() {
        _selectedUris.value = getMergedSelection()
    }

    fun toggleSelectAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val bucket = _currentBucket.value
            val filterIndex = selectedFilterIndex.value

            val qc = buildMediaConditions(
                bucket = bucket,
                filterIndex = filterIndex,
                ftsIds = ftsSearchResults.value.map { it.id },
                smartIds = smartSearchResults.value.map { it.id }
            )
            val queryString = "SELECT cm.uriString FROM core_media cm " +
                buildWhereClause(qc)

            try {
                val dbQuery = androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
                val allUris = database.mediaDao().getUrisRaw(dbQuery).toSet()

                val currentSelected = _selectedUris.value
                val allSelected = allUris.isNotEmpty() && allUris.all { currentSelected.contains(it) }

                if (allSelected) {
                    // Deselect all matching items in current view
                    _selectedUris.value = currentSelected - allUris
                } else {
                    // Select all matching items in current view
                    _selectedUris.value = currentSelected + allUris
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error toggling select all: ${e.message}", e)
            }
        }
    }

    fun selectRange(startUri: String, endUri: String) {
        // Not supported in Paging3 without memory indexing.
    }

    val albumCustomCovers: StateFlow<Map<String, String>> = settingsRepository.albumCustomCoversFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setAlbumCustomCover(bucketName: String, uriString: String) {
        viewModelScope.launch {
            settingsRepository.setAlbumCustomCover(bucketName, uriString)
        }
    }

    fun removeAlbumCustomCover(bucketName: String) {
        viewModelScope.launch {
            settingsRepository.removeAlbumCustomCover(bucketName)
        }
    }

    val allAlbums: StateFlow<List<AlbumBucket>> = combine(
        database.mediaDao().observeBuckets(),
        albumSortOrder,
        excludedFolders,
        settingsRepository.showHiddenAlbumsFlow,
        settingsRepository.albumCustomCoversFlow
    ) { buckets, order, excluded, showHidden, customCovers ->
        val excludedKeywords = setOf(BucketNames.CAMERA, BucketNames.SCREENSHOTS, BucketNames.TRASH, BucketNames.ALL, BucketNames.VIDEOS)

        val filtered = buckets.filter { bucket ->
            !excludedKeywords.contains(bucket.bucketName) &&
            !bucket.bucketName.contains(BucketNames.SCREENRECORDINGS, ignoreCase = true) &&
            !bucket.bucketName.contains(BucketNames.SCREEN_RECORDS, ignoreCase = true) &&
            !bucket.bucketName.contains(BucketNames.SCREEN_RECORDS_NO_SPACE, ignoreCase = true) &&
            !bucket.bucketName.contains(BucketNames.SCREEN_RECORD, ignoreCase = true) &&
            !bucket.bucketName.contains(BucketNames.SCREENSHOT, ignoreCase = true) &&
            !excluded.contains(bucket.bucketName) &&
            (showHidden || !bucket.bucketName.startsWith("."))
        }.map { b ->
            val customUri = customCovers[b.bucketName]?.let { Uri.parse(it) }
            AlbumBucket(
                bucketName = b.bucketName,
                coverUri = customUri ?: (if (b.coverUriString != null) Uri.parse(b.coverUriString) else Uri.EMPTY),
                itemCount = b.itemCount,
                totalSizeBytes = b.totalSizeBytes,
                maxDate = b.maxDate
            )
        }

        val sortedBuckets = when (order) {
            SortOrder.NewToOld -> filtered.sortedByDescending { it.maxDate }
            SortOrder.OldToNew -> filtered.sortedBy { it.maxDate }
            SortOrder.SmallToBig -> filtered.sortedBy { it.totalSizeBytes }
            SortOrder.BigToSmall -> filtered.sortedByDescending { it.totalSizeBytes }
            SortOrder.NameAsc -> filtered.sortedBy { it.bucketName }
        }

        sortedBuckets
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All bucket names for the Settings "Excluded Folders" UI (includes all folders)
    val allBucketNames: StateFlow<List<String>> = combine(
        database.mediaDao().observeBuckets(),
        settingsRepository.showHiddenAlbumsFlow
    ) { buckets, showHidden ->
        buckets.map { it.bucketName }
            .filter { it != "Trash" && (showHidden || !it.startsWith(".")) }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val duplicates: StateFlow<List<DuplicateGroup>> = combine(
        database.mediaDao().observeExactDuplicates(),
        excludedFolders
    ) { entities, excluded ->
        entities.filter { !excluded.contains(it.bucketName) && it.bucketName != "Trash" }
            .map { entity ->
                val uri = Uri.parse(entity.uriString)
                val exists = java.io.File(entity.filePath).exists()
                GalleryItem(
                    id = entity.id.toString(),
                    uri = uri,
                    bucketName = entity.bucketName,
                    dateAdded = entity.dateAdded,
                    size = entity.size,
                    name = entity.name,
                    dateModified = entity.dateModified,
                    path = entity.filePath,
                    isVideo = entity.isVideo,
                    durationMs = entity.durationMs,
                    localExists = exists,
                    resolvedUri = resolveFileUri(uri, entity.filePath),
                    pHash = entity.pHash,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    fileHash = entity.fileHash
                )
            }.filter { it.fileHash != null }
            .groupBy { it.fileHash!! }
            .filter { it.value.size > 1 }
            .map { (hash, items) -> DuplicateGroup(hash, items) }
            .sortedByDescending { group -> group.items.sumOf { it.size } }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanForDuplicates() {
        if (_duplicateScanState.value is DuplicateScanState.Scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            _duplicateScanState.value = DuplicateScanState.Scanning(0, 0)
            try {
                // Instantly find candidates that share the same byte size
                val candidates = database.mediaDao().getMediaSharingSameSize()
                val needsHash = candidates.filter { it.fileHash == null }
                val total = needsHash.size

                if (total == 0) {
                    _duplicateScanState.value = DuplicateScanState.Done
                    return@launch
                }

                val app = getApplication<Application>()
                val batchSize = 20
                var processed = 0

                for (batch in needsHash.chunked(batchSize)) {
                    val updates = batch.mapNotNull { entity ->
                        val uri = Uri.parse(entity.uriString)
                        val hash = com.inferno.gallery.utils.HashUtils.computeMediaHash(
                            app.contentResolver,
                            uri,
                            entity.filePath
                        )
                        if (hash != null && hash != entity.fileHash) {
                            entity.copy(fileHash = hash)
                        } else null
                    }

                    if (updates.isNotEmpty()) {
                        database.mediaDao().insertAll(updates)
                    }

                    processed += batch.size
                    _duplicateScanState.value = DuplicateScanState.Scanning(processed, total)
                }

                _duplicateScanState.value = DuplicateScanState.Done
            } catch (e: Exception) {
                e.printStackTrace()
                _duplicateScanState.value = DuplicateScanState.Done
            }
        }
    }

    // ── GPS Scan State ──
    sealed class GpsScanState {
        data object Idle : GpsScanState()
        data class Scanning(val processed: Int, val total: Int) : GpsScanState()
        data object Done : GpsScanState()
    }

    private val _gpsScanState = MutableStateFlow<GpsScanState>(GpsScanState.Idle)
    val gpsScanState: StateFlow<GpsScanState> = _gpsScanState.asStateFlow()

    private val _geotaggedMedia = MutableStateFlow<List<GalleryItem>>(emptyList())
    val geotaggedMedia: StateFlow<List<GalleryItem>> = _geotaggedMedia.asStateFlow()

    fun scanGpsMetadata() {
        if (_gpsScanState.value is GpsScanState.Scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            _gpsScanState.value = GpsScanState.Scanning(0, 0)
            try {
                val needsGps = database.mediaDao().getMediaNeedingGps()
                val total = needsGps.size

                if (total > 0) {
                    val batchSize = 100
                    var processed = 0

                    for (batch in needsGps.chunked(batchSize)) {
                        val updates = batch.mapNotNull { entity ->
                            try {
                                val file = if (entity.filePath.isNotEmpty()) java.io.File(entity.filePath) else null
                                if (file != null && file.exists()) {
                                    val exif = androidx.exifinterface.media.ExifInterface(entity.filePath)
                                    val latLong = exif.latLong
                                    if (latLong != null) {
                                        entity.copy(latitude = latLong[0], longitude = latLong[1])
                                    } else null
                                } else null
                            } catch (_: Exception) { null }
                        }

                        if (updates.isNotEmpty()) {
                            database.mediaDao().insertAll(updates)
                        }

                        processed += batch.size
                        _gpsScanState.value = GpsScanState.Scanning(processed, total)
                    }
                }

                // Load geotagged results
                loadGeotaggedMedia()
                _gpsScanState.value = GpsScanState.Done
            } catch (e: Exception) {
                e.printStackTrace()
                _gpsScanState.value = GpsScanState.Done
            }
        }
    }

    fun loadGeotaggedMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val excluded = excludedFolders.value
                val entities = database.mediaDao().getGeotaggedMedia()
                _geotaggedMedia.value = entities
                    .filter { !excluded.contains(it.bucketName) && it.bucketName != "Trash" }
                    .map { entity ->
                        val uri = Uri.parse(entity.uriString)
                        GalleryItem(
                            id = entity.id.toString(),
                            uri = uri,
                            bucketName = entity.bucketName,
                            dateAdded = entity.dateAdded,
                            size = entity.size,
                            name = entity.name,
                            dateModified = entity.dateModified,
                            path = entity.filePath,
                            isVideo = entity.isVideo,
                            durationMs = entity.durationMs,
                            localExists = true,
                            resolvedUri = resolveFileUri(uri, entity.filePath),
                            latitude = entity.latitude,
                            longitude = entity.longitude
                        )
                    }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to load geotagged media: ${e.message}")
            }
        }
    }

    val pinnedAlbums: StateFlow<List<AlbumBucket>> = combine(
        database.mediaDao().observeBuckets(),
        database.mediaDao().observeAllMediaStats(),
        database.mediaDao().observeTopCoverUris(),
        excludedFolders
    ) { buckets, allStats, topCoverUris, excluded ->
        val allBucket = AlbumBucket(
            bucketName = "All",
            coverUri = allStats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
            itemCount = allStats.itemCount,
            totalSizeBytes = allStats.totalSizeBytes,
            maxDate = allStats.maxDate,
            coverUris = topCoverUris.map { Uri.parse(it) }
        )

        val cameraItems = buckets.filter { it.bucketName.equals(BucketNames.CAMERA, ignoreCase = true) }
        val cameraBucket = AlbumBucket(
            bucketName = cameraItems.firstOrNull()?.bucketName ?: BucketNames.CAMERA,
            coverUri = cameraItems.maxByOrNull { it.maxDate }?.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
            itemCount = cameraItems.sumOf { it.itemCount },
            totalSizeBytes = cameraItems.sumOf { it.totalSizeBytes },
            maxDate = cameraItems.maxOfOrNull { it.maxDate } ?: 0L
        )

        listOfNotNull(
            allBucket,
            if (!excluded.contains(cameraBucket.bucketName)) cameraBucket else null
        ).filter { it.itemCount > 0 || it.bucketName == BucketNames.ALL || it.bucketName == BucketNames.CAMERA }
    }.combine(settingsRepository.albumCustomCoversFlow) { buckets, customCovers ->
        buckets.map { bucket ->
            val customUri = customCovers[bucket.bucketName]?.let { Uri.parse(it) }
            if (customUri != null) bucket.copy(coverUri = customUri) else bucket
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaTypeBuckets: StateFlow<List<AlbumBucket>> = combine(
        database.mediaDao().observeRawStats(),
        database.mediaDao().observePanoramaStats(),
        database.mediaDao().observeSlowMoStats(),
        database.mediaDao().observeAnimationStats()
    ) { rawStats, panoStats, slowMoStats, animStats ->
        val list = mutableListOf<AlbumBucket>()

        if (rawStats.itemCount > 0) {
            list.add(
                AlbumBucket(
                    bucketName = BucketNames.MEDIA_TYPE_RAW,
                    coverUri = rawStats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                    itemCount = rawStats.itemCount,
                    totalSizeBytes = rawStats.totalSizeBytes,
                    maxDate = rawStats.maxDate
                )
            )
        }

        if (panoStats.itemCount > 0) {
            list.add(
                AlbumBucket(
                    bucketName = BucketNames.MEDIA_TYPE_PANORAMAS,
                    coverUri = panoStats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                    itemCount = panoStats.itemCount,
                    totalSizeBytes = panoStats.totalSizeBytes,
                    maxDate = panoStats.maxDate
                )
            )
        }

        if (slowMoStats.itemCount > 0) {
            list.add(
                AlbumBucket(
                    bucketName = BucketNames.MEDIA_TYPE_SLOW_MO,
                    coverUri = slowMoStats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                    itemCount = slowMoStats.itemCount,
                    totalSizeBytes = slowMoStats.totalSizeBytes,
                    maxDate = slowMoStats.maxDate
                )
            )
        }

        if (animStats.itemCount > 0) {
            list.add(
                AlbumBucket(
                    bucketName = BucketNames.MEDIA_TYPE_ANIMATIONS,
                    coverUri = animStats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                    itemCount = animStats.itemCount,
                    totalSizeBytes = animStats.totalSizeBytes,
                    maxDate = animStats.maxDate
                )
            )
        }

        list
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User-pinned folders (from Settings, not the hardcoded system buckets)
    val userPinnedFolderNames: StateFlow<Set<String>> = settingsRepository.pinnedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val pinnedFoldersOrder: StateFlow<List<String>> = settingsRepository.pinnedFoldersOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPinnedAlbums: StateFlow<List<AlbumBucket>> = combine(
        allAlbums,
        settingsRepository.pinnedFoldersFlow
    ) { albums, pinnedNames ->
        albums.filter { it.bucketName in pinnedNames }
        // Custom covers already baked into allAlbums items above
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun togglePinAlbum(bucketName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.togglePinnedFolder(bucketName)
        }
    }

    fun updatePinnedFoldersOrder(newOrder: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.savePinnedFoldersOrder(newOrder)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentSearches: StateFlow<List<String>> = settingsRepository.recentSearchesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addRecentSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.addRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.clearRecentSearches()
        }
    }


    private var searchJob: kotlinx.coroutines.Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _ftsSearchResults.value = emptyList()
            _smartSearchResults.value = emptyList()
            _isSearching.value = false
            searchJob?.cancel()
            return
        }
        // Debounce: wait 300ms after user stops typing, then fire searches
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            addRecentSearch(query)
            performUnifiedSearch(query)
        }
    }

    private val _ftsSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val ftsSearchResults: StateFlow<List<GalleryItem>> = _ftsSearchResults.asStateFlow()

    private val _smartSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val smartSearchResults: StateFlow<List<GalleryItem>> = _smartSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val smartSearchStatus: StateFlow<SmartSearchStatus> = combine(
        database.mediaDao().observeTotalImageCount(),
        database.embeddingDao().observeUnindexedCount(),
        com.inferno.gallery.data.IndexingProgressManager.clipProgress
    ) { totalCount, unindexedCount, clipProgress ->
        val indexed = (totalCount - unindexedCount).coerceAtLeast(0)
        SmartSearchStatus(
            modelDownloaded = true,
            isIndexing = clipProgress.isIndexing,
            indexedCount = indexed,
            totalCount = totalCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartSearchStatus(false, false, 0, 0))

    /** Whether the smart search ONNX model is downloaded and ready for inference. */
    val isSmartSearchReady: StateFlow<Boolean> = smartSearchStatus
        .map { it.modelDownloaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private suspend fun performUnifiedSearch(query: String) {
        _isSearching.value = true
        val currentExcluded = excludedFolders.value
        try {
            kotlinx.coroutines.coroutineScope {
                    val ftsDeferred = async(Dispatchers.IO) {
                        try {
                            val ftsEntities = if (query.isNotBlank()) {
                                DatabaseProvider.searchFts(database, query, currentExcluded)
                            } else {
                                emptyList()
                            }
                            ftsEntities.map { entity ->
                                val uri = Uri.parse(entity.uriString)
                                val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                                GalleryItem(
                                    id = entity.id.toString(),
                                    uri = uri,
                                    bucketName = entity.bucketName,
                                    dateAdded = entity.dateAdded,
                                    size = entity.size,
                                    name = entity.name,
                                    dateModified = entity.dateModified,
                                    path = entity.filePath,
                                    isVideo = entity.isVideo,
                                    durationMs = entity.durationMs,
                                    localExists = exists,
                                    resolvedUri = resolved
                                )
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            android.util.Log.e("GalleryViewModel", "FTS search failed: ${e.message}")
                            emptyList()
                        }
                    }

                    val smartDeferred = async(Dispatchers.Default) {
                        try {
                            val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication())
                            if (searchEngine.isModelDownloaded()) {
                                if (!searchEngine.isLoaded()) {
                                    searchEngine.loadModel()
                                }
                                val queryVector = searchEngine.encodeText(query)
                                val allEmbeddings = database.embeddingDao().getAllEmbeddings()
                                if (allEmbeddings.isEmpty()) return@async emptyList()

                                val threshold = 0.27f
                                android.util.Log.i(
                                    "GalleryViewModel",
                                    "Smart search for: '$query'. Query vector sample: [${queryVector.take(3).joinToString(", ")}], Total DB embeddings: ${allEmbeddings.size}, Threshold: $threshold"
                                )

                                val scored = allEmbeddings.map { record ->
                                    record.mediaId to searchEngine.dotProduct(queryVector, record.embedding)
                                }.sortedByDescending { it.second }

                                val topScore = scored.firstOrNull()?.second ?: return@async emptyList()
                                val adaptiveThreshold = maxOf(threshold, topScore - 0.06f)
                                val maxResults = 200
                                val matched = scored.filter { it.second >= adaptiveThreshold }.take(maxResults)

                                if (matched.isNotEmpty()) {
                                    val scoreMap = matched.associate { it.first to it.second }
                                    val matchedIds = matched.map { it.first }
                                    withContext(Dispatchers.IO) {
                                        val entitiesMap = database.mediaDao().getMediaByIdsList(matchedIds).associateBy { it.id }
                                        val orderedEntities = matchedIds.mapNotNull { entitiesMap[it] }
                                            .filter { !currentExcluded.contains(it.bucketName) && it.bucketName != "Trash" }

                                        orderedEntities.map { entity ->
                                            val uri = Uri.parse(entity.uriString)
                                            val (exists, resolved) = resolveItemFields(uri, entity.filePath)
                                            GalleryItem(
                                                id = entity.id.toString(),
                                                uri = uri,
                                                bucketName = entity.bucketName,
                                                dateAdded = entity.dateAdded,
                                                size = entity.size,
                                                name = entity.name,
                                                dateModified = entity.dateModified,
                                                path = entity.filePath,
                                                isVideo = entity.isVideo,
                                                durationMs = entity.durationMs,
                                                searchScore = scoreMap[entity.id],
                                                localExists = exists,
                                                resolvedUri = resolved
                                            )
                                        }
                                    }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            android.util.Log.e("GalleryViewModel", "Smart search failed: ${e.message}")
                            emptyList()
                        }
                    }

                    val ftsResults = ftsDeferred.await()
                    val smartResults = smartDeferred.await()
                    val ftsIds = ftsResults.map { it.id }.toSet()
                    val filteredSmartResults = smartResults.filterNot { it.id in ftsIds }

                    if (query == _searchQuery.value) {
                        _ftsSearchResults.value = ftsResults
                        _smartSearchResults.value = filteredSmartResults
                    }
                }
            } finally {
                if (query == _searchQuery.value) {
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
            // Also remove from the detail pager list immediately
            _detailMedia.value = _detailMedia.value.filter { it.uri.toString() != uriString }
            _uiEvents.send(UiEvent.DeleteSuccess)
        }
    }

    fun moveSelectedMedia(targetBucket: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                    val resolver = getApplication<android.app.Application>().contentResolver
                    for (uriString in selected) {
                        val uri = Uri.parse(uriString)
                        if (uriString.startsWith("content://")) {
                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/" + targetBucket)
                            }
                            try {
                                resolver.update(uri, values, null, null)
                                database.mediaDao().updateBucketByUri(uriString, targetBucket)
                            } catch (e: Exception) {
                                android.util.Log.e("GalleryViewModel", "Failed to move media: ${e.message}")
                            }
                        }
                    }
                    clearSelection()
                    withContext(Dispatchers.Main) {
                        showToast("Moved to $targetBucket")
                    }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error moving media: ${e.message}", e)
            }
        }
    }

    fun moveSelectedMediaToPath(targetDirectoryPath: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                val targetDir = java.io.File(targetDirectoryPath)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val allMedia = database.mediaDao().getAllMedia().associateBy { it.uriString }
                val movedPaths = mutableListOf<String>()

                for (uriString in selected) {
                    val entity = allMedia[uriString] ?: continue
                    val srcFile = java.io.File(entity.filePath)
                    if (srcFile.exists()) {
                        val destFile = java.io.File(targetDir, srcFile.name)
                        if (srcFile.renameTo(destFile)) {
                            // Update Room DB immediately
                            database.mediaDao().updatePathAndBucket(
                                id = entity.id,
                                newPath = destFile.absolutePath,
                                newBucket = targetDir.name,
                                newName = destFile.name
                            )
                            movedPaths.add(srcFile.absolutePath)
                            movedPaths.add(destFile.absolutePath)
                        }
                    }
                }

                if (movedPaths.isNotEmpty()) {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        movedPaths.toTypedArray(),
                        null,
                        null
                    )
                }

                withContext(Dispatchers.Main) {
                    clearSelection()
                    showToast("Moved to ${targetDir.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error moving media to path: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error moving media: ${e.message}")
                }
            }
        }
    }

    fun copySelectedMedia(targetBucket: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                    val resolver = getApplication<android.app.Application>().contentResolver
                    var successCount = 0
                    for (uriString in selected) {
                        val uri = Uri.parse(uriString)
                        if (uriString.startsWith("content://")) {
                            // 1. Get info about original file
                            var displayName: String? = null
                            var mimeType: String? = null
                            resolver.query(uri, arrayOf(
                                android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                                android.provider.MediaStore.MediaColumns.MIME_TYPE
                            ), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    displayName = cursor.getString(0)
                                    mimeType = cursor.getString(1)
                                }
                            }

                            if (displayName == null) {
                                displayName = "copied_media_${System.currentTimeMillis()}"
                            }
                            if (mimeType == null) {
                                mimeType = "image/jpeg"
                            }

                            // 2. Insert copy entry in MediaStore
                            val isVideo = mimeType?.startsWith("video/") == true
                            val baseUri = if (isVideo) {
                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            } else {
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            }

                            val values = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/" + targetBucket)
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                            }

                            val newUri = resolver.insert(baseUri, values)
                            if (newUri != null) {
                                try {
                                    // 3. Copy bytes
                                    resolver.openInputStream(uri)?.use { input ->
                                        resolver.openOutputStream(newUri)?.use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    // 4. Release pending status
                                    val updateValues = android.content.ContentValues().apply {
                                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                                    }
                                    resolver.update(newUri, updateValues, null, null)
                                    successCount++
                                } catch (e: Exception) {
                                    android.util.Log.e("GalleryViewModel", "Failed copy stream: ${e.message}")
                                    resolver.delete(newUri, null, null)
                                }
                            }
                        }
                    }

                    if (successCount > 0) {
                        // Trigger MediaSyncWorker to update our database and grid UI
                        val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                            "MediaSyncWorker",
                            androidx.work.ExistingWorkPolicy.REPLACE,
                            syncWorkRequest
                        )
                    }

                    clearSelection()
                    withContext(Dispatchers.Main) {
                        showToast("Copied $successCount items to $targetBucket")
                    }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error copying media: ${e.message}", e)
            }
        }
    }

    fun copySelectedMediaToPath(targetDirectoryPath: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                val targetDir = java.io.File(targetDirectoryPath)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val allMedia = database.mediaDao().getAllMedia().associateBy { it.uriString }
                val copiedPaths = mutableListOf<String>()

                for (uriString in selected) {
                    val entity = allMedia[uriString] ?: continue
                    val srcFile = java.io.File(entity.filePath)
                    if (srcFile.exists()) {
                        val destFile = java.io.File(targetDir, srcFile.name)
                        try {
                            srcFile.copyTo(destFile, overwrite = true)
                            copiedPaths.add(destFile.absolutePath)
                        } catch (copyEx: Exception) {
                            android.util.Log.e("GalleryViewModel", "Failed to copy physical file: ${copyEx.message}", copyEx)
                        }
                    }
                }

                if (copiedPaths.isNotEmpty()) {
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        copiedPaths.toTypedArray(),
                        null,
                        null
                    )
                    
                    val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "MediaSyncWorker",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        syncWorkRequest
                    )
                }

                withContext(Dispatchers.Main) {
                    clearSelection()
                    showToast("Copied to ${targetDir.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error copying media to path: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error copying media: ${e.message}")
                }
            }
        }
    }

    fun deleteSelectedMediaFromDb(uris: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (uriString in uris) {
                database.mediaDao().deleteByUri(uriString)
            }
        }
    }

    fun createAlbum(albumName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val newFolder = java.io.File(picturesDir, albumName)
                if (newFolder.exists()) {
                    withContext(Dispatchers.Main) {
                        onError("Album directory already exists")
                    }
                    return@launch
                }
                val success = newFolder.mkdirs()
                if (success) {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        arrayOf(newFolder.absolutePath),
                        null
                    ) { _, _ -> }

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Could not create directory. Make sure you have storage permissions.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error")
                }
            }
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

    // ── Face Indexing & Clustering Methods ────────────────────────────────

    val isFaceModelDownloaded: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        val faceEngine = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(getApplication())
        while (true) {
            emit(faceEngine.isModelDownloaded())
            kotlinx.coroutines.delay(2000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.ai.FaceRecognitionEngine.getInstance(application).isModelDownloaded()
    )

    val faceModelDownloadWorkInfo: StateFlow<androidx.work.WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceModelDownloadWorker")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun startFaceModelDownload() {
        viewModelScope.launch {
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceModelDownloadWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("FaceModelDownloadWorker", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancelFaceModelDownload() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceModelDownloadWorker")
        }
    }

    val faceIndexWorkInfo = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceIndexWorker")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun startFaceIndexing() {
        viewModelScope.launch {
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("FaceIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun stopFaceIndexing() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceIndexWorker")
            IndexingProgressManager.updateFaceProgress(isIndexing = false, progress = 0, total = 0)
        }
    }

    fun clearFaceIndexAndReindex() {
        viewModelScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceIndexWorker")
            database.faceDao().clearAllFaces()
            database.faceDao().clearAllClusters()
            database.faceDao().resetFaceIndexStatus()
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("FaceIndexWorker", ExistingWorkPolicy.REPLACE, request)
            showToast("Re-indexing faces with improved AI engine…")
        }
    }

    fun reclusterFaces() {
        viewModelScope.launch(Dispatchers.IO) {
            FaceClusteringManager.getInstance(getApplication<Application>()).runGraphClustering()
            showToast("Faces re-clustered successfully")
        }
    }

    fun updatePersonName(clusterId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().updatePersonName(clusterId, name.trim())
            showToast(if (name.isBlank()) "Name removed" else "Named $name")
        }
    }

    fun mergePersonClusters(sourceClusterId: Long, targetClusterId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().mergeClusters(sourceClusterId, targetClusterId)
            showToast("People merged successfully")
        }
    }

    fun setPersonFavorite(clusterId: Long, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().setClusterFavorite(clusterId, isFavorite)
        }
    }

    fun setPersonHidden(clusterId: Long, isHidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().setClusterHidden(clusterId, isHidden)
            showToast(if (isHidden) "Person hidden" else "Person unhidden")
        }
    }

    fun setPersonCover(clusterId: Long, faceId: Long, mediaId: Long, cropPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().updateClusterCover(clusterId, faceId, mediaId, cropPath)
            showToast("Cover photo updated")
        }
    }

    fun deletePersonCluster(clusterId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.faceDao().deleteFacesForCluster(clusterId)
            database.faceDao().deleteClusterOnly(clusterId)
            showToast("Person removed")
        }
    }

    fun observeMediaForCluster(clusterId: Long): Flow<List<GalleryItem>> {
        return combine(database.faceDao().observeMediaForCluster(clusterId), excludedFolders) { entities, excluded ->
            entities
                .filter { !excluded.contains(it.bucketName) && it.bucketName != "Trash" }
                .map { entity ->
                    GalleryItem(
                        id = entity.id.toString(),
                        uri = Uri.parse(entity.uriString),
                        path = entity.filePath,
                        name = entity.name,
                        dateAdded = entity.dateAdded,
                        dateModified = entity.dateModified,
                        size = entity.size,
                        isVideo = entity.isVideo,
                        durationMs = entity.durationMs,
                        bucketName = entity.bucketName
                    )
                }
        }.flowOn(Dispatchers.IO)
    }

    fun observeFacesForMedia(mediaId: Long): Flow<List<FaceEntity>> {
        return database.faceDao().observeFacesForMedia(mediaId)
    }



    sealed class UiEvent {
        object DeleteSuccess : UiEvent()
    }

    data class SystemStatus(val isOffline: Boolean)

    val systemStatus: StateFlow<SystemStatus> = kotlinx.coroutines.flow.flow {
        while (true) {
            val offline = !isNetworkConnected(getApplication())
            emit(SystemStatus(offline))
            kotlinx.coroutines.delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SystemStatus(false))

    private fun isNetworkConnected(context: android.content.Context): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val rawPagerFlow: Flow<PagingData<com.inferno.gallery.data.db.CoreMediaProjection>> = combine(
        _currentBucket, selectedFilterIndex, sortOrder, favoritesManager.favoritesFlow, excludedFolders
    ) { bucket, filterIndex, order, favs, excluded ->
        val qc = buildMediaConditions(
            bucket = bucket,
            filterIndex = filterIndex,
            excluded = excluded,
            favIds = favs,
            ftsIds = ftsSearchResults.value.map { it.id },
            smartIds = smartSearchResults.value.map { it.id }
        )
        val smartIds = smartSearchResults.value.map { it.id }
        val queryString = "SELECT cm.* FROM core_media cm " +
            buildWhereClause(qc) + buildOrderClause(order, bucket, smartIds)

        androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
    }.flatMapLatest { query ->
        Pager(
            config = PagingConfig(pageSize = 120, prefetchDistance = 180, enablePlaceholders = true)
        ) {
            database.mediaDao().observeMediaPagingRaw(query)
        }.flow
    }.cachedIn(viewModelScope)

    // Simplified paging pipeline — direct entity-to-GalleryItem mapping.
    // Previously this was a combine() of 5 sources (rawPagerFlow + photoStacksEnabledFlow +
    // observeUnstackedIds + duplicates + similarPhotos). Any emission from any of those 5
    // flows fully rebuilt the PagingData and caused visible grid stutter even while stationary.
    // Photo stacks feature is detached from the grid pipeline; duplicates / similarPhotos
    // StateFlows are kept for the Cleaner feature and are unaffected.
    val pagedMediaRaw: Flow<PagingData<GalleryItem>> = rawPagerFlow.map { pagingData ->
        pagingData.map { entity ->
            val uri = Uri.parse(entity.uriString)
            GalleryItem(
                id = entity.id.toString(),
                uri = uri,
                bucketName = entity.bucketName,
                dateAdded = entity.dateAdded,
                size = entity.size,
                name = entity.name,
                dateModified = entity.dateModified,
                path = entity.filePath,
                isVideo = entity.isVideo,
                durationMs = entity.durationMs,
                localExists = true,
                resolvedUri = resolveFileUri(uri, entity.filePath),
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }.cachedIn(viewModelScope)

    companion object {
        private val headerFormatterCurrentYear = java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.getDefault())
        private val headerFormatterOtherYear = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
        private val headerCache = java.util.concurrent.ConcurrentHashMap<Long, String>()
    }

    private fun formatGroupHeader(dateAddedSeconds: Long): String {
        val timeMs = dateAddedSeconds * 1000L
        val offsetMs = java.util.TimeZone.getDefault().getOffset(timeMs).toLong()
        val localDay = (timeMs + offsetMs) / 86400000L

        return headerCache.getOrPut(localDay) {
            val itemDate = java.time.LocalDate.ofEpochDay(localDay)
            val today = java.time.LocalDate.now()
            val yesterday = today.minusDays(1)

            when (itemDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> {
                    if (itemDate.year == today.year) {
                        itemDate.format(headerFormatterCurrentYear)
                    } else {
                        itemDate.format(headerFormatterOtherYear)
                    }
                }
            }
        }
    }

    val pagedMedia: Flow<PagingData<GalleryListItem>> = combine(viewMode, sortOrder) { mode, order ->
        Pair(mode, order)
    }.flatMapLatest { (mode, order) ->
        val isDateSort = order == SortOrder.NewToOld || order == SortOrder.OldToNew
        pagedMediaRaw.map { pagingData ->
            if (mode == ViewMode.Immersive || !isDateSort) {
                // No date headers in Immersive mode or non-date sorts
                pagingData.map { GalleryListItem.Item(it) as GalleryListItem }
            } else {
                pagingData.insertSeparators { before: GalleryItem?, after: GalleryItem? ->
                    if (after == null) return@insertSeparators null

                    val afterTitle = formatGroupHeader(after.dateAdded)

                    if (before == null) {
                        GalleryListItem.Header(afterTitle)
                    } else {
                        val beforeTitle = formatGroupHeader(before.dateAdded)
                        if (beforeTitle != afterTitle) {
                            GalleryListItem.Header(afterTitle)
                        } else {
                            null
                        }
                    }
                }.map { item ->
                    if (item is GalleryItem) GalleryListItem.Item(item) else item as GalleryListItem.Header
                }
            }
        }.cachedIn(viewModelScope)
    }
}
