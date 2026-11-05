package dev.formwork.host

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import java.io.File

/** Finds the `dotnet` program and checks that it can run .NET 10 (which the designer host needs). */
object DotnetLocator {
    /** Where .NET is usually installed on Linux, after PATH and DOTNET_ROOT. */
    private val wellKnown = listOf("/usr/share/dotnet/dotnet", "/usr/lib/dotnet/dotnet", "/usr/lib64/dotnet/dotnet", "/opt/dotnet/dotnet", "/snap/bin/dotnet")

    fun find(configured: String? = null): File? {
        configured?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.canExecute() }?.let { return it }
        System.getenv("DOTNET_ROOT")?.let { File(it, "dotnet") }?.takeIf { it.canExecute() }?.let { return it }
        System.getenv("PATH")?.split(File.pathSeparatorChar)?.forEach { dir ->
            if (dir.isEmpty() || !File(dir).isAbsolute) return@forEach // never "." or a relative folder
            val f = File(dir, "dotnet")
            if (f.canExecute()) return f.canonicalFile
        }
        File(System.getProperty("user.home"), ".dotnet/dotnet").takeIf { it.canExecute() }?.let { return it }
        return wellKnown.map(::File).firstOrNull { it.canExecute() }
    }

    /** The .NET runtime versions `dotnet --list-runtimes` reports for Microsoft.NETCore.App. */
    fun runtimes(dotnet: File): List<String> = try {
        val out = ExecUtil.execAndGetOutput(GeneralCommandLine(dotnet.path, "--list-runtimes"), 15_000)
        out.stdoutLines.mapNotNull { line ->
            val parts = line.split(' ')
            if (parts.size >= 2 && parts[0] == "Microsoft.NETCore.App") parts[1] else null
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun hasNet10(dotnet: File): Boolean = runtimes(dotnet).any { it.substringBefore('.').toIntOrNull()?.let { major -> major >= 10 } == true }
}
