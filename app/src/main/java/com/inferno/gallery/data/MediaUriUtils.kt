package com.inferno.gallery.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

/**
 * Utility functions for canonicalizing MediaStore URIs and validating their existence.
 *
 * MediaStore operations such as createDeleteRequest and createTrashRequest in AOSP MediaProvider
 * strictly require canonical "content://media/external/..." URIs and will throw
 * IllegalArgumentException if given "content://media/external_primary/..." URIs or URIs whose
 * underlying records no longer exist.
 */
object MediaUriUtils {
    private const val TAG = "MediaUriUtils"

    /**
     * Normalizes non-standard MediaStore volume URIs (e.g. content://media/external_primary/images/media/123)
     * to canonical MediaStore external URIs (content://media/external/images/media/123) that Android's
     * MediaProvider, createDeleteRequest, and createTrashRequest require.
     */
    fun normalizeMediaUri(uri: Uri): Uri {
        val uriStr = uri.toString()
        val normalized = normalizeUriString(uriStr)
        return if (normalized != uriStr) Uri.parse(normalized) else uri
    }

    /**
     * Replaces "content://media/external_primary/" with canonical "content://media/external/".
     */
    fun normalizeUriString(uriString: String): String {
        return if (uriString.startsWith("content://media/external_primary/")) {
            uriString.replaceFirst("content://media/external_primary/", "content://media/external/")
        } else {
            uriString
        }
    }

    /**
     * Inspects a collection of URIs against Android MediaStore to partition them into:
     * - first: List<Uri> of verified, canonicalized URIs safe to pass to MediaStore.createDeleteRequest / createTrashRequest.
     * - second: List<Uri> of ghost URIs that no longer exist in MediaStore.
     */
    fun validateAndSanitizeUris(context: Context, uris: Collection<Uri>): Pair<List<Uri>, List<Uri>> {
        if (uris.isEmpty()) return Pair(emptyList(), emptyList())

        val validUris = mutableListOf<Uri>()
        val ghostUris = mutableListOf<Uri>()
        val idToUris = mutableMapOf<Long, MutableList<Uri>>()

        for (uri in uris) {
            val normUri = normalizeMediaUri(uri)
            val id = try {
                ContentUris.parseId(normUri)
            } catch (_: Exception) {
                null
            }

            if (id != null && normUri.authority == MediaStore.AUTHORITY) {
                idToUris.getOrPut(id) { mutableListOf() }.add(normUri)
            } else {
                // Non-MediaStore or non-ID URI (e.g. file:// scheme), pass through
                validUris.add(normUri)
            }
        }

        if (idToUris.isNotEmpty()) {
            val existingIds = mutableSetOf<Long>()
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val allIds = idToUris.keys.toList()

            allIds.chunked(500).forEach { chunk ->
                val selection = "${MediaStore.MediaColumns._ID} IN (${chunk.joinToString(",")})"
                val bundle = Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1) // 1 = MATCH_INCLUDE (both active and trashed)
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                }

                try {
                    context.contentResolver.query(
                        MediaStore.Files.getContentUri("external"),
                        projection,
                        bundle,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                        if (idCol >= 0) {
                            while (cursor.moveToNext()) {
                                existingIds.add(cursor.getLong(idCol))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaStore validation query failed; treating chunk as valid", e)
                    existingIds.addAll(chunk)
                }
            }

            for ((id, list) in idToUris) {
                if (existingIds.contains(id)) {
                    validUris.addAll(list)
                } else {
                    ghostUris.addAll(list)
                }
            }
        }

        return Pair(validUris, ghostUris)
    }
}

