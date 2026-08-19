package com.inferno.gallery.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun rememberReorderableLazyGridState(
    lazyGridState: LazyGridState,
    onMove: (fromKey: Any, toKey: Any) -> Unit,
    onDragEnd: () -> Unit
): ReorderableLazyGridState {
    return remember(lazyGridState, onMove, onDragEnd) {
        ReorderableLazyGridState(lazyGridState, onMove, onDragEnd)
    }
}

class ReorderableLazyGridState(
    val lazyGridState: LazyGridState,
    val onMove: (fromKey: Any, toKey: Any) -> Unit,
    val dragEnd: () -> Unit
) {
    var draggedKey by mutableStateOf<Any?>(null)
    var currentPosition by mutableStateOf<Offset?>(null)

    fun onDragStart(key: Any) {
        val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        if (item != null) {
            draggedKey = item.key
            currentPosition = Offset(
                item.offset.x.toFloat() + item.size.width / 2f,
                item.offset.y.toFloat() + item.size.height / 2f
            )
        }
    }

    fun onDrag(dragAmount: Offset) {
        val currentPos = currentPosition ?: return
        val newPos = currentPos + dragAmount
        currentPosition = newPos

        val currentKey = draggedKey ?: return

        val hoveredItem = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull {
            newPos.x >= it.offset.x && newPos.x <= it.offset.x + it.size.width &&
            newPos.y >= it.offset.y && newPos.y <= it.offset.y + it.size.height
        }

        if (hoveredItem != null && hoveredItem.key != currentKey) {
            onMove(currentKey, hoveredItem.key)
            // Wait, we shouldn't necessarily change draggedKey. The item itself moves to the hoveredItem's position.
            // The dragged item's key remains the same!
            // Yes, draggedKey should remain constant throughout the drag.
        }
    }

    fun onDragEnd() {
        draggedKey = null
        currentPosition = null
        dragEnd()
    }
}

fun Modifier.reorderableItem(state: ReorderableLazyGridState, key: Any): Modifier = this.pointerInput(key) {
    detectDragGesturesAfterLongPress(
        onDragStart = { _ ->
            state.onDragStart(key)
        },
        onDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount)
        },
        onDragEnd = {
            state.onDragEnd()
        },
        onDragCancel = {
            state.onDragEnd()
        }
    )
}
