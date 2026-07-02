package com.omniclaw.data.local.runtime

import android.content.Context
import android.util.Log
import com.omniclaw.core.logging.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "OmniClawRuntime"
private const val BUSYBOX_ASSET = "busybox-arm64"
private const val BUSYBOX_BINARY = "busybox"

/**
 * Resolve the absolute path to a system tool (sh, chmod, cp, mv) under
 * `/system/bin`. We honor `ANDROID_ROOT` so custom ROMs that mount the system
 * tree elsewhere (e.g. emulators) work transparently.
 */
private fun systemBin(tool: String): String {
    val root = android.system.Os.getenv("ANDROID_ROOT") ?: "/system"
    return "$root/bin/$tool"
}

class OmniClawRuntimeManager(val context: Context) {
    val runtimeDir = File(context.filesDir, "orbit_runtime")
    val binDir = File(runtimeDir, "bin")
    val tmpDir = File(runtimeDir, "tmp")
    val packagesDir = File(runtimeDir, "packages")
    val downloadsDir = File(runtimeDir, "downloads")
    val agentsDir = File(runtimeDir, "agents")
    val logsDir = File(runtimeDir, "logs")
    val environmentsDir = File(runtimeDir, "environments")

    // Caches whether busybox is actually executable.
    private var busyboxVerified: Boolean? = null

    init {
        listOf(runtimeDir, binDir, tmpDir, packagesDir, downloadsDir, agentsDir, logsDir, environmentsDir).forEach {
            it.mkdirs()
        }
    }

    fun getEnvVars(): Array<String> {
        val existingPath = System.getenv("PATH") ?: ""
        return arrayOf("PATH=${binDir.absolutePath}:$existingPath")
    }

    /** Full path to the BusyBox binary, or null if not installed or not executable. */
    fun busyBoxPath(): String? {
        val f = File(binDir, BUSYBOX_BINARY)
        if (!f.exists()) return null

        if (busyboxVerified != null) {
            return if (busyboxVerified!!) f.absolutePath else null
        }

        if (!f.canExecute()) {
            busyboxVerified = false
            return null
        }

        return try {
            val p = Runtime.getRuntime().exec(arrayOf(f.absolutePath, "true"))
            p.waitFor()
            busyboxVerified = true
            FileLogger.i(TAG, "BusyBox verified at ${f.absolutePath}")
            f.absolutePath
        } catch (e: Exception) {
            FileLogger.w(TAG, "BusyBox at ${f.absolutePath} exists but cannot be executed: ${e.message}")
            busyboxVerified = false
            null
        }
    }

    /**
     * Find the actual node binary inside the installed nodejs package.
     * Returns the absolute path if found, null otherwise.
     *
     * The Termux nodejs deb installs the binary at:
     *   packages/nodejs/usr/bin/node
     */
    fun findNodeBinary(): String? {
        val candidates = listOf(
            File(packagesDir, "nodejs/usr/bin/node"),
            File(packagesDir, "nodejs/bin/node"),
            File(packagesDir, "node/bin/node")
        )
        for (c in candidates) {
            if (c.exists() && c.canExecute()) {
                FileLogger.d(TAG, "Found node binary at ${c.absolutePath}")
                return c.absolutePath
            }
        }
        FileLogger.w(TAG, "Node binary not found in any expected location under ${packagesDir.absolutePath}")
        return null
    }

    /**
     * Find the node binary's lib directory (for LD_LIBRARY_PATH).
     * The Termux nodejs deb puts shared libs at:
     *   packages/nodejs/usr/lib
     */
    fun findNodeLibDir(): String? {
        val candidates = listOf(
            File(packagesDir, "nodejs/usr/lib"),
            File(packagesDir, "nodejs/lib"),
            File(packagesDir, "node/lib")
        )
        for (c in candidates) {
            if (c.isDirectory && c.canRead()) {
                return c.absolutePath
            }
        }
        return null
    }

    /**
     * Build a colon-separated LD_LIBRARY_PATH that includes all
     * packages/[star]/usr/lib directories. This lets shared-lib-dependent
     * binaries (node, git, python) find their libs at runtime.
     */
    fun buildLdLibraryPath(): String {
        val libs = mutableListOf<String>()
        packagesDir.listFiles()?.forEach { pkgDir ->
            if (pkgDir.isDirectory) {
                val usrLib = File(pkgDir, "usr/lib")
                if (usrLib.isDirectory && usrLib.canRead()) {
                    libs.add(usrLib.absolutePath)
                }
                val lib = File(pkgDir, "lib")
                if (lib.isDirectory && lib.canRead()) {
                    libs.add(lib.absolutePath)
                }
            }
        }
        return libs.joinToString(":")
    }

    /**
     * Build a PATH string that includes:
     *  1. orbit_runtime/bin/ (for wrapper scripts and busybox)
     *  2. Every packages/[star]/usr/bin/ (for actual binaries like node, git, python)
     *  3. The system PATH
     */
    fun buildPath(): String {
        val paths = mutableListOf(binDir.absolutePath)
        packagesDir.listFiles()?.forEach { pkgDir ->
            if (pkgDir.isDirectory) {
                val usrBin = File(pkgDir, "usr/bin")
                if (usrBin.isDirectory && usrBin.canRead()) {
                    paths.add(usrBin.absolutePath)
                }
                val bin = File(pkgDir, "bin")
                if (bin.isDirectory && bin.canRead()) {
                    paths.add(bin.absolutePath)
                }
            }
        }
        val systemPath = System.getenv("PATH") ?: ""
        paths.add(systemPath)
        return paths.joinToString(":")
    }

    /**
     * Install BusyBox from bundled APK assets. BusyBox provides ~300 POSIX tools
     * in a single ~1MB binary, making Orbit-AI self-contained without Termux.
     *
     * CRITICAL: BusyBox is a real BINARY (not a script), so it CAN be exec'd
     * from app-private storage on Android 10+. This is why we use `busybox sh -c`
     * instead of creating wrapper scripts (scripts CANNOT be exec'd).
     *
     * This is safe to call repeatedly — it skips if already installed.
     *
     * @return true if BusyBox is available after the call
     */
    suspend fun installBusyBox(): Boolean = withContext(Dispatchers.IO) {
        val busyboxFile = File(binDir, BUSYBOX_BINARY)

        if (busyboxVerified == true && busyboxFile.exists()) {
            return@withContext true
        }

        busyboxVerified = null

        try {
            FileLogger.i(TAG, "Extracting BusyBox from APK assets...")
            context.assets.open(BUSYBOX_ASSET).use { input ->
                busyboxFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val chmodResult = Runtime.getRuntime()
                .exec(arrayOf(systemBin("chmod"), "755", busyboxFile.absolutePath))
            val chmodExit = chmodResult.waitFor()
            FileLogger.d(TAG, "chmod 755 ${busyboxFile.absolutePath} → exit $chmodExit")

            if (!busyboxFile.canExecute()) {
                FileLogger.w(TAG, "canExecute()=false after chmod, trying cp+mv workaround...")
                Runtime.getRuntime().exec(arrayOf(
                    systemBin("sh"), "-c",
                    "${systemBin("cp")} ${busyboxFile.absolutePath} ${busyboxFile.absolutePath}.tmp && " +
                    "${systemBin("mv")} ${busyboxFile.absolutePath}.tmp ${busyboxFile.absolutePath} && " +
                    "${systemBin("chmod")} 755 ${busyboxFile.absolutePath}"
                )).waitFor()
            }

            val ok = busyboxFile.canExecute()
            if (ok) {
                FileLogger.i(TAG, "BusyBox installed and executable at ${busyboxFile.absolutePath}")
            } else {
                FileLogger.e(TAG, "BusyBox extracted but NOT executable — W^X enforcement may be blocking it. " +
                    "File: ${busyboxFile.absolutePath}, canExecute=${busyboxFile.canExecute()}, canRead=${busyboxFile.canRead()}, canWrite=${busyboxFile.canWrite()}")
            }
            ok
        } catch (e: Exception) {
            FileLogger.e(TAG, "BusyBox install failed: ${e.message}", e)
            false
        }
    }
}
