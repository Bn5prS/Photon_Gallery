package com.inferno.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_preferences")

class FavoritesManager(private val context: Context) {
    companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("favorite_ids")
        val UNFAVORITES_KEY = stringSetPreferencesKey("unfavorite_ids")
    }

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Queries Android system MediaStore for all photos and videos marked with IS_FAVORITE = 1.
     * This captures media favorited in Google Photos, OEM Gallery apps, or camera apps.
     */
    fun queryMediaStoreFavorites(): Set<String> {
        val mediaStoreFavorites = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.IS_FAVORITE} = 1"

        fun queryUri(uri: Uri) {
            try {
                contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idColumn != -1) {
                        while (cursor.moveToNext()) {
                            mediaStoreFavorites.add(cursor.getLong(idColumn).toString())
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }

        queryUri(MediaStore.Files.getContentUri("external"))
        queryUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        queryUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        return mediaStoreFavorites
    }

    /**
     * Observes MediaStore external storage changes so that whenever another app (like Google Photos,
     * Refra, or OEM camera/gallery) favorites or unfavorites an item, Photon Gallery immediately reacts in real time.
     */
    private val mediaStoreFavoritesFlow: Flow<Set<String>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryMediaStoreFavorites())
            }
        }

        val fileUri = MediaStore.Files.getContentUri("external")
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        contentResolver.registerContentObserver(fileUri, true, observer)
        contentResolver.registerContentObserver(imageUri, true, observer)
        contentResolver.registerContentObserver(videoUri, true, observer)

        // Initial emission
        trySend(queryMediaStoreFavorites())

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Unified favorites flow:
     * (MediaStore system favorites + locally favorited items) - locally unfavorited items.
     * Emits instantly and updates whenever local preference or MediaStore changes.
     */
    val favoritesFlow: Flow<Set<String>> = combine(
        mediaStoreFavoritesFlow,
        context.favoritesDataStore.data
    ) { mediaStoreFavs, preferences ->
        val localFavs = preferences[FAVORITES_KEY] ?: emptySet()
        val localUnfavs = preferences[UNFAVORITES_KEY] ?: emptySet()
        (mediaStoreFavs + localFavs) - localUnfavs
    }.flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(id: String) {
        val mediaStoreFavs = withContext(Dispatchers.IO) { queryMediaStoreFavorites() }
        val isCurrentlyMediaStoreFav = mediaStoreFavs.contains(id)
        var newFavoriteState = false

        context.favoritesDataStore.edit { preferences ->
            val localFavs = preferences[FAVORITES_KEY] ?: emptySet()
            val localUnfavs = preferences[UNFAVORITES_KEY] ?: emptySet()

            val isCurrentlyEffectiveFav = (isCurrentlyMediaStoreFav || localFavs.contains(id)) && !localUnfavs.contains(id)
            newFavoriteState = !isCurrentlyEffectiveFav

            if (isCurrentlyEffectiveFav) {
                // Removing favorite
                preferences[FAVORITES_KEY] = localFavs - id
                if (isCurrentlyMediaStoreFav) {
                    preferences[UNFAVORITES_KEY] = localUnfavs + id
                }
            } else {
                // Adding favorite
                preferences[FAVORITES_KEY] = localFavs + id
                preferences[UNFAVORITES_KEY] = localUnfavs - id
            }
        }

        // Best effort sync back to Android MediaStore
        withContext(Dispatchers.IO) {
            try {
                val mediaId = id.toLongOrNull()
                if (mediaId != null) {
                    val mediaUri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"),
                        mediaId
                    )
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_FAVORITE, if (newFavoriteState) 1 else 0)
                    }
                    contentResolver.update(mediaUri, values, null, null)
                }
            } catch (ignored: Exception) {
                // Scoped Storage may require intent sender on third-party files
            }
        }
    }

    suspend fun removeFavorites(ids: Set<String>) {
        if (ids.isEmpty()) return
        context.favoritesDataStore.edit { preferences ->
            val localFavs = preferences[FAVORITES_KEY] ?: emptySet()
            val localUnfavs = preferences[UNFAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = localFavs - ids
            preferences[UNFAVORITES_KEY] = localUnfavs + ids
        }
    }
}
