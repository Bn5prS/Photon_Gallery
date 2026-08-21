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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class ReverseGeocodeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ReverseGeocodeWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = DatabaseProvider.getDatabase(applicationContext)
        val mediaDao = database.mediaDao()
        val placesDao = database.placesDao()

        try {
            // STEP 1: Scan EXIF GPS metadata for any images in core_media that haven't been checked yet
            val needsGps = mediaDao.getMediaNeedingGps()
            if (needsGps.isNotEmpty()) {
                Log.d(TAG, "Scanning EXIF GPS for ${needsGps.size} unindexed photos...")
                val batchSize = 100
                for (batch in needsGps.chunked(batchSize)) {
                    val updates = batch.mapNotNull { entity ->
                        try {
                            val file = if (entity.filePath.isNotEmpty()) File(entity.filePath) else null
                            if (file != null && file.exists()) {
                                val exif = androidx.exifinterface.media.ExifInterface(entity.filePath)
                                val latLong = exif.latLong
                                if (latLong != null && latLong.size >= 2 && (latLong[0] != 0.0 || latLong[1] != 0.0)) {
                                    entity.copy(latitude = latLong[0], longitude = latLong[1])
                                } else null
                            } else null
                        } catch (_: Exception) { null }
                    }
                    if (updates.isNotEmpty()) {
                        mediaDao.insertAll(updates)
                    }
                }
            }

            // STEP 2: Now find all geotagged photos that haven't been reverse-geocoded into place names
            val allGeotagged = mediaDao.getGeotaggedMedia()
            if (allGeotagged.isEmpty()) {
                Log.d(TAG, "No geotagged media found in database.")
                return@withContext Result.success()
            }

            val alreadyProcessedIds = placesDao.getAllGeocodedMediaIds().toSet()
            val toProcess = allGeotagged.filter { it.id !in alreadyProcessedIds }
            if (toProcess.isEmpty()) {
                Log.d(TAG, "All ${allGeotagged.size} geotagged photos are already reverse-geocoded.")
                return@withContext Result.success()
            }

            Log.d(TAG, "Reverse geocoding ${toProcess.size} geotagged photos...")
            val geocoder = if (Geocoder.isPresent()) Geocoder(applicationContext, Locale.getDefault()) else null
            val chunk = toProcess.take(250)
            val locations = mutableListOf<GeocodedLocation>()

            for (media in chunk) {
                val lat = media.latitude
                val lon = media.longitude
                if (lat != null && lon != null) {
                    val placeName = if (geocoder != null) {
                        getPlaceName(geocoder, lat, lon)
                    } else null

                    val resolvedPlace = placeName ?: String.format(Locale.US, "Location (%.2f, %.2f)", lat, lon)
                    locations.add(
                        GeocodedLocation(
                            mediaId = media.id,
                            placeName = resolvedPlace,
                            adminArea = null
                        )
                    )
                }
            }

            if (locations.isNotEmpty()) {
                placesDao.insertGeocodedLocations(locations)
                Log.d(TAG, "Successfully inserted ${locations.size} reverse-geocoded locations.")
            }

            if (toProcess.size > chunk.size) {
                return@withContext Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ReverseGeocodeWorker", e)
            Result.retry()
        }
    }

    private suspend fun getPlaceName(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return withTimeoutOrNull(3500L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            val address = addresses.firstOrNull()
                            val place = address?.locality
                                ?: address?.subLocality
                                ?: address?.subAdminArea
                                ?: address?.adminArea
                                ?: address?.countryName
                                ?: address?.featureName
                            if (continuation.isActive) {
                                continuation.resume(place)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        val address = addresses?.firstOrNull()
                        val place = address?.locality
                            ?: address?.subLocality
                            ?: address?.subAdminArea
                            ?: address?.adminArea
                            ?: address?.countryName
                            ?: address?.featureName
                        if (continuation.isActive) {
                            continuation.resume(place)
                        }
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
