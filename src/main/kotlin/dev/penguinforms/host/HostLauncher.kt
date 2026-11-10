package dev.penguinforms.host

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
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
        val plugin = pluginDir() ?: return null
        return File(File(plugin, "host"), "Formwork.Host.dll").takeIf { it.isFile }
    }

    /**
     * The plugin's folder: the jar this class was loaded from is `<plugin>/lib/penguin-forms-….jar`. Found with plain
     * Java rather than the IDE's plugin manager, whose lookups (PluginManagerCore.getPlugin, PluginManager.getPluginByClass)
     * are internal API that JetBrains' plugin checker - and so the Marketplace - rejects.
     */
    internal fun pluginDir(): File? = try {
        val url = HostLauncher::class.java.getResource("HostLauncher.class")?.toString()
        if (url == null || !url.startsWith("jar:file:")) null
        else File(java.net.URI(url.removePrefix("jar:").substringBefore("!/"))).parentFile?.parentFile
    } catch (e: Exception) {
        log.warn("Penguin Forms: could not find its own plugin folder", e)
        null
    }

    fun start(workingDir: File?): HostClient {
        val search = DotnetLocator.search(PenguinFormsSettings.getInstance().state.dotnetPath)
        val dotnet = search.found ?: run {
            log.warn("Penguin Forms: no dotnet with .NET 10 found. Where it looked:\n${search.describe()}")
            throw HostNotAvailable(
                "Penguin Forms needs .NET 10, and it could not find a 'dotnet' program that has it.\n\nWhere it looked:\n${search.describe()}",
                needsDotnet = true,
            )
        }
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
