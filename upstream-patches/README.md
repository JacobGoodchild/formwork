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
overwriting unsaved typing. With the hooks, Penguin Forms' host reads from Rider's buffers and hands
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

## 0004-theme-windows11-buttons.patch

**What:** `Theme.Windows11Buttons` (internal, off by default): push buttons get Windows 11's white face (#FDFDFD)
and light grey border (#D0D0D0) instead of Windows 10's grey (#E1E1E1 / #ADADAD). Penguin Forms' designer host turns
it on, so the designer shows buttons as Visual Studio does on Windows 11 (Jacob's screenshots).

**Behaviour change for NetForms users:** none (off by default). Note: a NetForms app *running* on Linux still uses
the Windows 10 look — the designer shows the college (Windows 11) look.
