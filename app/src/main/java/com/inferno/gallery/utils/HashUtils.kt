package com.inferno.gallery.utils

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {

    /**
     * Generates a SHA-256 hash of the media file.
     * Uses ContentResolver stream first for reliable Scoped Storage access on modern Android,
     * falling back to java.io.File if needed.
     */
    fun computeMediaHash(contentResolver: ContentResolver, uri: Uri, filePath: String? = null): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(65536) // 64KB chunks

            val stream: InputStream? = try {
                contentResolver.openInputStream(uri)
            } catch (_: Exception) {
                if (!filePath.isNullOrEmpty()) {
                    val file = File(filePath)
                    if (file.exists() && file.canRead()) FileInputStream(file) else null
                } else null
            }

            if (stream == null) return null

            stream.use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a SHA-256 hash of a file directly.
     */
    fun computeFileHash(file: File): String? {
        return try {
            if (!file.exists() || !file.canRead()) return null
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(65536)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
