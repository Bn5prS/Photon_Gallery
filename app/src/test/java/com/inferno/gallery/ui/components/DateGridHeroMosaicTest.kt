package com.inferno.gallery.ui.components

import com.inferno.gallery.data.db.MediaFaceQuality
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.GalleryListItem
import com.inferno.gallery.ui.mapMediaFaceQualities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateGridHeroMosaicTest {

    private fun item(
        id: String,
        bucket: String = "Camera",
        name: String = "IMG_$id.jpg",
        path: String = "/storage/emulated/0/DCIM/Camera/$name",
        isVideo: Boolean = false
    ): GalleryListItem.Item = GalleryListItem.Item(
        GalleryItem(
            id = id,
            uri = android.net.FakeUri("content://media/external/images/media/$id"),
            bucketName = bucket,
            dateAdded = 1_700_000_000L,
            size = 2_000_000L,
            name = name,
            dateModified = 1_700_000_000L,
            path = path,
            isVideo = isVideo
        )
    )

    @Test
    fun `mapMediaFaceQualities keys string media ids to max quality score`() {
        val map = mapMediaFaceQualities(
            listOf(
                MediaFaceQuality(mediaId = 101L, maxQuality = 0.88f),
                MediaFaceQuality(mediaId = 202L, maxQuality = 0.45f)
            )
        )
        assertEquals(0.88f, map["101"]!!, 1e-4f)
        assertEquals(0.45f, map["202"]!!, 1e-4f)
    }

    @Test
    fun `promotes favorite over face and camera photo and anchors hero on left`() {
        val feed = listOf(
            GalleryListItem.Header("Yesterday"),
            item("1"),
            item("2", bucket = "Screenshots", name = "Screenshot_2026.png"),
            item("3"),
            item("4"),
            item("5")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("4"),
            faceQualityByMediaId = mapOf("3" to 0.92f)
        )

        assertEquals(3, display.size)
        val big = display[1] as MosaicDisplayItem.BigTileItem
        val quad = display[2] as MosaicDisplayItem.QuadTileItem
        assertEquals("4", big.item.id)
        assertEquals(2, big.span)
        assertEquals(1.0f, big.aspectRatio, 1e-4f)
        assertEquals(listOf("1", "2", "3", "5"), quad.items.map { it.id })
    }

    @Test
    fun `promotes highest quality face when no favorite exists`() {
        val feed = listOf(
            GalleryListItem.Header("Sunday"),
            item("10"),
            item("11"),
            item("12"),
            item("13"),
            item("14")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            faceQualityByMediaId = mapOf("11" to 0.61f, "13" to 0.95f)
        )

        val big = display[1] as MosaicDisplayItem.BigTileItem
        assertEquals("13", big.item.id)
    }

    @Test
    fun `paces two calm 1x1 rows between hero blocks and anchors heroes on left`() {
        val feed = buildList {
            add(GalleryListItem.Header("Saturday"))
            for (i in 1..18) add(item(i.toString()))
        }
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("2", "17")
        )

        assertEquals("2", (display[1] as MosaicDisplayItem.BigTileItem).item.id)
        assertTrue(display[2] is MosaicDisplayItem.QuadTileItem)

        for (idx in 3..10) {
            assertTrue("Expected SingleItem at $idx", display[idx] is MosaicDisplayItem.SingleItem)
        }

        assertEquals("17", (display[11] as MosaicDisplayItem.BigTileItem).item.id)
        assertTrue(display[12] is MosaicDisplayItem.QuadTileItem)
    }

    @Test
    fun `never promotes screenshots or screen recordings and falls back to calm singles`() {
        val screenRec = item(
            id = "sr1",
            bucket = "Screen recordings",
            name = "screen-20261118-010203.mp4",
            isVideo = true
        )
        assertTrue(scoreMosaicHeroCandidate(screenRec.galleryItem, isFavorite = false, faceQuality = null) < 0f)

        val feed = listOf(
            GalleryListItem.Header("Today"),
            item("s1", bucket = "Screenshots", name = "Screenshot_1.png"),
            item("s2", bucket = "Screenshots", name = "Screenshot_2.png"),
            item("s3", bucket = "Screenshots", name = "Screenshot_3.png"),
            item("s4", bucket = "Screenshots", name = "Screenshot_4.png"),
            screenRec,
            item("c1"),
            item("c2"),
            item("c3"),
            item("c4")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("c3")
        )

        for (idx in 1..4) {
            assertTrue(display[idx] is MosaicDisplayItem.SingleItem)
        }
        val big = display.filterIsInstance<MosaicDisplayItem.BigTileItem>().single()
        assertEquals("c3", big.item.id)
    }

    @Test
    fun `handles short days mid-day nulls and all column counts 2 through 6 with aligned spans`() {
        for (cols in listOf(1, 2, 3, 4, 5, 6, 8)) {
            val clampedCols = cols.coerceIn(2, 6)
            val feed: List<GalleryListItem?> = buildList {
                add(GalleryListItem.Header("ShortDay"))
                add(item("a"))
                add(item("b"))
                add(GalleryListItem.Header("MidNullDay"))
                add(item("m1"))
                add(item("m2"))
                add(null)
                for (i in 3..14) add(item("m$i"))
                add(GalleryListItem.Header("FullDay"))
                for (i in 1..30) add(item("f$i"))
            }
            val display = buildDateGridHeroItems(
                totalCount = feed.size,
                itemAt = { feed[it] },
                columns = cols,
                favoriteIds = setOf("f3", "f28")
            )

            val heroes = display.filterIsInstance<MosaicDisplayItem.BigTileItem>()
            assertTrue("Expected at least 2 heroes for cols=$cols", heroes.size >= 2)
            heroes.forEach { hero ->
                assertEquals(2, hero.span)
                assertEquals(1.0f, hero.aspectRatio, 1e-4f)
            }

            // Simulate LazyVerticalGrid span layout: no multi-span or composite block item may ever wrap mid-row!
            var colCursor = 0
            for (d in display) {
                when (d) {
                    is MosaicDisplayItem.HeaderItem -> colCursor = 0
                    is MosaicDisplayItem.BigTileItem -> {
                        assertTrue("BigTileItem overflowed row at colCursor=$colCursor for cols=$clampedCols", colCursor + d.span <= clampedCols)
                        colCursor = (colCursor + d.span) % clampedCols
                    }
                    is MosaicDisplayItem.QuadTileItem -> {
                        val span = 2.coerceAtMost(clampedCols)
                        assertTrue("QuadTileItem overflowed row at colCursor=$colCursor for cols=$clampedCols", colCursor + span <= clampedCols)
                        colCursor = (colCursor + span) % clampedCols
                    }
                    is MosaicDisplayItem.PairTileItem -> {
                        assertTrue("PairTileItem must sit inside hero row for cols=$clampedCols", colCursor + 1 <= clampedCols)
                        colCursor = (colCursor + 1) % clampedCols
                    }
                    is MosaicDisplayItem.SingleItem -> {
                        colCursor = (colCursor + 1) % clampedCols
                    }
                }
            }

            val emittedUris = display.flatMap { it.uris }
            assertEquals(46, emittedUris.size)
            assertEquals(46, emittedUris.distinct().size)
        }
    }

    @Test
    fun `companion tile keys update when non-first member swaps with hero and single cells avoid 720px override`() {
        val feed4 = listOf(
            GalleryListItem.Header("Day4"),
            item("1"), item("2"), item("3"), item("4"), item("5"), item("6")
        )
        val before4 = buildDateGridHeroItems(feed4.size, { feed4[it] }, columns = 4, favoriteIds = setOf("5"))
        val after4 = buildDateGridHeroItems(feed4.size, { feed4[it] }, columns = 4, favoriteIds = setOf("4"))

        val quadBefore = before4[2] as MosaicDisplayItem.QuadTileItem
        val quadAfter = after4[2] as MosaicDisplayItem.QuadTileItem
        assertNotEquals(quadBefore.key, quadAfter.key)

        val feed3 = listOf(
            GalleryListItem.Header("Day3"),
            item("1"), item("2"), item("3")
        )
        val before3 = buildDateGridHeroItems(feed3.size, { feed3[it] }, columns = 3, favoriteIds = setOf("3"))
        val after3 = buildDateGridHeroItems(feed3.size, { feed3[it] }, columns = 3, favoriteIds = setOf("2"))

        val pairBefore = before3[2] as MosaicDisplayItem.PairTileItem
        val pairAfter = after3[2] as MosaicDisplayItem.PairTileItem
        assertNotEquals(pairBefore.key, pairAfter.key)

        val big = before4[1] as MosaicDisplayItem.BigTileItem
        val single = before4[3] as MosaicDisplayItem.SingleItem
        assertEquals(720, mosaicCellTargetPxOverride(big))
        assertNull(mosaicCellTargetPxOverride(single))
    }
}
