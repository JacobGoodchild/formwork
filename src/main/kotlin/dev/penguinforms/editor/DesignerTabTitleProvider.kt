package dev.penguinforms.editor

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** `Form1.cs [Design]` while the Design tab is showing, with `*` for unsaved changes - as in Visual Studio. */
class DesignerTabTitleProvider : EditorTabTitleProvider, DumbAware {
    override fun getEditorTabTitle(project: Project, file: VirtualFile): String? {
        val designer = FileEditorManager.getInstance(project).getSelectedEditor(file) as? DesignerEditor ?: return null
        return "${file.name} [Design]" + if (designer.isModified) "*" else ""
    }
}
