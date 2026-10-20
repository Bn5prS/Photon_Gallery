package com.inferno.gallery.ui

import android.net.FakeUri
import com.inferno.gallery.data.AlbumLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumLayoutAndSortingTest {

    private fun createBucket(name: String, count: Int, maxDate: Long, sizeBytes: Long = 1000L): AlbumBucket {
        return AlbumBucket(
            bucketName = name,
            coverUri = FakeUri(),
            itemCount = count,
            totalSizeBytes = sizeBytes,
            maxDate = maxDate,
            defaultCoverUri = FakeUri()
        )
    }

    @Test
    fun testAlbumLayoutModeEnumValues() {
        val modes = AlbumLayoutMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(AlbumLayoutMode.GRID_2_COL))
        assertTrue(modes.contains(AlbumLayoutMode.GRID_3_COL))
        assertTrue(modes.contains(AlbumLayoutMode.LIST))
    }

    @Test
    fun testAlbumLayoutModeResolution() {
        fun resolveLayout(name: String?): AlbumLayoutMode = when (name) {
            AlbumLayoutMode.GRID_2_COL.name -> AlbumLayoutMode.GRID_2_COL
            AlbumLayoutMode.GRID_3_COL.name -> AlbumLayoutMode.GRID_3_COL
            AlbumLayoutMode.LIST.name -> AlbumLayoutMode.LIST
            else -> AlbumLayoutMode.GRID_2_COL
        }

        assertEquals(AlbumLayoutMode.GRID_2_COL, resolveLayout("GRID_2_COL"))
        assertEquals(AlbumLayoutMode.GRID_3_COL, resolveLayout("GRID_3_COL"))
        assertEquals(AlbumLayoutMode.LIST, resolveLayout("LIST"))
        assertEquals(AlbumLayoutMode.GRID_2_COL, resolveLayout(null))
        assertEquals(AlbumLayoutMode.GRID_2_COL, resolveLayout("UNKNOWN"))
    }

    @Test
    fun testAlbumSortingByName() {
        val b1 = createBucket("Vacation", 10, 1000L)
        val b2 = createBucket("animals", 25, 2000L)
        val b3 = createBucket("Family", 5, 3000L)
        val list = listOf(b1, b2, b3)

        val nameAsc = list.sortedBy { it.bucketName.lowercase() }
        assertEquals(listOf("animals", "Family", "Vacation"), nameAsc.map { it.bucketName })

        val nameDesc = list.sortedByDescending { it.bucketName.lowercase() }
        assertEquals(listOf("Vacation", "Family", "animals"), nameDesc.map { it.bucketName })
    }

    @Test
    fun testAlbumSortingByDateModified() {
        val b1 = createBucket("Album1", 10, 1000L)
        val b2 = createBucket("Album2", 25, 3000L)
        val b3 = createBucket("Album3", 5, 2000L)
        val list = listOf(b1, b2, b3)

        val dateDesc = list.sortedByDescending { it.maxDate }
        assertEquals(listOf("Album2", "Album3", "Album1"), dateDesc.map { it.bucketName })

        val dateAsc = list.sortedBy { it.maxDate }
        assertEquals(listOf("Album1", "Album3", "Album2"), dateAsc.map { it.bucketName })
    }

    @Test
    fun testAlbumSortingByItemCount() {
        val b1 = createBucket("Few", 3, 1000L)
        val b2 = createBucket("Many", 120, 2000L)
        val b3 = createBucket("Medium", 42, 3000L)
        val list = listOf(b1, b2, b3)

        val countDesc = list.sortedByDescending { it.itemCount }
        assertEquals(listOf("Many", "Medium", "Few"), countDesc.map { it.bucketName })

        val countAsc = list.sortedBy { it.itemCount }
        assertEquals(listOf("Few", "Medium", "Many"), countAsc.map { it.bucketName })
    }

    @Test
    fun testSortOrderEnumValues() {
        val orders = SortOrder.values()
        assertTrue(orders.contains(SortOrder.NameAsc))
        assertTrue(orders.contains(SortOrder.NameDesc))
        assertTrue(orders.contains(SortOrder.NewToOld))
        assertTrue(orders.contains(SortOrder.OldToNew))
        assertTrue(orders.contains(SortOrder.CountDesc))
        assertTrue(orders.contains(SortOrder.CountAsc))
    }
}
