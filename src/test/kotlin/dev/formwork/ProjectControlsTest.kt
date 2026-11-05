package dev.formwork

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/** The project's own controls in the toolbox after a build, and a control that fails to draw (FORMWORK_BUILD_TESTS=1). */
@EnabledIfEnvironmentVariable(named = "FORMWORK_BUILD_TESTS", matches = "1")
class ProjectControlsTest {
    @Test
    fun `own controls appear after a build, and a broken one is drawn as a red error box`() {
        val dir = File(TestSupport.copySample("CustomControls"), "CustomControls")
        val p = ProcessBuilder(TestSupport.dotnet, "build", dir.path, "-nologo", "-v", "q").redirectErrorStream(true).start()
        val log = p.inputStream.bufferedReader().readText(); p.waitFor(5, TimeUnit.MINUTES)
        assertEquals(0, p.exitValue(), log)
        val dll = File(dir, "bin/Debug/net10.0/CustomControls.dll")
        TestSupport.startHost().use { host ->
            host.call("libraries", JsonObject().apply { add("assemblies", JsonArray().apply { add(dll.path) }) })
            val groups = host.call("toolbox", JsonObject().apply { add("libraries", JsonArray().apply { add(JsonObject().apply {
                addProperty("id", "project"); addProperty("name", "CustomControls Components"); add("assemblies", JsonArray().apply { add(dll.path) }) }) }) }).result!!.asJsonArray
            val own = groups.map { it.asJsonObject }.first { it.get("name").asString == "CustomControls Components" }
            assertEquals(listOf("BrokenControl", "ScoreBox"), own.getAsJsonArray("items").map { it.asJsonObject.get("name").asString })

            host.call("open", JsonObject().apply { addProperty("path", File(dir, "Form1.Designer.cs").path) })
            val view = host.call("apply", JsonObject().apply { add("ops", JsonArray().apply {
                add(JsonObject().apply { addProperty("op", "add"); addProperty("type", "CustomControls.BrokenControl"); addProperty("parent", ""); addProperty("x", 20); addProperty("y", 20) })
                add(JsonObject().apply { addProperty("op", "add"); addProperty("type", "System.Windows.Forms.Button"); addProperty("parent", ""); addProperty("x", 20); addProperty("y", 80) })
            }) }).result!!.asJsonObject
            assertEquals(setOf("brokenControl1", "button1"), view.getAsJsonArray("items").map { it.asJsonObject.get("id").asString }.toSet(), "the rest of the form still works")
            val png = ImageIO.read(java.util.Base64.getDecoder().decode(view.get("png").asString).inputStream())
            // The broken control's corner is drawn red (the error box), where a working control would be grey.
            val c = java.awt.Color(png.getRGB(21, 21))
            assertTrue(c.red > 180 && c.green < 80 && c.blue < 80, "red error box at the broken control, got $c")
        }
    }
}
