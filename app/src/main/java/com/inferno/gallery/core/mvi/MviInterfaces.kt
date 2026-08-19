package com.inferno.gallery.core.mvi

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Represents the State of a UI screen.
 * This should be a data class.
 */
interface UiState

/**
 * Represents a user action or system event that the ViewModel should handle.
 * This should be a sealed class.
 */
interface UiIntent

/**
 * Represents one-off events that don't belong in the State (e.g., showing a Toast, Navigation).
 * This should be a sealed class.
 */
interface UiEffect

/**
 * Base interface for ViewModels following the MVI architecture.
 */
interface MviViewModel<S : UiState, I : UiIntent, E : UiEffect> {
    val state: StateFlow<S>
    val effect: SharedFlow<E>

    /**
     * Sends an intent to be processed by the ViewModel.
     */
    fun sendIntent(intent: I)
}
