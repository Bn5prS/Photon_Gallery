package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "virtual_folder_mapping")
data class VirtualFolderMappingEntity(
    @PrimaryKey val mediaId: Long,
    val folderName: String,
    val dateMapped: Long
)

@Dao
interface VirtualFolderMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: VirtualFolderMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<VirtualFolderMappingEntity>)

    @Query("SELECT * FROM virtual_folder_mapping WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getMappingForMedia(mediaId: Long): VirtualFolderMappingEntity?

    @Query("SELECT * FROM virtual_folder_mapping")
    suspend fun getAllMappings(): List<VirtualFolderMappingEntity>

    @Query("SELECT folderName, COUNT(*) as itemCount FROM virtual_folder_mapping GROUP BY folderName")
    fun observeVirtualFolders(): Flow<List<VirtualFolderInfo>>

    @Query("DELETE FROM virtual_folder_mapping WHERE mediaId = :mediaId")
    suspend fun deleteMapping(mediaId: Long)
    
    @Query("DELETE FROM virtual_folder_mapping")
    suspend fun clearAllMappings()
}

data class VirtualFolderInfo(
    val folderName: String,
    val itemCount: Int
)
