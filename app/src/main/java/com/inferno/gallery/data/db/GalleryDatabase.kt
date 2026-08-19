package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface MediaDao {
    @Query("SELECT * FROM core_media ORDER BY dateAdded DESC")
    fun observeAllMedia(): Flow<List<CoreMediaEntity>>

    @Query("SELECT * FROM core_media WHERE (:bucketName IS NULL OR bucketName = :bucketName) ORDER BY dateAdded DESC")
    fun observeMediaPaging(bucketName: String? = null): PagingSource<Int, CoreMediaEntity>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class])
    fun observeMediaPagingRaw(query: androidx.sqlite.db.SupportSQLiteQuery): PagingSource<Int, CoreMediaProjection>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class])
    suspend fun getMediaRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<CoreMediaProjection>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class])
    suspend fun getUrisRaw(query: androidx.sqlite.db.SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT bucketName FROM core_media WHERE bucketName != 'Trash' AND bucketName IS NOT NULL")
    fun observeAllBucketNames(): Flow<List<String>>

    @Query("""
        SELECT bucketName, COUNT(*) as itemCount, MAX(uriString) as representativeUri 
        FROM core_media 
        WHERE bucketName != 'Trash' AND bucketName IS NOT NULL 
        GROUP BY bucketName 
        ORDER BY bucketName ASC
    """)
    fun observeBucketInfo(): Flow<List<com.inferno.gallery.data.db.BucketInfo>>

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

    @Query("SELECT * FROM core_media WHERE uriString = :uriString LIMIT 1")
    suspend fun getMediaByUri(uriString: String): CoreMediaEntity?

    @Query("SELECT id FROM core_media")
    suspend fun getAllMediaIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: CoreMediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<CoreMediaEntity>)

    @Query("DELETE FROM core_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM core_media WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("UPDATE core_media SET bucketName = :bucket WHERE uriString = :uriString")
    suspend fun updateBucketByUri(uriString: String, bucket: String)

    @Query("UPDATE core_media SET filePath = :newPath, bucketName = :newBucket, name = :newName WHERE id = :id")
    suspend fun updatePathAndBucket(id: Long, newPath: String, newBucket: String, newName: String)

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

    @Query("UPDATE core_media SET fileHash = :hash WHERE id = :id")
    suspend fun updateFileHash(id: Long, hash: String)


    @Query("SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate, (SELECT uriString FROM core_media WHERE bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString FROM core_media WHERE bucketName != 'Trash'")
    fun observeAllMediaStats(): Flow<MediaAggregateStats>

    @Query("SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate, (SELECT uriString FROM core_media WHERE isVideo = 1 AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString FROM core_media WHERE isVideo = 1 AND bucketName != 'Trash'")
    fun observeVideoStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (mimeType LIKE '%raw%' OR mimeType LIKE '%dng%' OR name LIKE '%.dng' OR name LIKE '%.raw' OR name LIKE '%.cr2' OR name LIKE '%.nef' OR name LIKE '%.arw' OR name LIKE '%.orf' OR name LIKE '%.raf') AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (mimeType LIKE '%raw%' OR mimeType LIKE '%dng%' OR name LIKE '%.dng' OR name LIKE '%.raw' OR name LIKE '%.cr2' OR name LIKE '%.nef' OR name LIKE '%.arw' OR name LIKE '%.orf' OR name LIKE '%.raf') AND bucketName != 'Trash'
    """)
    fun observeRawStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%') AND isVideo = 0 AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%') AND isVideo = 0 AND bucketName != 'Trash'
    """)
    fun observePanoramaStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%SLOW%' OR name LIKE '%HFR%' OR filePath LIKE '%slow_motion%') AND isVideo = 1 AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%SLOW%' OR name LIKE '%HFR%' OR filePath LIKE '%slow_motion%') AND isVideo = 1 AND bucketName != 'Trash'
    """)
    fun observeSlowMoStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (mimeType = 'image/gif' OR name LIKE '%.gif') AND isVideo = 0 AND bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (mimeType = 'image/gif' OR name LIKE '%.gif') AND isVideo = 0 AND bucketName != 'Trash'
    """)
    fun observeAnimationStats(): Flow<MediaAggregateStats>

    @Query("SELECT uriString FROM core_media WHERE bucketName != 'Trash' ORDER BY dateAdded DESC LIMIT 4")
    fun observeTopCoverUris(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM core_media WHERE size IN (SELECT size FROM core_media WHERE bucketName != 'Trash' AND size > 0 GROUP BY size HAVING COUNT(id) > 1) AND bucketName != 'Trash'")
    suspend fun getMediaSharingSameSize(): List<CoreMediaEntity>

    @Transaction
    @Query("SELECT * FROM core_media WHERE fileHash IN (SELECT fileHash FROM core_media WHERE fileHash IS NOT NULL AND bucketName != 'Trash' GROUP BY fileHash HAVING COUNT(id) > 1) AND bucketName != 'Trash' ORDER BY fileHash, dateAdded DESC")
    fun observeExactDuplicates(): Flow<List<CoreMediaEntity>>

    @Query("SELECT * FROM core_media WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND bucketName != 'Trash' ORDER BY dateAdded DESC")
    suspend fun getGeotaggedMedia(): List<CoreMediaEntity>

    @Query("SELECT * FROM core_media WHERE latitude IS NULL AND isVideo = 0 AND bucketName != 'Trash'")
    suspend fun getMediaNeedingGps(): List<CoreMediaEntity>

    @Query("SELECT COUNT(*) FROM core_media WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND bucketName != 'Trash'")
    suspend fun getGeotaggedCount(): Int
}

@Database(
    entities = [
        CoreMediaEntity::class,
        MediaEmbeddingEntity::class,
        MediaEmbeddingStatusEntity::class,
        VaultMediaEntity::class,
        GeocodedLocation::class,
        FaceEntity::class
        // ImageFtsEntity is intentionally excluded: @Fts5 has a KSP 2.2.x incompatibility.
        // The FTS5 virtual table is instead created manually in DatabaseProvider's
        // RoomDatabase.Callback onCreate hook using raw CREATE VIRTUAL TABLE SQL.
    ],
    version = 21,
    exportSchema = true
)
@androidx.room.TypeConverters(EmbeddingConverter::class)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun embeddingStatusDao(): EmbeddingStatusDao
    abstract fun vaultDao(): VaultDao
    abstract fun placesDao(): PlacesDao
    abstract fun faceDao(): FaceDao
}

