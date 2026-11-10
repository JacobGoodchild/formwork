package dev.penguinforms.host

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import java.io.File

/**
 * Finds the `dotnet` program that can run .NET 10 (which the designer host needs). A computer can have several
 * (a distribution package with .NET 8, a Microsoft install with .NET 10 in ~/.dotnet, ...), so every candidate is
 * collected and the first one with a .NET 10 or newer runtime wins.
 */
object DotnetLocator {
    /** Where .NET is usually installed on Linux, after the setting, DOTNET_ROOT, PATH and ~/.dotnet. */
    private val wellKnown = listOf("/usr/share/dotnet/dotnet", "/usr/lib/dotnet/dotnet", "/usr/lib64/dotnet/dotnet", "/opt/dotnet/dotnet", "/snap/bin/dotnet")

    /** One place looked at: the program, and the runtimes it has (empty: missing, or it didn't answer). */
    data class Checked(val path: String, val exists: Boolean, val runtimes: List<String>)

    /** The outcome of a search: [found] is the program to use (null: none has .NET 10), [checked] says where it looked. */
    data class Search(val found: File?, val checked: List<Checked>) {
        /** Plain-English list of the places looked, for error messages. */
        fun describe(): String = checked.joinToString("\n") { c ->
            when {
                !c.exists -> "  ${c.path} (not there)"
                c.runtimes.isEmpty() -> "  ${c.path} (no .NET runtime reported)"
                else -> "  ${c.path} (has .NET ${c.runtimes.joinToString(", ")})"
            }
        }
    }

    /** Every place a `dotnet` program could be, in order, without duplicates. Nothing is run. */
    internal fun candidates(configured: String?, env: Map<String, String>, home: String?): List<File> {
        val list = mutableListOf<File>()
        configured?.takeIf { it.isNotBlank() }?.let { list += File(it.trim()) }
        env["DOTNET_ROOT"]?.takeIf { it.isNotBlank() }?.let { list += File(it, "dotnet") }
        env["PATH"]?.split(File.pathSeparatorChar)?.forEach { dir ->
            if (dir.isNotEmpty() && File(dir).isAbsolute) list += File(dir, "dotnet") // never "." or a relative folder
        }
        home?.let { list += File(it, ".dotnet/dotnet") }
        wellKnown.forEach { list += File(it) }
        val seen = HashSet<String>()
        return list.filter { seen.add(it.absolutePath) }
    }

    /** Picks the first candidate whose runtimes include .NET 10 or newer; [runtimesOf] is only asked for existing programs. */
    internal fun choose(candidates: List<File>, runtimesOf: (File) -> List<String>): Search {
        val checked = mutableListOf<Checked>()
        val seenReal = HashSet<String>()
        for (f in candidates) {
            if (!f.canExecute()) { checked += Checked(f.path, false, emptyList()); continue }
            // /usr/bin/dotnet is usually a link to /usr/share/dotnet/dotnet: ask each real program once.
            val real = try { f.canonicalPath } catch (_: Exception) { f.absolutePath }
            if (!seenReal.add(real)) continue
            val runtimes = runtimesOf(f)
            checked += Checked(f.path, true, runtimes)
            if (runtimes.any(::isNet10OrNewer)) return Search(File(real), checked)
        }
        return Search(null, checked)
    }

    @Volatile private var last: Pair<String, Search>? = null

    /** Searches every candidate (runs `dotnet --list-runtimes` on each that exists; call off the UI thread). */
    fun search(configured: String? = null): Search {
        val s = choose(candidates(configured, System.getenv(), System.getProperty("user.home")), ::runtimes)
        last = (configured ?: "") to s
        return s
    }

    /** The `dotnet` to use, or null when none with .NET 10 was found. */
    fun find(configured: String? = null): File? = search(configured).found

    /** For the settings page (UI thread): the last search's answer, without running anything. */
    fun lastFound(): File? = last?.second?.found

    /** The .NET runtime versions `dotnet --list-runtimes` reports for Microsoft.NETCore.App. */
    fun runtimes(dotnet: File): List<String> = try {
        val out = ExecUtil.execAndGetOutput(
            GeneralCommandLine(dotnet.path, "--list-runtimes").withEnvironment("DOTNET_CLI_TELEMETRY_OPTOUT", "1"), 15_000,
        )
        out.stdoutLines.mapNotNull { line ->
            val parts = line.split(' ')
            if (parts.size >= 2 && parts[0] == "Microsoft.NETCore.App") parts[1] else null
        }
    } catch (_: Exception) {
        emptyList()
    }

    internal fun isNet10OrNewer(version: String): Boolean = version.substringBefore('.').toIntOrNull()?.let { it >= 10 } == true

    fun hasNet10(dotnet: File): Boolean = runtimes(dotnet).any(::isNet10OrNewer)
}
