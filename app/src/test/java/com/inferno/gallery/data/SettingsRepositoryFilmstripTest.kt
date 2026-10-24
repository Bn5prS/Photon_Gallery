package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsRepositoryFilmstripTest {
    @Test
    fun testViewerFilmstripKeyExists() {
        assertNotNull(SettingsRepository.VIEWER_FILMSTRIP_ENABLED)
        assertEquals("viewer_filmstrip_enabled", SettingsRepository.VIEWER_FILMSTRIP_ENABLED.name)
    }
}
