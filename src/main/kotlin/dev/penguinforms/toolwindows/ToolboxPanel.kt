package dev.penguinforms.toolwindows

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SearchTextField
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import dev.penguinforms.editor.ActiveDesigner
import dev.penguinforms.editor.DesignerEditor
import dev.penguinforms.editor.DesignerListener
import dev.penguinforms.editor.ToolboxModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.Base64
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * The Toolbox tool window, laid out like Visual Studio's: "Search Toolbox", then collapsible groups (All Windows
 * Forms, Common Controls, …, General) whose first item is the Pointer. Click a tool, then click or draw on the form;
 * double-click to add it to the selected container; or drag it onto the form. After placing, back to Pointer.
 */
class ToolboxPanel(private val project: Project, parent: Disposable) : JPanel(BorderLayout()), DesignerListener {
    sealed class Node {
        data class Group(val name: String, val library: Boolean) : Node()
        data class Pointer(val group: String) : Node()
        data class Tool(val type: String, val name: String, val icon: Icon?, val tray: Boolean, val unavailable: String?) : Node()
        object Empty : Node()
    }

    private val root = DefaultMutableTreeNode()
    private val model = DefaultTreeModel(root)
    private val tree = Tree(model)
    private val search = SearchTextField(false)
    private var categories = JsonArray()
    private val expanded = HashSet<String>(listOf("Common Controls"))
    private var armed: String? = null
    private val iconCache = HashMap<String, Icon?>()

    private lateinit var hintCard: HintCard

    init {
        search.textEditor.emptyText.text = "Search Toolbox"
        search.textEditor.accessibleContext.accessibleName = "Search Toolbox"
        search.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = rebuild()
        })
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = Renderer()
        tree.addTreeExpansionListener(object : javax.swing.event.TreeExpansionListener {
            override fun treeExpanded(e: javax.swing.event.TreeExpansionEvent) { groupOf(e.path)?.let { if (search.text.isBlank()) expanded.add(it) } }
            override fun treeCollapsed(e: javax.swing.event.TreeExpansionEvent) { groupOf(e.path)?.let { if (search.text.isBlank()) expanded.remove(it) } }
        })
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val node = nodeAt(e) ?: return
                val editor = ActiveDesigner.get(project).editor ?: run {
                    if (node !is Node.Group) com.intellij.openapi.diagnostic.logger<ToolboxPanel>().warn("Penguin Forms add: Toolbox click, but no form designer is active")
                    return
                }
                when (node) {
                    // A click on the group's name opens or closes it, as in VS (the ▷ arrow is the tree's own).
                    is Node.Group -> if (e.clickCount == 1 && tree.getClosestPathForLocation(e.x, e.y)?.let { p -> tree.getPathBounds(p)?.let { b -> e.x >= b.x } } == true) toggle(e)
                    is Node.Pointer -> { armed = null; editor.armTool(null) }
                    is Node.Tool -> if (node.unavailable == null) {
                        if (e.clickCount >= 2) { armed = null; editor.addToSelection(node.type); selectPointer() }
                        else { armed = node.type; editor.armTool(node.type) }
                    }
                    else -> {}
                }
            }
        })
        tree.dragEnabled = true
        tree.transferHandler = object : TransferHandler() {
            override fun getSourceActions(c: JComponent) = COPY
            override fun createTransferable(c: JComponent): Transferable? {
                val node = (tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Node.Tool ?: return null
                return StringSelection(DRAG_PREFIX + node.type)
            }
        }
        add(JPanel(BorderLayout()).apply { border = JBUI.Borders.empty(4); add(search, BorderLayout.CENTER) }, BorderLayout.NORTH)
        hintCard = HintCard(JBScrollPane(tree).apply { border = JBUI.Borders.empty() })
        add(hintCard, BorderLayout.CENTER)
        ActiveDesigner.get(project).addListener(this, parent)
        activeChanged(ActiveDesigner.get(project).editor)
    }

    private fun groupOf(path: TreePath): String? = ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Node.Group)?.name
    private fun nodeAt(e: MouseEvent): Node? = (tree.getClosestPathForLocation(e.x, e.y)?.takeIf { tree.getPathBounds(it)?.let { b -> e.y >= b.y && e.y < b.y + b.height } == true }
        ?.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Node

    private fun toggle(e: MouseEvent) {
        val path = tree.getClosestPathForLocation(e.x, e.y) ?: return
        if (tree.isExpanded(path)) tree.collapsePath(path) else tree.expandPath(path)
    }

    override fun activeChanged(editor: DesignerEditor?) {
        categories = editor?.session?.lastToolbox ?: JsonArray()
        rebuild()
        tree.emptyText.text = "Loading…"
        hintCard.showHint(if (editor == null) "Open a form in the designer to use the Toolbox." else null)
    }

    override fun toolboxChanged(editor: DesignerEditor, categories: JsonArray, notice: String?) {
        this.categories = categories
        rebuild()
    }

    override fun toolChanged(editor: DesignerEditor, type: String?) {
        armed = type
        if (type == null) selectPointer()
        tree.repaint()
    }

    private fun icon(item: JsonObject): Icon? {
        val data = item.get("icon")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
        return iconCache.getOrPut(data) {
            try {
                val image = javax.imageio.ImageIO.read(Base64.getDecoder().decode(data).inputStream()) ?: return@getOrPut null
                val size = JBUI.scale(16)
                ImageIcon(if (image.width == size) image else image.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH))
            } catch (_: Exception) { null }
        }
    }

    private fun rebuild() {
        val filter = search.text.trim().lowercase()
        root.removeAllChildren()
        for (c in categories) {
            val cat = c.asJsonObject
            val name = cat.get("name")?.asString ?: continue
            val items = cat.getAsJsonArray("items")?.map { it.asJsonObject } ?: emptyList()
            val shown = items.filter { filter.isEmpty() || (it.get("name")?.asString ?: "").lowercase().contains(filter) }
            if (filter.isNotEmpty() && shown.isEmpty()) continue
            val group = DefaultMutableTreeNode(Node.Group(name, cat.get("library")?.asBoolean == true))
            if (items.isEmpty()) group.add(DefaultMutableTreeNode(Node.Empty))
            else {
                group.add(DefaultMutableTreeNode(Node.Pointer(name)))
                for (it in shown) group.add(DefaultMutableTreeNode(Node.Tool(
                    it.get("type").asString, it.get("name").asString, icon(it), it.get("tray")?.asBoolean == true,
                    it.get("unavailable")?.takeIf { u -> u.isJsonPrimitive }?.asString,
                )))
            }
            root.add(group)
        }
        model.reload()
        for (i in 0 until root.childCount) {
            val g = root.getChildAt(i) as DefaultMutableTreeNode
            if (filter.isNotEmpty() || (g.userObject as Node.Group).name in expanded) tree.expandPath(TreePath(arrayOf(root, g)))
        }
        if (armed == null) selectPointer()
    }

    private fun selectPointer() {
        val path = TreeUtil.treePathTraverser(tree).preOrderDfsTraversal().firstOrNull { p ->
            (p.lastPathComponent as? DefaultMutableTreeNode)?.userObject is Node.Pointer && tree.isVisible(p)
        }
        if (path != null) tree.selectionPath = path else tree.clearSelection()
    }

    private inner class Renderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
            when (val node = (value as? DefaultMutableTreeNode)?.userObject) {
                is Node.Group -> append(node.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                is Node.Pointer -> { icon = PointerIcon; append("Pointer") }
                is Node.Tool -> {
                    icon = node.icon ?: ToolboxIcons.forControl(node.name)
                    append(node.name, if (node.unavailable != null) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    toolTipText = node.unavailable ?: node.type
                }
                Node.Empty -> append("There are no usable controls in this group. Drag an item onto this text to add it to the toolbox.", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                else -> {}
            }
        }
    }

    /** VS's Pointer: a small arrow, drawn so it follows the theme's text colour. */
    private object PointerIcon : Icon {
        override fun getIconWidth() = JBUI.scale(16)
        override fun getIconHeight() = JBUI.scale(16)
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.translate(x, y); g2.scale(JBUI.scale(16) / 16.0, JBUI.scale(16) / 16.0)
            val arrow = java.awt.Polygon(intArrayOf(4, 4, 7, 9, 11, 9, 12), intArrayOf(2, 13, 10, 14, 13, 9, 9), 7)
            g2.color = UIUtil.getLabelForeground(); g2.fillPolygon(arrow)
            g2.dispose()
        }
    }

    private object BlankIcon : Icon {
        override fun getIconWidth() = JBUI.scale(16)
        override fun getIconHeight() = JBUI.scale(16)
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {}
    }

    companion object {
        const val DRAG_PREFIX = "penguinforms-tool:"
        fun draggedTool(t: Transferable): String? = try {
            (t.getTransferData(DataFlavor.stringFlavor) as? String)?.takeIf { it.startsWith(DRAG_PREFIX) }?.removePrefix(DRAG_PREFIX)
        } catch (_: Exception) { null }
    }
}
