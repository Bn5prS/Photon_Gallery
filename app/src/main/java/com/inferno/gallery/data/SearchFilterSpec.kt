package com.inferno.gallery.data

import androidx.compose.runtime.Immutable
import java.util.Calendar
import java.util.TimeZone

enum class SearchMediaTypeFilter(val label: String) {
    ALL("All media"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    MOTION_PHOTOS("Motion Photos"),
    SCREENSHOTS("Screenshots"),
    RAW("RAW"),
    FAVORITES("Favorites")
}

enum class SearchDatePreset(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Past 7 days"),
    LAST_30_DAYS("Past 30 days"),
    THIS_YEAR("This year"),
    LAST_YEAR("Last year");

    fun computeEpochSecRange(
        nowEpochSec: Long = System.currentTimeMillis() / 1000L,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochSec * 1000L
        }
        return when (this) {
            TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                (cal.timeInMillis / 1000L) to nowEpochSec
            }
            LAST_7_DAYS -> (nowEpochSec - 7L * 86_400L).coerceAtLeast(0L) to nowEpochSec
            LAST_30_DAYS -> (nowEpochSec - 30L * 86_400L).coerceAtLeast(0L) to nowEpochSec
            THIS_YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                (cal.timeInMillis / 1000L) to nowEpochSec
            }
            LAST_YEAR -> {
                val prevYear = cal.get(Calendar.YEAR) - 1
                val startCal = Calendar.getInstance(timeZone).apply {
                    clear()
                    set(prevYear, Calendar.JANUARY, 1, 0, 0, 0)
                }
                val endCal = Calendar.getInstance(timeZone).apply {
                    clear()
                    set(prevYear, Calendar.DECEMBER, 31, 23, 59, 59)
                }
                (startCal.timeInMillis / 1000L) to (endCal.timeInMillis / 1000L)
            }
        }
    }
}

enum class SearchDocumentTypeOption(val categoryKey: String, val label: String) {
    RECEIPTS(BucketNames.DOC_RECEIPTS, "Receipts"),
    PAYMENTS(BucketNames.DOC_PAYMENTS, "Payments"),
    IDS(BucketNames.DOC_IDS, "IDs & Cards"),
    NOTES(BucketNames.DOC_NOTES, "Notes"),
    EVENTS(BucketNames.DOC_EVENTS, "Tickets & Events"),
    BOOKS(BucketNames.DOC_BOOKS_MAGAZINES, "Books & Pages"),
    SOCIAL(BucketNames.DOC_SOCIAL, "Social Posts")
}

@Immutable
data class SearchFilterState(
    val dateStartSec: Long? = null,
    val dateEndSec: Long? = null,
    val dateLabel: String? = null,
    val personIds: Set<Long> = emptySet(),
    val documentCategories: Set<String> = emptySet(),
    val mediaType: SearchMediaTypeFilter = SearchMediaTypeFilter.ALL,
    val albumNames: Set<String> = emptySet(),
    val locationName: String? = null
) {
    val hasDateFilter: Boolean get() = dateStartSec != null || dateEndSec != null

    val activeCount: Int
        get() {
            var count = 0
            if (hasDateFilter) count++
            if (personIds.isNotEmpty()) count++
            if (documentCategories.isNotEmpty()) count++
            if (mediaType != SearchMediaTypeFilter.ALL) count++
            if (albumNames.isNotEmpty()) count++
            if (!locationName.isNullOrBlank()) count++
            return count
        }

    val isActive: Boolean get() = activeCount > 0

    fun clearAll(): SearchFilterState = SearchFilterState()
}

data class BuiltFilterSql(
    val sql: String,
    val args: List<Any>
)

/**
 * Pure Kotlin local SQL query builder for composable multi-criteria search filtering (#53).
 * Executes 100% offline against `core_media`, `faces`, `document_classifications`, and `geocoded_locations`.
 */
object SearchFilterQueryBuilder {

    private const val NORMALIZED_DATE_EXPR =
        "(CASE WHEN cm.dateAdded > 100000000000 THEN cm.dateAdded / 1000 ELSE cm.dateAdded END)"

    /**
     * Converts Material 3 DateRangePicker UTC midnight timestamps into local-timezone
     * [startOfDay 00:00:00, endOfDay 23:59:59] epoch seconds so single-day and multi-day
     * ranges never shift across timezones.
     */
    fun utcPickerRangeToLocalEpochSec(
        startUtcMs: Long,
        endUtcMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val utc = TimeZone.getTimeZone("UTC")
        val startUtc = Calendar.getInstance(utc).apply { timeInMillis = startUtcMs }
        val endUtc = Calendar.getInstance(utc).apply { timeInMillis = endUtcMs }

        val localStart = Calendar.getInstance(timeZone).apply {
            clear()
            set(
                startUtc.get(Calendar.YEAR),
                startUtc.get(Calendar.MONTH),
                startUtc.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
        }
        val localEnd = Calendar.getInstance(timeZone).apply {
            clear()
            set(
                endUtc.get(Calendar.YEAR),
                endUtc.get(Calendar.MONTH),
                endUtc.get(Calendar.DAY_OF_MONTH),
                23, 59, 59
            )
        }
        return (localStart.timeInMillis / 1000L) to (localEnd.timeInMillis / 1000L)
    }

    fun formatUtcPickerRangeLabel(startUtcMs: Long, endUtcMs: Long): String {
        val fmt = java.text.SimpleDateFormat("MMM d, yy", java.util.Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return if (startUtcMs == endUtcMs) {
            fmt.format(java.util.Date(startUtcMs))
        } else {
            "${fmt.format(java.util.Date(startUtcMs))} – ${fmt.format(java.util.Date(endUtcMs))}"
        }
    }

    fun buildFilteredMediaQuery(
        filters: SearchFilterState,
        excludedBuckets: Set<String> = emptySet(),
        favoriteIds: Set<Long> = emptySet(),
        candidateIds: Collection<Long>? = null,
        limit: Int = 600
    ): BuiltFilterSql {
        val conditions = ArrayList<String>()
        val args = ArrayList<Any>()

        conditions.add("cm.bucketName != 'Trash'")
        conditions.add("cm.uriString NOT IN (SELECT originalUri FROM vault_media)")
        conditions.add("cm.filePath NOT IN (SELECT originalPath FROM vault_media)")

        // Respect excluded folders unless explicitly selected via Album filter
        val effectiveExcluded = excludedBuckets.filter { it !in filters.albumNames }
        if (effectiveExcluded.isNotEmpty()) {
            val placeholders = effectiveExcluded.joinToString(",") { "?" }
            conditions.add("cm.bucketName NOT IN ($placeholders)")
            args.addAll(effectiveExcluded)
        }

        // 1. Date Range
        filters.dateStartSec?.let { startSec ->
            conditions.add("$NORMALIZED_DATE_EXPR >= ?")
            args.add(startSec)
        }
        filters.dateEndSec?.let { endSec ->
            conditions.add("$NORMALIZED_DATE_EXPR <= ?")
            args.add(endSec)
        }

        // 2. Person / Face (multiple people = photos containing ALL selected people together)
        if (filters.personIds.isNotEmpty()) {
            for (personId in filters.personIds.sorted()) {
                conditions.add("EXISTS (SELECT 1 FROM faces f WHERE f.mediaId = cm.id AND f.personId = ?)")
                args.add(personId)
            }
        }

        // 3. Document / OCR Classification
        if (filters.documentCategories.isNotEmpty()) {
            val sortedDocs = filters.documentCategories.sorted()
            val placeholders = sortedDocs.joinToString(",") { "?" }
            conditions.add("cm.id IN (SELECT mediaId FROM document_classifications WHERE category IN ($placeholders))")
            args.addAll(sortedDocs)
        }

        // 4. Media Type
        when (filters.mediaType) {
            SearchMediaTypeFilter.ALL -> Unit
            SearchMediaTypeFilter.PHOTOS -> conditions.add("cm.isVideo = 0")
            SearchMediaTypeFilter.VIDEOS -> conditions.add("cm.isVideo = 1")
            SearchMediaTypeFilter.MOTION_PHOTOS -> {
                conditions.add("cm.isVideo = 0")
                conditions.add(
                    "(LOWER(COALESCE(cm.mimeType, '')) IN ('image/jpeg', 'image/jpg', 'image/heic', 'image/heif') " +
                        "OR LOWER(cm.name) LIKE '%.jpg' OR LOWER(cm.name) LIKE '%.jpeg' OR LOWER(cm.name) LIKE '%.heic')"
                )
                conditions.add("LOWER(cm.bucketName) NOT IN ('screenshots', 'screenshot')")
            }
            SearchMediaTypeFilter.SCREENSHOTS -> {
                conditions.add("(LOWER(cm.bucketName) IN ('screenshots', 'screenshot') OR LOWER(cm.filePath) LIKE '%/screenshots/%')")
            }
            SearchMediaTypeFilter.RAW -> {
                conditions.add(
                    "(LOWER(COALESCE(cm.mimeType, '')) LIKE '%raw%' OR LOWER(COALESCE(cm.mimeType, '')) LIKE '%dng%' " +
                        "OR LOWER(cm.name) LIKE '%.dng' OR LOWER(cm.name) LIKE '%.raw' OR LOWER(cm.name) LIKE '%.cr2' " +
                        "OR LOWER(cm.name) LIKE '%.nef' OR LOWER(cm.name) LIKE '%.arw' OR LOWER(cm.name) LIKE '%.orf' OR LOWER(cm.name) LIKE '%.raf')"
                )
            }
            SearchMediaTypeFilter.FAVORITES -> {
                if (favoriteIds.isEmpty()) {
                    conditions.add("1 = 0")
                } else {
                    val sortedFavs = favoriteIds.sorted()
                    val placeholders = sortedFavs.joinToString(",") { "?" }
                    conditions.add("cm.id IN ($placeholders)")
                    args.addAll(sortedFavs)
                }
            }
        }

        // 5. Album
        if (filters.albumNames.isNotEmpty()) {
            val sortedAlbums = filters.albumNames.sorted()
            val placeholders = sortedAlbums.joinToString(",") { "?" }
            conditions.add("cm.bucketName IN ($placeholders)")
            args.addAll(sortedAlbums)
        }

        // 6. Location (geocoded placeName)
        if (!filters.locationName.isNullOrBlank()) {
            conditions.add("cm.id IN (SELECT mediaId FROM geocoded_locations WHERE placeName = ?)")
            args.add(filters.locationName)
        }

        // Optional intersection with text/semantic search candidate IDs
        if (candidateIds != null) {
            if (candidateIds.isEmpty()) {
                conditions.add("1 = 0")
            } else {
                val sortedCandidates = candidateIds.distinct().sorted()
                val placeholders = sortedCandidates.joinToString(",") { "?" }
                conditions.add("cm.id IN ($placeholders)")
                args.addAll(sortedCandidates)
            }
        }

        val sql = buildString {
            append("SELECT cm.* FROM core_media cm WHERE ")
            append(conditions.joinToString(" AND "))
            if (filters.mediaType == SearchMediaTypeFilter.MOTION_PHOTOS) {
                append(" ORDER BY (CASE WHEN LOWER(cm.name) LIKE 'mvimg_%' OR LOWER(cm.name) LIKE '%.mp.%' OR LOWER(cm.name) LIKE '%_mp.%' THEN 0 ELSE 1 END), cm.dateAdded DESC LIMIT ?")
            } else {
                append(" ORDER BY cm.dateAdded DESC LIMIT ?")
            }
        }
        args.add(limit.coerceAtLeast(1))
        return BuiltFilterSql(sql, args)
    }
}
