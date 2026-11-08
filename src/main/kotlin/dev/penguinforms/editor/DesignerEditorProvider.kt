package dev.penguinforms.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * The form designer. By default (as Visual Studio) it is its own editor tab, `Form1.cs [Design]`, opened for a
 * [FormDesignFile]; the code stays in Rider's own C# editor in a separate `Form1.cs` tab ([DesignerTabListener] opens the
 * Design tab when a form's code file is opened). With "separate tabs" off in the settings, the designer is a **Design**
 * sub-tab placed before Rider's text editor in the `Form1.cs` tab instead. `Form1.Designer.cs` always opens as text.
 */
class DesignerEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean =
        if (file is FormDesignFile) FormFiles.of(file.code) != null else !FormDesignFile.separateTabs && FormFiles.of(file) != null

    override fun acceptRequiresReadAction(): Boolean = false

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        DesignerEditor(project, file, FormFiles.of(FormDesignFile.codeOf(file))!!)

    override fun getEditorTypeId(): String = TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    companion object {
        const val TYPE_ID = "penguinforms-designer"
    }
}
