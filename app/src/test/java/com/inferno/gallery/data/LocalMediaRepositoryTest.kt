package com.inferno.gallery.data

import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMediaRepositoryTest {

    @Test
    fun `explicit mediaType image and video are respected`() {
        assertEquals(
            false,
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                null,
                "IMG_001.jpg",
                "/storage/emulated/0/DCIM/Camera/.trashed-123-IMG_001.jpg"
            )
        )
        assertEquals(
            true,
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                null,
                "VID_001.mp4",
                "/storage/emulated/0/DCIM/Camera/.trashed-123-VID_001.mp4"
            )
        )
    }

    @Test
    fun `trashed video with reset mediaType 0 is still classified as video`() {
        assertEquals(
            true,
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                "video/mp4",
                ".trashed-1731690000-VID_001.mp4",
                "/storage/emulated/0/DCIM/Camera/.trashed-1731690000-VID_001.mp4"
            )
        )
        assertEquals(
            true,
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                null,
                ".trashed-1731690000-clip.mkv",
                ""
            )
        )
    }

    @Test
    fun `trashed image with reset mediaType 0 is classified as image`() {
        assertEquals(
            false,
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                null,
                ".trashed-1731690000-photo.heic",
                ""
            )
        )
    }

    @Test
    fun `trashed non-media files are excluded`() {
        assertNull(
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                "application/pdf",
                ".trashed-1731690000-invoice.pdf",
                "/storage/emulated/0/Download/.trashed-1731690000-invoice.pdf"
            )
        )
        assertNull(
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO,
                "audio/mpeg",
                ".trashed-1731690000-song.mp3",
                ""
            )
        )
        assertNull(
            LocalMediaRepository.classifyTrashedMedia(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                null,
                "app-release.apk",
                "/storage/emulated/0/Download/app-release.apk"
            )
        )
    }
}
