package com.inferno.gallery.ui.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inferno.gallery.data.ai.FaceClusteringManager
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.PersonEntity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
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

class PeopleViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)
    private val faceDao = database.faceDao()
    private val clusteringManager = FaceClusteringManager.getInstance(application)

    val faceIndexWorkInfo: StateFlow<WorkInfo?> = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow("FaceIndexWorker")
        .map { it.firstOrNull() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val totalPeopleCount: StateFlow<Int> = faceDao.observePromotedPersons()
        .combine(MutableStateFlow(Unit)) { list, _ -> list.size }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    fun setPersonName(personId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = name.trim().ifEmpty { null }
            faceDao.setPersonName(personId, trimmed)
        }
    }

    fun setPersonCover(personId: Long, cropPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            faceDao.setPersonCover(personId, cropPath)
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

    fun mergePersons(sourcePersonId: Long, targetPersonId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            clusteringManager.mergePersons(sourcePersonId, targetPersonId)
            clearSelection()
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
            if (forceRescan || faceDao.getFaceCount() == 0) {
                faceDao.clearAllFaceStatuses()
            }
            val request = androidx.work.OneTimeWorkRequestBuilder<com.inferno.gallery.workers.FaceIndexWorker>().build()
            WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                "FaceIndexWorker",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun stopFaceIndexing() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("FaceIndexWorker")
    }
}
