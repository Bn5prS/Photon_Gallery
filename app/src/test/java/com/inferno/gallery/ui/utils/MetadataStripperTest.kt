package com.inferno.gallery.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

class MetadataStripperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // ─────────────────────────────────────────────────────────────────────────────
    // JPEG
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testStripJpegBytes_removesAppMarkersAndComments() {
        val bos = ByteArrayOutputStream()

        // SOI
        bos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))

        // APP0 (JFIF) - Should be preserved
        val app0Payload = "JFIF\u0000\u0001\u0001\u0000\u0000\u0001\u0000\u0001\u0000\u0000".toByteArray(Charsets.US_ASCII)
        bos.write(byteArrayOf(0xFF.toByte(), 0xE0.toByte()))
        val app0Len = app0Payload.size + 2
        bos.write(byteArrayOf((app0Len shr 8).toByte(), (app0Len and 0xFF).toByte()))
        bos.write(app0Payload)

        // APP1 (EXIF & GPS) - Must be stripped!
        val app1Payload = "Exif\u0000\u0000GPS_COORDINATES_SECRET_37.7749".toByteArray(Charsets.US_ASCII)
        bos.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))
        val app1Len = app1Payload.size + 2
        bos.write(byteArrayOf((app1Len shr 8).toByte(), (app1Len and 0xFF).toByte()))
        bos.write(app1Payload)

        // APP2 (FlashPix / ICC / XMP) - Must be stripped!
        val app2Payload = "ICC_PROFILE_OR_EXTENDED_XMP".toByteArray(Charsets.US_ASCII)
        bos.write(byteArrayOf(0xFF.toByte(), 0xE2.toByte()))
        val app2Len = app2Payload.size + 2
        bos.write(byteArrayOf((app2Len shr 8).toByte(), (app2Len and 0xFF).toByte()))
        bos.write(app2Payload)

        // APP13 (Photoshop IPTC) - Must be stripped!
        val app13Payload = "Photoshop 3.0 8BIM IPTC_AUTHOR_LEAK".toByteArray(Charsets.US_ASCII)
        bos.write(byteArrayOf(0xFF.toByte(), 0xED.toByte()))
        val app13Len = app13Payload.size + 2
        bos.write(byteArrayOf((app13Len shr 8).toByte(), (app13Len and 0xFF).toByte()))
        bos.write(app13Payload)

        // COM (User comment) - Must be stripped!
        val comPayload = "User comment leaked from camera app".toByteArray(Charsets.US_ASCII)
        bos.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
        val comLen = comPayload.size + 2
        bos.write(byteArrayOf((comLen shr 8).toByte(), (comLen and 0xFF).toByte()))
        bos.write(comPayload)

        // DQT (Quantization Table) - Preserved
        val dqtPayload = ByteArray(65) { 1 }
        bos.write(byteArrayOf(0xFF.toByte(), 0xDB.toByte()))
        val dqtLen = dqtPayload.size + 2
        bos.write(byteArrayOf((dqtLen shr 8).toByte(), (dqtLen and 0xFF).toByte()))
        bos.write(dqtPayload)

        // SOS (Start of Scan) - Preserved
        val sosPayload = byteArrayOf(0x01, 0x01, 0x00, 0x00, 0x3F, 0x00)
        bos.write(byteArrayOf(0xFF.toByte(), 0xDA.toByte()))
        val sosLen = sosPayload.size + 2
        bos.write(byteArrayOf((sosLen shr 8).toByte(), (sosLen and 0xFF).toByte()))
        bos.write(sosPayload)

        // Entropy scan data with byte-stuffed 0xFF 0x00
        bos.write(byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0x00, 0x56, 0x78))

        // EOI
        bos.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte()))

        // Trailing garbage after EOI (e.g. appended file / hidden thumbnail)
        bos.write("TRAILING_GARBAGE_AFTER_EOI".toByteArray(Charsets.US_ASCII))

        val stripped = MetadataStripper.stripJpegBytes(bos.toByteArray())
        val strippedStr = String(stripped, Charsets.ISO_8859_1)

        // Asserts: All sensitive markers completely eliminated
        assertFalse("APP1 EXIF must be removed", strippedStr.contains("GPS_COORDINATES"))
        assertFalse("APP2 ICC must be removed", strippedStr.contains("ICC_PROFILE"))
        assertFalse("APP13 Photoshop IPTC must be removed", strippedStr.contains("IPTC_AUTHOR_LEAK"))
        assertFalse("COM comment must be removed", strippedStr.contains("User comment leaked"))
        assertFalse("Trailing garbage after EOI must be stripped", strippedStr.contains("TRAILING_GARBAGE"))

        // Critical image markers preserved
        assertTrue("SOI must remain at start", stripped[0] == 0xFF.toByte() && stripped[1] == 0xD8.toByte())
        assertTrue("APP0 JFIF remains", strippedStr.contains("JFIF"))
        assertTrue("DQT remains", strippedStr.contains(String(dqtPayload, Charsets.ISO_8859_1)))
        assertTrue("EOI must terminate image", stripped[stripped.size - 2] == 0xFF.toByte() && stripped[stripped.size - 1] == 0xD9.toByte())
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PNG
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testStripPng_removesMetadataChunksPreservesScanlines() {
        val inputFile = tempFolder.newFile("test.png")
        val outputFile = tempFolder.newFile("test_stripped.png")

        val bos = ByteArrayOutputStream()
        // PNG Signature
        bos.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A))

        fun writeChunk(type: String, data: ByteArray) {
            val len = data.size
            bos.write(byteArrayOf((len shr 24).toByte(), (len shr 16).toByte(), (len shr 8).toByte(), (len and 0xFF).toByte()))
            bos.write(type.toByteArray(Charsets.US_ASCII))
            bos.write(data)
            // Fake CRC 4 bytes
            bos.write(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        }

        // IHDR
        val ihdrData = ByteArray(13) { 0 }
        writeChunk("IHDR", ihdrData)

        // tEXt (Author / camera / timestamp) - Must be stripped!
        writeChunk("tEXt", "Author\u0000John Doe with iPhone 15".toByteArray(Charsets.US_ASCII))

        // iTXt (XMP packet) - Must be stripped!
        writeChunk("iTXt", "XML:com.adobe.xmp\u0000\u0000\u0000\u0000\u0000<x:xmpmeta>SECRET_GPS</x:xmpmeta>".toByteArray(Charsets.US_ASCII))

        // eXIf (Exif raw payload) - Must be stripped!
        writeChunk("eXIf", "EXIF_BODY_SERIAL_123456789".toByteArray(Charsets.US_ASCII))

        // tIME (Last modification timestamp) - Must be stripped!
        writeChunk("tIME", byteArrayOf(0x07, 0xE8.toByte(), 0x01, 0x01, 0x00, 0x00, 0x00))

        // IDAT (Pixel data) - Must be preserved!
        val idatData = byteArrayOf(0x78, 0x9C.toByte(), 0x63, 0x60, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01)
        writeChunk("IDAT", idatData)

        // IEND
        writeChunk("IEND", ByteArray(0))

        inputFile.writeBytes(bos.toByteArray())

        val success = MetadataStripper.strip(inputFile, outputFile, "image/png")
        assertTrue("Stripper must return true for PNG", success)

        val strippedBytes = outputFile.readBytes()
        val strippedStr = String(strippedBytes, Charsets.ISO_8859_1)

        assertFalse("tEXt chunk must be dropped", strippedStr.contains("John Doe"))
        assertFalse("iTXt XMP must be dropped", strippedStr.contains("SECRET_GPS"))
        assertFalse("eXIf chunk must be dropped", strippedStr.contains("EXIF_BODY_SERIAL"))
        assertFalse("tIME chunk must be dropped", strippedStr.contains("tIME"))

        assertTrue("IHDR chunk must be preserved", strippedStr.contains("IHDR"))
        assertTrue("IDAT chunk must be preserved", strippedStr.contains("IDAT"))
        assertTrue("IEND chunk must be preserved", strippedStr.contains("IEND"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // WebP
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testStripWebp_removesExifAndXmpChunksAndClearsVp8xFlags() {
        val inputFile = tempFolder.newFile("test.webp")
        val outputFile = tempFolder.newFile("test_stripped.webp")

        val bos = ByteArrayOutputStream()
        // RIFF header placeholder (will fix size later)
        bos.write("RIFF".toByteArray(Charsets.US_ASCII))
        bos.write(ByteArray(4)) // size
        bos.write("WEBP".toByteArray(Charsets.US_ASCII))

        fun writeWebpChunk(fourCc: String, data: ByteArray) {
            bos.write(fourCc.toByteArray(Charsets.US_ASCII))
            val len = data.size
            bos.write(byteArrayOf((len and 0xFF).toByte(), ((len shr 8) and 0xFF).toByte(), ((len shr 16) and 0xFF).toByte(), ((len shr 24) and 0xFF).toByte()))
            bos.write(data)
            if (len % 2 != 0) bos.write(0) // padding byte
        }

        // VP8X extended header with ICC (0x20), EXIF (0x08), XMP (0x04) set
        // flags byte is index 0 of the 10-byte payload: flags = 0x20 | 0x08 | 0x04 = 0x2C
        val vp8xPayload = byteArrayOf(0x2C, 0, 0, 0, 100, 0, 0, 100, 0, 0)
        writeWebpChunk("VP8X", vp8xPayload)

        // EXIF chunk - Must be stripped!
        writeWebpChunk("EXIF", "Exif\u0000\u0000SECRET_EXIF_COORDINATES".toByteArray(Charsets.US_ASCII))

        // XMP chunk - Must be stripped!
        writeWebpChunk("XMP ", "<x:xmpmeta>SECRET_XMP_TAGS</x:xmpmeta>".toByteArray(Charsets.US_ASCII))

        // VP8 chunk (image data) - Must be preserved!
        val vp8Data = ByteArray(64) { 0x55 }
        writeWebpChunk("VP8 ", vp8Data)

        val initialBytes = bos.toByteArray()
        val riffSize = initialBytes.size - 8
        initialBytes[4] = (riffSize and 0xFF).toByte()
        initialBytes[5] = ((riffSize shr 8) and 0xFF).toByte()
        initialBytes[6] = ((riffSize shr 16) and 0xFF).toByte()
        initialBytes[7] = ((riffSize shr 24) and 0xFF).toByte()

        inputFile.writeBytes(initialBytes)

        val success = MetadataStripper.strip(inputFile, outputFile, "image/webp")
        assertTrue("Stripper must return true for WebP", success)

        val strippedBytes = outputFile.readBytes()
        val strippedStr = String(strippedBytes, Charsets.ISO_8859_1)

        assertFalse("EXIF chunk must be dropped", strippedStr.contains("SECRET_EXIF_COORDINATES"))
        assertFalse("XMP chunk must be dropped", strippedStr.contains("SECRET_XMP_TAGS"))
        assertTrue("VP8 frame data must be preserved", strippedStr.contains("VP8 "))

        // Verify VP8X flags were cleaned (bits 0x20, 0x08, 0x04 cleared -> 0x00)
        val vp8xPos = strippedStr.indexOf("VP8X")
        assertTrue("VP8X header should exist", vp8xPos >= 0)
        val flagsByte = strippedBytes[vp8xPos + 8].toInt() and 0xFF
        assertEquals("VP8X EXIF/XMP/ICC flags must be cleared to 0", 0, flagsByte and 0x2C)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GIF
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testStripGif_removesCommentsAndXmpExtensions() {
        val inputFile = tempFolder.newFile("test.gif")
        val outputFile = tempFolder.newFile("test_stripped.gif")

        val bos = ByteArrayOutputStream()
        // Header: GIF89a
        bos.write("GIF89a".toByteArray(Charsets.US_ASCII))
        // Logical Screen Descriptor (7 bytes, no GCT)
        bos.write(byteArrayOf(10, 0, 10, 0, 0, 0, 0))

        // Comment Extension (0x21 0xFE) - Must be stripped!
        bos.write(byteArrayOf(0x21, 0xFE.toByte()))
        val commentStr = "COMMENT_LEAK_IN_GIF".toByteArray(Charsets.US_ASCII)
        bos.write(commentStr.size)
        bos.write(commentStr)
        bos.write(0x00) // Block terminator

        // Application Extension (XMP) - Must be stripped!
        bos.write(byteArrayOf(0x21, 0xFF.toByte()))
        val xmpHeader = "XMP DataXMP".toByteArray(Charsets.US_ASCII)
        bos.write(xmpHeader.size)
        bos.write(xmpHeader)
        val xmpBody = "SECRET_XMP_IN_GIF".toByteArray(Charsets.US_ASCII)
        bos.write(xmpBody.size)
        bos.write(xmpBody)
        bos.write(0x00) // Block terminator

        // Netscape Looping Application Extension - Must be preserved!
        bos.write(byteArrayOf(0x21, 0xFF.toByte()))
        val netscapeHeader = "NETSCAPE2.0".toByteArray(Charsets.US_ASCII)
        bos.write(netscapeHeader.size)
        bos.write(netscapeHeader)
        bos.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00)) // 3 bytes subblock + 0 terminator

        // Graphic Control Extension - Must be preserved!
        bos.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x00, 0x0A, 0x00, 0x00, 0x00))

        // Image Descriptor (0x2C) - 10 bytes
        bos.write(byteArrayOf(0x2C, 0, 0, 0, 0, 10, 0, 10, 0, 0))
        // LZW code size
        bos.write(0x02)
        // Image data sub-block
        bos.write(byteArrayOf(0x02, 0x4C, 0x01, 0x00)) // 2 bytes + 0 terminator

        // Trailer
        bos.write(0x3B)

        inputFile.writeBytes(bos.toByteArray())

        val success = MetadataStripper.strip(inputFile, outputFile, "image/gif")
        assertTrue("Stripper must return true for GIF", success)

        val strippedBytes = outputFile.readBytes()
        val strippedStr = String(strippedBytes, Charsets.ISO_8859_1)

        assertFalse("Comment extension must be stripped", strippedStr.contains("COMMENT_LEAK_IN_GIF"))
        assertFalse("XMP application extension must be stripped", strippedStr.contains("SECRET_XMP_IN_GIF"))
        assertTrue("Netscape application extension must be preserved", strippedStr.contains("NETSCAPE2.0"))
        assertTrue("Trailer must terminate file", strippedBytes.last() == 0x3B.toByte())
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Video: MP4 / MOV / 3GP (ISOBMFF)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testStripMp4Video_wipesUdtaMetaUuidAndZeroesTimestamps() {
        val inputFile = tempFolder.newFile("test.mp4")
        val outputFile = tempFolder.newFile("test_stripped.mp4")

        val bos = ByteArrayOutputStream()

        fun writeBox(fourCc: String, payload: ByteArray) {
            val size = payload.size + 8
            bos.write(byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), (size and 0xFF).toByte()))
            bos.write(fourCc.toByteArray(Charsets.US_ASCII))
            bos.write(payload)
        }

        // ftyp box
        writeBox("ftyp", "mp42\u0000\u0000\u0000\u0000isommp42".toByteArray(Charsets.US_ASCII))

        // Construct moov box
        val moovStream = ByteArrayOutputStream()

        // mvhd box with non-zero timestamps (version 0, creation=1000, modification=2000)
        val mvhdPayload = ByteArrayOutputStream().apply {
            write(0) // version 0
            write(byteArrayOf(0, 0, 0)) // flags
            write(byteArrayOf(0, 0, 0x03, 0xE8.toByte())) // creation_time = 1000
            write(byteArrayOf(0, 0, 0x07, 0xD0.toByte())) // mod_time = 2000
            write(byteArrayOf(0, 0, 0x03, 0xE8.toByte())) // timescale = 1000
            write(byteArrayOf(0, 0, 0x27, 0x10)) // duration = 10000
            write(ByteArray(80)) // other fields
        }.toByteArray()
        val mvhdSize = mvhdPayload.size + 8
        moovStream.write(byteArrayOf((mvhdSize shr 24).toByte(), (mvhdSize shr 16).toByte(), (mvhdSize shr 8).toByte(), (mvhdSize and 0xFF).toByte()))
        moovStream.write("mvhd".toByteArray(Charsets.US_ASCII))
        moovStream.write(mvhdPayload)

        // udta box containing GPS coordinates
        val udtaPayload = "©xyz+37.7749-122.4194/lociLOCATION_STRING_SECRET".toByteArray(Charsets.US_ASCII)
        val udtaSize = udtaPayload.size + 8
        moovStream.write(byteArrayOf((udtaSize shr 24).toByte(), (udtaSize shr 16).toByte(), (udtaSize shr 8).toByte(), (udtaSize and 0xFF).toByte()))
        moovStream.write("udta".toByteArray(Charsets.US_ASCII))
        moovStream.write(udtaPayload)

        // meta box containing Apple / Android quicktime metadata
        val metaPayload = "keyscom.apple.quicktime.location.ISO6709ilstMAKE_IPHONE_SECRET".toByteArray(Charsets.US_ASCII)
        val metaSize = metaPayload.size + 8
        moovStream.write(byteArrayOf((metaSize shr 24).toByte(), (metaSize shr 16).toByte(), (metaSize shr 8).toByte(), (metaSize and 0xFF).toByte()))
        moovStream.write("meta".toByteArray(Charsets.US_ASCII))
        moovStream.write(metaPayload)

        // uuid box containing XMP metadata
        val uuidPayload = ByteArray(16) { 0xAA.toByte() } + "<xmp>SECRET_XMP_PACKET</xmp>".toByteArray(Charsets.US_ASCII)
        val uuidSize = uuidPayload.size + 8
        moovStream.write(byteArrayOf((uuidSize shr 24).toByte(), (uuidSize shr 16).toByte(), (uuidSize shr 8).toByte(), (uuidSize and 0xFF).toByte()))
        moovStream.write("uuid".toByteArray(Charsets.US_ASCII))
        moovStream.write(uuidPayload)

        // Write moov box
        writeBox("moov", moovStream.toByteArray())

        // mdat box (video media frames)
        writeBox("mdat", "H264_VIDEO_FRAME_PAYLOAD_DATA".toByteArray(Charsets.US_ASCII))

        val originalBytes = bos.toByteArray()
        inputFile.writeBytes(originalBytes)

        val success = MetadataStripper.strip(inputFile, outputFile, "video/mp4")
        assertTrue("Stripper must return true for MP4", success)

        val strippedBytes = outputFile.readBytes()
        val strippedStr = String(strippedBytes, Charsets.ISO_8859_1)

        // 1. File size must remain strictly identical so stco/co64 sample chunk offsets are not corrupted
        assertEquals("File size must not change in ISOBMFF scrubbing", originalBytes.size.toLong(), outputFile.length())

        // 2. Sensitive locations & device details must be 100% wiped
        assertFalse("GPS coordinates in udta must be wiped", strippedStr.contains("+37.7749"))
        assertFalse("Location strings in udta must be wiped", strippedStr.contains("LOCATION_STRING_SECRET"))
        assertFalse("QuickTime metadata keys in meta must be wiped", strippedStr.contains("com.apple.quicktime"))
        assertFalse("Device make in meta must be wiped", strippedStr.contains("MAKE_IPHONE_SECRET"))
        assertFalse("XMP in uuid must be wiped", strippedStr.contains("SECRET_XMP_PACKET"))

        // 3. udta, meta, uuid box types must be converted to 'free'
        assertTrue("Free boxes should replace wiped metadata boxes", strippedStr.contains("free"))

        // 4. Video media data must be fully intact
        assertTrue("mdat payload must remain intact", strippedStr.contains("H264_VIDEO_FRAME_PAYLOAD_DATA"))

        // 5. Timestamps in mvhd must be zeroed out
        RandomAccessFile(outputFile, "r").use { raf ->
            val moovIndex = strippedStr.indexOf("moov")
            val mvhdIndex = strippedStr.indexOf("mvhd", moovIndex)
            // mvhd header is 8 bytes, payload: 1 byte version, 3 bytes flags, 4 bytes creation_time, 4 bytes mod_time
            raf.seek(mvhdIndex.toLong() + 4 + 4) // seek to creation_time (after 'mvhd' + version/flags)
            val creationTime = raf.readInt()
            val modTime = raf.readInt()
            assertEquals("mvhd creation_time must be zeroed", 0, creationTime)
            assertEquals("mvhd modification_time must be zeroed", 0, modTime)
        }
    }
}
