package dev.penguinforms.editor

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Puts the host's toolbox groups in Visual Studio 2022's order: All Windows Forms, Common Controls, Containers,
 * Menus & Toolbars, Components, Printing, Dialogs, Data, General (empty unless the user adds to it). Groups of
 * the project's own controls (built libraries) come first, as VS lists "<Project> Components" at the top.
 */
object ToolboxModel {
    val VS_ORDER = listOf("All Windows Forms", "Common Controls", "Containers", "Menus & Toolbars", "Components", "Printing", "Dialogs", "Data", "General")

    /** Groups Visual Studio shows collapsed on first use; Common Controls is open (as in Visual Studio 2022). */
    val COLLAPSED_AT_FIRST = VS_ORDER - "Common Controls"

    fun vsOrder(categories: JsonElement?): JsonArray {
        val input = categories?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
        val byName = LinkedHashMap<String, JsonObject>()
        val libraries = ArrayList<JsonObject>()
        for (c in input) {
            val o = c.asJsonObject
            if (o.get("library")?.asBoolean == true) libraries.add(o) else byName[o.get("name")?.asString ?: ""] = o
        }
        val out = JsonArray()
        libraries.forEach(out::add)
        for (name in VS_ORDER) {
            val group = byName.remove(name) ?: if (name == "General") JsonObject().apply { addProperty("name", "General"); add("items", JsonArray()) } else continue
            // Alphabetical within a group, as VS lists them (Pointer is added first by the toolbox itself).
            val items = group.getAsJsonArray("items")?.sortedBy { it.asJsonObject.get("name")?.asString?.lowercase() }
            if (items != null) group.add("items", JsonArray().apply { items.forEach(::add) })
            out.add(group)
        }
        byName.values.forEach(out::add) // any group VS doesn't have, at the end
        return out
    }

    fun names(categories: JsonArray): List<String> = categories.mapNotNull { it.asJsonObject.get("name")?.asString }
}
