package com.inferno.gallery.ui.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrimMathTest {

    @Test
    fun clampRange_enforcesBoundsAndMinGap() {
        val (s1, e1) = VideoTrimMath.clampRange(-500L, 15_000L, 10_000L)
        assertEquals(0L, s1)
        assertEquals(10_000L, e1)

        val (s2, e2) = VideoTrimMath.clampRange(5_000L, 5_100L, 10_000L)
        assertEquals(5_000L, s2)
        assertEquals(5_500L, e2)

        // Short video shorter than default 500ms min duration
        val (s3, e3) = VideoTrimMath.clampRange(0L, 300L, 300L)
        assertEquals(0L, s3)
        assertEquals(300L, e3)
    }

    @Test
    fun dragHandles_neverCrossAndRespectMinDuration() {
        val start = VideoTrimMath.dragStartHandle(
            newStartMs = 9_800L,
            currentEndMs = 10_000L,
            totalDurationMs = 10_000L
        )
        assertEquals(9_500L, start)

        val end = VideoTrimMath.dragEndHandle(
            currentStartMs = 4_000L,
            newEndMs = 3_200L,
            totalDurationMs = 10_000L
        )
        assertEquals(4_500L, end)
    }

    @Test
    fun estimateTrimmedSizeBytes_scalesProportionally() {
        assertEquals(5_000_000L, VideoTrimMath.estimateTrimmedSizeBytes(10_000_000L, 5_000L, 10_000L))
        assertEquals(0L, VideoTrimMath.estimateTrimmedSizeBytes(0L, 5_000L, 10_000L))
        assertEquals(0L, VideoTrimMath.estimateTrimmedSizeBytes(10_000_000L, 5_000L, 0L))
    }

    @Test
    fun computeOutputDurationMs_scalesWithSpeed() {
        assertEquals(10_000L, VideoTrimMath.computeOutputDurationMs(10_000L, 1.0f))
        assertEquals(5_000L, VideoTrimMath.computeOutputDurationMs(10_000L, 2.0f))
        assertEquals(20_000L, VideoTrimMath.computeOutputDurationMs(10_000L, 0.5f))
        assertEquals(0L, VideoTrimMath.computeOutputDurationMs(0L, 1.5f))
    }

    @Test
    fun computeTargetResolution_handlesPortraitLandscapeRotationAndNoUpscale() {
        // 1080x1920 portrait -> 720p scales short side to 720
        assertEquals(
            720 to 1280,
            VideoTrimMath.computeTargetResolution(1080, 1920, 0, VideoExportResolution.P720)
        )
        // 1080x1920 rotated 90° -> landscape 1920x1080 -> 720p becomes 1280x720
        assertEquals(
            1280 to 720,
            VideoTrimMath.computeTargetResolution(1080, 1920, 90, VideoExportResolution.P720)
        )
        // Never upscales 720p source to 1080p or 720p
        assertNull(VideoTrimMath.computeTargetResolution(720, 1280, 0, VideoExportResolution.P1080))
        assertNull(VideoTrimMath.computeTargetResolution(720, 1280, 0, VideoExportResolution.P720))
        assertEquals(
            480 to 852,
            VideoTrimMath.computeTargetResolution(720, 1280, 0, VideoExportResolution.P480)
        )
        // Original returns null
        assertNull(VideoTrimMath.computeTargetResolution(2160, 3840, 0, VideoExportResolution.ORIGINAL))
    }

    @Test
    fun toMedia3RotationCcw_mapsClockwiseAnglesCorrectly() {
        assertEquals(0f, VideoTrimMath.toMedia3RotationCcw(0), 0.001f)
        assertEquals(270f, VideoTrimMath.toMedia3RotationCcw(90), 0.001f)
        assertEquals(180f, VideoTrimMath.toMedia3RotationCcw(180), 0.001f)
        assertEquals(90f, VideoTrimMath.toMedia3RotationCcw(270), 0.001f)
        assertEquals(0f, VideoTrimMath.toMedia3RotationCcw(360), 0.001f)
    }

    @Test
    fun estimateEditedSizeBytes_reflectsSpeedResolutionAndMute() {
        val base = VideoTrimMath.estimateEditedSizeBytes(
            originalSizeBytes = 20_000_000L,
            trimmedDurationMs = 10_000L,
            totalDurationMs = 10_000L,
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        val fast = VideoTrimMath.estimateEditedSizeBytes(
            originalSizeBytes = 20_000_000L,
            trimmedDurationMs = 10_000L,
            totalDurationMs = 10_000L,
            speed = 2.0f,
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        val downscaled = VideoTrimMath.estimateEditedSizeBytes(
            originalSizeBytes = 20_000_000L,
            trimmedDurationMs = 10_000L,
            totalDurationMs = 10_000L,
            sourceWidth = 1080,
            sourceHeight = 1920,
            resolution = VideoExportResolution.P720
        )
        val muted = VideoTrimMath.estimateEditedSizeBytes(
            originalSizeBytes = 20_000_000L,
            trimmedDurationMs = 10_000L,
            totalDurationMs = 10_000L,
            sourceWidth = 1080,
            sourceHeight = 1920,
            muteAudio = true
        )
        assertEquals(20_000_000L, base)
        assertEquals(10_000_000L, fast)
        assertTrue(downscaled in 8_000_000L..14_000_000L)
        assertTrue(muted < base)
    }

    @Test
    fun buildTrimmedDisplayName_sanitizesAndAvoidsDoubleSuffix() {
        assertEquals(
            "VID_2026_trimmed_12345.mp4",
            VideoTrimMath.buildTrimmedDisplayName("VID_2026.mp4", timestampMs = 12345L)
        )
        assertEquals(
            "clip_trimmed_99.mp4",
            VideoTrimMath.buildTrimmedDisplayName("clip_trimmed.mov", timestampMs = 99L)
        )
        assertEquals(
            "VID_TRIM_55.mp4",
            VideoTrimMath.buildTrimmedDisplayName(null, timestampMs = 55L)
        )
    }

    @Test
    fun formatTrimTime_formatsSubMinuteAndHoursCleanly() {
        assertEquals("00:00.0", VideoTrimMath.formatTrimTime(0L))
        assertEquals("00:08.4", VideoTrimMath.formatTrimTime(8_450L))
        assertEquals("01:05.2", VideoTrimMath.formatTrimTime(65_200L))
        assertEquals("1:01:02.9", VideoTrimMath.formatTrimTime(3_662_900L))
    }

    @Test
    fun isTrimModified_detectsStartEndAndMuteChanges() {
        assertFalse(VideoTrimMath.isTrimModified(0L, 10_000L, 10_000L, muteAudio = false))
        assertTrue(VideoTrimMath.isTrimModified(200L, 10_000L, 10_000L, muteAudio = false))
        assertTrue(VideoTrimMath.isTrimModified(0L, 9_500L, 10_000L, muteAudio = false))
        assertTrue(VideoTrimMath.isTrimModified(0L, 10_000L, 10_000L, muteAudio = true))
    }

    @Test
    fun isEditModified_detectsSpeedRotationFlipAndDownscale() {
        assertFalse(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                sourceWidth = 1080, sourceHeight = 1920
            )
        )
        assertTrue(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                speed = 1.5f
            )
        )
        assertTrue(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                rotationDegreesCw = 90
            )
        )
        assertTrue(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                flipHorizontal = true
            )
        )
        assertTrue(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                resolution = VideoExportResolution.P720,
                sourceWidth = 1080, sourceHeight = 1920
            )
        )
        // Selecting 1080p on a 720p video is a no-op so should not count as modified
        assertFalse(
            VideoTrimMath.isEditModified(
                startMs = 0L, endMs = 10_000L, totalDurationMs = 10_000L, muteAudio = false,
                resolution = VideoExportResolution.P1080,
                sourceWidth = 720, sourceHeight = 1280
            )
        )
    }
}
