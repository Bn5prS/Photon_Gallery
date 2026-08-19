package com.inferno.gallery.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IndexingProgressManager {
    data class ProgressState(
        val isIndexing: Boolean = false,
        val progress: Int = 0,
        val total: Int = 0,
        val currentImageName: String? = null
    )

    private val _ocrProgress = MutableStateFlow(ProgressState())
    val ocrProgress: StateFlow<ProgressState> = _ocrProgress.asStateFlow()

    private val _clipProgress = MutableStateFlow(ProgressState())
    val clipProgress: StateFlow<ProgressState> = _clipProgress.asStateFlow()

    fun updateOcrProgress(isIndexing: Boolean, progress: Int, total: Int, currentImageName: String? = null) {
        _ocrProgress.value = ProgressState(isIndexing, progress, total, currentImageName)
    }

    fun updateClipProgress(isIndexing: Boolean, progress: Int, total: Int, currentImageName: String? = null) {
        _clipProgress.value = ProgressState(isIndexing, progress, total, currentImageName)
    }
}
