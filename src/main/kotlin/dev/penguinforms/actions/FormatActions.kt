package dev.penguinforms.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import dev.penguinforms.editor.DesignerEditor
import dev.penguinforms.settings.PenguinFormsSettings

/**
 * One command of Visual Studio's Format menu (Align Lefts, Make Same Width, Increase Horizontal Spacing, Center
 * Horizontally, Bring to Front, Lock Controls…) and View → Tab Order. Registered once per command in plugin.xml,
 * with the command as the action id's suffix: `PenguinForms.Format.left`.
 */
class FormatAction : DumbAwareAction() {
    private val how: String get() = (templatePresentation.getClientProperty(HOW) ?: "")

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val designer = e.getData(PlatformDataKeys.FILE_EDITOR) as? DesignerEditor
        e.presentation.isVisible = true
        e.presentation.isEnabled = designer != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val designer = e.getData(PlatformDataKeys.FILE_EDITOR) as? DesignerEditor ?: return
        val id = com.intellij.openapi.actionSystem.ActionManager.getInstance().getId(this) ?: return
        val grid = PenguinFormsSettings.getInstance().state.gridWidth.coerceAtLeast(2)
        designer.format(id.removePrefix("PenguinForms.Format."), grid)
    }

    companion object {
        val HOW = com.intellij.openapi.util.Key.create<String>("penguinforms.format")
    }
}
