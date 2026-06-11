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
    @ColumnInfo(name = "is_indexed_ocr") val isIndexedOcr: Boolean = false
)



// NOTE: ImageFtsEntity / @Fts5 are intentionally absent.
// The image_fts FTS5 virtual table is created via raw SQL in DatabaseProvider's
// RoomDatabase.Callback.onCreate() hook, bypassing Room's KSP annotation processor
// which has a KSP 2.2.x incompatibility with @Fts5. All FTS reads/writes go through
// DatabaseProvider.insertFtsRow() and DatabaseProvider.searchFts() using openHelper directly.

@Entity(tableName = "telegram_backups")
data class TelegramBackupEntity(
    @PrimaryKey val mediaId: Long,
    val telegramFileId: String?,
    val telegramThumbFileId: String?,
    val telegramMessageId: Long? = null,
    val backupStatus: String, // "PENDING", "SUCCESS", "FAILED"
    val backupTimestamp: Long
)

data class BucketMetadata(
    val bucketName: String,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val maxDate: Long,
    val coverUriString: String?,
    val isVideo: Boolean?
)
