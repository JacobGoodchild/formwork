package dev.penguinforms.toolwindows

import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.NamedColorUtil
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * A tool window's content, or in its place a grey, centred hint that wraps to the window's width
 * ("Open a form in the designer to use the Toolbox."). A tree's own empty text is one line and was cut off.
 */
class HintCard(content: JComponent) : JPanel(CardLayout()) {
    private val label = JBLabel().apply {
        setAllowAutoWrapping(true)
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.TOP
        foreground = NamedColorUtil.getInactiveTextColor()
        border = JBUI.Borders.empty(16, 12)
    }

    init {
        isOpaque = false
        add(content, CONTENT)
        add(label, HINT)
    }

    /** Shows [hint] instead of the content, or the content again when null. */
    fun showHint(hint: String?) {
        if (hint != null) label.text = "<html><div style='text-align:center'>${StringUtil.escapeXmlEntities(hint)}</div></html>"
        (layout as CardLayout).show(this, if (hint == null) CONTENT else HINT)
    }

    private companion object { const val CONTENT = "content"; const val HINT = "hint" }
}
