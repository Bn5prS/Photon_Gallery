package com.inferno.gallery.data.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class MotionPhotoParserTest {

    @Test
    fun testNonMotionJpegReturnsNull() {
        // Plain JPEG with SOI, APP0, and EOI
        val plainJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00.toByte(), 0x10.toByte(), 0x4A.toByte(), 0x46.toByte(),
            0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
        val result = MotionPhotoParser.parseMotionPhoto(ByteArrayInputStream(plainJpeg), plainJpeg.size.toLong())
        assertNull("Plain JPEG must return null", result)
    }

    @Test
    fun testSamsungMotionPhotoDataDetection() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val mp4Payload = "FAKE_MP4_VIDEO_PAYLOAD_FOR_TESTING".toByteArray(Charsets.US_ASCII)
        val marker = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)
        val lenBytes = byteArrayOf(
            ((mp4Payload.size ushr 24) and 0xFF).toByte(),
            ((mp4Payload.size ushr 16) and 0xFF).toByte(),
            ((mp4Payload.size ushr 8) and 0xFF).toByte(),
            (mp4Payload.size and 0xFF).toByte()
        )
        // Samsung appends MP4 payload, then MotionPhoto_Data marker + 4-byte length
        val combined = jpegHeader + mp4Payload + marker + lenBytes
        val result = MotionPhotoParser.parseMotionPhoto(ByteArrayInputStream(combined), combined.size.toLong())

        assertNotNull("Samsung motion photo must be detected", result)
        assertTrue(result!!.isMotionPhoto)
        // Video starts right after jpegHeader
        assertEquals(jpegHeader.size.toLong(), result.videoOffsetFromStart)
    }

    @Test
    fun testGoogleCameraMicroVideoXmpDetection() {
        val xmpXml = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
               GCamera:MotionPhoto="1"
               GCamera:MicroVideo="1"
               GCamera:MicroVideoVersion="1"
               GCamera:MicroVideoOffset="64"
               GCamera:MicroVideoPresentationTimestampUs="1500000"/>
             </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val jpegPrefix = byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + xmpXml + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        val videoPayload = ByteArray(64) { 0x7F }
        val combined = jpegPrefix + videoPayload

        val result = MotionPhotoParser.parseMotionPhoto(ByteArrayInputStream(combined), combined.size.toLong())
        assertNotNull("Google Camera Motion Photo must be detected", result)
        assertTrue(result!!.isMotionPhoto)
        assertEquals(64L, result.videoLength)
        assertEquals(jpegPrefix.size.toLong(), result.videoOffsetFromStart)
        assertEquals(1500000L, result.presentationTimestampUs)
    }

    @Test
    fun testUniversalFtypTrailerFallback() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        // MP4 header: 4-byte size (0x00000020), "ftyp", "mp42"
        val ftypBox = byteArrayOf(
            0x00, 0x00, 0x00, 0x20,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte()
        )
        val dummyVideoBody = ByteArray(40) { 0x01 }
        val combined = jpegHeader + ftypBox + dummyVideoBody

        val result = MotionPhotoParser.parseMotionPhoto(ByteArrayInputStream(combined), combined.size.toLong())
        assertNotNull("Universal ftyp atom must be detected", result)
        assertTrue(result!!.isMotionPhoto)
        assertEquals(jpegHeader.size.toLong(), result.videoOffsetFromStart)
    }
}
