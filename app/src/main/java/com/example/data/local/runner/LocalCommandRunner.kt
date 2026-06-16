package com.example.data.local.runner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(val output: String, val exitCode: Int, val command: String)

class LocalCommandRunner(private val runtimeManager: com.example.data.local.runtime.OrbitRuntimeManager) {

    suspend fun executeCommandStreamed(command: String, onOutput: (String) -> Unit): CommandResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder("sh", "-c", command)
            processBuilder.directory(runtimeManager.runtimeDir)
            val env = processBuilder.environment()
            val currentPath = env["PATH"] ?: ""
            env["PATH"] = "${runtimeManager.binDir.absolutePath}:$currentPath"
            
            val process = processBuilder.start()
            val outputBuilder = StringBuilder()

            val stdInThread = Thread {
                java.io.BufferedReader(java.io.InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        outputBuilder.appendLine(line)
                        onOutput(line ?: "")
                    }
                }
            }
            
            val stdErrThread = Thread {
                java.io.BufferedReader(java.io.InputStreamReader(process.errorStream)).use { reader ->
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
                java.io.BufferedReader(java.io.InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendLine(line)
                    }
                }
                java.io.BufferedReader(java.io.InputStreamReader(process.errorStream)).use { reader ->
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
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
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
            CommandResult("Error executing privileged command via Shizuku: ${e.message}", -1, command)
        }
    }
}
