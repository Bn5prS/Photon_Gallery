package com.inferno.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupAndStartupTabTest {

    @Test
    fun sanitizeStartupTabHandlesAllowedInvalidAndHiddenVideosTab() {
        assertEquals("photos", SettingsRepository.sanitizeStartupTab(null, showVideosInDock = false))
        assertEquals("photos", SettingsRepository.sanitizeStartupTab("unknown", showVideosInDock = true))
        assertEquals("albums", SettingsRepository.sanitizeStartupTab("ALBUMS", showVideosInDock = false))
        assertEquals("collections", SettingsRepository.sanitizeStartupTab(" collections ", showVideosInDock = false))
        assertEquals("videos", SettingsRepository.sanitizeStartupTab("videos", showVideosInDock = true))
        assertEquals("photos", SettingsRepository.sanitizeStartupTab("videos", showVideosInDock = false))
    }

    @Test
    fun backupRoundTripPreservesTypesAndExcludesEphemeralKeys() {
        val sample = linkedMapOf<String, Any?>(
            SettingsRepository.THEME_MODE.name to "DARK",
            SettingsRepository.USE_AMOLED_BLACK.name to true,
            SettingsRepository.GRID_CELLS_COUNT.name to 4,
            SettingsRepository.THUMBNAIL_CORNER_RADIUS.name to 12.5f,
            SettingsRepository.APP_LOCK_TIMEOUT_SECONDS.name to 60L,
            SettingsRepository.DEFAULT_STARTUP_TAB.name to "albums",
            "test_string_set" to setOf("Camera", "Screenshots"),
            SettingsRepository.ONBOARDING_COMPLETED.name to true,
            SettingsRepository.ONBOARDING_V210_COMPLETED.name to true,
            SettingsRepository.APP_LOCK_ENABLED.name to true,
            SettingsRepository.LAST_UPDATE_CHECK_TIMESTAMP.name to 123456789L
        )

        val json = SettingsRepository.encodeSettingsMapToJson(sample, exportedAtMs = 1700000000000L)
        assertFalse(json.contains(SettingsRepository.ONBOARDING_COMPLETED.name))
        assertFalse(json.contains(SettingsRepository.ONBOARDING_V210_COMPLETED.name))
        assertFalse(json.contains(SettingsRepository.APP_LOCK_ENABLED.name))
        assertFalse(json.contains(SettingsRepository.LAST_UPDATE_CHECK_TIMESTAMP.name))

        val decoded = SettingsRepository.decodeSettingsJsonToTypedMap(json)
        assertEquals(7, decoded.size)
        assertEquals(SettingsRepository.TypedSettingValue.StringVal("DARK"), decoded[SettingsRepository.THEME_MODE.name])
        assertEquals(SettingsRepository.TypedSettingValue.BoolVal(true), decoded[SettingsRepository.USE_AMOLED_BLACK.name])
        assertEquals(SettingsRepository.TypedSettingValue.IntVal(4), decoded[SettingsRepository.GRID_CELLS_COUNT.name])
        val floatVal = (decoded[SettingsRepository.THUMBNAIL_CORNER_RADIUS.name] as SettingsRepository.TypedSettingValue.FloatVal).value
        assertEquals(12.5f, floatVal, 0.0001f)
        assertEquals(SettingsRepository.TypedSettingValue.LongVal(60L), decoded[SettingsRepository.APP_LOCK_TIMEOUT_SECONDS.name])
        assertEquals(SettingsRepository.TypedSettingValue.StringVal("albums"), decoded[SettingsRepository.DEFAULT_STARTUP_TAB.name])
        assertEquals(
            SettingsRepository.TypedSettingValue.StringSetVal(setOf("Camera", "Screenshots")),
            decoded["test_string_set"]
        )
    }

    @Test
    fun decodeRejectsMalformedOrNonPhotonGalleryJson() {
        val badJson = try {
            SettingsRepository.decodeSettingsJsonToTypedMap("not json")
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(badJson)

        val wrongApp = try {
            SettingsRepository.decodeSettingsJsonToTypedMap("""{"app":"OtherApp","settings":{}}""")
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(wrongApp)
    }
}
