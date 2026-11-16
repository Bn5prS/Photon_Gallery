package com.inferno.gallery.data.model

enum class ExifStripAction(val id: String) {
    ASK_EVERY_TIME("ask_every_time"),
    SAVE_AS_COPY("save_as_copy"),
    OVERWRITE_ORIGINAL("overwrite_original");

    companion object {
        fun fromId(id: String): ExifStripAction = entries.firstOrNull { it.id == id } ?: ASK_EVERY_TIME
    }
}

