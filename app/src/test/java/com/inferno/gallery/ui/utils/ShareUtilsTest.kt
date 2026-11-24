package com.inferno.gallery.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareUtilsTest {

    @Test
    fun normalizeMimeTypeLowercasesAndStripsParameters() {
        assertEquals("image/jpeg", ShareUtils.normalizeMimeType("IMAGE/JPEG"))
        assertEquals("image/png", ShareUtils.normalizeMimeType(" image/PNG; charset=utf-8 "))
        assertEquals("video/mp4", ShareUtils.normalizeMimeType("video/mp4"))
        assertEquals("image/*", ShareUtils.normalizeMimeType(null))
        assertEquals("image/*", ShareUtils.normalizeMimeType("invalid"))
        assertEquals("image/*", ShareUtils.normalizeMimeType("/jpeg"))
        assertEquals("image/*", ShareUtils.normalizeMimeType("image/"))
    }

    @Test
    fun computeSharedMimeTypeHandlesSingleHomogeneousAndMixedMedia() {
        assertEquals("image/jpeg", ShareUtils.computeSharedMimeType(listOf("IMAGE/JPEG")))
        assertEquals(
            "image/jpeg",
            ShareUtils.computeSharedMimeType(listOf("image/jpeg", "IMAGE/JPEG"))
        )
        assertEquals(
            "image/*",
            ShareUtils.computeSharedMimeType(listOf("image/jpeg", "image/png"))
        )
        assertEquals(
            "video/*",
            ShareUtils.computeSharedMimeType(listOf("video/mp4", "video/webm"))
        )
        assertEquals(
            "*/*",
            ShareUtils.computeSharedMimeType(listOf("image/jpeg", "video/mp4"))
        )
        assertEquals("image/*", ShareUtils.computeSharedMimeType(emptyList()))
    }

    @Test
    fun isOwnFileProviderUriOnlyMatchesAppFileProviderAuthority() {
        assertTrue(
            ShareUtils.isOwnFileProviderUri(
                uriAuthority = "com.inferno.gallery.fileprovider",
                packageName = "com.inferno.gallery"
            )
        )
        assertTrue(
            ShareUtils.isOwnFileProviderUri(
                uriAuthority = "com.inferno.gallery.debug.fileprovider",
                packageName = "com.inferno.gallery.debug"
            )
        )
        assertFalse(
            ShareUtils.isOwnFileProviderUri(
                uriAuthority = "media",
                packageName = "com.inferno.gallery"
            )
        )
        assertFalse(
            ShareUtils.isOwnFileProviderUri(
                uriAuthority = null,
                packageName = "com.inferno.gallery"
            )
        )
    }
}
