package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsRepositoryZoomMinimapTest {
    @Test
    fun testViewerZoomMinimapKeyExists() {
        assertNotNull(SettingsRepository.VIEWER_ZOOM_MINIMAP_ENABLED)
        assertEquals("viewer_zoom_minimap_enabled", SettingsRepository.VIEWER_ZOOM_MINIMAP_ENABLED.name)
    }
}
