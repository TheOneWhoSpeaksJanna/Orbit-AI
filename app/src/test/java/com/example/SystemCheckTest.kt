package com.example

import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader

class SystemCheckTest {
    
    data class PkgInfo(val id: String, val url: String, val binary: String, val isTarGz: Boolean = true)

    val packages = listOf(
        PkgInfo("git", "https://github.com/nwtgck/pre-built-static-static-git/releases/download/v2.44.0/git-aarch64-linux-gnu.tar.gz", "git"),
        PkgInfo("python", "https://github.com/indygreg/python-build-standalone/releases/download/20230507/cpython-3.11.3+20230507-aarch64-unknown-linux-musl-install_only.tar.gz", "python/bin/python3"),
        PkgInfo("nodejs", "https://nodejs.org/dist/v18.16.0/node-v18.16.0-linux-arm64.tar.gz", "node-v18.16.0-linux-arm64/bin/node"),
        PkgInfo("curl", "https://curl.se/download/curl-8.5.0.tar.gz", "curl"), // It's source, we'll see it fail
        PkgInfo("busybox", "https://busybox.net/downloads/binaries/1.35.0-arm64/busybox", "busybox", false)
    )

    @Test
    fun runVerification() {
        val client = OkHttpClient()
        val tempDir = File(System.getProperty("java.io.tmpdir"), "orbit_runtime").apply { mkdirs() }
        val downloadsDir = File(tempDir, "downloads").apply { mkdirs() }
        val packagesDir = File(tempDir, "packages").apply { mkdirs() }
        val binDir = File(tempDir, "bin").apply { mkdirs() }
        
        println("================= SYSTEM CHECK =================")

        for (pkg in packages) {
            println("PACKAGE REPORT")
            println("Package: ${pkg.id}")
            
            try {
                // 1. Download
                val downloadFile = File(downloadsDir, if (pkg.isTarGz) "${pkg.id}.tar.gz" else pkg.id)
                if (!downloadFile.exists()) {
                    val request = Request.Builder().url(pkg.url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(downloadFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                
                // 2. Extract
                val installDir = File(packagesDir, pkg.id)
                installDir.mkdirs()
                if (pkg.isTarGz) {
                    val p = ProcessBuilder("tar", "-xzf", downloadFile.absolutePath, "-C", installDir.absolutePath).start()
                    p.waitFor()
                    if (p.exitValue() != 0) throw Exception("Extraction failed with ${p.exitValue()}")
                } else {
                    val target = File(installDir, pkg.binary)
                    target.parentFile?.mkdirs()
                    downloadFile.copyTo(target, overwrite = true)
                    target.setExecutable(true)
                }

                val actualBinary = File(installDir, pkg.binary)
                actualBinary.setExecutable(true)
                val installPath = actualBinary.absolutePath
                println("Install Path: $installPath")

                if (!actualBinary.exists()) {
                    println("Status: FAIL")
                    println("Architecture: unknown")
                    println("Stdout: ")
                    println("Stderr: NO_SUCH_FILE")
                    println("Exit Code: -1")
                    println("Failure Reason: NO_SUCH_FILE")
                    println("------------------------------------------------------------")
                    continue
                }

                // 4. Architecture Validation
                var arch = "unknown"
                try {
                    val pFile = ProcessBuilder("file", installPath).start()
                    val fileOut = pFile.inputStream.bufferedReader().readText()
                    if (fileOut.contains("aarch64") || fileOut.contains("ARM aarch64")) {
                        arch = "aarch64"
                    } else if (fileOut.contains("x86-64") || fileOut.contains("x86_64")) {
                        arch = "x86_64"
                    } else if (fileOut.contains("armv7") || fileOut.contains("ARM")) {
                        arch = "armv7"
                    } else {
                        arch = "unknown ($fileOut)"
                    }
                } catch (e: Exception) {}

                // 5. Execution Test
                val cmd = when(pkg.id) {
                    "git" -> listOf(installPath, "--version")
                    "python" -> listOf(installPath, "-c", "print('test')")
                    "nodejs" -> listOf(installPath, "-e", "console.log('test')")
                    "curl" -> listOf(installPath, "--version")
                    "busybox" -> listOf(installPath)
                    else -> listOf(installPath)
                }

                val execP = ProcessBuilder(cmd).start()
                val stdout = execP.inputStream.bufferedReader().readText().trim()
                val stderr = execP.errorStream.bufferedReader().readText().trim()
                execP.waitFor()
                val exitCode = execP.exitValue()

                println("Architecture: $arch")
                println("Stdout: $stdout")
                println("Stderr: $stderr")
                println("Exit Code: $exitCode")

                if (exitCode == 0) {
                    println("Status: PASS")
                    println("Failure Reason: NONE")
                } else {
                    println("Status: FAIL")
                    var reason = "UNKNOWN"
                    if (stderr.contains("Exec format error")) reason = "EXEC_FORMAT_ERROR"
                    else if (stderr.contains("Permission denied")) reason = "PERMISSION_DENIED"
                    else if (stderr.contains("not found")) reason = "LINKER_MISSING"
                    else if (exitCode == 126) reason = "EXEC_FORMAT_ERROR"
                    else if (exitCode == 127) reason = "NO_SUCH_FILE / LINKER_MISSING"
                    println("Failure Reason: $reason")
                }

            } catch (e: Exception) {
                println("Status: FAIL")
                println("Architecture: unknown")
                println("Install Path: unknown")
                println("Stdout: ")
                println("Stderr: ${e.message}")
                println("Exit Code: -1")
                println("Failure Reason: EXCEPTION_DURING_INSTALL")
            }
            println("------------------------------------------------------------")
        }
    }
}
