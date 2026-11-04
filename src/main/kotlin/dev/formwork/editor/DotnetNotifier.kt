package dev.formwork.editor

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

/** A plain-English note when the .NET 10 SDK is missing, with a link to the setup steps. Once per session. */
object DotnetNotifier {
    const val SETUP_URL = "https://github.com/jacobgoodchild/formwork/blob/main/TESTING-FOR-JACOB.md#1-install-net-10"
    private val shown = AtomicBoolean(false)

    fun notifyMissing(project: Project, detail: String) {
        if (!shown.compareAndSet(false, true)) return
        NotificationGroupManager.getInstance().getNotificationGroup("Formwork")
            .createNotification(
                "Formwork needs .NET 10",
                "$detail<br>The form designer runs on .NET 10, which isn't installed yet. It takes about five minutes to set up.",
                NotificationType.WARNING,
            )
            .addAction(NotificationAction.createSimpleExpiring("Show me how") { BrowserUtil.browse(SETUP_URL) })
            .notify(project)
    }
}
