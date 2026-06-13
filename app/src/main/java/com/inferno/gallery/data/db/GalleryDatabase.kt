package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface MediaDao {
    @Query("SELECT * FROM core_media ORDER BY dateAdded DESC")
    fun observeAllMedia(): Flow<List<CoreMediaEntity>>

    @Query("SELECT * FROM core_media WHERE (:bucketName IS NULL OR bucketName = :bucketName) ORDER BY dateAdded DESC")
    fun observeMediaPaging(bucketName: String? = null): PagingSource<Int, CoreMediaEntity>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class, TelegramBackupEntity::class])
    fun observeMediaPagingRaw(query: androidx.sqlite.db.SupportSQLiteQuery): PagingSource<Int, MediaWithBackup>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class, TelegramBackupEntity::class])
    suspend fun getMediaRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<MediaWithBackup>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class, TelegramBackupEntity::class])
    suspend fun getUrisRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT bucketName FROM core_media WHERE bucketName != 'Trash' AND bucketName IS NOT NULL")
    fun observeAllBucketNames(): Flow<List<String>>

    @Query("SELECT * FROM core_media WHERE id IN (:ids) ORDER BY dateAdded DESC")
    fun observeMediaByIds(ids: List<Long>): Flow<List<CoreMediaEntity>>

    @Query("SELECT * FROM core_media WHERE id IN (:ids)")
    suspend fun getMediaByIdsList(ids: List<Long>): List<CoreMediaEntity>

    @Query("SELECT bucketName, COUNT(*) as itemCount, SUM(size) as totalSizeBytes, MAX(dateAdded) as maxDate, (SELECT uriString FROM core_media c2 WHERE c2.bucketName = c1.bucketName ORDER BY dateAdded DESC LIMIT 1) as coverUriString, (SELECT isVideo FROM core_media c2 WHERE c2.bucketName = c1.bucketName ORDER BY dateAdded DESC LIMIT 1) as isVideo FROM core_media c1 WHERE bucketName != 'Trash' AND bucketName IS NOT NULL GROUP BY bucketName ORDER BY maxDate DESC")
    fun observeBuckets(): Flow<List<BucketMetadata>>

    @Query("SELECT * FROM core_media")
    suspend fun getAllMedia(): List<CoreMediaEntity>

    @Query("SELECT * FROM core_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): CoreMediaEntity?

    @Query("SELECT id FROM core_media")
    suspend fun getAllMediaIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<CoreMediaEntity>)

    @Query("DELETE FROM core_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("DELETE FROM core_media WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)
    
    @Query("UPDATE core_media SET bucketName = :bucket WHERE uriString = :uriString")
    suspend fun updateBucketByUri(uriString: String, bucket: String)
    
    @Query("UPDATE core_media SET is_indexed_ocr = :isIndexed WHERE id = :id")
    suspend fun updateOcrIndexStatus(id: Long, isIndexed: Boolean)

    @Query("UPDATE core_media SET is_indexed_ocr = 1 WHERE id IN (:ids)")
    suspend fun markOcrIndexed(ids: List<Long>)

    @Query("UPDATE core_media SET is_indexed_ocr = 0")
    suspend fun resetOcrIndexStatus()

    @Query("SELECT * FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0")
    suspend fun getUnindexedOcrMedia(): List<CoreMediaEntity>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0")
    suspend fun getTotalImageCount(): Int

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0")
    suspend fun getUnindexedImageCount(): Int

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0")
    fun observeTotalImageCount(): Flow<Int>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0")
    fun observeUnindexedImageCount(): Flow<Int>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0")
    fun observeUnindexedOcrImageCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM core_media WHERE bucketName = 'Trash'")
    fun observeTrashCount(): Flow<Int>

    @Query("UPDATE core_media SET name = :newName WHERE id = :id")
    suspend fun updateMediaName(id: Long, newName: String)
}

@Database(
    entities = [
        CoreMediaEntity::class,
        TelegramBackupEntity::class,
        MediaEmbeddingEntity::class
        // ImageFtsEntity is intentionally excluded: @Fts5 has a KSP 2.2.x incompatibility.
        // The FTS5 virtual table is instead created manually in DatabaseProvider's
        // RoomDatabase.Callback onCreate hook using raw CREATE VIRTUAL TABLE SQL.
    ],
    version = 6,
    exportSchema = false
)
@androidx.room.TypeConverters(EmbeddingConverter::class)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun telegramBackupDao(): TelegramBackupDao
    abstract fun embeddingDao(): EmbeddingDao
}
