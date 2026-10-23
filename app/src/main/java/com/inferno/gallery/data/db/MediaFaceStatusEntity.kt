package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Tracks face scanning status for media items to avoid repeatedly re-processing
 * photos that contain no detected faces.
 */
@Entity(
    tableName = "media_face_status",
    foreignKeys = [
        ForeignKey(
            entity = CoreMediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaFaceStatusEntity(
    @PrimaryKey val mediaId: Long,
    val faceCount: Int = 0,
    val scannedAt: Long = System.currentTimeMillis()
)
