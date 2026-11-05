package dev.penguinforms.editor

import com.google.gson.JsonObject
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.DocumentReferenceProvider
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import java.beans.PropertyChangeListener
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * The **Design** tab of a form (`Form1.cs [Design]`). It shows NetForms' designer webview in Rider's built-in
 * Chromium (JCEF); [DesignerSession] connects it to the host. Implements [DocumentReferenceProvider] so that
 * Rider's Undo/Redo, run from this tab, undo the designer's edits to the form's documents.
 */
class DesignerEditor(private val project: Project, private val file: VirtualFile, val form: FormFiles) :
    UserDataHolderBase(), FileEditor, DocumentReferenceProvider {

    private val log = com.intellij.openapi.diagnostic.logger<DesignerEditor>()
    private val panel = JPanel(BorderLayout())
    private val browser: JBCefBrowser?
    private val query: JBCefJSQuery?
    val session: DesignerSession
    @Volatile private var loaded = false
    private val queue = ArrayList<String>()

    init {
        session = DesignerSession(project, form, ::post) { this }
        Disposer.register(this, session)
        session.onFilesChanged = { FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file) }
        session.editorRef = { this }
        session.publish = { event -> ActiveDesigner.get(project).publish(this, event) }
        if (JBCefApp.isSupported()) {
            val b = JBCefBrowser.createBuilder().setOffScreenRendering(false).build()
            browser = b
            Disposer.register(this, b)
            val q = JBCefJSQuery.create(b as JBCefBrowserBase)
            query = q
            Disposer.register(this, q)
            // The answer tells the page its message arrived, so it sends the next one (bridge.js sends one at a time).
            q.addHandler { text ->
                try { session.onMessage(text) } catch (e: Throwable) { log.warn("Penguin Forms add: a page message failed: ${e.message}", e) }
                JBCefJSQuery.Response("ok")
            }
            b.jbCefClient.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
                override fun onLoadEnd(cefBrowser: org.cef.browser.CefBrowser?, frame: org.cef.browser.CefFrame?, httpStatusCode: Int) {
                    if (frame?.isMain != true) return
                    com.intellij.openapi.diagnostic.logger<DesignerEditor>().info("Designer page loaded (HTTP $httpStatusCode) for ${form.code}")
                    cefBrowser?.executeJavaScript("document.body.classList.add('fw-native');", cefBrowser.url, 0)
                    // Settings → Tools → Penguin Forms: in SnapToGrid mode with "Show grid", the form shows VS's grid of dots.
                    val st = dev.penguinforms.settings.PenguinFormsSettings.getInstance().state
                    if (st.layoutMode == "SnapToGrid" && st.showGrid) cefBrowser?.executeJavaScript(
                        "document.body.classList.add('fw-grid'); document.body.style.setProperty('--fw-grid-w','${st.gridWidth}px'); document.body.style.setProperty('--fw-grid-h','${st.gridHeight}px');",
                        cefBrowser.url, 0)
                    val pending = synchronized(queue) { loaded = true; queue.toList().also { queue.clear() } }
                    pending.forEach(::run)
                }
            }, b.cefBrowser)
            b.jbCefClient.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
                override fun onLoadError(cefBrowser: org.cef.browser.CefBrowser?, frame: org.cef.browser.CefFrame?, errorCode: org.cef.handler.CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                    com.intellij.openapi.diagnostic.logger<DesignerEditor>().warn("Designer page failed to load: $errorCode $errorText $failedUrl")
                }
            }, b.cefBrowser)
            b.loadHTML(WebviewPage.html("function(text, ok, fail){${q.inject("text", "function(r){ if (ok) ok(r); }", "function(c, m){ if (fail) fail(c, m); }")}}", ThemeCss.css(), initialWebviewState()))
            panel.add(b.component, BorderLayout.CENTER)
            // Tools dragged from the native Toolbox are placed where they are dropped (as in VS).
            b.component.dropTarget = java.awt.dnd.DropTarget(b.component, java.awt.dnd.DnDConstants.ACTION_COPY, object : java.awt.dnd.DropTargetAdapter() {
                override fun dragOver(e: java.awt.dnd.DropTargetDragEvent) {
                    if (e.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) e.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY) else e.rejectDrag()
                }
                override fun drop(e: java.awt.dnd.DropTargetDropEvent) {
                    e.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                    val type = try { dev.penguinforms.toolwindows.ToolboxPanel.draggedTool(e.transferable) } catch (ex: Exception) {
                        log.warn("Penguin Forms add: could not read the dragged tool: ${ex.message}", ex); null
                    }
                    log.info("Penguin Forms add: drop of ${type ?: "(not a Toolbox tool)"} at ${e.location.x}, ${e.location.y} on ${form.displayName}")
                    if (type != null) dropTool(type, e.location.x, e.location.y)
                    e.dropComplete(type != null)
                }
            }, true)
            val bus = ApplicationManager.getApplication().messageBus.connect(this)
            // A build writes the project's bin/ folder: its controls are loaded again once it has settled.
            val projectDir = dev.penguinforms.project.ProjectLibraries.projectFileOf(form.code.parentFile)?.parentFile?.path
            if (projectDir != null) {
                val alarm = com.intellij.util.Alarm(com.intellij.util.Alarm.ThreadToUse.POOLED_THREAD, this)
                bus.subscribe(com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES, object : com.intellij.openapi.vfs.newvfs.BulkFileListener {
                    override fun after(events: List<com.intellij.openapi.vfs.newvfs.events.VFileEvent>) {
                        if (events.any { it.path.startsWith("$projectDir/bin/") && it.path.endsWith(".dll") }) {
                            alarm.cancelAllRequests()
                            alarm.addRequest({ session.librariesChanged() }, 1500)
                        }
                    }
                })
            }
            bus.subscribe(LafManagerListener.TOPIC, LafManagerListener { refreshTheme() })
            bus.subscribe(EditorColorsManager.TOPIC, EditorColorsListener { refreshTheme() })
        } else {
            browser = null
            query = null
            panel.add(JBLabel("<html><center>The Penguin Forms designer needs Rider's built-in browser (JCEF), which isn't available in this Rider.<br>" +
                "Help → Find Action → \"Choose Boot Java Runtime for the IDE\" → pick a runtime with JCEF.</center></html>", SwingConstants.CENTER).apply {
                border = JBUI.Borders.empty(20)
            }, BorderLayout.CENTER)
        }
    }

    /** What the webview remembers between openings (VS Code's getState): Penguin Forms starts it as VS looks at first. */
    private fun initialWebviewState(): String {
        val saved = DesignerStateStore.get(form.code.path)
        if (saved != null) return saved
        val collapsed = com.google.gson.JsonArray().apply { ToolboxModel.COLLAPSED_AT_FIRST.forEach { add(it) } }
        return com.google.gson.JsonObject().apply { add("toolboxCollapsed", collapsed); addProperty("tab", "properties") }.toString()
    }

    /** Sends a message to the webview (as VS Code's `webview.postMessage`). */
    fun post(message: JsonObject) = run("window.postMessage(${message}, '*');")

    private fun run(js: String) {
        val b = browser ?: return
        synchronized(queue) { if (!loaded) { queue.add(js); return } }
        b.cefBrowser.executeJavaScript(js, b.cefBrowser.url, 0)
    }

    private fun refreshTheme() {
        val css = com.google.gson.JsonPrimitive(ThemeCss.css()).toString()
        run("(function(){var s=document.getElementById('fw-theme'); if(s){s.textContent=$css;} document.body.classList.toggle('fw-dark', ${ThemeCss.isDark()});})();")
    }

    /** For UI tests: runs JavaScript in the page. */
    fun executeJavaScript(js: String) = run(js)

    // ---- driven by the native tool windows ------------------------------------------------------------------

    private fun jsString(s: String?) = if (s == null) "null" else com.google.gson.JsonPrimitive(s).toString()
    fun armTool(type: String?) {
        log.info("Penguin Forms add: Toolbox picked ${type ?: "Pointer"} for ${form.displayName}")
        session.toolPickedInIde(type)
        run("window.NetFormsDesigner ? NetFormsDesigner.arm(${jsString(type)}) : window.__penguinFormsSend({type:'__fwLog', level:'warn', message:'Penguin Forms add: the designer page is not ready to take a tool'});")
    }
    fun addToSelection(type: String) {
        log.info("Penguin Forms add: Toolbox double-click on $type for ${form.displayName}")
        run("window.NetFormsDesigner && NetFormsDesigner.addToSelection(${jsString(type)});")
    }
    fun select(ids: List<String>) = run("window.NetFormsDesigner && NetFormsDesigner.select(${com.google.gson.Gson().toJson(ids)});")
    /** A tool dropped from the native Toolbox at a point of this editor's component. */
    fun dropTool(type: String, x: Int, y: Int) = run(
        "(function(){ var r = window.NetFormsDesigner ? NetFormsDesigner.dropAt(${jsString(type)}, $x, $y) : 'no page';" +
        " if (r !== true) window.__penguinFormsSend({type:'__fwLog', level:'warn', message:'Penguin Forms add: drop of ' + ${jsString(type)} + ' at $x, $y was not placed: ' +" +
        " (r === false ? 'the point is outside the form area (page size ' + window.innerWidth + 'x' + window.innerHeight + ', scale ' + window.devicePixelRatio + ')' : r)}); })();")

    /** A command of the Format menu (see FormatAction). */
    fun format(how: String, grid: Int) = run("window.NetFormsDesigner && NetFormsDesigner.format(${jsString(how)}, $grid);")

    /** The native tool windows replace the page's own Toolbox and Properties panes. */
    fun useNativePanes(on: Boolean) = run("document.body.classList.toggle('fw-native', $on);")
    val browserComponent: JComponent? get() = browser?.component

    override fun getDocumentReferences(): Collection<DocumentReference> {
        val manager = DocumentReferenceManager.getInstance()
        return form.all.mapNotNull { LocalFileSystem.getInstance().findFileByIoFile(it) }.map { manager.create(it) }
    }

    /** Unsaved changes in any of the form's files (the `*` in the tab title). */
    override fun isModified(): Boolean {
        val fdm = FileDocumentManager.getInstance()
        return form.all.any { f -> LocalFileSystem.getInstance().findFileByIoFile(f)?.let { v -> fdm.getCachedDocument(v)?.let(fdm::isDocumentUnsaved) } == true }
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent? = browser?.component ?: panel
    override fun getName(): String = "Design"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) {}
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? = null
    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null
    override fun dispose() {}

    companion object {
        val KEY = Key.create<DesignerEditor>("penguinforms.designer")
    }
}
