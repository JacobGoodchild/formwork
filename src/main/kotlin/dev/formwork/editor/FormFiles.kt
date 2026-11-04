package dev.formwork.editor

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * The files of one form, as Visual Studio pairs them: `Form1.cs` (your code), `Form1.Designer.cs`
 * (InitializeComponent) and `Form1.resx` (pictures and other resources).
 */
data class FormFiles(val code: File, val designer: File, val resx: File) {
    val all: List<File> get() = listOf(designer, code, resx)
    val displayName: String get() = code.name

    companion object {
        /**
         * The form whose code file is [file] (`Form1.cs`), when `Form1.Designer.cs` sits next to it and holds an
         * `InitializeComponent()`. `Resources.Designer.cs` / `Settings.Designer.cs` don't, so they're never forms.
         */
        fun of(file: VirtualFile): FormFiles? {
            if (file.isDirectory || !file.isInLocalFileSystem) return null
            val name = file.name
            if (!name.endsWith(".cs", ignoreCase = true) || name.endsWith(".Designer.cs", ignoreCase = true)) return null
            val base = name.substring(0, name.length - 3)
            val designer = file.parent?.findChild("$base.Designer.cs") ?: return null
            if (!looksLikeDesignerFile(designer)) return null
            val dir = File(file.parent.path)
            return FormFiles(File(dir, name), File(dir, "$base.Designer.cs"), File(dir, "$base.resx"))
        }

        /** A quick look, no parsing: the designer file declares InitializeComponent. */
        fun looksLikeDesignerFile(designer: VirtualFile): Boolean = try {
            designer.length < 4_000_000 && VfsUtilCore.loadText(designer).contains("InitializeComponent(")
        } catch (_: Exception) {
            false
        }

        fun ofDesignerFile(file: VirtualFile): FormFiles? {
            val name = file.name
            if (!name.endsWith(".Designer.cs", ignoreCase = true)) return null
            val code = file.parent?.findChild(name.substring(0, name.length - ".Designer.cs".length) + ".cs") ?: return null
            return of(code)
        }
    }
}
