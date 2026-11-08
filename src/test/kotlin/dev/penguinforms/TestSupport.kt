package dev.penguinforms

import dev.penguinforms.host.HostClient
import java.io.File
import java.nio.file.Files

object TestSupport {
    val repo: File = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }

    val dotnet: String = listOfNotNull(System.getenv("DOTNET_ROOT")?.let { "$it/dotnet" }, "/opt/dotnet/dotnet", "/usr/share/dotnet/dotnet", "/usr/lib/dotnet/dotnet")
        .firstOrNull { File(it).canExecute() } ?: "dotnet"

    val hostDll: File = File(repo, "build/host/linux-x64/Formwork.Host.dll")

    fun startHost(): HostClient {
        check(hostDll.isFile) { "Build the host first: ./gradlew publishHost" }
        val process = ProcessBuilder(listOf(dotnet, hostDll.path)).start()
        return HostClient(process)
    }

    /**
     * Scratch folders for tests live in the project's own build/ folder (not the shared /tmp), so `./gradlew clean`
     * or deleting build/ removes them, and each test run starts by clearing the last run's.
     */
    val scratch: File by lazy { File(repo, "build/test-scratch").also { it.deleteRecursively(); it.mkdirs() } }

    fun tempDir(prefix: String): File = Files.createTempDirectory(scratch.toPath(), prefix).toFile()

    /** A fresh copy of a sample, so tests never touch the originals. */
    fun copySample(name: String): File {
        val target = tempDir("penguinforms-test")
        File(repo, "samples/$name").copyRecursively(target, overwrite = true)
        return target
    }
}
