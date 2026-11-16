package com.inferno.gallery.data

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust date extractor that recovers capture timestamps from filename patterns
 * (e.g. camera, WhatsApp, screenshots) and embedded EXIF headers when MediaStore's
 * DATE_TAKEN column is null or 0 (common for imported/transferred files).
 */
object FilenameDateParser {

    // Matches timestamps like:
    // 20231108_143025, 2023-11-08-14-30-25, 2023_11_08_14_30_25, 2023-11-08 14.30.25, 2023-11-08T143025
    private val DATE_TIME_PATTERN: Pattern = Pattern.compile(
        "(?:^|[^0-9])(19\\d\\d|20\\d\\d)[-_]?(0[1-9]|1[0-2])[-_]?(0[1-9]|[12]\\d|3[01])(?:[-_T\\s]+)([01]\\d|2[0-3])[-_.:]?([0-5]\\d)[-_.:]?([0-5]\\d)"
    )

    // Matches date-only filenames like:
    // IMG-20231108-WA0001, WP_20231108_001, 2023-11-08, 20231108
    private val DATE_ONLY_PATTERN: Pattern = Pattern.compile(
        "(?:^|[^0-9])(19\\d\\d|20\\d\\d)[-_]?(0[1-9]|1[0-2])[-_]?(0[1-9]|[12]\\d|3[01])(?:[^0-9]|$)"
    )

    /**
     * Attempts to parse an epoch second timestamp from standard camera/messaging naming conventions.
     * Returns epoch seconds if valid, or null if no valid date pattern is found.
     */
    fun parseDateFromFilename(name: String): Long? {
        if (name.isBlank()) return null

        val dtMatcher = DATE_TIME_PATTERN.matcher(name)
        if (dtMatcher.find()) {
            val year = dtMatcher.group(1)?.toIntOrNull() ?: return null
            val month = dtMatcher.group(2)?.toIntOrNull() ?: return null
            val day = dtMatcher.group(3)?.toIntOrNull() ?: return null
            val hour = dtMatcher.group(4)?.toIntOrNull() ?: return null
            val minute = dtMatcher.group(5)?.toIntOrNull() ?: return null
            val second = dtMatcher.group(6)?.toIntOrNull() ?: return null

            return toEpochSeconds(year, month, day, hour, minute, second)
        }

        val dMatcher = DATE_ONLY_PATTERN.matcher(name)
        if (dMatcher.find()) {
            val year = dMatcher.group(1)?.toIntOrNull() ?: return null
            val month = dMatcher.group(2)?.toIntOrNull() ?: return null
            val day = dMatcher.group(3)?.toIntOrNull() ?: return null

            // Midday (12:00:00) to avoid edge timezone shifts
            return toEpochSeconds(year, month, day, 12, 0, 0)
        }

        return null
    }

    private fun toEpochSeconds(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long? {
        if (year !in 1990..2035) return null
        if (month !in 1..12) return null
        if (day !in 1..31) return null
        if (hour !in 0..23) return null
        if (minute !in 0..59) return null
        if (second !in 0..59) return null

        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
        }
        val ms = cal.timeInMillis
        return if (ms > 0) ms / 1000L else null
    }

    /**
     * Reads EXIF capture date (TAG_DATETIME_ORIGINAL or TAG_DATETIME) from the local file if present.
     */
    fun extractExifDate(filePath: String): Long? {
        if (filePath.isBlank()) return null
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() < 128) return null
            val exif = ExifInterface(filePath)
            val dateOrigStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: return null
            val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val parsedMs = sdf.parse(dateOrigStr)?.time ?: return null
            if (parsedMs > 0) parsedMs / 1000L else null
        } catch (_: Throwable) {
            null
        }
    }
}

