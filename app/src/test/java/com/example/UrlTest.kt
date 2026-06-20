package com.omniclaw

import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class UrlTest {
    @Test
    fun checkUrls() {
        try {
            val tempZip = File(System.getProperty("java.io.tmpdir"), "bootstrap.zip")
            val termuxDir = File(System.getProperty("java.io.tmpdir"), "termux")
            val xzBin = File(termuxDir, "bin/xz")
            
            // run strings to find interpreter
            val p = ProcessBuilder("strings", xzBin.absolutePath).start()
            val text = p.inputStream.bufferedReader().readText()
            val lines = text.split("\n")
            val linker = lines.filter { it.contains("linker") || it.contains("ld-") }
            println("INTERPRETER: $linker")
            
            // just execute
            val p2 = ProcessBuilder(xzBin.absolutePath, "--version").start()
            val o2 = p2.inputStream.bufferedReader().readText()
            val e2 = p2.errorStream.bufferedReader().readText()
            p2.waitFor()
            println("EXEC EXT: ${p2.exitValue()} | $o2 | $e2")
        } catch(e: Exception) {}
    }
}
