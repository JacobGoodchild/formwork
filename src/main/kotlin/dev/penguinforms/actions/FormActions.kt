package dev.penguinforms.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import dev.penguinforms.editor.DesignerEditor
import dev.penguinforms.editor.DesignerEditorProvider
import dev.penguinforms.editor.FormFiles

/** The form a context points at: a Form1.cs / Form1.Designer.cs file in an editor or in the Solution Explorer. */
private fun formOf(e: AnActionEvent): Pair<VirtualFile, FormFiles>? {
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    FormFiles.of(file)?.let { return file to it }
    val form = FormFiles.ofDesignerFile(file) ?: return null
    val code = LocalFileSystem.getInstance().findFileByIoFile(form.code) ?: return null
    return code to form
}

/** F7 - View Code: the form's code (`Form1.cs`) in Rider's text editor. */
class ViewCodeAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && formOf(e) != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (code, _) = formOf(e) ?: return
        val designer = e.getData(PlatformDataKeys.FILE_EDITOR) as? DesignerEditor
        if (designer != null) { designer.session.viewCode(); return }
        dev.penguinforms.editor.CodeOpener.open(project, code)
    }
}

/** Shift+F7 - View Designer: the form's Design tab. */
class ViewDesignerAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && formOf(e) != null
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (code, _) = formOf(e) ?: return
        val manager = FileEditorManager.getInstance(project)
        manager.openFile(code, true)
        manager.setSelectedEditor(code, DesignerEditorProvider.TYPE_ID)
    }
}
