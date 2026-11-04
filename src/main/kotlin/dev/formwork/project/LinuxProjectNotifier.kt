package dev.formwork.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * When Rider on Linux opens a Visual Studio Windows Forms project, a small note offers "Make this project work on
 * Linux too". Nothing changes without a click; "Don't show again" is remembered per project.
 */
class LinuxProjectNotifier : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!SystemInfo.isLinux) return
        val props = PropertiesComponent.getInstance(project)
        if (props.getBoolean(DISMISSED, false)) return
        val base = project.basePath ?: return
        val projects = MakeLinuxAction.findWinFormsProjects(File(base))
        if (projects.isEmpty()) return
        val names = projects.joinToString(", ") { it.name }
        NotificationGroupManager.getInstance().getNotificationGroup("Formwork").createNotification(
            "This is a Windows Forms project",
            "$names uses Windows Forms, which only builds on Windows. Formwork can make it work on Linux too, and it will still open in Visual Studio at college.",
            NotificationType.INFORMATION,
        ).addAction(NotificationAction.createSimpleExpiring("Make it work on Linux too…") {
            val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(projects.first()) ?: return@createSimpleExpiring
            MakeLinuxAction.run(project, vf)
        }).addAction(NotificationAction.createSimpleExpiring("Don't show again") { props.setValue(DISMISSED, true) })
            .notify(project)
    }

    companion object { const val DISMISSED = "formwork.linuxNotice.dismissed" }
}
