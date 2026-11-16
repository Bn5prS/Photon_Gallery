package com.inferno.gallery.ui.editor.model

import androidx.annotation.DrawableRes
import com.inferno.gallery.R

enum class EditorTab(val label: String, @param:DrawableRes val iconRes: Int) {
    AUTO("Auto", R.drawable.ic_ms_thin_auto_fix_high),
    CROP("Crop", R.drawable.ic_ms_thin_crop),
    ADJUST("Adjust", R.drawable.ic_ms_thin_tune),
    MARKUP("Markup", R.drawable.ic_ms_thin_edit),
    FILTERS("Filters", R.drawable.ic_ms_thin_palette),
    MORE("More", R.drawable.ic_ms_thin_more_horiz)
}
