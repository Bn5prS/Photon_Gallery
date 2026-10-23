package com.inferno.gallery.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NoMediaUtilsTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun testNormalFileIsNotHidden() {
        val folder = tempFolder.newFolder("DCIM", "Camera")
        val image = File(folder, "IMG_001.jpg").apply { createNewFile() }

        val isHidden = NoMediaUtils.isPathHiddenByNoMedia(image.absolutePath)
        assertFalse("Normal camera file should not be hidden", isHidden)
    }

    @Test
    fun testFileInNoMediaFolderIsHidden() {
        val folder = tempFolder.newFolder("Vault")
        File(folder, ".nomedia").apply { createNewFile() }
        val image = File(folder, "secret.jpg").apply { createNewFile() }

        val isHidden = NoMediaUtils.isPathHiddenByNoMedia(image.absolutePath)
        assertTrue("File in folder with .nomedia must be hidden", isHidden)
    }

    @Test
    fun testFileInSubdirectoryOfNoMediaFolderIsHidden() {
        val parent = tempFolder.newFolder("EncryptedPhotos")
        File(parent, ".nomedia").apply { createNewFile() }
        val subfolder = File(parent, "SubAlbum").apply { mkdir() }
        val image = File(subfolder, "sub_secret.jpg").apply { createNewFile() }

        val isHidden = NoMediaUtils.isPathHiddenByNoMedia(image.absolutePath)
        assertTrue("File in nested subfolder under .nomedia must be hidden", isHidden)
    }

    @Test
    fun testFileInDotPrefixFolderIsHidden() {
        val dotFolder = tempFolder.newFolder(".GalleryVault")
        val image = File(dotFolder, "hidden.jpg").apply { createNewFile() }

        val isHidden = NoMediaUtils.isPathHiddenByNoMedia(image.absolutePath)
        assertTrue("File inside dot-prefixed directory must be hidden", isHidden)
    }

    @Test
    fun testCacheSpeedsUpSubsequentLookups() {
        val folder = tempFolder.newFolder("BatchTest")
        File(folder, ".nomedia").apply { createNewFile() }
        val img1 = File(folder, "1.jpg").apply { createNewFile() }
        val img2 = File(folder, "2.jpg").apply { createNewFile() }

        val cache = mutableMapOf<String, Boolean>()
        assertTrue(NoMediaUtils.isPathHiddenByNoMedia(img1.absolutePath, cache))
        assertTrue("Cache should now contain directory", cache.containsKey(folder.absolutePath))
        assertTrue(NoMediaUtils.isPathHiddenByNoMedia(img2.absolutePath, cache))
    }
}
