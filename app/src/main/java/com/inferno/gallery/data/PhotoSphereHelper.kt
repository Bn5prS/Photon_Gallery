package com.inferno.gallery.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Helper to differentiate 360° Photo Spheres from standard Panoramas.
 *
 * Google Camera and 360 camera apps embed Google Panorama (`GPano`) XMP metadata
 * inside equirectangular spherical captures:
 * - `GPano:UsePanoramaViewer="True"`
 * - `GPano:ProjectionType="equirectangular"`
 *
 * In contrast, standard cylindrical panoramas either lack GPano tags entirely
 * or specify `GPano:ProjectionType="cylindrical"`.
 */
object PhotoSphereHelper {

    private const val HEADER_SCAN_LIMIT = 65536 // First 64KB contains APP1/XMP markers

    /**
     * Checks if a given file or URI represents a 360° Photo Sphere.
     */
    fun isPhotoSphere(context: Context?, filePath: String?, uriString: String?): Boolean {
        if (filePath.isNullOrEmpty() && uriString.isNullOrEmpty()) return false

        // 1. Quick check based on filename / folder path conventions
        if (!filePath.isNullOrEmpty() && hasPhotoSphereNaming(filePath)) return true
        if (!uriString.isNullOrEmpty() && hasPhotoSphereNaming(uriString)) return true

        // 2. Check XMP metadata inside the JPEG header via direct file read
        if (!filePath.isNullOrEmpty()) {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                val res = isEquirectangularSphere(file)
                if (res) return true
            }
        }

        // 3. Fallback to ContentResolver if direct file read wasn't readable or was omitted
        if (context != null && !uriString.isNullOrEmpty()) {
            return try {
                val uri = Uri.parse(uriString)
                isEquirectangularSphere(context, uri)
            } catch (_: Exception) {
                false
            }
        }

        return false
    }

    /**
     * Checks if a given path, file, or URI represents a 360° Photo Sphere.
     */
    fun isPhotoSphere(context: Context?, pathOrUri: String?): Boolean =
        isPhotoSphere(context, filePath = pathOrUri, uriString = pathOrUri)

    /**
     * Checks filename or folder path for explicit 360 / sphere keywords.
     */
    fun hasPhotoSphereNaming(pathOrName: String): Boolean {
        val upper = pathOrName.uppercase()
        return upper.contains("PHOTOSPHERE") ||
               upper.contains("_SPHERE") ||
               upper.contains("SPHERE_") ||
               upper.contains("360_PHOTO") ||
               upper.contains("_360.") ||
               upper.contains("/PHOTOSPHERE/") ||
               upper.contains("/360/")
    }

    fun isEquirectangularSphere(file: File): Boolean {
        if (!file.exists() || !file.canRead() || file.length() < 100) return false
        return try {
            file.inputStream().use { stream ->
                isEquirectangularSphere(stream, file.length())
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isEquirectangularSphere(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                isEquirectangularSphere(stream, HEADER_SCAN_LIMIT.toLong())
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Inspects the first 64KB of a JPEG stream for GPano equirectangular tags.
     */
    fun isEquirectangularSphere(stream: InputStream, totalSize: Long): Boolean {
        val headerLimit = minOf(totalSize.toInt().coerceAtLeast(HEADER_SCAN_LIMIT), HEADER_SCAN_LIMIT)
        val headerBytes = ByteArray(headerLimit)
        var totalRead = 0
        while (totalRead < headerLimit) {
            val read = stream.read(headerBytes, totalRead, headerLimit - totalRead)
            if (read <= 0) break
            totalRead += read
        }
        if (totalRead < 4) return false

        // Verify JPEG SOI marker (0xFF, 0xD8)
        if ((headerBytes[0].toInt() and 0xFF) != 0xFF || (headerBytes[1].toInt() and 0xFF) != 0xD8) {
            return false
        }

        val headerText = String(headerBytes, 0, totalRead, StandardCharsets.ISO_8859_1)

        val hasViewer = headerText.contains("GPano:UsePanoramaViewer=\"True\"", ignoreCase = true) ||
                        headerText.contains("GPano:UsePanoramaViewer>True<", ignoreCase = true)

        val isEquirectangular = headerText.contains("GPano:ProjectionType=\"equirectangular\"", ignoreCase = true) ||
                                headerText.contains("GPano:ProjectionType>equirectangular<", ignoreCase = true)

        return hasViewer || isEquirectangular
    }
}
