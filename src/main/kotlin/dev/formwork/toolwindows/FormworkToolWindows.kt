package dev.formwork.toolwindows

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Toolbox (left, like VS). */
class ToolboxToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ToolboxPanel(project, toolWindow.disposable)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
        // VS's Toolbox is narrow (about 200 px): leave the room to the form.
        narrow(toolWindow, 210)
    }
}

/** Properties (right side; VS docks it under the Solution Explorer). */
class PropertiesToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PropertiesPanel(project, toolWindow.disposable)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
        narrow(toolWindow, 300)
    }
}

/** Document Outline (left, below the Toolbox). */
class OutlineToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = OutlinePanel(project, toolWindow.disposable)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
    }
}

/** Makes a tool window about [width] px wide the first time it is laid out (the user can drag it wider). */
internal fun narrow(toolWindow: ToolWindow, width: Int) {
    val ex = toolWindow as? com.intellij.openapi.wm.ex.ToolWindowEx ?: return
    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
        val now = toolWindow.component.width
        val target = com.intellij.util.ui.JBUI.scale(width)
        if (now > target) ex.stretchWidth(target - now)
    }
}
