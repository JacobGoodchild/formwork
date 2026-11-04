package dev.formwork.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Opens a form's code file (`Form1.cs`, when `Form1.Designer.cs` is beside it) with a **Design** tab first,
 * as Visual Studio opens a form in the designer on double-click. Rider's own text editor stays as the second
 * tab, so every C# feature keeps working there. `Form1.Designer.cs` itself still opens as text.
 */
class DesignerEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = FormFiles.of(file) != null

    override fun acceptRequiresReadAction(): Boolean = false

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        DesignerEditor(project, file, FormFiles.of(file)!!)

    override fun getEditorTypeId(): String = TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    companion object {
        const val TYPE_ID = "formwork-designer"
    }
}
