package com.example.data.local.shizuku

import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShizukuExecutor {

    fun isInstalled(): Boolean {
        return try {
            Shizuku.getVersion() > 0
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    suspend fun executePrivilegedCommand(command: String): String {
        return withContext(Dispatchers.IO) {
            if (!hasPermission()) {
                return@withContext "Error: Shizuku permission not granted."
            }
            try {
                // Use reflection to access newProcess since it might be restricted/private in newer API versions
                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                
                val inputStream = process.inputStream.bufferedReader()
                val errorStream = process.errorStream.bufferedReader()
                
                val output = StringBuilder()
                val error = StringBuilder()
                
                var iter = inputStream.readLine()
                while (iter != null) {
                    output.append(iter).append("\n")
                    iter = inputStream.readLine()
                }

                iter = errorStream.readLine()
                while (iter != null) {
                    error.append(iter).append("\n")
                    iter = errorStream.readLine()
                }
                
                process.waitFor()
                val finalOutput = output.toString().trim()
                val finalError = error.toString().trim()
                if (finalError.isNotEmpty()) {
                    if (finalOutput.isNotEmpty()) "$finalOutput\n[STDERR]:\n$finalError" else finalError
                } else {
                    finalOutput
                }
            } catch (e: Exception) {
                e.message ?: "Unknown Shizuku error"
            }
        }
    }
}
