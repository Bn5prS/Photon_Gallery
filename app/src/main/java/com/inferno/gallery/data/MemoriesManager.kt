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

        val monthStr = String.format(Locale.US, "%02d", currentMonth)
        val dayStr = String.format(Locale.US, "%02d", currentDay)
        val currentYearStr = currentYear.toString()

        // 1. Try exact day match across past years
        var rawEntities = mediaDao.getOnThisDayMedia(monthStr, dayStr, currentYearStr)
        var isExactDay = true

        // 2. If exact day has no memories, check adjacent days (±1 day) for nearby memories
        if (rawEntities.isEmpty()) {
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yMonth = String.format(Locale.US, "%02d", yesterdayCal.get(Calendar.MONTH) + 1)
            val yDay = String.format(Locale.US, "%02d", yesterdayCal.get(Calendar.DAY_OF_MONTH))
            val yesterdayEntities = mediaDao.getOnThisDayMedia(yMonth, yDay, currentYearStr)

            if (yesterdayEntities.isNotEmpty()) {
                rawEntities = yesterdayEntities
                isExactDay = false
            } else {
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tMonth = String.format(Locale.US, "%02d", tomorrowCal.get(Calendar.MONTH) + 1)
                val tDay = String.format(Locale.US, "%02d", tomorrowCal.get(Calendar.DAY_OF_MONTH))
                val tomorrowEntities = mediaDao.getOnThisDayMedia(tMonth, tDay, currentYearStr)
                if (tomorrowEntities.isNotEmpty()) {
                    rawEntities = tomorrowEntities
                    isExactDay = false
                }
            }
        }

        // Filter out user-excluded folders
        val pastYearFiltered = rawEntities.filter { it.bucketName !in excludedFolders }

        // If we found past-year memories, group them by year
        if (pastYearFiltered.isNotEmpty()) {
            val pastYearGroups = createPastYearGroups(
                filtered = pastYearFiltered,
                currentYear = currentYear,
                isExactDay = isExactDay,
                favoriteIds = favoriteIds,
                uriResolver = uriResolver
            )
            if (pastYearGroups.isNotEmpty()) {
                return@withContext pastYearGroups
            }
        }

        // ── Smart Fallback: Google Photos style Recent Highlights / Monthly Spotlight ──
        return@withContext createFallbackHighlights(
            mediaDao = mediaDao,
            excludedFolders = excludedFolders,
            favoriteIds = favoriteIds,
            uriResolver = uriResolver,
            currentYear = currentYear
        )
    }

    private fun createPastYearGroups(
        filtered: List<CoreMediaEntity>,
        currentYear: Int,
        isExactDay: Boolean,
        favoriteIds: Set<String>,
        uriResolver: ((uriString: String, filePath: String) -> Uri)?
    ): List<MemoryDayGroup> {
        val yearGroups = filtered.groupBy { entity ->
            val cal = Calendar.getInstance().apply {
                val ms = if (entity.dateAdded > 100_000_000_000L) entity.dateAdded else entity.dateAdded * 1000L
                timeInMillis = ms
            }
            cal.get(Calendar.YEAR)
        }

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val resultGroups = mutableListOf<MemoryDayGroup>()

        yearGroups.keys.sortedDescending().forEach { year ->
            val yearsAgo = currentYear - year
            if (yearsAgo > 0) {
                val groupEntities = yearGroups[year] ?: emptyList()
                if (groupEntities.isNotEmpty()) {
                    val galleryItems = groupEntities.map { entityToGalleryItem(it, uriResolver) }
                    val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) } ?: galleryItems.first()
                    val sampleDateMs = if (coverItem.dateAdded > 100_000_000_000L) {
                        coverItem.dateAdded
                    } else {
                        coverItem.dateAdded * 1000L
                    }
                    val formattedDate = dateFormat.format(Date(sampleDateMs))
                    val title = if (isExactDay) {
                        when (yearsAgo) {
                            1 -> "1 Year Ago Today"
                            else -> "$yearsAgo Years Ago Today"
                        }
                    } else {
                        when (yearsAgo) {
                            1 -> "Around This Time (1 Year Ago)"
                            else -> "Around This Time ($yearsAgo Years Ago)"
                        }
                    }

                    resultGroups.add(
                        MemoryDayGroup(
                            key = "memory_year_$year",
                            year = year,
                            yearsAgo = yearsAgo,
                            title = title,
                            formattedDate = formattedDate,
                            coverItem = coverItem,
                            items = galleryItems,
                            isExactDay = isExactDay
                        )
                    )
                }
            }
        }
        return resultGroups
    }

    private suspend fun createFallbackHighlights(
        mediaDao: MediaDao,
        excludedFolders: Set<String>,
        favoriteIds: Set<String>,
        uriResolver: ((uriString: String, filePath: String) -> Uri)?,
        currentYear: Int
    ): List<MemoryDayGroup> {
        var rawCandidates = mediaDao.getRecentHighlightMedia(limit = 120)
            .filter { it.bucketName !in excludedFolders }

        if (rawCandidates.isEmpty()) {
            rawCandidates = mediaDao.getAnyRecentMedia(limit = 60)
                .filter { it.bucketName !in excludedFolders }
        }

        if (rawCandidates.isEmpty()) return emptyList()

        val allItems = rawCandidates.map { entityToGalleryItem(it, uriResolver) }
        val resultGroups = mutableListOf<MemoryDayGroup>()

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Group by Year-Month
        val monthGroups = allItems.groupBy { item ->
            val cal = Calendar.getInstance().apply {
                val ms = if (item.dateAdded > 100_000_000_000L) item.dateAdded else item.dateAdded * 1000L
                timeInMillis = ms
            }
            cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
        }

        var groupIndex = 0
        for ((yearMonth, groupItems) in monthGroups) {
            if (resultGroups.size >= 3) break // Up to 3 curated cards
            if (groupItems.isEmpty()) continue

            val (itemYear, itemMonth) = yearMonth
            val sampleMs = if (groupItems.first().dateAdded > 100_000_000_000L) {
                groupItems.first().dateAdded
            } else {
                groupItems.first().dateAdded * 1000L
            }
            val sampleDate = Date(sampleMs)

            val title = when (groupIndex) {
                0 -> "Recent Highlights"
                1 -> "Best of ${monthFormat.format(sampleDate)}"
                else -> "Moments from ${monthFormat.format(sampleDate)}"
            }
            val formattedDate = monthYearFormat.format(sampleDate)

            val coverItem = groupItems.firstOrNull { favoriteIds.contains(it.id) } ?: groupItems.first()
            val limitedItems = groupItems.take(20)

            resultGroups.add(
                MemoryDayGroup(
                    key = "memory_fallback_${itemYear}_${itemMonth}_$groupIndex",
                    year = itemYear,
                    yearsAgo = (currentYear - itemYear).coerceAtLeast(0),
                    title = title,
                    formattedDate = formattedDate,
                    coverItem = coverItem,
                    items = limitedItems,
                    isExactDay = false
                )
            )
            groupIndex++
        }

        // Fallback safety: if grouping didn't produce anything, take first 15 as Recent Highlights
        if (resultGroups.isEmpty() && allItems.isNotEmpty()) {
            val sampleMs = if (allItems.first().dateAdded > 100_000_000_000L) {
                allItems.first().dateAdded
            } else {
                allItems.first().dateAdded * 1000L
            }
            val coverItem = allItems.firstOrNull { favoriteIds.contains(it.id) } ?: allItems.first()
            resultGroups.add(
                MemoryDayGroup(
                    key = "memory_fallback_recent_general",
                    year = currentYear,
                    yearsAgo = 0,
                    title = "Recent Highlights",
                    formattedDate = monthYearFormat.format(Date(sampleMs)),
                    coverItem = coverItem,
                    items = allItems.take(20),
                    isExactDay = false
                )
            )
        }

        return resultGroups
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
