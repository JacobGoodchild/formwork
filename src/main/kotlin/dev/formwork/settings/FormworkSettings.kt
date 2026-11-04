package dev.formwork.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Formwork's settings (Settings → Tools → Formwork). Stored locally; nothing leaves the computer. */
@Service(Service.Level.APP)
@State(name = "FormworkSettings", storages = [Storage("formwork.xml")])
class FormworkSettings : PersistentStateComponent<FormworkSettings.State> {
    class State {
        var dotnetPath: String = ""
        var hostPath: String = ""
        var snapToLines: Boolean = true
        var layoutMode: String = "SnapLines"
        var gridWidth: Int = 8
        var gridHeight: Int = 8
        var showGrid: Boolean = true
        var snapToGrid: Boolean = true
    }

    private var state = State()
    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    companion object {
        fun getInstance(): FormworkSettings = ApplicationManager.getApplication().getService(FormworkSettings::class.java)
    }
}
