package com.inferno.gallery.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.LocalMediaRepository
import com.inferno.gallery.data.SettingsRepository
import com.inferno.gallery.data.DockStyle
import com.inferno.gallery.data.FavoritesManager
import com.inferno.gallery.data.ai.ONNXTextEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.inferno.gallery.workers.AIIndexWorker
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

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
    val durationMs: Long? = null
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
    private val textEncoder = ONNXTextEncoder(application)

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    val gridCellsCount: StateFlow<Int> = settingsRepository.gridCellsCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 4
    )

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
        val folderName = when (filterIndex) {
            1 -> "Camera"
            2 -> "Screenshots"
            else -> null
        }
        val filtered = if (bucket == "All") {
            raw
        } else if (bucket == "Videos") {
            raw.filter { it.isVideo }
        } else if (bucket != null) {
            raw.filter { it.bucketName == bucket }
        } else if (folderName != null) {
            raw.filter { it.bucketName == folderName }
        } else {
            raw
        }

        when (order) {
            SortOrder.NewToOld -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.OldToNew -> filtered.sortedBy { it.dateAdded }
            SortOrder.SmallToBig -> filtered.sortedBy { it.size }
            SortOrder.BigToSmall -> filtered.sortedByDescending { it.size }
            SortOrder.NameAsc -> filtered.sortedBy { it.name }
        }
    }.stateIn(
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
    }.stateIn(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var searchJob: kotlinx.coroutines.Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _semanticSearchResults.value = emptyList()
            _ftsSearchResults.value = emptyList()
            _isSearching.value = false
            searchJob?.cancel()
            return
        }
        // Debounce: wait 500ms after user stops typing, then fire both searches
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            performUnifiedSearch(query)
        }
    }

    private val _semanticSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val semanticSearchResults: StateFlow<List<GalleryItem>> = _semanticSearchResults.asStateFlow()

    private val _ftsSearchResults = MutableStateFlow<List<GalleryItem>>(emptyList())
    val ftsSearchResults: StateFlow<List<GalleryItem>> = _ftsSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /**
     * Runs both FTS (text-in-image) and Semantic (AI visual) searches
     * concurrently. Called automatically after a 500ms debounce.
     */
    private fun performUnifiedSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Run semantic search
                val semanticResults = withContext(Dispatchers.Default) {
                    try {
                        val queryEmbedding = textEncoder.encodeText(query)
                        val vectors = withContext(Dispatchers.IO) {
                            database.searchDao().getAllVectors()
                        }
                        val allEntities = withContext(Dispatchers.IO) {
                            database.mediaDao().getAllMedia()
                        }
                        val mediaMap = allEntities.associateBy { it.id }
                        vectors.mapNotNull { vec ->
                            val entity = mediaMap[vec.mediaId] ?: return@mapNotNull null
                            val item = GalleryItem(
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
                            val imgEmb = vec.clipVector.toFloatArray()
                            val score = cosineSimilarity(queryEmbedding, imgEmb)
                            Pair(item, score)
                        }.sortedByDescending { it.second }
                            .take(50)
                            .map { it.first }
                    } catch (e: Exception) {
                        android.util.Log.e("GalleryViewModel", "Semantic search failed: ${e.message}")
                        emptyList()
                    }
                }

                // Run FTS text-in-image search
                val ftsResults = withContext(Dispatchers.IO) {
                    try {
                        val ftsEntities = DatabaseProvider.searchFts(database, query)
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

                _semanticSearchResults.value = semanticResults
                _ftsSearchResults.value = ftsResults
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Keep legacy function for backward compat (e.g. Settings)
    fun performSemanticSearch(query: String) {
        if (query.isBlank()) {
            _semanticSearchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val queryEmbedding = withContext(Dispatchers.Default) {
                    textEncoder.encodeText(query)
                }
                val vectors = withContext(Dispatchers.IO) {
                    database.searchDao().getAllVectors()
                }
                val allEntities = withContext(Dispatchers.IO) {
                    database.mediaDao().getAllMedia()
                }
                val mediaMap = allEntities.associateBy { it.id }

                val scored = withContext(Dispatchers.Default) {
                    vectors.mapNotNull { vec ->
                        val entity = mediaMap[vec.mediaId] ?: return@mapNotNull null
                        val item = GalleryItem(
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
                        val imgEmb = vec.clipVector.toFloatArray()
                        val score = cosineSimilarity(queryEmbedding, imgEmb)
                        Pair(item, score)
                    }.sortedByDescending { it.second }
                        .take(50)
                        .map { it.first }
                }
                _semanticSearchResults.value = scored
            } catch (e: Exception) {
                android.util.Log.e("GalleryViewModel", "Semantic search failed: ${e.message}")
                _semanticSearchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun ByteArray.toFloatArray(): FloatArray {
        val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(this.size / 4) { buf.getFloat() }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
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
        viewModelScope.launch {
            settingsRepository.updateGridCellsCount(count)
        }
    }

    private val _uiEvents = kotlinx.coroutines.channels.Channel<UiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun removeMediaOptimistically(uriString: String) {
        viewModelScope.launch {
            _uiEvents.send(UiEvent.DeleteSuccess)
        }
    }

    val aiIndexWorkInfo = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("AIIndexWorker")
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun startAiIndexing() {
        val request = OneTimeWorkRequestBuilder<AIIndexWorker>().build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "AIIndexWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun stopAiIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("AIIndexWorker")
    }

    sealed class UiEvent {
        object DeleteSuccess : UiEvent()
    }

}
