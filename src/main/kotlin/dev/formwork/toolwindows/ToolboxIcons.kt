package dev.formwork.toolwindows

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon

/**
 * Small toolbox pictures, one per Windows Forms control, drawn by Formwork (not copied from Visual Studio):
 * simple outlines in the theme's text colour with a blue accent, so they read well in light and dark themes.
 * NetForms ships no bitmaps for its built-in controls; a project's own controls keep the icon NetForms reads
 * from their [ToolboxBitmap].
 */
object ToolboxIcons {
    private val accent = JBColor(Color(0x2F6FD0), Color(0x5C9DFF))
    private val green = JBColor(Color(0x3C9A3C), Color(0x6CC46C))
    private val fill = JBColor(Color(0xFFFFFF), Color(0x3C3F41))

    private class Drawn(private val paint: Graphics2D.() -> Unit) : Icon {
        override fun getIconWidth() = JBUI.scale(16)
        override fun getIconHeight() = JBUI.scale(16)
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.translate(x, y)
                val s = JBUI.scale(16) / 16.0
                g2.scale(s, s)
                g2.stroke = BasicStroke(1f)
                g2.color = UIUtil.getLabelForeground()
                g2.paint()
            } finally { g2.dispose() }
        }
    }

    private fun Graphics2D.box(x: Double, y: Double, w: Double, h: Double, filled: Boolean = true) {
        val r = Rectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1)
        if (filled) { val c = color; color = fill; fill(r); color = c }
        draw(r)
    }
    private fun Graphics2D.line(x1: Double, y1: Double, x2: Double, y2: Double) = draw(Line2D.Double(x1, y1, x2, y2))
    private fun Graphics2D.text(s: String, x: Float, y: Float, size: Float = 7f, bold: Boolean = false) {
        font = Font(Font.SANS_SERIF, if (bold) Font.BOLD else Font.PLAIN, 1).deriveFont(size)
        drawString(s, x, y)
    }
    private fun Graphics2D.accent(block: Graphics2D.() -> Unit) { val c = color; color = accent; block(); color = c }
    private fun Graphics2D.down(x: Double, y: Double) {
        fill(Path2D.Double().apply { moveTo(x, y); lineTo(x + 4, y); lineTo(x + 2, y + 2.5); closePath() })
    }

    private val icons: Map<String, Icon> = mapOf(
        "Button" to Drawn { draw(RoundRectangle2D.Double(1.5, 4.5, 13.0, 8.0, 3.0, 3.0)); text("ab", 4f, 11f) },
        "CheckBox" to Drawn { box(2.0, 3.0, 11.0, 11.0); accent { stroke = BasicStroke(1.6f); draw(Path2D.Double().apply { moveTo(4.5, 8.5); lineTo(7.0, 11.0); lineTo(11.0, 5.5) }) } },
        "CheckedListBox" to Drawn { box(1.0, 1.0, 14.0, 14.0); for (i in 0..2) { box(3.0, 3.0 + i * 4, 3.0, 3.0, false); line(8.0, 4.5 + i * 4, 13.0, 4.5 + i * 4) }; accent { line(3.5, 4.5, 5.5, 3.5) } },
        "ComboBox" to Drawn { box(1.0, 4.0, 14.0, 8.0); line(10.5, 4.5, 10.5, 11.5); accent { down(11.0, 7.0) }; line(3.0, 8.0, 8.0, 8.0) },
        "DateTimePicker" to Drawn { box(1.0, 4.0, 14.0, 8.0); line(10.5, 4.5, 10.5, 11.5); accent { down(11.0, 7.0) }; text("12", 2.5f, 10.5f, 6f) },
        "Label" to Drawn { text("A", 3f, 13f, 12f, bold = true) },
        "LinkLabel" to Drawn { accent { text("A", 3f, 12f, 11f, bold = true); line(2.0, 14.0, 12.0, 14.0) } },
        "ListBox" to Drawn { box(1.0, 1.0, 14.0, 14.0); for (i in 0..2) line(3.0, 4.5 + i * 3.5, 12.0, 4.5 + i * 3.5); accent { fill(Rectangle2D.Double(2.5, 7.0, 10.0, 2.5)) } },
        "ListView" to Drawn { for (i in 0..1) for (j in 0..1) { accent { fill(Rectangle2D.Double(2.0 + i * 7, 2.0 + j * 7, 3.0, 3.0)) }; line(6.0 + i * 7, 3.5 + j * 7, 8.5 + i * 7, 3.5 + j * 7) } },
        "MaskedTextBox" to Drawn { box(1.0, 4.0, 14.0, 8.0); text("#-#", 2.5f, 10.5f, 6.5f) },
        "MonthCalendar" to Drawn { box(1.0, 2.0, 14.0, 12.0); accent { fill(Rectangle2D.Double(1.5, 2.5, 13.0, 3.0)) }; for (i in 0..2) for (j in 0..1) fill(Rectangle2D.Double(3.0 + i * 4, 7.5 + j * 3, 2.0, 1.5)) },
        "NotifyIcon" to Drawn { box(1.0, 9.0, 14.0, 6.0); accent { fill(Ellipse2D.Double(9.0, 10.0, 4.0, 4.0)) }; draw(RoundRectangle2D.Double(3.5, 1.5, 9.0, 5.0, 2.0, 2.0)); line(6.0, 6.5, 8.0, 8.5) },
        "NumericUpDown" to Drawn { box(1.0, 4.0, 14.0, 8.0); line(10.5, 4.5, 10.5, 11.5); line(10.5, 8.0, 14.5, 8.0); text("1", 3f, 10.5f, 6.5f) },
        "PictureBox" to Drawn { box(1.0, 2.0, 14.0, 12.0); accent { fill(Path2D.Double().apply { moveTo(2.5, 12.5); lineTo(6.5, 7.0); lineTo(9.0, 10.0); lineTo(10.5, 8.5); lineTo(13.5, 12.5); closePath() }) }; color = JBColor(Color(0xE0A020), Color(0xE0B040)); fill(Ellipse2D.Double(10.0, 3.5, 2.5, 2.5)) },
        "ProgressBar" to Drawn { box(1.0, 5.0, 14.0, 6.0); color = green; for (i in 0..2) fill(Rectangle2D.Double(2.5 + i * 3, 6.5, 2.0, 3.0)) },
        "RadioButton" to Drawn { draw(Ellipse2D.Double(2.5, 2.5, 11.0, 11.0)); accent { fill(Ellipse2D.Double(5.5, 5.5, 5.0, 5.0)) } },
        "RichTextBox" to Drawn { box(1.0, 1.0, 14.0, 14.0); line(3.0, 4.5, 12.0, 4.5); accent { line(3.0, 7.5, 9.0, 7.5) }; line(3.0, 10.5, 11.0, 10.5); color = JBColor.RED; line(3.0, 13.0, 7.0, 13.0) },
        "TextBox" to Drawn { box(1.0, 4.0, 14.0, 8.0); text("ab", 2.5f, 10.5f, 6.5f); line(11.0, 5.5, 11.0, 10.5) },
        "ToolTip" to Drawn { draw(RoundRectangle2D.Double(1.5, 2.5, 13.0, 8.0, 3.0, 3.0)); draw(Path2D.Double().apply { moveTo(4.0, 10.5); lineTo(4.0, 14.0); lineTo(7.5, 10.5) }); accent { line(4.0, 6.5, 12.0, 6.5) } },
        "TreeView" to Drawn { line(3.5, 2.0, 3.5, 13.5); line(3.5, 6.5, 7.0, 6.5); line(3.5, 12.5, 7.0, 12.5); accent { box(1.0, 1.0, 5.0, 3.0); box(7.0, 5.0, 7.0, 3.0); box(7.0, 11.0, 7.0, 3.0) } },
        // Containers
        "FlowLayoutPanel" to Drawn { box(1.0, 1.0, 14.0, 14.0, false); accent { fill(Rectangle2D.Double(3.0, 3.0, 4.0, 3.0)); fill(Rectangle2D.Double(9.0, 3.0, 4.0, 3.0)); fill(Rectangle2D.Double(3.0, 9.0, 4.0, 3.0)) }; line(8.0, 10.5, 12.0, 10.5) },
        "GroupBox" to Drawn { draw(Path2D.Double().apply { moveTo(5.0, 3.5); lineTo(1.5, 3.5); lineTo(1.5, 14.5); lineTo(14.5, 14.5); lineTo(14.5, 3.5); lineTo(12.0, 3.5) }); text("xy", 5.5f, 6f, 6f) },
        "Panel" to Drawn { box(1.0, 1.0, 14.0, 14.0); accent { box(4.0, 4.0, 8.0, 8.0, false) } },
        "SplitContainer" to Drawn { box(1.0, 1.0, 14.0, 14.0); accent { line(8.0, 1.5, 8.0, 14.5) } },
        "TabControl" to Drawn { draw(Path2D.Double().apply { moveTo(1.5, 14.5); lineTo(1.5, 2.5); lineTo(7.5, 2.5); lineTo(7.5, 5.5); lineTo(14.5, 5.5); lineTo(14.5, 14.5); closePath() }); accent { line(8.0, 3.5, 12.0, 3.5) } },
        "TableLayoutPanel" to Drawn { box(1.0, 1.0, 14.0, 14.0); accent { line(8.0, 1.5, 8.0, 14.5); line(1.5, 8.0, 14.5, 8.0) } },
        // Menus & Toolbars
        "ContextMenuStrip" to Drawn { box(3.0, 1.0, 11.0, 14.0); for (i in 0..2) line(5.0, 4.5 + i * 4, 12.0, 4.5 + i * 4); accent { fill(Rectangle2D.Double(3.5, 7.0, 10.0, 3.0)) } },
        "MenuStrip" to Drawn { box(1.0, 2.0, 14.0, 4.0); text("File", 2f, 5.5f, 4.5f); box(3.0, 6.0, 8.0, 8.0); accent { line(4.5, 8.5, 9.5, 8.5) }; line(4.5, 11.5, 9.5, 11.5) },
        "StatusStrip" to Drawn { box(1.0, 9.0, 14.0, 5.0); line(2.0, 4.0, 14.0, 4.0); accent { line(3.0, 11.5, 8.0, 11.5) } },
        "ToolStrip" to Drawn { box(1.0, 4.0, 14.0, 7.0); accent { fill(Rectangle2D.Double(3.0, 6.0, 3.0, 3.0)); fill(Rectangle2D.Double(8.0, 6.0, 3.0, 3.0)) } },
        "ToolStripContainer" to Drawn { box(1.0, 1.0, 14.0, 14.0); accent { line(1.5, 4.5, 14.5, 4.5); line(1.5, 11.5, 14.5, 11.5) } },
        // Data
        "DataGridView" to Drawn { box(1.0, 2.0, 14.0, 12.0); accent { fill(Rectangle2D.Double(1.5, 2.5, 13.0, 3.0)) }; line(1.5, 9.5, 14.5, 9.5); line(6.0, 2.5, 6.0, 13.5); line(10.5, 2.5, 10.5, 13.5) },
        "BindingNavigator" to Drawn { box(1.0, 5.0, 14.0, 6.0); accent { fill(Path2D.Double().apply { moveTo(5.0, 6.5); lineTo(2.5, 8.0); lineTo(5.0, 9.5); closePath() }); fill(Path2D.Double().apply { moveTo(11.0, 6.5); lineTo(13.5, 8.0); lineTo(11.0, 9.5); closePath() }) } },
        "BindingSource" to Drawn { accent { draw(Ellipse2D.Double(2.0, 2.0, 7.0, 4.0)); line(2.0, 4.0, 2.0, 11.0); line(9.0, 4.0, 9.0, 11.0); draw(Arc2D.Double(2.0, 9.0, 7.0, 4.0, 180.0, 180.0, Arc2D.OPEN)) }; line(10.0, 10.0, 14.0, 10.0) },
        // Components
        "Timer" to Drawn { draw(Ellipse2D.Double(2.0, 2.5, 12.0, 12.0)); accent { line(8.0, 8.5, 8.0, 4.5); line(8.0, 8.5, 11.0, 8.5) }; line(6.0, 1.0, 10.0, 1.0) },
        "ImageList" to Drawn { box(4.0, 1.0, 11.0, 9.0); box(2.0, 3.0, 11.0, 9.0); box(0.0, 5.0, 11.0, 9.0); accent { fill(Path2D.Double().apply { moveTo(1.5, 12.5); lineTo(4.5, 9.0); lineTo(7.0, 11.0); lineTo(9.5, 12.5); closePath() }) } },
        "ErrorProvider" to Drawn { color = JBColor.RED; fill(Ellipse2D.Double(2.0, 2.0, 12.0, 12.0)); color = Color.WHITE; stroke = BasicStroke(1.8f); line(8.0, 4.5, 8.0, 9.0); line(8.0, 11.0, 8.0, 11.5) },
        "HelpProvider" to Drawn { accent { draw(Ellipse2D.Double(2.0, 2.0, 12.0, 12.0)) }; text("?", 5.5f, 12f, 10f, bold = true) },
        // Printing
        "PrintDocument" to Drawn { box(3.0, 1.0, 10.0, 14.0); for (i in 0..2) line(5.0, 4.5 + i * 3, 11.0, 4.5 + i * 3) },
        "PrintDialog" to Drawn { box(3.0, 1.0, 10.0, 5.0); box(1.0, 6.0, 14.0, 6.0); box(4.0, 10.0, 8.0, 5.0); accent { fill(Ellipse2D.Double(12.0, 7.5, 1.5, 1.5)) } },
        "PageSetupDialog" to Drawn { box(3.0, 1.0, 10.0, 14.0); accent { box(5.0, 3.0, 6.0, 10.0, false) } },
        "PrintPreviewControl" to Drawn { box(1.0, 1.0, 10.0, 13.0); accent { draw(Ellipse2D.Double(7.0, 7.0, 6.0, 6.0)); line(12.0, 12.0, 15.0, 15.0) } },
        "PrintPreviewDialog" to Drawn { box(1.0, 1.0, 10.0, 13.0); accent { draw(Ellipse2D.Double(7.0, 7.0, 6.0, 6.0)); line(12.0, 12.0, 15.0, 15.0) } },
        // Dialogs
        "ColorDialog" to Drawn { for ((i, c) in listOf(0xE04040, 0x40A040, 0x4060E0, 0xE0C040).withIndex()) { color = Color(c); fill(Rectangle2D.Double(2.0 + (i % 2) * 6, 2.0 + (i / 2) * 6, 5.0, 5.0)) } },
        "FolderBrowserDialog" to Drawn { color = JBColor(Color(0xD8A630), Color(0xD0A040)); fill(Path2D.Double().apply { moveTo(1.0, 4.0); lineTo(6.0, 4.0); lineTo(7.5, 5.5); lineTo(15.0, 5.5); lineTo(15.0, 13.0); lineTo(1.0, 13.0); closePath() }) },
        "FontDialog" to Drawn { text("A", 1f, 13f, 12f, bold = true); accent { text("a", 9f, 13f, 9f) } },
        "OpenFileDialog" to Drawn { color = JBColor(Color(0xD8A630), Color(0xD0A040)); fill(Path2D.Double().apply { moveTo(1.0, 4.0); lineTo(6.0, 4.0); lineTo(7.5, 5.5); lineTo(15.0, 5.5); lineTo(15.0, 13.0); lineTo(1.0, 13.0); closePath() }); color = Color(0xFFF4D0); fill(Path2D.Double().apply { moveTo(3.0, 8.0); lineTo(15.0, 8.0); lineTo(13.0, 13.0); lineTo(1.0, 13.0); closePath() }) },
        "SaveFileDialog" to Drawn { accent { box(2.0, 2.0, 12.0, 12.0) }; box(4.0, 2.0, 8.0, 4.0); box(4.0, 9.0, 8.0, 5.0) },
        // Others in All Windows Forms
        "DomainUpDown" to Drawn { box(1.0, 4.0, 14.0, 8.0); line(10.5, 4.5, 10.5, 11.5); line(10.5, 8.0, 14.5, 8.0); line(3.0, 8.0, 8.0, 8.0) },
        "HScrollBar" to Drawn { box(1.0, 5.0, 14.0, 6.0); accent { fill(Rectangle2D.Double(5.0, 6.5, 5.0, 3.0)) } },
        "VScrollBar" to Drawn { box(5.0, 1.0, 6.0, 14.0); accent { fill(Rectangle2D.Double(6.5, 5.0, 3.0, 5.0)) } },
        "TrackBar" to Drawn { line(1.0, 8.0, 15.0, 8.0); accent { fill(Path2D.Double().apply { moveTo(6.0, 4.0); lineTo(10.0, 4.0); lineTo(10.0, 10.0); lineTo(8.0, 12.0); lineTo(6.0, 10.0); closePath() }) }; for (i in 0..3) line(2.0 + i * 4, 13.5, 2.0 + i * 4, 15.0) },
        "Splitter" to Drawn { box(1.0, 1.0, 14.0, 14.0, false); accent { fill(Rectangle2D.Double(7.0, 1.5, 2.0, 13.0)) } },
        "PropertyGrid" to Drawn { box(1.0, 1.0, 14.0, 14.0); line(7.0, 1.5, 7.0, 14.5); for (i in 0..2) line(1.5, 5.0 + i * 3.5, 14.5, 5.0 + i * 3.5) },
        "WebBrowser" to Drawn { accent { draw(Ellipse2D.Double(2.0, 2.0, 12.0, 12.0)); line(2.0, 8.0, 14.0, 8.0); draw(Ellipse2D.Double(5.0, 2.0, 6.0, 12.0)) } },
    )

    private val generic: Icon = Drawn { box(2.0, 2.0, 12.0, 12.0); accent { fill(Rectangle2D.Double(5.0, 5.0, 6.0, 6.0)) } }

    /** The picture for a toolbox item by its control name (Button, TextBox, …), or a generic one. */
    fun forControl(name: String): Icon = icons[name] ?: generic
}
