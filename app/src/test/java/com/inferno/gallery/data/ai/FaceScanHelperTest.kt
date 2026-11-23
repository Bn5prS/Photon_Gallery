package com.inferno.gallery.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceScanHelperTest {

    @Test
    fun shouldRunTileScan_triggersOnlyForGroupOrSmallFaces() {
        // 0 faces (scenery/receipt): never waste CPU on tiles
        assertFalse(FaceScanHelper.shouldRunTileScan(2048, 1536, emptyList()))

        // 1 large closeup/selfie face (400px on 2048px image = 19.5%): no tiles needed
        val selfie = listOf(FaceBox2D(800f, 400f, 1200f, 900f))
        assertFalse(FaceScanHelper.shouldRunTileScan(2048, 1536, selfie))

        // 2+ faces detected: group photo -> run tiles so smaller/back-row faces aren't missed
        val twoFaces = listOf(
            FaceBox2D(300f, 400f, 500f, 650f),
            FaceBox2D(900f, 420f, 1080f, 640f)
        )
        assertTrue(FaceScanHelper.shouldRunTileScan(2048, 1536, twoFaces))

        // 1 small distant face (120px wide on 2048px wide shot = 5.8%): likely wider group/scene
        val distantFace = listOf(FaceBox2D(500f, 500f, 620f, 650f))
        assertTrue(FaceScanHelper.shouldRunTileScan(2048, 1536, distantFace))
    }

    @Test
    fun computeScanTiles_enclosesCenterAndCornerFacesWithoutSplitting() {
        val tiles = FaceScanHelper.computeScanTiles(2048, 1536)
        assertTrue("Expected overlapping tiles for high-res frame", tiles.size in 4..6)

        val sampleFaces = listOf(
            FaceBox2D(960f, 700f, 1080f, 840f),   // Dead center straddling X=1024, Y=768
            FaceBox2D(80f, 120f, 180f, 240f),     // Top-left corner
            FaceBox2D(1820f, 1280f, 1940f, 1420f) // Bottom-right corner
        )

        for (face in sampleFaces) {
            val enclosed = tiles.any { tile ->
                face.left >= tile.left && face.top >= tile.top &&
                    face.right <= tile.right && face.bottom <= tile.bottom
            }
            assertTrue("Face $face must sit completely uncut inside at least one scan tile", enclosed)
        }
    }

    @Test
    fun deduplicateBoxes_mergesDuplicateTileHitsWhileKeepingAdjacentCrowdFaces() {
        // Same face found in full-frame (rankScore=30) and sharper tile pass (rankScore=52),
        // plus a neighbor standing shoulder-to-shoulder right next to them.
        val candidates = listOf(
            RankedFaceBox(index = 0, box = FaceBox2D(400f, 300f, 500f, 420f), score = 30f),
            RankedFaceBox(index = 1, box = FaceBox2D(406f, 304f, 496f, 416f), score = 52f),
            RankedFaceBox(index = 2, box = FaceBox2D(515f, 305f, 610f, 422f), score = 48f)
        )

        val kept = FaceScanHelper.deduplicateRankedBoxes(candidates)
        assertEquals(listOf(1, 2), kept.map { it.index })
    }

    @Test
    fun isCropLightingUsable_rejectsDarkBlownOutAndFlatCropsWhileKeepingRealFaces() {
        // Pitch-black shadow blob
        val pitchBlack = FloatArray(200) { i -> if (i % 10 == 0) 18f else 4f }
        assertFalse(FaceScanHelper.isCropLightingUsable(pitchBlack))

        // Blown-out white glare
        val blownOutWhite = FloatArray(200) { i -> if (i % 10 == 0) 238f else 253f }
        assertFalse(FaceScanHelper.isCropLightingUsable(blownOutWhite))

        // Featureless flat gray wall
        val flatWall = FloatArray(200) { i -> 120f + (i % 5).toFloat() }
        assertFalse(FaceScanHelper.isCropLightingUsable(flatWall))

        // Dim indoor real face (still has eyes/skin contrast)
        val dimRealFace = FloatArray(200) { i ->
            when (i % 4) {
                0 -> 28f
                1 -> 48f
                2 -> 64f
                else -> 76f
            }
        }
        assertTrue(FaceScanHelper.isCropLightingUsable(dimRealFace))

        // Bright sunlit real face (bright highlights + shaded eyes/mouth)
        val brightRealFace = FloatArray(200) { i ->
            when (i % 4) {
                0 -> 150f
                1 -> 205f
                2 -> 230f
                else -> 244f
            }
        }
        assertTrue(FaceScanHelper.isCropLightingUsable(brightRealFace))
    }
}
