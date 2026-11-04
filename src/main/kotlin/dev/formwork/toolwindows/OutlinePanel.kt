package dev.formwork.toolwindows

import com.google.gson.JsonObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import dev.formwork.editor.ActiveDesigner
import dev.formwork.editor.DesignerEditor
import dev.formwork.editor.DesignerListener
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/** Document Outline (VS: View → Other Windows → Document Outline): the form's controls as a tree; click to select. */
class OutlinePanel(private val project: Project, parent: Disposable) : JPanel(BorderLayout()), DesignerListener {
    private data class Entry(val id: String, val type: String)

    private val root = DefaultMutableTreeNode()
    private val model = DefaultTreeModel(root)
    private val tree = Tree(model)
    private var updating = false
    private var view: JsonObject? = null

    init {
        tree.isRootVisible = true
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(t: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
                val e = (value as? DefaultMutableTreeNode)?.userObject as? Entry ?: return
                append(e.id.substringAfterLast('.').ifEmpty { view?.get("className")?.asString ?: "Form" }, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append("  " + e.type.substringAfterLast('.'), SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }
        tree.addTreeSelectionListener {
            if (updating) return@addTreeSelectionListener
            val ids = tree.selectionPaths?.mapNotNull { ((it.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Entry)?.id } ?: return@addTreeSelectionListener
            ActiveDesigner.get(project).editor?.select(ids)
        }
        add(JBScrollPane(tree), BorderLayout.CENTER)
        ActiveDesigner.get(project).addListener(this, parent)
        activeChanged(ActiveDesigner.get(project).editor)
    }

    override fun activeChanged(editor: DesignerEditor?) {
        view = editor?.session?.lastView
        rebuild()
        tree.emptyText.text = if (editor == null) "Open a form in the designer to see its outline." else ""
    }

    override fun viewChanged(editor: DesignerEditor, view: JsonObject) { this.view = view; rebuild(); selectionChanged(editor, editor.session.selection) }

    override fun selectionChanged(editor: DesignerEditor, ids: List<String>) {
        updating = true
        try {
            val paths = TreeUtil.treePathTraverser(tree).preOrderDfsTraversal().filter { p -> ((p.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? Entry)?.id in ids }.toList()
            tree.selectionPaths = paths.toTypedArray()
            paths.lastOrNull()?.let { tree.scrollPathToVisible(it) }
        } finally { updating = false }
    }

    private fun rebuild() {
        val v = view
        root.removeAllChildren()
        root.userObject = Entry("", v?.get("rootType")?.asString ?: "")
        if (v != null) {
            val nodes = HashMap<String, DefaultMutableTreeNode>()
            nodes[""] = root
            val items = v.getAsJsonArray("items")?.map { it.asJsonObject }?.filter { it.get("kind")?.asString != "dropdown" } ?: emptyList()
            // Parents first (the host lists them in z-order; a child can come before its parent).
            val pending = items.toMutableList()
            var guard = 0
            while (pending.isNotEmpty() && guard++ < 50) {
                val it = pending.iterator()
                while (it.hasNext()) {
                    val o = it.next()
                    val parent = nodes[o.get("parent")?.asString ?: ""] ?: continue
                    val node = DefaultMutableTreeNode(Entry(o.get("id").asString, o.get("type")?.asString ?: ""))
                    parent.add(node); nodes[o.get("id").asString] = node; it.remove()
                }
            }
            v.getAsJsonArray("tray")?.forEach { val o = it.asJsonObject; root.add(DefaultMutableTreeNode(Entry(o.get("id").asString, o.get("type")?.asString ?: ""))) }
        }
        updating = true
        try { model.reload(); TreeUtil.expandAll(tree) } finally { updating = false }
    }
}
