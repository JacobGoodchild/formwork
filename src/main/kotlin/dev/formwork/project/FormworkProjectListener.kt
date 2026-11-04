package dev.formwork.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseListener
import dev.formwork.host.HostRegistry

/** When the last project closes, no designer host may be left running. */
class FormworkProjectListener : ProjectCloseListener {
    override fun projectClosed(project: Project) {
        // Each editor's session closes its own host when the editor is disposed; this catches any left over.
        if (com.intellij.openapi.project.ProjectManager.getInstance().openProjects.isEmpty()) HostRegistry.killAll()
    }
}
