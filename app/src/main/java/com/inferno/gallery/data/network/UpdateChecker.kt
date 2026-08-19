package com.inferno.gallery.data.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val isAvailable: Boolean,
    val version: String,
    val url: String
)

object UpdateChecker {
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/Bn5prS/Photon_Gallery/releases/latest"
    private val client = OkHttpClient()

    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)
                
                val tagName = json.getString("tag_name")
                val htmlUrl = json.getString("html_url")

                // Strip 'v' prefix if present
                val latestVersion = tagName.removePrefix("v")
                val current = currentVersion.removePrefix("v")

                // Simple string comparison for versions like "2.5.0" vs "2.6.0"
                // Assuming semantic versioning format
                val isAvailable = isNewerVersion(current, latestVersion)

                return@withContext UpdateInfo(
                    isAvailable = isAvailable,
                    version = tagName,
                    url = htmlUrl
                )
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Failed to check for updates: ${e.message}")
            null
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
