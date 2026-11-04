package dev.formwork.project

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * "Make This Project Work on Linux Too…": shows what will change in the project file, keeps the original as
 * `<name>.csproj.formwork.bak`, then rewrites it through Rider's document (so Ctrl+Z puts it back).
 */
class MakeLinuxAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val csproj = projectFileFor(project, e.getData(CommonDataKeys.VIRTUAL_FILE)) ?: run {
            Messages.showInfoMessage(project, "Select the project's .csproj file (or a file in the project) in the Solution Explorer, then try again.", TITLE)
            return
        }
        run(project, csproj)
    }

    companion object {
        const val TITLE = "Make This Project Work on Linux Too"

        fun projectFileFor(project: Project, selected: VirtualFile?): VirtualFile? {
            if (selected != null && selected.name.endsWith(".csproj", ignoreCase = true)) return selected
            val start = selected?.let { if (it.isDirectory) it else it.parent }?.path ?: project.basePath ?: return null
            val file = ProjectLibraries.projectFileOf(File(start)) ?: findWinFormsProjects(File(project.basePath ?: return null)).firstOrNull() ?: return null
            return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        }

        /** Projects under [root] (a few levels deep) that use Windows Forms and don't reference NetForms yet. */
        fun findWinFormsProjects(root: File): List<File> =
            root.walkTopDown().maxDepth(4).onEnter { it.name !in setOf("bin", "obj", ".git", ".idea", "node_modules") }
                .filter { it.isFile && it.name.endsWith(".csproj", ignoreCase = true) }
                .filter { f -> val t = try { f.readText() } catch (_: Exception) { "" }; ProjectConverter.usesWindowsForms(t) && !ProjectConverter.alreadyWorksOnLinux(t) }
                .toList()

        fun run(project: Project, csproj: VirtualFile) {
            val document = FileDocumentManager.getInstance().getDocument(csproj) ?: return
            val before = document.text
            when (val result = ProjectConverter.convert(before)) {
                is ProjectConverter.Result.AlreadyDone -> Messages.showInfoMessage(project, result.reason, TITLE)
                is ProjectConverter.Result.NotPossible -> Messages.showWarningDialog(project, result.reason, TITLE)
                is ProjectConverter.Result.OldFormat -> Messages.showWarningDialog(
                    project,
                    "This is an old-style .NET Framework project (its first line mentions ToolsVersion). Formwork can't convert these safely yet.\n\n" +
                        "What you can do: at college, open it in Visual Studio and use \"Upgrade\" (or make a new Windows Forms App (.NET 8) project and copy your forms into it). Then bring that project home.",
                    TITLE,
                )
                is ProjectConverter.Result.Converted -> {
                    val after = StringUtil.convertLineSeparators(result.text)
                    if (!PreviewDialog(project, csproj, before, after, result).showAndGet()) return
                    apply(project, csproj, document, after)
                }
            }
        }

        private fun apply(project: Project, csproj: VirtualFile, document: com.intellij.openapi.editor.Document, after: String) {
            val dir = csproj.parent
            val backupName = generateSequence(0) { it + 1 }.map { if (it == 0) "${csproj.name}.formwork.bak" else "${csproj.name}.formwork.bak$it" }
                .first { dir.findChild(it) == null }
            WriteCommandAction.writeCommandAction(project).withName(TITLE).withGlobalUndo().run<RuntimeException> {
                // The original, byte for byte, before anything changes.
                val backup = dir.createChildData(this, backupName)
                backup.setBinaryContent(csproj.contentsToByteArray())
                document.setText(after)
            }
            FileDocumentManager.getInstance().saveDocument(document)
            NotificationGroupManager.getInstance().getNotificationGroup("Formwork").createNotification(
                "Done: ${csproj.name} now works on Linux too",
                "The original is saved as $backupName. Rider reloads the project; the first build downloads NetForms from NuGet. At college it still opens in Visual Studio as before.",
                NotificationType.INFORMATION,
            ).notify(project)
        }
    }

    /** The before/after preview, with a plain-English summary on top. */
    private class PreviewDialog(
        private val project: Project, private val csproj: VirtualFile,
        private val before: String, private val after: String, private val result: ProjectConverter.Result.Converted,
    ) : DialogWrapper(project, true) {
        init {
            title = TITLE
            setOKButtonText("Make It Work on Linux")
            init()
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout(0, JBUI.scale(8)))
            val notes = (result.notes + result.warnings.map { "Note: $it" } + listOf("The original file is kept next to it as ${csproj.name}.formwork.bak, and Ctrl+Z undoes the change."))
            panel.add(JBLabel("<html><b>Formwork will change ${csproj.name} like this (left: now, right: after):</b><ul>" + notes.joinToString("") { "<li>${StringUtil.escapeXmlEntities(it)}</li>" } + "</ul></html>"), BorderLayout.NORTH)
            val factory = DiffContentFactory.getInstance()
            val request = SimpleDiffRequest("${csproj.name}", factory.create(project, before, FileTypes.PLAIN_TEXT), factory.create(project, after, FileTypes.PLAIN_TEXT), "Now", "After")
            val diff = DiffManager.getInstance().createRequestPanel(project, disposable, window)
            diff.setRequest(request)
            diff.component.preferredSize = Dimension(JBUI.scale(900), JBUI.scale(480))
            panel.add(diff.component, BorderLayout.CENTER)
            return panel
        }
    }
}
