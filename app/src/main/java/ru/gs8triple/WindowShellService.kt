package ru.gs8triple

import android.os.Binder
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku UserService. Runs with shell UID (Shizuku started by ADB) or root UID
 * (Shizuku/Sui started with root). It deliberately accepts argv arrays, not
 * arbitrary shell command strings, to avoid shell injection.
 */
class WindowShellService : IWindowShellService.Stub() {
    override fun execute(args: Array<out String>): String {
        if (args.isEmpty()) return "ERROR: empty command"
        val allowed = setOf("am", "dumpsys", "id", "getprop")
        if (args[0] !in allowed) return "ERROR: command not allowed"
        return try {
            val process = ProcessBuilder(args.toList()).redirectErrorStream(true).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val code = process.waitFor()
            "exit=$code\n$output"
        } catch (e: Exception) {
            "ERROR: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
