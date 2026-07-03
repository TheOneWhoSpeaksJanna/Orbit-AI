package com.omniclaw.data.local.runtime

import android.content.Context
import com.omniclaw.core.logging.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "PRootRuntime"

/**
 * PRoot-based Linux runtime manager.
 *
 * This is the NEW architecture for running AI agents on Android. It replaces
 * the old BusyBox-based approach which was unreliable due to:
 *  1. SELinux W^X enforcement blocking exec of files in filesDir
 *  2. Shared library dependencies (Termux debs need libc++, openssl, etc.)
 *  3. Stale package URLs causing 404 errors
 *
 * HOW IT WORKS:
 *  1. Alpine Linux minirootfs (3.9MB compressed) is bundled in APK assets
 *  2. PRoot static binary (402KB) is bundled as libproot.so in jniLibs
 *  3. PRoot loader (18KB) is bundled as libproot_loader.so in jniLibs
 *  4. On first launch, the rootfs is extracted to filesDir/alpine-rootfs/
 *  5. Node.js, npm, and git are installed via apk (Alpine's package manager)
 *     inside the proot environment on first launch
 *  6. Agents run inside the proot environment, where they have a full Linux
 *     filesystem with proper /usr/bin/node, /usr/bin/git, etc.
 *
 * WHY PRoot WORKS WHEN BusyBox DIDN'T:
 *  - PRoot is a real binary, so it CAN be exec'd from nativeLibraryDir
 *    (which has the app_lib_data_file SELinux label that allows exec)
 *  - PRoot uses ptrace (unprivileged) to intercept syscalls and translate
 *    paths — no root, no chroot, no mount needed
 *  - Inside the proot environment, agents see a standard Linux filesystem
 *    with /usr/bin/node, /usr/bin/git, /usr/lib, etc. — no wrapper scripts
 *    needed, no PATH hacks, no LD_LIBRARY_PATH issues
 *  - The proot environment has its own /tmp, /etc, /var — completely
 *    isolated from Android's filesystem quirks
 *
 * AGENT EXECUTION FLOW:
 *  1. User sends a message
 *  2. ChatViewModel calls prootRuntime.executeInRootfs("node /agents/openclaude/cli.js", stdin)
 *  3. PRootRuntime builds the proot command line:
 *     libproot.so --rootfs=.../alpine-rootfs --cwd=/root
 *       --bind=.../agents:/agents --bind=.../workspace:/workspace
 *       -- /usr/bin/node /agents/openclaude/cli.js
 *  4. The command runs inside the Alpine environment with full Linux compat
 *  5. Output is captured and returned to the user
 */
class PRootRuntime(private val context: Context) {

    val runtimeDir = File(context.filesDir, "orbit_runtime")
    val rootfsDir = File(runtimeDir, "alpine-rootfs")
    val agentsDir = File(runtimeDir, "agents")
    val workspaceDir = File(runtimeDir, "workspace")
    val tmpDir = File(runtimeDir, "tmp")

    private val nativeLibDir: String by lazy {
        context.applicationInfo.nativeLibraryDir
            ?: throw IllegalStateException("nativeLibraryDir not available")
    }

    val prootBinary: String by lazy { "$nativeLibDir/libproot.so" }
    val prootLoader: String by lazy { "$nativeLibDir/libproot_loader.so" }

    val isRootfsInstalled: Boolean get() = File(rootfsDir, "bin/sh").exists()

    /**
     * Create versioned symlinks for shared libraries that proot needs.
     *
     * The Termux proot binary has NEEDED: libtalloc.so.2 in its ELF headers.
     * We bundle the file as libtalloc.so (because Android jniLibs only
     * accepts *.so filenames). At runtime, we create a symlink
     * libtalloc.so.2 -> libtalloc.so so the linker finds it.
     *
     * This is called before every proot execution. It's idempotent — if
     * the symlink already exists, it does nothing.
     */
    private fun ensureVersionedSymlinks() {
        try {
            val talloc = File(nativeLibDir, "libtalloc.so")
            val talloc2 = File(nativeLibDir, "libtalloc.so.2")
            if (talloc.exists() && !talloc2.exists()) {
                // Try symlink first (works on most devices)
                try {
                    Runtime.getRuntime().exec(arrayOf(
                        "ln", "-s", "libtalloc.so", talloc2.absolutePath
                    )).waitFor()
                } catch (_: Exception) { }
                // If symlink failed (read-only dir), try copy
                if (!talloc2.exists()) {
                    try { talloc.copyTo(talloc2, overwrite = false) } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
    }

    init {
        runtimeDir.mkdirs()
        agentsDir.mkdirs()
        workspaceDir.mkdirs()
        tmpDir.mkdirs()
    }

    /**
     * Extract the bundled Alpine rootfs from APK assets to filesDir.
     * Safe to call repeatedly — skips if already extracted.
     *
     * @param onProgress called with (0.0 to 1.0, statusMessage) during extraction
     * @return true if rootfs is ready after this call
     */
    suspend fun installRootfs(
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        if (isRootfsInstalled) {
            FileLogger.i(TAG, "Rootfs already installed")
            return@withContext true
        }

        FileLogger.i(TAG, "Rootfs install start")
        val startTime = System.currentTimeMillis()

        try {
            onProgress(0.1f, "Extracting Alpine Linux rootfs...")

            rootfsDir.mkdirs()

            // Try both .tar.gz and .tar — AAPT2 may have decompressed the .tar.gz
            val assetStream = try {
                FileLogger.d(TAG, "Asset lookup", "trying=alpine-rootfs.tar.gz")
                context.assets.open("alpine-rootfs.tar.gz")
            } catch (e: Exception) {
                FileLogger.w(TAG, "Asset fallback", "alpine-rootfs.tar.gz not found, trying alpine-rootfs.tar")
                try {
                    context.assets.open("alpine-rootfs.tar")
                } catch (e2: Exception) {
                    val assets = context.assets.list("") ?: arrayOf()
                    FileLogger.e(TAG, "Rootfs asset not found", "tried=tar.gz,tar available=${assets.joinToString(",")}")
                    throw e2
                }
            }

            val tempArchive = File(tmpDir, "alpine-rootfs.tar")
            tempArchive.parentFile?.mkdirs()

            // Copy asset to temp file
            val copyStart = System.currentTimeMillis()
            assetStream.use { input ->
                FileOutputStream(tempArchive).use { output ->
                    input.copyTo(output)
                }
            }
            FileLogger.d(TAG, "Asset copied", "bytes=${tempArchive.length()} time=${System.currentTimeMillis() - copyStart}ms")

            onProgress(0.3f, "Extracting rootfs archive...")
            val extractStart = System.currentTimeMillis()

            // Extract: try gzip first (if the file is still .tar.gz),
            // then fall back to plain tar (if AAPT2 decompressed it).
            val extractRootfs: (java.io.InputStream) -> Unit = { input ->
                org.apache.commons.compress.archivers.tar.TarArchiveInputStream(input).use { tis ->
                    var entry = tis.nextEntry
                    while (entry != null) {
                        val outFile = File(rootfsDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                tis.copyTo(fos)
                            }
                            // Preserve executable bits
                            if (entry.mode and 0b001000000 != 0) {
                                outFile.setExecutable(true, false)
                            }
                        }
                        entry = tis.nextEntry
                    }
                }
            }

            try {
                java.util.zip.GZIPInputStream(tempArchive.inputStream()).use { gzip ->
                    extractRootfs(gzip)
                }
                FileLogger.d(TAG, "Rootfs extracted", "format=gzip time=${System.currentTimeMillis() - extractStart}ms")
            } catch (gzipEx: Exception) {
                FileLogger.w(TAG, "Gzip extraction failed, trying plain tar", "reason=${gzipEx.message}")
                tempArchive.inputStream().use { plain ->
                    extractRootfs(plain)
                }
                FileLogger.d(TAG, "Rootfs extracted", "format=tar time=${System.currentTimeMillis() - extractStart}ms")
            }

            tempArchive.delete()
            onProgress(0.7f, "Rootfs extracted, configuring...")

            // Create necessary directories
            File(rootfsDir, "root").mkdirs()
            File(rootfsDir, "tmp").mkdirs()
            File(rootfsDir, "etc").mkdirs()

            // Set up DNS resolution
            File(rootfsDir, "etc/resolv.conf").writeText("nameserver 8.8.8.8\nnameserver 8.8.4.4\n")

            // Set up Alpine repositories for apk
            File(rootfsDir, "etc/apk/repositories").writeText(
                "https://dl-cdn.alpinelinux.org/alpine/v3.20/main\n" +
                "https://dl-cdn.alpinelinux.org/alpine/v3.20/community\n"
            )

            // Create /agents and /workspace mount points (will be bind-mounted)
            File(rootfsDir, "agents").mkdirs()
            File(rootfsDir, "workspace").mkdirs()

            onProgress(0.8f, "Installing nodejs, npm, git, gh, python3, pip, curl, wget, ssh, make, gcc...")
            FileLogger.i(TAG, "apk install start", "packages=nodejs,npm,git,gh,python3,py3-pip,curl,wget,openssh-client,make,gcc,g++")
            val apkStart = System.currentTimeMillis()

            val installResult = executeInRootfs(
                "apk update && apk add --no-cache nodejs npm git gh python3 py3-pip curl wget openssh-client make gcc g++",
                "",
                workingDir = "/root"
            )
            val apkDuration = System.currentTimeMillis() - apkStart

            if (installResult.exitCode != 0) {
                FileLogger.e(TAG, "apk install failed", "exit=${installResult.exitCode} time=${apkDuration}ms output=${installResult.output.take(300)}")
                onProgress(0.0f, "Failed to install packages: ${installResult.output.take(200)}")
                FileLogger.e(TAG, "SUMMARY: rootfs install failed because apk install exited ${installResult.exitCode}")
                return@withContext false
            }

            val totalDuration = System.currentTimeMillis() - startTime
            FileLogger.i(TAG, "Rootfs install success", "time=${totalDuration}ms packages=${apkDuration}ms")
            onProgress(1.0f, "Rootfs ready")
            true
        } catch (e: Exception) {
            val totalDuration = System.currentTimeMillis() - startTime
            FileLogger.e(TAG, "Rootfs install failed", e, "time=${totalDuration}ms reason=${e.message}")
            FileLogger.e(TAG, "SUMMARY: rootfs install failed because ${e.message}")
            onProgress(0.0f, "Installation failed: ${e.message}")
            false
        }
    }

    /**
     * Execute a command inside the PRoot Alpine environment.
     *
     * @param command The shell command to run (e.g. "node /agents/openclaude/cli.js")
     * @param stdin Text to pipe to the command's stdin
     * @param workingDir Working directory inside the rootfs (default: /root)
     * @return CommandResult with output, exit code, and the original command
     */
    suspend fun executeInRootfs(
        command: String,
        stdin: String = "",
        workingDir: String = "/root"
    ): CommandResult = withContext(Dispatchers.IO) {
        if (!isRootfsInstalled) {
            return@withContext CommandResult(
                "Rootfs not installed. Call installRootfs() first.",
                -1,
                command
            )
        }

        // Ensure libtalloc.so.2 symlink exists (proot needs it)
        ensureVersionedSymlinks()

        val prootCmd = buildProotCommand(command, workingDir)
        val execStart = System.currentTimeMillis()
        FileLogger.d(TAG, "PRoot exec start", "cmd=${command.take(200)}")

        try {
            val pb = ProcessBuilder(prootCmd)
            pb.directory(runtimeDir)
            pb.redirectErrorStream(false)

            // Set up environment for PRoot
            val env = pb.environment()
            env["PROOT_LOADER"] = prootLoader
            env["PROOT_NO_SECCOMP"] = "1"
            env["PROOT_TMP_DIR"] = tmpDir.absolutePath
            env["HOME"] = "/root"
            env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            env["TERM"] = "xterm-256color"
            env["LANG"] = "C.UTF-8"
            // CRITICAL: The Termux proot binary is dynamically linked and needs
            // libtalloc.so and libandroid-shmem.so. These are bundled as native
            // libraries in the same directory as libproot.so. Setting
            // LD_LIBRARY_PATH ensures the Android linker finds them.
            env["LD_LIBRARY_PATH"] = nativeLibDir
            // link2symlink backing store directory
            env["PROOT_L2S_DIR"] = File(rootfsDir, ".l2s").absolutePath
            // Remove LD_PRELOAD — it conflicts with ptrace
            env.remove("LD_PRELOAD")

            val process = pb.start()

            // Write stdin
            if (stdin.isNotEmpty()) {
                process.outputStream.write(stdin.toByteArray())
                process.outputStream.flush()
            }
            process.outputStream.close()

            // Read stdout and stderr in parallel
            val stdoutText = StringBuilder()
            val stderrText = StringBuilder()

            val stdoutThread = Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { stdoutText.appendLine(it) }
                }
            }
            val stderrThread = Thread {
                process.errorStream.bufferedReader().use { reader ->
                    reader.forEachLine { stderrText.appendLine(it) }
                }
            }
            stdoutThread.start()
            stderrThread.start()

            process.waitFor()
            stdoutThread.join()
            stderrThread.join()

            val exitCode = process.exitValue()
            val output = stdoutText.toString().trim()
            val stderr = stderrText.toString().trim()
            val execDuration = System.currentTimeMillis() - execStart

            if (exitCode != 0) {
                FileLogger.w(TAG, "PRoot exec failed", "exit=$exitCode time=${execDuration}ms stderr=${stderr.take(300)}")
            } else {
                FileLogger.d(TAG, "PRoot exec success", "exit=0 time=${execDuration}ms output=${output.length}chars")
            }

            // Return stdout + stderr combined (like a terminal would show)
            val combinedOutput = if (stderr.isNotBlank()) {
                "$output\n--- stderr ---\n$stderr"
            } else {
                output
            }

            CommandResult(combinedOutput.trim(), exitCode, command)
        } catch (e: Exception) {
            FileLogger.e(TAG, "PRoot exec exception", e, "cmd=${command.take(100)} reason=${e.message}")
            CommandResult("Error: ${e.message}", -1, command)
        }
    }

    /**
     * Build the PRoot command line for executing a command inside the rootfs.
     */
    private fun buildProotCommand(command: String, workingDir: String): List<String> {
        return listOf(
            prootBinary,
            "--kill-on-exit",
            "--link2symlink",
            "--rootfs=${rootfsDir.absolutePath}",
            "--cwd=$workingDir",
            "--change-id=0:0",
            // Bind standard filesystems
            "--bind=/dev",
            "--bind=/proc",
            "--bind=/sys",
            // Bind Android storage so agents can access user files
            "--bind=/sdcard:/sdcard",
            // Bind our agent code and workspace directories
            "--bind=${agentsDir.absolutePath}:/agents",
            "--bind=${workspaceDir.absolutePath}:/workspace",
            "--bind=${tmpDir.absolutePath}:/tmp",
            // Run via sh -c so we get full shell syntax (pipes, redirects, etc.)
            "/bin/sh", "-c", command
        )
    }

    /**
     * Check if a specific tool is installed in the rootfs.
     */
    fun isToolInstalled(tool: String): Boolean {
        return File(rootfsDir, "usr/bin/$tool").exists()
    }

    /**
     * Get the version of a tool (e.g. "node --version").
     */
    suspend fun getToolVersion(tool: String, args: String = "--version"): String? {
        val result = executeInRootfs("$tool $args")
        return if (result.exitCode == 0) result.output.trim() else null
    }
}

/**
 * Result of a command execution.
 */
data class CommandResult(
    val output: String,
    val exitCode: Int,
    val command: String
)
