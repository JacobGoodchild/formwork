package dev.penguinforms.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.JToggleButton
import javax.swing.ListSelectionModel
import javax.swing.SpinnerNumberModel

/**
 * The pop-up editors of Visual Studio's property grid, as Swing dialogs that follow the IDE theme: Dock (the six
 * places), ContentAlignment (a 3×3 grid), Color (Custom / Web / System tabs) and Font. Each returns the value in the
 * text form the designer host takes ("Fill", "MiddleCenter", "Red", "192, 0, 0", "Segoe UI, 12pt, style=Bold").
 */
object PopupEditors {
    /** VS's Dock editor: Top, Left, Fill, Right, Bottom around the middle, and None underneath. */
    class DockDialog(project: Project, current: String) : DialogWrapper(project, true) {
        private var picked = current
        init { title = "Dock"; init() }
        override fun createCenterPanel(): JComponent {
            val p = JPanel(GridBagLayout())
            fun b(text: String, x: Int, y: Int, w: Int = 1) {
                val button = JToggleButton(text, text == picked).apply {
                    preferredSize = Dimension(JBUI.scale(if (w == 3) 180 else 60), JBUI.scale(if (text == "Fill") 60 else 28))
                    addActionListener { picked = text; close(OK_EXIT_CODE) }
                }
                p.add(button, GridBagConstraints().apply { gridx = x; gridy = y; gridwidth = w; fill = GridBagConstraints.BOTH; insets = JBUI.insets(2) })
            }
            b("Top", 0, 0, 3); b("Left", 0, 1); b("Fill", 1, 1); b("Right", 2, 1); b("Bottom", 0, 2, 3); b("None", 0, 3, 3)
            return p
        }
        fun ask(): String? = if (showAndGet()) picked else null
    }

    /** VS's ContentAlignment editor: nine buttons, top-left to bottom-right. */
    class AlignmentDialog(project: Project, current: String) : DialogWrapper(project, true) {
        private var picked = current
        init { title = "Alignment"; init() }
        override fun createCenterPanel(): JComponent {
            val p = JPanel(GridLayout(3, 3, JBUI.scale(2), JBUI.scale(2)))
            for (v in listOf("Top", "Middle", "Bottom")) for (h in listOf("Left", "Center", "Right")) {
                val value = v + h
                p.add(JToggleButton(if (value == picked) "●" else "", value == picked).apply {
                    toolTipText = value
                    preferredSize = Dimension(JBUI.scale(48), JBUI.scale(32))
                    addActionListener { picked = value; close(OK_EXIT_CODE) }
                })
            }
            return p
        }
        fun ask(): String? = if (showAndGet()) picked else null
    }

    /** VS's Color editor: Custom (any colour), Web (named colours) and System (the theme's system colours). */
    class ColorDialog(project: Project, private val current: String, private val web: List<String>, private val system: List<String>,
                      private val swatch: (String) -> Color?) : DialogWrapper(project, true) {
        private val tabs = JBTabbedPane()
        private val webList = JBList(web)
        private val systemList = JBList(system)
        private var custom: Color? = null
        init { title = "Color"; init() }

        private fun renderer() = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean): Component {
                val c = super.getListCellRendererComponent(list, value, index, selected, focus) as DefaultListCellRenderer
                val color = swatch(value as String)
                c.icon = color?.let { col -> object : javax.swing.Icon {
                    override fun getIconWidth() = JBUI.scale(18)
                    override fun getIconHeight() = JBUI.scale(12)
                    override fun paintIcon(cc: Component?, g: java.awt.Graphics, x: Int, y: Int) {
                        g.color = col; g.fillRect(x, y, iconWidth - 2, iconHeight); g.color = Color.GRAY; g.drawRect(x, y, iconWidth - 2, iconHeight - 1)
                    }
                } }
                return c
            }
        }

        override fun createCenterPanel(): JComponent {
            val start = swatch(current) ?: Color.WHITE
            val picker = javax.swing.JColorChooser(start)
            picker.previewPanel = JPanel()
            picker.selectionModel.addChangeListener { custom = picker.color }
            for (list in listOf(webList, systemList)) {
                list.selectionMode = ListSelectionModel.SINGLE_SELECTION
                list.cellRenderer = renderer()
                list.addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) { if (e.clickCount == 2) close(OK_EXIT_CODE) }
                })
            }
            tabs.addTab("Custom", picker)
            tabs.addTab("Web", JBScrollPane(webList))
            tabs.addTab("System", JBScrollPane(systemList))
            when (current) {
                in system -> { tabs.selectedIndex = 2; systemList.setSelectedValue(current, true) }
                in web -> { tabs.selectedIndex = 1; webList.setSelectedValue(current, true) }
            }
            tabs.preferredSize = Dimension(JBUI.scale(360), JBUI.scale(380))
            return tabs
        }

        fun ask(): String? {
            if (!showAndGet()) return null
            return when (tabs.selectedIndex) {
                1 -> webList.selectedValue
                2 -> systemList.selectedValue
                else -> custom?.let { "${it.red}, ${it.green}, ${it.blue}" }
            }
        }
    }

    /** The Font dialog: family, size and style, written as WinForms' FontConverter writes it. */
    class FontDialog(project: Project, current: String, families: List<String>) : DialogWrapper(project, true) {
        private val parts = current.split(',').map { it.trim() }
        private val family = ComboBox((listOf(parts.getOrElse(0) { "Segoe UI" }) + families).distinct().toTypedArray()).apply { isEditable = true; selectedItem = parts.getOrElse(0) { "Segoe UI" } }
        private val sizeSpinner = JSpinner(SpinnerNumberModel(parts.getOrNull(1)?.removeSuffix("pt")?.toDoubleOrNull() ?: 9.0, 1.0, 200.0, 0.75))
        private val style = current.substringAfter("style=", "")
        private val bold = JBCheckBox("Bold", style.contains("Bold"))
        private val italic = JBCheckBox("Italic", style.contains("Italic"))
        private val underline = JBCheckBox("Underline", style.contains("Underline"))
        private val strikeout = JBCheckBox("Strikeout", style.contains("Strikeout"))
        init { title = "Font"; init() }
        override fun createCenterPanel(): JComponent = JPanel(GridBagLayout()).apply {
            val c = GridBagConstraints().apply { anchor = GridBagConstraints.WEST; insets = JBUI.insets(3) }
            add(JBLabel("Font:"), c.apply { gridx = 0; gridy = 0 }); add(family, c.apply { gridx = 1; fill = GridBagConstraints.HORIZONTAL })
            add(JBLabel("Size:"), c.apply { gridx = 0; gridy = 1; fill = GridBagConstraints.NONE }); add(sizeSpinner, c.apply { gridx = 1 })
            add(JPanel(GridLayout(2, 2)).apply { add(bold); add(italic); add(underline); add(strikeout) }, c.apply { gridx = 1; gridy = 2 })
        }
        fun ask(): String? {
            if (!showAndGet()) return null
            val pt = (sizeSpinner.value as Double).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
            val styles = listOfNotNull("Bold".takeIf { bold.isSelected }, "Italic".takeIf { italic.isSelected }, "Underline".takeIf { underline.isSelected }, "Strikeout".takeIf { strikeout.isSelected })
            return "${family.editor.item ?: family.selectedItem}, ${pt}pt" + if (styles.isEmpty()) "" else ", style=" + styles.joinToString(", ")
        }
    }

    /** A small "▾" / "…" button for a value cell, as VS shows on properties with a pop-up editor. */
    fun cellButton(text: String, action: () -> Unit): JButton = JButton(text).apply {
        margin = JBUI.emptyInsets(); preferredSize = Dimension(JBUI.scale(22), JBUI.scale(18)); isFocusable = false
        addActionListener { action() }
    }

    @Suppress("unused") private val keep = BorderLayout::class
}
