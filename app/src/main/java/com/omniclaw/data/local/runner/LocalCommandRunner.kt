package com.omniclaw.data.local.runner

import com.omniclaw.core.logging.FileLogger
import com.omniclaw.data.local.runtime.OmniClawRuntimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "LocalCommandRunner"
private const val ERROR_EXIT_CODE = -1

private const val SHIZUKU_NOT_RUNNING = "Shizuku is not running or unavailable."
private const val SHIZUKU_API_CHANGED = "Shizuku API changed \u2014 newProcess method not found. Please update the app."
private const val SHIZUKU_PERMISSION_DENIED = "Shizuku permission denied. Grant permission in the Shizuku app."
private const val ERROR_COMMAND_PREFIX = "Error executing local command: "
private const val ERROR_PRIVILEGED_PREFIX = "Error executing privileged command via Shizuku: "

data class CommandResult(val output: String, val exitCode: Int, val command: String)

class LocalCommandRunner(
    private val runtimeManager: OmniClawRuntimeManager
) {

    /**
     * Returns the shell command array to use.
     *
     * CRITICAL Android 10+ CONTEXT:
     * On Android 10+ (API 29+), the kernel enforces W^X (writable XOR executable)
     * on app-private storage. This means:
     *  - Real BINARIES (like busybox, node) CAN be exec'd from /data/data/<pkg>/files/
     *  - Shell SCRIPTS CANNOT be exec'd — the kernel blocks execve() on script files
     *    in app_data_file SELinux context, even if they have +x permission
     *
     * So we always use `busybox sh -c <cmd>` (or system `sh -c <cmd>` as fallback).
     * This works because:
     *  - busybox is a real binary → can be exec'd
     *  - `sh -c` reads the command string, doesn't try to exec a script file
     *  - When the command does `exec node`, the shell looks up `node` in PATH;
     *    if PATH includes packages/nodejs/usr/bin/, it finds the real node BINARY
     *    (not a wrapper script) and exec's it successfully
     */
    private fun shellCommand(command: String): List<String> {
        val busyboxPath = runtimeManager.busyBoxPath()
        if (busyboxPath != null) {
            return listOf(busyboxPath, "sh", "-c", command)
        }
        return listOf("sh", "-c", command)
    }

    /**
     * Build the ProcessBuilder with the correct environment.
     *
     * PATH includes:
     *  - orbit_runtime/bin/ (busybox + wrapper scripts for `sh <wrapper>` invocation)
     *  - packages/[star]/usr/bin/ (actual binaries: node, npm, git, python3, etc.)
     *  - system PATH
     *
     * LD_LIBRARY_PATH includes:
     *  - packages/[star]/usr/lib/ (shared libs for node, git, python, etc.)
     *
     * Without LD_LIBRARY_PATH, node/git/python fail with "linker: ... not found"
     * because they can't find libnode.so, libpcre2-8.so, etc.
     */
    private fun setupProcessBuilder(command: String): ProcessBuilder {
        val processBuilder = ProcessBuilder(shellCommand(command))
        processBuilder.directory(runtimeManager.runtimeDir)
        val env = processBuilder.environment()

        // Build a comprehensive PATH that includes all package bin directories
        // so that `exec node`, `git --version`, etc. find the REAL binaries
        // (not wrapper scripts that can't be exec'd on Android 10+).
        val fullPath = runtimeManager.buildPath()
        env["PATH"] = fullPath

        // LD_LIBRARY_PATH is critical for shared-lib-dependent binaries.
        val ldPath = runtimeManager.buildLdLibraryPath()
        if (ldPath.isNotBlank()) {
            val existingLd = env["LD_LIBRARY_PATH"] ?: ""
            env["LD_LIBRARY_PATH"] = if (existingLd.isNotBlank()) "$ldPath:$existingLd" else ldPath
        }

        // HOME and TMPDIR help node/npm/git behave correctly
        env["HOME"] = runtimeManager.runtimeDir.absolutePath
        env["TMPDIR"] = runtimeManager.tmpDir.absolutePath

        FileLogger.d(TAG, "ProcessBuilder env: PATH=${fullPath.take(200)}..., LD_LIBRARY_PATH=${ldPath.take(200)}")
        return processBuilder
    }

    suspend fun executeCommandStreamed(command: String, onOutput: (String) -> Unit): CommandResult =
        withContext(Dispatchers.IO) {
            FileLogger.i(TAG, "EXEC (streamed): $command")
            try {
                val process = setupProcessBuilder(command).start()
                val outputBuilder = StringBuilder()

                val stdInThread = Thread {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        generateSequence { reader.readLine() }.forEach { line ->
                            outputBuilder.appendLine(line)
                            onOutput(line)
                        }
                    }
                }

                val stdErrThread = Thread {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        generateSequence { reader.readLine() }.forEach { line ->
                            outputBuilder.appendLine(line)
                            onOutput(line)
                        }
                    }
                }

                stdInThread.start()
                stdErrThread.start()

                process.waitFor()
                stdInThread.join()
                stdErrThread.join()

                val exit = process.exitValue()
                if (exit != 0) {
                    FileLogger.w(TAG, "Command exited with $exit: $command")
                    FileLogger.w(TAG, "Output: ${outputBuilder.toString().take(500)}")
                }
                CommandResult(outputBuilder.toString().trim(), exit, command)
            } catch (e: Exception) {
                FileLogger.e(TAG, "Exception executing '$command': ${e.message}", e)
                val errorMsg = "$ERROR_COMMAND_PREFIX${e.message}"
                onOutput(errorMsg)
                CommandResult(errorMsg, ERROR_EXIT_CODE, command)
            }
        }

    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        FileLogger.i(TAG, "EXEC: $command")
        try {
            val process = setupProcessBuilder(command).start()
            val output = buildString {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    generateSequence { reader.readLine() }.forEach { line ->
                        appendLine(line)
                    }
                }
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    generateSequence { reader.readLine() }.forEach { line ->
                        appendLine(line)
                    }
                }
            }
            process.waitFor()
            val exit = process.exitValue()
            if (exit != 0) {
                FileLogger.w(TAG, "Command exited with $exit: $command")
                FileLogger.w(TAG, "Output: ${output.take(500)}")
            } else {
                FileLogger.d(TAG, "Command succeeded (exit=0): $command")
            }
            CommandResult(output.trim(), exit, command)
        } catch (e: Exception) {
            FileLogger.e(TAG, "Exception executing '$command': ${e.message}", e)
            CommandResult("$ERROR_COMMAND_PREFIX${e.message}", ERROR_EXIT_CODE, command)
        }
    }

    suspend fun executePrivilegedCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        FileLogger.i(TAG, "EXEC (privileged/Shizuku): $command")
        if (!Shizuku.pingBinder()) {
            FileLogger.w(TAG, "Shizuku not running — privileged command rejected")
            return@withContext CommandResult(SHIZUKU_NOT_RUNNING, ERROR_EXIT_CODE, command)
        }
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null, shellCommand(command).toTypedArray(), null, null
            ) as Process

            val output = buildString {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    generateSequence { reader.readLine() }.forEach { line ->
                        appendLine(line)
                    }
                }
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    generateSequence { reader.readLine() }.forEach { line ->
                        appendLine(line)
                    }
                }
            }
            process.waitFor()
            val exit = process.exitValue()
            FileLogger.i(TAG, "Privileged command exit=$exit: $command")
            CommandResult(output.trim(), exit, command)
        } catch (e: NoSuchMethodException) {
            FileLogger.e(TAG, "Shizuku API changed: ${e.message}", e)
            CommandResult(SHIZUKU_API_CHANGED, ERROR_EXIT_CODE, command)
        } catch (e: SecurityException) {
            FileLogger.e(TAG, "Shizuku permission denied: ${e.message}", e)
            CommandResult(SHIZUKU_PERMISSION_DENIED, ERROR_EXIT_CODE, command)
        } catch (e: Exception) {
            FileLogger.e(TAG, "Privileged command exception: ${e.message}", e)
            CommandResult("$ERROR_PRIVILEGED_PREFIX${e.message}", ERROR_EXIT_CODE, command)
        }
    }
}
