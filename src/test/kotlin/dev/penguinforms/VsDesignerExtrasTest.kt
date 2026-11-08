package dev.penguinforms

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.penguinforms.host.HostClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Text-baseline snaplines, the Locked property (kept in the .resx as VS does) and moving a menu item into another menu. */
class VsDesignerExtrasTest {
    private class Session(sample: String) : AutoCloseable {
        val dir = TestSupport.copySample(sample)
        val designer = File(dir.listFiles()!!.first { it.isDirectory }, "Form1.Designer.cs")
        val resx = File(designer.parentFile, "Form1.resx")
        val buffers = mutableMapOf<String, String?>(
            designer.path to designer.readText().removePrefix("﻿").replace("\r\n", "\n"),
            File(designer.parentFile, "Form1.cs").let { it.path to it.readText().removePrefix("﻿").replace("\r\n", "\n") },
            resx.path to resx.takeIf { it.isFile }?.readText()?.removePrefix("﻿")?.replace("\r\n", "\n"),
        )
        val host: HostClient = TestSupport.startHost()
        var view: JsonObject = host.call("open", JsonObject().apply { addProperty("path", designer.path) }, buffers).result!!.asJsonObject

        fun apply(vararg ops: JsonObject): JsonObject {
            val r = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply { ops.forEach(::add) }) }, buffers)
            buffers.putAll(r.writes)
            view = r.result!!.asJsonObject
            return view
        }

        fun item(id: String): JsonObject = view.getAsJsonArray("items").map { it.asJsonObject }.first { it.get("id").asString == id }
        val designerText get() = buffers[designer.path]!!
        val resxText get() = buffers[resx.path]?.replace("\r\n", "\n")
        override fun close() = host.close()
    }

    private fun op(name: String, vararg pairs: Pair<String, Any>) = JsonObject().apply {
        addProperty("op", name)
        for ((k, v) in pairs) when (v) { is Number -> addProperty(k, v); else -> addProperty(k, v.toString()) }
    }

    private fun baselineY(s: Session, id: String) = s.item(id).let { it.get("y").asInt + it.get("baseline").asInt }

    @Test
    fun `text baselines are where Visual Studio puts them`() = Session("LoginForm").use { s ->
        // VS's numbers for Segoe UI 9: a text box's baseline 17 px down, a button's (75 x 23) 17, an auto-size label's 14.
        assertEquals(17, s.item("txtUser").get("baseline").asInt)
        assertEquals(17, s.item("btnOK").get("baseline").asInt)
        assertEquals(14, s.item("lblUser").get("baseline").asInt)
        // The template's label sits 3 px lower than its text box: exactly where their baselines meet.
        assertEquals(baselineY(s, "txtUser"), baselineY(s, "lblUser"))
        assertEquals(baselineY(s, "txtPassword"), baselineY(s, "lblPassword"))
    }

    @Test
    fun `Locked is written to the resx as VS writes it, follows renames and goes with the control`() = Session("LoginForm").use { s ->
        s.apply(op("setProp", "id" to "btnOK", "prop" to "Locked", "value" to "True"))
        val resx = s.resxText
        assertNotNull(resx)
        assertTrue(resx!!.contains("""<metadata name="btnOK.Locked" type="System.Boolean, mscorlib, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089">
    <value>True</value>
  </metadata>"""), resx)
        assertFalse(s.designerText.contains("Locked"), "the code never mentions it, as in VS")
        assertTrue(s.item("btnOK").get("locked").asBoolean)
        assertFalse(s.item("btnOK").get("movable").asBoolean)
        assertFalse(s.item("btnCancel").get("locked").asBoolean)
        val row = s.host.call("properties", JsonObject().apply { addProperty("id", "btnOK") }, s.buffers).result!!.asJsonArray
            .map { it.asJsonObject }.first { it.get("name").asString == "Locked" }
        assertEquals("True", row.get("value").asString)
        assertEquals("Design", row.get("category").asString)

        s.apply(op("rename", "id" to "btnOK", "value" to "btnSignIn"))
        assertTrue(s.resxText!!.contains("btnSignIn.Locked") && !s.resxText!!.contains("btnOK.Locked"), s.resxText)
        s.apply(op("remove", "id" to "btnSignIn"))
        assertFalse(s.resxText!!.contains(".Locked"), s.resxText)

        // The form itself: $this.Locked; unlocking takes the entry out.
        s.apply(op("setProp", "id" to "", "prop" to "Locked", "value" to "True"))
        assertTrue(s.resxText!!.contains("<metadata name=\"\$this.Locked\""))
        assertTrue(s.view.get("locked").asBoolean)
        s.apply(op("setProp", "id" to "", "prop" to "Locked", "value" to "False"))
        assertFalse(s.resxText!!.contains("\$this.Locked"))
    }

    @Test
    fun `undoing in the editor the edit that made the resx unlocks again`() = Session("LoginForm").use { s ->
        s.apply(op("setProp", "id" to "btnOK", "prop" to "Locked", "value" to "True"))
        assertTrue(s.item("btnOK").get("locked").asBoolean)
        // Rider's Ctrl+Z takes the new Form1.resx away; the next request carries no .resx.
        s.buffers[s.resx.path] = null
        s.apply(op("setProp", "id" to "btnCancel", "prop" to "Text", "value" to "Cancel"))
        assertFalse(s.item("btnOK").get("locked").asBoolean)
        assertTrue(s.item("btnOK").get("movable").asBoolean)
    }

    @Test
    fun `a menu item moves into another menu`() = Session("MenuDemo").use { s ->
        s.apply(op("moveItem", "id" to "exitToolStripMenuItem", "parent" to "helpToolStripMenuItem"))
        assertTrue(s.designerText.contains("fileToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { openToolStripMenuItem, toolStripSeparator1 });"), s.designerText)
        assertTrue(s.designerText.contains("helpToolStripMenuItem.DropDownItems.AddRange(new ToolStripItem[] { aboutToolStripMenuItem, exitToolStripMenuItem });"), s.designerText)
        // To a place on the menu bar, and not into its own sub-menu.
        s.apply(op("moveItem", "id" to "aboutToolStripMenuItem", "parent" to "menuStrip1", "index" to 1))
        assertTrue(s.designerText.contains("menuStrip1.Items.AddRange(new ToolStripItem[] { fileToolStripMenuItem, aboutToolStripMenuItem, helpToolStripMenuItem });"), s.designerText)
        val before = s.designerText
        runCatching { s.apply(op("moveItem", "id" to "fileToolStripMenuItem", "parent" to "openToolStripMenuItem")) }
        assertEquals(before, s.designerText)
    }
}
