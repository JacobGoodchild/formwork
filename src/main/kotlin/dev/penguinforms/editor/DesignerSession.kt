package dev.penguinforms.editor

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
import dev.penguinforms.host.HostClient
import dev.penguinforms.host.HostException
import dev.penguinforms.host.HostLauncher
import dev.penguinforms.host.HostNotAvailable
import dev.penguinforms.host.HostRegistry
import dev.penguinforms.settings.PenguinFormsSettings
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
        Thread(r, "Penguin Forms designer: ${form.displayName}").apply { isDaemon = true }
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
    /** News for the native tool windows (set by the editor; publishes through [ActiveDesigner]). */
    var publish: ((DesignerListener) -> Unit) -> Unit = {}
    @Volatile var selection: List<String> = listOf("")
        private set
    @Volatile var lastToolbox: JsonArray? = null
        private set
    @Volatile var lastNotice: String? = null
        private set

    companion object {
        const val CLIPBOARD_MARKER = "netforms/components-1"
        const val COMMAND_NAME = "Penguin Forms Designer Edit"
    }

    // ---- messages from the webview ------------------------------------------------------------------------

    fun onMessage(json: String) {
        val m = try { JsonParser.parseString(json).asJsonObject } catch (e: Exception) { log.warn("Penguin Forms add: unreadable page message (${json.take(200)}): ${e.message}"); return }
        // bridge.js numbers its messages: a gap means one was lost on the way.
        m.get("__seq")?.takeIf { it.isJsonPrimitive }?.asLong?.let { n ->
            val expected = lastSeq + 1
            if (lastSeq != 0L && n != expected) log.warn("Penguin Forms add: page messages ${expected}..${n - 1} never arrived (got $n after $lastSeq)")
            lastSeq = n
        }
        val adds = addsIn(m)
        if (adds != null) log.info("Penguin Forms add: page asked to add $adds to ${form.displayName}")
        when (m.str("type")) {
            "__fwState" -> { webviewState = m.get("state"); DesignerStateStore.put(form.code.path, m.get("state").toString()); return }
            "__fwLog" -> {
                val text = m.str("message") ?: ""
                if (m.str("level") == "info") log.info(text) else log.warn(if (text.startsWith("Penguin Forms")) text else "Penguin Forms page: $text")
                if (text.startsWith("Penguin Forms add:") && m.str("level") != "info") notifyAddFailed(text.removePrefix("Penguin Forms add:").trim())
                return
            }
            "__fwSelection" -> {
                selection = m.getAsJsonArray("ids")?.map { it.asString } ?: listOf("")
                publish { l -> l.selectionChanged(editorRef(), selection) }
                return
            }
            "__fwTool" -> {
                val t = m.get("tool")?.takeIf { it.isJsonPrimitive }?.asString
                log.info("Penguin Forms add: page tool is now ${t ?: "Pointer"}")
                // Back on the Pointer by the page itself (a control was placed): its add should follow at once.
                val picked = pickedTool
                if (t == null && picked != null && !releasedByIde) watchForAdd(picked)
                if (t == null) { pickedTool = null; releasedByIde = false }
                publish { l -> l.toolChanged(editorRef(), t) }
                return
            }
            else -> log.debug("Designer message: ${m.str("type")}")
        }
        worker.execute { if (!disposed) handle(m) }
    }

    private fun handle(m: JsonObject) {
        try {
            when (m.str("type")) {
                "ready" -> {
                    log.info("Designer page ready for ${form.designer}")
                    val h = ensureHost()
                    loadLibraries(h)
                    val view = h.call("open", JsonObject().apply { addProperty("path", form.designer.path) }, files()).result!!.asJsonObject
                    lastView = view
                    val toolbox = toolbox(h)
                    requestRows("properties", selection.lastOrNull() ?: "")
                    lastToolbox = toolbox
                    lastNotice = libraryNotice()
                    publish { l -> l.toolboxChanged(editorRef(), toolbox, lastNotice); l.viewChanged(editorRef(), view) }
                    listenToDocuments()
                    post(JsonObject().apply {
                        addProperty("type", "init")
                        add("view", view)
                        add("toolbox", toolbox)
                        libraryNotice()?.let { addProperty("notice", it) }
                        addProperty("snap", PenguinFormsSettings.getInstance().state.snapToLines)
                        add("strings", JsonObject())
                    })
                }
                "apply" -> {
                    val ops = m.get("ops")?.asJsonArray ?: JsonArray()
                    val adds = addsIn(m)
                    if (adds == null) {
                        checkOps(ops)
                        show(write(ops, nameFor(ops)), m.get("select"))
                    } else {
                        addArrived = true
                        try {
                            checkOps(ops)
                            val view = write(ops, nameFor(ops))
                            val count = listOf("items", "tray").sumOf { view.getAsJsonArray(it)?.size() ?: 0 }
                            log.info("Penguin Forms add: done ($adds); the form now has $count components")
                            show(view, m.get("select"))
                        } catch (e: Throwable) {
                            log.warn("Penguin Forms add: adding $adds failed: ${e.message}", e)
                            notifyAddFailed("${e.message ?: e.javaClass.simpleName}")
                            throw e
                        }
                    }
                }
                "undo" -> undoRedo(undo = true)
                "redo" -> undoRedo(undo = false)
                "menu" -> show(ensureHost().call("menu", JsonObject().apply { addProperty("id", m.str("id") ?: "") }, files()).result!!.asJsonObject, null)
                "properties", "events" -> {
                    val type = m.str("type")!!
                    val id = m.str("id") ?: ""
                    // The page asks for the rows of its primary selection: that is the selection news too (after an
                    // add or a paste the page selects the new control without another signal).
                    if (selection.lastOrNull() != id) {
                        selection = listOf(id)
                        publish { l -> l.selectionChanged(editorRef(), selection) }
                    }
                    val rows = ensureHost().call(type, JsonObject().apply { addProperty("id", id) }, files()).result
                    post(JsonObject().apply { addProperty("type", type); addProperty("id", id); add("rows", rows) })
                    rows?.takeIf { it.isJsonArray }?.asJsonArray?.let { r ->
                        publish { l -> if (type == "properties") l.propertiesChanged(editorRef(), id, r) else l.eventsChanged(editorRef(), id, r) }
                    }
                }
                "gotoHandler" -> gotoHandler(m.str("handler") ?: return, null)
                "viewCode" -> viewCode()
                // "Open as text": the designer file in Rider's editor, at the line the designer couldn't read.
                "openAsText" -> onEdt {
                    val v = vf(form.designer) ?: return@onEdt
                    val e = lastCodeError
                    if (e?.line != null) OpenFileDescriptor(project, v, e.line - 1, maxOf(0, (e.column ?: 1) - 1)).navigate(true)
                    else OpenFileDescriptor(project, v).navigate(true)
                }
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
                    if (text == null || !text.contains(CLIPBOARD_MARKER)) throw HostException("edit", "The clipboard holds no controls. Copy some in a Penguin Forms designer first.")
                    val op = JsonObject().apply { addProperty("op", "paste"); addProperty("value", text); addProperty("parent", m.str("parent") ?: "") }
                    show(write(JsonArray().apply { add(op) }, "Paste"), null)
                }
                "duplicate" -> {
                    val op = JsonObject().apply { addProperty("op", "duplicate"); add("ids", m.get("ids")) }
                    show(write(JsonArray().apply { add(op) }, "Duplicate"), null)
                }
                "run" -> if (project.isTrusted()) onEdt {
                    val action = com.intellij.openapi.actionSystem.ActionManager.getInstance().getAction("Run") ?: return@onEdt
                    com.intellij.openapi.actionSystem.ex.ActionUtil.invokeAction(action, com.intellij.openapi.actionSystem.impl.SimpleDataContext.getProjectContext(project), "PenguinFormsDesigner", null, null)
                }
                "rescan", "addLibrary", "setStartup" -> notify("This command isn't in Penguin Forms yet.", NotificationType.INFORMATION)
            }
        } catch (e: Throwable) {
            report(e)
        }
    }

    // ---- diagnosing adds (see "How to check the add-a-control fix" in TESTING-FOR-JACOB.md) ------------------

    @Volatile private var lastSeq = 0L
    @Volatile private var pickedTool: String? = null
    @Volatile private var releasedByIde = false
    @Volatile private var addArrived = false

    /** The controls an "apply" message adds, e.g. "System.Windows.Forms.Button", or null if it adds none. */
    private fun addsIn(m: JsonObject): String? {
        if (m.str("type") != "apply") return null
        val types = m.get("ops")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { o -> o.takeIf { it.isJsonObject }?.asJsonObject?.takeIf { it.str("op") == "add" }?.let { it.str("type") ?: "(no type)" } }
        return types?.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    /** The native Toolbox picked a tool (null: the Pointer). */
    fun toolPickedInIde(type: String?) {
        if (type == null) { releasedByIde = true } else { pickedTool = type; releasedByIde = false }
    }

    /** The page went back to the Pointer after placing [type]: its add must arrive within a few seconds. */
    private fun watchForAdd(type: String) {
        addArrived = false
        worker.schedule({
            if (!disposed && !addArrived) {
                log.warn("Penguin Forms add: the page put the Toolbox back on the Pointer after placing $type, but no add arrived")
            }
        }, 3, TimeUnit.SECONDS)
    }

    private fun notifyAddFailed(why: String) {
        if (disposed) return
        NotificationGroupManager.getInstance().getNotificationGroup("Penguin Forms").createNotification(
            "Penguin Forms couldn't add the control",
            "$why<br>Details are in Help → Show Log in Files (lines with \"Penguin Forms add:\").",
            NotificationType.ERROR,
        ).notify(project)
    }

    fun nameFor(ops: JsonArray?): String {
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

    // ---- for the native tool windows ----------------------------------------------------------------------

    /** The editor this session belongs to (set by it). */
    lateinit var editorRef: () -> DesignerEditor

    /** Edits from the Properties window: one undoable step, then the canvas and the grids update. */
    fun applyOps(ops: JsonArray, name: String = nameFor(ops)) {
        worker.execute {
            try { show(write(ops, name), null) } catch (e: Throwable) { report(e); refreshInspector() }
        }
    }

    /** Asks for the property (or event) rows of a component; they arrive through the listener. */
    fun requestRows(kind: String, id: String) {
        worker.execute {
            // Before the form has opened there is nothing to ask (the rows follow the first view).
            val view = lastView ?: return@execute
            // A control that is gone (undo, a text edit) is no longer asked about: the form is shown instead.
            if (id.isNotEmpty() && !exists(view, id)) {
                selection = listOf("")
                publish { l -> l.selectionChanged(editorRef(), selection) }
                return@execute
            }
            try {
                val rows = ensureHost().call(kind, JsonObject().apply { addProperty("id", id) }, files()).result?.asJsonArray ?: return@execute
                publish { l -> if (kind == "events") l.eventsChanged(editorRef(), id, rows) else l.propertiesChanged(editorRef(), id, rows) }
            } catch (e: Throwable) { report(e) }
        }
    }

    /**
     * Several controls selected: as Visual Studio, Properties lists only what they all have, with a value left blank
     * where they differ. (An edit already goes to every selected control, in one undo step.)
     */
    fun requestSharedRows(kind: String, ids: List<String>) {
        worker.execute {
            val view = lastView ?: return@execute
            val live = ids.filter { it.isNotEmpty() && exists(view, it) }.distinct()
            if (live.size < 2) { requestRows(kind, live.lastOrNull() ?: ""); return@execute }
            try {
                val host = ensureHost()
                val f = files()
                val all = live.map { id -> host.call(kind, JsonObject().apply { addProperty("id", id) }, f).result?.asJsonArray ?: return@execute }
                val rows = sharedRows(all, if (kind == "events") "handler" else "value")
                val primary = live.last()
                publish { l -> if (kind == "events") l.eventsChanged(editorRef(), primary, rows) else l.propertiesChanged(editorRef(), primary, rows) }
            } catch (e: Throwable) { report(e) }
        }
    }

    private fun refreshInspector() {
        val id = selection.lastOrNull() ?: ""
        requestRows("properties", id)
    }

    /** The Events window: binds a handler (writing its stub if new) and jumps to it, as a double-click in VS does. */
    fun bindEventAndGo(id: String, event: String, handler: String) {
        worker.execute {
            try {
                val op = JsonObject().apply { addProperty("op", "setEvent"); addProperty("id", id); addProperty("event", event); addProperty("handler", handler) }
                val view = write(JsonArray().apply { add(op) }, "Event Handler")
                show(view, null)
                if (view.handlerIfCreated() == null && handler.isNotEmpty()) gotoHandler(handler, null)
                requestRows("events", id)
            } catch (e: Throwable) { report(e) }
        }
    }

    private fun exists(view: JsonObject, id: String): Boolean =
        listOf("items", "tray").any { k -> view.getAsJsonArray(k)?.any { it.asJsonObject.get("id")?.asString == id } == true }

    fun gotoExistingHandler(handler: String) { worker.execute { gotoHandler(handler, null) } }

    // ---- the project's own controls ---------------------------------------------------------------------

    @Volatile private var libraries = dev.penguinforms.project.ProjectLibraries(null, null)
    private var loadedSignature = ""

    /**
     * Loads the project's last build into the host, so its own controls and UserControls draw and appear in the
     * toolbox - only when Rider trusts the project, because that runs the project's code (as Visual Studio does).
     */
    private fun loadLibraries(h: HostClient): Boolean {
        libraries = dev.penguinforms.project.ProjectLibraries.of(form.code.parentFile)
        val output = libraries.output
        val signature = if (project.isTrusted() && output != null) libraries.signature() else ""
        if (signature == loadedSignature) return false
        loadedSignature = signature
        val assemblies = JsonArray().apply { if (signature.isNotEmpty()) add(output!!.path) }
        return try {
            val r = h.call("libraries", JsonObject().apply { add("assemblies", assemblies) }, files()).result?.asJsonObject
            r?.getAsJsonArray("errors")?.forEach { log.warn("Project controls: ${it.asString}") }
            true
        } catch (e: HostException) {
            log.warn("Could not load the project's controls: ${e.message}")
            false
        }
    }

    private fun toolbox(h: HostClient): JsonArray {
        val groups = if (project.isTrusted()) libraries.groups() else JsonArray()
        val raw = try { h.call("toolbox", JsonObject().apply { add("libraries", groups) }).result } catch (_: HostException) { h.call("toolbox").result }
        return ToolboxModel.vsOrder(raw)
    }

    private fun libraryNotice(): String? = when {
        !project.isTrusted() -> "This project isn't trusted, so only the built-in controls are shown: showing your own controls would run the project's code."
        else -> null
    }

    /** After a build: the project's controls are loaded again, and the toolbox shows them. */
    fun librariesChanged() {
        if (disposed) return
        worker.execute {
            try {
                val h = host?.takeIf { it.alive } ?: return@execute
                if (!loadLibraries(h)) return@execute
                reopen()
                val tb = toolbox(h)
                lastToolbox = tb
                lastNotice = libraryNotice()
                post(JsonObject().apply { addProperty("type", "toolbox"); add("toolbox", tb); lastNotice?.let { addProperty("notice", it) } })
                publish { l -> l.toolboxChanged(editorRef(), tb, lastNotice) }
            } catch (e: Throwable) { report(e) }
        }
    }

    // ---- the host -----------------------------------------------------------------------------------------

    private fun ensureHost(): HostClient {
        host?.takeIf { it.alive }?.let { return it }
        val started = HostLauncher.start(form.code.parentFile)
        if (disposed) {
            // The editor closed while the host was starting: no orphan.
            try { started.close() } finally { HostRegistry.unregister(started) }
            throw HostException("internal", "The designer was closed.")
        }
        host = started
        return started
    }

    /** What Rider holds for the form's files right now: open documents (unsaved edits included), else the disk. */
    fun files(): Map<String, String?> = ReadAction.computeBlocking<Map<String, String?>, RuntimeException> {
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
        val adding = ops.any { it.isJsonObject && it.asJsonObject.str("op") == "add" }
        if (adding) log.info("Penguin Forms add: asking the designer host")
        val response = ensureHost().call("apply", JsonObject().apply { add("ops", ops) }, files())
        val view = response.result?.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw HostException("internal", "The designer host gave no answer to the edit.")
        if (adding) log.info("Penguin Forms add: host answered; it changed ${response.writes.keys.map { File(it).name }.ifEmpty { listOf("no files") }}")
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
        val written = ArrayList<String>()
        val skipped = ArrayList<String>()
        onEdt {
            applyingOwnWrites = true
            try {
                WriteCommandAction.writeCommandAction(project)
                    .withName("Penguin Forms: $name")
                    .withUndoConfirmationPolicy(UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION)
                    .withGlobalUndo()
                    .run<RuntimeException> {
                        val allowed = form.all.map { it.canonicalPath }.toSet()
                        for ((path, raw) in writes) {
                            val text = StringUtil.convertLineSeparators(raw)
                            val file = File(path)
                            // Only the form's own three files: the host runs project code in trusted projects, so a
                            // write to any other path is refused, whatever the host says.
                            if (file.canonicalPath !in allowed) { log.warn("Penguin Forms add: refused a designer write outside the form: $path"); continue }
                            var v = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                            if (v == null) {
                                val dir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file.parentFile)
                                if (dir == null) { skipped += "${file.name} (its folder isn't visible to Rider)"; continue }
                                v = dir.createChildData(this, file.name)
                            }
                            val doc = FileDocumentManager.getInstance().getDocument(v)
                            if (doc == null) { skipped += "${file.name} (Rider has no editable document for it)"; continue }
                            if (!doc.isWritable) { skipped += "${file.name} (read-only)"; continue }
                            DocumentDiff.replace(doc, text)
                            written += file.name
                            // A fresh handler stub: indent its empty line now, in this same command, so the
                            // cursor can sit there as in VS and one Ctrl+Z takes the whole handler away.
                            if (createdHandler != null && file == form.code) {
                                val method = createdHandler.str("method")
                                val target = method?.let { HandlerLocator.bodyPosition(doc.text, it, createdHandler.get("line")?.asInt) }
                                if (target != null && target.indentToInsert.isNotEmpty()) doc.insertString(target.offset, target.indentToInsert)
                            }
                        }
                    }
            } finally {
                applyingOwnWrites = false
            }
            listenToDocuments()
            onFilesChanged()
        }
        log.info("Penguin Forms add: wrote ${written.ifEmpty { listOf("nothing") }} ($name)")
        if (skipped.isNotEmpty()) throw HostException("edit", "Penguin Forms couldn't change ${skipped.joinToString(", ")}.")
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
        lastCodeError = null
        publish { l -> l.viewChanged(editorRef(), view) }
        post(JsonObject().apply { addProperty("type", "view"); add("view", view); if (select != null && !select.isJsonNull) add("select", select) })
        view.handlerIfCreated()?.let { h -> gotoHandler(h.str("method") ?: return@let, h.get("line")?.asInt) }
    }

    @Volatile private var lastCodeError: HostException? = null

    private fun report(e: Throwable) {
        if (e is HostException && e.kind == "code") lastCodeError = e
        when {
            e is HostException && e.kind == "code" -> post(JsonObject().apply {
                addProperty("type", "parseError"); addProperty("message", e.message)
                e.line?.let { addProperty("line", it) }; e.column?.let { addProperty("column", it) }
            })
            e is HostException -> {
                log.warn("Penguin Forms designer: ${e.message}" + (e.detail?.let { "\n$it" } ?: ""))
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message) })
            }
            e is HostNotAvailable -> {
                log.warn("Penguin Forms designer: the designer host isn't available: ${e.message}")
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message) })
                post(JsonObject().apply { addProperty("type", "fwFatal"); addProperty("message", e.message); addProperty("needsDotnet", e.needsDotnet) })
                if (e.needsDotnet) DotnetNotifier.notifyMissing(project, e.message ?: "")
            }
            else -> {
                log.warn("Penguin Forms designer: ${e.message}", e)
                post(JsonObject().apply { addProperty("type", "error"); addProperty("message", e.message ?: e.toString()) })
            }
        }
    }

    private fun notify(text: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("Penguin Forms").createNotification(text, type).notify(project)
    }

    // ---- code navigation ----------------------------------------------------------------------------------

    /** F7: the form's code file, in Rider's text editor. */
    fun viewCode() {
        onEdt { CodeOpener.open(project, vf(form.code) ?: return@onEdt) }
    }

    /**
     * Jumps into a handler in `Form1.cs` with the cursor inside its body, as Visual Studio does after a
     * double-click. [line] (1-based, from the host) is the method's line when the host just wrote it.
     */
    fun gotoHandler(method: String, line: Int?) {
        onEdt {
            val v = vf(form.code) ?: return@onEdt
            CodeNavigation.expect(project, v)
            val doc = FileDocumentManager.getInstance().getDocument(v) ?: return@onEdt
            val target = HandlerLocator.bodyPosition(doc.text, method, line)
            if (target == null) {
                OpenFileDescriptor(project, v).navigate(true)
                return@onEdt
            }
            OpenFileDescriptor(project, v, target.offset).navigate(true)
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

    /** Pictures the user chose in the file chooser: the only files an image edit may read into the .resx. */
    private val pickedFiles = java.util.Collections.synchronizedSet(HashSet<String>())

    // Every edit the designer host knows, except none that reads a file the page names (setImage is checked below).
    // "verb" = a smart-tag action (Insert Standard Items, Add Tab, Dock in Parent Container…); setTreeNodes = the
    // TreeView node editor; setChildIndex = reordering in Document Outline. (These three were missing until 8 Nov,
    // so the smart-tag action links did nothing.)
    private val knownOps = setOf("setBounds", "setForm", "setProp", "resetProp", "setItems", "setEvent", "add", "remove", "setParent",
        "setImage", "setImages", "reorder", "setCell", "setStyles", "rename", "paste", "duplicate", "bringToFront", "sendToBack",
        "verb", "setTreeNodes", "setChildIndex")

    /** Edits from the page: known kinds only, and picture paths only from the file chooser. */
    private fun checkOps(ops: JsonArray) {
        for (o in ops) {
            val op = o.asJsonObject
            val kind = op.str("op")
            if (kind !in knownOps) throw HostException("edit", "Unknown designer edit '$kind'.")
            val paths = when (kind) {
                "setImage" -> listOfNotNull(op.str("value")?.takeIf { it.isNotEmpty() })
                "setImages" -> op.getAsJsonArray("ids")?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString }?.filter { !it.startsWith("#") && it.isNotEmpty() } ?: emptyList()
                else -> emptyList()
            }
            for (path in paths) if (File(path).canonicalPath !in pickedFiles) throw HostException("edit", "Choose the picture with the file chooser.")
        }
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
            paths.forEach { pickedFiles.add(File(it).canonicalPath) }
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
        if (app.isDispatchThread) block() else app.invokeAndWait(block, ModalityState.nonModal())
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

/** The rows every component in [all] has (by name and type), in the primary's (last) order; differing values blank. */
internal fun sharedRows(all: List<JsonArray>, valueKey: String): JsonArray {
    val out = JsonArray()
    if (all.isEmpty()) return out
    val others = all.dropLast(1).map { rows -> rows.associateBy { it.asJsonObject.get("name")?.asString } }
    for (el in all.last()) {
        val row = el.asJsonObject
        val name = row.get("name")?.asString ?: continue
        if (name == "Name") continue // each control has its own name; VS leaves (Name) out too
        val matches = others.map { it[name]?.asJsonObject }
        if (matches.any { it == null || it.get("type") != row.get("type") }) continue
        val copy = row.deepCopy()
        if (matches.any { it!!.get(valueKey) != row.get(valueKey) }) {
            copy.addProperty(valueKey, "")
            copy.addProperty("modified", false)
        }
        out.add(copy)
    }
    return out
}
