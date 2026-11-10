package com.inferno.gallery.data

import com.inferno.gallery.ui.model.SelectionDockAction
import com.inferno.gallery.ui.model.ViewerToolbarAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsRepositoryToolbarTest {

    @Test
    fun testViewerToolbarActionDefaultsAndAll() {
        assertEquals(listOf("share", "favorite", "edit", "info", "delete"), ViewerToolbarAction.DEFAULTS)
        assertEquals(10, ViewerToolbarAction.ALL.size)
        assertNotNull(ViewerToolbarAction.fromId("share"))
        assertNotNull(ViewerToolbarAction.fromId("vault"))
        assertNull(ViewerToolbarAction.fromId("unknown_action"))
    }

    @Test
    fun testViewerToolbarActionSanitizeBounds() {
        // Unknown IDs filtered
        val withUnknown = listOf("share", "unknown", "edit", "fake")
        val sanitized = ViewerToolbarAction.sanitize(withUnknown)
        assertEquals(listOf("share", "edit"), sanitized)

        // Under min 2 falls back to defaults
        val tooFew = listOf("share")
        assertEquals(ViewerToolbarAction.DEFAULTS, ViewerToolbarAction.sanitize(tooFew))

        // Empty falls back to defaults
        assertEquals(ViewerToolbarAction.DEFAULTS, ViewerToolbarAction.sanitize(emptyList()))

        // Over max 6 takes first 6
        val tooMany = listOf("share", "favorite", "edit", "info", "delete", "move", "vault", "copy")
        val clamped = ViewerToolbarAction.sanitize(tooMany)
        assertEquals(6, clamped.size)
        assertEquals(listOf("share", "favorite", "edit", "info", "delete", "move"), clamped)
    }

    @Test
    fun testSelectionDockActionDefaultsAndAll() {
        assertEquals(listOf("share", "copy", "move", "create", "delete"), SelectionDockAction.DEFAULTS)
        assertEquals(7, SelectionDockAction.ALL.size)
        assertNotNull(SelectionDockAction.fromId("create"))
        assertNotNull(SelectionDockAction.fromId("selectAll"))
        assertNull(SelectionDockAction.fromId("unknown"))
    }

    @Test
    fun testSelectionDockActionSanitizeBounds() {
        // Unknown filtered
        val withUnknown = listOf("share", "unknown_tool", "copy", "move")
        assertEquals(listOf("share", "copy", "move"), SelectionDockAction.sanitize(withUnknown))

        // Under min 2 falls back to defaults
        assertEquals(SelectionDockAction.DEFAULTS, SelectionDockAction.sanitize(listOf("share")))
        assertEquals(SelectionDockAction.DEFAULTS, SelectionDockAction.sanitize(emptyList()))

        // Clamped to 6
        val tooMany = listOf("share", "copy", "move", "create", "delete", "vault", "selectAll")
        val clamped = SelectionDockAction.sanitize(tooMany)
        assertEquals(6, clamped.size)
    }

    @Test
    fun testDataStorePreferenceKeysExist() {
        assertNotNull(SettingsRepository.VIEWER_TOOLBAR_ACTIONS)
        assertEquals("viewer_toolbar_actions", SettingsRepository.VIEWER_TOOLBAR_ACTIONS.name)
        assertNotNull(SettingsRepository.SELECTION_DOCK_ACTIONS)
        assertEquals("selection_dock_actions", SettingsRepository.SELECTION_DOCK_ACTIONS.name)
    }
}
