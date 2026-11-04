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
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (CodeNavigation.consume(file)) return
        if (source.getEditors(file).none { it is DesignerEditor }) return
        if (source.getSelectedEditor(file) !is DesignerEditor) source.setSelectedEditor(file, DesignerEditorProvider.TYPE_ID)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
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
