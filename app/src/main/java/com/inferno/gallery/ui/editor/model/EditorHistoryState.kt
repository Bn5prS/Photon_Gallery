package com.inferno.gallery.ui.editor.model

/**
 * Generic immutable undo/redo history state container.
 *
 * @param past Stack of past states prior to [current], ordered oldest to newest.
 * @param current The active state.
 * @param future Stack of undone states that can be redone, ordered most recently undone first.
 */
data class EditorHistoryState<T>(
    val past: List<T> = emptyList(),
    val current: T,
    val future: List<T> = emptyList()
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /**
     * Appends [current] to [past], sets [current] to [newState], and invalidates [future].
     */
    fun push(newState: T): EditorHistoryState<T> {
        return EditorHistoryState(
            past = past + current,
            current = newState,
            future = emptyList()
        )
    }

    /**
     * Reverts to the most recent past state, moving [current] onto the [future] stack.
     * Returns this instance if [canUndo] is false.
     */
    fun undo(): EditorHistoryState<T> {
        if (!canUndo) return this
        val newCurrent = past.last()
        val newPast = past.dropLast(1)
        val newFuture = listOf(current) + future
        return EditorHistoryState(
            past = newPast,
            current = newCurrent,
            future = newFuture
        )
    }

    /**
     * Reapplies the most recently undone state, moving [current] onto the [past] stack.
     * Returns this instance if [canRedo] is false.
     */
    fun redo(): EditorHistoryState<T> {
        if (!canRedo) return this
        val newCurrent = future.first()
        val newFuture = future.drop(1)
        val newPast = past + current
        return EditorHistoryState(
            past = newPast,
            current = newCurrent,
            future = newFuture
        )
    }
}
