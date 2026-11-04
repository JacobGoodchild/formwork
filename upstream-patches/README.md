# Patches on top of NetForms v0.1.5

The submodule `external/NetForms` stays pinned to the public tag `v0.1.5`. These patches are applied
to its working tree by `host/apply-patches.sh` (Gradle runs it before building the host).
They're kept small so they can be offered upstream.

## 0001-designer-file-overrides.patch

**What:** adds `DesignSurface.ReadFileOverride` and `DesignSurface.WriteFileOverride` (static
hooks, null by default, so NetForms behaves exactly as before unless a host sets them).

**Why:** NetForms' designer reads the form's files from disk before every edit and writes them
straight back to disk. That suits VS Code (its extension saves your open editors first), but in
Rider it would bypass Rider's documents: no Ctrl+Z, no `*` for unsaved changes, and a risk of
overwriting unsaved typing. With the hooks, Formwork's host reads from Rider's buffers and hands
every write back to Rider as text.

**Behaviour change for NetForms users:** none (hooks unset = old behaviour).

## 0002-designer-webview-vs-toolbox.patch

**What:** the webview toolbox behaves like Visual Studio's: every group starts with **Pointer**;
a click *selects* a tool (it stays highlighted), then a click on the form places it at its default
size or a drawn rectangle gives its size; afterwards the toolbox goes back to Pointer; Escape
cancels. Double-click adds to the selected container (NetForms' old single-click behaviour).
Dragging a tool onto the form now uses mouse events instead of HTML5 drag-and-drop.
Adds `window.NetFormsDesigner` (`arm`, `dropAt`, `addToSelection`, `armed`) so an editor with its
own native toolbox can drive the canvas.

**Why:** Rider's embedded browser (JCEF, out-of-process mode) doesn't deliver HTML5 drag-and-drop
from synthetic or OSR input reliably, so dragging from the toolbox did nothing. And VS's
click-then-place behaviour is what Jacob knows.

**Behaviour change for NetForms users:** yes, deliberately: single-click no longer adds
immediately (it arms the tool, as VS does).

## 0003-designer-vs-menu-item-names.patch

**What:** a menu item added by typing its text (the "Type Here" box, or `setItems` on a strip's
`Items` / an item's `DropDownItems`) is named from its text, as Visual Studio names it:
`&File` → `fileToolStripMenuItem`, `E&xit` → `exitToolStripMenuItem`, `Save &As...` →
`saveAsToolStripMenuItem`. Other new strip items keep the usual names (`toolStripButton1`,
`toolStripSeparator1`, `toolStripStatusLabel1`). The naming function NetForms already used for
"Insert Standard Items" is shared.

**Why:** NetForms named typed menu items `toolStripMenuItem1, 2, …`, which differs from the code
Visual Studio writes for the same clicks (brief, group E: "New controls get Visual Studio's names").
