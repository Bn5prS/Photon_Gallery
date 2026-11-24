package com.inferno.gallery.data.decoder

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawImageDecoderTest {

    @Test
    fun shouldApplyExifOrientation_rotatesUnrotatedLandscapeBufferForPortraitDng() {
        val portrait90 = RawOrientationMetadata(
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_90,
            rawWidth = 4608,
            rawHeight = 3456
        )
        // IFD0 preview / SkRawCodec unrotated buffer (1280x960 or 3840x2880)
        assertTrue(RawImageHelper.shouldApplyExifOrientation(1280, 960, portrait90))
        assertTrue(RawImageHelper.shouldApplyExifOrientation(3840, 2880, portrait90))

        val portrait270 = portrait90.copy(exifOrientation = ExifInterface.ORIENTATION_ROTATE_270)
        assertTrue(RawImageHelper.shouldApplyExifOrientation(1280, 960, portrait270))
    }

    @Test
    fun shouldApplyExifOrientation_preventsDoubleRotationWhenCodecAlreadySwappedAxes() {
        val portrait90 = RawOrientationMetadata(
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_90,
            rawWidth = 4608,
            rawHeight = 3456
        )
        // Codec already returned portrait pixels (960x1280 / 2880x3840) -> do NOT rotate again
        assertFalse(RawImageHelper.shouldApplyExifOrientation(960, 1280, portrait90))
        assertFalse(RawImageHelper.shouldApplyExifOrientation(2880, 3840, portrait90))
    }

    @Test
    fun shouldApplyExifOrientation_handlesNormalAnd180Degrees() {
        val normal = RawOrientationMetadata(
            exifOrientation = ExifInterface.ORIENTATION_NORMAL,
            rawWidth = 4608,
            rawHeight = 3456
        )
        assertFalse(RawImageHelper.shouldApplyExifOrientation(1280, 960, normal))

        val flipped180 = RawOrientationMetadata(
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_180,
            rawWidth = 4608,
            rawHeight = 3456
        )
        assertTrue(RawImageHelper.shouldApplyExifOrientation(1280, 960, flipped180))
    }

    @Test
    fun degreesToExifOrientation_mapsCommonAnglesCorrectly() {
        assertEquals(ExifInterface.ORIENTATION_NORMAL, RawImageHelper.degreesToExifOrientation(0))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, RawImageHelper.degreesToExifOrientation(90))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_180, RawImageHelper.degreesToExifOrientation(180))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_270, RawImageHelper.degreesToExifOrientation(270))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_270, RawImageHelper.degreesToExifOrientation(-90))
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, RawImageHelper.degreesToExifOrientation(450))
    }

    @Test
    fun rawExtensionAndMimeChecks_matchCameraRawsAndExcludeStandardFormats() {
        assertTrue(RawImageHelper.isRawFilename("/storage/emulated/0/Pictures/Raw/SGCAM_001.dng"))
        assertTrue(RawImageHelper.isRawFilename("IMG_1234.CR3"))
        assertTrue(RawImageHelper.isRawFilename("DSC_9001.NEF"))
        assertTrue(RawImageHelper.isRawFilename("frame.arw"))
        assertTrue(RawImageHelper.isRawFilename("sample.rw2"))
        assertFalse(RawImageHelper.isRawFilename("IMG_001.jpg"))
        assertFalse(RawImageHelper.isRawFilename("IMG_001.heic"))

        assertTrue(RawImageHelper.isRawMimeType("image/x-adobe-dng"))
        assertTrue(RawImageHelper.isRawMimeType("IMAGE/X-CANON-CR2"))
        assertFalse(RawImageHelper.isRawMimeType("image/x-ms-bmp"))
        assertFalse(RawImageHelper.isRawMimeType("image/x-icon"))
        assertFalse(RawImageHelper.isRawMimeType("image/jpeg"))
    }
}
