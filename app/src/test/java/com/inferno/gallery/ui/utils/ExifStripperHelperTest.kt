package com.inferno.gallery.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ExifStripperHelperTest {
    @Test
    fun testGenerateCleanFilename() {
        assertEquals("photo_no_exif.jpg", ExifStripperHelper.generateCleanFilename("photo.jpg"))
        assertEquals("IMG_20261111_no_exif.png", ExifStripperHelper.generateCleanFilename("IMG_20261111.png"))
        assertEquals("image_no_exif.jpeg", ExifStripperHelper.generateCleanFilename("image.jpeg"))
        assertEquals("untitled_no_exif.jpg", ExifStripperHelper.generateCleanFilename("untitled"))
        assertEquals("my.cool.photo_no_exif.webp", ExifStripperHelper.generateCleanFilename("my.cool.photo.webp"))
    }
}

