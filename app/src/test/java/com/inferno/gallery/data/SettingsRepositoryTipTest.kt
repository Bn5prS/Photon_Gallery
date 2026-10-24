package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsRepositoryTipTest {

    @Test
    fun testTipKeyConstantsDefined() {
        assertEquals("tip_albums_customize", SettingsRepository.TIP_ALBUMS_CUSTOMIZE)
        assertEquals("tip_people_cover", SettingsRepository.TIP_PEOPLE_COVER)
        assertEquals("tip_motion_photo", SettingsRepository.TIP_MOTION_PHOTO)
        assertEquals("tip_markup_gestures", SettingsRepository.TIP_MARKUP_GESTURES)
    }

    @Test
    fun testAllTipKeysAreUnique() {
        val keys = listOf(
            SettingsRepository.TIP_ALBUMS_CUSTOMIZE,
            SettingsRepository.TIP_PEOPLE_COVER,
            SettingsRepository.TIP_MOTION_PHOTO,
            SettingsRepository.TIP_MARKUP_GESTURES
        )
        assertEquals("All tip keys must be unique", keys.size, keys.toSet().size)
    }
}
