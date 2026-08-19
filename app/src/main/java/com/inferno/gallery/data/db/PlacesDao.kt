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

    @Query("SELECT placeName as bucketName, COUNT(*) as itemCount, (SELECT uriString FROM core_media WHERE id = geocoded_locations.mediaId LIMIT 1) as representativeUri FROM geocoded_locations GROUP BY placeName")
    fun observePlaceClusters(): kotlinx.coroutines.flow.Flow<List<BucketInfo>>
}
