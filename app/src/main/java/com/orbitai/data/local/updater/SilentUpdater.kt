package com.orbitai.data.local.updater

import android.content.Context
import com.orbitai.core.logging.FileLogger
import com.orbitai.data.local.runtime.OrbitAiRuntimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private const val TAG = "SilentUpdater"

/**
 * Installs / updates an APK silently (no system "install?" prompt) using
 * Shizuku.
 *
 * How the silence works:
 *   Shizuku runs commands with the `shell` (uid 2000) identity, which is a
 *   member of the `adb` group and therefore holds
 *   `android.permission.INSTALL_PACKAGES`. Invoking `pm install -r <apk>`
 *   through Shizuku's process bridge performs the install WITHOUT showing the
 *   normal confirmation dialog — exactly the "silent update" behaviour asked
 *   for. This reuses the same Shizuku newProcess bridge already used by
 *   LocalCommandRunner for privileged (SUDO) commands, so it is a proven path.
 *
 * Requires:
 *   - Shizuku running and permission granted (Shizuku.checkSelfPermission()).
 *   - The app holds `REQUEST_INSTALL_PACKAGES` (declared in the manifest).
 *
 * Returns an [UpdateResult] describing success or the failure reason.
 */
class SilentUpdater(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val runtimeManager: OrbitAiRuntimeManager
) {
    sealed class UpdateResult {
        data object Success : UpdateResult()
        data class Failure(val reason: String) : UpdateResult()
        /** Shizuku unavailable — caller should open the system installer for this URI. */
        data class NeedsManualInstall(val apkUri: android.net.Uri) : UpdateResult()
    }

    /**
     * Returns true if a silent (Shizuku) install is possible right now.
     */
    fun canSilentInstall(): Boolean {
        return Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Attempt a silent install via Shizuku (`cmd package install -r`).
     * Uses `cmd package install` (not the deprecated `pm install`) so it
     * works across Android 7–15. Returns a descriptive result.
     */
    suspend fun installApk(apkFile: File): UpdateResult = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            return@withContext UpdateResult.Failure("Shizuku is not running.")
        }
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return@withContext UpdateResult.Failure("Shizuku permission not granted.")
        }
        if (!apkFile.exists()) {
            return@withContext UpdateResult.Failure("APK not found: ${apkFile.absolutePath}")
        }

        try {
            // Make the APK world-readable so the shell process (a different
            // UID) can open it.
            apkFile.setReadable(true, false)

            // `cmd package install -r` is the modern, ROM-agnostic path.
            // -r = keep data, -i = installer package id.
            val command = "cmd package install -r -i ${context.packageName} \"${apkFile.absolutePath}\""
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? Process ?: return@withContext UpdateResult.Failure("Shizuku API mismatch.")

            val output = buildString {
                BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                    generateSequence { r.readLine() }.forEach { appendLine(it) }
                }
                BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                    generateSequence { r.readLine() }.forEach { appendLine(it) }
                }
            }
            process.waitFor()
            val exit = process.exitValue()
            FileLogger.i(TAG, "pm install result", "exit=$exit output=${output.take(500)}")

            val lower = output.lowercase()
            if (exit == 0 && ("success" in lower || lower.isBlank())) {
                UpdateResult.Success
            } else if ("failure" in lower || "error" in lower || exit != 0) {
                UpdateResult.Failure(output.trim().ifBlank { "Install failed (exit $exit)" })
            } else {
                UpdateResult.Success
            }
        } catch (e: NoSuchMethodException) {
            FileLogger.e(TAG, "Shizuku API error", e, "reason=${e.message}")
            UpdateResult.Failure("Shizuku API changed — newProcess not found. Please update the app.")
        } catch (e: Exception) {
            FileLogger.e(TAG, "Silent install exception", e, "reason=${e.message}")
            UpdateResult.Failure("Exception: ${e.message}")
        }
    }
}
