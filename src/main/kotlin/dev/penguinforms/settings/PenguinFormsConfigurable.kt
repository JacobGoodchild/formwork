package dev.penguinforms.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import dev.penguinforms.host.DotnetLocator

/** Settings → Tools → Penguin Forms (the options Visual Studio has under Windows Forms Designer, plus where .NET is). */
class PenguinFormsConfigurable : BoundConfigurable("Penguin Forms") {
    private val state get() = PenguinFormsSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        group("Windows Forms Designer") {
            row("Layout mode:") {
                comboBox(listOf("SnapLines", "SnapToGrid")).bindItem({ state.layoutMode }, { state.layoutMode = it ?: "SnapLines" })
                    .comment("SnapLines: controls line up with each other as you drag (Visual Studio's default). SnapToGrid: the Format menu's \"to Grid\" commands use the grid below.")
            }
            row("Grid size:") {
                intTextField(2..100).bindIntText({ state.gridWidth }, { state.gridWidth = it }).columns(4)
                label("×")
                intTextField(2..100).bindIntText({ state.gridHeight }, { state.gridHeight = it }).columns(4)
            }
            row { checkBox("Show grid (in SnapToGrid mode)").bindSelected({ state.showGrid }, { state.showGrid = it }) }
            row { checkBox("Design and code in separate tabs, like Visual Studio (applies to forms opened afterwards)").bindSelected({ state.separateDesignTab }, { state.separateDesignTab = it }) }
            row { checkBox("Snap while dragging (hold Alt to move freely)").bindSelected({ state.snapToLines }, { state.snapToLines = it }) }
        }
        group(".NET") {
            row(".NET (dotnet) program:") {
                textFieldWithBrowseButton(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Choose the dotnet Program"))
                    .bindText({ state.dotnetPath }, { state.dotnetPath = it.trim() })
                    .comment("Leave empty to find it automatically. Found now: ${DotnetLocator.find()?.path ?: "none — install the .NET 10 SDK"}")
            }
            row("Designer host:") {
                textFieldWithBrowseButton(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Choose Formwork.Host.dll"))
                    .bindText({ state.hostPath }, { state.hostPath = it.trim() })
                    .comment("Leave empty to use the host that comes with Penguin Forms (for developers only).")
            }
        }
        row { comment("Penguin Forms makes no network requests of its own. Changes apply to designers opened after you click OK.") }
    }
}
