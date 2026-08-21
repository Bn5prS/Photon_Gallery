package com.inferno.gallery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlacesDao {
    @Query("SELECT mediaId FROM geocoded_locations")
    suspend fun getAllGeocodedMediaIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeocodedLocations(locations: List<GeocodedLocation>)

    @Query("""
        SELECT gl.placeName as bucketName, 
               COUNT(DISTINCT gl.mediaId) as itemCount, 
               MAX(cm2.uriString) as representativeUri 
        FROM geocoded_locations gl
        INNER JOIN core_media cm2 ON gl.mediaId = cm2.id
        WHERE cm2.bucketName != 'Trash'
          AND cm2.uriString NOT IN (SELECT originalUri FROM vault_media)
          AND cm2.filePath NOT IN (SELECT originalPath FROM vault_media)
          AND gl.placeName IS NOT NULL AND gl.placeName != ''
        GROUP BY gl.placeName
        ORDER BY itemCount DESC
    """)
    fun observePlaceClusters(): kotlinx.coroutines.flow.Flow<List<BucketInfo>>
}
