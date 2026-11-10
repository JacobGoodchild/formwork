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
click-then-place behaviour is what Visual Studio users know.

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
it on, so the designer shows buttons as Visual Studio does on Windows 11.

**Behaviour change for NetForms users:** none (off by default). Note: a NetForms app *running* on Linux still uses
the Windows 10 look — the designer shows the Windows 11 look.

## 0005-designer-strip-item-captions.patch

**What:** each tool strip item in the designer view (`kind: "item"`: menu bar items, drop-down items, ToolStrip and
StatusStrip items) carries its caption in a new `text` field ("-" for a separator), as the collection editor shows it.

**Why:** the designer page reorders menu items by dragging (patch 0002) by sending the parent's whole `Items` /
`DropDownItems` list back in the new order through `setItems`, which needs every item's caption.

**Behaviour change for NetForms users:** none (an extra field in the view JSON).

## 0006-designer-baselines-locks-menu-moves.patch

Three Visual Studio designer behaviours (on top of 0001–0005; it also changes lines 0002 added, so it must come after it):

1. **Text-baseline snaplines (VS's magenta line).** Each control with a line of text gets `baseline` in the view
   (pixels from its top), computed as VS's control designers compute theirs (dotnet/winforms, MIT, read as the spec:
   `DesignerUtils.GetTextBaseline` plus the offsets of `ButtonBaseDesigner`, `LabelDesigner`, `TextBoxBaseDesigner`,
   `ComboBoxDesigner`, `DateTimePickerDesigner`, `UpDownBaseDesigner`). For Segoe UI it uses Segoe UI's own Windows
   metrics (2210 / 514 per 2048), because the font that stands in for it on Linux has a squeezed line. `geometry.js`
   snaps a dragged control's baseline onto a neighbour's and draws the magenta guide; Ctrl+Up/Down stop there too.
   Checked: Segoe UI 9 gives text box 17, button 17, auto-size label 14 — so a label 3 px below its text box (as VS's
   own templates have it) lines up exactly.
2. **The Locked property.** As VS, kept in the form's `.resx`, never in the code:
   `<metadata name="button1.Locked" type="System.Boolean, mscorlib, …"><value>True</value></metadata>` (`$this.Locked`
   for the form). Shown in the grid's Design category; `setProp Locked` writes it, False takes the entry out, a rename
   carries it over, a deleted control's goes. A locked control is `locked: true` and not movable or resizable in the
   view; the page draws a padlock on it. Format → Lock Controls sets every control and the form to the opposite of the
   form's Locked (as VS's command does), in one undo step. Before, Lock Controls only locked the page's view and wasn't saved.
3. **Moving a menu item into another menu.** New edit `moveItem` (`id`, `parent`, optional `index`): the item leaves its
   collection and goes into the parent's (`Items` of a strip, `DropDownItems` of a menu item), refused for its own
   sub-menus and for item kinds the target doesn't take. The page: drag an item before/after an item of another open
   menu or of the menu bar (blue bar), or onto the middle of a menu's title (or an item with a sub-menu) to put it at
   the end of that menu (outlined).

Also `host/apply-patches.sh` now copes with a patch whose lines a later patch changed again.

**Behaviour change for NetForms users:** Lock Controls is now saved in the .resx (as VS); otherwise new fields and a new edit.

## 0007-designer-sharp-pictures-on-scaled-screens.patch

On a scaled screen (150–200 %), the page used to show the form picture at 1× and the browser stretched it, so the
text was blurry. The page now sends its screen density (`devicePixelRatio`, times the page zoom) in `ready` and in a
new `scale` message; the host paints the form and its open drop-downs at that density (`DrawToBitmap` with a scale,
clamped to 1–4 in quarter steps) and the view says which (`scale`). The page still shows the picture at the form's
own size, so positions and clicks are unchanged.

## 0008-designer-ide-properties-window.patch

The canvas right-click menu's **Properties** also posts `showProperties`, so an IDE with its own Properties window can
show and focus it (as VS's F4). The page's own grid still switches to Properties as before.

## 0009-avalonia-first-show-size-on-scaled-screens.patch

**A bug in the running app, not the designer.** On Linux with a scaled screen (checked with Cinnamon's window manager,
muffin, at 2×), the first time a window opened it lost the size and position set before `Show`: Avalonia first creates
it at its default size (¾ of the screen, in device pixels), reports that as a resize before `Opened`, and NetForms took
it as the form's new size. So a ComboBox's **first** drop-down filled the screen (later ones were fine, as the popup
window is reused), and forms opened too big or in the wrong place. Now a resize before the window has opened is
ignored, and on `Opened` the asked-for client size (normal windows only) and, if the scaling changed, the asked-for
position are applied again. Checked: ComboBox drop-down (first and second open), ContextMenuStrip, the main form,
at 2× and at 1× (unchanged there).

**Note:** Penguin Forms' designer doesn't open windows, so this patch only matters for apps. Apps get NetForms from
NuGet (0.1.5), so they'll have this fix only once NetForms includes it; until then the first drop-down on a scaled
screen can still be too big. This patch is written to be offered to NetForms as it is.
