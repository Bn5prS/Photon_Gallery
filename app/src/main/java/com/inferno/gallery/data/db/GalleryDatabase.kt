package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
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
    fun observeMediaEntitiesRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<List<CoreMediaEntity>>

    @androidx.room.RawQuery(observedEntities = [CoreMediaEntity::class])
    fun observeCountRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Flow<Int>

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

    @Query("""
        SELECT bucketName, COUNT(*) as itemCount, SUM(size) as totalSizeBytes, MAX(dateAdded) as maxDate, 
        (SELECT uriString FROM core_media c2 WHERE c2.bucketName = c1.bucketName AND c2.uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString, 
        (SELECT isVideo FROM core_media c2 WHERE c2.bucketName = c1.bucketName AND c2.uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as isVideo 
        FROM core_media c1 
        WHERE bucketName != 'Trash' AND bucketName IS NOT NULL 
          AND uriString NOT IN (SELECT originalUri FROM vault_media)
          AND filePath NOT IN (SELECT originalPath FROM vault_media)
        GROUP BY bucketName ORDER BY maxDate DESC
    """)
    fun observeBuckets(): Flow<List<BucketMetadata>>

    @Query("SELECT * FROM core_media WHERE uriString NOT IN (SELECT originalUri FROM vault_media) AND filePath NOT IN (SELECT originalPath FROM vault_media)")
    suspend fun getAllMedia(): List<CoreMediaEntity>

    /** Row count only — avoids materialising the whole table just to test emptiness. */
    @Query("SELECT COUNT(*) FROM core_media WHERE uriString NOT IN (SELECT originalUri FROM vault_media) AND filePath NOT IN (SELECT originalPath FROM vault_media)")
    suspend fun getAllMediaCount(): Int

    @Query("SELECT * FROM core_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): CoreMediaEntity?

    @Query("SELECT * FROM core_media WHERE uriString = :uriString LIMIT 1")
    suspend fun getMediaByUri(uriString: String): CoreMediaEntity?

    @Query("SELECT * FROM core_media WHERE filePath = :filePath LIMIT 1")
    suspend fun getMediaByPath(filePath: String): CoreMediaEntity?

    @Query("SELECT id FROM core_media WHERE uriString NOT IN (SELECT originalUri FROM vault_media) AND filePath NOT IN (SELECT originalPath FROM vault_media)")
    suspend fun getAllMediaIds(): List<Long>

    @Upsert
    suspend fun insert(media: CoreMediaEntity): Long

    @Upsert
    suspend fun insertAll(media: List<CoreMediaEntity>)

    @Query("DELETE FROM core_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM core_media WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("DELETE FROM core_media WHERE uriString IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("SELECT * FROM core_media WHERE uriString IN (:uris)")
    suspend fun getMediaByUrisList(uris: List<String>): List<CoreMediaEntity>

    @Query("UPDATE core_media SET bucketName = :bucket WHERE uriString = :uriString")
    suspend fun updateBucketByUri(uriString: String, bucket: String)

    @Query("UPDATE core_media SET bucketName = :bucket WHERE uriString IN (:uris)")
    suspend fun updateBucketByUris(uris: List<String>, bucket: String)

    @androidx.room.Transaction
    suspend fun updateBucketByUrisChunked(uris: List<String>, bucket: String) {
        uris.chunked(500).forEach { chunk ->
            updateBucketByUris(chunk, bucket)
        }
    }

    @androidx.room.Transaction
    suspend fun deleteByUrisChunked(uris: List<String>) {
        uris.chunked(500).forEach { chunk ->
            deleteByUris(chunk)
        }
    }

    @Query("UPDATE core_media SET filePath = :newPath, bucketName = :newBucket, name = :newName WHERE id = :id")
    suspend fun updatePathAndBucket(id: Long, newPath: String, newBucket: String, newName: String)

    @Query("UPDATE core_media SET is_indexed_ocr = :isIndexed WHERE id = :id")
    suspend fun updateOcrIndexStatus(id: Long, isIndexed: Boolean)

    @Query("UPDATE core_media SET is_indexed_ocr = 1 WHERE id IN (:ids)")
    suspend fun markOcrIndexed(ids: List<Long>)

    @Query("UPDATE core_media SET is_indexed_ocr = 0")
    suspend fun resetOcrIndexStatus()

    @Query("SELECT * FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    suspend fun getUnindexedOcrMedia(): List<CoreMediaEntity>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    suspend fun getTotalImageCount(): Int

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    suspend fun getUnindexedImageCount(): Int

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    fun observeTotalImageCount(): Flow<Int>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    fun observeUnindexedImageCount(): Flow<Int>

    @Query("SELECT COUNT(id) FROM core_media WHERE isVideo = 0 AND is_indexed_ocr = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
    fun observeUnindexedOcrImageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM core_media WHERE bucketName = 'Trash'")
    fun observeTrashCount(): Flow<Int>

    @Query("UPDATE core_media SET name = :newName WHERE id = :id")
    suspend fun updateMediaName(id: Long, newName: String)

    @Query("UPDATE core_media SET fileHash = :hash WHERE id = :id")
    suspend fun updateFileHash(id: Long, hash: String)

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate, 
        (SELECT uriString FROM core_media WHERE bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString 
        FROM core_media WHERE bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeAllMediaStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate, 
        (SELECT uriString FROM core_media WHERE isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString 
        FROM core_media WHERE isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeVideoStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT * FROM core_media cm
        WHERE strftime('%m', datetime(CASE WHEN cm.dateAdded > 100000000000 THEN cm.dateAdded / 1000 ELSE cm.dateAdded END, 'unixepoch', 'localtime')) = :month
          AND strftime('%d', datetime(CASE WHEN cm.dateAdded > 100000000000 THEN cm.dateAdded / 1000 ELSE cm.dateAdded END, 'unixepoch', 'localtime')) = :day
          AND strftime('%Y', datetime(CASE WHEN cm.dateAdded > 100000000000 THEN cm.dateAdded / 1000 ELSE cm.dateAdded END, 'unixepoch', 'localtime')) != :excludeYear
          AND cm.bucketName != 'Trash'
          AND cm.bucketName NOT IN ('Screenshots', 'Screenshot', 'Download', 'Downloads')
          AND cm.filePath NOT LIKE '%/Screenshots/%'
          AND cm.filePath NOT LIKE '%/Screenshot/%'
          AND cm.size >= 50000
          AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
          AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)
        ORDER BY cm.dateAdded DESC
    """)
    suspend fun getOnThisDayMedia(month: String, day: String, excludeYear: String): List<CoreMediaEntity>

    @Query("""
        SELECT * FROM core_media cm
        WHERE cm.bucketName != 'Trash'
          AND cm.bucketName NOT IN ('Trash', 'Screenshots', 'Screenshot')
          AND cm.filePath NOT LIKE '%/Screenshots/%'
          AND cm.filePath NOT LIKE '%/Screenshot/%'
          AND cm.size >= 30000
          AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
          AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)
        ORDER BY cm.dateAdded DESC
        LIMIT :limit
    """)
    suspend fun getRecentHighlightMedia(limit: Int = 120): List<CoreMediaEntity>

    @Query("""
        SELECT * FROM core_media cm
        WHERE cm.bucketName != 'Trash'
          AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
          AND cm.filePath NOT IN (SELECT originalPath FROM vault_media)
        ORDER BY cm.dateAdded DESC
        LIMIT :limit
    """)
    suspend fun getAnyRecentMedia(limit: Int = 60): List<CoreMediaEntity>


    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (mimeType LIKE '%raw%' OR mimeType LIKE '%dng%' OR name LIKE '%.dng' OR name LIKE '%.raw' OR name LIKE '%.cr2' OR name LIKE '%.nef' OR name LIKE '%.arw' OR name LIKE '%.orf' OR name LIKE '%.raf') AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (mimeType LIKE '%raw%' OR mimeType LIKE '%dng%' OR name LIKE '%.dng' OR name LIKE '%.raw' OR name LIKE '%.cr2' OR name LIKE '%.nef' OR name LIKE '%.arw' OR name LIKE '%.orf' OR name LIKE '%.raf') AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeRawStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (
            (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%')
            AND (name NOT LIKE '%PHOTOSPHERE%' AND name NOT LIKE '%_SPHERE%' AND name NOT LIKE '%SPHERE_%' AND name NOT LIKE '%360_PHOTO%' AND name NOT LIKE '%_360.%' AND filePath NOT LIKE '%/PhotoSphere/%' AND filePath NOT LIKE '%/360/%')
            AND id NOT IN (SELECT mediaId FROM photo_spheres WHERE isSphere = 1)
        ) AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (
            (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%')
            AND (name NOT LIKE '%PHOTOSPHERE%' AND name NOT LIKE '%_SPHERE%' AND name NOT LIKE '%SPHERE_%' AND name NOT LIKE '%360_PHOTO%' AND name NOT LIKE '%_360.%' AND filePath NOT LIKE '%/PhotoSphere/%' AND filePath NOT LIKE '%/360/%')
            AND id NOT IN (SELECT mediaId FROM photo_spheres WHERE isSphere = 1)
        ) AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observePanoramaStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%SLOW%' OR name LIKE '%HFR%' OR filePath LIKE '%slow_motion%') AND isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%SLOW%' OR name LIKE '%HFR%' OR filePath LIKE '%slow_motion%') AND isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeSlowMoStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (mimeType = 'image/gif' OR name LIKE '%.gif') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (mimeType = 'image/gif' OR name LIKE '%.gif') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeAnimationStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%_NIGHT%' OR name LIKE '%NIGHT_%' OR name LIKE '%NIGHT.%' OR name LIKE '%NIGHTSIGHT%' OR filePath LIKE '%/Night/%' OR filePath LIKE '%/NightMode/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%_NIGHT%' OR name LIKE '%NIGHT_%' OR name LIKE '%NIGHT.%' OR name LIKE '%NIGHTSIGHT%' OR filePath LIKE '%/Night/%' OR filePath LIKE '%/NightMode/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeNightStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (
            (name LIKE '%PHOTOSPHERE%' OR name LIKE '%_SPHERE%' OR name LIKE '%SPHERE_%' OR name LIKE '%360_PHOTO%' OR name LIKE '%_360.%' OR filePath LIKE '%/PhotoSphere/%' OR filePath LIKE '%/360/%')
            OR id IN (SELECT mediaId FROM photo_spheres WHERE isSphere = 1)
        ) AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (
            (name LIKE '%PHOTOSPHERE%' OR name LIKE '%_SPHERE%' OR name LIKE '%SPHERE_%' OR name LIKE '%360_PHOTO%' OR name LIKE '%_360.%' OR filePath LIKE '%/PhotoSphere/%' OR filePath LIKE '%/360/%')
            OR id IN (SELECT mediaId FROM photo_spheres WHERE isSphere = 1)
        ) AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observePhotoSphereStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%PORTRAIT%' OR name LIKE '%_BOKEH%' OR name LIKE '%_DEPTH%' OR filePath LIKE '%/Portrait/%' OR filePath LIKE '%/Portraits/%' OR filePath LIKE '%_PORTRAIT/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%PORTRAIT%' OR name LIKE '%_BOKEH%' OR name LIKE '%_DEPTH%' OR filePath LIKE '%/Portrait/%' OR filePath LIKE '%/Portraits/%' OR filePath LIKE '%_PORTRAIT/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observePortraitStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%MACRO%' OR name LIKE '%_MACRO%' OR filePath LIKE '%/Macro/%' OR filePath LIKE '%/SuperMacro/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%MACRO%' OR name LIKE '%_MACRO%' OR filePath LIKE '%/Macro/%' OR filePath LIKE '%/SuperMacro/%') AND isVideo = 0 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeMacroStats(): Flow<MediaAggregateStats>

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(size), 0) as totalSizeBytes, COALESCE(MAX(dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media WHERE (name LIKE '%TIMELAPSE%' OR name LIKE '%HYPERLAPSE%' OR name LIKE '%TIME_LAPSE%' OR name LIKE 'TL_%' OR name LIKE '%_TL.mp4' OR filePath LIKE '%/Timelapse/%' OR filePath LIKE '%/Hyperlapse/%') AND isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media
        WHERE (name LIKE '%TIMELAPSE%' OR name LIKE '%HYPERLAPSE%' OR name LIKE '%TIME_LAPSE%' OR name LIKE 'TL_%' OR name LIKE '%_TL.mp4' OR filePath LIKE '%/Timelapse/%' OR filePath LIKE '%/Hyperlapse/%') AND isVideo = 1 AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeTimelapseStats(): Flow<MediaAggregateStats>

    @Query("SELECT uriString FROM core_media WHERE bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY dateAdded DESC LIMIT 4")
    fun observeTopCoverUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoSpheres(entities: List<PhotoSphereEntity>)

    @Query("""
        SELECT id, filePath, uriString FROM core_media
        WHERE (name LIKE '%PANO%' OR name LIKE '%PANORAMA%' OR filePath LIKE '%PANO%' OR filePath LIKE '%panorama%')
          AND isVideo = 0
          AND id NOT IN (SELECT mediaId FROM photo_spheres)
        LIMIT 100
    """)
    suspend fun getUncheckedSphereCandidates(): List<SphereCandidate>

    @Transaction
    @Query("SELECT * FROM core_media WHERE size IN (SELECT size FROM core_media WHERE bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media) AND size > 0 GROUP BY size HAVING COUNT(id) > 1) AND bucketName != 'Trash' AND uriString NOT IN (SELECT originalUri FROM vault_media)")
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

    @Query("""
        SELECT COUNT(*) as itemCount, COALESCE(SUM(cm.size), 0) as totalSizeBytes, COALESCE(MAX(cm.dateAdded), 0) as maxDate,
        (SELECT uriString FROM core_media c2 JOIN document_classifications dc2 ON c2.id = dc2.mediaId WHERE dc2.category = :category AND c2.bucketName != 'Trash' AND c2.uriString NOT IN (SELECT originalUri FROM vault_media) ORDER BY c2.dateAdded DESC LIMIT 1) as coverUriString
        FROM core_media cm
        JOIN document_classifications dc ON cm.id = dc.mediaId
        WHERE dc.category = :category AND cm.bucketName != 'Trash' AND cm.uriString NOT IN (SELECT originalUri FROM vault_media)
    """)
    fun observeDocumentCategoryStats(category: String): Flow<MediaAggregateStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentClassifications(items: List<DocumentClassificationEntity>)

    @Query("DELETE FROM document_classifications WHERE mediaId = :mediaId")
    suspend fun deleteDocumentClassificationsForMedia(mediaId: Long)

    @Query("DELETE FROM document_classifications")
    suspend fun clearDocumentClassifications()

    @Query("SELECT COUNT(*) FROM document_classifications")
    suspend fun getDocumentClassificationsCount(): Int
}

@Database(
    entities = [
        CoreMediaEntity::class,
        MediaEmbeddingEntity::class,
        MediaEmbeddingStatusEntity::class,
        VaultMediaEntity::class,
        GeocodedLocation::class,
        PhotoSphereEntity::class,
        PersonEntity::class,
        FaceEntity::class,
        MediaFaceStatusEntity::class,
        PersonMergeSuggestionEntity::class,
        DocumentClassificationEntity::class
        // ImageFtsEntity is intentionally excluded: @Fts5 has a KSP 2.2.x incompatibility.
        // The FTS5 virtual table is instead created manually in DatabaseProvider's
        // RoomDatabase.Callback onCreate hook using raw CREATE VIRTUAL TABLE SQL.
    ],
    version = 29,
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

