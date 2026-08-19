package com.inferno.gallery.data

/**
 * Centralized bucket name constants used throughout the app.
 * Eliminates magic strings and prevents typo-related bugs.
 */
object BucketNames {
    const val ALL = "All"
    const val CAMERA = "Camera"
    const val VIDEOS = "Videos"
    const val FAVORITES = "Favorites"
    const val TRASH = "Trash"
    const val SCREENSHOTS = "Screenshots"
    const val SCREENRECORDINGS = "Screenrecordings"
    const val SCREEN_RECORDS = "Screen records"
    const val SCREEN_RECORDS_NO_SPACE = "Screenrecords"
    const val SCREEN_RECORD = "ScreenRecord"
    const val SCREENSHOT = "Screenshot"

    // Virtual buckets (not actual MediaStore bucket names)
    const val SEARCH_TEXT = "search_text"
    const val SEARCH_SMART = "search_smart"

    // Smart Media Types
    const val MEDIA_TYPE_RAW = "mediatype:raw"
    const val MEDIA_TYPE_PANORAMAS = "mediatype:panoramas"
    const val MEDIA_TYPE_SLOW_MO = "mediatype:slow_mo"
    const val MEDIA_TYPE_ANIMATIONS = "mediatype:animations"
}
