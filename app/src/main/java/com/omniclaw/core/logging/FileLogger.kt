package com.omniclaw.core.logging

import android.content.Context
import android.os.Build
import com.omniclaw.BuildConfig
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object FileLogger {

    private const val LOG_DIR = "logs"
    private const val MAX_LOG_FILES = 7
    private const val MAX_CRASH_FILES = 10
    private const val TAG = "FileLogger"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val executor = Executors.newSingleThreadExecutor()

    private var logDir: File? = null
    private var isInitialized = false
    private var originalExceptionHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (isInitialized) return
        logDir = context.getExternalFilesDir(LOG_DIR)
        logDir?.let { dir ->
            if (!dir.exists()) dir.mkdirs()
            cleanOldLogs(dir)
        }
        isInitialized = true
        installCrashHandler()
        i(TAG, "FileLogger initialized at: ${logDir?.absolutePath}")
        i(TAG, "App version: ${BuildConfig.VERSION_NAME}, SDK: ${Build.VERSION.SDK_INT}")
    }

    private fun installCrashHandler() {
        originalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logException("UNCAUGHT_CRASH", throwable, thread)
            originalExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    fun d(tag: String, msg: String) = write("D", tag, msg, null)
    fun i(tag: String, msg: String) = write("I", tag, msg, null)
    fun w(tag: String, msg: String) = write("W", tag, msg, null)
    fun e(tag: String, msg: String) = write("E", tag, msg, null)
    fun e(tag: String, msg: String, throwable: Throwable?) = write("E", tag, msg, throwable)

    private fun write(level: String, tag: String, msg: String, throwable: Throwable?) {
        if (!isInitialized) return
        val time = timeFormat.format(Date())
        val threadName = Thread.currentThread().name
        val line = buildString {
            append("$time | $level | $threadName | $tag | $msg")
            if (throwable != null) {
                append("\n")
                append(throwable.stackTraceToString())
            }
            append("\n")
        }
        executor.execute {
            logDir?.let { dir ->
                val file = File(dir, "app_${dateFormat.format(Date())}.log")
                try {
                    FileWriter(file, true).use { it.append(line) }
                } catch (_: Exception) {
                    // best effort
                }
            }
        }
    }

    private fun logException(level: String, throwable: Throwable, thread: Thread?) {
        if (!isInitialized) return
        val time = timeFormat.format(Date())
        val threadName = thread?.name ?: "unknown"
        val tag = "CRASH"
        val header = buildString {
            append("$time | E | $threadName | $tag | === $level ===\n")
            append("$time | E | $threadName | $tag | Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("$time | E | $threadName | $tag | Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("$time | E | $threadName | $tag | App: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\n")
            append("$time | E | $threadName | $tag | ${throwable.javaClass.name}: ${throwable.message}\n")
            append(throwable.stackTraceToString().lines().joinToString("\n") { line ->
                "$time | E | $threadName | $tag |   $line"
            })
            append("\n")
        }
        val crashFile = File(logDir, "crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log")
        executor.execute {
            try {
                FileWriter(crashFile, false).use { it.append(header) }
                val mainFile = File(logDir, "app_${dateFormat.format(Date())}.log")
                FileWriter(mainFile, true).use { it.append(header) }
                cleanCrashLogs(logDir!!)
            } catch (_: Exception) { }
        }
    }

    private fun cleanOldLogs(dir: File) {
        val files = dir.listFiles { f -> f.name.matches(Regex("app_\\d{4}-\\d{2}-\\d{2}\\.log")) }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > MAX_LOG_FILES) {
            files.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }

    private fun cleanCrashLogs(dir: File) {
        val files = dir.listFiles { f -> f.name.startsWith("crash_") }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > MAX_CRASH_FILES) {
            files.drop(MAX_CRASH_FILES).forEach { it.delete() }
        }
    }
}
