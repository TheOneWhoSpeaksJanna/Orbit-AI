package com.omniclaw.data.local.runtime

import android.content.Context
import com.omniclaw.core.logging.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

private const val TAG = "PackageInstaller"
private const val FAIL_PREFIX = "FAIL: "
private const val PROGRESS_START = 0f
private const val PROGRESS_COMPLETE = 1f
private const val BUFFER_SIZE = 4096
private const val DEFAULT_REGISTRY_ASSET = "packages.default.json"

class PackageInstaller(
    private val runtimeManager: OmniClawRuntimeManager,
    private val httpClient: OkHttpClient
) {
    /** Resolve a tool path — prefer the runtime BusyBox, fall back to system binary. */
    private fun resolveTool(tool: String): String {
        val busybox = runtimeManager.busyBoxPath()
        if (busybox != null) return busybox
        return tool
    }

    /** Build PATH that includes the runtime bin dir. */
    private fun envWithPath(): Map<String, String> {
        val currentPath = System.getenv("PATH") ?: ""
        return mapOf("PATH" to "${runtimeManager.binDir.absolutePath}:$currentPath")
    }

    /**
     * On-disk registry file. After first launch, this file wins over the APK-bundled
     * [DEFAULT_REGISTRY_ASSET] — so users can edit the runtime copy to pin newer
     * package versions without needing an app update.
     */
    private val registryFile = File(runtimeManager.runtimeDir, "registry/packages.json")

    init {
        registryFile.parentFile?.mkdirs()
        if (!registryFile.exists()) {
            // Seed the registry from the APK-bundled JSON asset. If the asset
            // is missing (older build), fall back to a tiny embedded default so
            // the app still boots — but log the issue.
            val seeded = seedFromAsset(runtimeManager.context)
            if (!seeded) {
                registryFile.writeText(JSONArray().toString(2))
            }
        }
    }

    /**
     * Copy [DEFAULT_REGISTRY_ASSET] from APK assets to the runtime registry path.
     * Returns true on success, false if the asset is missing or unreadable.
     */
    private fun seedFromAsset(context: Context): Boolean {
        return try {
            val text = context.assets.open(DEFAULT_REGISTRY_ASSET).bufferedReader().use { it.readText() }
            // Parse + re-emit so we catch malformed JSON early (instead of at install time).
            val parsed = JSONObject(text)
            val packages = parsed.optJSONArray("packages") ?: JSONArray()
            registryFile.writeText(packages.toString(2))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun loadRegistry(): List<JSONObject> {
        val array = JSONArray(registryFile.readText())
        val list = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            list.add(array.getJSONObject(i))
        }
        return list
    }

    suspend fun installPackage(
        packageId: String,
        onProgress: (progress: Float, status: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        FileLogger.i(TAG, "=== installPackage('$packageId') START ===")
        val pkg = loadRegistry().find { it.getString("name") == packageId }
        if (pkg == null) {
            FileLogger.e(TAG, "Package '$packageId' not found in registry. Available: ${loadRegistry().map { it.optString("name") }}")
            onProgress(PROGRESS_START, "${FAIL_PREFIX}PACKAGE_NOT_IN_REGISTRY")
            return@withContext false
        }

        try {
            val url = pkg.getString("url")
            val type = pkg.getString("type")
            val installMethod = pkg.getString("install_method")
            val name = pkg.getString("name")

            FileLogger.i(TAG, "Installing $name: url=$url, type=$type, method=$installMethod")
            onProgress(PROGRESS_START, "Downloading $name...")

            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                FileLogger.e(TAG, "Download failed: HTTP ${response.code} for $url")
                onProgress(PROGRESS_START, "${FAIL_PREFIX}DOWNLOAD_${response.code}")
                return@withContext false
            }

            val body = response.body ?: run {
                FileLogger.e(TAG, "Download failed: empty response body for $url")
                onProgress(PROGRESS_START, "${FAIL_PREFIX}DOWNLOAD_EMPTY_BODY")
                return@withContext false
            }
            val downloadFile = File(runtimeManager.downloadsDir, "$name.$type")
            FileLogger.d(TAG, "Downloading to ${downloadFile.absolutePath}...")

            body.byteStream().use { input ->
                FileOutputStream(downloadFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileLogger.i(TAG, "Downloaded ${downloadFile.length()} bytes to ${downloadFile.absolutePath}")

            if (type == "binary") {
                FileLogger.d(TAG, "Checking ELF header for glibc incompatibility...")
                try {
                    val elfCmd = if (runtimeManager.busyBoxPath() != null) {
                        listOf(runtimeManager.busyBoxPath()!!, "strings", downloadFile.absolutePath)
                    } else {
                        listOf("strings", downloadFile.absolutePath)
                    }
                    val processElf = ProcessBuilder(elfCmd).start()
                    val elfOut = processElf.inputStream.bufferedReader().readText()
                    processElf.waitFor()
                    if (elfOut.contains("ld-linux")) {
                        FileLogger.e(TAG, "Binary is glibc-linked (ld-linux found) — incompatible with Android bionic")
                        onProgress(PROGRESS_START, "${FAIL_PREFIX}GLIBC_INCOMPATIBLE")
                        return@withContext false
                    }
                    FileLogger.d(TAG, "ELF check passed (no ld-linux found)")
                } catch (e: Exception) {
                    FileLogger.w(TAG, "ELF check skipped: ${e.message}")
                }
            }

            val installDir = File(runtimeManager.packagesDir, name)
            if (installDir.exists()) installDir.deleteRecursively()
            installDir.mkdirs()
            FileLogger.d(TAG, "Install dir: ${installDir.absolutePath}")

            when (installMethod) {
                "tar_extract" -> {
                    FileLogger.d(TAG, "Extracting tar.gz...")
                    val bb = runtimeManager.busyBoxPath()
                    val tarExit = if (bb != null) {
                        ProcessBuilder(bb, "tar", "-xzf", downloadFile.absolutePath, "-C", installDir.absolutePath)
                            .start().waitFor()
                    } else {
                        ProcessBuilder("tar", "-xzf", downloadFile.absolutePath, "-C", installDir.absolutePath)
                            .start().waitFor()
                    }
                    FileLogger.d(TAG, "tar extract exit=$tarExit")
                }
                "binary_copy" -> {
                    FileLogger.d(TAG, "Copying static binary...")
                    val target = File(installDir, name)
                    downloadFile.copyTo(target)
                    try {
                        Runtime.getRuntime().exec(arrayOf(SYSTEM_CHMOD, "755", target.absolutePath)).waitFor()
                    } catch (_: Exception) {
                        target.setExecutable(true)
                    }
                }
                "deb_extract" -> {
                    FileLogger.d(TAG, "Extracting deb (ar archive)...")
                    val raf = RandomAccessFile(downloadFile, "r")
                    val magic = ByteArray(8)
                    raf.readFully(magic)
                    if (String(magic) == "!<arch>\n") {
                        var dataTarFile: File? = null
                        while (raf.filePointer < raf.length()) {
                            val header = ByteArray(60)
                            raf.readFully(header)
                            val entryName = String(header, 0, 16).trim()
                            val size = String(header, 48, 10).trim().toLong()
                            if (entryName.startsWith("data.tar")) {
                                dataTarFile = File(runtimeManager.downloadsDir, entryName.trim('/'))
                                FileLogger.d(TAG, "Found $entryName (${size} bytes) in deb")
                                val out = FileOutputStream(dataTarFile)
                                val buffer = ByteArray(BUFFER_SIZE)
                                var remaining = size
                                while (remaining > 0) {
                                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                                    val read = raf.read(buffer, 0, toRead)
                                    out.write(buffer, 0, read)
                                    remaining -= read
                                }
                                out.close()
                                break
                            } else {
                                raf.seek(raf.filePointer + size + (size % 2))
                            }
                        }
                        raf.close()
                        if (dataTarFile != null) {
                            // data.tar may be .xz or .gz compressed. Try both.
                            val bb = runtimeManager.busyBoxPath()
                            // Try xz first (most Termux debs use xz)
                            var tarExit = -1
                            val tarCmds = listOfNotNull(
                                if (bb != null) listOf(bb, "tar", "-xJf", dataTarFile.absolutePath, "-C", installDir.absolutePath) else null,
                                if (bb != null) listOf(bb, "tar", "-xf", dataTarFile.absolutePath, "-C", installDir.absolutePath) else null,
                                listOf("tar", "-xJf", dataTarFile.absolutePath, "-C", installDir.absolutePath),
                                listOf("tar", "-xf", dataTarFile.absolutePath, "-C", installDir.absolutePath)
                            )
                            for (cmd in tarCmds) {
                                tarExit = ProcessBuilder(cmd).start().waitFor()
                                FileLogger.d(TAG, "tar ${cmd.drop(2)} exit=$tarExit")
                                if (tarExit == 0) break
                            }
                            if (tarExit != 0) {
                                FileLogger.e(TAG, "All tar extraction attempts failed for ${dataTarFile.absolutePath}")
                            }
                        } else {
                            FileLogger.e(TAG, "No data.tar found in deb archive")
                        }
                    } else {
                        FileLogger.e(TAG, "Not a valid deb archive (bad magic: ${String(magic)})")
                    }
                }
            }

            // List what we extracted for debugging
            FileLogger.d(TAG, "Install dir contents:")
            installDir.walkTopDown().take(30).forEach { f ->
                FileLogger.d(TAG, "  ${f.absolutePath.replace(installDir.absolutePath, ".")} ${if (f.isFile) "(${f.length()}B)" else ""}")
            }

            val actualBinary = if (installMethod == "binary_copy") File(installDir, name) else File(installDir, "usr/bin/$name")
            val backupBinary = File(installDir, "bin/$name")
            val binToUse = if (actualBinary.exists()) actualBinary else backupBinary

            binToUse.setExecutable(true)
            if (!binToUse.canExecute()) {
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", binToUse.absolutePath)).waitFor()
            }

            val libDir = File(installDir, "usr/lib")
            val ldLibraryPathEnv = if (libDir.exists()) "export LD_LIBRARY_PATH=\"${libDir.absolutePath}:\$LD_LIBRARY_PATH\"\n" else ""

            // ── Create wrapper script(s) for the installed binary ───────────
            // On Android 10+, scripts in app-private dirs can't be exec()'d
            // even with +x — but `sh <wrapper>` always works because sh just
            // reads the file. The agent wrapper scripts in SetupViewModel and
            // ChatViewModel already use `sh <wrapper>` to invoke them, so
            // creating the wrappers here is correct.
            //
            // IMPORTANT: for deb_extract packages (like nodejs and git), the
            // package name doesn't always match the binary name. The Termux
            // nodejs deb installs `usr/bin/node` (not `usr/bin/nodejs`), and
            // also `usr/bin/npm`, `usr/bin/npx`. The old code only created a
            // wrapper named after the package (`bin/nodejs`), which pointed
            // to a non-existent `usr/bin/nodejs` — that's why `node` was
            // "inaccessible or not found" when the openclaude wrapper tried
            // to `exec node`.
            //
            // Fix: scan `usr/bin/` and create a wrapper for every binary we
            // find there. This gives us `bin/node`, `bin/npm`, `bin/npx`,
            // `bin/git`, etc. — matching what users actually type.
            val wrapperDir = runtimeManager.binDir
            wrapperDir.mkdirs()
            val createdWrappers = mutableListOf<File>()

            when (installMethod) {
                "binary_copy" -> {
                    // Single static binary — just wrap it under the package name.
                    val wrapper = File(wrapperDir, name)
                    try {
                        wrapper.writeText("#!$SYSTEM_SH\n$ldLibraryPathEnv\nexec \"${binToUse.absolutePath}\" \"\$@\"\n")
                        Runtime.getRuntime().exec(arrayOf(SYSTEM_CHMOD, "755", wrapper.absolutePath)).waitFor()
                        createdWrappers += wrapper
                    } catch (_: Exception) { }
                }
                "deb_extract", "tar_extract" -> {
                    // Scan usr/bin/ for actual binaries and wrap each one.
                    // Also scan bin/ as a fallback (some tarballs use bin/).
                    val usrBinDir = File(installDir, "usr/bin")
                    val altBinDir = File(installDir, "bin")
                    val binDirsToScan = listOfNotNull(
                        if (usrBinDir.isDirectory) usrBinDir else null,
                        if (altBinDir.isDirectory) altBinDir else null
                    )
                    for (binDir in binDirsToScan) {
                        val files = binDir.listFiles { f -> f.isFile && f.canRead() } ?: continue
                        for (binFile in files) {
                            // Skip obvious non-executable files
                            if (binFile.name.endsWith(".pyc") || binFile.name.endsWith(".conf")) continue
                            val wrapper = File(wrapperDir, binFile.name)
                            try {
                                wrapper.writeText(
                                    "#!$SYSTEM_SH\n$ldLibraryPathEnv\nexec \"${binFile.absolutePath}\" \"\$@\"\n"
                                )
                                Runtime.getRuntime().exec(arrayOf(SYSTEM_CHMOD, "755", wrapper.absolutePath)).waitFor()
                                createdWrappers += wrapper
                            } catch (_: Exception) { }
                        }
                    }
                    // Always also create a wrapper under the package name as a
                    // fallback, pointing to whatever binary we determined above.
                    if (createdWrappers.none { it.name == name } && binToUse.exists()) {
                        val wrapper = File(wrapperDir, name)
                        try {
                            wrapper.writeText("#!$SYSTEM_SH\n$ldLibraryPathEnv\nexec \"${binToUse.absolutePath}\" \"\$@\"\n")
                            Runtime.getRuntime().exec(arrayOf(SYSTEM_CHMOD, "755", wrapper.absolutePath)).waitFor()
                            createdWrappers += wrapper
                        } catch (_: Exception) { }
                    }
                }
            }

            // ── Validation: run test_command via the REAL BINARY ───────────
            // The test_command in the registry is e.g. "git --version" or
            // "node --version". We run the actual binary directly with
            // LD_LIBRARY_PATH set — NOT via a wrapper script (which can't be
            // exec'd on Android 10+).
            val testCmd = pkg.getString("test_command")  // e.g. "node --version"
            val rules = pkg.getJSONObject("validation_rules")
            val testBinaryName = testCmd.substringBefore(' ')
            val testArgs = testCmd.substringAfter(' ', "").trim()

            // Find the actual binary file to test. For "node --version" with
            // package "nodejs", the binary is at installDir/usr/bin/node.
            val testBinaryFile = listOf(
                File(installDir, "usr/bin/$testBinaryName"),
                File(installDir, "bin/$testBinaryName"),
                File(installDir, testBinaryName),
                binToUse  // fallback to whatever we found earlier
            ).firstOrNull { it.exists() && it.canExecute() }

            if (testBinaryFile == null) {
                val msg = "${FAIL_PREFIX}Binary '$testBinaryName' not found or not executable after install. " +
                    "Searched: ${installDir.absolutePath}/usr/bin/, ${installDir.absolutePath}/bin/"
                FileLogger.e(TAG, msg)
                onProgress(PROGRESS_COMPLETE, msg)
                return@withContext false
            }

            FileLogger.d(TAG, "Test: running '$testCmd' via binary at ${testBinaryFile.absolutePath}")
            // Build a test command that sets LD_LIBRARY_PATH then execs the binary.
            val libPath = runtimeManager.buildLdLibraryPath()
            val fullTest = "export LD_LIBRARY_PATH=\"$libPath:\$LD_LIBRARY_PATH\" && \"${testBinaryFile.absolutePath}\" $testArgs"

            val bb = runtimeManager.busyBoxPath()
            val shellBin = bb ?: SYSTEM_SH
            val testParts = listOf(shellBin, "-c", fullTest)
            val tProcess = ProcessBuilder(testParts)
                .apply { environment().putAll(envWithPath()) }
                .start()
            val tOut = tProcess.inputStream.bufferedReader().readText()
            val tErr = tProcess.errorStream.bufferedReader().readText()
            tProcess.waitFor()
            val tExit = tProcess.exitValue()

            FileLogger.i(TAG, "Test result: exit=$tExit, stdout='${tOut.take(200)}', stderr='${tErr.take(200)}'")

            if (tExit == rules.getInt("must_pass_exit_code") && tOut.contains(rules.getString("must_contain_output"))) {
                val wrapped = createdWrappers.joinToString(", ") { it.name }
                FileLogger.i(TAG, "=== installPackage('$packageId') SUCCESS === (wrappers: $wrapped)")
                onProgress(PROGRESS_COMPLETE, "PASS (wrappers: $wrapped)")
                return@withContext true
            } else {
                val reason = buildString {
                    append("${FAIL_PREFIX}Execution Test Failed")
                    append(" (exit=$tExit)")
                    if (tErr.isNotBlank()) {
                        append(": ")
                        append(tErr.trim().take(300))
                    } else if (tOut.isNotBlank()) {
                        append(": ")
                        append(tOut.trim().take(300))
                    }
                }
                FileLogger.e(TAG, "=== installPackage('$packageId') FAILED: $reason ===")
                onProgress(PROGRESS_COMPLETE, reason)
                return@withContext false
            }

        } catch (e: Exception) {
            FileLogger.e(TAG, "installPackage('$packageId') exception: ${e.message}", e)
            onProgress(PROGRESS_START, "${FAIL_PREFIX}Exception ${e.message}")
            return@withContext false
        }
    }

    companion object {
        /**
         * Resolve the system shell + chmod paths once. These live under `/system/bin`
         * on every shipping Android device, but resolving via [android.system.Os.getenv]
         * lets custom ROMs / emulator images that mount system tools elsewhere work
         * transparently.
         */
        private val SYSTEM_SH: String =
            android.system.Os.getenv("ANDROID_ROOT")?.let { "$it/bin/sh" } ?: "/system/bin/sh"
        private val SYSTEM_CHMOD: String =
            android.system.Os.getenv("ANDROID_ROOT")?.let { "$it/bin/chmod" } ?: "/system/bin/chmod"
    }
}
