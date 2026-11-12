package com.inferno.gallery.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.inferno.gallery.data.db.CoreMediaEntity
import com.inferno.gallery.data.db.MediaDao
import com.inferno.gallery.ui.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class MemoryDayGroup(
    val key: String,
    val year: Int,
    val yearsAgo: Int,
    val title: String,
    val formattedDate: String,
    val coverItem: GalleryItem,
    val items: List<GalleryItem>,
    val isExactDay: Boolean = true
)

object MemoriesManager {

    suspend fun loadOnThisDayMemories(
        mediaDao: MediaDao,
        excludedFolders: Set<String> = emptySet(),
        favoriteIds: Set<String> = emptySet(),
        uriResolver: ((uriString: String, filePath: String) -> Uri)? = null
    ): List<MemoryDayGroup> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentYearStr = currentYear.toString()
        val currentMonthStr = String.format(Locale.US, "%02d", currentMonth)

        val finalMemoryGroups = mutableListOf<MemoryDayGroup>()
        val usedMediaIds = mutableSetOf<Long>()
        val yearsCovered = mutableSetOf<Int>()

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val monthNameFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // ─────────────────────────────────────────────────────────────
        // 1. "ON THIS DAY" & "THIS WEEK" ANNIVERSARIES (±3 days window)
        // ─────────────────────────────────────────────────────────────
        val windowDays = (-3..3).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val m = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
            "$m-$d"
        }.distinct()

        val rangeMedia = mediaDao.getOnThisDayRangeMedia(windowDays, currentYearStr)
            .filter { it.bucketName !in excludedFolders }

        if (rangeMedia.isNotEmpty()) {
            val yearGroups = rangeMedia.groupBy { entity ->
                val cal = Calendar.getInstance().apply {
                    val ms = if (entity.dateAdded > 100_000_000_000L) entity.dateAdded else entity.dateAdded * 1000L
                    timeInMillis = ms
                }
                cal.get(Calendar.YEAR)
            }

            yearGroups.keys.sortedDescending().forEach { year ->
                val yearsAgo = currentYear - year
                if (yearsAgo > 0) {
                    val groupEntities = yearGroups[year] ?: emptyList()
                    if (groupEntities.isNotEmpty()) {
                        val galleryItems = groupEntities.map { entityToGalleryItem(it, uriResolver) }
                        val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()

                        val cal = Calendar.getInstance().apply {
                            val ms = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L
                            timeInMillis = ms
                        }
                        val itemDay = cal.get(Calendar.DAY_OF_MONTH)
                        val isExactDay = (itemDay == currentDay)

                        val title = if (isExactDay) {
                            when (yearsAgo) {
                                1 -> "1 Year Ago Today"
                                else -> "$yearsAgo Years Ago Today"
                            }
                        } else {
                            when (yearsAgo) {
                                1 -> "This Week 1 Year Ago"
                                else -> "This Week ($yearsAgo Years Ago)"
                            }
                        }

                        val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L
                        val formattedDate = dateFormat.format(Date(sampleDateMs))

                        finalMemoryGroups.add(
                            MemoryDayGroup(
                                key = "memory_anniversary_$year",
                                year = year,
                                yearsAgo = yearsAgo,
                                title = title,
                                formattedDate = formattedDate,
                                coverItem = coverItem,
                                items = galleryItems,
                                isExactDay = isExactDay
                            )
                        )
                        yearsCovered.add(year)
                        groupEntities.forEach { usedMediaIds.add(it.id) }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. THIS MONTH IN PAST YEARS (e.g. November 2024, November 2023)
        // ─────────────────────────────────────────────────────────────
        val monthMedia = mediaDao.getPastYearsMonthMedia(currentMonthStr, currentYearStr, limit = 350)
            .filter { it.bucketName !in excludedFolders && it.id !in usedMediaIds }

        if (monthMedia.isNotEmpty()) {
            val monthYearGroups = monthMedia.groupBy { entity ->
                val cal = Calendar.getInstance().apply {
                    val ms = if (entity.dateAdded > 100_000_000_000L) entity.dateAdded else entity.dateAdded * 1000L
                    timeInMillis = ms
                }
                cal.get(Calendar.YEAR)
            }

            monthYearGroups.keys.sortedDescending().forEach { year ->
                if (year !in yearsCovered || finalMemoryGroups.size < 5) {
                    val groupEntities = monthYearGroups[year] ?: emptyList()
                    if (groupEntities.size >= 2) {
                        val galleryItems = groupEntities.map { entityToGalleryItem(it, uriResolver) }
                        val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()
                        val yearsAgo = currentYear - year
                        val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L
                        val sampleDate = Date(sampleDateMs)

                        val monthName = monthNameFormat.format(sampleDate)
                        val title = if (yearsAgo == 1) {
                            "$monthName Last Year"
                        } else {
                            "$monthName ($yearsAgo Years Ago)"
                        }
                        val formattedDate = "$monthName $year • $yearsAgo ${if (yearsAgo == 1) "year" else "years"} ago"

                        finalMemoryGroups.add(
                            MemoryDayGroup(
                                key = "memory_month_${year}_$currentMonthStr",
                                year = year,
                                yearsAgo = yearsAgo,
                                title = title,
                                formattedDate = formattedDate,
                                coverItem = coverItem,
                                items = galleryItems.take(25),
                                isExactDay = false
                            )
                        )
                        yearsCovered.add(year)
                        groupEntities.forEach { usedMediaIds.add(it.id) }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. ARCHIVAL CLUSTERS & TRIPS (Older years / historical months)
        // ─────────────────────────────────────────────────────────────
        if (finalMemoryGroups.size < 8) {
            val sixMonthsAgoSec = (System.currentTimeMillis() / 1000L) - (180L * 24 * 3600)
            val historicalMedia = mediaDao.getHistoricalMedia(sixMonthsAgoSec, limit = 500)
                .filter { it.bucketName !in excludedFolders && it.id !in usedMediaIds }

            if (historicalMedia.isNotEmpty()) {
                val historicalGroups = historicalMedia.groupBy { entity ->
                    val cal = Calendar.getInstance().apply {
                        val ms = if (entity.dateAdded > 100_000_000_000L) entity.dateAdded else entity.dateAdded * 1000L
                        timeInMillis = ms
                    }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                }

                for ((yearMonth, groupEntities) in historicalGroups) {
                    if (finalMemoryGroups.size >= 8) break
                    if (groupEntities.size < 2) continue

                    val (itemYear, itemMonth) = yearMonth
                    val yearsAgo = (currentYear - itemYear).coerceAtLeast(0)

                    val galleryItems = groupEntities.map { entityToGalleryItem(it, uriResolver) }
                    val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()
                    val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L
                    val sampleDate = Date(sampleDateMs)

                    val topBucket = groupEntities.groupBy { it.bucketName }
                        .maxByOrNull { it.value.size }?.key

                    val isGeneralFolder = topBucket in listOf("Camera", "Pictures", "DCIM", "Photos", "0", null)
                    val title = when {
                        !isGeneralFolder && !topBucket.isNullOrBlank() -> "Trip: $topBucket ($itemYear)"
                        yearsAgo > 1 -> "Flashback: ${monthYearFormat.format(sampleDate)}"
                        yearsAgo == 1 -> "${monthNameFormat.format(sampleDate)} Last Year"
                        else -> "Moments from ${monthYearFormat.format(sampleDate)}"
                    }
                    val formattedDate = monthYearFormat.format(sampleDate)

                    finalMemoryGroups.add(
                        MemoryDayGroup(
                            key = "memory_archive_${itemYear}_${itemMonth}_${topBucket.orEmpty()}",
                            year = itemYear,
                            yearsAgo = yearsAgo,
                            title = title,
                            formattedDate = formattedDate,
                            coverItem = coverItem,
                            items = galleryItems.take(30),
                            isExactDay = false
                        )
                    )
                    groupEntities.forEach { usedMediaIds.add(it.id) }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 4. RECENT HIGHLIGHTS (Capped at exactly 1 card)
        // ─────────────────────────────────────────────────────────────
        val recentCandidates = mediaDao.getRecentHighlightMedia(limit = 40)
            .filter { it.bucketName !in excludedFolders && it.id !in usedMediaIds }

        if (recentCandidates.isNotEmpty()) {
            val galleryItems = recentCandidates.map { entityToGalleryItem(it, uriResolver) }
            val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()
            val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L

            finalMemoryGroups.add(
                MemoryDayGroup(
                    key = "memory_recent_highlights",
                    year = currentYear,
                    yearsAgo = 0,
                    title = "Recent Highlights",
                    formattedDate = monthYearFormat.format(Date(sampleDateMs)),
                    coverItem = coverItem,
                    items = galleryItems.take(20),
                    isExactDay = false
                )
            )
        }

        // Safety fallback: if database is tiny and has no past memories at all
        if (finalMemoryGroups.isEmpty()) {
            val anyRecent = mediaDao.getAnyRecentMedia(limit = 30)
                .filter { it.bucketName !in excludedFolders }
            if (anyRecent.isNotEmpty()) {
                val galleryItems = anyRecent.map { entityToGalleryItem(it, uriResolver) }
                val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()
                val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) coverItem.dateAdded else coverItem.dateAdded * 1000L

                finalMemoryGroups.add(
                    MemoryDayGroup(
                        key = "memory_fallback_recent_general",
                        year = currentYear,
                        yearsAgo = 0,
                        title = "Recent Highlights",
                        formattedDate = monthYearFormat.format(Date(sampleDateMs)),
                        coverItem = coverItem,
                        items = galleryItems.take(20),
                        isExactDay = false
                    )
                )
            }
        }

        return@withContext finalMemoryGroups
    }

    private fun entityToGalleryItem(
        entity: CoreMediaEntity,
        uriResolver: ((uriString: String, filePath: String) -> Uri)?
    ): GalleryItem {
        val uri = if (uriResolver != null) {
            uriResolver(entity.uriString, entity.filePath)
        } else {
            Uri.parse(entity.uriString)
        }
        val resolved = if (uriResolver != null) {
            uri
        } else if (entity.filePath.isNotBlank()) {
            val f = File(entity.filePath)
            if (f.exists()) Uri.fromFile(f) else uri
        } else uri

        return GalleryItem(
            id = entity.id.toString(),
            uri = uri,
            bucketName = entity.bucketName,
            dateAdded = entity.dateAdded,
            size = entity.size,
            name = entity.name,
            dateModified = entity.dateModified,
            path = entity.filePath,
            isVideo = entity.isVideo,
            durationMs = entity.durationMs,
            localExists = true,
            resolvedUri = resolved,
            latitude = entity.latitude,
            longitude = entity.longitude
        )
    }
}
