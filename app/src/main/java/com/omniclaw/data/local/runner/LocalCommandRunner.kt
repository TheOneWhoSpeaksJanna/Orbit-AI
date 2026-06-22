package com.omniclaw.data.local.runner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(val output: String, val exitCode: Int, val command: String)

class LocalCommandRunner(    private val runtimeManager: com.omniclaw.data.local.runtime.OmniClawRuntimeManager) {

    suspend fun executeCommandStreamed(command: String, onOutput: (String) -> Unit): CommandResult =
        withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder("sh", "-c", command)
                processBuilder.directory(runtimeManager.runtimeDir)
                val env = processBuilder.environment()
                val currentPath = env["PATH"] ?: ""
                env["PATH"] = "${runtimeManager.binDir.absolutePath}:$currentPath"

                val process = processBuilder.start()
                val outputBuilder = StringBuilder()

                val stdInThread = Thread {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            outputBuilder.appendLine(line)
                            onOutput(line ?: "")
                        }
                    }
                }

                val stdErrThread = Thread {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            outputBuilder.appendLine(line)
                            onOutput(line ?: "")
                        }
                    }
                }

                stdInThread.start()
                stdErrThread.start()

                process.waitFor()
                stdInThread.join()
                stdErrThread.join()

                CommandResult(outputBuilder.toString().trim(), process.exitValue(), command)
            } catch (e: Exception) {
                val errorMsg = "Error executing local command: ${e.message}"
                onOutput(errorMsg)
                CommandResult(errorMsg, -1, command)
            }
        }

    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder("sh", "-c", command)
            processBuilder.directory(runtimeManager.runtimeDir)
            val env = processBuilder.environment()
            val currentPath = env["PATH"] ?: ""
            env["PATH"] = "${runtimeManager.binDir.absolutePath}:$currentPath"

            val process = processBuilder.start()
            val output = buildString {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line)
                    }
                }
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line)
                    }
                }
            }
            process.waitFor()
            CommandResult(output.trim(), process.exitValue(), command)
        } catch (e: Exception) {
            CommandResult("Error executing local command: ${e.message}", -1, command)
        }
    }

    suspend fun executePrivilegedCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            return@withContext CommandResult("Shizuku is not running or unavailable.", -1, command)
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
                null, arrayOf("sh", "-c", command), null, null
            ) as Process

            val output = buildString {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line)
                    }
                }
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line)
                    }
                }
            }
            process.waitFor()
            CommandResult(output.trim(), process.exitValue(), command)
        } catch (e: NoSuchMethodException) {
            CommandResult(
                "Shizuku API changed — newProcess method not found. Please update the app.", -1, command
            )
        } catch (e: SecurityException) {
            CommandResult(
                "Shizuku permission denied. Grant permission in the Shizuku app.", -1, command
            )
        } catch (e: Exception) {
            CommandResult("Error executing privileged command via Shizuku: ${e.message}", -1, command)
        }
    }
}
