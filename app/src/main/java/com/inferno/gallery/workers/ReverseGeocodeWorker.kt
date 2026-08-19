package com.inferno.gallery.workers

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.inferno.gallery.data.db.DatabaseProvider
import com.inferno.gallery.data.db.GeocodedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReverseGeocodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = DatabaseProvider.getDatabase(applicationContext)
        val mediaDao = database.mediaDao()
        val placesDao = database.placesDao()

        try {
            val allGeotagged = mediaDao.getGeotaggedMedia()
            val alreadyProcessedIds = placesDao.getAllGeocodedMediaIds().toSet()
            
            val toProcess = allGeotagged.filter { it.id !in alreadyProcessedIds }
            if (toProcess.isEmpty()) {
                return@withContext Result.success()
            }

            val geocoder = Geocoder(applicationContext, Locale.getDefault())
            
            // Avoid hitting geocoder rate limits by taking chunks
            val chunk = toProcess.take(200)
            val locations = mutableListOf<GeocodedLocation>()

            for (media in chunk) {
                if (media.latitude != null && media.longitude != null) {
                    val placeName = getPlaceName(geocoder, media.latitude, media.longitude)
                    if (placeName != null) {
                        locations.add(
                            GeocodedLocation(
                                mediaId = media.id,
                                placeName = placeName,
                                adminArea = null // Optionally extract admin area if needed
                            )
                        )
                    }
                }
            }

            if (locations.isNotEmpty()) {
                placesDao.insertGeocodedLocations(locations)
            }

            // If there are still items remaining, we could enqueue another worker or just let the next schedule handle it
            if (toProcess.size > chunk.size) {
                return@withContext Result.retry()
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("ReverseGeocodeWorker", "Error geocoding locations", e)
            Result.retry()
        }
    }

    private suspend fun getPlaceName(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return suspendCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val place = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: address?.featureName
                        continuation.resume(place)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()
                    val place = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: address?.featureName
                    continuation.resume(place)
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }
}
