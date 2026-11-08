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

    // A Design tab is saved with the IDE's open tabs as penguinforms-design:///…/Form1.cs and found again by
    // [FormDesignFileSystem] when the project reopens, so it comes back like any other tab.
    override fun getFileSystem(): com.intellij.openapi.vfs.VirtualFileSystem = FormDesignFileSystem.getInstance()
    override fun getPath(): String = code.path

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

/**
 * Finds a form's Design tab file by its code file's path (`penguinforms-design:///home/…/Form1.cs`), so the IDE can
 * reopen Design tabs after a restart. Nothing is stored here and nothing is ever written: it only maps paths.
 */
class FormDesignFileSystem : com.intellij.openapi.vfs.DeprecatedVirtualFileSystem() {
    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? {
        if (!path.endsWith(".cs", ignoreCase = true) || path.endsWith(".Designer.cs", ignoreCase = true)) return null
        val code = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return FormDesignFile.of(code)
    }

    override fun refresh(asynchronous: Boolean) {}
    override fun refreshAndFindFileByPath(path: String): VirtualFile? = findFileByPath(path)

    companion object {
        const val PROTOCOL = "penguinforms-design"
        fun getInstance(): FormDesignFileSystem =
            com.intellij.openapi.vfs.VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as FormDesignFileSystem
    }
}

/**
 * Marks a project as opened (after the IDE has put back the tabs it had), so [DesignerTabListener] only swaps code tabs
 * the user opens, not `Form1.cs` code tabs the IDE restores.
 */
class DesignerStartup : com.intellij.openapi.startup.ProjectActivity {
    override suspend fun execute(project: com.intellij.openapi.project.Project) {
        project.putUserData(OPENED, true)
    }

    companion object {
        val OPENED = Key.create<Boolean>("penguinforms.projectOpened")
    }
}
