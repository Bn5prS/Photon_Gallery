package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faces")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val clusterId: Int,
    val personName: String?,
    val thumbnailUri: String? = null
)
