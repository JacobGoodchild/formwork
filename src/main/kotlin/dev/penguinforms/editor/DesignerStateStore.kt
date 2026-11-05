package dev.penguinforms.editor

import java.util.concurrent.ConcurrentHashMap

/** The webview's own remembered state (zoom, folded groups, Properties/Events tab), per form, for this IDE session. */
object DesignerStateStore {
    private val states = ConcurrentHashMap<String, String>()
    fun get(path: String): String? = states[path]
    fun put(path: String, state: String) { states[path] = state }
}
