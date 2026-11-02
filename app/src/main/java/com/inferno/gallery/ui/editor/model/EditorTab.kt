package com.inferno.gallery.ui.editor.model

import androidx.annotation.DrawableRes
import com.inferno.gallery.R

enum class EditorTab(val label: String, @param:DrawableRes val iconRes: Int) {
    AUTO("Auto", R.drawable.ic_ms_auto_fix_high),
    CROP("Crop", R.drawable.ic_ms_crop),
    ADJUST("Adjust", R.drawable.ic_ms_tune),
    MARKUP("Markup", R.drawable.ic_ms_edit),
    FILTERS("Filters", R.drawable.ic_ms_palette),
    MORE("More", R.drawable.ic_ms_more_horiz)
}
