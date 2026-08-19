package com.inferno.gallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_preferences")

class FavoritesManager(private val context: Context) {
    companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("favorite_ids")
    }

    val favoritesFlow: Flow<Set<String>> = context.favoritesDataStore.data.map { preferences ->
        preferences[FAVORITES_KEY] ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        context.favoritesDataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            if (currentFavorites.contains(id)) {
                preferences[FAVORITES_KEY] = currentFavorites - id
            } else {
                preferences[FAVORITES_KEY] = currentFavorites + id
            }
        }
    }
}
