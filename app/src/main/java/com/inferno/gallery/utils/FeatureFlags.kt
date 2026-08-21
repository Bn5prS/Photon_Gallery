package com.inferno.gallery.utils

/**
 * Global feature flags for gating experimental or in-development features.
 */
object FeatureFlags {
    /**
     * Controls visibility of the People & Pets UI (carousels, people list, face highlights in EXIF).
     * Set to false for shipping bug-fix releases while face clustering model tuning is completed.
     */
    const val ENABLE_PEOPLE_FEATURE = false
}
