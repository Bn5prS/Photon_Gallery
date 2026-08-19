package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {
    @Query("SELECT * FROM faces WHERE clusterId = :clusterId")
    fun getFacesForCluster(clusterId: Int): Flow<List<FaceEntity>>

    @Query("UPDATE faces SET personName = :name WHERE clusterId = :clusterId")
    suspend fun updatePersonName(clusterId: Int, name: String)

    @Query("SELECT * FROM faces GROUP BY clusterId")
    fun observeAllClusters(): Flow<List<FaceEntity>>

    @Query("SELECT * FROM faces GROUP BY clusterId")
    fun getClusterRepresentatives(): Flow<List<FaceEntity>>
}
