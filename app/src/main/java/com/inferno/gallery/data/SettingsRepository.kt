package com.inferno.gallery.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DockStyle { PILL, FULL_WIDTH }
enum class TimelineLayoutMode { STANDARD_GRID, EDITORIAL_MOSAIC, STAGGERED_MASONRY }

val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val TIMELINE_LAYOUT_MODE = stringPreferencesKey("timeline_layout_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val ALBUM_SORT_ORDER = stringPreferencesKey("album_sort_order")
        val DOCK_STYLE = stringPreferencesKey("dock_style")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val USE_AMOLED_BLACK = booleanPreferencesKey("use_amoled_black")
        val GRID_AUTO_PLAY = booleanPreferencesKey("grid_auto_play")
        val SELECTED_FILTER_INDEX = androidx.datastore.preferences.core.intPreferencesKey("selected_filter_index")
        val GRID_CELLS_COUNT = androidx.datastore.preferences.core.intPreferencesKey("grid_cells_count")
        val THUMBNAIL_CORNER_RADIUS = floatPreferencesKey("thumbnail_corner_radius")
        val USE_FULL_SCREEN = booleanPreferencesKey("use_full_screen")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STRIP_METADATA_ON_SHARE = booleanPreferencesKey("strip_metadata_on_share")
        val SMART_SEARCH_AUTO_INDEX = booleanPreferencesKey("smart_search_auto_index")
        val USE_SYSTEM_FONT = booleanPreferencesKey("use_system_font")
        val TYPOGRAPHY_STYLE = stringPreferencesKey("typography_style")
        val SECURE_RECENTS_ENABLED = booleanPreferencesKey("secure_recents_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTICS_STRENGTH = floatPreferencesKey("haptics_strength")

        val CONFIRM_DELETE_ENABLED = booleanPreferencesKey("confirm_delete_enabled")
        val AUTOPLAY_WITH_SOUND_ENABLED = booleanPreferencesKey("autoplay_with_sound_enabled")
        val AUTO_CLEAN_TRASH_ENABLED = booleanPreferencesKey("auto_clean_trash_enabled")
        val AUTO_CLEAN_TRASH_DAYS = androidx.datastore.preferences.core.intPreferencesKey("auto_clean_trash_days")
        val CACHE_THUMBNAILS_ENABLED = booleanPreferencesKey("cache_thumbnails_enabled")
        val MAX_BRIGHTNESS_ENABLED = booleanPreferencesKey("max_brightness_enabled")
        

        val EXCLUDED_FOLDERS = stringPreferencesKey("excluded_folders")
        val HDR_DISPLAY_ENABLED = booleanPreferencesKey("hdr_display_enabled")
        val PINNED_FOLDERS = stringPreferencesKey("pinned_folders")
        val PINNED_FOLDERS_ORDER = stringPreferencesKey("pinned_folders_order")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val SHOW_HIDDEN_ALBUMS = booleanPreferencesKey("show_hidden_albums")
        val VIEWER_BLUR_EFFECT = booleanPreferencesKey("viewer_blur_effect")
        val ALBUM_CUSTOM_COVERS = stringPreferencesKey("album_custom_covers")
        val SHOW_ALBUM_SIZE = booleanPreferencesKey("show_album_size")
        val OCR_INDEXING_ENABLED = booleanPreferencesKey("ocr_indexing_enabled")
        val STORY_MUSIC_MUTED = booleanPreferencesKey("story_music_muted")

        val ALBUMS_EXPANDED_PINNED = booleanPreferencesKey("albums_expanded_pinned")
        val ALBUMS_EXPANDED_MORE = booleanPreferencesKey("albums_expanded_more")
        val ALBUMS_EXPANDED_PEOPLE = booleanPreferencesKey("albums_expanded_people")
        val ALBUMS_EXPANDED_PLACES = booleanPreferencesKey("albums_expanded_places")
        val ALBUMS_EXPANDED_MEDIA_TYPES = booleanPreferencesKey("albums_expanded_media_types")

        // Theme Customization
        val APP_SEED_COLOR = intPreferencesKey("app_seed_color")
        val THEME_PALETTE_STYLE = stringPreferencesKey("theme_palette_style")
        val THEME_CONTRAST_LEVEL = floatPreferencesKey("theme_contrast_level")
        val INVERT_THEME_COLORS = booleanPreferencesKey("invert_theme_colors")

        // Advanced Theming (MaterialKolor)
        val COLOR_PRESET_NAME = stringPreferencesKey("color_preset_name")          // "Material You" | "Lavender Dream" | "Custom" etc.
        val SECONDARY_COLOR_OVERRIDE = intPreferencesKey("secondary_color_override") // -1 = auto (derived from seed)
        val TERTIARY_COLOR_OVERRIDE = intPreferencesKey("tertiary_color_override")   // -1 = auto (derived from seed)
        val CONTRAST_PRESET = stringPreferencesKey("contrast_preset")               // "Reduced" | "Default" | "Medium" | "High"
        val ANIMATE_THEME_TRANSITIONS = booleanPreferencesKey("animate_theme_transitions")
    }

    val albumsExpandedPinnedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALBUMS_EXPANDED_PINNED] ?: true }

    val albumsExpandedMoreFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALBUMS_EXPANDED_MORE] ?: true }

    val albumsExpandedPeopleFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALBUMS_EXPANDED_PEOPLE] ?: true }

    val albumsExpandedPlacesFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALBUMS_EXPANDED_PLACES] ?: true }

    val albumsExpandedMediaTypesFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALBUMS_EXPANDED_MEDIA_TYPES] ?: true }

    suspend fun updateAlbumsExpandedPinned(expanded: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALBUMS_EXPANDED_PINNED] = expanded }
    }

    suspend fun updateAlbumsExpandedMore(expanded: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALBUMS_EXPANDED_MORE] = expanded }
    }

    suspend fun updateAlbumsExpandedPeople(expanded: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALBUMS_EXPANDED_PEOPLE] = expanded }
    }

    suspend fun updateAlbumsExpandedPlaces(expanded: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALBUMS_EXPANDED_PLACES] = expanded }
    }

    suspend fun updateAlbumsExpandedMediaTypes(expanded: Boolean) {
        context.dataStore.edit { preferences -> preferences[ALBUMS_EXPANDED_MEDIA_TYPES] = expanded }
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

    val albumSortOrderFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ALBUM_SORT_ORDER] ?: "NameAsc"
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
            preferences[USE_AMOLED_BLACK] ?: true
        }

    val appSeedColorFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[APP_SEED_COLOR] ?: 0xFF6750A4.toInt() // Default M3 Purple
        }

    val themePaletteStyleFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_PALETTE_STYLE] ?: "TonalSpot"
        }

    val themeContrastLevelFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_CONTRAST_LEVEL] ?: 0f
        }

    val invertThemeColorsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[INVERT_THEME_COLORS] ?: false
        }

    // ── Advanced Theming Flows ──
    val colorPresetNameFlow: Flow<String> = context.dataStore.data
        .map { it[COLOR_PRESET_NAME] ?: "Material You" }

    val secondaryColorOverrideFlow: Flow<Int> = context.dataStore.data
        .map { it[SECONDARY_COLOR_OVERRIDE] ?: -1 }

    val tertiaryColorOverrideFlow: Flow<Int> = context.dataStore.data
        .map { it[TERTIARY_COLOR_OVERRIDE] ?: -1 }

    val contrastPresetFlow: Flow<String> = context.dataStore.data
        .map { it[CONTRAST_PRESET] ?: "Default" }

    val animateThemeTransitionsFlow: Flow<Boolean> = context.dataStore.data
        .map { it[ANIMATE_THEME_TRANSITIONS] ?: true }

    val smartSearchAutoIndexFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SMART_SEARCH_AUTO_INDEX] ?: false
        }

    val confirmDeleteEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CONFIRM_DELETE_ENABLED] ?: true
        }

    val autoplayWithSoundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTOPLAY_WITH_SOUND_ENABLED] ?: true
        }

    val autoCleanTrashEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_CLEAN_TRASH_ENABLED] ?: false
        }

    val autoCleanTrashDaysFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_CLEAN_TRASH_DAYS] ?: 30
        }

    val cacheThumbnailsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CACHE_THUMBNAILS_ENABLED] ?: true
        }

    val maxBrightnessEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[MAX_BRIGHTNESS_ENABLED] ?: false
        }

    val typographyStyleFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[TYPOGRAPHY_STYLE] ?: if (preferences[USE_SYSTEM_FONT] == true) "system" else "expressive"
        }

    suspend fun updateTypographyStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[TYPOGRAPHY_STYLE] = style
            preferences[USE_SYSTEM_FONT] = (style == "system")
        }
    }

    val useSystemFontFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_SYSTEM_FONT] ?: false
        }

    suspend fun updateUseSystemFont(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_FONT] = use
            preferences[TYPOGRAPHY_STYLE] = if (use) "system" else "expressive"
        }
    }

    val secureRecentsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SECURE_RECENTS_ENABLED] ?: false
        }

    suspend fun updateSecureRecentsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SECURE_RECENTS_ENABLED] = enabled
        }
    }

    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAPTICS_ENABLED] ?: true
        }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED] = enabled
        }
    }

    val hapticsStrengthFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[HAPTICS_STRENGTH] ?: 0.5f
        }

    suspend fun updateHapticsStrength(strength: Float) {
        context.dataStore.edit { preferences ->
            preferences[HAPTICS_STRENGTH] = strength
        }
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

    val timelineLayoutModeFlow: Flow<TimelineLayoutMode> = context.dataStore.data
        .map { preferences ->
            when (preferences[TIMELINE_LAYOUT_MODE]) {
                TimelineLayoutMode.EDITORIAL_MOSAIC.name -> TimelineLayoutMode.EDITORIAL_MOSAIC
                TimelineLayoutMode.STAGGERED_MASONRY.name -> TimelineLayoutMode.STAGGERED_MASONRY
                else -> TimelineLayoutMode.STANDARD_GRID
            }
        }

    suspend fun updateTimelineLayoutMode(mode: TimelineLayoutMode) {
        context.dataStore.edit { preferences ->
            preferences[TIMELINE_LAYOUT_MODE] = mode.name
        }
    }

    suspend fun updateSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER] = order
        }
    }

    suspend fun updateAlbumSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[ALBUM_SORT_ORDER] = order
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

    suspend fun updateAppSeedColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[APP_SEED_COLOR] = color
        }
    }

    suspend fun updateThemePaletteStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PALETTE_STYLE] = style
        }
    }

    suspend fun updateThemeContrastLevel(level: Float) {
        context.dataStore.edit { preferences ->
            preferences[THEME_CONTRAST_LEVEL] = level
        }
    }

    suspend fun updateInvertThemeColors(invert: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INVERT_THEME_COLORS] = invert
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

    val thumbnailCornerRadiusFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[THUMBNAIL_CORNER_RADIUS] ?: 0f
        }

    suspend fun updateThumbnailCornerRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_CORNER_RADIUS] = radius
        }
    }

    val useFullScreenFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_FULL_SCREEN] ?: false
        }

    suspend fun updateUseFullScreen(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_FULL_SCREEN] = use
        }
    }

    val ocrIndexingEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[OCR_INDEXING_ENABLED] ?: true
        }

    suspend fun updateOcrIndexingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OCR_INDEXING_ENABLED] = enabled
        }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    val stripMetadataOnShareFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[STRIP_METADATA_ON_SHARE] ?: true
        }

    suspend fun updateStripMetadataOnShare(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STRIP_METADATA_ON_SHARE] = enabled
        }
    }

    suspend fun updateSmartSearchAutoIndex(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_SEARCH_AUTO_INDEX] = enabled
        }
    }



    suspend fun updateConfirmDeleteEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CONFIRM_DELETE_ENABLED] = enabled
        }
    }

    suspend fun updateAutoplayWithSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOPLAY_WITH_SOUND_ENABLED] = enabled
        }
    }

    suspend fun updateAutoCleanTrashEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CLEAN_TRASH_ENABLED] = enabled
        }
    }

    suspend fun updateAutoCleanTrashDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CLEAN_TRASH_DAYS] = days
        }
    }

    suspend fun updateCacheThumbnailsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_THUMBNAILS_ENABLED] = enabled
        }
    }

    suspend fun updateMaxBrightnessEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MAX_BRIGHTNESS_ENABLED] = enabled
        }
    }

    val excludedFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val foldersStr = preferences[EXCLUDED_FOLDERS] ?: ""
            if (foldersStr.isBlank()) emptySet() else foldersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

    suspend fun updateExcludedFolders(folders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[EXCLUDED_FOLDERS] = folders.joinToString(",")
        }
    }

    val pinnedFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val str = preferences[PINNED_FOLDERS] ?: ""
            if (str.isBlank()) emptySet() else str.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

    suspend fun togglePinnedFolder(folder: String): Boolean {
        var success = true
        context.dataStore.edit { preferences ->
            val current = (preferences[PINNED_FOLDERS] ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (current.contains(folder)) {
                current.remove(folder)
            } else {
                if (current.size >= 6) {
                    success = false
                } else {
                    current.add(folder)
                }
            }
            if (success) {
                preferences[PINNED_FOLDERS] = current.joinToString(",")
            }
        }
        return success
    }

    val pinnedFoldersOrderFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val str = preferences[PINNED_FOLDERS_ORDER] ?: ""
            if (str.isBlank()) emptyList() else str.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

    suspend fun savePinnedFoldersOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PINNED_FOLDERS_ORDER] = order.joinToString(",")
        }
    }

    val showHiddenAlbumsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_HIDDEN_ALBUMS] ?: false
        }

    suspend fun updateShowHiddenAlbums(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HIDDEN_ALBUMS] = show
        }
    }

    val viewerBlurEffectFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIEWER_BLUR_EFFECT] ?: false
        }

    suspend fun updateViewerBlurEffect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIEWER_BLUR_EFFECT] = enabled
        }
    }

    // ── Album Custom Covers ──
    // Stored as a comma-separated list of "bucketName=uriString" pairs.
    val albumCustomCoversFlow: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            val str = preferences[ALBUM_CUSTOM_COVERS] ?: ""
            if (str.isBlank()) emptyMap()
            else str.split("|||")
                .filter { it.contains("==>") }
                .associate { pair ->
                    val idx = pair.indexOf("==>")
                    pair.substring(0, idx) to pair.substring(idx + 3)
                }
        }

    suspend fun setAlbumCustomCover(bucketName: String, uriString: String) {
        context.dataStore.edit { preferences ->
            val current = (preferences[ALBUM_CUSTOM_COVERS] ?: "")
                .split("|||")
                .filter { it.contains("==>") }
                .associate { pair ->
                    val idx = pair.indexOf("==>")
                    pair.substring(0, idx) to pair.substring(idx + 3)
                }.toMutableMap()
            current[bucketName] = uriString
            preferences[ALBUM_CUSTOM_COVERS] = current.entries.joinToString("|||") { (k, v) -> "$k==>$v" }
        }
    }

    suspend fun removeAlbumCustomCover(bucketName: String) {
        context.dataStore.edit { preferences ->
            val current = (preferences[ALBUM_CUSTOM_COVERS] ?: "")
                .split("|||")
                .filter { it.contains("==>") }
                .associate { pair ->
                    val idx = pair.indexOf("==>")
                    pair.substring(0, idx) to pair.substring(idx + 3)
                }.toMutableMap()
            if (current.remove(bucketName) != null) {
                preferences[ALBUM_CUSTOM_COVERS] = current.entries.joinToString("|||") { (k, v) -> "$k==>$v" }
            }
        }
    }

    val showAlbumSizeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_ALBUM_SIZE] ?: true
        }

    suspend fun updateShowAlbumSize(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_ALBUM_SIZE] = show
        }
    }



    suspend fun updateStoryMusicMuted(muted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STORY_MUSIC_MUTED] = muted
        }
    }

    val recentSearchesFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val str = preferences[RECENT_SEARCHES] ?: ""
            if (str.isBlank()) emptyList()
            else str.split("|||")
        }

    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = (preferences[RECENT_SEARCHES] ?: "").split("|||").filter { it.isNotBlank() }.toMutableList()
            current.remove(query)
            current.add(0, query)
            if (current.size > 5) {
                current.subList(5, current.size).clear()
            }
            preferences[RECENT_SEARCHES] = current.joinToString("|||")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES] = ""
        }
    }

    // ── Advanced Theming Updaters ──
    suspend fun updateColorPresetName(name: String) {
        context.dataStore.edit { it[COLOR_PRESET_NAME] = name }
    }

    suspend fun updateSecondaryColorOverride(color: Int) {
        context.dataStore.edit { it[SECONDARY_COLOR_OVERRIDE] = color }
    }

    suspend fun updateTertiaryColorOverride(color: Int) {
        context.dataStore.edit { it[TERTIARY_COLOR_OVERRIDE] = color }
    }

    suspend fun updateContrastPreset(preset: String) {
        // Also sync the legacy Float key so older codepaths stay compatible
        val level = when (preset) {
            "Reduced" -> -1f
            "Medium"  -> 0.5f
            "High"    -> 1f
            else      -> 0f   // "Default"
        }
        context.dataStore.edit {
            it[CONTRAST_PRESET] = preset
            it[THEME_CONTRAST_LEVEL] = level
        }
    }

    suspend fun updateAnimateThemeTransitions(animate: Boolean) {
        context.dataStore.edit { it[ANIMATE_THEME_TRANSITIONS] = animate }
    }
}
