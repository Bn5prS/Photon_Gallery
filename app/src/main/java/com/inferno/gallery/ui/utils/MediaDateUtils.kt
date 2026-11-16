package com.inferno.gallery.ui.utils

/**
 * Resolves the timestamp shown for a single media item.
 *
 * `dateAdded` carries the capture date resolved at scan time (MediaStore `DATE_TAKEN`, which is
 * EXIF derived). `dateModified` is the file mtime, so it is only used as a fallback for files
 * without any capture date.
 */
fun resolveDisplayDateSeconds(dateAdded: Long, dateModified: Long): Long =
    if (dateAdded > 0) dateAdded else dateModified
