package dev.penguinforms

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.penguinforms.host.HostClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Group E of the brief: the code the designer writes, checked against what Visual Studio writes. */
class CodeGenerationTest {
    /** A form open in the host, with Penguin Forms' buffer handling: writes are kept as the "editor" texts. */
    private class Session(sample: String) : AutoCloseable {
        val dir = TestSupport.copySample(sample)
        val designer = File(dir.listFiles()!!.first { it.isDirectory }, "Form1.Designer.cs")
        val code = File(designer.parentFile, "Form1.cs")
        val resx = File(designer.parentFile, "Form1.resx")
        val buffers = mutableMapOf<String, String?>(
            designer.path to designer.readText().removePrefix("﻿").replace("\r\n", "\n"),
            code.path to code.readText().removePrefix("﻿").replace("\r\n", "\n"),
            resx.path to resx.takeIf { it.isFile }?.readText()?.removePrefix("﻿")?.replace("\r\n", "\n"),
        )
        val host: HostClient = TestSupport.startHost()
        var view: JsonObject

        init {
            view = host.call("open", JsonObject().apply { addProperty("path", designer.path) }, buffers).result!!.asJsonObject
        }

        fun apply(vararg ops: JsonObject): JsonObject {
            val r = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { ops.forEach(::add) }) }, buffers)
            buffers.putAll(r.writes)
            view = r.result!!.asJsonObject
            return view
        }

        val designerText get() = buffers[designer.path]!!
        val codeText get() = buffers[code.path]!!
        fun ids() = view.getAsJsonArray("items").map { it.asJsonObject.get("id").asString }
        override fun close() = host.close()
    }

    private fun op(name: String, vararg pairs: Pair<String, Any>) = JsonObject().apply {
        addProperty("op", name)
        for ((k, v) in pairs) when (v) { is Number -> addProperty(k, v); else -> addProperty(k, v.toString()) }
    }
    private fun newControl(type: String, x: Int = 10, y: Int = 10) = op("add", "type" to "System.Windows.Forms.$type", "parent" to "", "x" to x, "y" to y)

    @Test
    fun `new controls get Visual Studio's names`() = Session("VsTemplate.Net8").use { s ->
        s.apply(newControl("Button"), newControl("Button", y = 40), newControl("TextBox", y = 70), newControl("Label", y = 100), newControl("CheckBox", y = 130))
        assertEquals(setOf("button1", "button2", "textBox1", "label1", "checkBox1"), s.ids().toSet())
        assertTrue(s.designerText.contains("private Button button2;"))
        assertTrue(s.designerText.contains("private TextBox textBox1;"))
    }

    @Test
    fun `deleting a control removes its designer code but keeps its handler`() = Session("VsTemplate.Net8").use { s ->
        s.apply(newControl("Button"))
        s.apply(op("setEvent", "id" to "button1", "event" to "Click", "handler" to "button1_Click"))
        s.apply(op("remove", "id" to "button1"))
        assertFalse(s.designerText.contains("button1"), "designer code for button1 is gone")
        assertTrue(s.codeText.contains("private void button1_Click(object sender, EventArgs e)"), "the handler stays, as in VS")
    }

    @Test
    fun `binding an existing handler doesn't write a second one`() = Session("VsTemplate.Net8").use { s ->
        s.apply(newControl("Button"))
        s.apply(op("setEvent", "id" to "button1", "event" to "Click", "handler" to "button1_Click"))
        val again = s.apply(op("setEvent", "id" to "button1", "event" to "Click", "handler" to "button1_Click"))
        assertEquals(1, Regex("void button1_Click\\(").findAll(s.codeText).count())
        assertFalse(again.get("handler")?.asJsonObject?.get("created")?.asBoolean == true)
    }

    @Test
    fun `renaming renames the field, its handlers and uses in Form1_cs`() = Session("VsTemplate.Net8").use { s ->
        s.apply(newControl("Button"))
        s.apply(op("setEvent", "id" to "button1", "event" to "Click", "handler" to "button1_Click"))
        // Code the user wrote, using the button.
        val code = s.codeText.replace("private void button1_Click(object sender, EventArgs e)\n    {\n\n    }", "private void button1_Click(object sender, EventArgs e)\n    {\n        button1.Text = \"Hi\";\n    }")
        s.buffers[s.code.path] = code
        s.apply(op("rename", "id" to "button1", "value" to "btnHello"))
        assertTrue(s.designerText.contains("private Button btnHello;"))
        assertTrue(s.designerText.contains("btnHello.Click += btnHello_Click;"))
        assertTrue(s.codeText.contains("private void btnHello_Click(object sender, EventArgs e)"), s.codeText)
        assertTrue(s.codeText.contains("btnHello.Text = \"Hi\";"), s.codeText)
        assertFalse(s.codeText.contains("button1"))
    }

    @Test
    fun `the old this-dot style of a designer file is kept`() {
        // A .NET Framework-era file, as Visual Studio 2019 and older wrote it.
        val dir = TestSupport.copySample("VsTemplate.Net8")
        val proj = dir.listFiles()!!.first { it.isDirectory }
        File(proj, "Form1.Designer.cs").writeText(OLD_STYLE)
        File(proj, "Form1.cs").writeText("namespace WinFormsApp1\n{\n    public partial class Form1 : System.Windows.Forms.Form\n    {\n        public Form1()\n        {\n            InitializeComponent();\n        }\n    }\n}\n")
        TestSupport.startHost().use { host ->
            host.call("open", JsonObject().apply { addProperty("path", File(proj, "Form1.Designer.cs").path) })
            val r = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { add(newControl("Button")) }) })
            val text = r.writes[File(proj, "Form1.Designer.cs").path]!!
            assertTrue(text.contains("this.button1 = new System.Windows.Forms.Button();"), text)
            assertTrue(text.contains("this.button1.Location = new System.Drawing.Point(10, 10);"), text)
            assertTrue(text.contains("private System.Windows.Forms.Button button1;"), text)
            assertTrue(text.contains("this.Controls.Add(this.button1);"), text)
        }
    }

    companion object {
        val OLD_STYLE = """
namespace WinFormsApp1
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.SuspendLayout();
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(800, 450);
            this.Name = "Form1";
            this.Text = "Form1";
            this.ResumeLayout(false);

        }

        #endregion
    }
}
""".trimStart()
    }
}
