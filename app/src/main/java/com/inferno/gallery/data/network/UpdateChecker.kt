package com.inferno.gallery.data.network

import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val isAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val releasePageUrl: String
)

object UpdateChecker {
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Bn5prS/Photon_Gallery/releases?per_page=5"
    private const val TAG = "UpdateChecker"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "PhotonGallery-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub releases API returned HTTP ${response.code}")
                    // If HTTP 404, the repo has no published releases yet -> already up to date
                    if (response.code == 404) {
                        return@withContext UpdateInfo(
                            isAvailable = false,
                            currentVersion = currentVersion,
                            latestVersion = currentVersion,
                            releaseTitle = "Latest",
                            changelog = "",
                            downloadUrl = null,
                            apkFileName = null,
                            apkSizeBytes = 0L,
                            releasePageUrl = "https://github.com/Bn5prS/Photon_Gallery/releases"
                        )
                    }
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null

                // Parse release object: supports JSONArray (from /releases) or single JSONObject (from /releases/latest)
                val releaseJson: JSONObject? = try {
                    val trimmed = responseBody.trimStart()
                    if (trimmed.startsWith("[")) {
                        val array = JSONArray(trimmed)
                        var picked: JSONObject? = null
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            // Include regular releases and prereleases, skip only drafts
                            if (!item.optBoolean("draft", false)) {
                                picked = item
                                break
                            }
                        }
                        picked
                    } else {
                        val obj = JSONObject(trimmed)
                        if (!obj.optBoolean("draft", false)) obj else null
                    }
                } catch (e: Exception) {
                    null
                }

                if (releaseJson == null) {
                    // No published release found -> current build is up to date
                    return@withContext UpdateInfo(
                        isAvailable = false,
                        currentVersion = currentVersion,
                        latestVersion = currentVersion,
                        releaseTitle = "Latest",
                        changelog = "",
                        downloadUrl = null,
                        apkFileName = null,
                        apkSizeBytes = 0L,
                        releasePageUrl = "https://github.com/Bn5prS/Photon_Gallery/releases"
                    )
                }

                val tagName = releaseJson.optString("tag_name", "").trim()
                val releaseTitle = releaseJson.optString("name", tagName).ifBlank { tagName }
                val changelog = releaseJson.optString("body", "").trim()
                val htmlUrl = releaseJson.optString("html_url", "https://github.com/Bn5prS/Photon_Gallery/releases")

                val latestCleanVersion = cleanVersionString(tagName)
                val currentCleanVersion = cleanVersionString(currentVersion)

                val isAvailable = isNewerVersion(currentCleanVersion, latestCleanVersion)

                // Locate the most compatible APK asset matching device ABI
                val assetsJson = releaseJson.optJSONArray("assets") ?: JSONArray()
                val selectedAsset = pickBestApkAsset(assetsJson)

                val downloadUrl = selectedAsset?.optString("browser_download_url")?.takeIf { it.isNotBlank() }
                val apkFileName = selectedAsset?.optString("name")
                val apkSizeBytes = selectedAsset?.optLong("size", 0L) ?: 0L

                UpdateInfo(
                    isAvailable = isAvailable,
                    currentVersion = currentVersion,
                    latestVersion = tagName.ifBlank { latestCleanVersion },
                    releaseTitle = releaseTitle,
                    changelog = changelog,
                    downloadUrl = downloadUrl,
                    apkFileName = apkFileName,
                    apkSizeBytes = apkSizeBytes,
                    releasePageUrl = htmlUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates: ${e.message}", e)
            null
        }
    }

    private fun pickBestApkAsset(assets: JSONArray): JSONObject? {
        val apkList = mutableListOf<JSONObject>()
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "").lowercase()
            if (name.endsWith(".apk")) {
                apkList.add(asset)
            }
        }

        if (apkList.isEmpty()) return null

        // 1. Try to find an APK matching the primary or supported ABIs
        val supportedAbis = Build.SUPPORTED_ABIS.map { it.lowercase() }
        for (abi in supportedAbis) {
            val match = apkList.firstOrNull { asset ->
                val name = asset.optString("name", "").lowercase()
                name.contains(abi)
            }
            if (match != null) return match
        }

        // 2. Fall back to universal APK if available
        val universalMatch = apkList.firstOrNull { asset ->
            asset.optString("name", "").lowercase().contains("universal")
        }
        if (universalMatch != null) return universalMatch

        // 3. Fall back to the first available APK
        return apkList.firstOrNull()
    }

    private fun cleanVersionString(version: String): String {
        return version.removePrefix("v").removePrefix("V").trim()
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        if (current.isBlank() || latest.isBlank()) return false
        if (current.equals(latest, ignoreCase = true)) return false

        // Extract numbers from version strings like "1.0.4 (beta)" -> [1, 0, 4]
        val currentParts = extractVersionDigits(current)
        val latestParts = extractVersionDigits(latest)

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun extractVersionDigits(version: String): List<Int> {
        val regex = Regex("\\d+")
        return regex.findAll(version).mapNotNull { it.value.toIntOrNull() }.toList()
    }
}
