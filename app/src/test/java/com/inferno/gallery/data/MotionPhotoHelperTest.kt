package com.inferno.gallery.data

import android.net.FakeUri
import com.inferno.gallery.ui.GalleryItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class MotionPhotoHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @org.junit.Before
    fun setUp() {
        MotionPhotoHelper.clearCacheForTests()
    }

    private fun createTestItem(file: File, isVideo: Boolean = false): GalleryItem {
        return GalleryItem(
            id = java.util.UUID.randomUUID().toString(),
            uri = FakeUri(file.toURI().toString()),
            bucketName = "Camera",
            dateAdded = System.currentTimeMillis() / 1000L,
            size = file.length(),
            name = file.name,
            dateModified = file.lastModified() / 1000L,
            path = file.absolutePath,
            isVideo = isVideo,
            localExists = true
        )
    }

    private class FakeContext(private val cache: File) : android.content.ContextWrapper(null) {
        override fun getCacheDir(): File = cache
    }

    @Test
    fun testAppleLivePhotoCompanionMovDetected() {
        val imageFile = tempFolder.newFile("IMG_1024.JPG")
        val movFile = tempFolder.newFile("IMG_1024.MOV")
        // Write dummy bytes to movFile so it has non-zero size
        FileOutputStream(movFile).use { it.write(ByteArray(2048)) }

        val item = createTestItem(imageFile)
        val context = FakeContext(tempFolder.root)

        val isMotion = MotionPhotoHelper.isMotionPhotoFast(context, item)
        assertTrue(isMotion)
    }

    @Test
    fun testStandardPhotoReturnsFalse() {
        val imageFile = tempFolder.newFile("NORMAL_PHOTO.JPG")
        // Write standard JPEG header without motion metadata
        FileOutputStream(imageFile).use { fos ->
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10))
            fos.write("JFIF".toByteArray(StandardCharsets.US_ASCII))
            fos.write(ByteArray(500))
        }

        val item = createTestItem(imageFile)
        val context = FakeContext(tempFolder.root)

        val isMotion = MotionPhotoHelper.isMotionPhotoFast(context, item)
        assertFalse(isMotion)
    }

    @Test
    fun testGoogleCameraMicroVideoXmpExtraction() = runBlocking {
        val totalFileSize = 1_000_000L
        val microVideoLength = 250_000L
        val presentationTimeUs = 1_500_000L

        val xmpXml = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description
                  xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                  GCamera:MotionPhoto="1"
                  GCamera:MicroVideo="1"
                  GCamera:MicroVideoOffset="$microVideoLength"
                  GCamera:MicroVideoPresentationTimestampUs="$presentationTimeUs" />
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

        val imageFile = tempFolder.newFile("GCamera_Motion.jpg")
        FileOutputStream(imageFile).use { fos ->
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte()))
            val xmpBytes = xmpXml.toByteArray(StandardCharsets.UTF_8)
            fos.write(xmpBytes)
            // Pad out to totalFileSize
            val remaining = (totalFileSize - (4 + xmpBytes.size)).toInt().coerceAtLeast(0)
            fos.write(ByteArray(remaining))
        }

        val item = createTestItem(imageFile)
        val context = FakeContext(tempFolder.root)

        val info = MotionPhotoHelper.getMotionPhotoInfo(context, item)
        assertTrue(info.isMotionPhoto)
        assertEquals(microVideoLength, info.videoLength)
        assertEquals(totalFileSize - microVideoLength, info.videoOffset)
        assertEquals(presentationTimeUs, info.presentationTimestampUs)
    }

    @Test
    fun testGoogleContainerItemSemanticMotionPhotoExtraction() = runBlocking {
        val totalFileSize = 2_000_000L
        val motionPhotoLength = 400_000L

        val xmpXml = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description xmlns:Container="http://ns.google.com/photos/1.0/container/">
                  <Container:Directory>
                    <rdf:Seq>
                      <rdf:li>
                        <Container:Item Container:ItemMime="image/jpeg" Container:Semantic="Primary" Container:Length="0"/>
                      </rdf:li>
                      <rdf:li>
                        <Container:Item Container:ItemMime="video/mp4" Container:Semantic="MotionPhoto" Container:Length="$motionPhotoLength"/>
                      </rdf:li>
                    </rdf:Seq>
                  </Container:Directory>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

        val imageFile = tempFolder.newFile("Container_Motion.jpg")
        FileOutputStream(imageFile).use { fos ->
            fos.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte()))
            val xmpBytes = xmpXml.toByteArray(StandardCharsets.UTF_8)
            fos.write(xmpBytes)
            val remaining = (totalFileSize - (4 + xmpBytes.size)).toInt().coerceAtLeast(0)
            fos.write(ByteArray(remaining))
        }

        val item = createTestItem(imageFile)
        val context = FakeContext(tempFolder.root)

        val info = MotionPhotoHelper.getMotionPhotoInfo(context, item)
        assertTrue(info.isMotionPhoto)
        assertEquals(motionPhotoLength, info.videoLength)
        assertEquals(totalFileSize - motionPhotoLength, info.videoOffset)
    }

    @Test
    fun testZeroCopyExtractionCreatesValidFile() = runBlocking {
        val totalFileSize = 200_000L
        val videoLength = 50_000L
        val videoOffset = totalFileSize - videoLength

        val imageFile = tempFolder.newFile("Embedded_Test.jpg")
        FileOutputStream(imageFile).use { fos ->
            // Write dummy photo bytes
            val photoBytes = ByteArray(videoOffset.toInt()) { 0x41.toByte() }
            fos.write(photoBytes)
            // Write dummy video bytes
            val videoBytes = ByteArray(videoLength.toInt()) { 0x56.toByte() }
            fos.write(videoBytes)
        }

        val item = createTestItem(imageFile)
        val cacheFolder = tempFolder.newFolder("cache")
        val context = FakeContext(cacheFolder)

        val info = MotionPhotoInfo(
            isMotionPhoto = true,
            videoOffset = videoOffset,
            videoLength = videoLength,
            sourceFile = imageFile
        )

        val extracted = MotionPhotoHelper.getOrExtractMotionVideo(context, item, info)
        assertNotNull(extracted)
        assertTrue(extracted!!.exists())
        assertEquals(videoLength, extracted.length())
    }
}
