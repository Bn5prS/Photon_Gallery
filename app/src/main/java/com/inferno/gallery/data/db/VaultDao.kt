package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_media ORDER BY dateHidden DESC")
    fun observeAll(): Flow<List<VaultMediaEntity>>

    @Query("SELECT COUNT(*) FROM vault_media")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultMediaEntity?

    @Query("SELECT * FROM vault_media WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<VaultMediaEntity>

    @Query("SELECT * FROM vault_media ORDER BY dateHidden DESC")
    suspend fun getAll(): List<VaultMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VaultMediaEntity>): List<Long>

    @Query("DELETE FROM vault_media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
