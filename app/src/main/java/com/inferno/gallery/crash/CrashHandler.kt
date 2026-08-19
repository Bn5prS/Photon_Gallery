package com.inferno.gallery.crash

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Global uncaught-exception handler that intercepts crashes and launches
 * [CrashActivity] with a formatted crash report instead of the default
 * system "App has stopped" dialog.
 *
 * Install via [CrashHandler.install] in [Application.onCreate].
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var application: Application

    /** Install this handler as the default uncaught-exception handler. */
    fun install(app: Application) {
        application = app
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // ── Build crash report ──
            val report = buildCrashReport(thread, throwable)

            // ── Write to file ──
            val logFile = writeCrashLog(report)

            // ── Launch CrashActivity in a new process-clean task ──
            val intent = Intent(application, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashActivity.EXTRA_CRASH_LOG_PATH, logFile?.absolutePath ?: "")
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
            application.startActivity(intent)
        } catch (e: Exception) {
            // If our handler itself fails, fall back to the default handler
            e.printStackTrace()
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            // Kill the current process so the old Activity stack is gone
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    // ── Report Builder ──

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
            .format(Date())

        val appVersion = try {
            val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (_: Exception) { "Unknown" }

        return buildString {
            appendLine("═══════════════════════════════════════════")
            appendLine("  PHOTON GALLERY — CRASH REPORT")
            appendLine("═══════════════════════════════════════════")
            appendLine()
            appendLine("Timestamp   : $timestamp")
            appendLine("App Version : $appVersion")
            appendLine("Package     : ${application.packageName}")
            appendLine()
            appendLine("── Device ─────────────────────────────────")
            appendLine("Model       : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Device      : ${Build.DEVICE}")
            appendLine("Android     : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Fingerprint : ${Build.FINGERPRINT}")
            appendLine()
            appendLine("── Thread ─────────────────────────────────")
            appendLine("Name        : ${thread.name}")
            appendLine("ID          : ${thread.threadId()}")
            appendLine("Priority    : ${thread.priority}")
            appendLine()
            appendLine("── Exception ──────────────────────────────")
            appendLine("Type        : ${throwable.javaClass.name}")
            appendLine("Message     : ${throwable.message}")
            appendLine()
            appendLine("── Stack Trace ────────────────────────────")
            appendLine(stackTrace)
            appendLine()
            appendLine("── Memory ─────────────────────────────────")
            val runtime = Runtime.getRuntime()
            val maxMem = runtime.maxMemory() / (1024 * 1024)
            val totalMem = runtime.totalMemory() / (1024 * 1024)
            val freeMem = runtime.freeMemory() / (1024 * 1024)
            val usedMem = totalMem - freeMem
            appendLine("Max Heap    : ${maxMem}MB")
            appendLine("Used Heap   : ${usedMem}MB / ${totalMem}MB")
            appendLine("Free Heap   : ${freeMem}MB")
            appendLine()
            appendLine("═══════════════════════════════════════════")
        }
    }

    // ── File Writer ──

    private fun writeCrashLog(report: String): File? {
        return try {
            val crashDir = File(application.filesDir, "crash_logs")
            crashDir.mkdirs()

            // Keep only last 10 crash logs to avoid disk bloat
            val existing = crashDir.listFiles()?.sortedByDescending { it.lastModified() }
            existing?.drop(9)?.forEach { it.delete() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val logFile = File(crashDir, "crash_$timestamp.txt")
            logFile.writeText(report)
            logFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
