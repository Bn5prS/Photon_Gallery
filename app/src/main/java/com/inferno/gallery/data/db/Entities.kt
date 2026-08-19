package com.inferno.gallery.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "core_media")
data class CoreMediaEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val filePath: String,
    val bucketName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val name: String,
    val mimeType: String?,
    val isVideo: Boolean,
    val durationMs: Long?,
    @ColumnInfo(name = "is_indexed_ocr") val isIndexedOcr: Boolean = false,
    val pHash: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fileHash: String? = null,
    // Deprecated, no longer populated or used by the app.
    // Kept to avoid complex Room DB table-recreation migrations.
    val faceFocalX: Float? = null,
    val faceFocalY: Float? = null
)

typealias MediaEntity = CoreMediaEntity



// NOTE: ImageFtsEntity / @Fts5 are intentionally absent.
// The image_fts FTS5 virtual table is created via raw SQL in DatabaseProvider's
// RoomDatabase.Callback.onCreate() hook, bypassing Room's KSP annotation processor
// which has a KSP 2.2.x incompatibility with @Fts5. All FTS reads/writes go through
// DatabaseProvider.insertFtsRow() and DatabaseProvider.searchFts() using openHelper directly.



data class BucketMetadata(
    val bucketName: String,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val maxDate: Long,
    val coverUriString: String?,
    val isVideo: Boolean?
)

data class MediaAggregateStats(
    val itemCount: Int,
    val totalSizeBytes: Long,
    val maxDate: Long,
    val coverUriString: String?
)

data class BucketInfo(
    val bucketName: String,
    val itemCount: Int,
    val representativeUri: String
)



@Entity(tableName = "geocoded_locations")
data class GeocodedLocation(
    @PrimaryKey val mediaId: Long,
    val placeName: String,
    val adminArea: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

// CoreMediaProjection: thin projection returned by the paging DAO query (SELECT cm.*).
// All fields map directly to core_media columns.
data class CoreMediaProjection(
    val id: Long,
    val uriString: String,
    val filePath: String,
    val bucketName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val name: String,
    val mimeType: String?,
    val isVideo: Boolean,
    val durationMs: Long?,
    @ColumnInfo(name = "is_indexed_ocr") val isIndexedOcr: Boolean = false,
    val pHash: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
