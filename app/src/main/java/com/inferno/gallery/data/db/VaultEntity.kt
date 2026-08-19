package com.inferno.gallery.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUri: String,
    val originalPath: String,
    val originalBucket: String,
    val originalRelativePath: String = "",
    val fileName: String,
    val vaultFileName: String,
    val mimeType: String?,
    val isVideo: Boolean,
    val durationMs: Long?,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long = 0,
    val dateHidden: Long
)
