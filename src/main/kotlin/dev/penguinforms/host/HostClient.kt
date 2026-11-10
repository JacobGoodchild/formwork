package dev.penguinforms.host

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** An error answered by the host (see docs/development/PROTOCOL.md). */
class HostException(
    val kind: String,
    override val message: String,
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val detail: String? = null,
) : RuntimeException(message)

/** One answer: the result, plus the full new text of every file the request changed. */
data class HostResponse(val result: JsonElement?, val writes: Map<String, String>)

/**
 * Talks to one running Formwork.Host process: one JSON object per line each way, answered in order,
 * matched by id. This class knows nothing about Rider, so it can be unit-tested against the real host.
 *
 * The process is created by the caller (see [HostLauncher]) so that the command line is always an
 * argument list, never a shell string.
 */
class HostClient(private val process: Process, private val log: (String) -> Unit = {}) : AutoCloseable {
    // Nulls kept: a file the editor no longer has (an undo took away a new Form1.resx) is sent as null, not left out,
    // or the host would go on using its last copy.
    private val gson = GsonBuilder().serializeNulls().create()
    private val nextId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CompletableFuture<HostResponse>>()
    private val stdin = OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8)
    private val stderrTail = StringBuilder()
    @Volatile private var exited = false

    init {
        Thread({ readStdout() }, "Penguin Forms host stdout").apply { isDaemon = true }.start()
        Thread({ readStderr() }, "Penguin Forms host stderr").apply { isDaemon = true }.start()
        process.onExit().thenRun {
            exited = true
            val error = HostException("internal", "The designer host stopped (exit code ${process.exitValue()}).", detail = synchronized(stderrTail) { stderrTail.toString() })
            pending.values.forEach { it.completeExceptionally(error) }
            pending.clear()
        }
    }

    val alive: Boolean get() = !exited && process.isAlive

    val pid: Long get() = process.pid()

    private fun readStdout() {
        BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
            while (true) {
                val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
                if (line.isBlank()) continue
                val message = try { JsonParser.parseString(line).asJsonObject } catch (_: Exception) { log("host: $line"); continue }
                val id = message.get("id")?.takeIf { it.isJsonPrimitive }?.asInt ?: continue
                val future = pending.remove(id) ?: continue
                val error = message.get("error")
                if (error != null && error.isJsonObject) {
                    val e = error.asJsonObject
                    future.completeExceptionally(
                        HostException(
                            kind = e.str("kind") ?: "internal",
                            message = e.str("message") ?: "Unknown error",
                            file = e.str("file"),
                            line = e.get("line")?.takeIf { it.isJsonPrimitive }?.asInt,
                            column = e.get("column")?.takeIf { it.isJsonPrimitive }?.asInt,
                            detail = e.str("detail"),
                        )
                    )
                } else {
                    val writes = LinkedHashMap<String, String>()
                    message.getAsJsonObject("writes")?.entrySet()?.forEach { (k, v) -> if (v.isJsonPrimitive) writes[k] = v.asString }
                    future.complete(HostResponse(message.get("result"), writes))
                }
            }
        }
    }

    private fun readStderr() {
        BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8)).use { reader ->
            while (true) {
                val line = try { reader.readLine() } catch (_: Exception) { null } ?: break
                log(line)
                synchronized(stderrTail) {
                    stderrTail.append(line).append('\n')
                    if (stderrTail.length > 8000) stderrTail.delete(0, stderrTail.length - 8000)
                }
            }
        }
    }

    /**
     * Sends a request. [files] are the editor's current texts of the form's files (null = the file does not
     * exist); the host reads those instead of the disk.
     */
    fun request(method: String, params: JsonObject? = null, files: Map<String, String?>? = null): CompletableFuture<HostResponse> {
        if (!alive) return CompletableFuture.failedFuture(HostException("internal", "The designer host is not running."))
        val id = nextId.getAndIncrement()
        val future = CompletableFuture<HostResponse>()
        pending[id] = future
        val message = JsonObject().apply {
            addProperty("id", id)
            addProperty("method", method)
            if (params != null) add("params", params)
            if (files != null) add("files", JsonObject().also { o -> files.forEach { (k, v) -> o.addProperty(k, v) } })
        }
        try {
            synchronized(stdin) {
                stdin.write(gson.toJson(message))
                stdin.write("\n")
                stdin.flush()
            }
        } catch (e: Exception) {
            pending.remove(id)
            future.completeExceptionally(HostException("internal", "Could not talk to the designer host: ${e.message}"))
        }
        return future
    }

    /** Sends a request and waits for its answer. */
    fun call(method: String, params: JsonObject? = null, files: Map<String, String?>? = null, timeoutSeconds: Long = 60): HostResponse =
        try {
            request(method, params, files).get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause ?: e
        } catch (e: java.util.concurrent.TimeoutException) {
            throw HostException("internal", "The designer host did not answer '$method' within $timeoutSeconds seconds.")
        }

    /** Asks the host to close, then makes sure the process is gone (no orphans). */
    override fun close() {
        if (process.isAlive) {
            try { request("close").get(2, TimeUnit.SECONDS) } catch (_: Exception) { }
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
        }
        exited = true
    }

    private fun JsonObject.str(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
}
