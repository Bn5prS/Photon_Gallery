package com.inferno.gallery.data

import android.net.FakeUri
import android.net.Uri
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.MediaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MemoriesManagerTest {

    private fun createEntity(
        id: Long,
        yearsAgo: Int,
        bucketName: String = "Camera",
        name: String = "photo_$id.jpg"
    ): CoreMediaEntity {
        val cal = Calendar.getInstance().apply {
            add(Calendar.YEAR, -yearsAgo)
        }
        val sec = cal.timeInMillis / 1000L

        return CoreMediaEntity(
            id = id,
            uriString = "content://media/external/images/media/$id",
            filePath = "/storage/emulated/0/DCIM/Camera/$name",
            bucketName = bucketName,
            dateAdded = sec,
            dateModified = sec,
            size = 102400L,
            name = name,
            mimeType = "image/jpeg",
            isVideo = false,
            durationMs = null
        )
    }

    private class FakeMediaDao(
        private val mediaList: List<CoreMediaEntity>
    ) : MediaDao {
        override suspend fun getOnThisDayMedia(month: String, day: String, excludeYear: String): List<CoreMediaEntity> {
            val exYear = excludeYear.toIntOrNull() ?: -1
            return mediaList.filter { entity ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = entity.dateAdded * 1000L
                }
                val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
                val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
                val y = cal.get(Calendar.YEAR)
                m == month && d == day && y != exYear
            }
        }

        override fun observeAllMedia(): Flow<List<CoreMediaEntity>> = emptyFlow()
        override fun observeMediaPaging(bucketName: String?): androidx.paging.PagingSource<Int, CoreMediaEntity> = throw NotImplementedError()
        override fun observeMediaPagingRaw(query: androidx.sqlite.db.SupportSQLiteQuery): androidx.paging.PagingSource<Int, com.inferno.gallery.data.db.CoreMediaProjection> = throw NotImplementedError()
        override suspend fun getMediaRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<com.inferno.gallery.data.db.CoreMediaProjection> = emptyList()
        override fun observeMediaEntitiesRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<List<CoreMediaEntity>> = emptyFlow()
        override fun observeCountRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun getUrisRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<String> = emptyList()
        override fun observeAllBucketNames(): Flow<List<String>> = emptyFlow()
        override fun observeBucketInfo(): Flow<List<com.inferno.gallery.data.db.BucketInfo>> = emptyFlow()
        override fun observeMediaByIds(ids: List<Long>): Flow<List<CoreMediaEntity>> = emptyFlow()
        override suspend fun getMediaByIdsList(ids: List<Long>): List<CoreMediaEntity> = emptyList()
        override fun observeBuckets(): Flow<List<com.inferno.gallery.data.db.BucketMetadata>> = emptyFlow()
        override suspend fun getAllMedia(): List<CoreMediaEntity> = emptyList()
        override suspend fun getAllMediaCount(): Int = 0
        override suspend fun getMediaById(id: Long): CoreMediaEntity? = null
        override suspend fun getMediaByUri(uriString: String): CoreMediaEntity? = null
        override suspend fun getAllMediaIds(): List<Long> = emptyList()
        override suspend fun insert(media: CoreMediaEntity): Long = 0L
        override suspend fun insertAll(media: List<CoreMediaEntity>) {}
        override suspend fun deleteByIds(ids: List<Long>) {}
        override suspend fun deleteByUri(uriString: String) {}
        override suspend fun updateBucketByUri(uriString: String, bucket: String) {}
        override suspend fun updatePathAndBucket(id: Long, newPath: String, newBucket: String, newName: String) {}
        override suspend fun updateOcrIndexStatus(id: Long, isIndexed: Boolean) {}
        override suspend fun markOcrIndexed(ids: List<Long>) {}
        override suspend fun resetOcrIndexStatus() {}
        override suspend fun getUnindexedOcrMedia(): List<CoreMediaEntity> = emptyList()
        override suspend fun getTotalImageCount(): Int = 0
        override suspend fun getUnindexedImageCount(): Int = 0
        override fun observeTotalImageCount(): Flow<Int> = emptyFlow()
        override fun observeUnindexedImageCount(): Flow<Int> = emptyFlow()
        override fun observeUnindexedOcrImageCount(): Flow<Int> = emptyFlow()
        override fun observeTrashCount(): Flow<Int> = emptyFlow()
        override suspend fun updateMediaName(id: Long, newName: String) {}
        override suspend fun updateFileHash(id: Long, hash: String) {}
        override fun observeAllMediaStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeVideoStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeRawStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observePanoramaStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeSlowMoStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeAnimationStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeNightStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observePhotoSphereStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observePortraitStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeMacroStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeTimelapseStats(): Flow<com.inferno.gallery.data.db.MediaAggregateStats> = emptyFlow()
        override fun observeTopCoverUris(): Flow<List<String>> = emptyFlow()
        override suspend fun getMediaSharingSameSize(): List<CoreMediaEntity> = emptyList()
        override fun observeExactDuplicates(): Flow<List<CoreMediaEntity>> = emptyFlow()
        override suspend fun getGeotaggedMedia(): List<CoreMediaEntity> = emptyList()
        override suspend fun getMediaNeedingGps(): List<CoreMediaEntity> = emptyList()
        override suspend fun getRecentHighlightMedia(limit: Int): List<CoreMediaEntity> = mediaList.filter { it.bucketName != "Screenshots" }.take(limit)
        override suspend fun getAnyRecentMedia(limit: Int): List<CoreMediaEntity> = mediaList.take(limit)
        override suspend fun getGeotaggedCount(): Int = 0
    }

    @Test
    fun testMemoriesGroupingByYear() = runBlocking {
        val fakeEntities = listOf(
            createEntity(id = 1L, yearsAgo = 1),
            createEntity(id = 2L, yearsAgo = 1),
            createEntity(id = 3L, yearsAgo = 3),
            createEntity(id = 4L, yearsAgo = 5)
        )
        val dao = FakeMediaDao(fakeEntities)

        val memories = MemoriesManager.loadOnThisDayMemories(
            mediaDao = dao,
            excludedFolders = emptySet(),
            favoriteIds = emptySet(),
            uriResolver = { uriStr, _ -> FakeUri(uriStr) }
        )

        assertEquals(3, memories.size)
        assertEquals("1 Year Ago Today", memories[0].title)
        assertEquals(2, memories[0].items.size)
        assertEquals(1, memories[0].yearsAgo)

        assertEquals("3 Years Ago Today", memories[1].title)
        assertEquals(1, memories[1].items.size)
        assertEquals(3, memories[1].yearsAgo)

        assertEquals("5 Years Ago Today", memories[2].title)
        assertEquals(1, memories[2].items.size)
        assertEquals(5, memories[2].yearsAgo)
    }

    @Test
    fun testSmartFallbackWhenNoPastYearMemories() = runBlocking {
        // Photos only from the current year (yearsAgo = 0)
        val fakeEntities = listOf(
            createEntity(id = 100L, yearsAgo = 0, bucketName = "Camera"),
            createEntity(id = 101L, yearsAgo = 0, bucketName = "Camera"),
            createEntity(id = 102L, yearsAgo = 0, bucketName = "WhatsApp Images")
        )
        val dao = FakeMediaDao(fakeEntities)

        val memories = MemoriesManager.loadOnThisDayMemories(
            mediaDao = dao,
            excludedFolders = emptySet(),
            favoriteIds = emptySet(),
            uriResolver = { uriStr, _ -> FakeUri(uriStr) }
        )

        // Must produce at least 1 smart fallback highlight card
        assertTrue(memories.isNotEmpty())
        assertEquals("Recent Highlights", memories[0].title)
        assertEquals(0, memories[0].yearsAgo)
        assertFalse(memories[0].isExactDay)
        assertEquals(3, memories[0].items.size)
    }

    @Test
    fun testExcludedFoldersAreFiltered() = runBlocking {
        val fakeEntities = listOf(
            createEntity(id = 1L, yearsAgo = 2, bucketName = "Camera"),
            createEntity(id = 2L, yearsAgo = 2, bucketName = "SecretFolder")
        )
        val dao = FakeMediaDao(fakeEntities)

        val memories = MemoriesManager.loadOnThisDayMemories(
            mediaDao = dao,
            excludedFolders = setOf("SecretFolder"),
            favoriteIds = emptySet(),
            uriResolver = { uriStr, _ -> FakeUri(uriStr) }
        )

        assertEquals(1, memories.size)
        assertEquals(1, memories[0].items.size)
        assertEquals("1", memories[0].items[0].id)
    }

    @Test
    fun testFavoritePhotoIsPrioritizedAsCover() = runBlocking {
        val fakeEntities = listOf(
            createEntity(id = 10L, yearsAgo = 1),
            createEntity(id = 20L, yearsAgo = 1),
            createEntity(id = 30L, yearsAgo = 1)
        )
        val dao = FakeMediaDao(fakeEntities)

        // Make photo with id 20 a favorite
        val memories = MemoriesManager.loadOnThisDayMemories(
            mediaDao = dao,
            excludedFolders = emptySet(),
            favoriteIds = setOf("20"),
            uriResolver = { uriStr, _ -> FakeUri(uriStr) }
        )

        assertEquals(1, memories.size)
        assertEquals("20", memories[0].coverItem.id)
    }
}
