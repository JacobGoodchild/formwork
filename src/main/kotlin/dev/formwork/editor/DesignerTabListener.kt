package dev.formwork.editor

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Collections

/**
 * Visual Studio behaviour for form tabs:
 * - opening a form (double-click in the Solution Explorer) always shows the **Design** tab, even though the
 *   platform would reopen whichever sub-tab was used last;
 * - switching between Design and Text changes the tab title (`Form1.cs [Design]` ↔ `Form1.cs`).
 */
class DesignerTabListener(private val project: Project) : FileEditorManagerListener {
    private var shownToolWindows = false

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val active = ActiveDesigner.get(project)
        if (active.editor?.file == file) active.setActive(source.selectedEditors.filterIsInstance<DesignerEditor>().firstOrNull())
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (CodeNavigation.consume(file)) return
        if (source.getEditors(file).none { it is DesignerEditor }) return
        if (source.getSelectedEditor(file) !is DesignerEditor) source.setSelectedEditor(file, DesignerEditorProvider.TYPE_ID)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        // The Toolbox and Properties follow the designer in front (they stay on the last one when code is shown,
        // as Visual Studio keeps them on the last designer).
        (event.newEditor as? DesignerEditor)?.let {
            ActiveDesigner.get(project).setActive(it)
            // The first designer of the session opens the Toolbox and Properties, as Visual Studio does.
            if (!shownToolWindows) {
                shownToolWindows = true
                val manager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                for (id in listOf("Toolbox", "Properties")) manager.getToolWindow(id)?.show()
            }
        }
        val file = event.newFile ?: event.oldFile ?: return
        if (event.newEditor is DesignerEditor || event.oldEditor is DesignerEditor) {
            FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file)
        }
    }
}

/** Formwork's own jumps to code (F7, a handler) mark the file, so opening it doesn't switch to Design. */
object CodeNavigation {
    private val files: MutableSet<String> = Collections.synchronizedSet(HashSet())
    fun expect(file: VirtualFile) { files.add(file.path) }
    fun consume(file: VirtualFile): Boolean = files.remove(file.path)
}

/** F7 (View Code): the form's code file in Rider's text editor, where its cursor last was. */
object CodeOpener {
    fun open(project: Project, file: VirtualFile) {
        CodeNavigation.expect(file)
        val text = FileEditorManager.getInstance(project).getEditors(file).filterIsInstance<com.intellij.openapi.fileEditor.TextEditor>().firstOrNull()
        // With a position, the platform selects the text editor; without one it would stay on the Design tab.
        val offset = text?.editor?.caretModel?.offset ?: 0
        com.intellij.openapi.fileEditor.OpenFileDescriptor(project, file, offset).navigate(true)
    }
}
