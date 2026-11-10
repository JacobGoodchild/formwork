package dev.penguinforms.project

import dev.penguinforms.util.UiText
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import dev.penguinforms.host.DotnetLocator
import dev.penguinforms.settings.PenguinFormsSettings
import java.io.File

/**
 * File → New → Windows Forms App (Penguin Forms)…: Microsoft's own "Windows Forms App" template from the .NET SDK
 * (`dotnet new winforms`, the one Visual Studio uses, .NET 8), already made to work on Linux too. It opens in
 * Visual Studio at college exactly as if it had been made there. Nothing is downloaded to create it; the first
 * build on Linux fetches NetForms from NuGet.
 */
class NewProjectAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val name = Messages.showInputDialog(project, "Project name:", "New Windows Forms App", null, "WinFormsApp1", null)?.trim() ?: return
        if (!Regex("^[A-Za-z_][A-Za-z0-9_.]*$").matches(name)) { Messages.showErrorDialog(project, UiText.dialog("'$name' can't be a project name (letters, digits, '_' and '.')."), "New Windows Forms App"); return }
        val parent = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Where to Create $name"), project, null) ?: return
        val dir = File(parent.path, name)
        if (dir.exists()) { Messages.showErrorDialog(project, UiText.dialog("${dir.path} already exists."), "New Windows Forms App"); return }
        val configured = PenguinFormsSettings.getInstance().state.dotnetPath
        val error = ProgressManager.getInstance().runProcessWithProgressSynchronously<String?, RuntimeException>({
            val search = DotnetLocator.search(configured)
            val dotnet = search.found
                ?: return@runProcessWithProgressSynchronously "This needs the .NET 10 SDK, and Penguin Forms couldn't find a 'dotnet' program with .NET 10.\n\nWhere it looked:\n${search.describe()}"
            val out = ExecUtil.execAndGetOutput(
                GeneralCommandLine(dotnet.path, "new", "winforms", "-f", "net8.0", "-n", name, "-o", dir.path, "--no-restore")
                    .withEnvironment("DOTNET_CLI_TELEMETRY_OPTOUT", "1").withEnvironment("DOTNET_NOLOGO", "1"), 120_000)
            if (out.exitCode != 0) return@runProcessWithProgressSynchronously out.stdout + out.stderr
            val csproj = File(dir, "$name.csproj")
            when (val r = ProjectConverter.convert(csproj.readText())) {
                is ProjectConverter.Result.Converted -> { csproj.writeText(r.text); null } // a brand-new file of ours: nothing to back up
                else -> "Unexpected template: $r"
            }
        }, "Creating $name…", false, project)
        if (error != null) { Messages.showErrorDialog(project, UiText.dialog(error.take(2000)), "New Windows Forms App"); return }
        // Rider opens the .csproj as a solution; an IDE without C# support ("No processor found") gets the folder instead.
        ProjectUtil.openOrImport(File(dir, "$name.csproj").toPath(), project, false)
            ?: ProjectUtil.openOrImport(dir.toPath(), project, false)
    }
}
