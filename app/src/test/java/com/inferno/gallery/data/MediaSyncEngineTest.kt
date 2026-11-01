package com.inferno.gallery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSyncEngineTest {

    @Test
    fun testIsMediaFile_supportedImageExtensions() {
        assertTrue(MediaSyncEngine.isMediaFile("photo.jpg"))
        assertTrue(MediaSyncEngine.isMediaFile("photo.JPEG"))
        assertTrue(MediaSyncEngine.isMediaFile("image.png"))
        assertTrue(MediaSyncEngine.isMediaFile("IMG_2026.PNG"))
        assertTrue(MediaSyncEngine.isMediaFile("graphic.webp"))
        assertTrue(MediaSyncEngine.isMediaFile("raw_shot.DNG"))
        assertTrue(MediaSyncEngine.isMediaFile("apple.HEIC"))
        assertTrue(MediaSyncEngine.isMediaFile("burst.heif"))
        assertTrue(MediaSyncEngine.isMediaFile("animated.gif"))
        assertTrue(MediaSyncEngine.isMediaFile("bitmap.bmp"))
        assertTrue(MediaSyncEngine.isMediaFile("nextgen.avif"))
    }

    @Test
    fun testIsMediaFile_supportedVideoExtensions() {
        assertTrue(MediaSyncEngine.isMediaFile("clip.mp4"))
        assertTrue(MediaSyncEngine.isMediaFile("RECORDING.MP4"))
        assertTrue(MediaSyncEngine.isMediaFile("movie.mkv"))
        assertTrue(MediaSyncEngine.isMediaFile("video.mov"))
        assertTrue(MediaSyncEngine.isMediaFile("stream.webm"))
        assertTrue(MediaSyncEngine.isMediaFile("legacy.3gp"))
        assertTrue(MediaSyncEngine.isMediaFile("capture.ts"))
    }

    @Test
    fun testIsMediaFile_unsupportedExtensions() {
        assertFalse(MediaSyncEngine.isMediaFile("document.pdf"))
        assertFalse(MediaSyncEngine.isMediaFile("archive.zip"))
        assertFalse(MediaSyncEngine.isMediaFile("application.apk"))
        assertFalse(MediaSyncEngine.isMediaFile("notes.txt"))
        assertFalse(MediaSyncEngine.isMediaFile(".nomedia"))
        assertFalse(MediaSyncEngine.isMediaFile("no_extension"))
        assertFalse(MediaSyncEngine.isMediaFile("empty_extension."))
    }
}

