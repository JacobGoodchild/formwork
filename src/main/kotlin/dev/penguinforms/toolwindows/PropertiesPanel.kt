package dev.penguinforms.toolwindows

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.penguinforms.editor.ActiveDesigner
import dev.penguinforms.editor.DesignerEditor
import dev.penguinforms.editor.DesignerListener
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.AbstractCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

/**
 * The Properties tool window, laid out like Visual Studio's (Jacob's screenshot): the selection drop-down
 * ("button1  System.Windows.Forms.Button"), the toolbar (Categorized, Alphabetical | Properties, Events | Property
 * Pages), a two-column grid grouped by category with non-default values in **bold**, and a description pane.
 * The Events view lists the events: double-click one to create or go to its handler, type a name and press Enter,
 * or clear it to unbind (the method stays in the code, as in VS).
 */
class PropertiesPanel(private val project: Project, parent: Disposable) : JPanel(BorderLayout()), DesignerListener {
    /** One row of the grid: a category band or a property / event. */
    private sealed class Row {
        data class Category(val name: String) : Row()
        data class Item(val name: String, val category: String, val value: String, val data: JsonObject) : Row()
        /** One part of an expanded Point / Size / Padding property (VS's ⊞ Size → Width, Height). */
        data class Part(val parent: Item, val index: Int, val label: String, val value: String) : Row()
    }

    private var categorized = true
    private var events = false
    private var primary = ""
    /** Everything selected; an edit applies to all of them, as in VS's multi-selection grid. */
    private var selected: List<String> = listOf("")
    private var rows = JsonArray()
    private var view: JsonObject? = null
    private val collapsed = HashSet<String>()
    private val shown = ArrayList<Row>()
    private val tableModel = Model()
    private val table = Grid()
    private val selector = ComboBox<String>()
    private var updatingSelector = false
    private val descriptionName = JBLabel().apply { font = font.deriveFont(Font.BOLD) }
    private val descriptionText = JBTextArea().apply { isEditable = false; lineWrap = true; wrapStyleWord = true; isOpaque = false; border = null; font = com.intellij.util.ui.UIUtil.getLabelFont() }

    private val editor: DesignerEditor? get() = ActiveDesigner.get(project).editor

    private lateinit var hintCard: HintCard

    init {
        // The selection drop-down: name in bold, then the type (as VS writes "Form1 System.Windows.Forms.Form").
        selector.renderer = object : ColoredListCellRenderer<String>() {
            override fun customizeCellRenderer(list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean) {
                if (value == null) return
                val (name, type) = labelFor(value)
                append(name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                append("  $type", SimpleTextAttributes.GRAY_ATTRIBUTES)
            }
        }
        selector.addActionListener {
            if (updatingSelector) return@addActionListener
            val id = selector.selectedItem as? String ?: return@addActionListener
            editor?.select(listOf(id))
        }

        val group = DefaultActionGroup().apply {
            add(object : ToggleAction("Categorized", "Group properties by category", AllIcons.Actions.GroupBy) {
                override fun isSelected(e: AnActionEvent) = categorized
                override fun setSelected(e: AnActionEvent, state: Boolean) { categorized = true; rebuild() }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(object : ToggleAction("Alphabetical", "Sort properties alphabetically", AllIcons.ObjectBrowser.Sorted) {
                override fun isSelected(e: AnActionEvent) = !categorized
                override fun setSelected(e: AnActionEvent, state: Boolean) { categorized = false; rebuild() }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(Separator.getInstance())
            add(object : ToggleAction("Properties", "Show properties", AllIcons.Nodes.Property) {
                override fun isSelected(e: AnActionEvent) = !events
                override fun setSelected(e: AnActionEvent, state: Boolean) { if (events) { events = false; request() } }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(object : ToggleAction("Events", "Show events", AllIcons.Actions.Lightning) {
                override fun isSelected(e: AnActionEvent) = events
                override fun setSelected(e: AnActionEvent, state: Boolean) { if (!events) { events = true; request() } }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(Separator.getInstance())
            add(object : DumbAwareAction("Property Pages", "Property pages (not used by Windows Forms controls)", AllIcons.General.Settings) {
                override fun actionPerformed(e: AnActionEvent) {}
                override fun update(e: AnActionEvent) { e.presentation.isEnabled = false }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("PenguinFormsProperties", group, true)
        toolbar.targetComponent = this

        val north = JPanel(BorderLayout()).apply {
            add(selector, BorderLayout.NORTH)
            add(toolbar.component, BorderLayout.CENTER)
        }
        val description = JPanel(BorderLayout(0, JBUI.scale(2))).apply {
            border = JBUI.Borders.compound(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0), JBUI.Borders.empty(4, 6))
            add(descriptionName, BorderLayout.NORTH)
            add(descriptionText, BorderLayout.CENTER)
            minimumSize = Dimension(0, JBUI.scale(56))
        }
        val splitter = JBSplitter(true, 0.82f).apply {
            firstComponent = JBScrollPane(table)
            secondComponent = description
        }
        add(north, BorderLayout.NORTH)
        hintCard = HintCard(splitter)
        add(hintCard, BorderLayout.CENTER)

        ActiveDesigner.get(project).addListener(this, parent)
        activeChanged(ActiveDesigner.get(project).editor)
    }

    // ---- data -------------------------------------------------------------------------------------------------

    private fun labelFor(id: String): Pair<String, String> {
        val v = view ?: return id to ""
        if (id == "") return (v.get("className")?.asString ?: "Form") to (v.get("rootType")?.asString ?: "")
        val item = v.getAsJsonArray("items")?.firstOrNull { it.asJsonObject.get("id")?.asString == id }?.asJsonObject
            ?: v.getAsJsonArray("tray")?.firstOrNull { it.asJsonObject.get("id")?.asString == id }?.asJsonObject
        return id.substringAfterLast('.').let { if (id.contains('.')) id else it } to (item?.get("type")?.asString ?: "")
    }

    private fun request() {
        val e = editor ?: return
        val kind = if (events) "events" else "properties"
        if (selected.size > 1) e.session.requestSharedRows(kind, selected) else e.session.requestRows(kind, primary)
    }

    override fun activeChanged(editor: DesignerEditor?) {
        view = editor?.session?.lastView
        primary = editor?.session?.selection?.lastOrNull() ?: ""
        selected = editor?.session?.selection?.ifEmpty { null } ?: listOf("")
        rows = JsonArray()
        refreshSelector()
        rebuild()
        hintCard.showHint(if (editor == null) "Open a form in the designer to see its properties." else null)
        if (editor != null) request()
    }

    override fun viewChanged(editor: DesignerEditor, view: JsonObject) {
        this.view = view
        refreshSelector()
        request()
    }

    override fun selectionChanged(editor: DesignerEditor, ids: List<String>) {
        primary = ids.lastOrNull() ?: ""
        selected = ids.ifEmpty { listOf("") }
        refreshSelector()
        request()
    }

    override fun propertiesChanged(editor: DesignerEditor, id: String, rows: JsonArray) {
        if (events || id != primary) return
        this.rows = rows
        rebuild()
    }

    override fun eventsChanged(editor: DesignerEditor, id: String, rows: JsonArray) {
        if (!events || id != primary) return
        this.rows = rows
        rebuild()
    }

    private fun refreshSelector() {
        updatingSelector = true
        try {
            val v = view
            val ids = ArrayList<String>()
            ids.add("")
            v?.getAsJsonArray("items")?.forEach { val o = it.asJsonObject; if (o.get("kind")?.asString != "dropdown") ids.add(o.get("id").asString) }
            v?.getAsJsonArray("tray")?.forEach { ids.add(it.asJsonObject.get("id").asString) }
            selector.model = DefaultComboBoxModel(ids.distinct().sortedWith(compareBy({ it != "" }, { it.lowercase() })).toTypedArray())
            // Several controls: blank, as Visual Studio's drop-down (the grid shows what they share).
            selector.selectedItem = if (selected.size > 1) null else primary
            selector.toolTipText = if (selected.size > 1) "${selected.size} controls selected: changes apply to all of them" else null
        } finally { updatingSelector = false }
    }

    private val expanded = HashSet<String>()

    private fun partsOf(item: Row.Item): List<String>? = when (item.data.get("type")?.asString) {
        "Point" -> listOf("X", "Y")
        "Size" -> listOf("Width", "Height")
        "Padding" -> listOf("All", "Left", "Top", "Right", "Bottom")
        else -> null
    }

    private fun withParts(list: List<Row.Item>): List<Row> = list.flatMap { item ->
        val labels = if (events) null else partsOf(item)
        if (labels == null || item.data.get("name").asString !in expanded) listOf(item)
        else {
            val nums = item.value.split(',').map { it.trim() }
            // Padding shows All when the four sides are equal (as WinForms' PaddingConverter does).
            val values = if (labels.size == 5) listOf(if (nums.size == 4 && nums.distinct().size == 1) nums[0] else "-1") + nums else nums
            listOf(item) + labels.mapIndexed { i, l -> Row.Part(item, i, l, values.getOrElse(i) { "" }) }
        }
    }

    private fun setPart(part: Row.Part, value: String) {
        val n = value.trim().toIntOrNull() ?: return
        val labels = partsOf(part.parent) ?: return
        val now = part.parent.value.split(',').map { it.trim() }.toMutableList()
        if (now.size < (if (labels.size == 5) 4 else labels.size)) return // blank: the selected controls differ
        val text = if (labels.size == 5) {
            if (part.index == 0) List(4) { n.toString() }.joinToString(", ") else { now[part.index - 1] = n.toString(); now.joinToString(", ") }
        } else { now[part.index] = n.toString(); now.joinToString(", ") }
        setValue(part.parent, text)
    }

    private fun rebuild() {
        if (table.isEditing) table.cellEditor?.cancelCellEditing()
        val items = rows.map { it.asJsonObject }.map { o ->
            val value = if (events) o.get("handler")?.takeIf { it.isJsonPrimitive }?.asString ?: "" else o.get("value")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            Row.Item(displayName(o.get("name").asString), o.get("category")?.asString ?: "Misc", value, o)
        }
        shown.clear()
        if (categorized) {
            for ((cat, list) in items.groupBy { it.category }.toSortedMap()) {
                shown.add(Row.Category(cat))
                if (cat !in collapsed) shown.addAll(withParts(list.sortedBy { it.name.lowercase().removePrefix("(") }))
            }
        } else shown.addAll(withParts(items.sortedBy { it.name.lowercase().removePrefix("(") }))
        tableModel.fireTableDataChanged()
        // As VS, a property is always selected so the description pane says something: Text if there is one.
        val pick = shown.indexOfFirst { it is Row.Item && it.name == (if (events) "Click" else "Text") }.takeIf { it >= 0 }
            ?: shown.indexOfFirst { it is Row.Item }
        if (pick >= 0) { table.setRowSelectionInterval(pick, pick); showDescription(shown[pick] as Row.Item) } else showDescription(null)
    }

    /** VS writes (Name) for the design-time Name property, and lists data bindings under (DataBindings). */
    private fun displayName(name: String) = when {
        name == "Name" -> "(Name)"
        name.startsWith("DataBindings.") -> "(DataBindings) " + name.removePrefix("DataBindings.")
        else -> name
    }

    private fun showDescription(row: Row.Item?) {
        descriptionName.text = row?.name ?: ""
        descriptionText.text = row?.data?.get("description")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
    }

    // ---- edits ------------------------------------------------------------------------------------------------

    private fun op(name: String, vararg pairs: Pair<String, Any?>) = JsonObject().apply {
        addProperty("op", name)
        addProperty("id", primary)
        for ((k, v) in pairs) when (v) { null -> {}; is JsonArray -> add(k, v); else -> addProperty(k, v.toString()) }
    }

    private fun setValue(row: Row.Item, value: String) {
        val e = editor ?: return
        val name = row.data.get("name").asString
        if (events) {
            if (value.isBlank()) e.session.applyOps(JsonArray().apply { add(op("setEvent", "event" to name, "handler" to "")) }, "Event Handler")
            else e.session.bindEventAndGo(primary, name, value.trim())
            return
        }
        if (value == row.value && selected.size == 1) return
        if (name == "Name") { e.session.applyOps(JsonArray().apply { add(op("rename", "value" to value)) }, "Rename"); return }
        // With several controls selected, the same value goes to each of them: one Ctrl+Z step.
        val ops = JsonArray()
        for (id in selected) ops.add(op("setProp", "prop" to name, "value" to value).apply { addProperty("id", id) })
        e.session.applyOps(ops, "Property Change")
    }

    private fun reset(row: Row.Item) {
        editor?.session?.applyOps(JsonArray().apply { add(op("resetProp", "prop" to row.data.get("name").asString)) }, "Reset")
    }

    /** Double-click on an event: its handler is created (VS's default name) or opened. */
    private fun activateEvent(row: Row.Item) {
        val e = editor ?: return
        val handler = row.value.ifBlank { row.data.get("defaultHandler")?.asString ?: return }
        if (row.value.isNotBlank()) e.session.gotoExistingHandler(handler) else e.session.bindEventAndGo(primary, row.data.get("name").asString, handler)
    }

    /**
     * The methods in the form's code that can handle this event (VS's drop-down on an event): same parameter list.
     * The current handler first, so an untouched cell keeps it.
     */
    private fun matchingHandlers(row: Row.Item): List<String> {
        val e = editor ?: return listOf(row.value)
        val params = row.data.get("parameters")?.asString ?: return listOf(row.value)
        val code = e.session.files()[e.form.code.path] ?: return listOf(row.value)
        val types = params.split(',').map { it.trim().substringBefore(' ').substringAfterLast('.') }
        val found = Regex("""\bvoid\s+(\w+)\s*\(([^)]*)\)""").findAll(code).filter { m ->
            m.groupValues[2].split(',').map { it.trim().split(Regex("\\s+")).dropLast(1).joinToString("").removeSuffix("?").substringAfterLast('.') } == types
        }.map { it.groupValues[1] }.toList()
        return (listOf(row.value) + found).distinct()
    }

    /** "…" editors that open a dialog: collections, images, the Anchor arms, multi-line text. */
    private fun openDialog(row: Row.Item) {
        val kind = row.data.get("editor")?.asString
        val name = row.data.get("name").asString
        when (kind) {
            "image" -> {
                val d = FileChooserDescriptor(true, false, false, false, false, false).withTitle("Choose a Picture for $name")
                    .withFileFilter { it.extension?.lowercase() in setOf("png", "jpg", "jpeg", "gif", "bmp", "ico") }
                val file = FileChooser.chooseFile(d, project, null) ?: return
                editor?.session?.applyOps(JsonArray().apply { add(op("setImage", "prop" to name, "value" to file.path)) }, "Choose Image")
            }
            "collection" -> {
                val items = row.data.getAsJsonArray("items")?.map { it.asString } ?: emptyList()
                val ids = row.data.getAsJsonArray("itemIds")?.map { it.asString }
                val text = TextDialog(project, "$name Collection Editor", "Enter one item per line:", items.joinToString("\n"), multiLine = true).ask() ?: return
                val lines = text.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
                val values = JsonArray().apply { lines.forEach { add(it) } }
                val o = op("setItems", "prop" to name, "values" to values)
                if (ids != null) o.add("ids", JsonArray().apply { lines.indices.forEach { add(if (it < ids.size) ids[it] else "") } })
                editor?.session?.applyOps(JsonArray().apply { add(o) }, "Edit Items")
            }
            "flags" -> {
                val options = row.data.getAsJsonArray("options")?.map { it.asString }?.filter { it != "None" } ?: return
                val current = row.value.split(',').map { it.trim() }.toSet()
                val picked = FlagsDialog(project, name, options, current).ask() ?: return
                setValue(row, if (picked.isEmpty()) "None" else picked.joinToString(", "))
            }
            else -> {
                val type = row.data.get("type")?.asString
                val picked = when {
                    name == "Dock" -> PopupEditors.DockDialog(project, row.value).ask()
                    type == "ContentAlignment" -> PopupEditors.AlignmentDialog(project, row.value).ask()
                    kind == "color" -> PopupEditors.ColorDialog(project, row.value,
                        row.data.getAsJsonArray("options")?.map { it.asString } ?: emptyList(),
                        row.data.getAsJsonArray("systemOptions")?.map { it.asString } ?: emptyList(),
                    ) { v -> (windowsSystemColors[v] ?: colorByName(v))?.let { h -> try { Color.decode(h) } catch (_: Exception) { null } } }.ask()
                    kind == "font" -> PopupEditors.FontDialog(project, row.value, row.data.getAsJsonArray("options")?.map { it.asString } ?: emptyList()).ask()
                    // A multi-line Text gets Windows line breaks ("Sign\r\nIn"), as Visual Studio's string editor writes it.
                    name == "Text" -> TextDialog(project, name, "$name:", row.value, multiLine = true).ask()
                        ?.replace("\r\n", "\n")?.replace("\n", "\r\n")
                    else -> TextDialog(project, name, "$name:", row.value, multiLine = name == "Lines").ask()
                } ?: return
                setValue(row, picked)
            }
        }
    }

    // ---- the grid ---------------------------------------------------------------------------------------------

    private inner class Model : AbstractTableModel() {
        override fun getRowCount() = shown.size
        override fun getColumnCount() = 2
        override fun getColumnName(c: Int) = if (c == 0) "Property" else "Value"
        override fun getValueAt(r: Int, c: Int): Any = shown[r]
        override fun isCellEditable(r: Int, c: Int): Boolean {
            (shown[r] as? Row.Part)?.let { return c == 1 && it.parent.data.get("readOnly")?.asBoolean != true }
            val row = shown[r] as? Row.Item ?: return false
            if (c != 1) return false
            if (events) return true
            val kind = row.data.get("editor")?.asString
            return row.data.get("readOnly")?.asBoolean != true && kind != "readonly" && kind != "image" && kind != "collection"
        }
    }

    private val categoryBand: Color get() = JBColor.namedColor("Table.stripeColor", UIUtil.getPanelBackground())

    private inner class Grid : JBTable(tableModel) {
        init {
            setShowGrid(true)
            gridColor = JBColor.border()
            tableHeader = null
            rowHeight = JBUI.scale(20)
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            putClientProperty("terminateEditOnFocusLost", true)
            selectionModel.addListSelectionListener { showDescription(shown.getOrNull(selectedRow).let { it as? Row.Item ?: (it as? Row.Part)?.parent }) }
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val r = rowAtPoint(e.point).takeIf { it >= 0 } ?: return
                    when (val row = shown[r]) {
                        is Row.Category -> if (e.clickCount == 1) { if (!collapsed.add(row.name)) collapsed.remove(row.name); rebuild() }
                        is Row.Part -> {}
                        is Row.Item -> if (e.clickCount == 1 && columnAtPoint(e.point) == 0 && !events && partsOf(row) != null && e.x < JBUI.scale(if (categorized) 16 else 14)) {
                                // The ⊞ / ⊟ in front of Location, Size, Padding…
                                val n = row.data.get("name").asString
                                if (!expanded.add(n)) expanded.remove(n)
                                rebuild()
                            } else if (e.clickCount == 2 && columnAtPoint(e.point) == 0 && events) activateEvent(row)
                            else if (e.clickCount == 2 && columnAtPoint(e.point) == 0) {
                                // VS: double-clicking a bool or enum name moves to the next value.
                                val options = row.data.getAsJsonArray("options")?.map { it.asString }
                                    ?: if (row.data.get("editor")?.asString == "bool") listOf("True", "False") else null
                                if (options != null && options.isNotEmpty() && isCellEditable(r, 1)) setValue(row, options[(options.indexOf(row.value) + 1) % options.size])
                            }
                    }
                }
                override fun mousePressed(e: MouseEvent) = popup(e)
                override fun mouseReleased(e: MouseEvent) = popup(e)
                private fun popup(e: MouseEvent) {
                    if (!e.isPopupTrigger) return
                    val r = rowAtPoint(e.point).takeIf { it >= 0 } ?: return
                    val row = shown[r] as? Row.Item ?: return
                    setRowSelectionInterval(r, r)
                    JBPopupMenu().apply {
                        add(JBMenuItem("Reset").apply {
                            isEnabled = !events && row.data.get("modified")?.asBoolean == true && row.data.get("readOnly")?.asBoolean != true
                            addActionListener { reset(row) }
                        })
                        add(JBMenuItem("Description").apply { addActionListener { showDescription(row) } })
                    }.show(this@Grid, e.x, e.y)
                }
            })
            columnModel.getColumn(0).preferredWidth = JBUI.scale(140)
            columnModel.getColumn(1).preferredWidth = JBUI.scale(160)
        }

        override fun getCellRenderer(row: Int, column: Int) = renderer
        override fun getCellEditor(row: Int, column: Int): TableCellEditor = when (val r = shown[row]) {
            is Row.Part -> PartEditor(r)
            else -> ValueEditor(r as Row.Item)
        }
    }

    private val renderer = object : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(t: JTable, value: Any?, selected: Boolean, focus: Boolean, r: Int, c: Int): Component {
            val row = value as Row
            val comp = super.getTableCellRendererComponent(t, "", selected, false, r, c) as DefaultTableCellRenderer
            comp.icon = null
            comp.border = JBUI.Borders.empty(0, 4)
            comp.foreground = if (selected) t.selectionForeground else t.foreground
            when (row) {
                is Row.Category -> {
                    comp.text = if (c == 0) (if (row.name in collapsed) "⊞ " else "⊟ ") + row.name else ""
                    comp.font = t.font.deriveFont(Font.BOLD)
                    if (!selected) comp.background = categoryBand
                }
                is Row.Part -> {
                    comp.font = t.font
                    comp.text = if (c == 0) row.label else row.value
                    if (c == 0) comp.border = JBUI.Borders.emptyLeft(JBUI.scale(if (categorized) 30 else 18))
                }
                is Row.Item -> {
                    comp.font = t.font
                    if (c == 0) {
                        val toggle = if (!events && partsOf(row) != null) (if (row.data.get("name").asString in expanded) "⊟ " else "⊞ ") else ""
                        comp.text = toggle + row.name
                        comp.border = JBUI.Borders.emptyLeft(if (toggle.isNotEmpty()) JBUI.scale(if (categorized) 2 else 0) else if (categorized) JBUI.scale(16) else JBUI.scale(4))
                    } else {
                        comp.text = row.value
                        // Visual Studio shows values that differ from the default in bold.
                        if (!events && row.data.get("modified")?.asBoolean == true) comp.font = t.font.deriveFont(Font.BOLD)
                        if (row.data.get("editor")?.asString == "color") comp.icon = swatch(row)
                        if (row.data.get("readOnly")?.asBoolean == true || row.data.get("editor")?.asString == "readonly") comp.foreground = UIUtil.getInactiveTextColor()
                    }
                }
            }
            return comp
        }
    }

    /** System colours as Windows 10/11 paints them (the designer's form uses the same table), not .NET-on-Linux's XP one. */
    private val windowsSystemColors = mapOf(
        "Control" to "#f0f0f0", "ControlDark" to "#a0a0a0", "ControlDarkDark" to "#696969", "ControlLight" to "#e3e3e3",
        "ControlLightLight" to "#ffffff", "ControlText" to "#000000", "Window" to "#ffffff", "WindowText" to "#000000",
        "ButtonFace" to "#f0f0f0", "ButtonHighlight" to "#ffffff", "ButtonShadow" to "#a0a0a0", "Highlight" to "#0078d7",
        "HighlightText" to "#ffffff", "GrayText" to "#6d6d6d", "Info" to "#ffffe1", "InfoText" to "#000000", "Menu" to "#f0f0f0",
        "MenuText" to "#000000", "MenuBar" to "#f0f0f0", "ActiveBorder" to "#b4b4b4", "ActiveCaption" to "#99b4d1",
        "InactiveCaption" to "#bfcddb", "AppWorkspace" to "#ababab", "HotTrack" to "#0066cc", "ScrollBar" to "#c8c8c8",
        "WindowFrame" to "#646464", "Desktop" to "#000000", "InactiveBorder" to "#f4f7fc",
    )

    private fun swatch(row: Row.Item): Icon? {
        val hex = windowsSystemColors[row.value] ?: row.data.getAsJsonObject("swatches")?.get(row.value)?.asString ?: colorByName(row.value) ?: return null
        val color = try { Color.decode(hex) } catch (_: Exception) { return null }
        return object : Icon {
            override fun getIconWidth() = JBUI.scale(16)
            override fun getIconHeight() = JBUI.scale(12)
            override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
                g.color = color; g.fillRect(x, y, iconWidth - 1, iconHeight - 1)
                g.color = JBColor.border(); g.drawRect(x, y, iconWidth - 1, iconHeight - 1)
            }
        }
    }

    private fun colorByName(name: String): String? = when {
        name.matches(Regex("""\d+,\s*\d+,\s*\d+""")) -> name.split(',').map { it.trim().toInt() }.let { (r, g, b) -> String.format("#%02x%02x%02x", r, g, b) }
        else -> try { (java.awt.Color::class.java.getField(name.lowercase()).get(null) as? Color)?.let { String.format("#%02x%02x%02x", it.red, it.green, it.blue) } } catch (_: Exception) { null }
    }

    /** A part of an expanded value (X, Width, Left…): a number. */
    private inner class PartEditor(private val part: Row.Part) : AbstractCellEditor(), TableCellEditor {
        private val field = JBTextField(part.value).apply { addActionListener { stopCellEditing() } }
        override fun getCellEditorValue(): Any = field.text
        override fun stopCellEditing(): Boolean { val v = field.text; val ok = super.stopCellEditing(); if (ok) setPart(part, v); return ok }
        override fun getTableCellEditorComponent(t: JTable, value: Any?, selected: Boolean, r: Int, c: Int): Component = field
    }

    /** The value cell's editor: a text box, or a drop-down for True/False, enums, colours and components, with "…" where VS has one. */
    private inner class ValueEditor(private val row: Row.Item) : AbstractCellEditor(), TableCellEditor {
        private val kind = row.data.get("editor")?.asString
        private val options: List<String>? = when {
            events -> matchingHandlers(row)
            kind == "bool" -> listOf("True", "False")
            kind == "color" -> (listOfNotNull(row.value) + (row.data.getAsJsonArray("systemOptions")?.map { it.asString } ?: emptyList()) + (row.data.getAsJsonArray("options")?.map { it.asString } ?: emptyList())).distinct()
            kind == "enum" || kind == "component" || kind == "font" -> row.data.getAsJsonArray("options")?.map { it.asString }
            // Text the host offers choices for (DisplayMember / ValueMember / DataMember / DataPropertyName: the data
            // source's fields, as VS lists them); typing something else stays possible.
            else -> row.data.getAsJsonArray("options")?.map { it.asString }?.takeIf { it.isNotEmpty() }
        }
        private val freeText = kind == "font" || events || kind !in setOf("bool", "color", "enum", "component")
        private val field = JBTextField(row.value)
        private val combo = options?.let { ComboBox(it.toTypedArray()).apply { isEditable = freeText; selectedItem = row.value } }
        private val panel = JPanel(BorderLayout())

        init {
            if (combo != null) {
                combo.addActionListener { if (combo.isPopupVisible || !combo.isEditable) stopCellEditing() }
                panel.add(combo, BorderLayout.CENTER)
            } else {
                field.addActionListener { stopCellEditing() }
                panel.add(field, BorderLayout.CENTER)
            }
            val popup = !events && (row.data.get("name").asString == "Dock" || row.data.get("type")?.asString == "ContentAlignment" || kind == "color" || kind == "font")
            if (popup) panel.add(PopupEditors.cellButton("▾") { cancelCellEditing(); openDialog(row) }, BorderLayout.EAST)
            if (!events && (kind == "flags" || kind == "text" && (row.data.get("name").asString == "Text" || row.data.get("name").asString == "Lines"))) {
                panel.add(JButton("…").apply {
                    margin = JBUI.emptyInsets(); preferredSize = Dimension(JBUI.scale(22), JBUI.scale(18))
                    addActionListener { cancelCellEditing(); openDialog(row) }
                }, BorderLayout.EAST)
            }
        }

        override fun getCellEditorValue(): Any = (combo?.let { (if (it.isEditable) it.editor.item else it.selectedItem)?.toString() } ?: field.text)
        override fun stopCellEditing(): Boolean {
            val value = cellEditorValue as String
            val ok = super.stopCellEditing()
            if (ok) setValue(row, value)
            return ok
        }
        override fun getTableCellEditorComponent(t: JTable, value: Any?, selected: Boolean, r: Int, c: Int): Component = panel
    }

    init {
        // Collections, images and the like open their editor from a double-click on the value, as VS's "…" does.
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val r = table.rowAtPoint(e.point).takeIf { it >= 0 } ?: return
                val row = shown[r] as? Row.Item ?: return
                val kind = row.data.get("editor")?.asString
                if (!events && table.columnAtPoint(e.point) == 1 && (kind == "image" || kind == "collection")) openDialog(row)
            }
        })
    }

    private class TextDialog(project: Project, title: String, private val prompt: String, initial: String, private val multiLine: Boolean) : DialogWrapper(project, true) {
        private val area = JBTextArea(initial, if (multiLine) 10 else 1, 40)
        private val line = JBTextField(initial, 40)
        init { this.title = title; init() }
        override fun createCenterPanel(): JComponent = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(JBLabel(prompt), BorderLayout.NORTH)
            add(if (multiLine) JBScrollPane(area) else line, BorderLayout.CENTER)
        }
        override fun getPreferredFocusedComponent(): JComponent = if (multiLine) area else line
        fun ask(): String? = if (showAndGet()) (if (multiLine) area.text else line.text) else null
    }

    /** The Anchor editor: the four sides, as VS's cross of arms. */
    private class FlagsDialog(project: Project, private val name: String, private val options: List<String>, current: Set<String>) : DialogWrapper(project, true) {
        private val boxes = options.associateWith { JBCheckBox(it, it in current) }
        init { title = name; init() }
        override fun createCenterPanel(): JComponent {
            val p = JPanel(GridBagLayout())
            val at = mapOf("Top" to (1 to 0), "Left" to (0 to 1), "Right" to (2 to 1), "Bottom" to (1 to 2))
            if (options.toSet() == at.keys) {
                for ((k, pos) in at) p.add(boxes[k], GridBagConstraints().apply { gridx = pos.first; gridy = pos.second; insets = JBUI.insets(4) })
                p.add(JBLabel("▣"), GridBagConstraints().apply { gridx = 1; gridy = 1 })
            } else options.forEachIndexed { i, o -> p.add(boxes[o], GridBagConstraints().apply { gridx = 0; gridy = i; anchor = GridBagConstraints.WEST }) }
            return p
        }
        fun ask(): List<String>? = if (showAndGet()) options.filter { boxes[it]!!.isSelected } else null
    }
}
