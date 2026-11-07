package com.inferno.gallery.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.inferno.gallery.ui.editor.markup.MarkupPalette
import com.inferno.gallery.ui.editor.markup.MarkupState
import com.inferno.gallery.ui.editor.markup.MarkupStroke
import com.inferno.gallery.ui.editor.markup.MarkupText
import com.inferno.gallery.ui.editor.markup.MarkupTool
import com.inferno.gallery.ui.editor.model.AdjustTool
import com.inferno.gallery.ui.editor.model.AspectRatioOption
import com.inferno.gallery.ui.editor.model.CropState
import com.inferno.gallery.ui.editor.model.EditorHistoryState
import com.inferno.gallery.ui.editor.model.FilterPreset
import com.inferno.gallery.ui.editor.model.TuneValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotonEditorScreenTest {

    @Test
    fun testDefaultSnapshotIsNotModified() {
        val snapshot = EditorSnapshot()
        assertFalse("Default snapshot should not be modified", snapshot.isModified)
        assertEquals(CropState(), snapshot.cropState)
        assertEquals(TuneValues(), snapshot.tuneValues)
        assertEquals(FilterPreset.ORIGINAL, snapshot.filterPreset)
        assertEquals(100f, snapshot.filterStrength, 0.001f)
        assertTrue(snapshot.markupState.strokes.isEmpty())
        assertTrue(snapshot.markupState.texts.isEmpty())
    }

    @Test
    fun testCropModificationMarksSnapshotAsModified() {
        val snapshot = EditorSnapshot(
            cropState = CropState().rotate90Ccw()
        )
        assertTrue(snapshot.isModified)

        val tiltSnapshot = EditorSnapshot(
            cropState = CropState().withVerticalTilt(15f)
        )
        assertTrue(tiltSnapshot.isModified)

        val aspectSnapshot = EditorSnapshot(
            cropState = CropState(aspectOption = AspectRatioOption.SQUARE)
        )
        assertTrue(aspectSnapshot.isModified)
    }

    @Test
    fun testTuneValuesModificationMarksSnapshotAsModified() {
        val snapshot = EditorSnapshot(
            tuneValues = TuneValues().withValue(AdjustTool.BRIGHTNESS, 25f)
        )
        assertTrue(snapshot.isModified)

        val vignetteSnapshot = EditorSnapshot(
            tuneValues = TuneValues(vignette = 40f)
        )
        assertTrue(vignetteSnapshot.isModified)
    }

    @Test
    fun testFilterModificationMarksSnapshotAsModified() {
        val snapshot = EditorSnapshot(
            filterPreset = FilterPreset.PLAYA
        )
        assertTrue(snapshot.isModified)
    }

    @Test
    fun testMarkupModificationMarksSnapshotAsModified() {
        val stroke = MarkupStroke(
            points = listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.2f)),
            color = Color.White,
            strokeWidth = MarkupPalette.DEFAULT_PEN_WIDTH,
            tool = MarkupTool.PEN
        )
        val strokeSnapshot = EditorSnapshot(
            markupState = MarkupState(strokes = listOf(stroke))
        )
        assertTrue(strokeSnapshot.isModified)

        val text = MarkupText(
            text = "Hello World",
            position = Offset(0.5f, 0.5f),
            color = Color.White
        )
        val textSnapshot = EditorSnapshot(
            markupState = MarkupState(texts = listOf(text))
        )
        assertTrue(textSnapshot.isModified)
    }

    @Test
    fun testEditorHistoryWorkflow() {
        val initial = EditorSnapshot()
        var history = EditorHistoryState(current = initial)

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertFalse(history.current.isModified)

        // Step 1: User rotates 90 degrees CCW
        val step1 = initial.copy(cropState = initial.cropState.rotate90Ccw())
        history = history.push(step1)

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertTrue(history.current.isModified)
        assertEquals(270, history.current.cropState.rotation)

        // Step 2: User applies Playa filter
        val step2 = step1.copy(filterPreset = FilterPreset.PLAYA)
        history = history.push(step2)

        assertTrue(history.canUndo)
        assertEquals(FilterPreset.PLAYA, history.current.filterPreset)

        // Undo filter step
        history = history.undo()
        assertEquals(FilterPreset.ORIGINAL, history.current.filterPreset)
        assertEquals(270, history.current.cropState.rotation)
        assertTrue(history.canRedo)

        // Undo rotate step
        history = history.undo()
        assertEquals(0, history.current.cropState.rotation)
        assertFalse(history.current.isModified)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)

        // Redo rotate step
        history = history.redo()
        assertEquals(270, history.current.cropState.rotation)
        assertTrue(history.canUndo)
        assertTrue(history.canRedo)
    }
}
