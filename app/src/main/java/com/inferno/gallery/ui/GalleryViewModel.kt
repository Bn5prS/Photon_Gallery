@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.inferno.gallery.ui

import android.app.Application
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.MediaEntity
import com.inferno.gallery.data.db.MediaFaceQuality
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.data.FavoritesManager
import com.inferno.gallery.data.VaultAuthManager
import com.inferno.gallery.data.BucketNames
import com.inferno.gallery.data.AlbumLayoutMode
import com.inferno.gallery.data.MediaQueryBuilder
import com.inferno.gallery.data.db.PersonEntity
import android.util.Log

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
import com.inferno.gallery.workers.ReverseGeocodeWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    val fileHash: String? = null,
    val matchType: String? = null,
    val matchSnippet: String? = null,
    val uriString: String = uri.toString(),
    val stackCount: Int = 1
)

@Immutable
data class SmartSearchStatus(
    val modelDownloaded: Boolean,
    val isIndexing: Boolean,
    val indexedCount: Int,
    val totalCount: Int,
    val isModelDownloading: Boolean = false,
    val modelDownloadProgress: Int = 0
)

sealed class GalleryListItem {
    data class Item(val galleryItem: GalleryItem) : GalleryListItem()
    data class Header(val title: String, val id: String = title) : GalleryListItem()
}

internal fun mapMediaFaceQualities(rows: List<MediaFaceQuality>): Map<String, Float> =
    rows.associate { it.mediaId.toString() to it.maxQuality }

/**
 * Stable wrapper around the current selection set.
 *
 * `kotlin.collections.Set` is an unstable Compose type, so passing a raw `Set<String>` into
 * the grid made [com.inferno.gallery.ui.components.PhotonGrid] permanently unskippable under
 * strong skipping: every selection tick rebuilt the set, so all ~100 visible cells
 * re-subcomposed — 60x/second during drag-select. Instances are replaced (never mutated) on
 * each selection change, so equality is content-based and skipping is correct.
 */
@Immutable
data class StableUriSet(val uris: Set<String>) {
    val isEmpty: Boolean get() = uris.isEmpty()
    operator fun contains(uri: String): Boolean = uris.contains(uri)
    companion object {
        val Empty = StableUriSet(emptySet())
    }
}

enum class SortOrder {
    NewToOld,
    OldToNew,
    DateModifiedNewToOld,
    DateModifiedOldToNew,
    SmallToBig,
    BigToSmall,
    NameAsc,
    NameDesc,
    CountDesc,
    CountAsc
}

enum class ViewMode {
    Immersive,
    Grouped
}

enum class TimelineGroupBy {
    Day,
    Month,
    Year
}

@Immutable
data class AlbumBucket(
    val bucketName: String,
    val coverUri: Uri,
    val itemCount: Int,
    val totalSizeBytes: Long = 0L,
    val maxDate: Long = 0L,
    val coverUris: List<Uri> = emptyList(),
    val defaultCoverUri: Uri = coverUri
)

@Immutable
data class DuplicateGroup(
    val fileHash: String,
    val items: List<GalleryItem>
)

@Immutable
data class SimilarPhotoItem(
    val item: GalleryItem,
    val sharpness: Float,
    val pixelCount: Long,
    val isBestShot: Boolean
)

@Immutable
data class SimilarPhotoGroup(
    val id: String,
    val items: List<SimilarPhotoItem>,
    val isBurst: Boolean,
    val similarityPct: Int
)

@Immutable
data class BlurryPhotoItem(
    val item: GalleryItem,
    val sharpness: Float,
    val pixelCount: Long
)

sealed class DuplicateScanState {
    data object Idle : DuplicateScanState()
    data class Scanning(val processed: Int, val total: Int, val stage: String = "") : DuplicateScanState()
    data object Done : DuplicateScanState()
}

enum class SearchMode { SMART, FTS }

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application.contentResolver)
    val settingsRepository = SettingsRepository.getInstance(application)
    private val favoritesManager = FavoritesManager(application)
    val favoriteIds: StateFlow<Set<String>> = favoritesManager.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    private val database = DatabaseProvider.getDatabase(application)
    val faceQualityByMediaId: StateFlow<Map<String, Float>> = database.faceDao()
        .observeMediaFaceQualities()
        .map(::mapMediaFaceQualities)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    

    // UI State for Place Clusters
    private val _placesClusters = MutableStateFlow<List<com.inferno.gallery.data.db.BucketInfo>>(emptyList())
    val placesClusters: StateFlow<List<com.inferno.gallery.data.db.BucketInfo>> = _placesClusters.asStateFlow()

    // Map of mediaId → placeName from geocoded_locations cache (used by PhotoMapScreen)
    val placeNameByMediaId: StateFlow<Map<Long, String>> = database.placesDao()
        .observeAllGeocodedLocations()
        .map { locations -> locations.associate { it.mediaId to it.placeName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ── Duplicate & Similar Photo Scan State ──
    private val _duplicateScanState = MutableStateFlow<DuplicateScanState>(DuplicateScanState.Idle)
    val duplicateScanState: StateFlow<DuplicateScanState> = _duplicateScanState.asStateFlow()
    private val _qualityCacheTick = MutableStateFlow(0)

    // ── Toast Event Channel ──
    private val _toastEvent = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private fun showToast(message: String) {
        _toastEvent.trySend(message)
    }

    /** True while the initial fast sync is running — used to show loading UI on first launch. */
    private val _isInitialSyncRunning = MutableStateFlow(false)
    val isInitialSyncRunning: StateFlow<Boolean> = _isInitialSyncRunning.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isTopBarCollapsed = MutableStateFlow(false)
    val isTopBarCollapsed: StateFlow<Boolean> = _isTopBarCollapsed.asStateFlow()

    fun setTopBarCollapsed(collapsed: Boolean) {
        if (_isTopBarCollapsed.value != collapsed) {
            _isTopBarCollapsed.value = collapsed
        }
    }

    // ── Trash & Delete In-Session State ──
    var sessionSkipDeleteConfirmation by mutableStateOf(false)
        private set

    var sessionSkipRecycleBin by mutableStateOf(false)
        private set

    fun setTrashSessionOptions(skipConfirm: Boolean, skipRecycleBin: Boolean) {
        sessionSkipDeleteConfirmation = skipConfirm
        sessionSkipRecycleBin = skipRecycleBin
    }

    fun resetTrashSessionOptions() {
        sessionSkipDeleteConfirmation = false
        sessionSkipRecycleBin = false
    }

    // ── On This Day Memories State ──
    private val _onThisDayMemories = MutableStateFlow<List<com.inferno.gallery.data.MemoryDayGroup>>(emptyList())
    val onThisDayMemories: StateFlow<List<com.inferno.gallery.data.MemoryDayGroup>> = _onThisDayMemories.asStateFlow()

    private val _isMemoriesDismissed = MutableStateFlow(false)
    val isMemoriesDismissed: StateFlow<Boolean> = _isMemoriesDismissed.asStateFlow()

    val showMemoriesSetting: StateFlow<Boolean> = settingsRepository.showMemoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val memoriesCarouselStyle: StateFlow<com.inferno.gallery.data.MemoriesCarouselStyle> = settingsRepository.memoriesCarouselStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.MemoriesCarouselStyle.SPOTLIGHT
    )

    val pinnedAlbumsStyle: StateFlow<com.inferno.gallery.data.PinnedAlbumsStyle> = settingsRepository.pinnedAlbumsStyleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.inferno.gallery.data.PinnedAlbumsStyle.DEFAULT
    )

    fun dismissMemories() {
        _isMemoriesDismissed.value = true
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateMemoriesDismissed(true)
        }
    }

    fun uncollapseMemories() {
        _isMemoriesDismissed.value = false
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateMemoriesDismissed(false)
        }
    }

    fun refreshMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val excluded = settingsRepository.excludedFoldersFlow.first()
                val favs = favoritesManager.favoritesFlow.first()
                val groups = com.inferno.gallery.data.MemoriesManager.loadOnThisDayMemories(
                    mediaDao = database.mediaDao(),
                    excludedFolders = excluded,
                    favoriteIds = favs
                )
                _onThisDayMemories.value = groups
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to load On This Day memories", e)
            }
        }
    }

    fun refreshMedia(onComplete: (() -> Unit)? = null) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val startedAt = android.os.SystemClock.elapsedRealtime()
            try {
                withContext(Dispatchers.IO) {
                    val engine = com.inferno.gallery.data.MediaSyncEngine.getInstance(getApplication())
                    engine.sync(forceFullSync = true)
                    if (settingsRepository.photoStacksEnabledFlow.first()) {
                        photoStackBurstHashStarted.set(false)
                        recomputePhotoStacks(
                            manualPicks = settingsRepository.photoStackBestOverridesFlow.first(),
                            unstackedIds = settingsRepository.photoStackUnstackedIdsFlow.first(),
                            manualGroups = settingsRepository.photoStackManualGroupsFlow.first(),
                            allowLazyHash = true
                        )
                    }
                }
                refreshMemories()
                refreshEmptyAlbums()
                nativeTimelineRepository.refresh()
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Refresh failed", e)
            } finally {
                val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
                val minIndicatorMs = 520L
                if (elapsed < minIndicatorMs) {
                    kotlinx.coroutines.delay(minIndicatorMs - elapsed)
                }
                _isRefreshing.value = false
                onComplete?.invoke()
            }
        }
    }

    // ── Albums Section Expansion Persistence ──
    val albumsExpandedPinned: StateFlow<Boolean> = settingsRepository.albumsExpandedPinnedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val albumsExpandedMore: StateFlow<Boolean> = settingsRepository.albumsExpandedMoreFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val albumsExpandedPeople: StateFlow<Boolean> = settingsRepository.albumsExpandedPeopleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val albumsExpandedPlaces: StateFlow<Boolean> = settingsRepository.albumsExpandedPlacesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val albumsExpandedMediaTypes: StateFlow<Boolean> = settingsRepository.albumsExpandedMediaTypesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    val viewerToolbarActions: StateFlow<List<String>> = settingsRepository.viewerToolbarActionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.inferno.gallery.ui.model.ViewerToolbarAction.DEFAULTS
    )

    val selectionDockActions: StateFlow<List<String>> = settingsRepository.selectionDockActionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = com.inferno.gallery.ui.model.SelectionDockAction.DEFAULTS
    )
    val albumsExpandedDocuments: StateFlow<Boolean> = settingsRepository.albumsExpandedDocumentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val albumsExpandedUtilities: StateFlow<Boolean> = settingsRepository.albumsExpandedUtilitiesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun toggleAlbumsExpandedPinned() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedPinned(!albumsExpandedPinned.value)
        }
    }

    fun toggleAlbumsExpandedMore() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedMore(!albumsExpandedMore.value)
        }
    }

    fun toggleAlbumsExpandedPeople() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedPeople(!albumsExpandedPeople.value)
        }
    }

    fun toggleAlbumsExpandedPlaces() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedPlaces(!albumsExpandedPlaces.value)
        }
    }

    fun toggleAlbumsExpandedMediaTypes() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedMediaTypes(!albumsExpandedMediaTypes.value)
        }
    }

    fun toggleAlbumsExpandedDocuments() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedDocuments(!albumsExpandedDocuments.value)
        }
    }

    fun toggleAlbumsExpandedUtilities() {
        viewModelScope.launch {
            settingsRepository.updateAlbumsExpandedUtilities(!albumsExpandedUtilities.value)
        }
    }

    // Only one start-up sync at a time. Several onResume calls on first open used to start
    // up to five full MediaStore reads in parallel.
    // Declared before init {} because init calls runFastInitialSync().
    private val initialSyncInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    private val mediaStoreChangeChannel = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private val observerThread = android.os.HandlerThread("MediaStoreObserver").apply { start() }
    private val observerHandler = android.os.Handler(observerThread.looper)

    private val mediaStoreObserver = object : android.database.ContentObserver(observerHandler) {
        override fun onChange(selfChange: Boolean) {
            mediaStoreChangeChannel.trySend(Unit)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.mediaDao().normalizeExternalPrimaryUris()
            } catch (_: Exception) {}
        }

        // Ensure default filter is 0 (All Photos)
        viewModelScope.launch {
            settingsRepository.updateSelectedFilterIndex(0)
        }

        // Collect place clusters
        viewModelScope.launch(Dispatchers.IO) {
            database.placesDao().observePlaceClusters().collect { clusters ->
                _placesClusters.value = clusters
            }
        }

        // Fast initial sync: if Room is empty, bulk-insert MediaStore metadata
        // directly on IO. This populates the grid in sub-second time instead of waiting
        // for WorkManager to schedule and execute MediaSyncWorker.
        runFastInitialSync()

        // Auto-trigger GPS extraction & Reverse Geocoding in background on startup
        val geocodeRequest = androidx.work.OneTimeWorkRequestBuilder<ReverseGeocodeWorker>().build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "ReverseGeocodeWorker",
            ExistingWorkPolicy.KEEP,
            geocodeRequest
        )

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

        // Coalesce MediaStore change bursts on IO without cancelling an active sync mid-flight.
        // During large PC-to-phone imports, cancelling sync() prevents last_generation from saving
        // and starves Room/Paging3; instead, let each sync finish and throttle consecutive runs.
        viewModelScope.launch(Dispatchers.IO) {
            for (signal in mediaStoreChangeChannel) {
                // Act on the change at once, like the system gallery. No delay: events that
                // arrive while this sync runs are conflated by the channel into one more pass.
                try {
                    Log.d("GalleryViewModel", "MediaStore change detected, executing coalesced incremental sync...")
                    val syncResult = com.inferno.gallery.data.MediaSyncEngine.getInstance(getApplication()).syncDelta()
                    if (syncResult.inserted > 0 || syncResult.deleted > 0) {
                        nativeTimelineRepository.refresh()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("GalleryViewModel", "Fast incremental sync failed: ${e.message}")
                }
            }
        }

        // Silently scan for duplicates and photo spheres in the background on startup
        scanForDuplicates()
        scanPhotoSpheres()
        viewModelScope.launch(Dispatchers.IO) {
            val currentVersion = 3
            val storedVersion = settingsRepository.docClassifierVersionFlow.first()
            val force = storedVersion < currentVersion
            DatabaseProvider.backfillDocumentClassifications(database, forceReclassify = force)
            if (force) {
                settingsRepository.updateDocClassifierVersion(currentVersion)
            }
        }

        // Collect & observe memories whenever excluded folders, favorites, or media count changes
        viewModelScope.launch(Dispatchers.IO) {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(
                settingsRepository.excludedFoldersFlow,
                favoritesManager.favoritesFlow,
                database.mediaDao().observeTotalImageCount()
            ) { excluded, favs, _ ->
                excluded to favs
            }.debounce(500L).collectLatest { (excluded, favs) ->
                try {
                    val groups = com.inferno.gallery.data.MemoriesManager.loadOnThisDayMemories(
                        mediaDao = database.mediaDao(),
                        excludedFolders = excluded,
                        favoriteIds = favs
                    )
                    _onThisDayMemories.value = groups
                } catch (e: Exception) {
                    Log.e("GalleryViewModel", "Error loading memories in collector", e)
                }
            }
        }

        // Observe memories dismissed state from DataStore settings
        viewModelScope.launch {
            settingsRepository.memoriesDismissedFlow.collect { dismissed ->
                _isMemoriesDismissed.value = dismissed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        mediaStoreChangeChannel.close()
        observerThread.quitSafely()
    }

    /**
     * Executes an immediate, sub-second fast synchronization on Dispatchers.IO.
     * When Room is empty (e.g. fresh install after permissions granted), this
     * queries MediaStore directly and bulk-inserts all entries into Room in ~200ms,
     * immediately populating the timeline without waiting 5-10s for WorkManager.
     */
    fun runFastInitialSync(force: Boolean = false) {
        if (!hasStoragePermissions(getApplication())) {
            android.util.Log.d("GalleryViewModel", "Fast sync deferred: storage permissions not yet granted")
            return
        }
        if (!initialSyncInFlight.compareAndSet(false, true)) return
        _isInitialSyncRunning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = database.mediaDao().getAllMediaCount()
                if (count == 0 || force) {
                    android.util.Log.d("GalleryViewModel", "Room empty or forced — running fast initial sync on IO")
                    // The engine does the one-time import (no per-file EXIF reads, saves the
                    // generation, queues a background date pass); a second caller just waits for it.
                    com.inferno.gallery.data.MediaSyncEngine.getInstance(getApplication()).syncDelta()
                    _isInitialSyncRunning.value = false
                    refreshMemories()
                    nativeTimelineRepository.refresh()
                    scanPhotoSpheres()
                } else {
                    _isInitialSyncRunning.value = false
                    try {
                        com.inferno.gallery.data.MediaSyncEngine.getInstance(getApplication()).syncDelta()
                        refreshMemories()
                        nativeTimelineRepository.refresh()
                        scanPhotoSpheres()
                    } catch (e: Exception) {
                        android.util.Log.e("GalleryViewModel", "Startup sync failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Fast sync failed: ${e.message}")
                _isInitialSyncRunning.value = false
            } finally {
                initialSyncInFlight.set(false)
            }
        }
    }

    /**
     * Resolves localExists and resolvedUri for a GalleryItem.
     */
    private fun resolveFileUri(uri: Uri, path: String, bucketName: String? = null): Uri {
        if (bucketName == "Trash") return uri
        val file = if (path.isNotEmpty()) java.io.File(path) else null
        return if (file != null && file.exists()) {
            Uri.fromFile(file)
        } else {
            uri
        }
    }

    private fun resolveItemFields(
        uri: Uri,
        path: String,
        bucketName: String? = null
    ): Pair<Boolean, Uri> {
        if (bucketName == "Trash") {
            return true to uri
        }
        val file = if (path.isNotEmpty()) java.io.File(path) else null
        val exists = file != null && file.exists()
        val resolved = if (exists) Uri.fromFile(file) else uri
        return exists to resolved
    }

    // ── Shared SQL Query Builder ──
    // Delegates to MediaQueryBuilder for testability
    private fun buildMediaConditions(
        bucket: String?,
        filterIndex: Int,
        excluded: Set<String> = emptySet(),
        favIds: Set<String> = emptySet(),
        ftsIds: List<String> = emptyList(),
        smartIds: List<String> = emptyList(),
        albumMediaFilter: com.inferno.gallery.data.AlbumMediaFilter = com.inferno.gallery.data.AlbumMediaFilter.ALL
    ): MediaQueryBuilder.QueryConditions {
        return MediaQueryBuilder.buildMediaConditions(bucket, filterIndex, excluded, favIds, ftsIds, smartIds, albumMediaFilter)
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

    fun lockVault() {
        vaultAuthManager.lock()
        vaultRepository.clearPreviewCache()
    }

    data class PendingVaultHideDelete(
        val intentSender: android.content.IntentSender,
        val staged: List<com.inferno.gallery.data.VaultRepository.StagedVaultItem>,
        val immediateCount: Int,
        val lockAfter: Boolean
    )

    private val _pendingVaultHideDelete = MutableStateFlow<PendingVaultHideDelete?>(null)
    val pendingVaultHideDelete: StateFlow<PendingVaultHideDelete?> = _pendingVaultHideDelete.asStateFlow()

    fun hideMedia(uris: List<Uri>, lockAfter: Boolean = false) {
        if (uris.isEmpty()) {
            if (lockAfter) lockVault()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var deferredLock = false
            try {
                val res = vaultRepository.hideMediaWithStaging(uris)
                val immediateCount = res.hiddenIds.size
                if (immediateCount > 0) {
                    nativeTimelineRepository.refresh()
                }
                if (res.stagedForDelete.isNotEmpty()) {
                    val intentSender = try {
                        android.provider.MediaStore.createDeleteRequest(
                            getApplication<Application>().contentResolver,
                            res.stagedForDelete.map { it.deleteUri }
                        ).intentSender
                    } catch (_: Exception) {
                        null
                    }
                    if (intentSender != null) {
                        deferredLock = lockAfter
                        _pendingVaultHideDelete.value = PendingVaultHideDelete(
                            intentSender = intentSender,
                            staged = res.stagedForDelete,
                            immediateCount = immediateCount,
                            lockAfter = lockAfter
                        )
                        return@launch
                    } else {
                        vaultRepository.rollbackStagedHide(res.stagedForDelete)
                    }
                }
                if (immediateCount > 0) {
                    showToast("$immediateCount item${if (immediateCount != 1) "s" else ""} moved to Private Space")
                } else {
                    showToast("Could not move item(s) to Private Space")
                }
            } finally {
                if (lockAfter && !deferredLock) {
                    lockVault()
                }
            }
        }
    }

    fun resolvePendingVaultHideDelete(approved: Boolean) {
        val pending = _pendingVaultHideDelete.value ?: return
        _pendingVaultHideDelete.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (approved) {
                    val stagedCount = vaultRepository.commitStagedHide(pending.staged).size
                    val total = pending.immediateCount + stagedCount
                    if (total > 0) {
                        nativeTimelineRepository.refresh()
                        showToast("$total item${if (total != 1) "s" else ""} moved to Private Space")
                    } else {
                        showToast("Could not move item(s) to Private Space")
                    }
                } else {
                    vaultRepository.rollbackStagedHide(pending.staged)
                    if (pending.immediateCount > 0) {
                        showToast("${pending.immediateCount} item(s) moved to Private Space")
                    } else {
                        showToast("Move to Private Space cancelled")
                    }
                }
            } finally {
                if (pending.lockAfter) {
                    lockVault()
                }
            }
        }
    }

    fun syncVaultOnUnlock() {
        viewModelScope.launch(Dispatchers.IO) {
            vaultRepository.syncAndMigrateOnUnlock()
        }
    }

    fun unhideMedia(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val count = vaultRepository.unhideMedia(ids)
            if (count > 0) {
                nativeTimelineRepository.refresh()
            }
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

    suspend fun getViewableVaultFile(entity: com.inferno.gallery.data.db.VaultMediaEntity): java.io.File? {
        return vaultRepository.getViewableVaultFile(entity)
    }

    fun clearVaultPreviewCache() {
        vaultRepository.clearPreviewCache()
    }

    fun exportVaultBackup(
        outputUri: Uri,
        password: String,
        onResult: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { vaultRepository.exportEncryptedBackup(outputUri, password) }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun importVaultBackup(
        inputUri: Uri,
        password: String,
        onResult: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { vaultRepository.importEncryptedBackup(inputUri, password) }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    private val _isScrollDockVisible = MutableStateFlow(true)
    val isScrollDockVisible: StateFlow<Boolean> = _isScrollDockVisible.asStateFlow()

    fun setScrollDockVisible(visible: Boolean) {
        _isScrollDockVisible.value = visible
    }

    private val _sortOrder = MutableStateFlow(SortOrder.NewToOld)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _albumSortOrder = MutableStateFlow(SortOrder.NameAsc)
    val albumSortOrder: StateFlow<SortOrder> = _albumSortOrder.asStateFlow()

    private val _albumLayoutMode = MutableStateFlow(AlbumLayoutMode.GRID)
    val albumLayoutMode: StateFlow<AlbumLayoutMode> = _albumLayoutMode.asStateFlow()

    val albumsGridCellsCount: StateFlow<Int> = settingsRepository.albumsGridCellsCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3
        )

    fun updateAlbumsGridCellsCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateAlbumsGridCellsCount(count)
        }
    }

    private val _viewMode = MutableStateFlow(ViewMode.Grouped)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _timelineGroupBy = MutableStateFlow(TimelineGroupBy.Day)
    val timelineGroupBy: StateFlow<TimelineGroupBy> = _timelineGroupBy.asStateFlow()

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

    val showVideosInDock: StateFlow<Boolean> = settingsRepository.showVideosInDockFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setShowVideosInDock(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowVideosInDock(show)
        }
    }

    val showPillLabels: StateFlow<Boolean> = settingsRepository.showPillLabelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
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

    // ── Timeline Photo Stacks (Burst & Rapid Similar Retake Grouping) ──

    val photoStacksEnabled: StateFlow<Boolean> = settingsRepository.photoStacksEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setPhotoStacksEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePhotoStacksEnabled(enabled)
        }
    }

    private val _photoStackIndex = MutableStateFlow(com.inferno.gallery.data.PhotoStackIndex.EMPTY)
    val photoStackIndex: StateFlow<com.inferno.gallery.data.PhotoStackIndex> = _photoStackIndex.asStateFlow()

    private val _restorableStacksByMemberId = MutableStateFlow<Map<Long, com.inferno.gallery.data.PhotoStack>>(emptyMap())
    val restorableStacksByMemberId: StateFlow<Map<Long, com.inferno.gallery.data.PhotoStack>> = _restorableStacksByMemberId.asStateFlow()

    private val _restorableStackCount = MutableStateFlow(0)
    val restorableStackCount: StateFlow<Int> = _restorableStackCount.asStateFlow()

    private data class PhotoStackInputs(
        val enabled: Boolean,
        val manualPicks: Map<Long, Long>,
        val unstackedIds: Set<Long>,
        val manualGroups: List<List<Long>>
    )

    private val photoStackBurstHashStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                settingsRepository.photoStacksEnabledFlow,
                settingsRepository.photoStackBestOverridesFlow,
                settingsRepository.photoStackUnstackedIdsFlow,
                settingsRepository.photoStackManualGroupsFlow,
                database.mediaDao().observeTotalImageCount().distinctUntilChanged()
            ) { enabled, manualPicks, unstackedIds, manualGroups, _ ->
                PhotoStackInputs(enabled, manualPicks, unstackedIds, manualGroups)
            }
                .debounce(120L)
                .collectLatest { inputs ->
                    if (!inputs.enabled) {
                        _photoStackIndex.value = com.inferno.gallery.data.PhotoStackIndex.EMPTY
                        _restorableStacksByMemberId.value = emptyMap()
                        _restorableStackCount.value = 0
                        return@collectLatest
                    }
                    recomputePhotoStacks(
                        manualPicks = inputs.manualPicks,
                        unstackedIds = inputs.unstackedIds,
                        manualGroups = inputs.manualGroups,
                        allowLazyHash = true
                    )
                }
        }
    }

    private suspend fun recomputePhotoStacks(
        manualPicks: Map<Long, Long>,
        unstackedIds: Set<Long>,
        manualGroups: List<List<Long>>,
        allowLazyHash: Boolean
    ) {
        try {
            val rows = database.mediaDao().getPhotoStackCandidates()
            val qualityCache = com.inferno.gallery.data.PhotoQualityCache.getInstance(getApplication())
            qualityCache.ensureLoaded()
            val candidates = rows.map { row ->
                val metrics = qualityCache.get(row.id, row.dateModified)
                com.inferno.gallery.data.PhotoStackCandidate(
                    id = row.id,
                    name = row.name,
                    bucketName = row.bucketName,
                    dateAddedSec = row.dateAdded,
                    sizeBytes = row.size,
                    pHash = row.pHash,
                    sharpness = metrics?.sharpness ?: 0f,
                    pixelCount = metrics?.pixelCount ?: 0L,
                    isVideo = row.isVideo
                )
            }
            val nextIndex = com.inferno.gallery.data.PhotoStackPlanner.buildStacks(
                candidates = candidates,
                manualBestShotPicks = manualPicks,
                unstackedIds = unstackedIds,
                manualGroups = manualGroups
            )
            if (_photoStackIndex.value != nextIndex) {
                _photoStackIndex.value = nextIndex
            }

            if (unstackedIds.isEmpty()) {
                if (_restorableStackCount.value != 0) _restorableStackCount.value = 0
                if (_restorableStacksByMemberId.value.isNotEmpty()) _restorableStacksByMemberId.value = emptyMap()
            } else {
                val restorable = com.inferno.gallery.data.PhotoStackPlanner.findRestorableAutoStacks(
                    candidates = candidates,
                    unstackedIds = unstackedIds,
                    manualGroups = manualGroups
                )
                _restorableStackCount.value = restorable.size
                val byMember = HashMap<Long, com.inferno.gallery.data.PhotoStack>()
                for (stack in restorable) {
                    for (mid in stack.memberIds) {
                        if (mid in unstackedIds) {
                            byMember[mid] = stack
                        }
                    }
                }
                _restorableStacksByMemberId.value = byMember
            }

            // Lightweight one-shot background hashing ONLY for unhashed burst neighbors (within 120s)
            if (allowLazyHash && photoStackBurstHashStarted.compareAndSet(false, true)) {
                val idsToHash = com.inferno.gallery.data.PhotoStackPlanner.findBurstHashCandidates(candidates, maxCount = 240)
                if (idsToHash.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val app = getApplication<Application>()
                            val entities = database.mediaDao().getMediaByIdsList(idsToHash)
                            val updatedHashes = ArrayList<Pair<Long, Long>>(entities.size)
                            val cache = com.inferno.gallery.data.PhotoQualityCache.getInstance(app)
                            cache.ensureLoaded()
                            for (entity in entities) {
                                val sig = com.inferno.gallery.data.SimilarPhotoAnalyzer.analyze(
                                    app.contentResolver,
                                    Uri.parse(entity.uriString),
                                    entity.filePath
                                ) ?: continue
                                updatedHashes.add(entity.id to sig.pHash)
                                cache.put(
                                    entity.id,
                                    com.inferno.gallery.data.PhotoQualityMetrics(
                                        dateModified = entity.dateModified,
                                        sharpness = sig.sharpness,
                                        pixelCount = sig.pixelCount
                                    )
                                )
                            }
                            if (updatedHashes.isNotEmpty()) {
                                database.mediaDao().updatePHashBatch(updatedHashes)
                                cache.persist()
                                val latestEnabled = settingsRepository.photoStacksEnabledFlow.first()
                                if (latestEnabled) {
                                    val latestPicks = settingsRepository.photoStackBestOverridesFlow.first()
                                    val latestUnstacked = settingsRepository.photoStackUnstackedIdsFlow.first()
                                    val latestManualGroups = settingsRepository.photoStackManualGroupsFlow.first()
                                    recomputePhotoStacks(
                                        manualPicks = latestPicks,
                                        unstackedIds = latestUnstacked,
                                        manualGroups = latestManualGroups,
                                        allowLazyHash = false
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("GalleryViewModel", "Lazy burst pHash pass failed: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GalleryViewModel", "Failed to compute photo stacks: ${e.message}")
        }
    }

    fun setPhotoStackBestShot(stack: com.inferno.gallery.data.PhotoStack, newBestShotId: Long) {
        if (newBestShotId !in stack.memberIds) return
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setPhotoStackBestShot(newBestShotId, stack.memberIds)
        }
    }

    fun unstackPhotoStack(stack: com.inferno.gallery.data.PhotoStack) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.unstackPhotos(stack.memberIds)
        }
    }

    fun undoUnstackPhotoStack(stack: com.inferno.gallery.data.PhotoStack) {
        viewModelScope.launch(Dispatchers.IO) {
            if (stack.isManual) {
                settingsRepository.createManualStack(
                    memberIds = stack.memberIds,
                    preferredBestShotId = if (stack.isManualBestPick) stack.bestShotId else null
                )
            } else {
                settingsRepository.restackPhotos(stack.memberIds)
            }
        }
    }

    fun restackPhotoWithSimilar(mediaId: Long, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetStack = _restorableStacksByMemberId.value[mediaId]
            if (targetStack != null && targetStack.memberIds.size >= 2) {
                settingsRepository.restackPhotos(targetStack.memberIds)
                withContext(Dispatchers.Main) {
                    onResult(targetStack.count)
                }
            } else {
                settingsRepository.restackPhotos(listOf(mediaId))
                withContext(Dispatchers.Main) {
                    onResult(0)
                }
            }
        }
    }

    fun restoreAllUnstackedPhotos(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = _restorableStackCount.value
            settingsRepository.clearAllUnstackedPhotos()
            withContext(Dispatchers.Main) {
                onResult(count)
            }
        }
    }

    fun stackSelectedUris(uriStrings: List<String>, onResult: (Int) -> Unit = {}) {
        if (uriStrings.size < 2) {
            onResult(0)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val matchedByUri = database.mediaDao().getMediaByUrisList(uriStrings)
            val matchedIds = matchedByUri.map { it.id }.toMutableSet()
            if (matchedIds.size < uriStrings.size) {
                val fallbackIds = uriStrings.mapNotNull { uri ->
                    uri.substringAfterLast('/').toLongOrNull()
                }.filter { it !in matchedIds }
                if (fallbackIds.isNotEmpty()) {
                    database.mediaDao().getMediaByIdsList(fallbackIds).forEach { matchedIds.add(it.id) }
                }
            }
            val entities = if (matchedIds.isEmpty()) emptyList() else database.mediaDao().getMediaByIdsList(matchedIds.toList())
            val eligiblePhotoIds = entities
                .filter { !it.isVideo && !it.bucketName.equals("Trash", ignoreCase = true) }
                .map { it.id }

            // Expand any selected stack cover so stacking a cover + extra photo merges cleanly into one stack
            val currentIndex = _photoStackIndex.value
            val expandedIds = LinkedHashSet<Long>()
            for (id in eligiblePhotoIds) {
                val existingStack = currentIndex.stacksByCoverId[id]
                if (existingStack != null) {
                    expandedIds.addAll(existingStack.memberIds)
                } else {
                    expandedIds.add(id)
                }
            }

            if (expandedIds.size < 2) {
                withContext(Dispatchers.Main) { onResult(0) }
                return@launch
            }

            settingsRepository.updatePhotoStacksEnabled(true)
            settingsRepository.createManualStack(expandedIds.toList())
            withContext(Dispatchers.Main) {
                clearSelection()
                onResult(expandedIds.size)
            }
        }
    }

    suspend fun getStackMemberItems(memberIds: List<Long>): List<GalleryItem> = withContext(Dispatchers.IO) {
        if (memberIds.isEmpty()) return@withContext emptyList()
        val entities = database.mediaDao().getMediaByIdsList(memberIds).associateBy { it.id }
        memberIds.mapNotNull { id ->
            val entity = entities[id] ?: return@mapNotNull null
            val uri = Uri.parse(entity.uriString)
            GalleryItem(
                id = entity.id.toString(),
                uri = uri,
                uriString = entity.uriString,
                bucketName = entity.bucketName,
                dateAdded = entity.dateAdded,
                size = entity.size,
                name = entity.name,
                dateModified = entity.dateModified,
                path = entity.filePath,
                isVideo = entity.isVideo,
                durationMs = entity.durationMs,
                localExists = true,
                resolvedUri = resolveFileUri(uri, entity.filePath, entity.bucketName),
                pHash = entity.pHash,
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    }

    val dateHeaderAlignment: StateFlow<String> = settingsRepository.dateHeaderAlignmentFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "CENTER"
    )

    fun setDateHeaderAlignment(alignment: String) {
        viewModelScope.launch {
            settingsRepository.updateDateHeaderAlignment(alignment)
        }
    }

    private val _currentBucket = MutableStateFlow<String?>(null)
    val currentBucket: StateFlow<String?> = _currentBucket.asStateFlow()

    private val _albumMediaFilter = MutableStateFlow(com.inferno.gallery.data.AlbumMediaFilter.ALL)
    val albumMediaFilter: StateFlow<com.inferno.gallery.data.AlbumMediaFilter> = _albumMediaFilter.asStateFlow()

    fun setAlbumMediaFilter(filter: com.inferno.gallery.data.AlbumMediaFilter) {
        if (_albumMediaFilter.value != filter) {
            _albumMediaFilter.value = filter
        }
    }

    fun setBucket(bucket: String?) {
        if (_currentBucket.value != bucket) {
            _currentBucket.value = bucket
            _albumMediaFilter.value = com.inferno.gallery.data.AlbumMediaFilter.ALL
        }
    }

    val selectedFilterIndex: StateFlow<Int> = settingsRepository.selectedFilterIndexFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun setFilterIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.updateSelectedFilterIndex(index)
        }
    }

    val nativeTimelineRepository = com.inferno.gallery.data.NativeTimelineRepository(
        database = database,
        scope = viewModelScope
    )
    val nativeTimelineItems: StateFlow<List<com.inferno.gallery.data.NativeTimelineItem>> =
        nativeTimelineRepository.timelineItems

    val nativeTimelineSnapshot: StateFlow<com.inferno.gallery.data.NativeTimelineRepository.TimelineSnapshot> =
        nativeTimelineRepository.timelineSnapshot

    fun getCachedTimelineItems(bucket: String?): List<com.inferno.gallery.data.NativeTimelineItem> =
        nativeTimelineRepository.getCachedItems(bucket)

    val isNativeTimelineLoading: StateFlow<Boolean> =
        nativeTimelineRepository.isLoading

    val isNativeTimelineLoaded: StateFlow<Boolean> =
        nativeTimelineRepository.isInitialLoaded

    private val _ftsSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val ftsSearchResults: StateFlow<List<GalleryItem>> = _ftsSearchResults.asStateFlow()

    private val _smartSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val smartSearchResults: StateFlow<List<GalleryItem>> = _smartSearchResults.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.sortOrderFlow.collect { raw ->
                val parsed = try { SortOrder.valueOf(raw) } catch (e: Exception) { SortOrder.NewToOld }
                if (_sortOrder.value != parsed) {
                    _sortOrder.value = parsed
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.albumSortOrderFlow.collect { raw ->
                val parsed = try { SortOrder.valueOf(raw) } catch (e: Exception) { SortOrder.NameAsc }
                if (_albumSortOrder.value != parsed) {
                    _albumSortOrder.value = parsed
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.albumLayoutModeFlow.collect { mode ->
                if (_albumLayoutMode.value != mode) {
                    _albumLayoutMode.value = mode
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.viewModeFlow.collect { raw ->
                val parsed = try { ViewMode.valueOf(raw) } catch (e: Exception) { ViewMode.Grouped }
                if (_viewMode.value != parsed) {
                    _viewMode.value = parsed
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.timelineGroupByFlow.collect { raw ->
                val parsed = try { TimelineGroupBy.valueOf(raw) } catch (e: Exception) { TimelineGroupBy.Day }
                if (_timelineGroupBy.value != parsed) {
                    _timelineGroupBy.value = parsed
                }
            }
        }
        viewModelScope.launch {
            combine(
                combine(_currentBucket, selectedFilterIndex, sortOrder, albumMediaFilter) { b, f, o, af ->
                    Triple(b, f, Pair(o, af))
                },
                combine(viewMode, timelineGroupBy, excludedFolders, favoritesManager.favoritesFlow) { m, g, e, fav ->
                    listOf(m, g, e, fav)
                },
                combine(ftsSearchResults, smartSearchResults) { fts, smart -> Pair(fts, smart) }
            ) { (b, f, orderAndFilter), viewConfig, (fts, smart) ->
                val (o, af) = orderAndFilter
                val m = viewConfig[0] as ViewMode
                val g = viewConfig[1] as TimelineGroupBy
                @Suppress("UNCHECKED_CAST")
                val e = viewConfig[2] as Set<String>
                @Suppress("UNCHECKED_CAST")
                val fav = viewConfig[3] as Set<String>
                nativeTimelineRepository.updatePipeline(
                    bucket = b,
                    filterIndex = f,
                    sortOrder = o,
                    viewMode = m,
                    groupBy = g,
                    excluded = e,
                    favIds = fav,
                    ftsIds = fts.map { it.id },
                    smartIds = smart.map { it.id },
                    albumMediaFilter = af
                )
            }.collect()
        }
    }







    val onboardingV210Completed: StateFlow<Boolean?> = settingsRepository.onboardingV210CompletedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val onboardingCompleted: StateFlow<Boolean?> = settingsRepository.onboardingCompletedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.updateOnboardingV210Completed(true)
        }
    }

    val trashCount: StateFlow<Int> = database.mediaDao().observeTrashCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val videoCount: StateFlow<Int> = database.mediaDao().observeVideoStats()
        .map { it.itemCount }
        .stateIn(
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
        val numericIds = favs.mapNotNull { it.toLongOrNull() }
        if (numericIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            val qc = buildMediaConditions(
                bucket = BucketNames.FAVORITES,
                filterIndex = 0,
                favIds = favs,
                excluded = excluded
            )
            val queryString = "SELECT cm.* FROM core_media cm " +
                buildWhereClause(qc) + " ORDER BY cm.dateAdded DESC"
            val query = androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
            database.mediaDao().observeMediaEntitiesRaw(query)
                .map { entities ->
                    entities.map { entity ->
                        val uri = Uri.parse(entity.uriString)
                        val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
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
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _geotaggedMedia = MutableStateFlow<List<GalleryItem>>(emptyList())
    val geotaggedMedia: StateFlow<List<GalleryItem>> = _geotaggedMedia.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _detailMedia = MutableStateFlow<List<GalleryItem>>(emptyList())
    val detailMedia: StateFlow<List<GalleryItem>> = _detailMedia.asStateFlow()

    private val _initialDetailItem = MutableStateFlow<GalleryItem?>(null)
    val initialDetailItem: StateFlow<GalleryItem?> = _initialDetailItem.asStateFlow()

    private data class DetailQueryKey(
        val bucket: String?,
        val filterIndex: Int,
        val order: SortOrder,
        val albumMediaFilter: com.inferno.gallery.data.AlbumMediaFilter,
        val excluded: Set<String>
    )

    private var currentDetailQueryKey: DetailQueryKey? = null
    private var inFlightDetailQueryKey: DetailQueryKey? = null
    private var inFlightDetailMediaId: String? = null
    private var loadDetailJob: kotlinx.coroutines.Job? = null

    private fun seedDetailFromTimelineCache(
        targetId: String,
        bucketName: String?,
        queryKey: DetailQueryKey
    ): Boolean {
        val snapshot = nativeTimelineRepository.timelineSnapshot.value
        val snapshotExact = snapshot.isLoaded &&
            snapshot.bucket == bucketName &&
            nativeTimelineRepository.currentBucket.value == bucketName &&
            nativeTimelineRepository.filterIndex.value == queryKey.filterIndex &&
            nativeTimelineRepository.sortOrder.value == queryKey.order &&
            nativeTimelineRepository.albumMediaFilter.value == queryKey.albumMediaFilter &&
            nativeTimelineRepository.excludedFolders.value == queryKey.excluded

        val timelineSource = when {
            snapshotExact -> snapshot.items
            else -> nativeTimelineRepository.getCachedItems(bucketName)
        }
        if (timelineSource.isEmpty()) return false

        val seeded = ArrayList<GalleryItem>(timelineSource.size)
        var containsTarget = false
        for (entry in timelineSource) {
            if (entry is com.inferno.gallery.data.NativeTimelineItem.Media) {
                val item = GalleryItem(
                    id = entry.id,
                    uri = entry.uri,
                    bucketName = entry.bucketName,
                    dateAdded = entry.dateAdded,
                    size = entry.size,
                    name = entry.path.substringAfterLast('/').ifEmpty { entry.id },
                    dateModified = entry.dateModified,
                    path = entry.path,
                    isVideo = entry.isVideo,
                    durationMs = entry.durationMs,
                    localExists = true,
                    resolvedUri = entry.uri,
                    uriString = entry.uriString.ifEmpty { entry.uri.toString() }
                )
                if (item.id == targetId || item.uri.toString() == targetId) {
                    containsTarget = true
                }
                seeded.add(item)
            }
        }
        if (!containsTarget || seeded.isEmpty()) return false

        _detailMedia.value = seeded
        if (snapshotExact) {
            currentDetailQueryKey = queryKey
        }
        return true
    }

    fun setInitialDetailItem(item: GalleryItem, bucketName: String? = null) {
        _initialDetailItem.value = item

        val queryKey = DetailQueryKey(
            bucket = bucketName,
            filterIndex = selectedFilterIndex.value,
            order = sortOrder.value,
            albumMediaFilter = albumMediaFilter.value,
            excluded = excludedFolders.value
        )
        val isSameQuery = currentDetailQueryKey == queryKey

        if (isSameQuery && _detailMedia.value.any { it.id == item.id || it.uri.toString() == item.id }) {
            return
        }

        when (bucketName) {
            BucketNames.SEARCH_TEXT -> {
                if (_ftsSearchResults.value.isNotEmpty()) {
                    _detailMedia.value = _ftsSearchResults.value
                    currentDetailQueryKey = queryKey
                    return
                }
            }
            BucketNames.SEARCH_SMART -> {
                if (_smartSearchResults.value.isNotEmpty()) {
                    _detailMedia.value = _smartSearchResults.value
                    currentDetailQueryKey = queryKey
                    return
                }
            }
            BucketNames.FAVORITES -> {
                if (favoriteMedia.value.isNotEmpty()) {
                    _detailMedia.value = favoriteMedia.value
                    currentDetailQueryKey = queryKey
                    return
                }
            }
            "geotagged" -> {
                if (_geotaggedMedia.value.isNotEmpty()) {
                    _detailMedia.value = _geotaggedMedia.value
                    currentDetailQueryKey = queryKey
                    return
                }
            }
        }

        val seededFromTimeline = seedDetailFromTimelineCache(item.id, bucketName, queryKey)
        if (seededFromTimeline && currentDetailQueryKey == queryKey) {
            return
        }
        if (!seededFromTimeline) {
            _detailMedia.value = emptyList()
        }
        loadDetailMedia(item.id, bucketName)
    }

    fun loadDetailMedia(mediaId: String? = null, bucketName: String? = null) {
        val filterIndex = selectedFilterIndex.value
        val order = sortOrder.value
        val albumFilter = albumMediaFilter.value
        val excluded = excludedFolders.value
        val queryKey = DetailQueryKey(
            bucket = bucketName,
            filterIndex = filterIndex,
            order = order,
            albumMediaFilter = albumFilter,
            excluded = excluded
        )
        val isSameQuery = currentDetailQueryKey == queryKey

        if (isSameQuery && _detailMedia.value.isNotEmpty() && (mediaId == null || _detailMedia.value.any { it.id == mediaId || it.uri.toString() == mediaId })) {
            return
        }
        if (loadDetailJob?.isActive == true && inFlightDetailQueryKey == queryKey && (mediaId == null || inFlightDetailMediaId == mediaId)) {
            return
        }

        inFlightDetailQueryKey = queryKey
        inFlightDetailMediaId = mediaId
        loadDetailJob?.cancel()
        loadDetailJob = viewModelScope.launch(Dispatchers.IO) {
            if (bucketName == "External") {
                val parsedUri = runCatching { Uri.parse(mediaId) }.getOrNull()
                if (parsedUri != null && (parsedUri.scheme == "content" || parsedUri.scheme == "file")) {
                    _detailMedia.value = listOf(createExternalGalleryItem(mediaId ?: parsedUri.toString(), parsedUri))
                    currentDetailQueryKey = queryKey
                    return@launch
                }
            }
            if (bucketName == BucketNames.SEARCH_TEXT) {
                _detailMedia.value = _ftsSearchResults.value
                currentDetailQueryKey = queryKey
                return@launch
            }
            if (bucketName == BucketNames.SEARCH_SMART) {
                _detailMedia.value = _smartSearchResults.value
                currentDetailQueryKey = queryKey
                return@launch
            }
            if (bucketName == BucketNames.FAVORITES) {
                _detailMedia.value = favoriteMedia.value
                currentDetailQueryKey = queryKey
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
                    val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
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

            if (bucketName?.startsWith("person:") == true) {
                val personId = bucketName.removePrefix("person:").toLongOrNull()
                if (personId != null) {
                    val entities = database.faceDao().getMediaForPerson(personId)
                    val items = entities.map { entity ->
                        val uri = Uri.parse(entity.uriString)
                        val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
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
                    currentDetailQueryKey = queryKey
                    return@launch
                }
            }

            // Build the SQLite query for the bucket
            val smartIds = smartSearchResults.value.map { it.id }
            val ftsIds = ftsSearchResults.value.map { it.id }
            val qc = buildMediaConditions(
                bucket = bucketName, 
                filterIndex = filterIndex, 
                excluded = excluded,
                favIds = favoriteIds.value,
                ftsIds = ftsIds,
                smartIds = smartIds,
                albumMediaFilter = albumFilter
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
                    resolvedUri = uri
                )
            }

            val finalItems = items

            val containsTarget = if (mediaId != null) finalItems.any { it.id == mediaId || it.uri.toString() == mediaId } else true
            if (finalItems.isNotEmpty() && containsTarget) {
                _detailMedia.value = finalItems
                currentDetailQueryKey = queryKey
            } else if (mediaId != null) {
                // Fetch the clicked item as a fallback
                val idLong = mediaId.toLongOrNull()
                val entity = idLong?.let { database.mediaDao().getMediaById(it) }
                    ?: database.mediaDao().getMediaByUri(mediaId)
                if (entity != null) {
                    val uri = Uri.parse(entity.uriString)
                    val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
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
                    val parsedUri = runCatching { Uri.parse(mediaId) }.getOrNull()
                    if (parsedUri != null && (parsedUri.scheme == "content" || parsedUri.scheme == "file")) {
                        _detailMedia.value = listOf(createExternalGalleryItem(mediaId, parsedUri))
                    } else {
                        _detailMedia.value = emptyList()
                    }
                }
            } else if (finalItems.isNotEmpty()) {
                _detailMedia.value = finalItems
                currentDetailQueryKey = queryKey
            }
        }
    }

    private fun createExternalGalleryItem(mediaId: String, parsedUri: Uri): GalleryItem {
        val app = getApplication<android.app.Application>()
        val isVideo = runCatching {
            val mime = app.contentResolver.getType(parsedUri)
            mime?.startsWith("video/") == true ||
                (parsedUri.lastPathSegment ?: "").lowercase().let { 
                    it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".mov") || it.endsWith(".webm") || it.endsWith(".avi") || it.endsWith(".3gp")
                }
        }.getOrDefault(false)

        val resolvedPath = if (parsedUri.scheme == "file") {
            parsedUri.path ?: ""
        } else {
            runCatching {
                var filePath = ""
                app.contentResolver.query(
                    parsedUri,
                    arrayOf(android.provider.MediaStore.MediaColumns.DATA),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (dataIdx >= 0) {
                            filePath = cursor.getString(dataIdx) ?: ""
                        }
                    }
                }
                if (filePath.isNotEmpty() && java.io.File(filePath).exists()) filePath else ""
            }.getOrDefault("")
        }

        val fileName = runCatching {
            var displayName: String? = null
            if (parsedUri.scheme == "content") {
                app.contentResolver.query(
                    parsedUri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                    }
                }
            }
            displayName ?: parsedUri.lastPathSegment ?: "Media"
        }.getOrDefault(parsedUri.lastPathSegment ?: "Media")

        val fileSize = runCatching {
            var s = 0L
            if (parsedUri.scheme == "content") {
                app.contentResolver.query(
                    parsedUri,
                    arrayOf(android.provider.OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIdx >= 0) s = cursor.getLong(sizeIdx)
                    }
                }
            } else if (parsedUri.scheme == "file") {
                val f = java.io.File(parsedUri.path ?: "")
                if (f.exists()) s = f.length()
            }
            s
        }.getOrDefault(0L)

        return GalleryItem(
            id = mediaId,
            uri = parsedUri,
            bucketName = "External",
            dateAdded = System.currentTimeMillis() / 1000L,
            size = fileSize,
            name = fileName,
            dateModified = System.currentTimeMillis() / 1000L,
            path = resolvedPath,
            isVideo = isVideo,
            durationMs = null,
            localExists = true,
            resolvedUri = parsedUri
        )
    }

    fun recordMediaView(mediaId: String) {
        // No-op for local-first gallery
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
    private fun calculateRotatedOrientation(currentOrient: Int, degrees: Int): Int {
        val normDegrees = ((degrees % 360) + 360) % 360
        return when (normDegrees) {
            90 -> when (currentOrient) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                else -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
            }
            180 -> when (currentOrient) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
                else -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
            }
            270 -> when (currentOrient) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                else -> androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
            }
            else -> currentOrient
        }
    }

    fun rotateMedia(
        context: android.content.Context,
        item: GalleryItem,
        degrees: Int,
        onSecurityException: (android.app.PendingIntent) -> Unit,
        onSuccess: () -> Unit = {}
    ) {
        val normDegrees = ((degrees % 360) + 360) % 360
        if (normDegrees == 0) {
            onSuccess()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            val uri = item.uri
            try {
                val isJpegOrHeic = item.name.endsWith(".jpg", ignoreCase = true) ||
                                  item.name.endsWith(".jpeg", ignoreCase = true) ||
                                  item.name.endsWith(".heic", ignoreCase = true) ||
                                  item.name.endsWith(".webp", ignoreCase = true)

                if (isJpegOrHeic) {
                    val filePath = if (item.path.isNotBlank()) {
                        item.path
                    } else {
                        try {
                            context.contentResolver.query(
                                uri,
                                arrayOf(android.provider.MediaStore.Images.Media.DATA),
                                null, null, null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val idx = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                                    if (idx >= 0) cursor.getString(idx) else null
                                } else null
                            }
                        } catch (_: Exception) { null }
                    }

                    val file = if (!filePath.isNullOrBlank()) File(filePath) else null
                    val hasAllFilesAccess = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()
                    val canWriteDirectly = file != null && file.exists() && (hasAllFilesAccess || file.canWrite())

                    if (canWriteDirectly && file != null) {
                        try {
                            val exif = androidx.exifinterface.media.ExifInterface(file)
                            val currentOrient = exif.getAttributeInt(
                                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                            )
                            val newOrient = calculateRotatedOrientation(currentOrient, normDegrees)
                            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, newOrient.toString())
                            exif.saveAttributes()
                            success = true
                        } catch (_: Exception) {}
                    }

                    if (!success) {
                        try {
                            val pfd = context.contentResolver.openFileDescriptor(uri, "rw")
                            if (pfd != null) {
                                pfd.use { fd ->
                                    val exif = androidx.exifinterface.media.ExifInterface(fd.fileDescriptor)
                                    val currentOrient = exif.getAttributeInt(
                                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                                    )
                                    val newOrient = calculateRotatedOrientation(currentOrient, normDegrees)
                                    exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, newOrient.toString())
                                    exif.saveAttributes()
                                    success = true
                                }
                            }
                        } catch (sec: SecurityException) {
                            throw sec
                        } catch (_: Exception) {
                            // Fall back to bitmap rotation
                        }
                    }
                }

                if (!success) {
                    val exportResult = com.inferno.gallery.ui.editor.engine.ImageExportWorker.exportImage(
                        context = context,
                        sourceUri = uri,
                        cropState = com.inferno.gallery.ui.editor.model.CropState(rotation = normDegrees),
                        tuneValues = com.inferno.gallery.ui.editor.model.TuneValues(),
                        overwrite = true
                    )
                    success = exportResult.isSuccess
                }

                if (success) {
                    val now = System.currentTimeMillis() / 1000
                    val cv = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DATE_MODIFIED, now)
                    }
                    try {
                        context.contentResolver.update(uri, cv, null, null)
                    } catch (_: Exception) {}

                    // Update Room DB dateModified so timeline & paging detect modification
                    database.mediaDao().updateDateModifiedByUri(uri.toString(), now)
                    item.id.toLongOrNull()?.let { idLong ->
                        database.mediaDao().updateDateModifiedById(idLong, now)
                    }

                    // Invalidate grid thumbnail cache entries across Coil and notify MediaScanner
                    com.inferno.gallery.ui.utils.ImageCacheEvictor.evictMedia(context, uri, explicitId = item.id)

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                }
            } catch (securityException: SecurityException) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                    try {
                        val manageIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(manageIntent)
                        return@launch
                    } catch (_: Exception) {}
                }
                val recoverable = securityException as? android.app.RecoverableSecurityException
                if (recoverable != null) {
                    withContext(Dispatchers.Main) {
                        onSecurityException(recoverable.userAction.actionIntent)
                    }
                } else {
                    val pendingIntent = android.provider.MediaStore.createWriteRequest(context.contentResolver, listOf(item.uri))
                    withContext(Dispatchers.Main) {
                        onSecurityException(pendingIntent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error rotating: ${e.message}")
                }
            }
        }
    }


    private val _gridCellsCount = MutableStateFlow(4)
    val gridCellsCount: StateFlow<Int> = _gridCellsCount.asStateFlow()

    private var saveGridCellsJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            _gridCellsCount.value = settingsRepository.gridCellsCountFlow.first().coerceIn(2, 8)
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(application)
                if (searchEngine.isModelDownloaded()) {
                    Log.d("GalleryViewModel", "Pre-warming Smart Search tokenizer...")
                    searchEngine.prewarmTokenizer()
                }
            } catch (e: Exception) {
                Log.w("GalleryViewModel", "Failed to pre-warm Smart Search tokenizer: ${e.message}")
            }
        }
    }

    val thumbnailCornerRadius: StateFlow<Float> = settingsRepository.thumbnailCornerRadiusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )


    fun setThumbnailCornerRadius(radius: Float) {
        viewModelScope.launch {
            settingsRepository.updateThumbnailCornerRadius(radius.coerceIn(0f, 16f))
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
        _isDragSelecting.value = false
        _selectedUris.value = emptySet()
        _draggedUris.value = emptySet()
        baseSelectedUris = emptySet()
    }

    private var baseSelectedUris = emptySet<String>()
    private val _draggedUris = MutableStateFlow<Set<String>>(emptySet())
    private var dragSelecting = true
    private val _isDragSelecting = MutableStateFlow(false)
    val isDragSelecting: StateFlow<Boolean> = _isDragSelecting.asStateFlow()

    fun startDragSelection(initialUri: String, isSelecting: Boolean) {
        _isDragSelecting.value = true
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
        _isDragSelecting.value = false
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

    fun isCoverValid(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = Uri.parse(uriString)
        return try {
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return false
                    File(path).exists()
                }
                "content" -> {
                    getApplication<Application>().contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    val albumCustomCovers: StateFlow<Map<String, String>> = settingsRepository.albumCustomCoversFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setAlbumCustomCover(bucketName: String, uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val finalUriString = if (uri.scheme == "file") {
                    uriString
                } else {
                    val coversDir = File(getApplication<Application>().filesDir, "album_covers").apply { mkdirs() }
                    val safeBucket = bucketName.filter { it.isLetterOrDigit() }.take(20)
                    val targetFile = File(coversDir, "cover_${safeBucket}_${bucketName.hashCode()}.jpg")

                    val copied = getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        targetFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream) > 0
                        }
                    } ?: false

                    if (copied && targetFile.exists() && targetFile.length() > 0) {
                        Uri.fromFile(targetFile).toString()
                    } else {
                        uriString
                    }
                }
                settingsRepository.setAlbumCustomCover(bucketName, finalUriString)
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to save custom album cover for $bucketName", e)
                settingsRepository.setAlbumCustomCover(bucketName, uriString)
            }
        }
    }

    fun removeAlbumCustomCover(bucketName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val coversDir = File(getApplication<Application>().filesDir, "album_covers")
                val safeBucket = bucketName.filter { it.isLetterOrDigit() }.take(20)
                val targetFile = File(coversDir, "cover_${safeBucket}_${bucketName.hashCode()}.jpg")
                if (targetFile.exists()) {
                    targetFile.delete()
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Error deleting album cover file", e)
            }
            settingsRepository.removeAlbumCustomCover(bucketName)
        }
    }

    val albumCustomShapes: StateFlow<Map<String, String>> = settingsRepository.albumCustomShapesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setAlbumCustomShape(bucketName: String, shapeId: String) {
        viewModelScope.launch {
            settingsRepository.setAlbumCustomShape(bucketName, shapeId)
        }
    }

    fun removeAlbumCustomShape(bucketName: String) {
        viewModelScope.launch {
            settingsRepository.removeAlbumCustomShape(bucketName)
        }
    }

    val showEmptyAlbums: StateFlow<Boolean> = settingsRepository.showEmptyAlbumsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _emptyAlbumsRefreshTrigger = MutableStateFlow(0L)
    fun refreshEmptyAlbums() {
        _emptyAlbumsRefreshTrigger.value = System.currentTimeMillis()
    }

    private fun scanEmptyAlbumDirectories(
        showHidden: Boolean,
        existingBucketNames: Set<String>
    ): List<File> {
        val results = mutableListOf<File>()
        val seenNames = existingBucketNames.map { it.lowercase() }.toMutableSet()

        val mediaRoots = listOfNotNull(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
        ).filter { it.exists() && it.isDirectory }

        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "dng",
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v"
        )

        fun hasMediaFiles(dir: File): Boolean {
            val files = dir.listFiles() ?: return false
            return files.any { f ->
                f.isFile && mediaExtensions.contains(f.extension.lowercase())
            }
        }

        val ignoredDirNames = setOf(
            "thumbnails", ".thumbnails", "cache", ".cache", "android", "lost.dir",
            "system volume information", ".gallery"
        )

        for (root in mediaRoots) {
            val subDirs = root.listFiles { file ->
                file.isDirectory &&
                (showHidden || (!file.name.startsWith(".") && !File(file, ".nomedia").exists())) &&
                !ignoredDirNames.contains(file.name.lowercase())
            } ?: continue

            for (subDir in subDirs) {
                val nameLower = subDir.name.lowercase()
                if (!seenNames.contains(nameLower)) {
                    if (!hasMediaFiles(subDir)) {
                        results.add(subDir)
                        seenNames.add(nameLower)
                    }
                }

                // Also scan 1 level deeper in Pictures and DCIM
                val nestedDirs = subDir.listFiles { file ->
                    file.isDirectory &&
                    (showHidden || (!file.name.startsWith(".") && !File(file, ".nomedia").exists())) &&
                    !ignoredDirNames.contains(file.name.lowercase())
                } ?: continue

                for (nested in nestedDirs) {
                    val nestedLower = nested.name.lowercase()
                    if (!seenNames.contains(nestedLower)) {
                        if (!hasMediaFiles(nested)) {
                            results.add(nested)
                            seenNames.add(nestedLower)
                        }
                    }
                }
            }
        }

        return results
    }

    val allAlbums: StateFlow<List<AlbumBucket>> = combine(
        database.mediaDao().observeBuckets(),
        albumSortOrder,
        excludedFolders,
        settingsRepository.showHiddenAlbumsFlow,
        combine(
            settingsRepository.showEmptyAlbumsFlow,
            settingsRepository.albumCustomCoversFlow,
            _emptyAlbumsRefreshTrigger
        ) { showEmpty, customCovers, _ ->
            Pair(showEmpty, customCovers)
        }
    ) { buckets, order, excluded, showHidden, (showEmpty, customCovers) ->
        val excludedKeywords = setOf(BucketNames.TRASH, BucketNames.ALL, BucketNames.FAVORITES)

        val existingBuckets = buckets.filter { bucket ->
            !excludedKeywords.contains(bucket.bucketName) &&
            !excluded.contains(bucket.bucketName) &&
            (showHidden || !bucket.bucketName.startsWith("."))
        }.map { b ->
            val defaultUri = if (b.coverUriString != null) Uri.parse(b.coverUriString) else Uri.EMPTY
            val customUriString = customCovers[b.bucketName]
            val customUri = if (customUriString != null && isCoverValid(customUriString)) {
                Uri.parse(customUriString)
            } else {
                if (customUriString != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        settingsRepository.removeAlbumCustomCover(b.bucketName)
                    }
                }
                null
            }
            AlbumBucket(
                bucketName = b.bucketName,
                coverUri = customUri ?: defaultUri,
                itemCount = b.itemCount,
                totalSizeBytes = b.totalSizeBytes,
                maxDate = b.maxDate,
                defaultCoverUri = defaultUri
            )
        }.toMutableList()

        if (showEmpty) {
            val existingNames = buckets.map { it.bucketName }.toSet()
            val emptyDirs = scanEmptyAlbumDirectories(showHidden, existingNames)
            for (dir in emptyDirs) {
                if (!excludedKeywords.contains(dir.name) &&
                    !excluded.contains(dir.name) &&
                    (showHidden || !dir.name.startsWith("."))
                ) {
                    val customUriString = customCovers[dir.name]
                    val customUri = if (customUriString != null && isCoverValid(customUriString)) {
                        Uri.parse(customUriString)
                    } else null

                    existingBuckets.add(
                        AlbumBucket(
                            bucketName = dir.name,
                            coverUri = customUri ?: Uri.EMPTY,
                            itemCount = 0,
                            totalSizeBytes = 0L,
                            maxDate = dir.lastModified() / 1000L,
                            defaultCoverUri = Uri.EMPTY
                        )
                    )
                }
            }
        }

        val sortedBuckets = when (order) {
            SortOrder.NewToOld -> existingBuckets.sortedByDescending { it.maxDate }
            SortOrder.OldToNew -> existingBuckets.sortedBy { it.maxDate }
            SortOrder.DateModifiedNewToOld -> existingBuckets.sortedByDescending { it.maxDate }
            SortOrder.DateModifiedOldToNew -> existingBuckets.sortedBy { it.maxDate }
            SortOrder.SmallToBig -> existingBuckets.sortedBy { it.totalSizeBytes }
            SortOrder.BigToSmall -> existingBuckets.sortedByDescending { it.totalSizeBytes }
            SortOrder.NameAsc -> existingBuckets.sortedBy { it.bucketName.lowercase() }
            SortOrder.NameDesc -> existingBuckets.sortedByDescending { it.bucketName.lowercase() }
            SortOrder.CountDesc -> existingBuckets.sortedByDescending { it.itemCount }
            SortOrder.CountAsc -> existingBuckets.sortedBy { it.itemCount }
        }

        sortedBuckets
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // All bucket names for the Settings "Excluded Folders" UI (includes all folders)
    val allBucketNames: StateFlow<List<String>> = combine(
        database.mediaDao().observeBuckets(),
        settingsRepository.showHiddenAlbumsFlow,
        settingsRepository.showEmptyAlbumsFlow,
        _emptyAlbumsRefreshTrigger
    ) { buckets, showHidden, showEmpty, _ ->
        val bucketNames = buckets.map { it.bucketName }
            .filter { it != "Trash" && (showHidden || !it.startsWith(".")) }
            .toMutableSet()

        if (showEmpty) {
            val emptyDirs = scanEmptyAlbumDirectories(showHidden, bucketNames)
            emptyDirs.forEach { bucketNames.add(it.name) }
        }

        bucketNames.distinct().sorted()
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
                    resolvedUri = resolveFileUri(uri, entity.filePath, entity.bucketName),
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

    private val cleanupAnalysis: StateFlow<Pair<List<SimilarPhotoGroup>, List<BlurryPhotoItem>>> = combine(
        database.mediaDao().observeImagesWithPHash(),
        excludedFolders,
        _qualityCacheTick
    ) { entities, excluded, _ ->
        val app = getApplication<Application>()
        val qualityCache = com.inferno.gallery.data.PhotoQualityCache.getInstance(app)
        qualityCache.ensureLoaded()
        val qualitySnapshot = qualityCache.snapshot()

        val filtered = entities.filter {
            !excluded.contains(it.bucketName) && it.bucketName != "Trash" && it.pHash != null
        }
        if (filtered.isEmpty()) return@combine Pair(emptyList(), emptyList())

        val entityById = HashMap<Long, com.inferno.gallery.data.db.CoreMediaEntity>(filtered.size)
        val candidates = ArrayList<com.inferno.gallery.data.SimilarCandidate>(filtered.size)
        for (entity in filtered) {
            entityById[entity.id] = entity
            val metrics = qualitySnapshot[entity.id]?.takeIf { it.dateModified == entity.dateModified }
            candidates.add(
                com.inferno.gallery.data.SimilarCandidate(
                    id = entity.id,
                    pHash = entity.pHash!!,
                    dateAddedSec = entity.dateAdded,
                    sizeBytes = entity.size,
                    fileHash = entity.fileHash,
                    sharpness = metrics?.sharpness ?: 0f,
                    pixelCount = metrics?.pixelCount ?: 0L,
                    isScreenshotOrDownload = com.inferno.gallery.data.SimilarPhotoAnalyzer.isScreenshotOrDownload(
                        entity.bucketName,
                        entity.name,
                        entity.filePath
                    )
                )
            )
        }

        fun toGalleryItem(entity: com.inferno.gallery.data.db.CoreMediaEntity): GalleryItem {
            val uri = Uri.parse(entity.uriString)
            return GalleryItem(
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
                resolvedUri = resolveFileUri(uri, entity.filePath, entity.bucketName),
                pHash = entity.pHash,
                latitude = entity.latitude,
                longitude = entity.longitude,
                fileHash = entity.fileHash
            )
        }

        val clusters = com.inferno.gallery.data.SimilarPhotoClusterer.cluster(candidates)
        val similarGroups = clusters.mapNotNull { cluster ->
            val items = cluster.memberIds.mapNotNull { id ->
                val entity = entityById[id] ?: return@mapNotNull null
                val metrics = qualitySnapshot[id]?.takeIf { it.dateModified == entity.dateModified }
                SimilarPhotoItem(
                    item = toGalleryItem(entity),
                    sharpness = metrics?.sharpness ?: 0f,
                    pixelCount = metrics?.pixelCount ?: 0L,
                    isBestShot = id == cluster.bestShotId
                )
            }
            if (items.size < 2) null
            else SimilarPhotoGroup(
                id = cluster.id,
                items = items,
                isBurst = cluster.isBurst,
                similarityPct = cluster.averageSimilarityPct
            )
        }

        val blurryIds = com.inferno.gallery.data.SimilarPhotoClusterer.detectBlurry(candidates)
        val blurryItems = blurryIds.mapNotNull { id ->
            val entity = entityById[id] ?: return@mapNotNull null
            val metrics = qualitySnapshot[id]?.takeIf { it.dateModified == entity.dateModified } ?: return@mapNotNull null
            BlurryPhotoItem(
                item = toGalleryItem(entity),
                sharpness = metrics.sharpness,
                pixelCount = metrics.pixelCount
            )
        }

        Pair(similarGroups, blurryItems)
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(emptyList(), emptyList()))

    val similarPhotoGroups: StateFlow<List<SimilarPhotoGroup>> = cleanupAnalysis
        .map { it.first }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blurryPhotos: StateFlow<List<BlurryPhotoItem>> = cleanupAnalysis
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCleanupReclaimableBytes: StateFlow<Long> = combine(
        duplicates,
        similarPhotoGroups,
        blurryPhotos
    ) { exact, similar, blurry ->
        val seenIds = HashSet<String>()
        var bytes = 0L
        exact.forEach { group ->
            group.items.drop(1).forEach { item ->
                if (seenIds.add(item.id)) bytes += item.size
            }
        }
        similar.forEach { group ->
            group.items.filterNot { it.isBestShot }.forEach { entry ->
                if (seenIds.add(entry.item.id)) bytes += entry.item.size
            }
        }
        blurry.forEach { entry ->
            if (seenIds.add(entry.item.id)) bytes += entry.item.size
        }
        bytes
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun scanForDuplicates(scanLimit: Int = 500) {
        if (_duplicateScanState.value is DuplicateScanState.Scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()

                // Phase 1: Exact duplicates (same byte size -> SHA-256)
                val candidates = database.mediaDao().getMediaSharingSameSize()
                val needsHash = candidates.filter { it.fileHash == null }

                // Phase 2: Similar / Burst & Blurry photo perceptual hash + sharpness scan
                val needsPHash = database.mediaDao().getImagesNeedingPHash(limit = scanLimit)
                val totalWork = needsHash.size + needsPHash.size

                if (totalWork == 0) {
                    _qualityCacheTick.value++
                    _duplicateScanState.value = DuplicateScanState.Done
                    return@launch
                }

                _duplicateScanState.value = DuplicateScanState.Scanning(0, totalWork, "Checking exact copies…")

                val updates = mutableListOf<com.inferno.gallery.data.db.CoreMediaEntity>()
                val saveBatchSize = 100
                var processed = 0

                for (entity in needsHash) {
                    val uri = Uri.parse(entity.uriString)
                    val hash = com.inferno.gallery.utils.HashUtils.computeMediaHash(
                        app.contentResolver,
                        uri,
                        entity.filePath
                    )
                    if (hash != null && hash != entity.fileHash) {
                        updates.add(entity.copy(fileHash = hash))
                    }

                    if (updates.size >= saveBatchSize) {
                        database.mediaDao().insertAll(updates)
                        updates.clear()
                    }

                    processed++
                    if (processed % 5 == 0 || processed == needsHash.size) {
                        _duplicateScanState.value = DuplicateScanState.Scanning(processed, totalWork, "Checking exact copies…")
                    }
                }

                if (updates.isNotEmpty()) {
                    database.mediaDao().insertAll(updates)
                    updates.clear()
                }

                if (needsPHash.isNotEmpty()) {
                    val qualityCache = com.inferno.gallery.data.PhotoQualityCache.getInstance(app)
                    qualityCache.ensureLoaded()
                    val pHashBatch = ArrayList<Pair<Long, Long>>(40)

                    for (entity in needsPHash) {
                        val sig = com.inferno.gallery.data.SimilarPhotoAnalyzer.analyze(
                            contentResolver = app.contentResolver,
                            uri = Uri.parse(entity.uriString),
                            filePath = entity.filePath
                        )
                        if (sig != null) {
                            pHashBatch.add(entity.id to sig.pHash)
                            qualityCache.put(
                                entity.id,
                                com.inferno.gallery.data.PhotoQualityMetrics(
                                    dateModified = entity.dateModified,
                                    sharpness = sig.sharpness,
                                    pixelCount = sig.pixelCount
                                )
                            )
                        }
                        if (pHashBatch.size >= 40) {
                            database.mediaDao().updatePHashBatch(pHashBatch)
                            pHashBatch.clear()
                            _qualityCacheTick.value++
                        }
                        processed++
                        if (processed % 8 == 0 || processed == totalWork) {
                            _duplicateScanState.value = DuplicateScanState.Scanning(processed, totalWork, "Analyzing similar & blurry shots…")
                        }
                    }

                    if (pHashBatch.isNotEmpty()) {
                        database.mediaDao().updatePHashBatch(pHashBatch)
                        pHashBatch.clear()
                    }
                    qualityCache.persist()
                    _qualityCacheTick.value++
                }

                _duplicateScanState.value = DuplicateScanState.Done
            } catch (e: Exception) {
                e.printStackTrace()
                _duplicateScanState.value = DuplicateScanState.Done
            }
        }
    }

    fun scanPhotoSpheres() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val candidates = database.mediaDao().getUncheckedSphereCandidates()
                if (candidates.isEmpty()) return@launch
                val app = getApplication<Application>()
                val updates = candidates.map { candidate ->
                    val isSphere = com.inferno.gallery.data.PhotoSphereHelper.isPhotoSphere(
                        context = app,
                        filePath = candidate.filePath,
                        uriString = candidate.uriString
                    )
                    com.inferno.gallery.data.db.PhotoSphereEntity(mediaId = candidate.id, isSphere = isSphere)
                }
                database.mediaDao().insertPhotoSpheres(updates)
                Log.d("GalleryViewModel", "Classified ${updates.size} photo sphere candidates")
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to scan photo spheres: ${e.message}")
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

                // Load geotagged results & trigger reverse geocoding to populate Places Carousel
                loadGeotaggedMedia()
                val geocodeRequest = androidx.work.OneTimeWorkRequestBuilder<ReverseGeocodeWorker>().build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "ReverseGeocodeWorker",
                    ExistingWorkPolicy.REPLACE,
                    geocodeRequest
                )
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
                            resolvedUri = resolveFileUri(uri, entity.filePath, entity.bucketName),
                            latitude = entity.latitude,
                            longitude = entity.longitude
                        )
                    }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "Failed to load geotagged media: ${e.message}")
            }
        }
    }

    val animationStats: StateFlow<com.inferno.gallery.data.db.MediaAggregateStats> = database.mediaDao().observeAnimationStats()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.inferno.gallery.data.db.MediaAggregateStats(0, 0L, 0L, null))

    val mediaTypeBuckets: StateFlow<List<AlbumBucket>> = combine(
        listOf(
            database.mediaDao().observeRawStats(),
            database.mediaDao().observePanoramaStats(),
            database.mediaDao().observeSlowMoStats(),
            database.mediaDao().observeAnimationStats(),
            database.mediaDao().observeNightStats(),
            database.mediaDao().observePhotoSphereStats(),
            database.mediaDao().observePortraitStats(),
            database.mediaDao().observeMacroStats(),
            database.mediaDao().observeTimelapseStats()
        )
    ) { statsArray ->
        val list = mutableListOf<AlbumBucket>()
        val types = listOf(
            BucketNames.MEDIA_TYPE_RAW,
            BucketNames.MEDIA_TYPE_PANORAMAS,
            BucketNames.MEDIA_TYPE_SLOW_MO,
            BucketNames.MEDIA_TYPE_ANIMATIONS,
            BucketNames.MEDIA_TYPE_NIGHT,
            BucketNames.MEDIA_TYPE_PHOTO_SPHERE,
            BucketNames.MEDIA_TYPE_PORTRAIT,
            BucketNames.MEDIA_TYPE_MACRO,
            BucketNames.MEDIA_TYPE_TIMELAPSE
        )
        types.forEachIndexed { index, bucketName ->
            val stats = statsArray[index]
            if (stats.itemCount > 0) {
                list.add(
                    AlbumBucket(
                        bucketName = bucketName,
                        coverUri = stats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                        itemCount = stats.itemCount,
                        totalSizeBytes = stats.totalSizeBytes,
                        maxDate = stats.maxDate
                    )
                )
            }
        }
        list
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val documentBuckets: StateFlow<List<AlbumBucket>> = combine(
        listOf(
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_BOOKS_MAGAZINES),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_EVENTS),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_IDS),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_NOTES),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_PAYMENTS),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_RECEIPTS),
            database.mediaDao().observeDocumentCategoryStats(BucketNames.DOC_SOCIAL)
        )
    ) { statsArray ->
        val types = listOf(
            BucketNames.DOC_BOOKS_MAGAZINES,
            BucketNames.DOC_EVENTS,
            BucketNames.DOC_IDS,
            BucketNames.DOC_NOTES,
            BucketNames.DOC_PAYMENTS,
            BucketNames.DOC_RECEIPTS,
            BucketNames.DOC_SOCIAL
        )
        val list = mutableListOf<AlbumBucket>()
        types.forEachIndexed { index, bucketName ->
            val stats = statsArray[index]
            if (stats.itemCount > 0) {
                list.add(
                    AlbumBucket(
                        bucketName = bucketName,
                        coverUri = stats.coverUriString?.let { Uri.parse(it) } ?: Uri.EMPTY,
                        itemCount = stats.itemCount,
                        totalSizeBytes = stats.totalSizeBytes,
                        maxDate = stats.maxDate
                    )
                )
            }
        }
        list
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // User-pinned folders (from Settings, not the hardcoded system buckets)
    val userPinnedFolderNames: StateFlow<Set<String>> = settingsRepository.pinnedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val pinnedFoldersOrder: StateFlow<List<String>> = settingsRepository.pinnedFoldersOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val userPinnedAlbums: StateFlow<List<AlbumBucket>> = combine(
        allAlbums,
        settingsRepository.pinnedFoldersFlow,
        settingsRepository.pinnedFoldersOrderFlow
    ) { albums, pinnedNames, order ->
        val filtered = albums.filter { it.bucketName in pinnedNames }
        if (order.isNotEmpty()) {
            filtered.sortedBy { bucket ->
                val idx = order.indexOf(bucket.bucketName)
                if (idx >= 0) idx else Int.MAX_VALUE
            }
        } else {
            filtered
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun togglePinAlbum(bucketName: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = settingsRepository.togglePinnedFolder(bucketName)
            if (onResult != null) {
                withContext(Dispatchers.Main) {
                    onResult(success)
                }
            }
        }
    }

    fun updatePinnedFoldersOrder(newOrder: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.savePinnedFoldersOrder(newOrder)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilters = MutableStateFlow(com.inferno.gallery.data.SearchFilterState())
    val searchFilters: StateFlow<com.inferno.gallery.data.SearchFilterState> = _searchFilters.asStateFlow()

    /** All non-hidden recognized people available for composable search filtering (#53). */
    val availableSearchPeople: StateFlow<List<PersonEntity>> = database.faceDao()
        .observeAllVisiblePersons()
        .map { list ->
            list.filter { !it.isHidden }
                .sortedWith(
                    compareByDescending<PersonEntity> { !it.name.isNullOrBlank() }
                        .thenByDescending { it.faceCount }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Distinct geocoded locations sorted by photo count for composable search filtering (#53). */
    val availableSearchLocations: StateFlow<List<String>> = placeNameByMediaId
        .map { map ->
            map.values
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Albums whose name matches the current search query. */
    val matchingAlbums: StateFlow<List<AlbumBucket>> = combine(
        searchQuery,
        allAlbums
    ) { query, albums ->
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            emptyList()
        } else {
            albums.filter { it.bucketName.contains(trimmed, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * People whose name matches the current search query, or top visible people when query is
     * empty. Ordered by recent search activity — people you searched for most recently come
     * first; everyone else keeps the default order (unlike the Albums people carousel).
     */
    val matchingPeople: StateFlow<List<PersonEntity>> = combine(
        searchQuery,
        database.faceDao().observeAllVisiblePersons(),
        settingsRepository.recentSearchesFlow
    ) { query, allPersons, recentSearches ->
        // recentSearches is newest-first, capped at 5
        val recencyRank: (PersonEntity) -> Int = { person ->
            val name = person.name
            if (name.isNullOrBlank()) {
                Int.MAX_VALUE
            } else {
                val index = recentSearches.indexOfFirst { recent ->
                    recent.equals(name, ignoreCase = true) ||
                            recent.contains(name, ignoreCase = true) ||
                            (recent.length >= 3 && name.contains(recent, ignoreCase = true))
                }
                if (index < 0) Int.MAX_VALUE else index
            }
        }
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            allPersons.filter { it.isPromoted && !it.isHidden }
                .sortedBy(recencyRank)
                .take(10)
        } else {
            allPersons.filter { person ->
                !person.isHidden && person.name?.contains(trimmed, ignoreCase = true) == true
            }.sortedBy(recencyRank)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateSearchFilters(transform: (com.inferno.gallery.data.SearchFilterState) -> com.inferno.gallery.data.SearchFilterState) {
        val next = transform(_searchFilters.value)
        if (next == _searchFilters.value) return
        _searchFilters.value = next
        val currentQuery = _searchQuery.value
        searchJob?.cancel()
        if (currentQuery.isBlank() && !next.isActive) {
            _ftsSearchResults.value = emptyList()
            _smartSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            performUnifiedSearch(currentQuery, next)
        }
    }

    fun clearSearchFilters() {
        updateSearchFilters { it.clearAll() }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val filters = _searchFilters.value
        if (query.isBlank() && !filters.isActive) {
            _ftsSearchResults.value = emptyList()
            _smartSearchResults.value = emptyList()
            _isSearching.value = false
            searchJob?.cancel()
            return
        }
        // Debounce: wait 300ms after user stops typing, then fire searches without polluting history.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(if (query.isBlank()) 0L else 300L)
            performUnifiedSearch(query, filters)
        }
    }

    /** Submits a search immediately (IME action / suggestion tap) and records recent history. */
    fun submitSearch(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        val filters = _searchFilters.value
        searchJob?.cancel()
        if (trimmed.isBlank() && !filters.isActive) {
            _ftsSearchResults.value = emptyList()
            _smartSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        if (trimmed.isNotBlank()) addRecentSearch(trimmed)
        searchJob = viewModelScope.launch {
            performUnifiedSearch(trimmed, filters)
        }
    }

    fun startSmartSearchIndexing() {
        val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.SmartSearchIndexWorker>()
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SmartSearchIndexWorker",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private val modelDownloadWorkFlow = WorkManager.getInstance(getApplication())
        .getWorkInfosForUniqueWorkFlow("ModelDownloadWorker")
        .map { it.firstOrNull() }

    val smartSearchStatus: StateFlow<SmartSearchStatus> = combine(
        com.inferno.gallery.data.ai.AiIndexingSupport.observeEligibleTotalCount(
            db = database,
            feature = com.inferno.gallery.data.ai.AiIndexingFeature.SMART_SEARCH,
            configFlow = settingsRepository.aiIndexingFolderConfigFlow,
            galleryExcludedFlow = excludedFolders
        ),
        com.inferno.gallery.data.ai.AiIndexingSupport.observeEligibleUnindexedCount(
            db = database,
            feature = com.inferno.gallery.data.ai.AiIndexingFeature.SMART_SEARCH,
            configFlow = settingsRepository.aiIndexingFolderConfigFlow,
            galleryExcludedFlow = excludedFolders
        ),
        com.inferno.gallery.data.IndexingProgressManager.clipProgress,
        modelDownloadWorkFlow
    ) { totalCount, unindexedCount, clipProgress, downloadWorkInfo ->
        val modelReady = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication()).isModelDownloaded()
        val isDownloading = !modelReady && (
            downloadWorkInfo?.state == androidx.work.WorkInfo.State.RUNNING ||
                downloadWorkInfo?.state == androidx.work.WorkInfo.State.ENQUEUED
            )
        val downloadProgress = downloadWorkInfo?.progress?.getInt("progress", 0) ?: 0
        val indexed = (totalCount - unindexedCount).coerceAtLeast(0)
        SmartSearchStatus(
            modelDownloaded = modelReady,
            isIndexing = clipProgress.isIndexing,
            indexedCount = indexed,
            totalCount = totalCount,
            isModelDownloading = isDownloading,
            modelDownloadProgress = downloadProgress
        )
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmartSearchStatus(false, false, 0, 0))

    /** Whether the smart search ONNX model is downloaded and ready for inference. */
    val isSmartSearchReady: StateFlow<Boolean> = smartSearchStatus
        .map { it.modelDownloaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val searchMatchMap = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String?>>()

    private suspend fun executeLocalMetadataFilter(
        filters: com.inferno.gallery.data.SearchFilterState,
        excluded: Set<String>,
        candidateIds: Collection<Long>? = null
    ): List<GalleryItem> = withContext(Dispatchers.IO) {
        val favLongIds = favoriteIds.value.mapNotNull { it.toLongOrNull() }.toSet()
        val queryLimit = if (filters.mediaType == com.inferno.gallery.data.SearchMediaTypeFilter.MOTION_PHOTOS) 1500 else 600
        val built = com.inferno.gallery.data.SearchFilterQueryBuilder.buildFilteredMediaQuery(
            filters = filters,
            excludedBuckets = excluded,
            favoriteIds = favLongIds,
            candidateIds = candidateIds,
            limit = queryLimit
        )
        val entities = database.mediaDao().getMediaEntitiesRaw(
            androidx.sqlite.db.SimpleSQLiteQuery(built.sql, built.args.toTypedArray())
        )
        val app = getApplication<Application>()
        val results = entities.mapNotNull { entity ->
            val uri = Uri.parse(entity.uriString)
            val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
            val item = GalleryItem(
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
                matchType = null
            )
            if (filters.mediaType == com.inferno.gallery.data.SearchMediaTypeFilter.MOTION_PHOTOS) {
                if (com.inferno.gallery.data.MotionPhotoHelper.isMotionPhotoFast(app, item)) item else null
            } else {
                item
            }
        }
        if (filters.mediaType == com.inferno.gallery.data.SearchMediaTypeFilter.MOTION_PHOTOS) {
            results.sortedByDescending { it.dateAdded }
        } else {
            results
        }
    }

    private suspend fun performUnifiedSearch(
        query: String,
        filters: com.inferno.gallery.data.SearchFilterState = _searchFilters.value
    ) {
        _isSearching.value = true
        val currentExcluded = excludedFolders.value

        // Pure metadata filter mode (no text query): execute single 100% local SQL query
        if (query.isBlank() && filters.isActive) {
            try {
                val filteredItems = executeLocalMetadataFilter(filters, currentExcluded)
                if (query == _searchQuery.value && filters == _searchFilters.value) {
                    searchMatchMap.clear()
                    _ftsSearchResults.value = filteredItems
                    _smartSearchResults.value = emptyList()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("GalleryViewModel", "Metadata filter search failed: ${e.message}")
            } finally {
                if (query == _searchQuery.value && filters == _searchFilters.value) {
                    _isSearching.value = false
                }
            }
            return
        }

        val aiConfig = settingsRepository.aiIndexingFolderConfigFlow.first()
        val ocrBucketFilter = com.inferno.gallery.data.ai.AiIndexingQueryBuilder.buildBucketInclusionCondition(
            alias = "cm",
            rule = aiConfig.ruleFor(com.inferno.gallery.data.ai.AiIndexingFeature.OCR_TEXT),
            galleryExcluded = currentExcluded,
            respectGalleryExcluded = aiConfig.respectGalleryExcluded
        )
        try {
            kotlinx.coroutines.coroutineScope {
                    val ftsDeferred = async(Dispatchers.IO) {
                        try {
                            val ftsEntities = if (query.isNotBlank()) {
                                DatabaseProvider.searchMedia(
                                    db = database,
                                    query = query,
                                    excluded = currentExcluded,
                                    ocrBucketFilter = ocrBucketFilter,
                                    isOcrBucketEligible = { bucket ->
                                        aiConfig.isBucketEligible(
                                            com.inferno.gallery.data.ai.AiIndexingFeature.OCR_TEXT,
                                            bucket,
                                            currentExcluded
                                        )
                                    }
                                )
                            } else {
                                emptyList()
                            }
                            ftsEntities.map { searchRes ->
                                val entity = searchRes.entity
                                val uri = Uri.parse(entity.uriString)
                                val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
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
                                    matchType = searchRes.matchType,
                                    matchSnippet = searchRes.matchSnippet
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
                            val cleanQuery = query.trim()
                            val searchEngine = com.inferno.gallery.data.ai.SmartSearchEngine.getInstance(getApplication())
                            if (cleanQuery.length >= 2 && searchEngine.isModelDownloaded()) {
                                searchEngine.loadTextModel()
                                val queryVector = searchEngine.encodeText(cleanQuery)
                                val allEmbeddings = database.embeddingDao().getAllEmbeddings()
                                if (allEmbeddings.isEmpty()) return@async emptyList()

                                val scored = allEmbeddings.map { record ->
                                    record.mediaId to searchEngine.dotProduct(queryVector, record.embedding)
                                }
                                val scoringResult = com.inferno.gallery.data.ai.SemanticSearchScorer.filterMatches(
                                    scored = scored,
                                    query = cleanQuery
                                )
                                android.util.Log.i(
                                    "GalleryViewModel",
                                    "Smart search '$cleanQuery': total=${allEmbeddings.size}, top=${scoringResult.topScore}, median=${scoringResult.baselineMedian}, threshold=${scoringResult.threshold}, matches=${scoringResult.matches.size}"
                                )

                                val matched = scoringResult.matches
                                if (matched.isNotEmpty()) {
                                    val matchMap = matched.associateBy { it.mediaId }
                                    val matchedIds = matched.map { it.mediaId }
                                    withContext(Dispatchers.IO) {
                                        val entitiesMap = database.mediaDao().getMediaByIdsList(matchedIds).associateBy { it.id }
                                        val orderedEntities = matchedIds.mapNotNull { entitiesMap[it] }
                                            .filter {
                                                !currentExcluded.contains(it.bucketName) &&
                                                    aiConfig.isBucketEligible(
                                                        com.inferno.gallery.data.ai.AiIndexingFeature.SMART_SEARCH,
                                                        it.bucketName,
                                                        currentExcluded
                                                    )
                                            }

                                        orderedEntities.map { entity ->
                                            val uri = Uri.parse(entity.uriString)
                                            val (exists, resolved) = resolveItemFields(uri, entity.filePath, entity.bucketName)
                                            val scoredMatch = matchMap[entity.id]
                                            val snippet = scoredMatch?.let { "${it.confidencePercent}% match" }
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
                                                searchScore = scoredMatch?.score,
                                                localExists = exists,
                                                resolvedUri = resolved,
                                                matchType = "SEMANTIC",
                                                matchSnippet = snippet
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

                    val ftsResults = ftsDeferred.await().distinctBy { it.id }.toMutableList()
                    val smartResults = smartDeferred.await().distinctBy { it.id }
                    val ftsIds = ftsResults.map { it.id }.toSet()
                    val unclassifiedSmart = smartResults.filterNot { it.id in ftsIds }
                    val smartScoreMap = smartResults.associate { it.id to (it.searchScore ?: 0f) }

                    val cleanQ = query.trim()
                    val queryTokens = cleanQ.split(Regex("[\\s_\\-\\.]+")).filter { it.isNotBlank() }
                    val filteredSmartResults = mutableListOf<GalleryItem>()

                    if (unclassifiedSmart.isNotEmpty() && cleanQ.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            val rawDb = database.openHelper.readableDatabase
                            val chunks = unclassifiedSmart.chunked(100)
                            for (chunk in chunks) {
                                val placeholders = chunk.joinToString(",") { "?" }
                                val checkSql = "SELECT mediaId, extractedText FROM image_fts WHERE mediaId IN ($placeholders)"
                                val ocrMap = mutableMapOf<String, String>()
                                try {
                                    rawDb.query(checkSql, chunk.map { it.id }.toTypedArray()).use { c ->
                                        val mIdCol = c.getColumnIndex("mediaId")
                                        val textCol = c.getColumnIndex("extractedText")
                                        while (c.moveToNext()) {
                                            val mId = if (mIdCol >= 0) c.getString(mIdCol) else null
                                            val text = if (textCol >= 0 && !c.isNull(textCol)) c.getString(textCol) else null
                                            if (mId != null && !text.isNullOrBlank()) {
                                                ocrMap[mId] = text
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("GalleryViewModel", "Failed to check smart OCR text: ${e.message}", e)
                                }

                                for (item in chunk) {
                                    val isOcrAllowed = aiConfig.isBucketEligible(
                                        com.inferno.gallery.data.ai.AiIndexingFeature.OCR_TEXT,
                                        item.bucketName,
                                        currentExcluded
                                    )
                                    val ocrText = if (isOcrAllowed) ocrMap[item.id] else null
                                    val matchesOcr = if (!ocrText.isNullOrBlank()) {
                                        ocrText.contains(cleanQ, ignoreCase = true) ||
                                            (queryTokens.size > 1 && queryTokens.all { t -> ocrText.contains(t, ignoreCase = true) })
                                    } else false

                                    if (matchesOcr) {
                                        val snippet = DatabaseProvider.extractOcrSnippet(ocrText!!, cleanQ)
                                        ftsResults.add(
                                            item.copy(
                                                matchType = "OCR",
                                                matchSnippet = snippet
                                            )
                                        )
                                    } else {
                                        filteredSmartResults.add(item)
                                    }
                                }
                            }
                        }
                    } else {
                        filteredSmartResults.addAll(unclassifiedSmart)
                    }

                    // Sort FTS results AFTER promoting OCR+semantic dual matches so high-confidence
                    // dual matches consistently float to the top of direct matches.
                    ftsResults.sortByDescending { item ->
                        val semanticScore = smartScoreMap[item.id] ?: 0f
                        if (semanticScore > 0f) 10f + semanticScore else 1f
                    }

                    // Intersect FTS + Smart matches with any active composable metadata filters (#53)
                    val finalFts: List<GalleryItem>
                    val finalSmart: List<GalleryItem>
                    if (filters.isActive) {
                        val allCandidateLongIds = (ftsResults.mapNotNull { it.id.toLongOrNull() } +
                            filteredSmartResults.mapNotNull { it.id.toLongOrNull() }).distinct()
                        val allowedIdSet = if (allCandidateLongIds.isEmpty()) {
                            emptySet()
                        } else {
                            // Chunk in groups of 400 to stay comfortably under SQLite bind-arg limits
                            allCandidateLongIds.chunked(400)
                                .flatMap { chunk -> executeLocalMetadataFilter(filters, currentExcluded, chunk) }
                                .mapTo(HashSet()) { it.id }
                        }
                        finalFts = ftsResults.filter { it.id in allowedIdSet }
                        finalSmart = filteredSmartResults.filter { it.id in allowedIdSet }
                    } else {
                        finalFts = ftsResults
                        finalSmart = filteredSmartResults
                    }

                    searchMatchMap.clear()
                    finalFts.forEach { item ->
                        item.matchType?.let { type ->
                            searchMatchMap[item.id] = type to item.matchSnippet
                        }
                    }
                    finalSmart.forEach { item ->
                        item.matchType?.let { type ->
                            searchMatchMap[item.id] = type to item.matchSnippet
                        }
                    }

                    if (query == _searchQuery.value && filters == _searchFilters.value) {
                        _ftsSearchResults.value = finalFts
                        _smartSearchResults.value = finalSmart
                    }
                }
            } finally {
                if (query == _searchQuery.value && filters == _searchFilters.value) {
                    _isSearching.value = false
                }
            }
    }



    private var saveSortOrderJob: kotlinx.coroutines.Job? = null
    fun setSortOrder(order: SortOrder) {
        if (_sortOrder.value == order) return
        _sortOrder.value = order
        saveSortOrderJob?.cancel()
        saveSortOrderJob = viewModelScope.launch {
            settingsRepository.updateSortOrder(order.name)
        }
    }

    private var saveAlbumSortOrderJob: kotlinx.coroutines.Job? = null
    fun setAlbumSortOrder(order: SortOrder) {
        if (_albumSortOrder.value == order) return
        _albumSortOrder.value = order
        saveAlbumSortOrderJob?.cancel()
        saveAlbumSortOrderJob = viewModelScope.launch {
            settingsRepository.updateAlbumSortOrder(order.name)
        }
    }

    private var saveAlbumLayoutJob: kotlinx.coroutines.Job? = null
    fun setAlbumLayoutMode(mode: AlbumLayoutMode) {
        if (_albumLayoutMode.value == mode) return
        _albumLayoutMode.value = mode
        saveAlbumLayoutJob?.cancel()
        saveAlbumLayoutJob = viewModelScope.launch {
            settingsRepository.updateAlbumLayoutMode(mode)
        }
    }

    private var saveViewModeJob: kotlinx.coroutines.Job? = null
    fun setViewMode(mode: ViewMode) {
        if (_viewMode.value == mode) return
        _viewMode.value = mode
        saveViewModeJob?.cancel()
        saveViewModeJob = viewModelScope.launch {
            settingsRepository.updateViewMode(mode.name)
        }
    }

    private var saveGroupByJob: kotlinx.coroutines.Job? = null
    fun setTimelineGroupBy(groupBy: TimelineGroupBy) {
        if (_timelineGroupBy.value == groupBy) return
        _timelineGroupBy.value = groupBy
        saveGroupByJob?.cancel()
        saveGroupByJob = viewModelScope.launch {
            settingsRepository.updateTimelineGroupBy(groupBy.name)
        }
    }



    fun setFilter(index: Int) {
        viewModelScope.launch {
            settingsRepository.updateSelectedFilterIndex(index)
        }
    }

    fun setGridCellsCount(count: Int) {
        val clamped = count.coerceIn(2, 8)
        _gridCellsCount.value = clamped
        saveGridCellsJob?.cancel()
        saveGridCellsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            settingsRepository.updateGridCellsCount(clamped)
        }
    }

    private val _uiEvents = kotlinx.coroutines.channels.Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun removeMediaOptimistically(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = listOf(
                uriString,
                com.inferno.gallery.data.MediaUriUtils.normalizeUriString(uriString),
                if (uriString.startsWith("content://media/external/")) uriString.replaceFirst("content://media/external/", "content://media/external_primary/") else uriString
            ).distinct()
            database.mediaDao().updateBucketByUrisChunked(allPossibleUris, "Trash")
            // Also remove from the detail pager list immediately
            withContext(Dispatchers.Main) {
                _detailMedia.value = _detailMedia.value.filter { it.uri.toString() !in allPossibleUris }
                if (_initialDetailItem.value?.uri?.toString() in allPossibleUris) {
                    _initialDetailItem.value = null
                }
                _uiEvents.send(UiEvent.DeleteSuccess)
            }
        }
    }

    fun deleteMediaPermanentlyOptimistically(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = listOf(
                uriString,
                com.inferno.gallery.data.MediaUriUtils.normalizeUriString(uriString),
                if (uriString.startsWith("content://media/external/")) uriString.replaceFirst("content://media/external/", "content://media/external_primary/") else uriString
            ).distinct()
            database.mediaDao().deleteByUrisChunked(allPossibleUris)
            withContext(Dispatchers.Main) {
                _detailMedia.value = _detailMedia.value.filter { it.uri.toString() !in allPossibleUris }
                if (_initialDetailItem.value?.uri?.toString() in allPossibleUris) {
                    _initialDetailItem.value = null
                }
                _uiEvents.send(UiEvent.DeleteSuccess)
            }
        }
    }

    fun notifyExternalMediaModifiedOrDeleted(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.mediaDao().deleteByUri(uriString)
            withContext(Dispatchers.Main) {
                _detailMedia.value = _detailMedia.value.filter { it.uri.toString() != uriString }
                if (_initialDetailItem.value?.uri?.toString() == uriString) {
                    _initialDetailItem.value = null
                }
            }
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>()
                .build()
            androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "MediaSyncWorker_Foreground",
                androidx.work.ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }

    fun notifyMediaItemEdited(savedUri: Uri, isOverwrite: Boolean) {
        val now = System.currentTimeMillis() / 1000
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Evict all caches (Coil memory, Coil disk, MediaStore OS thumbnail)
            com.inferno.gallery.ui.utils.ImageCacheEvictor.evictMedia(getApplication(), savedUri)

            if (isOverwrite) {
                // 2. Update Room DB dateModified so timeline & paging detect modification
                database.mediaDao().updateDateModifiedByUri(savedUri.toString(), now)

                // 3. Update current detail view item so DetailScreen re-evaluates its image request
                withContext(Dispatchers.Main) {
                    val uriStr = savedUri.toString()
                    val currentList = _detailMedia.value
                    if (currentList.isNotEmpty()) {
                        _detailMedia.value = currentList.map { item ->
                            if (item.uriString == uriStr || item.uri == savedUri) {
                                item.copy(dateModified = now)
                            } else {
                                item
                            }
                        }
                    }
                    _initialDetailItem.value?.let { initItem ->
                        if (initItem.uriString == uriStr || initItem.uri == savedUri) {
                            _initialDetailItem.value = initItem.copy(dateModified = now)
                        }
                    }
                }
            }

            // 4. Invalidate and refresh media in background
            refreshMedia()
        }
    }

    fun restoreMediaOptimistically(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = listOf(
                uriString,
                com.inferno.gallery.data.MediaUriUtils.normalizeUriString(uriString),
                if (uriString.startsWith("content://media/external/")) uriString.replaceFirst("content://media/external/", "content://media/external_primary/") else uriString
            ).distinct()
            val entities = database.mediaDao().getMediaByUrisList(allPossibleUris)
            val entity = entities.firstOrNull()
            val parentName = if (entity != null && entity.filePath.isNotBlank()) {
                java.io.File(entity.filePath).parentFile?.name
            } else null
            val originalBucket = if (parentName != null && !parentName.startsWith(".")) {
                parentName
            } else "Camera"
            database.mediaDao().updateBucketByUrisChunked(allPossibleUris, originalBucket)
            withContext(Dispatchers.Main) {
                _detailMedia.value = _detailMedia.value.filter { it.uri.toString() !in allPossibleUris }
                if (_initialDetailItem.value?.uri?.toString() in allPossibleUris) {
                    _initialDetailItem.value = null
                }
                _uiEvents.send(UiEvent.DeleteSuccess)
            }
        }
    }

    fun moveMedia(uris: List<Uri>, targetBucket: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<android.app.Application>().contentResolver
                var successCount = 0
                val allMedia = database.mediaDao().getAllMedia().associateBy { it.uriString }
                val targetDir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    targetBucket
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val movedPaths = mutableListOf<String>()

                for (uri in uris) {
                    val uriString = uri.toString()
                    val entity = allMedia[uriString]
                    var movedSuccessfully = false

                    // Direct file rename if path exists
                    if (entity != null) {
                        val srcFile = java.io.File(entity.filePath)
                        if (srcFile.exists()) {
                            val destFile = java.io.File(targetDir, srcFile.name)
                            if (srcFile.renameTo(destFile)) {
                                database.mediaDao().updatePathAndBucket(
                                    id = entity.id,
                                    newPath = destFile.absolutePath,
                                    newBucket = targetBucket,
                                    newName = destFile.name
                                )
                                movedPaths.add(srcFile.absolutePath)
                                movedPaths.add(destFile.absolutePath)
                                movedSuccessfully = true
                                successCount++
                            }
                        }
                    }

                    // Fallback to MediaStore update
                    if (!movedSuccessfully && uriString.startsWith("content://")) {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/" + targetBucket)
                        }
                        try {
                            val rows = resolver.update(uri, values, null, null)
                            if (rows > 0) {
                                database.mediaDao().updateBucketByUri(uriString, targetBucket)
                                successCount++
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("GalleryViewModel", "Failed to move media via MediaStore: ${e.message}")
                        }
                    }
                }

                if (movedPaths.isNotEmpty()) {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        movedPaths.toTypedArray(),
                        null,
                        null
                    )
                }

                val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "MediaSyncWorker",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )

                withContext(Dispatchers.Main) {
                    showToast("Moved $successCount items to $targetBucket")
                    onComplete?.invoke(successCount > 0)
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error moving media: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error moving media: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun moveSelectedMedia(targetBucket: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        moveMedia(selected.map { Uri.parse(it) }, targetBucket) {
            clearSelection()
        }
    }

    fun moveMediaToPath(uris: List<Uri>, targetDirectoryPath: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                val targetDir = java.io.File(targetDirectoryPath)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val allMedia = database.mediaDao().getAllMedia().associateBy { it.uriString }
                val movedPaths = mutableListOf<String>()

                for (uri in uris) {
                    val uriString = uri.toString()
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

                val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "MediaSyncWorker",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )

                withContext(Dispatchers.Main) {
                    showToast("Moved to ${targetDir.name}")
                    onComplete?.invoke(movedPaths.isNotEmpty())
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error moving media to path: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error moving media: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun renameAlbum(
        oldBucketName: String,
        newBucketName: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val trimmed = newBucketName.trim()
        if (trimmed.isEmpty()) {
            showToast("Album name cannot be empty")
            onComplete?.invoke(false, null)
            return
        }
        if (trimmed == oldBucketName) {
            onComplete?.invoke(true, trimmed)
            return
        }
        if (!com.inferno.gallery.data.BucketNames.isRenameableAlbum(oldBucketName)) {
            showToast("This album cannot be renamed")
            onComplete?.invoke(false, null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entities = database.mediaDao().getMediaByBucket(oldBucketName)
                if (entities.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showToast("No media found in album $oldBucketName")
                        onComplete?.invoke(false, null)
                    }
                    return@launch
                }

                val movedPaths = mutableListOf<String>()
                val oldPaths = mutableListOf<String>()

                // Locate directory on disk
                val firstFile = entities.firstNotNullOfOrNull {
                    val f = java.io.File(it.filePath)
                    if (f.exists()) f else null
                }
                val oldDir = firstFile?.parentFile

                if (oldDir != null && oldDir.exists() && oldDir.isDirectory) {
                    val targetDir = java.io.File(oldDir.parentFile, trimmed)
                    val renamed = oldDir.renameTo(targetDir)
                    if (!renamed) {
                        if (!targetDir.exists()) {
                            targetDir.mkdirs()
                        }
                        for (entity in entities) {
                            val src = java.io.File(entity.filePath)
                            if (src.exists()) {
                                val dst = java.io.File(targetDir, src.name)
                                if (src.renameTo(dst)) {
                                    oldPaths.add(src.absolutePath)
                                    movedPaths.add(dst.absolutePath)
                                    database.mediaDao().updatePathAndBucket(
                                        id = entity.id,
                                        newPath = dst.absolutePath,
                                        newBucket = trimmed,
                                        newName = dst.name
                                    )
                                }
                            }
                        }
                    } else {
                        // Entire folder directory was atomically renamed
                        val oldDirPath = oldDir.absolutePath
                        val targetDirPath = targetDir.absolutePath
                        for (entity in entities) {
                            val oldPath = entity.filePath
                            val newPath = if (oldPath.startsWith(oldDirPath)) {
                                targetDirPath + oldPath.substring(oldDirPath.length)
                            } else {
                                java.io.File(targetDir, java.io.File(oldPath).name).absolutePath
                            }
                            oldPaths.add(oldPath)
                            movedPaths.add(newPath)
                            database.mediaDao().updatePathAndBucket(
                                id = entity.id,
                                newPath = newPath,
                                newBucket = trimmed,
                                newName = entity.name
                            )
                        }
                    }
                } else {
                    database.mediaDao().updateBucketName(oldBucketName, trimmed)
                }

                // Update MediaStore BUCKET_DISPLAY_NAME
                try {
                    val resolver = getApplication<android.app.Application>().contentResolver
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, trimmed)
                    }
                    for (entity in entities) {
                        if (entity.uriString.startsWith("content://")) {
                            try {
                                resolver.update(android.net.Uri.parse(entity.uriString), values, null, null)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GalleryViewModel", "Error updating MediaStore for album rename: ${e.message}")
                }

                // Trigger MediaScanner for new and old paths
                if (movedPaths.isNotEmpty()) {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        movedPaths.toTypedArray(),
                        null,
                        null
                    )
                }
                if (oldPaths.isNotEmpty()) {
                    android.media.MediaScannerConnection.scanFile(
                        getApplication(),
                        oldPaths.toTypedArray(),
                        null,
                        null
                    )
                }

                // Update pinned folders preference
                settingsRepository.renamePinnedFolder(oldBucketName, trimmed)

                // Update custom album shape
                val shape = albumCustomShapes.value[oldBucketName]
                if (shape != null) {
                    removeAlbumCustomShape(oldBucketName)
                    setAlbumCustomShape(trimmed, shape)
                }

                // Update custom album cover
                val cover = albumCustomCovers.value[oldBucketName]
                if (cover != null) {
                    removeAlbumCustomCover(oldBucketName)
                    setAlbumCustomCover(trimmed, cover)
                }

                // If currently viewing this bucket, update current bucket
                if (_currentBucket.value == oldBucketName) {
                    _currentBucket.value = trimmed
                }

                // Schedule background media sync
                val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "MediaSyncWorker",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )

                withContext(Dispatchers.Main) {
                    showToast("Album renamed to \"$trimmed\"")
                    onComplete?.invoke(true, trimmed)
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error renaming album: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error renaming album: ${e.message}")
                    onComplete?.invoke(false, null)
                }
            }
        }
    }

    fun deleteAlbum(
        bucketName: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (!com.inferno.gallery.data.BucketNames.isRenameableAlbum(bucketName)) {
            showToast("This album cannot be deleted")
            onComplete?.invoke(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                val resolver = context.contentResolver
                val entities = database.mediaDao().getMediaByBucket(bucketName)

                val candidateDirs = linkedSetOf<java.io.File>()
                val deletedUris = mutableListOf<String>()

                for (entity in entities) {
                    val file = java.io.File(entity.filePath)
                    file.parentFile?.let { candidateDirs.add(it) }

                    var deletedOnDisk = false
                    if (file.exists()) {
                        deletedOnDisk = runCatching { file.delete() }.getOrDefault(false)
                    }
                    val deletedInStore = runCatching {
                        resolver.delete(android.net.Uri.parse(entity.uriString), null, null) > 0
                    }.getOrDefault(false)

                    if (deletedOnDisk || deletedInStore || !file.exists()) {
                        deletedUris.add(entity.uriString)
                        com.inferno.gallery.data.MediaUriUtils.normalizeUriString(entity.uriString)
                            .takeIf { it != entity.uriString }
                            ?.let { deletedUris.add(it) }
                    }
                }

                // Also check Pictures/<bucketName> in case it was an empty album folder
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                )
                val emptyAlbumDir = java.io.File(picturesDir, bucketName)
                if (emptyAlbumDir.exists() && emptyAlbumDir.isDirectory) {
                    candidateDirs.add(emptyAlbumDir)
                }

                // Remove emptied directories (including any .nomedia file inside)
                for (dir in candidateDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val children = dir.listFiles() ?: emptyArray()
                        if (children.isEmpty() || children.all { it.name == ".nomedia" }) {
                            children.forEach { runCatching { it.delete() } }
                            runCatching { dir.delete() }
                        }
                    }
                }

                if (deletedUris.isNotEmpty()) {
                    database.mediaDao().deleteByUrisChunked(deletedUris.distinct())
                }

                // Clean up album metadata & preferences
                if (albumCustomShapes.value.containsKey(bucketName)) {
                    removeAlbumCustomShape(bucketName)
                }
                if (albumCustomCovers.value.containsKey(bucketName)) {
                    removeAlbumCustomCover(bucketName)
                }
                if (userPinnedAlbums.value.any { it.bucketName == bucketName }) {
                    settingsRepository.togglePinnedFolder(bucketName)
                }

                refreshEmptyAlbums()
                if (_currentBucket.value == bucketName) {
                    _currentBucket.value = null
                }

                val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.MediaSyncWorker>().build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "MediaSyncWorker",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncWorkRequest
                )

                withContext(Dispatchers.Main) {
                    showToast("Deleted album \"$bucketName\"")
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error deleting album: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error deleting album: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun moveSelectedMediaToPath(targetDirectoryPath: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        moveMediaToPath(selected.map { Uri.parse(it) }, targetDirectoryPath) {
            clearSelection()
        }
    }

    fun copyMedia(uris: List<Uri>, targetBucket: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<android.app.Application>().contentResolver
                var successCount = 0
                for (uri in uris) {
                    val uriString = uri.toString()
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
                        val isVideo = mimeType.startsWith("video/")
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

                withContext(Dispatchers.Main) {
                    showToast("Copied $successCount items to $targetBucket")
                    onComplete?.invoke(successCount > 0)
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error copying media: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error copying media: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun copySelectedMedia(targetBucket: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        copyMedia(selected.map { Uri.parse(it) }, targetBucket) {
            clearSelection()
        }
    }

    fun copyMediaToPath(uris: List<Uri>, targetDirectoryPath: String, onComplete: ((Boolean) -> Unit)? = null) {
        if (uris.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                val targetDir = java.io.File(targetDirectoryPath)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val allMedia = database.mediaDao().getAllMedia().associateBy { it.uriString }
                val copiedPaths = mutableListOf<String>()

                for (uri in uris) {
                    val entity = allMedia[uri.toString()] ?: continue
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
                    showToast("Copied to ${targetDir.name}")
                    onComplete?.invoke(copiedPaths.isNotEmpty())
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Error copying media to path: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showToast("Error copying media: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun copySelectedMediaToPath(targetDirectoryPath: String) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        copyMediaToPath(selected.map { Uri.parse(it) }, targetDirectoryPath) {
            clearSelection()
        }
    }

    fun trashSelectedMedia(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = uris.flatMap {
                listOf(
                    it,
                    com.inferno.gallery.data.MediaUriUtils.normalizeUriString(it),
                    if (it.startsWith("content://media/external/")) it.replaceFirst("content://media/external/", "content://media/external_primary/") else it
                )
            }.distinct()
            database.mediaDao().updateBucketByUrisChunked(allPossibleUris, "Trash")
            withContext(Dispatchers.Main) {
                _uiEvents.send(UiEvent.DeleteSuccess)
            }
        }
    }

    fun restoreSelectedMedia(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = uris.flatMap {
                listOf(
                    it,
                    com.inferno.gallery.data.MediaUriUtils.normalizeUriString(it),
                    if (it.startsWith("content://media/external/")) it.replaceFirst("content://media/external/", "content://media/external_primary/") else it
                )
            }.distinct()
            val updatesByBucket = mutableMapOf<String, MutableList<String>>()
            for (chunk in allPossibleUris.chunked(500)) {
                val entities = database.mediaDao().getMediaByUrisList(chunk)
                for (entity in entities) {
                    val parentName = if (entity.filePath.isNotBlank()) {
                        java.io.File(entity.filePath).parentFile?.name
                    } else null
                    val originalBucket = if (parentName != null && !parentName.startsWith(".")) {
                        parentName
                    } else "Camera"
                    updatesByBucket.getOrPut(originalBucket) { mutableListOf() }.add(entity.uriString)
                }
            }
            database.mediaDao().restoreMediaToBuckets(updatesByBucket)
            withContext(Dispatchers.Main) {
                _uiEvents.send(UiEvent.DeleteSuccess)
            }
        }
    }

    fun deleteSelectedMediaFromDb(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val allPossibleUris = uris.flatMap {
                listOf(
                    it,
                    com.inferno.gallery.data.MediaUriUtils.normalizeUriString(it),
                    if (it.startsWith("content://media/external/")) it.replaceFirst("content://media/external/", "content://media/external_primary/") else it
                )
            }.distinct()
            database.mediaDao().deleteByUrisChunked(allPossibleUris)
            withContext(Dispatchers.Main) {
                _uiEvents.send(UiEvent.DeleteSuccess)
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

                    settingsRepository.updateShowEmptyAlbums(true)
                    refreshEmptyAlbums()

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
            val request = OneTimeWorkRequestBuilder<com.inferno.gallery.workers.OcrIndexWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork("OcrIndexWorker", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun stopOcrIndexing() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).cancelUniqueWork("OcrIndexWorker")
        }
    }

    fun clearOcrIndexAndReindex() {
        viewModelScope.launch(Dispatchers.IO) {
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

    val currentMediaCount: StateFlow<Int> = combine(
        combine(_currentBucket, selectedFilterIndex, favoritesManager.favoritesFlow, albumMediaFilter) { b, f, favs, af ->
            Triple(b, f, Pair(favs, af))
        },
        combine(excludedFolders, ftsSearchResults, smartSearchResults) { excl, fts, smart -> Triple(excl, fts, smart) }
    ) { (bucket, filterIndex, favsAndFilter), (excluded, fts, smart) ->
        val (favs, af) = favsAndFilter
        val qc = buildMediaConditions(
            bucket = bucket,
            filterIndex = filterIndex,
            excluded = excluded,
            favIds = favs,
            ftsIds = fts.map { it.id },
            smartIds = smart.map { it.id },
            albumMediaFilter = af
        )
        val queryString = "SELECT COUNT(*) FROM core_media cm " + buildWhereClause(qc)
        androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
    }.flatMapLatest { query ->
        database.mediaDao().observeCountRaw(query)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val rawPagerFlow: Flow<PagingData<com.inferno.gallery.data.db.CoreMediaProjection>> = combine(
        combine(_currentBucket, selectedFilterIndex, sortOrder, albumMediaFilter) { b, f, o, af ->
            Triple(b, f, Pair(o, af))
        },
        combine(favoritesManager.favoritesFlow, excludedFolders) { favs, excluded -> Pair(favs, excluded) },
        combine(ftsSearchResults, smartSearchResults) { fts, smart -> Pair(fts, smart) }
    ) { (bucket, filterIndex, orderAndFilter), (favs, excluded), (fts, smart) ->
        val (order, af) = orderAndFilter
        val smartIds = smart.map { it.id }
        val ftsIds = fts.map { it.id }
        val qc = buildMediaConditions(
            bucket = bucket,
            filterIndex = filterIndex,
            excluded = excluded,
            favIds = favs,
            ftsIds = ftsIds,
            smartIds = smartIds,
            albumMediaFilter = af
        )
        val isSmartSearch = bucket == com.inferno.gallery.data.BucketNames.SEARCH_SMART && smartIds.isNotEmpty()

        val queryString = "SELECT cm.* FROM core_media cm " +
            buildWhereClause(qc) + buildOrderClause(order, bucket, smartIds)
        val query = androidx.sqlite.db.SimpleSQLiteQuery(queryString, qc.args.toTypedArray())
        Pager(
            // enablePlaceholders = false: the grid's `span` lambda runs during the MEASURE pass,
            // so a placeholder resolving into a date Header flipped GridItemSpan(1) ->
            // maxLineSpan mid-fling and forced a row re-measure (a dropped frame). Disabling
            // placeholders also drops a COUNT(*) over the predicate from every generation.
            config = PagingConfig(pageSize = 120, prefetchDistance = 240, enablePlaceholders = false)
        ) {
            database.mediaDao().observeMediaPagingRaw(query)
        }.flow
    }.flatMapLatest { it }.cachedIn(viewModelScope)

    // Simplified paging pipeline — direct entity-to-GalleryItem mapping.
    // Previously this was a combine() of 5 sources (rawPagerFlow + photoStacksEnabledFlow +
    // observeUnstackedIds + duplicates + similarPhotos). Any emission from any of those 5
    // flows fully rebuilt the PagingData and caused visible grid stutter even while stationary.
    // Photo stacks feature is detached from the grid pipeline; duplicates / similarPhotos
    // StateFlows are kept for the Cleaner feature and are unaffected.
    val pagedMediaRaw: Flow<PagingData<GalleryItem>> = rawPagerFlow.map { pagingData ->
        pagingData.map { entity ->
            val uri = Uri.parse(entity.uriString)
            val matchInfo = searchMatchMap[entity.id.toString()]
            GalleryItem(
                id = entity.id.toString(),
                uri = uri,
                uriString = entity.uriString,
                bucketName = entity.bucketName,
                dateAdded = entity.dateAdded,
                size = entity.size,
                name = entity.name,
                dateModified = entity.dateModified,
                path = entity.filePath,
                isVideo = entity.isVideo,
                durationMs = entity.durationMs,
                localExists = true,
                resolvedUri = resolveFileUri(uri, entity.filePath, entity.bucketName),
                latitude = entity.latitude,
                longitude = entity.longitude,
                matchType = matchInfo?.first,
                matchSnippet = matchInfo?.second
            )
        }
    }.cachedIn(viewModelScope)

    companion object {
        private val headerFormatterWithYear = java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.getDefault())
        private val monthHeaderFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())
        private val headerCache = java.util.concurrent.ConcurrentHashMap<Long, String>()
        private val monthHeaderCache = java.util.concurrent.ConcurrentHashMap<Int, String>()

        fun formatGroupHeader(
            dateAddedSeconds: Long,
            groupBy: TimelineGroupBy = TimelineGroupBy.Day
        ): String {
            val timeMs = dateAddedSeconds * 1000L
            val offsetMs = java.util.TimeZone.getDefault().getOffset(timeMs).toLong()
            val localDay = (timeMs + offsetMs) / 86400000L

            return when (groupBy) {
                TimelineGroupBy.Day -> headerCache.getOrPut(localDay) {
                    val itemDate = java.time.LocalDate.ofEpochDay(localDay)
                    val today = java.time.LocalDate.now()
                    val yesterday = today.minusDays(1)

                    when (itemDate) {
                        today -> "Today"
                        yesterday -> "Yesterday"
                        else -> itemDate.format(headerFormatterWithYear)
                    }
                }
                TimelineGroupBy.Month -> {
                    val itemDate = java.time.LocalDate.ofEpochDay(localDay)
                    val ymKey = itemDate.year * 12 + itemDate.monthValue
                    monthHeaderCache.getOrPut(ymKey) {
                        itemDate.format(monthHeaderFormatter)
                    }
                }
                TimelineGroupBy.Year -> {
                    java.time.LocalDate.ofEpochDay(localDay).year.toString()
                }
            }
        }
    }

    val pagedMedia: Flow<PagingData<GalleryListItem>> = combine(
        viewMode,
        timelineGroupBy,
        sortOrder,
        _currentBucket,
        selectedFilterIndex,
        photoStacksEnabled,
        photoStackIndex
    ) { values ->
        values
    }.flatMapLatest { values ->
        val mode = values[0] as ViewMode
        val groupBy = values[1] as TimelineGroupBy
        val order = values[2] as SortOrder
        val bucket = values[3] as String?
        val filterIdx = values[4] as Int
        val stacksEnabled = values[5] as Boolean
        val stackIndex = values[6] as com.inferno.gallery.data.PhotoStackIndex
        val shouldStack = stacksEnabled && bucket == null && filterIdx == 0 && !stackIndex.isEmpty
        val isSmartSearch = bucket == BucketNames.SEARCH_SMART
        val isDateSort = (order == SortOrder.NewToOld || order == SortOrder.OldToNew ||
                         order == SortOrder.DateModifiedNewToOld || order == SortOrder.DateModifiedOldToNew) && !isSmartSearch
        pagedMediaRaw.map { rawData ->
            val pagingData = if (shouldStack) {
                rawData
                    .filter { item ->
                        val idLong = item.id.toLongOrNull()
                        idLong == null || idLong !in stackIndex.hiddenMemberIds
                    }
                    .map { item ->
                        val count = item.id.toLongOrNull()?.let { stackIndex.stackCountForCover(it) } ?: 1
                        if (count > 1) item.copy(stackCount = count) else item
                    }
            } else {
                rawData
            }
            if (mode == ViewMode.Immersive || !isDateSort) {
                // No date headers in Immersive mode, Smart Search (ordered by relevance), or non-date sorts
                pagingData.map { GalleryListItem.Item(it) as GalleryListItem }
            } else {
                pagingData.insertSeparators { before: GalleryItem?, after: GalleryItem? ->
                    if (after == null) return@insertSeparators null

                    val afterDate = if (order == SortOrder.DateModifiedNewToOld || order == SortOrder.DateModifiedOldToNew) {
                        after.dateModified
                    } else {
                        after.dateAdded
                    }
                    val afterTitle = formatGroupHeader(afterDate, groupBy)

                    if (before == null) {
                        GalleryListItem.Header(title = afterTitle, id = afterTitle)
                    } else {
                        val beforeDate = if (order == SortOrder.DateModifiedNewToOld || order == SortOrder.DateModifiedOldToNew) {
                            before.dateModified
                        } else {
                            before.dateAdded
                        }
                        val beforeTitle = formatGroupHeader(beforeDate, groupBy)
                        if (beforeTitle != afterTitle) {
                            GalleryListItem.Header(title = afterTitle, id = afterTitle)
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

    private val scrollPositions = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Int>>()

    fun saveGridScrollPosition(bucket: String?, index: Int, offset: Int) {
        val key = bucket ?: "main_gallery"
        scrollPositions[key] = Pair(index, offset)
    }

    fun getSavedGridScrollPosition(bucket: String?): Pair<Int, Int> {
        val key = bucket ?: "main_gallery"
        return scrollPositions[key] ?: Pair(0, 0)
    }

    fun resetGridScrollPosition(bucket: String?) {
        val key = bucket ?: "main_gallery"
        android.util.Log.d("NativePhotonGrid", "resetGridScrollPosition: key=$key")
        scrollPositions.remove(key)
    }

    fun syncGridScrollToMedia(bucket: String?, mediaId: String) {
        val initialId = _initialDetailItem.value?.id
        // If user is still viewing the item they opened, preserve their exact pixel scroll position
        if (initialId != null && initialId == mediaId) {
            android.util.Log.d("NativePhotonGrid", "syncGridScrollToMedia: same item $mediaId, preserving scroll")
            return
        }
        val items = nativeTimelineItems.value
        if (items.isNotEmpty()) {
            val index = items.indexOfFirst { it is com.inferno.gallery.data.NativeTimelineItem.Media && it.id == mediaId }
            if (index >= 0) {
                android.util.Log.d("NativePhotonGrid", "syncGridScrollToMedia: swiped to item $mediaId at grid index $index")
                saveGridScrollPosition(bucket, index, 0)
            }
        }
    }

    private val _albumsScrollToTopTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val albumsScrollToTopTrigger: SharedFlow<Unit> = _albumsScrollToTopTrigger.asSharedFlow()

    fun triggerAlbumsScrollToTop() {
        _albumsScrollToTopTrigger.tryEmit(Unit)
    }

    private val _collectionsScrollToTopTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val collectionsScrollToTopTrigger: SharedFlow<Unit> = _collectionsScrollToTopTrigger.asSharedFlow()

    fun triggerCollectionsScrollToTop() {
        _collectionsScrollToTopTrigger.tryEmit(Unit)
    }

    private val _photosScrollToTopTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val photosScrollToTopTrigger: SharedFlow<Unit> = _photosScrollToTopTrigger.asSharedFlow()

    fun triggerPhotosScrollToTop() {
        _photosScrollToTopTrigger.tryEmit(Unit)
    }

    private val _videosScrollToTopTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val videosScrollToTopTrigger: SharedFlow<Unit> = _videosScrollToTopTrigger.asSharedFlow()

    fun triggerVideosScrollToTop() {
        _videosScrollToTopTrigger.tryEmit(Unit)
    }
}

