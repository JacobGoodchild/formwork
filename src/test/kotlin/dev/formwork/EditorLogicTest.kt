package dev.formwork

import dev.formwork.editor.DocumentDiff
import dev.formwork.editor.HandlerLocator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EditorLogicTest {
    @Test
    fun `diff touches only the changed middle`() {
        val c = DocumentDiff.change("abcXYZdef", "abc123def")!!
        assertEquals(3, c.start); assertEquals(6, c.endOld); assertEquals("123", c.replacement)
        assertNull(DocumentDiff.change("same", "same"))
        val insert = DocumentDiff.change("ab", "aXb")!!
        assertEquals(1, insert.start); assertEquals(1, insert.endOld); assertEquals("X", insert.replacement)
    }

    @Test
    fun `cursor goes inside a fresh handler, indented as in Visual Studio`() {
        val code = "namespace A;\n\npublic partial class Form1 : Form\n{\n    public Form1()\n    {\n        InitializeComponent();\n    }\n\n    private void button1_Click(object sender, EventArgs e)\n    {\n\n    }\n}\n"
        val t = HandlerLocator.bodyPosition(code, "button1_Click", 10)!!
        assertEquals("        ", t.indentToInsert)
        assertEquals(code.indexOf("    {\n\n") + "    {\n".length, t.offset)
    }

    @Test
    fun `an existing handler with code puts the cursor at its first statement`() {
        val code = "class F {\n    private void button1_Click(object sender, EventArgs e)\n    {\n        label1.Text = \"Hi\";\n    }\n}\n"
        val t = HandlerLocator.bodyPosition(code, "button1_Click", null)!!
        assertEquals("", t.indentToInsert)
        assertEquals(code.indexOf("label1"), t.offset)
    }

    @Test
    fun `a subscription line is not mistaken for the method`() {
        val code = "button1.Click += button1_Click;\nprivate void button1_Click(object sender, EventArgs e)\n{\n}\n"
        val t = HandlerLocator.bodyPosition(code, "button1_Click", null)!!
        assertEquals(code.indexOf("{\n") + 2, t.offset)
    }
}

class ToolboxModelTest {
    @org.junit.jupiter.api.Test
    fun `groups come in Visual Studio's order with an empty General group`() {
        dev.formwork.TestSupport.startHost().use { host ->
            val vs = dev.formwork.editor.ToolboxModel.vsOrder(host.call("toolbox").result)
            org.junit.jupiter.api.Assertions.assertEquals(
                listOf("All Windows Forms", "Common Controls", "Containers", "Menus & Toolbars", "Components", "Printing", "Dialogs", "Data", "General"),
                dev.formwork.editor.ToolboxModel.names(vs),
            )
            val common = vs[1].asJsonObject.getAsJsonArray("items").map { it.asJsonObject.get("name").asString }
            // Visual Studio's Common Controls, minus WebBrowser (a Windows-only Internet Explorer control; see Known gaps).
            org.junit.jupiter.api.Assertions.assertEquals(
                listOf("Button", "CheckBox", "CheckedListBox", "ComboBox", "DateTimePicker", "Label", "LinkLabel", "ListBox", "ListView", "MaskedTextBox",
                    "MonthCalendar", "NotifyIcon", "NumericUpDown", "PictureBox", "ProgressBar", "RadioButton", "RichTextBox", "TextBox", "ToolTip", "TreeView"),
                common,
            )
            org.junit.jupiter.api.Assertions.assertEquals(0, vs[8].asJsonObject.getAsJsonArray("items").size())
        }
    }
}
