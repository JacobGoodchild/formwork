package dev.formwork.editor

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList

/** What the native tool windows (Toolbox, Properties, Document Outline) show; all calls arrive on the EDT. */
interface DesignerListener {
    /** The designer the user is working in changed (null: no form designer is showing). */
    fun activeChanged(editor: DesignerEditor?) {}
    fun viewChanged(editor: DesignerEditor, view: JsonObject) {}
    fun selectionChanged(editor: DesignerEditor, ids: List<String>) {}
    fun propertiesChanged(editor: DesignerEditor, id: String, rows: JsonArray) {}
    fun eventsChanged(editor: DesignerEditor, id: String, rows: JsonArray) {}
    fun toolboxChanged(editor: DesignerEditor, categories: JsonArray, notice: String?) {}
    /** The armed toolbox tool (null: the Pointer), e.g. back to Pointer after placing a control. */
    fun toolChanged(editor: DesignerEditor, type: String?) {}
}

/**
 * The form designer that has the user's attention, like Visual Studio's "active designer": its Toolbox,
 * Properties and Document Outline follow whichever `Form1.cs [Design]` tab was selected last.
 */
@Service(Service.Level.PROJECT)
class ActiveDesigner(private val project: Project) {
    private val listeners = CopyOnWriteArrayList<DesignerListener>()

    var editor: DesignerEditor? = null
        private set

    fun addListener(listener: DesignerListener, parent: Disposable) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    fun setActive(e: DesignerEditor?) {
        if (editor === e) return
        editor = e
        fire { it.activeChanged(e) }
    }

    /** Only the active designer's news reaches the tool windows. */
    fun publish(from: DesignerEditor, event: (DesignerListener) -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            if (editor === from) listeners.forEach(event)
        }
    }

    private fun fire(event: (DesignerListener) -> Unit) {
        val app = ApplicationManager.getApplication()
        if (app.isDispatchThread) listeners.forEach(event) else app.invokeLater { listeners.forEach(event) }
    }

    companion object {
        fun get(project: Project): ActiveDesigner = project.getService(ActiveDesigner::class.java)
    }
}
