package dev.formwork.editor

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.UIManager

/**
 * NetForms' webview is styled with VS Code's `--vscode-*` colour variables. Formwork fills them from Rider's
 * current theme, so the designer chrome follows Rider (light, dark, any theme) and changes live with it.
 * Only the form itself stays light, painted by NetForms as Windows does, exactly as Visual Studio shows it.
 */
object ThemeCss {
    private fun hex(c: Color?): String = c?.let { String.format("#%02x%02x%02x", it.red, it.green, it.blue) } ?: "#808080"
    private fun ui(key: String, fallback: Color): Color = UIManager.getColor(key) ?: fallback

    fun isDark(): Boolean = !JBColor.isBright()

    fun variables(): Map<String, String> {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val editorBg = scheme.defaultBackground
        val panel = UIUtil.getPanelBackground()
        val fg = UIUtil.getLabelForeground()
        val border = JBColor.border()
        val selBg = UIUtil.getListSelectionBackground(true)
        val selFg = UIUtil.getListSelectionForeground(true)
        val inactiveSel = UIUtil.getListSelectionBackground(false)
        val hover = JBUI.CurrentTheme.List.Hover.background(true)
        val input = UIUtil.getTextFieldBackground()
        val popup = JBUI.CurrentTheme.Popup.BACKGROUND
        val font = UIUtil.getLabelFont()
        val link = JBUI.CurrentTheme.Link.Foreground.ENABLED
        val error = NamedColorUtil.getErrorForeground()
        val buttonBg = ui("Button.startBackground", ui("Button.background", panel))
        val toolbarHover = JBUI.CurrentTheme.ActionButton.hoverBackground()
        val toolbarPressed = JBUI.CurrentTheme.ActionButton.pressedBackground()
        val focus = JBUI.CurrentTheme.Focus.focusColor()
        val info = JBUI.CurrentTheme.Link.Foreground.ENABLED
        return linkedMapOf(
            "--vscode-font-family" to "'${font.family}', system-ui, sans-serif",
            "--vscode-font-size" to "${font.size}px",
            "--vscode-foreground" to hex(fg),
            "--vscode-editor-background" to hex(editorBg),
            "--vscode-sideBar-background" to hex(panel),
            "--vscode-sideBarSectionHeader-background" to hex(panel),
            "--vscode-editorGroupHeader-tabsBackground" to hex(panel),
            "--vscode-panel-border" to hex(border),
            "--vscode-widget-border" to hex(border),
            "--vscode-editorWidget-border" to hex(border),
            "--vscode-editorWidget-background" to hex(popup),
            "--vscode-editorWidget-foreground" to hex(fg),
            "--vscode-widget-shadow" to if (isDark()) "rgba(0,0,0,0.5)" else "rgba(0,0,0,0.2)",
            "--vscode-input-background" to hex(input),
            "--vscode-input-border" to hex(border),
            "--vscode-focusBorder" to hex(focus),
            "--vscode-list-activeSelectionBackground" to hex(selBg),
            "--vscode-list-activeSelectionForeground" to hex(selFg),
            "--vscode-list-inactiveSelectionBackground" to hex(inactiveSel),
            "--vscode-list-hoverBackground" to hex(hover),
            "--vscode-menu-background" to hex(popup),
            "--vscode-menu-foreground" to hex(fg),
            "--vscode-menu-border" to hex(border),
            "--vscode-menu-selectionBackground" to hex(selBg),
            "--vscode-menu-selectionForeground" to hex(selFg),
            "--vscode-menu-separatorBackground" to hex(border),
            "--vscode-button-border" to hex(border),
            "--vscode-button-secondaryBackground" to hex(buttonBg),
            "--vscode-button-secondaryForeground" to hex(fg),
            "--vscode-button-secondaryHoverBackground" to hex(toolbarHover),
            "--vscode-toolbar-hoverBackground" to hex(toolbarHover),
            "--vscode-toolbar-activeBackground" to hex(toolbarPressed),
            "--vscode-notifications-background" to hex(popup),
            "--vscode-notifications-border" to hex(border),
            "--vscode-errorForeground" to hex(error),
            "--vscode-editorInfo-foreground" to hex(info),
            "--vscode-textLink-foreground" to hex(link),
            "--vscode-textBlockQuote-background" to hex(panel),
            "--vscode-textCodeBlock-background" to hex(panel),
        )
    }

    fun css(): String = buildString {
        append(":root{")
        for ((k, v) in variables()) append(k).append(':').append(v).append(';')
        append("color-scheme:").append(if (isDark()) "dark" else "light").append(";}")
    }
}
