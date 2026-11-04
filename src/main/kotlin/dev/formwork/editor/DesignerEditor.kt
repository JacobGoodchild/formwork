package dev.formwork.editor

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
        if (JBCefApp.isSupported()) {
            val b = JBCefBrowser.createBuilder().setOffScreenRendering(false).build()
            browser = b
            Disposer.register(this, b)
            val q = JBCefJSQuery.create(b as JBCefBrowserBase)
            query = q
            Disposer.register(this, q)
            q.addHandler { text -> session.onMessage(text); null }
            b.jbCefClient.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
                override fun onLoadEnd(cefBrowser: org.cef.browser.CefBrowser?, frame: org.cef.browser.CefFrame?, httpStatusCode: Int) {
                    if (frame?.isMain != true) return
                    com.intellij.openapi.diagnostic.logger<DesignerEditor>().info("Designer page loaded (HTTP $httpStatusCode) for ${form.code}")
                    val pending = synchronized(queue) { loaded = true; queue.toList().also { queue.clear() } }
                    pending.forEach(::run)
                }
            }, b.cefBrowser)
            b.jbCefClient.addLoadHandler(object : org.cef.handler.CefLoadHandlerAdapter() {
                override fun onLoadError(cefBrowser: org.cef.browser.CefBrowser?, frame: org.cef.browser.CefFrame?, errorCode: org.cef.handler.CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                    com.intellij.openapi.diagnostic.logger<DesignerEditor>().warn("Designer page failed to load: $errorCode $errorText $failedUrl")
                }
            }, b.cefBrowser)
            b.loadHTML(WebviewPage.html("function(text){${q.inject("text")}}", ThemeCss.css(), initialWebviewState()))
            panel.add(b.component, BorderLayout.CENTER)
            val bus = ApplicationManager.getApplication().messageBus.connect(this)
            bus.subscribe(LafManagerListener.TOPIC, LafManagerListener { refreshTheme() })
            bus.subscribe(EditorColorsManager.TOPIC, EditorColorsListener { refreshTheme() })
        } else {
            browser = null
            query = null
            panel.add(JBLabel("<html><center>The Formwork designer needs Rider's built-in browser (JCEF), which isn't available in this Rider.<br>" +
                "Help → Find Action → \"Choose Boot Java Runtime for the IDE\" → pick a runtime with JCEF.</center></html>", SwingConstants.CENTER).apply {
                border = JBUI.Borders.empty(20)
            }, BorderLayout.CENTER)
        }
    }

    /** What the webview remembers between openings (VS Code's getState): Formwork starts it as VS looks at first. */
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
        val KEY = Key.create<DesignerEditor>("formwork.designer")
    }
}
