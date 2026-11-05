package dev.formwork.project

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import dev.formwork.editor.DesignerEditorProvider
import java.io.File

/**
 * Add → Windows Form… / User Control…: the two files Visual Studio's templates make (`Form2.cs` + `Form2.Designer.cs`),
 * with the text of Microsoft's own templates (`dotnet new winforms` / `winformscontrollib`), in the namespace of the
 * project and folder, in the style the project's code already uses. Then the designer opens.
 */
open class AddFormAction(private val userControl: Boolean, private val template: Template? = null) : DumbAwareAction() {
    class Form : AddFormAction(false)
    class UserControl : AddFormAction(true)
    class Dialog : AddFormAction(false, Template("netforms-dialog", "Dialog1", "Dialog"))
    class AboutBox : AddFormAction(false, Template("netforms-aboutbox", "AboutBox1", "AboutBox"))
    class LoginForm : AddFormAction(false, Template("netforms-login", "LoginForm1", "LoginForm"))
    class SplashScreen : AddFormAction(false, Template("netforms-splash", "SplashScreen1", "SplashScreen"))

    /** One of NetForms' item templates, shipped in the plugin (no download): its files use [source] as the class name. */
    data class Template(val dir: String, val source: String, val base: String) {
        fun file(ext: String): String = AddFormAction::class.java.getResourceAsStream("/formwork/templates/$dir/$source$ext")!!
            .use { String(it.readAllBytes(), Charsets.UTF_8) }.removePrefix("\uFEFF").replace("\r\n", "\n")
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && folder(e) != null
    }

    private fun folder(e: AnActionEvent): VirtualFile? {
        val f = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        val dir = if (f.isDirectory) f else f.parent ?: return null
        return dir.takeIf { ProjectLibraries.projectFileOf(File(it.path)) != null }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dir = folder(e) ?: return
        val base = template?.base ?: if (userControl) "UserControl" else "Form"
        val suggested = generateSequence(1) { it + 1 }.map { "$base$it" }.first { dir.findChild("$it.cs") == null }
        val name = Messages.showInputDialog(project, "Name:", if (userControl) "Add User Control" else "Add Windows Form", null, suggested, null)?.trim()?.removeSuffix(".cs") ?: return
        if (!Regex("^[A-Za-z_][A-Za-z0-9_]*$").matches(name)) { Messages.showErrorDialog(project, "'$name' isn't a valid C# class name.", "Add"); return }
        if (dir.findChild("$name.cs") != null || dir.findChild("$name.Designer.cs") != null) { Messages.showErrorDialog(project, "$name.cs already exists here.", "Add"); return }

        val csproj = ProjectLibraries.projectFileOf(File(dir.path))!!
        val text = csproj.readText()
        val root = Regex("<RootNamespace>\\s*([^<]+?)\\s*</RootNamespace>").find(text)?.groupValues?.get(1) ?: csproj.name.removeSuffix(".csproj").replace(Regex("[^A-Za-z0-9_.]"), "_")
        val relative = File(dir.path).relativeTo(csproj.parentFile).path.replace(File.separatorChar, '.').trim('.')
        val namespace = if (relative.isEmpty()) root else "$root.$relative"
        // The style of the project's own code: file-scoped `namespace X;` (the .NET 6+ templates) or a block.
        val fileScoped = csproj.parentFile.walkTopDown().maxDepth(2).filter { it.extension == "cs" }.take(20)
            .any { f -> Regex("^namespace\\s+[\\w.]+\\s*;", RegexOption.MULTILINE).containsMatchIn(f.readText()) }
        val (code, designer) = if (template != null) {
            val fill = { t: String -> t.replace("NetFormsNamespace", namespace).replace(Regex("\\b${template.source}\\b"), name) }
            fill(template.file(".cs")) to fill(template.file(".Designer.cs"))
        } else Templates.files(name, namespace, userControl, fileScoped)

        var created: VirtualFile? = null
        WriteCommandAction.writeCommandAction(project).withName(if (userControl) "Add User Control" else "Add Windows Form").withGlobalUndo().run<RuntimeException> {
            // As Visual Studio writes them: CRLF line endings, a byte-order mark on the Designer file.
            created = dir.createChildData(this, "$name.cs").also { it.setBinaryContent(code.replace("\n", "\r\n").toByteArray(Charsets.UTF_8)) }
            dir.createChildData(this, "$name.Designer.cs").setBinaryContent(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + designer.replace("\n", "\r\n").toByteArray(Charsets.UTF_8))
        }
        created?.let {
            VfsUtilCore.virtualToIoFile(it)
            FileEditorManager.getInstance(project).openFile(it, true)
            FileEditorManager.getInstance(project).setSelectedEditor(it, DesignerEditorProvider.TYPE_ID)
        }
    }

    object Templates {
        fun files(name: String, ns: String, userControl: Boolean, fileScoped: Boolean): Pair<String, String> {
            val base = if (userControl) "UserControl" else "Form"
            val region = if (userControl) "Component Designer generated code" else "Windows Form Designer generated code"
            val sep = if (userControl) ": " else " : " // Microsoft's templates really differ here.
            val codeBody = "public partial class $name$sep$base\n{\n    public $name()\n    {\n        InitializeComponent();\n    }\n}\n"
            val init = buildString {
                append("        components = new System.ComponentModel.Container();\n")
                append("        AutoScaleMode = AutoScaleMode.Font;\n")
                append("        ClientSize = new Size(800, 450);\n")
                if (!userControl) append("        Text = \"$name\";\n")
            }
            // Microsoft's two templates even differ in comment spacing; each is copied as it is.
            val c = if (userControl) "/// " else "///  "
            val modify = if (userControl) "do not modify " else "do not modify"
            val designerBody = """partial class $name
{
    /// <summary>
    ${c}Required designer variable.
    /// </summary>
    private System.ComponentModel.IContainer components = null;

    /// <summary>
    ${c}Clean up any resources being used.
    /// </summary>
    /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
    protected override void Dispose(bool disposing)
    {
        if (disposing && (components != null))
        {
            components.Dispose();
        }
        base.Dispose(disposing);
    }

    #region $region

    /// <summary>
    ${c}Required method for Designer support - $modify
    ${c}the contents of this method with the code editor.
    /// </summary>
    private void InitializeComponent()
    {
${init}    }

    #endregion
}
"""
            return if (fileScoped) "namespace $ns;\n\n$codeBody" to "namespace $ns;\n\n$designerBody"
            else "namespace $ns\n{\n${indent(codeBody)}}\n" to "namespace $ns\n{\n${indent(designerBody)}}\n"
        }

        private fun indent(s: String) = s.lines().joinToString("\n") { if (it.isEmpty()) it else "    $it" }.trimEnd() + "\n"
    }
}
