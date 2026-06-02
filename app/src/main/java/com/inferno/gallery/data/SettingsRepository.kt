package com.inferno.gallery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DockStyle { PILL, FULL_WIDTH }

val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DOCK_STYLE = stringPreferencesKey("dock_style")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val USE_AMOLED_BLACK = booleanPreferencesKey("use_amoled_black")
        val GRID_AUTO_PLAY = booleanPreferencesKey("grid_auto_play")
        val SELECTED_FILTER_INDEX = androidx.datastore.preferences.core.intPreferencesKey("selected_filter_index")
        val GRID_CELLS_COUNT = androidx.datastore.preferences.core.intPreferencesKey("grid_cells_count")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: "SYSTEM"
        }

    val viewModeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[VIEW_MODE] ?: "Grouped"
        }

    val sortOrderFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SORT_ORDER] ?: "NewToOld"
        }

    val dockStyleFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DOCK_STYLE] ?: DockStyle.PILL.name
        }

    val useMaterialYouFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_MATERIAL_YOU] ?: true
        }

    val useAmoledBlackFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_AMOLED_BLACK] ?: false
        }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun updateViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE] = mode
        }
    }

    suspend fun updateSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER] = order
        }
    }

    suspend fun updateDockStyle(style: DockStyle) {
        context.dataStore.edit { preferences ->
            preferences[DOCK_STYLE] = style.name
        }
    }

    suspend fun updateUseMaterialYou(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_MATERIAL_YOU] = use
        }
    }

    suspend fun updateUseAmoledBlack(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_AMOLED_BLACK] = use
        }
    }

    val gridAutoPlayFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[GRID_AUTO_PLAY] ?: true
        }

    suspend fun toggleGridAutoPlay(current: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GRID_AUTO_PLAY] = !current
        }
    }



    val selectedFilterIndexFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_FILTER_INDEX] ?: 0
        }

    suspend fun updateSelectedFilterIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_FILTER_INDEX] = index
        }
    }

    val gridCellsCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[GRID_CELLS_COUNT] ?: 4
        }

    suspend fun updateGridCellsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[GRID_CELLS_COUNT] = count
        }
    }
}
