package dev.formwork.editor

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.impl.isTrusted
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import dev.formwork.host.HostClient
import dev.formwork.host.HostException
import dev.formwork.host.HostLauncher
import dev.formwork.host.HostNotAvailable
import dev.formwork.host.HostRegistry
import dev.formwork.settings.FormworkSettings
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * One open form: owns its host process and connects the webview to it. The Kotlin counterpart of NetForms'
 * `designerEditorProvider.ts`, with one big difference: **every write goes through Rider's documents**, as one
 * command per gesture, so Ctrl+Z (in the designer or in the code) undoes it, unsaved changes show, and the
 * user's own unsaved typing is never overwritten. The host's own undo stack is not used.
 */
class DesignerSession(
    val project: Project,
    val form: FormFiles,
    private val post: (JsonObject) -> Unit,
    private val undoContext: () -> com.intellij.openapi.fileEditor.FileEditor,
) : Disposable {
    private val log = logger<DesignerSession>()
    private val gson = Gson()
    private val worker: java.util.concurrent.ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "Formwork designer: ${form.displayName}").apply { isDaemon = true }
    }
    private var host: HostClient? = null
    @Volatile private var applyingOwnWrites = false
    @Volatile private var disposed = false
    private var reopenTask: ScheduledFuture<*>? = null
    private val listenedDocuments = HashSet<Document>()
    /** Set when the latest view could be shown; the webview then holds a form (not an error page). */
    @Volatile var lastView: JsonObject? = null
        private set
    var webviewState: JsonElement? = null
    /** Called (on the EDT) after the form's files changed, so the tab title (`*`) can be refreshed. */
    var onFilesChanged: () -> Unit = {}

    companion object {
        const val CLIPBOARD_MARKER = "netforms/components-1"
        const val COMMAND_NAME = "Formwork Designer Edit"
    }

    // ---- messages from the webview ------------------------------------------------------------------------

    fun onMessage(json: String) {
        val m = try { JsonParser.parseString(json).asJsonObject } catch (_: Exception) { return }
        if (m.str("type") == "__fwState") { webviewState = m.get("state"); return }
        worker.execute { if (!disposed) handle(m) }
    }

    private fun handle(m: JsonObject) {
        try {
            when (m.str("type")) {
                "ready" -> {
                    val h = ensureHost()
                    val view = h.call("open", JsonObject().apply { addProperty("path", form.designer.path) }, files()).result!!.asJsonObject
                    lastView = view
                    val toolbox = h.call("toolbox").result
                    listenToDocuments()
                    post(JsonObject().apply {
                        addProperty("type", "init")
                        add("view", view)
                        add("toolbox", toolbox)
                        if (!project.isTrusted()) addProperty("notice", "This project isn't trusted, so only the built-in controls are shown (your project's own controls would run its code).")
                        addProperty("snap", FormworkSettings.getInstance().state.snapToLines)
                        add("strings", JsonObject())
                    })
                }
                "apply" -> show(write(m.get("ops")?.asJsonArray ?: JsonArray(), nameFor(m.get("ops")?.asJsonArray)), m.get("select"))
                "undo" -> undoRedo(undo = true)
                "redo" -> undoRedo(undo = false)
                "menu" -> show(ensureHost().call("menu", JsonObject().apply { addProperty("id", m.str("id") ?: "") }, files()).result!!.asJsonObject, null)
                "properties", "events" -> {
                    val type = m.str("type")!!
                    val rows = ensureHost().call(type, JsonObject().apply { addProperty("id", m.str("id") ?: "") }, files()).result
                    post(JsonObject().apply { addProperty("type", type); addProperty("id", m.str("id") ?: ""); add("rows", rows) })
                }
                "gotoHandler" -> gotoHandler(m.str("handler") ?: return, null)
                "viewCode" -> viewCode()
                "openAsText" -> onEdt { OpenFileDescriptor(project, vf(form.designer) ?: return@onEdt).navigate(true) }
                "pickFiles" -> pickFiles(m)
                "copy" -> {
                    val ids = m.get("ids")?.asJsonArray ?: JsonArray()
                    copy(ids)
                    post(JsonObject().apply { addProperty("type", "copied"); addProperty("count", ids.size()) })
                }
                "cut" -> {
                    val ids = m.get("ids")?.asJsonArray ?: JsonArray()
                    copy(ids)
                    val ops = JsonArray().apply { ids.forEach { id -> add(JsonObject().apply { addProperty("op", "remove"); add("id", id) }) } }
                    show(write(ops, "Cut"), m.get("select"))
                }
                "paste" -> {
                    val text = clipboardText()
                    if (text == null || !text.contains(CLIPBOARD_MARKER)) throw HostException("edit", "The clipboard holds no controls. Copy some in a Formwork designer first.")
                    val op = JsonObject().apply { addProperty("op", "paste"); addProperty("value", text); addProperty("parent", m.str("parent") ?: "") }
                    show(write(JsonArray().apply { add(op) }, "Paste"), null)
                }
                "duplicate" -> {
                    val op = JsonObject().apply { addProperty("op", "duplicate"); add("ids", m.get("ids")) }
                    show(write(JsonArray().apply { add(op) }, "Duplicate"), null)
                }
                "run" -> onEdt {
                    val action = com.intellij.openapi.actionSystem.ActionManager.getInstance().getAction("Run") ?: return@onEdt
                    com.intellij.openapi.actionSystem.ex.ActionUtil.invokeAction(action, com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project), "FormworkDesigner", null, null)
                }
                "rescan", "addLibrary", "setStartup" -> notify("This command isn't in Formwork yet.", NotificationType.INFORMATION)
            }
        } catch (e: Throwable) {
            report(e)
        }
    }

    private fun nameFor(ops: JsonArray?): String {
        val first = ops?.firstOrNull()?.asJsonObject?.str("op") ?: return COMMAND_NAME
        return when (first) {
            "add" -> "Add Control"
            "remove" -> "Delete"
            "setBounds" -> "Move/Resize"
            "setProp", "resetProp", "setItems" -> "Property Change"
            "setEvent" -> "Event Handler"
            "rename" -> "Rename"
            else -> COMMAND_NAME
        }
    }

    // ---- the host -----------------------------------------------------------------------------------------

    private fun ensureHost(): HostClient {
        host?.takeIf { it.alive }?.let { return it }
        val started = HostLauncher.start(form.code.parentFile)
        host = started
        return started
    }

    /** What Rider holds for the form's files right now: open documents (unsaved edits included), else the disk. */
    fun files(): Map<String, String?> = ReadAction.compute<Map<String, String?>, RuntimeException> {
        val fdm = FileDocumentManager.getInstance()
        form.all.associate { f ->
            val v = LocalFileSystem.getInstance().findFileByIoFile(f)
            f.path to when {
                v == null || !v.isValid -> null
                else -> fdm.getDocument(v)?.text ?: try { VfsUtil.loadText(v) } catch (_: Exception) { null }
            }
        }
    }

    /** Applies ops in the host, then its writes to Rider's documents as one undoable command. */
    private fun write(ops: JsonArray, name: String): JsonObject {
        val response = ensureHost().call("apply", JsonObject().apply { add("ops", ops) }, files())
        val view = response.result!!.asJsonObject
        applyWrites(response.writes, name, view.handlerIfCreated())
        return view
    }

    private fun JsonObject.handlerIfCreated(): JsonObject? =
        get("handler")?.takeIf { it.isJsonObject }?.asJsonObject?.takeIf { it.get("created")?.asBoolean == true }

    /**
     * Writes the host's new texts into Rider's documents: one command (one Ctrl+Z step) for all files, each
     * document changed only where it differs, so carets and markers elsewhere stay put. A new handler stub gets
     * its empty line indented, as Visual Studio leaves the cursor there.
     */
    private fun applyWrites(writes: Map<String, String>, name: String, createdHandler: JsonObject?) {
        if (writes.isEmpty()) return
        onEdt {
            applyingOwnWrites = true
            try {
                WriteCommandAction.writeCommandAction(project)
                    .withName("Formwork: $name")
                    .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
                    .withGlobalUndo()
                    .run<RuntimeException> {
                        for ((path, raw) in writes) {
                            val text = StringUtil.convertLineSeparators(raw)
                            val file = File(path)
                            var v = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                            if (v == null) {
                                val dir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file.parentFile) ?: continue
                                v = dir.createChildData(this, file.name)
                            }
                            val doc = FileDocumentManager.getInstance().getDocument(v) ?: continue
                            DocumentDiff.replace(doc, text)
                        }
                    }
            } finally {
                applyingOwnWrites = false
            }
            listenToDocuments()
            onFilesChanged()
        }
    }

    // ---- undo / reload ------------------------------------------------------------------------------------

    /** Ctrl+Z / Ctrl+Y in the designer: Rider's own undo, over the form's documents. */
    private fun undoRedo(undo: Boolean) {
        var did = false
        onEdt {
            val manager = UndoManager.getInstance(project)
            val editor = undoContext()
            if (undo && manager.isUndoAvailable(editor)) { manager.undo(editor); did = true }
            if (!undo && manager.isRedoAvailable(editor)) { manager.redo(editor); did = true }
        }
        if (!did) lastView?.let { post(JsonObject().apply { addProperty("type", "view"); add("view", it) }) }
        // The documents changed: their listener reloads the canvas.
    }

    private fun listenToDocuments() {
        onEdt {
            for (f in form.all) {
                val v = LocalFileSystem.getInstance().findFileByIoFile(f) ?: continue
                val doc = FileDocumentManager.getInstance().getDocument(v) ?: continue
                if (!listenedDocuments.add(doc)) continue
                doc.addDocumentListener(object : DocumentListener {
                    override fun documentChanged(event: DocumentEvent) {
                        onFilesChanged()
                        if (!applyingOwnWrites) reloadSoon()
                    }
                }, this)
            }
        }
    }

    /** The form's files changed outside the designer (typed in, undo, git checkout): show them again. */
    fun reloadSoon(delayMs: Long = 250) {
        if (disposed) return
        synchronized(this) {
            reopenTask?.cancel(false)
            reopenTask = worker.schedule({ if (!disposed) reopen() }, delayMs, TimeUnit.MILLISECONDS)
        }
    }

    private fun reopen() {
        try {
            val view = ensureHost().call("open", JsonObject().apply { addProperty("path", form.designer.path) }, files()).result!!.asJsonObject
            show(view, null)
        } catch (e: Throwable) {
            report(e)
        }
    }

    // ---- showing ------------------------------------------------------------------------------------------

    private fun show(view: JsonObject, select: JsonElement?) {
        lastView = view
        post(JsonObject().apply { addProperty("type", "view"); add("view", view); if (select != null && !select.isJsonNull) add("select", select) })
        view.handlerIfCreated()?.let { h -> gotoHandler(h.str("method") ?: return@let, h.get("line")?.asInt) }
    }

    private fun report(e: Throwable) {
        when {
            e is HostException && e.kind == "code" -> post(JsonObject().apply {
                addProperty("type", "parseError"); addProperty("message", e.message)
                e.line?.let { addProperty("line", it) }; e.column?.let { addProperty("column", it) }
            })
            e is HostException -> {
                if (e.detail != null) log.warn("Designer host: ${e.message}\n${e.detail}")
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message) })
            }
            e is HostNotAvailable -> {
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message) })
                post(JsonObject().apply { addProperty("type", "fwFatal"); addProperty("message", e.message); addProperty("needsDotnet", e.needsDotnet) })
                if (e.needsDotnet) DotnetNotifier.notifyMissing(project, e.message ?: "")
            }
            else -> {
                log.warn("Formwork designer", e)
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message ?: e.toString()) })
            }
        }
    }

    private fun notify(text: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("Formwork").createNotification(text, type).notify(project)
    }

    // ---- code navigation ----------------------------------------------------------------------------------

    /** F7: the form's code file, in Rider's text editor. */
    fun viewCode() {
        onEdt { OpenFileDescriptor(project, vf(form.code) ?: return@onEdt).navigate(true) }
    }

    /**
     * Jumps into a handler in `Form1.cs` with the cursor inside its body, as Visual Studio does after a
     * double-click. [line] (1-based, from the host) is the method's line when the host just wrote it.
     */
    fun gotoHandler(method: String, line: Int?) {
        onEdt {
            val v = vf(form.code) ?: return@onEdt
            val doc = FileDocumentManager.getInstance().getDocument(v) ?: return@onEdt
            val target = HandlerLocator.bodyPosition(doc.text, method, line)
            if (target == null) {
                OpenFileDescriptor(project, v).navigate(true)
                return@onEdt
            }
            if (target.indentToInsert.isNotEmpty()) {
                applyingOwnWrites = true
                try {
                    // Same command group as the stub, so one Ctrl+Z takes both away.
                    WriteCommandAction.writeCommandAction(project).withName("Formwork: Event Handler")
                        .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
                        .run<RuntimeException> { doc.insertString(target.offset, target.indentToInsert) }
                } finally { applyingOwnWrites = false }
            }
            OpenFileDescriptor(project, v, target.offset + target.indentToInsert.length).navigate(true)
        }
    }

    // ---- clipboard and files ------------------------------------------------------------------------------

    private fun copy(ids: JsonArray) {
        val text = ensureHost().call("copy", JsonObject().apply { add("ids", ids) }, files()).result!!.asJsonObject.get("text").asString
        onEdt { CopyPasteManager.getInstance().setContents(StringSelection(text)) }
    }

    private fun clipboardText(): String? {
        var text: String? = null
        onEdt { text = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor) }
        return text
    }

    private fun pickFiles(m: JsonObject) {
        val many = m.get("many")?.asBoolean == true
        val icon = m.get("icon")?.asBoolean == true
        var paths = emptyList<String>()
        onEdt {
            val extensions = if (icon) setOf("ico") else setOf("png", "jpg", "jpeg", "gif", "bmp", "ico")
            val descriptor = FileChooserDescriptor(true, false, false, false, false, many)
                .withTitle(if (icon) "Choose an Icon" else "Choose a Picture")
                .withFileFilter { it.extension?.lowercase() in extensions }
            val start = vf(form.code.parentFile)
            paths = FileChooser.chooseFiles(descriptor, project, start).map { it.path }
        }
        post(JsonObject().apply {
            addProperty("type", "pickedFiles"); add("requestId", m.get("requestId"))
            add("paths", JsonArray().apply { paths.forEach { add(it) } })
        })
    }

    private fun vf(f: File): VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(f)

    /** Runs on the EDT and waits (the worker thread is never the EDT). */
    private fun onEdt(block: () -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) block() else app.invokeAndWait(block, ModalityState.any())
    }

    override fun dispose() {
        disposed = true
        reopenTask?.cancel(false)
        val h = host
        host = null
        worker.shutdownNow()
        if (h != null) {
            // Close in the background, then make sure the process is gone.
            ApplicationManager.getApplication().executeOnPooledThread {
                try { h.close() } finally { HostRegistry.unregister(h) }
            }
        }
    }

    private fun JsonObject.str(key: String): String? = get(key)?.takeIf { it.isJsonPrimitive }?.asString
}
