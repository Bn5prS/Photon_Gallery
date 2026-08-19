package com.inferno.gallery.crash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.inferno.gallery.ui.theme.PhotonGalleryTheme
import java.io.File

/**
 * Standalone Activity displayed after an unhandled crash.
 *
 * This is a **separate** Activity (not part of the main task stack) so
 * that it survives the death of the crashing process. It receives the
 * crash report text and log file path via Intent extras.
 *
 * Features:
 * - Beautiful "Something Went Wrong" UI
 * - Expandable crash details
 * - Share / save crash log
 * - "Create Issue" → GitHub Issues
 * - "Restart App" → re-launches MainActivity
 */
class CrashActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "crash_report"
        const val EXTRA_CRASH_LOG_PATH = "crash_log_path"
        private const val GITHUB_ISSUES_URL = "https://github.com/Bn5prS/Photon_Gallery/issues/new"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashReport = intent.getStringExtra(EXTRA_CRASH_REPORT) ?: "No crash data available."
        val crashLogPath = intent.getStringExtra(EXTRA_CRASH_LOG_PATH) ?: ""

        setContent {
            PhotonGalleryTheme(darkTheme = true) {
                CrashScreen(
                    crashReport = crashReport,
                    onShareLog = { shareCrashLog(crashLogPath, crashReport) },
                    onCreateIssue = { openGitHubIssue(crashReport) },
                    onRestartApp = { restartApp() },
                    onCopyLog = { copyCrashLog(crashReport) }
                )
            }
        }
    }

    private fun shareCrashLog(logPath: String, report: String) {
        try {
            val file = if (logPath.isNotBlank()) File(logPath) else null
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Photon Gallery Crash Report")
                    putExtra(Intent.EXTRA_TEXT, "Crash report attached.\n\nPlease include steps to reproduce if possible.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
            } else {
                // Fallback: share as text
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Photon Gallery Crash Report")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Crash Log"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGitHubIssue(report: String) {
        // Extract just the exception type + message for the title
        val exceptionLine = report.lines()
            .firstOrNull { it.startsWith("Type") }
            ?.substringAfter(": ")
            ?: "Unknown Crash"
        val messageLine = report.lines()
            .firstOrNull { it.startsWith("Message") }
            ?.substringAfter(": ")
            ?: ""

        val title = Uri.encode("[Crash] $exceptionLine: $messageLine".take(200))

        // Truncate the full report for the issue body (URLs have length limits)
        val truncatedReport = report.take(1500)
        val body = Uri.encode(
            """
            |## Crash Report
            |
            |**Steps to reproduce:**
            |(Please describe what you were doing when the crash happened)
            |
            |1. 
            |2. 
            |3. 
            |
            |<details>
            |<summary>Crash Log (click to expand)</summary>
            |
            |```
            |$truncatedReport
            |```
            |
            |</details>
            |
            |> Full crash log is available in the app (Settings → About → Crash Logs) or via the Share button on the crash screen.
            """.trimMargin()
        )

        val url = "$GITHUB_ISSUES_URL?title=$title&body=$body&labels=bug,crash"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyCrashLog(report: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Photon Gallery Crash Log", report)
        clipboard.setPrimaryClip(clip)
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finishAffinity()
    }
}
