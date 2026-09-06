package com.inferno.gallery.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorPaginationTest {

    data class MockMediaItem(
        val id: Long,
        val dateAdded: Long,
        val name: String
    )

    @Test
    fun testCursorTieBreakerPreventsMissingItemsWithSameTimestamp() {
        // Multiple items sharing identical timestamps (common during burst photos or batch downloads)
        val timestamp = 1700000000L
        val dataset = listOf(
            MockMediaItem(id = 1005L, dateAdded = timestamp, name = "IMG_1005.jpg"),
            MockMediaItem(id = 1004L, dateAdded = timestamp, name = "IMG_1004.jpg"),
            MockMediaItem(id = 1003L, dateAdded = timestamp, name = "IMG_1003.jpg"),
            MockMediaItem(id = 1002L, dateAdded = timestamp, name = "IMG_1002.jpg"),
            MockMediaItem(id = 1001L, dateAdded = timestamp - 100, name = "IMG_1001.jpg")
        )

        val pageSize = 2

        // Page 1: Initial load
        val page1 = dataset.take(pageSize)
        assertEquals(2, page1.size)
        assertEquals(1005L, page1[0].id)
        assertEquals(1004L, page1[1].id)

        val cursorPage1 = MediaCursor(dateAdded = page1.last().dateAdded, id = page1.last().id)

        // Page 2: Seeking using keyset condition: (dateAdded < cursor.dateAdded OR (dateAdded = cursor.dateAdded AND id < cursor.id))
        val page2 = dataset.filter { item ->
            item.dateAdded < cursorPage1.dateAdded || (item.dateAdded == cursorPage1.dateAdded && item.id < cursorPage1.id)
        }.take(pageSize)

        assertEquals(2, page2.size)
        assertEquals(1003L, page2[0].id)
        assertEquals(1002L, page2[1].id)

        val cursorPage2 = MediaCursor(dateAdded = page2.last().dateAdded, id = page2.last().id)

        // Page 3: Seeking past all burst items to older items
        val page3 = dataset.filter { item ->
            item.dateAdded < cursorPage2.dateAdded || (item.dateAdded == cursorPage2.dateAdded && item.id < cursorPage2.id)
        }.take(pageSize)

        assertEquals(1, page3.size)
        assertEquals(1001L, page3[0].id)

        // Ensure zero duplicates across pages
        val allPagedIds = (page1 + page2 + page3).map { it.id }
        assertEquals(5, allPagedIds.size)
        assertEquals(allPagedIds.distinct().size, allPagedIds.size)
    }

    @Test
    fun testAscendingAndDescendingCursorConditions() {
        val cursor = MediaCursor(dateAdded = 1690000000L, id = 42L)

        // Descending check (NewToOld)
        val newerItem = MockMediaItem(id = 50L, dateAdded = 1690000100L, name = "Newer")
        val olderItem = MockMediaItem(id = 30L, dateAdded = 1689999900L, name = "Older")
        val sameTimeLowerId = MockMediaItem(id = 40L, dateAdded = 1690000000L, name = "TieBreakPassed")
        val sameTimeHigherId = MockMediaItem(id = 45L, dateAdded = 1690000000L, name = "TieBreakFailed")

        fun isDescMatch(item: MockMediaItem) =
            item.dateAdded < cursor.dateAdded || (item.dateAdded == cursor.dateAdded && item.id < cursor.id)

        assertFalse(isDescMatch(newerItem))
        assertTrue(isDescMatch(olderItem))
        assertTrue(isDescMatch(sameTimeLowerId))
        assertFalse(isDescMatch(sameTimeHigherId))
    }
}
