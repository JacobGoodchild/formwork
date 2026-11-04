package dev.formwork

import dev.formwork.host.HostClient
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

    /** A fresh copy of a sample, so tests never touch the originals. */
    fun copySample(name: String): File {
        val target = Files.createTempDirectory("formwork-test").toFile()
        File(repo, "samples/$name").copyRecursively(target, overwrite = true)
        return target
    }
}
