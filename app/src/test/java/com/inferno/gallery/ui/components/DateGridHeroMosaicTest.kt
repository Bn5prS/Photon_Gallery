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
    fun `promotes favorite to Left Hero when at position 0 in 4 columns and maintains strict row order`() {
        val feed = listOf(
            GalleryListItem.Header("Yesterday"),
            item("1"),
            item("2"),
            item("3"),
            item("4"),
            item("5")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("1")
        )

        assertEquals(3, display.size)
        // Row 1: Cols 0-1 = BigTile("1"), Col 2 = Quad[0]("2"), Col 3 = Quad[1]("3")
        // Row 2: Cols 0-1 = BigTile cont, Col 2 = Quad[2]("4"), Col 3 = Quad[3]("5")
        val big = display[1] as MosaicDisplayItem.BigTileItem
        val quad = display[2] as MosaicDisplayItem.QuadTileItem
        assertEquals("1", big.item.id)
        assertEquals(2, big.span)
        assertEquals(1.0f, big.aspectRatio, 1e-4f)
        assertEquals(listOf("2", "3", "4", "5"), quad.items.map { it.id })
    }

    @Test
    fun `promotes favorite to Center Hero when at position 1 in 4 columns and maintains strict row order`() {
        val feed = listOf(
            GalleryListItem.Header("Yesterday"),
            item("1"),
            item("2"),
            item("3"),
            item("4"),
            item("5")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("2")
        )

        assertEquals(4, display.size)
        // Row 1: Col 0 = PairLeft[0]("1"), Cols 1-2 = BigTile("2"), Col 3 = PairRight[0]("3")
        // Row 2: Col 0 = PairLeft[1]("4"), Cols 1-2 = BigTile cont, Col 3 = PairRight[1]("5")
        val pairLeft = display[1] as MosaicDisplayItem.PairTileItem
        val big = display[2] as MosaicDisplayItem.BigTileItem
        val pairRight = display[3] as MosaicDisplayItem.PairTileItem

        assertEquals(listOf("1", "4"), pairLeft.items.map { it.id })
        assertEquals("2", big.item.id)
        assertEquals(2, big.span)
        assertEquals(listOf("3", "5"), pairRight.items.map { it.id })
    }

    @Test
    fun `promotes favorite to Right Hero when at position 2 in 4 columns and maintains strict row order`() {
        val feed = listOf(
            GalleryListItem.Header("Yesterday"),
            item("1"),
            item("2"),
            item("3"),
            item("4"),
            item("5")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("3")
        )

        assertEquals(3, display.size)
        // Row 1: Col 0 = Quad[0]("1"), Col 1 = Quad[1]("2"), Cols 2-3 = BigTile("3")
        // Row 2: Col 0 = Quad[2]("4"), Col 1 = Quad[3]("5"), Cols 2-3 = BigTile cont
        val quad = display[1] as MosaicDisplayItem.QuadTileItem
        val big = display[2] as MosaicDisplayItem.BigTileItem

        assertEquals(listOf("1", "2", "4", "5"), quad.items.map { it.id })
        assertEquals("3", big.item.id)
        assertEquals(2, big.span)
    }

    @Test
    fun `promotes twin heroes in 4 columns when first two items are both standout highlights`() {
        val feed = listOf(
            GalleryListItem.Header("Yesterday"),
            item("1"),
            item("2"),
            item("3"),
            item("4"),
            item("5")
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("1", "2")
        )

        // Twin Heroes consumes items 1 and 2, remaining 3 items become singles during calm quota
        val big1 = display[1] as MosaicDisplayItem.BigTileItem
        val big2 = display[2] as MosaicDisplayItem.BigTileItem
        assertEquals("1", big1.item.id)
        assertEquals(2, big1.span)
        assertEquals("2", big2.item.id)
        assertEquals(2, big2.span)

        for (idx in 3 until display.size) {
            assertTrue(display[idx] is MosaicDisplayItem.SingleItem)
        }
    }

    @Test
    fun `supports 3-column Left Hero and Right Hero with strict row order`() {
        // 1. Left Hero (when position 0 is favorite)
        val feedLeft = listOf(
            GalleryListItem.Header("DayLeft"),
            item("A1"), item("A2"), item("A3")
        )
        val displayLeft = buildDateGridHeroItems(feedLeft.size, { feedLeft[it] }, columns = 3, favoriteIds = setOf("A1"))
        assertEquals(3, displayLeft.size)
        val bigLeft = displayLeft[1] as MosaicDisplayItem.BigTileItem
        val pairLeft = displayLeft[2] as MosaicDisplayItem.PairTileItem
        assertEquals("A1", bigLeft.item.id)
        assertEquals(2, bigLeft.span)
        assertEquals(listOf("A2", "A3"), pairLeft.items.map { it.id })

        // 2. Right Hero (when position 1 is favorite): user prompt #8 specification!
        // Row 1: Col 0 = B1, Cols 1-2 = B2 (Hero)
        // Row 2: Col 0 = B3, Cols 1-2 = B2 (Hero cont)
        val feedRight = listOf(
            GalleryListItem.Header("DayRight"),
            item("B1"), item("B2"), item("B3")
        )
        val displayRight = buildDateGridHeroItems(feedRight.size, { feedRight[it] }, columns = 3, favoriteIds = setOf("B2"))
        assertEquals(3, displayRight.size)
        val pairRight = displayRight[1] as MosaicDisplayItem.PairTileItem
        val bigRight = displayRight[2] as MosaicDisplayItem.BigTileItem
        assertEquals(listOf("B1", "B3"), pairRight.items.map { it.id })
        assertEquals("B2", bigRight.item.id)
        assertEquals(2, bigRight.span)
    }

    @Test
    fun `promotes highest quality face on row 1 when no favorite exists`() {
        val feed = listOf(
            GalleryListItem.Header("Sunday"),
            item("10"),
            item("11"),
            item("12"),
            item("13"),
            item("14")
        )
        // item("11") is position 1 -> Center Hero
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            faceQualityByMediaId = mapOf("11" to 0.95f)
        )

        val big = display[2] as MosaicDisplayItem.BigTileItem
        assertEquals("11", big.item.id)
    }

    @Test
    fun `paces two calm 1x1 rows between hero blocks`() {
        val feed = buildList {
            add(GalleryListItem.Header("Saturday"))
            for (i in 1..18) add(item(i.toString()))
        }
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4,
            favoriteIds = setOf("1", "14")
        )

        // Block 1: Left Hero for "1"
        assertEquals("1", (display[1] as MosaicDisplayItem.BigTileItem).item.id)
        assertTrue(display[2] is MosaicDisplayItem.QuadTileItem)

        // 8 calm singles (2 rows of 4)
        for (idx in 3..10) {
            assertTrue("Expected SingleItem at $idx", display[idx] is MosaicDisplayItem.SingleItem)
        }

        // Block 2 starts at index 11
        val hero2 = display.filterIsInstance<MosaicDisplayItem.BigTileItem>().last()
        assertEquals("14", hero2.item.id)
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
            screenRec
        )
        val display = buildDateGridHeroItems(
            totalCount = feed.size,
            itemAt = { feed[it] },
            columns = 4
        )

        for (idx in 1..5) {
            assertTrue("Expected SingleItem at $idx for screenshot", display[idx] is MosaicDisplayItem.SingleItem)
        }
        assertTrue("No BigTileItem should exist for screenshots", display.none { it is MosaicDisplayItem.BigTileItem })
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
    fun `companion tile keys update when items change and single cells avoid 720px override`() {
        val feed4 = listOf(
            GalleryListItem.Header("Day4"),
            item("1"), item("2"), item("3"), item("4"), item("5"), item("6")
        )
        val before4 = buildDateGridHeroItems(feed4.size, { feed4[it] }, columns = 4, favoriteIds = setOf("1"))
        val after4 = buildDateGridHeroItems(feed4.size, { feed4[it] }, columns = 4, favoriteIds = setOf("3"))

        val quadBefore = before4[2] as MosaicDisplayItem.QuadTileItem
        val quadAfter = after4[1] as MosaicDisplayItem.QuadTileItem
        assertNotEquals(quadBefore.key, quadAfter.key)

        val feed3 = listOf(
            GalleryListItem.Header("Day3"),
            item("1"), item("2"), item("3")
        )
        val before3 = buildDateGridHeroItems(feed3.size, { feed3[it] }, columns = 3, favoriteIds = setOf("1"))
        val after3 = buildDateGridHeroItems(feed3.size, { feed3[it] }, columns = 3, favoriteIds = setOf("2"))

        val pairBefore = before3[2] as MosaicDisplayItem.PairTileItem
        val pairAfter = after3[1] as MosaicDisplayItem.PairTileItem
        assertNotEquals(pairBefore.key, pairAfter.key)

        val big = before4[1] as MosaicDisplayItem.BigTileItem
        val single = before4[3] as MosaicDisplayItem.SingleItem
        assertEquals(720, mosaicCellTargetPxOverride(big))
        assertNull(mosaicCellTargetPxOverride(single))
    }
}
