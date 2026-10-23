package com.inferno.gallery.data

import java.io.File

/**
 * Utility to identify whether a file or directory is hidden by a `.nomedia` file
 * or by a dot prefix (hidden folder) in its path hierarchy.
 */
object NoMediaUtils {

    private val storageRoots = setOf(
        "/",
        "/storage",
        "/storage/emulated",
        "/storage/emulated/0"
    )

    /**
     * Checks if the given file or any of its parent directories contains a `.nomedia` file
     * or is a hidden directory (starts with `.`).
     *
     * @param filePath Absolute path to the media file or folder.
     * @param cache Optional mutable map used to cache directory check results during batch processing.
     */
    fun isPathHiddenByNoMedia(filePath: String, cache: MutableMap<String, Boolean>? = null): Boolean {
        if (filePath.isBlank()) return false
        val file = File(filePath)
        return isDirectoryHiddenByNoMedia(file.parentFile, cache)
    }

    /**
     * Checks if a directory or any of its ancestors up to storage root contains a `.nomedia` file
     * or is hidden (starts with `.`).
     */
    fun isDirectoryHiddenByNoMedia(directory: File?, cache: MutableMap<String, Boolean>? = null): Boolean {
        var current: File? = directory ?: return false
        val traversedDirs = mutableListOf<String>()

        while (current != null) {
            val curr = current ?: break
            if (curr.path in storageRoots) break

            val path = curr.absolutePath
            val cachedResult = cache?.get(path)
            if (cachedResult != null) {
                traversedDirs.forEach { cache?.put(it, cachedResult) }
                return cachedResult
            }

            traversedDirs.add(path)

            // Check if directory name starts with dot (e.g. .GalleryVault)
            if (curr.name.startsWith(".")) {
                traversedDirs.forEach { cache?.put(it, true) }
                return true
            }

            // Check if .nomedia file exists in this directory
            val noMediaFile = File(curr, ".nomedia")
            if (noMediaFile.exists()) {
                traversedDirs.forEach { cache?.put(it, true) }
                return true
            }

            current = curr.parentFile
        }

        traversedDirs.forEach { cache?.put(it, false) }
        return false
    }
}
