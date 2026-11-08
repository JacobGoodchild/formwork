package dev.penguinforms.editor

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import dev.penguinforms.settings.PenguinFormsSettings
import javax.swing.Icon

/**
 * The `Form1.cs [Design]` tab, as its own editor tab next to `Form1.cs` (Visual Studio shows the designer and the code
 * as two tabs). The tab needs a file of its own, so this stands for the form: it holds no text and is never saved;
 * the designer reads and writes the form's real files ([FormFiles]) through Rider's documents, as before.
 * One per code file, so opening the designer twice brings the same tab to the front.
 */
class FormDesignFile private constructor(val code: VirtualFile) :
    LightVirtualFile("${code.name} [Design]", FormDesignFileType, "") {

    init {
        isWritable = false
    }

    override fun isValid(): Boolean = code.isValid
    override fun getPresentableName(): String = name

    companion object {
        private val KEY = Key.create<FormDesignFile>("penguinforms.designFile")

        /** The Design tab's file for a form's code file (`Form1.cs`). */
        fun of(code: VirtualFile): FormDesignFile =
            code.getUserData(KEY)?.takeIf { it.code == code } ?: FormDesignFile(code).also { code.putUserData(KEY, it) }

        /** Design and code as two tabs (Visual Studio's way, the default) or as Design/Text sub-tabs of one tab. */
        val separateTabs: Boolean get() = PenguinFormsSettings.getInstance().state.separateDesignTab

        /** The form's code file behind [file]: itself, or the code file of a Design tab. */
        fun codeOf(file: VirtualFile): VirtualFile = (file as? FormDesignFile)?.code ?: file
    }
}

/** The (never saved) file type of [FormDesignFile]; binary, so no text editor is offered for it. */
object FormDesignFileType : FileType {
    override fun getName(): String = "PenguinFormsDesign"
    override fun getDescription(): String = "Windows Form (design view)"
    override fun getDefaultExtension(): String = ""
    override fun getIcon(): Icon = AllIcons.FileTypes.UiForm
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
}
