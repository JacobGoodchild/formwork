package dev.penguinforms

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import dev.penguinforms.editor.sharedRows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Several controls selected: Properties shows what they share, blank where they differ (as Visual Studio). */
class SharedRowsTest {
    private fun rows(json: String): JsonArray = JsonParser.parseString(json).asJsonArray

    @Test
    fun `only shared properties, differing values blank, Name left out`() {
        val button = rows("""[{"name":"Name","type":"String","value":"btnOK"},{"name":"Text","type":"String","value":"OK","modified":true},
            {"name":"Cursor","type":"Cursor","value":"Default"},{"name":"DialogResult","type":"DialogResult","value":"None"}]""")
        val textBox = rows("""[{"name":"Name","type":"String","value":"txtUser"},{"name":"Text","type":"String","value":"","modified":false},
            {"name":"Cursor","type":"Cursor","value":"Default"},{"name":"Multiline","type":"Boolean","value":"False"}]""")
        val shared = sharedRows(listOf(textBox, button), "value") // button is the primary (last selected)
        assertEquals(listOf("Text", "Cursor"), shared.map { it.asJsonObject.get("name").asString })
        assertEquals("", shared[0].asJsonObject.get("value").asString, "Text differs: blank")
        assertEquals(false, shared[0].asJsonObject.get("modified").asBoolean)
        assertEquals("Default", shared[1].asJsonObject.get("value").asString, "Cursor is the same: shown")
    }
}
