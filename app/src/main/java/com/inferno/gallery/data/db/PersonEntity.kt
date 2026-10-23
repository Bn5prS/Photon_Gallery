package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a recognized person cluster in the gallery.
 */
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String? = null,
    val coverCropPath: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val isPromoted: Boolean = false, // True once >= 3 distinct photos contain this person
    val faceCount: Int = 0,
    val centroid: FloatArray? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PersonEntity
        if (id != other.id) return false
        if (name != other.name) return false
        if (coverCropPath != other.coverCropPath) return false
        if (isHidden != other.isHidden) return false
        if (isFavorite != other.isFavorite) return false
        if (isPromoted != other.isPromoted) return false
        if (faceCount != other.faceCount) return false
        if (createdAt != other.createdAt) return false
        if (centroid != null) {
            if (other.centroid == null) return false
            if (!centroid.contentEquals(other.centroid)) return false
        } else if (other.centroid != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (coverCropPath?.hashCode() ?: 0)
        result = 31 * result + isHidden.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isPromoted.hashCode()
        result = 31 * result + faceCount.hashCode()
        result = 31 * result + (centroid?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
