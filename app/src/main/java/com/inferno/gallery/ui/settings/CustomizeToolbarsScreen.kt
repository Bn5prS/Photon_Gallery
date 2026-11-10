package com.inferno.gallery.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.inferno.gallery.R
import com.inferno.gallery.ui.components.DragActionType
import com.inferno.gallery.ui.components.DragDropTargetZone
import com.inferno.gallery.ui.components.ExpressiveButton
import com.inferno.gallery.ui.components.PhotonBackButton
import com.inferno.gallery.ui.components.rememberToolbarDragDropState
import com.inferno.gallery.ui.model.SelectionDockAction
import com.inferno.gallery.ui.model.ViewerToolbarAction
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeExtraLarge
import com.inferno.gallery.ui.theme.ShapeMedium
import kotlin.math.roundToInt

enum class ToolbarCustomizationTarget {
    VIEWER,
    SELECTION_DOCK
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun CustomizeToolbarsScreen(
    onBackClick: () -> Unit,
    settingsViewModel: com.inferno.gallery.ui.SettingsViewModel,
    initialTarget: ToolbarCustomizationTarget = ToolbarCustomizationTarget.VIEWER,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var selectedTarget by remember { mutableStateOf(initialTarget) }

    val viewerActiveIds by settingsViewModel.viewerToolbarActions.collectAsState()
    val selectionActiveIds by settingsViewModel.selectionDockActions.collectAsState()

    val currentActiveIds = if (selectedTarget == ToolbarCustomizationTarget.VIEWER) viewerActiveIds else selectionActiveIds
    val currentAvailableIds = remember(selectedTarget, currentActiveIds) {
        if (selectedTarget == ToolbarCustomizationTarget.VIEWER) {
            ViewerToolbarAction.ALL.map { it.id }.filter { it !in currentActiveIds }
        } else {
            SelectionDockAction.ALL.map { it.id }.filter { it !in currentActiveIds }
        }
    }

    val dragDropState = rememberToolbarDragDropState(minSlots = 2, maxSlots = 6)

    var toolbarBounds by remember { mutableStateOf<Rect?>(null) }
    var shelfBounds by remember { mutableStateOf<Rect?>(null) }
    val slotCenters = remember { mutableStateMapOf<Int, Float>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhotonBackButton(onClick = onBackClick)
            Column {
                Text(
                    text = stringResource(R.string.customize_toolbars_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.customize_toolbars_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Target Switcher ────────────────────────────────
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = {
                    selectedTarget = ToolbarCustomizationTarget.VIEWER
                    dragDropState.reset()
                },
                selected = selectedTarget == ToolbarCustomizationTarget.VIEWER,
                label = { Text(stringResource(R.string.toolbar_target_viewer)) }
            )
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = {
                    selectedTarget = ToolbarCustomizationTarget.SELECTION_DOCK
                    dragDropState.reset()
                },
                selected = selectedTarget == ToolbarCustomizationTarget.SELECTION_DOCK,
                label = { Text(stringResource(R.string.toolbar_target_selection)) }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Live Interactive Preview Area ───────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (dragDropState.hoverTargetZone == DragDropTargetZone.TOOLBAR)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.toolbar_live_preview),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentActiveIds.size}/6 tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = stringResource(R.string.toolbar_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 20.dp)
                    )

                    // The actual live pill mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .onGloballyPositioned { coordinates ->
                                toolbarBounds = coordinates.boundsInRoot()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedTarget == ToolbarCustomizationTarget.VIEWER) {
                            // Viewer Stadium Capsule
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 2.dp,
                                shadowElevation = 4.dp,
                                modifier = Modifier.height(64.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    currentActiveIds.forEachIndexed { index, id ->
                                        val action = ViewerToolbarAction.fromId(id) ?: return@forEachIndexed
                                        val isBeingDragged = dragDropState.isDragging &&
                                                dragDropState.dragSourceZone == DragDropTargetZone.TOOLBAR &&
                                                dragDropState.draggedId == id

                                        val isSlotHovered = dragDropState.isDragging &&
                                                dragDropState.hoverTargetZone == DragDropTargetZone.TOOLBAR &&
                                                dragDropState.hoverSlotIndex == index

                                        val slotScale by animateFloatAsState(
                                            targetValue = when {
                                                isBeingDragged -> 1.18f
                                                isSlotHovered -> 1.08f
                                                else -> 1f
                                            },
                                            animationSpec = MotionTokens.bouncySpring(),
                                            label = "viewerSlotScale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .onGloballyPositioned { coords ->
                                                    val bounds = coords.boundsInRoot()
                                                    toolbarBounds?.let { tb ->
                                                        slotCenters[index] = bounds.center.x - tb.left
                                                    }
                                                }
                                                .zIndex(if (isBeingDragged) 10f else 1f)
                                                .graphicsLayer {
                                                    scaleX = slotScale
                                                    scaleY = slotScale
                                                    if (isBeingDragged) {
                                                        translationX = dragDropState.dragOffset.x
                                                        translationY = dragDropState.dragOffset.y
                                                    }
                                                }
                                                .pointerInput(id, index) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            dragDropState.startDrag(
                                                                id = id,
                                                                sourceZone = DragDropTargetZone.TOOLBAR,
                                                                sourceIndex = index,
                                                                initialGlobalOffset = offset
                                                            )
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val sortedCenters = slotCenters.entries.sortedBy { it.key }.map { it.value }
                                                            dragDropState.updateDrag(
                                                                dragDelta = dragAmount,
                                                                toolbarBounds = toolbarBounds,
                                                                shelfBounds = shelfBounds,
                                                                slotCenters = sortedCenters
                                                            )
                                                        },
                                                        onDragEnd = {
                                                            val result = dragDropState.endDrag(
                                                                currentActive = currentActiveIds,
                                                                currentAvailable = currentAvailableIds
                                                            )
                                                            if (result.actionTaken != DragActionType.REJECTED) {
                                                                settingsViewModel.updateViewerToolbarActions(result.updatedActive)
                                                            } else if (currentActiveIds.size <= 2 && dragDropState.hoverTargetZone == DragDropTargetZone.SHELF) {
                                                                Toast.makeText(context, context.getString(R.string.toolbar_min_limit_toast), Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        onDragCancel = { dragDropState.reset() }
                                                    )
                                                }
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSlotHovered && !isBeingDragged)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else
                                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(action.iconRes),
                                                contentDescription = stringResource(action.labelRes),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Selection Dock Rounded Container
                            Surface(
                                shape = ShapeExtraLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    currentActiveIds.forEachIndexed { index, id ->
                                        val action = SelectionDockAction.fromId(id) ?: return@forEachIndexed
                                        val isBeingDragged = dragDropState.isDragging &&
                                                dragDropState.dragSourceZone == DragDropTargetZone.TOOLBAR &&
                                                dragDropState.draggedId == id

                                        val isSlotHovered = dragDropState.isDragging &&
                                                dragDropState.hoverTargetZone == DragDropTargetZone.TOOLBAR &&
                                                dragDropState.hoverSlotIndex == index

                                        val slotScale by animateFloatAsState(
                                            targetValue = when {
                                                isBeingDragged -> 1.18f
                                                isSlotHovered -> 1.08f
                                                else -> 1f
                                            },
                                            animationSpec = MotionTokens.bouncySpring(),
                                            label = "dockSlotScale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .onGloballyPositioned { coords ->
                                                    val bounds = coords.boundsInRoot()
                                                    toolbarBounds?.let { tb ->
                                                        slotCenters[index] = bounds.center.x - tb.left
                                                    }
                                                }
                                                .zIndex(if (isBeingDragged) 10f else 1f)
                                                .graphicsLayer {
                                                    scaleX = slotScale
                                                    scaleY = slotScale
                                                    if (isBeingDragged) {
                                                        translationX = dragDropState.dragOffset.x
                                                        translationY = dragDropState.dragOffset.y
                                                    }
                                                }
                                                .pointerInput(id, index) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            dragDropState.startDrag(
                                                                id = id,
                                                                sourceZone = DragDropTargetZone.TOOLBAR,
                                                                sourceIndex = index,
                                                                initialGlobalOffset = offset
                                                            )
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val sortedCenters = slotCenters.entries.sortedBy { it.key }.map { it.value }
                                                            dragDropState.updateDrag(
                                                                dragDelta = dragAmount,
                                                                toolbarBounds = toolbarBounds,
                                                                shelfBounds = shelfBounds,
                                                                slotCenters = sortedCenters
                                                            )
                                                        },
                                                        onDragEnd = {
                                                            val result = dragDropState.endDrag(
                                                                currentActive = currentActiveIds,
                                                                currentAvailable = currentAvailableIds
                                                            )
                                                            if (result.actionTaken != DragActionType.REJECTED) {
                                                                settingsViewModel.updateSelectionDockActions(result.updatedActive)
                                                            } else if (currentActiveIds.size <= 2 && dragDropState.hoverTargetZone == DragDropTargetZone.SHELF) {
                                                                Toast.makeText(context, context.getString(R.string.toolbar_min_limit_toast), Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        onDragCancel = { dragDropState.reset() }
                                                    )
                                                }
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSlotHovered && !isBeingDragged)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else
                                                        Color.Transparent
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(action.iconRes),
                                                contentDescription = stringResource(action.labelRes),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Available Tools Shelf (Drop zone to remove) ─────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (dragDropState.hoverTargetZone == DragDropTargetZone.SHELF)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        shelfBounds = coordinates.boundsInRoot()
                    }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.toolbar_available_shelf),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.toolbar_shelf_drop_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (currentAvailableIds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.toolbar_all_tools_active),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentAvailableIds.forEachIndexed { index, id ->
                                val (iconRes, labelRes) = if (selectedTarget == ToolbarCustomizationTarget.VIEWER) {
                                    val action = ViewerToolbarAction.fromId(id) ?: return@forEachIndexed
                                    action.iconRes to action.labelRes
                                } else {
                                    val action = SelectionDockAction.fromId(id) ?: return@forEachIndexed
                                    action.iconRes to action.labelRes
                                }

                                val isBeingDragged = dragDropState.isDragging &&
                                        dragDropState.dragSourceZone == DragDropTargetZone.SHELF &&
                                        dragDropState.draggedId == id

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .zIndex(if (isBeingDragged) 10f else 1f)
                                        .graphicsLayer {
                                            if (isBeingDragged) {
                                                scaleX = 1.15f
                                                scaleY = 1.15f
                                                translationX = dragDropState.dragOffset.x
                                                translationY = dragDropState.dragOffset.y
                                            }
                                        }
                                        .pointerInput(id, index) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    dragDropState.startDrag(
                                                        id = id,
                                                        sourceZone = DragDropTargetZone.SHELF,
                                                        sourceIndex = index,
                                                        initialGlobalOffset = offset
                                                    )
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val sortedCenters = slotCenters.entries.sortedBy { it.key }.map { it.value }
                                                    dragDropState.updateDrag(
                                                        dragDelta = dragAmount,
                                                        toolbarBounds = toolbarBounds,
                                                        shelfBounds = shelfBounds,
                                                        slotCenters = sortedCenters
                                                    )
                                                },
                                                onDragEnd = {
                                                    val result = dragDropState.endDrag(
                                                        currentActive = currentActiveIds,
                                                        currentAvailable = currentAvailableIds
                                                    )
                                                    if (result.actionTaken != DragActionType.REJECTED) {
                                                        if (selectedTarget == ToolbarCustomizationTarget.VIEWER) {
                                                            settingsViewModel.updateViewerToolbarActions(result.updatedActive)
                                                        } else {
                                                            settingsViewModel.updateSelectionDockActions(result.updatedActive)
                                                        }
                                                    }
                                                },
                                                onDragCancel = { dragDropState.reset() }
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(iconRes),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(labelRes),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Reset to Defaults ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ExpressiveButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selectedTarget == ToolbarCustomizationTarget.VIEWER) {
                            settingsViewModel.resetViewerToolbarActions()
                        } else {
                            settingsViewModel.resetSelectionDockActions()
                        }
                        Toast.makeText(context, context.getString(R.string.toolbar_reset_toast), Toast.LENGTH_SHORT).show()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.toolbar_reset_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
