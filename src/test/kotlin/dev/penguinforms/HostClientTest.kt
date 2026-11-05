package dev.penguinforms

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.penguinforms.host.HostException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class HostClientTest {
    private fun op(vararg pairs: Pair<String, Any>) = JsonObject().apply {
        for ((k, v) in pairs) when (v) { is Number -> addProperty(k, v); else -> addProperty(k, v.toString()) }
    }

    @Test
    fun `hello answers with the Penguin Forms protocol`() {
        TestSupport.startHost().use { host ->
            val r = host.call("formwork.hello").result!!.asJsonObject
            assertEquals(1, r.get("formwork").asInt)
            assertEquals(1, r.get("protocol").asInt)
        }
    }

    @Test
    fun `edits come back as writes and the disk is never touched`() {
        val dir = TestSupport.copySample("VsTemplate.Net8")
        val designer = File(dir, "WinFormsApp1/Form1.Designer.cs")
        val code = File(dir, "WinFormsApp1/Form1.cs")
        val before = designer.readBytes()
        val codeBefore = code.readBytes()
        TestSupport.startHost().use { host ->
            // The editor's buffer differs from the disk (an unsaved change): the host must build on it.
            val buffer = designer.readText().removePrefix("﻿").replace("\r\n", "\n").replace("Text = \"Form1\";", "Text = \"Unsaved\";")
            val files = mapOf(designer.path to buffer, code.path to code.readText().replace("\r\n", "\n"), File(dir, "WinFormsApp1/Form1.resx").path to null)
            val opened = host.call("open", JsonObject().apply { addProperty("path", designer.path) }, files)
            assertEquals("Unsaved", opened.result!!.asJsonObject.get("text").asString)
            assertTrue(opened.writes.isEmpty())

            val add = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { add(op("op" to "add", "type" to "System.Windows.Forms.Button", "parent" to "", "x" to 300, "y" to 100)) }) })
            val view = add.result!!.asJsonObject
            assertEquals("button1", view.getAsJsonArray("items")[0].asJsonObject.get("id").asString)
            val written = add.writes[designer.path]!!
            assertTrue(written.contains("private Button button1;"), written)
            assertTrue(written.contains("button1.Location = new Point(300, 100);"), written)
            assertTrue(written.contains("Text = \"Unsaved\";"), "built on the buffer, not the disk")

            val event = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { add(op("op" to "setEvent", "id" to "button1", "event" to "Click", "handler" to "button1_Click")) }) })
            val handler = event.result!!.asJsonObject.getAsJsonObject("handler")
            assertEquals("button1_Click", handler.get("method").asString)
            assertTrue(handler.get("created").asBoolean)
            assertTrue(event.writes[code.path]!!.contains("private void button1_Click(object sender, EventArgs e)"))
            assertTrue(event.writes[designer.path]!!.contains("button1.Click += button1_Click;"))
        }
        assertTrue(before.contentEquals(designer.readBytes()), "Form1.Designer.cs on disk unchanged")
        assertTrue(codeBefore.contentEquals(code.readBytes()), "Form1.cs on disk unchanged")
    }

    @Test
    fun `a broken designer file is a code error with its line, and nothing is written`() {
        val dir = TestSupport.copySample("VsTemplate.Net8")
        val designer = File(dir, "WinFormsApp1/Form1.Designer.cs")
        TestSupport.startHost().use { host ->
            val broken = designer.readText().removePrefix("﻿").replace("\r\n", "\n").replace("ClientSize = new Size(800, 450);", "ClientSize = new Size(800, 450")
            val e = assertThrows<HostException> {
                host.call("open", JsonObject().apply { addProperty("path", designer.path) }, mapOf(designer.path to broken))
            }
            assertEquals("code", e.kind)
            assertTrue((e.line ?: 0) > 0, "has a line: ${e.message}")
        }
    }

    @Test
    fun `close ends the process`() {
        val host = TestSupport.startHost()
        assertTrue(host.alive)
        host.close()
        assertFalse(host.alive)
    }
}
