package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores individual representative 512-D embeddings for user-dismissed false detections
 * ("Not a face") so future scans drop recurring non-person artifacts without averaging
 * unrelated objects into a single centroid.
 */
@Entity(tableName = "ignored_face_exemplars")
data class IgnoredFaceExemplarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val embedding: FloatArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IgnoredFaceExemplarEntity
        if (id != other.id) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (createdAt != other.createdAt) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
