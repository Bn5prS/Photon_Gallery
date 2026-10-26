package com.inferno.gallery.ui.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.ai.FaceClusteringManager
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.PersonEntity
import com.inferno.gallery.data.db.MergeSuggestionWithPersons
import androidx.work.WorkInfo
import androidx.work.WorkManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.inferno.gallery.data.ai.FaceScanHelper
import java.io.File
import java.io.FileOutputStream
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PeopleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    private val faceDao = database.faceDao()
    private val clusteringManager = FaceClusteringManager.getInstance(application)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.recalculateAllPersonFaceCounts()
        }
    }

    val faceIndexWorkInfo: StateFlow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceIndexWorker")
        .map { it.firstOrNull() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val faceProgress = com.inferno.gallery.data.IndexingProgressManager.faceProgress

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPersonIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPersonIds: StateFlow<Set<Long>> = _selectedPersonIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedPersonIds
        .combine(MutableStateFlow(Unit)) { selected, _ -> selected.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val promotedPersons: StateFlow<List<PersonEntity>> = faceDao.observePromotedPersons()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { person ->
                    person.name?.contains(query, ignoreCase = true) == true
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersons: StateFlow<List<PersonEntity>> = faceDao.observeAllVisiblePersons()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { person ->
                    person.name?.contains(query, ignoreCase = true) == true
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersonsIncludingHidden: StateFlow<List<PersonEntity>> = faceDao.observeAllPersonsIncludingHidden()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPeopleCount: StateFlow<Int> = faceDao.observePromotedPersons()
        .combine(MutableStateFlow(Unit)) { list, _ -> list.size }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingMergeSuggestions: StateFlow<List<MergeSuggestionWithPersons>> = faceDao.observePendingMergeSuggestions()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelectPerson(personId: Long) {
        val current = _selectedPersonIds.value
        _selectedPersonIds.value = if (current.contains(personId)) {
            current - personId
        } else {
            current + personId
        }
    }

    fun clearSelection() {
        _selectedPersonIds.value = emptySet()
    }

    suspend fun checkNameCollision(personId: Long, name: String): PersonEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return faceDao.findPersonByName(trimmed, personId)
    }

    fun setPersonName(personId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = name.trim().ifEmpty { null }
            faceDao.setPersonName(personId, trimmed)
        }
    }

    fun setPersonCover(personId: Long, cropPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setPersonCover(personId, cropPath)
            clusteringManager.invalidateCache()
        }
    }

    fun setMediaAsCover(personId: Long, mediaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val cropPath = faceDao.getCropCachePathForMedia(mediaId, personId)
            if (cropPath != null && File(cropPath).exists()) {
                faceDao.setPersonCover(personId, cropPath)
                clusteringManager.invalidateCache()
                return@launch
            }

            // Fallback: extract/generate avatar crop dynamically from the selected media
            val media = database.mediaDao().getMediaById(mediaId) ?: return@launch
            val uri = Uri.parse(media.uriString)
            val context = getApplication<Application>()
            val sampledBitmap = FaceScanHelper.decodeSampledBitmap(context, media.filePath, uri, FaceScanHelper.MAX_THUMBNAIL_DIM) ?: return@launch
            try {
                val faceCropDir = FaceScanHelper.getFaceCropDir(context)
                val cropFile = File(faceCropDir, "face_${mediaId}_cover.webp")

                val face = faceDao.getFaceForMediaAndPerson(mediaId, personId)
                val targetBox = if (face != null && face.bboxRight > face.bboxLeft && face.bboxBottom > face.bboxTop) {
                    Rect(
                        (face.bboxLeft * sampledBitmap.width).toInt().coerceIn(0, sampledBitmap.width),
                        (face.bboxTop * sampledBitmap.height).toInt().coerceIn(0, sampledBitmap.height),
                        (face.bboxRight * sampledBitmap.width).toInt().coerceIn(0, sampledBitmap.width),
                        (face.bboxBottom * sampledBitmap.height).toInt().coerceIn(0, sampledBitmap.height)
                    )
                } else {
                    // Try face detector as fallback
                    val detector = FaceDetection.getClient(FaceScanHelper.createDetectorOptions())
                    try {
                        val inputImage = InputImage.fromBitmap(sampledBitmap, 0)
                        val detectedFaces: List<Face> = Tasks.await(detector.process(inputImage))
                        if (detectedFaces.isNotEmpty()) {
                            val bestFace = detectedFaces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                            bestFace?.boundingBox ?: Rect(0, 0, sampledBitmap.width, sampledBitmap.height)
                        } else {
                            Rect(0, 0, sampledBitmap.width, sampledBitmap.height)
                        }
                    } catch (_: Exception) {
                        Rect(0, 0, sampledBitmap.width, sampledBitmap.height)
                    } finally {
                        detector.close()
                    }
                }

                val avatarBitmap = FaceScanHelper.extractNaturalPortraitCrop(
                    sampledBitmap,
                    targetBox,
                    160
                )
                try {
                    FileOutputStream(cropFile).use { out ->
                        avatarBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                    }
                    faceDao.setPersonCover(personId, cropFile.absolutePath)
                    if (face != null) {
                        faceDao.updateFace(face.copy(cropCachePath = cropFile.absolutePath))
                    }
                    clusteringManager.invalidateCache()
                } finally {
                    avatarBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.w("PeopleViewModel", "Failed to generate cover photo: ${e.message}")
            } finally {
                sampledBitmap.recycle()
            }
        }
    }

    fun toggleFavorite(person: PersonEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setPersonFavorite(person.id, !person.isFavorite)
        }
    }

    fun hidePerson(personId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setPersonHidden(personId, true)
            _selectedPersonIds.value = _selectedPersonIds.value - personId
        }
    }

    fun unhidePerson(personId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setPersonHidden(personId, false)
        }
    }

    fun removeMediaFromPerson(personId: Long, mediaIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.unassignFacesFromPerson(mediaIds, personId)
            val remaining = faceDao.getFaceCountForPerson(personId)
            if (remaining == 0) {
                faceDao.deletePerson(personId)
            } else {
                faceDao.updatePersonFaceCount(personId, remaining)
            }
            clusteringManager.invalidateCache()
        }
    }

    fun mergePersons(
        sourcePersonId: Long,
        targetPersonId: Long,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO + NonCancellable) {
                try {
                    clusteringManager.mergePersons(sourcePersonId, targetPersonId)
                    true
                } catch (e: Exception) {
                    Log.e("PeopleViewModel", "Failed to merge persons: ${e.message}", e)
                    false
                }
            }
            if (success) {
                clearSelection()
            }
            onComplete?.invoke(success)
        }
    }

    fun mergeMultiplePersons(
        sourcePersonIds: List<Long>,
        targetPersonId: Long,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO + NonCancellable) {
                try {
                    clusteringManager.mergeMultiplePersons(sourcePersonIds, targetPersonId)
                    true
                } catch (e: Exception) {
                    Log.e("PeopleViewModel", "Failed to merge multiple persons: ${e.message}", e)
                    false
                }
            }
            if (success) {
                clearSelection()
            }
            onComplete?.invoke(success)
        }
    }

    fun moveMediaToPerson(
        sourcePersonId: Long,
        targetPersonId: Long,
        mediaIds: List<Long>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO + NonCancellable) {
                try {
                    clusteringManager.reassignMediaToPerson(sourcePersonId, targetPersonId, mediaIds)
                    true
                } catch (e: Exception) {
                    Log.e("PeopleViewModel", "Failed to move media to person: ${e.message}", e)
                    false
                }
            }
            onComplete?.invoke(success)
        }
    }

    fun createPersonAndMoveMedia(
        sourcePersonId: Long,
        name: String,
        mediaIds: List<Long>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO + NonCancellable) {
                try {
                    val trimmed = name.trim()
                    val existing = faceDao.findPersonByName(trimmed, excludePersonId = -1L)
                    val targetPersonId = if (existing != null) {
                        existing.id
                    } else {
                        faceDao.insertPerson(
                            PersonEntity(
                                name = trimmed,
                                isPromoted = true,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    clusteringManager.reassignMediaToPerson(sourcePersonId, targetPersonId, mediaIds)
                    true
                } catch (e: Exception) {
                    Log.e("PeopleViewModel", "Failed to create person and move media: ${e.message}", e)
                    false
                }
            }
            onComplete?.invoke(success)
        }
    }

    fun acceptMergeSuggestion(suggestion: MergeSuggestionWithPersons) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pA = suggestion.personA
                val pB = suggestion.personB
                // The person with a name or higher faceCount becomes the target
                val (target, source) = if (!pA.name.isNullOrBlank() || pA.faceCount >= pB.faceCount) {
                    Pair(pA, pB)
                } else {
                    Pair(pB, pA)
                }
                clusteringManager.mergePersons(sourcePersonId = source.id, targetPersonId = target.id)
            } catch (e: Exception) {
                Log.e("PeopleViewModel", "Failed to accept merge suggestion: ${e.message}", e)
            }
        }
    }

    fun dismissMergeSuggestion(suggestion: MergeSuggestionWithPersons) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setMergeSuggestionStatus(suggestion.suggestion.personIdA, suggestion.suggestion.personIdB, "DISMISSED")
        }
    }

    fun observeMediaForPerson(personId: Long): Flow<List<CoreMediaEntity>> {
        return faceDao.observeMediaForPerson(personId).flowOn(Dispatchers.IO)
    }

    fun observePerson(personId: Long): Flow<PersonEntity?> = flow {
        emit(faceDao.getPersonById(personId))
        faceDao.observeAllVisiblePersons().collect { list ->
            emit(list.find { it.id == personId })
        }
    }.flowOn(Dispatchers.IO)

    fun startFaceIndexing(forceRescan: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (forceRescan) {
                faceDao.clearAllFaceStatuses()
            }
            val request = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceIndexWorker>().build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "FaceIndexWorker",
                if (forceRescan) androidx.work.ExistingWorkPolicy.REPLACE else androidx.work.ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    fun consolidateClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clusteringManager.consolidateClusters()
            } catch (e: Exception) {
                Log.e("PeopleViewModel", "Failed to consolidate clusters: ${e.message}", e)
            }
        }
    }

    fun stopFaceIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceIndexWorker")
    }
}
