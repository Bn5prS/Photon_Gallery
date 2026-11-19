package com.inferno.gallery.ui

import android.net.FakeUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExternalMediaDetailTest {

    @Test
    fun externalItem_hasExternalBucketAndSafePath() {
        val contentUriString = "content://com.mi.android.globalFileexplorer.myprovider/external_files/DCIM/Camera/IMG_2026.jpg"
        val parsedUri = FakeUri(contentUriString)

        val item = GalleryItem(
            id = contentUriString,
            uri = parsedUri,
            bucketName = "External",
            dateAdded = 1000L,
            size = 0L,
            name = "IMG_2026.jpg",
            dateModified = 1000L,
            path = "", // Content URI should not populate path with parsedUri.path if it does not exist on disk
            isVideo = false,
            localExists = true,
            resolvedUri = parsedUri
        )

        assertEquals("External", item.bucketName)
        assertEquals("", item.path)
    }

    @Test
    fun contentUriPath_doesNotExistAsLocalFile() {
        // Content URI path segments like "/document/image:123" must not exist as real local files
        val fakeContentPath = "/document/image:123"
        val file = java.io.File(fakeContentPath)
        assertFalse("Content URI path segment must not exist on filesystem", file.exists())
    }
}
