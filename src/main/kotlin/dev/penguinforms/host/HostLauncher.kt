package dev.penguinforms.host

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import dev.penguinforms.settings.PenguinFormsSettings
import java.io.File

/** Thrown when the designer can't start: carries a plain-English explanation for Jacob. */
class HostNotAvailable(message: String, val needsDotnet: Boolean = false) : RuntimeException(message)

/** Starts Formwork.Host. Always with an argument list (GeneralCommandLine), never through a shell. */
object HostLauncher {
    private val log = logger<HostLauncher>()
    const val PLUGIN_ID = "dev.penguinforms"

    /** The bundled host (`<plugin>/host/Formwork.Host.dll`), unless the settings name another. */
    fun hostDll(): File? {
        PenguinFormsSettings.getInstance().state.hostPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.isFile }?.let { return it }
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)) ?: return null
        return plugin.pluginPath.resolve("host").resolve("Formwork.Host.dll").toFile().takeIf { it.isFile }
    }

    fun start(workingDir: File?): HostClient {
        val dotnet = DotnetLocator.find(PenguinFormsSettings.getInstance().state.dotnetPath)
            ?: throw HostNotAvailable("Penguin Forms needs the .NET 10 SDK, and it could not find the 'dotnet' program on this computer.", needsDotnet = true)
        val dll = hostDll() ?: throw HostNotAvailable("Penguin Forms' designer host is missing from the plugin. Please reinstall the Penguin Forms plugin.")
        val cmd = GeneralCommandLine(dotnet.path, dll.path)
            .withWorkDirectory(dll.parentFile)
            .withEnvironment("DOTNET_CLI_TELEMETRY_OPTOUT", "1")
            .withEnvironment("DOTNET_NOLOGO", "1")
            .withCharset(Charsets.UTF_8)
        val process = try { cmd.createProcess() } catch (e: Exception) {
            throw HostNotAvailable("Penguin Forms could not start the designer host: ${e.message}")
        }
        val client = HostClient(process) { line -> log.info("host[${process.pid()}]: $line") }
        HostRegistry.register(client)
        try {
            client.call("formwork.hello", timeoutSeconds = 30)
        } catch (e: HostException) {
            client.close()
            HostRegistry.unregister(client)
            val net10 = DotnetLocator.hasNet10(dotnet)
            throw HostNotAvailable(
                if (!net10) "Penguin Forms needs the .NET 10 runtime, but '${dotnet.path}' doesn't have it." else "The designer host didn't start: ${e.message}",
                needsDotnet = !net10,
            )
        }
        return client
    }
}

/** Every running host, so none is left behind when Rider exits. */
object HostRegistry {
    private val clients = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<HostClient, Boolean>())

    init {
        Runtime.getRuntime().addShutdownHook(Thread({ killAll() }, "Penguin Forms host cleanup"))
    }

    fun register(client: HostClient) { clients.add(client) }
    fun unregister(client: HostClient) { clients.remove(client) }
    fun count(): Int = clients.count { it.alive }

    fun killAll() {
        for (c in clients.toList()) try { c.close() } catch (_: Exception) { }
        clients.clear()
    }
}
