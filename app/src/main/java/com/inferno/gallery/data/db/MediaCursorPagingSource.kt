package com.inferno.gallery.data.db

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keyset cursor representing the seek position in the ordered media dataset.
 * Combines [dateAdded] with [id] as a strict tie-breaker for deterministic pagination.
 */
data class MediaCursor(
    val dateAdded: Long,
    val id: Long
)

/**
 * High-performance Keyset/Cursor PagingSource for Photon Gallery.
 *
 * Replaces O(N) LIMIT/OFFSET queries with O(log N) index seek operations:
 *   WHERE ... AND (cm.dateAdded < :lastDate OR (cm.dateAdded = :lastDate AND cm.id < :lastId))
 *   ORDER BY cm.dateAdded DESC, cm.id DESC LIMIT :limit
 *
 * Utilizes composite index `(bucketName, dateAdded DESC, id DESC)` / `(dateAdded DESC, id DESC)`.
 * Automatically observes Room table invalidation for "core_media".
 */
class MediaCursorPagingSource(
    private val database: GalleryDatabase,
    private val whereConditions: List<String>,
    private val args: List<Any>,
    private val isAscending: Boolean = false
) : PagingSource<MediaCursor, CoreMediaProjection>() {

    private val observer = object : InvalidationTracker.Observer("core_media") {
        override fun onInvalidated(tables: Set<String>) {
            invalidate()
        }
    }

    init {
        database.invalidationTracker.addObserver(observer)
        registerInvalidatedCallback {
            database.invalidationTracker.removeObserver(observer)
        }
    }

    override fun getRefreshKey(state: PagingState<MediaCursor, CoreMediaProjection>): MediaCursor? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)?.let { item ->
                MediaCursor(item.dateAdded, item.id)
            }
        }
    }

    override suspend fun load(params: LoadParams<MediaCursor>): LoadResult<MediaCursor, CoreMediaProjection> {
        return withContext(Dispatchers.IO) {
            try {
                val cursor = params.key
                val limit = params.loadSize

                val conditions = ArrayList(whereConditions)
                val queryArgs = ArrayList(args)

                if (cursor != null) {
                    if (isAscending) {
                        conditions.add("(cm.dateAdded > ? OR (cm.dateAdded = ? AND cm.id > ?))")
                    } else {
                        conditions.add("(cm.dateAdded < ? OR (cm.dateAdded = ? AND cm.id < ?))")
                    }
                    queryArgs.add(cursor.dateAdded)
                    queryArgs.add(cursor.dateAdded)
                    queryArgs.add(cursor.id)
                }

                val whereSql = if (conditions.isNotEmpty()) {
                    "WHERE ${conditions.joinToString(" AND ")}"
                } else ""

                val orderSql = if (isAscending) {
                    "ORDER BY cm.dateAdded ASC, cm.id ASC"
                } else {
                    "ORDER BY cm.dateAdded DESC, cm.id DESC"
                }

                val sql = "SELECT cm.* FROM core_media cm $whereSql $orderSql LIMIT $limit"
                val query = SimpleSQLiteQuery(sql, queryArgs.toArray())
                val items = database.mediaDao().getMediaRaw(query)

                val nextKey = if (items.size < limit) {
                    null
                } else {
                    val last = items.last()
                    MediaCursor(last.dateAdded, last.id)
                }

                LoadResult.Page(
                    data = items,
                    prevKey = null,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}
