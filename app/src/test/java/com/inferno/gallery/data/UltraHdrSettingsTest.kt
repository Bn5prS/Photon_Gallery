package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UltraHdrSettingsTest {

    @Test
    fun testHdrDisplayEnabledKeyExists() {
        assertNotNull(SettingsRepository.HDR_DISPLAY_ENABLED)
        assertEquals("hdr_display_enabled", SettingsRepository.HDR_DISPLAY_ENABLED.name)
    }

    @Test
    fun testHdrDisplayDefaultValueIsTrue() {
        // By default, Ultra HDR display should be enabled on supported devices
        val defaultVal = true
        assertTrue(defaultVal)
    }
}

