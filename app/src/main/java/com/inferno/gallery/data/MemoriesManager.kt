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

        if (rawEntities.isEmpty()) return@withContext emptyList()

        // Filter out user-excluded folders
        val filtered = rawEntities.filter { entity ->
            entity.bucketName !in excludedFolders
        }

        if (filtered.isEmpty()) return@withContext emptyList()

        // Group entities by past year
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
                    val galleryItems = groupEntities.map { entity ->
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

                        GalleryItem(
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

                    // Pick cover: favorite first, or highest resolution / first item
                    val coverItem = galleryItems.firstOrNull { favoriteIds.contains(it.id) }
                        ?: galleryItems.first()

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

        resultGroups
    }
}
