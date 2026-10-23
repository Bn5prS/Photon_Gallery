package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single detected face in a media item.
 */
@Entity(
    tableName = "faces",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CoreMediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personId"),
        Index("mediaId")
    ]
)
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val personId: Long? = null,
    val bboxLeft: Float = 0f,
    val bboxTop: Float = 0f,
    val bboxRight: Float = 0f,
    val bboxBottom: Float = 0f,
    val qualityScore: Float = 0f,
    val isExemplar: Boolean = false,
    val cropCachePath: String? = null,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEntity
        if (id != other.id) return false
        if (mediaId != other.mediaId) return false
        if (personId != other.personId) return false
        if (bboxLeft != other.bboxLeft) return false
        if (bboxTop != other.bboxTop) return false
        if (bboxRight != other.bboxRight) return false
        if (bboxBottom != other.bboxBottom) return false
        if (qualityScore != other.qualityScore) return false
        if (isExemplar != other.isExemplar) return false
        if (cropCachePath != other.cropCachePath) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + mediaId.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + bboxLeft.hashCode()
        result = 31 * result + bboxTop.hashCode()
        result = 31 * result + bboxRight.hashCode()
        result = 31 * result + bboxBottom.hashCode()
        result = 31 * result + qualityScore.hashCode()
        result = 31 * result + isExemplar.hashCode()
        result = 31 * result + (cropCachePath?.hashCode() ?: 0)
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
