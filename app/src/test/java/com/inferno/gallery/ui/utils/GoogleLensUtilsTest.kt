package com.inferno.gallery.ui.utils

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GoogleLensUtilsTest {

    @Test
    fun testConstants_areCorrectPackages() {
        assertEquals("com.google.ar.lens", GoogleLensUtils.LENS_PACKAGE)
        assertEquals("com.google.android.googlequicksearchbox", GoogleLensUtils.GOOGLE_APP_PACKAGE)
    }

    @Test
    fun testCreateLensIntent_returnsNonNullIntent() {
        val fakeUri = FakeUri("content://media/external/images/media/42")
        val intent = GoogleLensUtils.createLensIntent(fakeUri)
        assertNotNull(intent)
    }

    @Test
    fun testCreateGoogleAppIntent_returnsNonNullIntent() {
        val fakeUri = FakeUri("content://media/external/images/media/42")
        val intent = GoogleLensUtils.createGoogleAppIntent(fakeUri)
        assertNotNull(intent)
    }
}
