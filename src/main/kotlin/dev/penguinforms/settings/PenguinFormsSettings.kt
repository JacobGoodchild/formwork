package dev.penguinforms.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Penguin Forms' settings (Settings → Tools → Penguin Forms). Stored locally; nothing leaves the computer. */
@Service(Service.Level.APP)
@State(name = "PenguinFormsSettings", storages = [Storage("penguinforms.xml")])
class PenguinFormsSettings : PersistentStateComponent<PenguinFormsSettings.State> {
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
        fun getInstance(): PenguinFormsSettings = ApplicationManager.getApplication().getService(PenguinFormsSettings::class.java)
    }
}
