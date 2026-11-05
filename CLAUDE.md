# CLAUDE.md — Formwork

Read this file and `git log` first, every session. Never redo committed work.
Part 1 is the progress log (kept up to date). Part 2 is a copy of Jacob's brief.

Wake-ups: scheduled with `send_later` (one ~60 min ahead after each commit, plus a backup
chain at 21:05, 22:35, 00:05, 01:35, 03:05, 04:35, 06:05 UK). Deadline: 07:00 UK, 5 Nov 2026.

---

# Part 1 — Progress log

## Status

**Current stage:** Stage 2 (look and feel like VS). Stage 1 finished 21:00 UK (story works end to end; review verdict below). Stage 0 finished 20:12: **GO**.

### Stage 0 result (20:12)
The pipeline works: in the sandbox IDE, double-clicking `Form1.cs` opens a **Design** tab; the
JCEF page runs NetForms' webview; Formwork.Host (started by the plugin with an argument list)
reads the form from the editor's buffers and the form is drawn by NetForms in light Windows colours.
Screenshot: `docs/screenshots/stage0-designer-in-ide-dark.png`. 8 unit tests pass
(`./gradlew test`: host protocol, buffers-not-disk, broken file error, close; diff; handler cursor).

Honest comparison with Jacob's VS screenshots: the form's **title bar is a flat strip** (no icon,
no min/max/close buttons); the toolbox is one long "All Windows Forms" list (VS has groups); no
description pane under Properties; top tab showed `Form1.cs` instead of `Form1.cs [Design]`
(refresh missing). All are Stage 1/2 work.

Test-environment notes (not product issues): this machine runs as root, so JCEF needs
`ide.browser.jcef.run-under-superuser.allowed` (set only in the runIdea task); the IDEA build from
JetBrains' Maven repo lacks JCEF's native folder, so `plugins/jcef-plugin/jcef` in the Gradle cache
is linked to the JetBrains Runtime's copy (a real Rider install has it).

## Network check (Stage 0, 19:35)

| What | Result |
|---|---|
| .NET SDK download (dot.net install script) | OK — .NET SDK 10.0.403 installed to /opt/dotnet |
| NuGet (api.nuget.org) | OK — NetForms versions up to 0.1.5 listed |
| JetBrains Rider SDK (intellij-repository) | OK (metadata reachable) |
| Gradle plugin portal, Maven Central | OK |
| GitHub (git clone of Go-Forms/.NetForms) | OK over git; plain https page browsing is blocked by the session proxy, which is fine |
| Gradle → Rider SDK `riderRD 2026.2.4.1` | OK (3.6 GB zip, 9.4 GB unpacked, includes the ReSharper back end) |

## BLOCKER: the sandbox Rider needs a JetBrains account login

`./gradlew runIde` starts Rider 2026.2.4.1 under Xvfb fine and loads the sample solution, but then shows
the licence dialog. All three choices ("Free — non-commercial use", "Paid license", "Start trial")
need **Log in to JetBrains Account**. The brief says never log in, so I did not.
Screenshot of the dialog: `docs/screenshots/stage0-rider-licence-dialog.png`.

**Fallback (as the brief says):** the plugin's UI uses only the shared IntelliJ Platform (plus the
JCEF browser plugin), so it is screenshot-tested in the free **IntelliJ IDEA Community 2026.2.4**
(same platform build line, 262) via `./gradlew runIdeaCommunity`. Checks only Rider can do (Solution
Explorer nesting, Rider's Run button, the C# editor) are listed for Jacob in `TESTING-FOR-JACOB.md`.

## Decisions (with reasons)

1. NetForms pinned to tag `v0.1.5` (newest stable release; same as newest NuGet `0.1.5`).
2. Plugin built with IntelliJ Platform Gradle Plugin 2.19.0, Kotlin 2.4.21, Gradle 9.2.1 (wrapper
   checksum pinned), target Rider 2026.2.4.1 (sinceBuild 262).
3. Canvas route: reuse NetForms' webview (`designer/media/designer.js`) in JCEF. Reason: it only
   touches VS Code through `acquireVsCodeApi()` and `window` messages, so a small shim is enough
   (`src/main/resources/formwork/webview/bridge.js`).
4. File writes: NetForms' host writes the files to disk itself. Formwork patches NetForms with two
   hooks (`upstream-patches/0001-…`) and wraps it in `host/Formwork.Host`, which reads Rider's buffers
   and returns writes as text; the plugin applies them in one WriteCommandAction per gesture.
   Undo/redo in the designer call Rider's UndoManager, not the host's, so there's one history.
5. Form colour: Formwork.Host switches on NetForms' Windows 10/11 palette (designer only), so the
   form is #F0F0F0 like VS rather than .NET-on-Linux's Windows XP beige #ECE9D8.
6. Tabs: the Design tab is a FileEditor on `Form1.cs` placed before Rider's text editor, so
   double-click opens the designer and Rider's own C# editor stays as the second tab (named "Text"
   by the platform; renaming it would mean wrapping Rider's editor, which risks breaking C# features).
   The top tab reads `Form1.cs [Design]` (+ `*`) while the Design tab is shown.
7. Host published for linux-x64 only (35 MB instead of 574 MB for every OS).
8. Toolbox behaves like VS: click arms a tool, click/drag on the form places it, back to Pointer
   (NetForms patch 0002). Group order as VS 2022, empty General group with VS's hint.
9. Menu items typed in get VS's text-based names (NetForms patch 0003).
10. The designer measures text with **Formwork Sans** = Microsoft's Selawik (OFL) with Segoe UI's
    vertical metrics, so the sizes written into Designer.cs match VS's (`host/fonts/README.md`).
11. The project converter edits the .csproj text minimally (not an XML rewrite) so every other line is
    byte-identical; NETFORMS_VERSION 0.1.5.
12. Build against IntelliJ IDEA 2026.2.4 by default (frees 13 GB; same platform API). Rider build via
    `-PbuildAgainst=rider`.

## Feature checklist (statuses: Not started / Partial / Done (unverified) / Verified)

Story evidence (01:00, maximised IDE, native tool windows, both themes): `docs/screenshots/story-dark-0*.png` and
`story-light-0*.png` — open → pick Button → place → double-click → `button1_Click` with the cursor inside → Shift+F7 → Ctrl+Z
(handler gone) → Ctrl+Z (button gone). Script: `tools/story.sh`.

Evidence key: **T** = automated test (`./gradlew test`; build tests with `FORMWORK_BUILD_TESTS=1`),
**S** = screenshot in `docs/screenshots/`. All UI screenshots are from the sandbox **IntelliJ IDEA**
2026.2.4 (same platform as Rider 2026.2), dark theme, because the sandbox Rider needs a login (see BLOCKER).

### A. Opening and switching (Must)
- [x] Double-click Form1.cs opens designer; Designer.cs openable as text — **Verified (in IDEA)**: S `story-dark-02-designer-open.png`; Designer.cs opens as text (seen in story run 1). Rider Solution Explorer: 🔎 Jacob.
- [x] Design and Code tabs; `Form1.cs [Design]` + `*` — **Verified (in IDEA)**, with one difference: the sub-tabs are "Design" and **"Text"** (the platform's own name for Rider's code editor; see decision 6). S `story-dark-04-button-placed.png` (shows `[Design]*`).
- [ ] F7 / Shift+F7; right-click menu; Find Action — **Partial**: F7 and Shift+F7 **Verified** (S `stage1-dark-F7-view-code.png`, `stage1-dark-shift-F7-view-designer.png`; `tools/checks.sh`); right-click menu entries and Find Action registered but not yet seen in a screenshot.
- [x] Several forms open at once — **Verified**: S `stage1-dark-two-forms-open.png` (TwoForms sample, two Design tabs, separate canvases).
- [x] Editing Designer.cs as text / git checkout reloads canvas — **Verified** for an outside change + Reload from Disk (S `stage1-dark-reload-after-outside-change.png`) and for undo (S `story-dark-08-after-second-ctrl-z-button-gone.png`).

### B. Toolbox (Must)
- [x] Groups, order, contents; Pointer first — **Verified** for order and Common Controls contents: T `ToolboxModelTest`; S `story-dark-03-button-tool-selected.png`. Gap: no **WebBrowser** (Windows-only IE control), Components has only ErrorProvider/HelpProvider/ImageList/Timer (VS has more).
- [x] Search filters as you type — **Verified** (native Toolbox): "text" → ContextMenuStrip, MaskedTextBox, RichTextBox, TextBox in their groups (S `stage2-dark-toolbox-search-and-tray.png`).
- [x] Click a tool then click/drag on canvas — **Verified (click)**: S `story-dark-03…`, `-04…`. Drawing a rectangle: Done (unverified).
- [x] Drag a tool onto canvas / into container — **Verified** onto the form from the native Toolbox (S `stage2-dark-toolbox-drag-onto-form.png`); drop target picks the container under the pointer (table cell drop verified via click-place, S `stage4-dark-drop-into-table-cell.png`).
- [x] Double-click a tool adds to selected container — **Verified**: TextBox double-clicked with the form selected → textBox1 at the container's top-left (S `stage2-dark-toolbox-double-click-adds.png`).
- [x] After placing, the toolbox returns to Pointer — **Verified**: S `story-dark-04-button-placed.png`.
- [x] Project's own controls group after build — **Verified**: CustomControls sample; "CustomControls Components" group with BrokenControl and ScoreBox at the top of the Toolbox, placed and drawn (S `stage2-dark-project-controls-and-red-cross.png`, T `ProjectControlsTest`). Loaded only for trusted projects (code review: PASS).
- [x] Non-visual components go to the tray — **Verified**: "timer1 : Timer" in the tray under the form (S `stage2-dark-toolbox-search-and-tray.png`).

### C. Canvas (Must)
- [x] Click / Ctrl/Shift+click / rubber-band select — **Partial**: click verified (S `story-dark-04…`); Ctrl/Shift/rubber band are NetForms webview features, untested.
- [x] Primary selection white handles, others dark — **Verified**: one control: dotted outline + 8 white squares with dark borders (S `stage2-light-selection-handles-zoom.png`); multi-selection: primary (last selected) white, others dark (S `stage2-light-multi-select-primary-white-others-dark.png`). Format commands use the primary as reference. Form grips only while the form is selected.
- [x] Move, resize, form resize handles — **Verified** for move (S `stage2-light-drag-move-with-snaplines.png`) and resize by handle (S `stage2-light-resize-by-handle.png`); form grips right/bottom/corner shown (S `stage2-dark-whole-form-maximised.png`), dragging them not screenshot-tested.
- [ ] Snaplines — **Partial, verified for spacing lines**: blue dashed margin/spacing lines and the ghost outline while dragging (S `stage2-light-drag-move-with-snaplines.png`). Edge lines now blue like VS (were magenta), NetForms' centre lines hidden (VS has none). **No text-baseline (magenta) snaplines**: NetForms doesn't compute baselines (Known gap).
- [ ] Keyboard nudge/resize as VS — **Partial, Ctrl+arrow verified**: arrows 1 px, Shift+arrows resize 1 px (NetForms); Ctrl+arrow now jumps to the next snapline (neighbour edges, margin spacing, container padding) — OK button 114 → 90, aligned with the text boxes (S `stage2-light-ctrl-left-jumps-to-snapline.png`, Designer.cs `Point(90, 85)`); Ctrl+Shift+arrow resizes to the next snapline (Done, unverified). Added to patch 0002.
- [ ] Tab / Shift+Tab, Escape selects parent — **Partial**: Escape → parent exists; Tab order cycling untested.
- [ ] Drop into containers; move glyph — **Done (unverified)**.
- [x] Delete, Ctrl+X/C/V, Ctrl+A — **Verified** for Delete, Ctrl+Z after it, Ctrl+C/Ctrl+V (copy named `button1` as VS names it when the name is taken) (S `stage2-light-delete-undo-copy-paste.png`). Cut, Ctrl+A and paste into another form: Done (unverified).
- [x] Undo/redo joined with Rider's undo — **Verified**: Ctrl+Z in the designer runs Rider's undo; after 2× Ctrl+Z + save, both files are byte-identical to the template (story run 5, 20:24). S `story-dark-07-after-ctrl-z-handler-gone.png` then `story-dark-08-after-second-ctrl-z-button-gone.png` (re-taken 00:35 with the native tool windows: the button visibly disappears).
- [ ] Docked controls: free-edge resize only — **Done (unverified)** (NetForms README says so).

### D. Properties and Events (Must)
- [x] Selection drop-down; Categorized/Alphabetical; bold non-defaults; description pane — **Verified (native Properties tool window)**: S `stage2-dark-native-toolbox-properties.png`, `stage2-light-designer-native-windows.png` ("btnOK System.Windows.Forms.Button", 5-button toolbar, ⊟ categories, bold FixedDialog/Log In/OK). Description pane verified: "Text / The text associated with the control." with Text selected by default, as in VS (S `stage2-dark-whole-form-maximised.png`).
- [ ] Editors: text, numbers, bool/enum, flags, Size/Point/Padding/Font — **Partial, mostly verified** (native grid): text (Text → "Sign In", S `stage2-dark-native-toolbox-properties.png`), enum drop-downs (AcceptButton, S `stage2-light-acceptbutton-dropdown.png`), double-click a name to step bool/enum values (Cursor), flags (Anchor dialog, unverified), expandable Point/Size/Padding with ⊞ (Location → X, Y; X=120 wrote `new Point(120, 85)`, S `stage2-light-expandable-location.png`). Font is a dialog, not expandable rows.
- [ ] Pop-up editors (Anchor, Dock, Color, Font, ContentAlignment, multi-line, Cursor) — **Partial, ContentAlignment and Color verified**: native dialogs that follow the IDE theme. ContentAlignment 3×3 grid (S `stage2-light-contentalignment-picker.png`) → `btnOK.TextAlign = ContentAlignment.TopLeft;`. Color with Custom / Web / System tabs and swatches (S `stage2-light-color-picker-tabs.png`) → `btnOK.BackColor = Color.AntiqueWhite;`. Dock six-place picker, Font dialog (family, size, Bold/Italic/Underline/Strikeout), Anchor four-sides dialog, multi-line Text dialog, Cursor list: Done (unverified). Anchor is checkboxes in a cross, not VS's drawn arms.
- [ ] Collection editors — **Done (unverified)**: setItems works (samples use TabPages, menu DropDownItems, StatusStrip Items).
- [ ] Image pickers into .resx — **Done (unverified)**: NetForms writes .resx; file chooser goes through Rider.
- [x] AcceptButton / CancelButton — **Verified**: the drop-down lists the form's buttons (S `stage2-light-acceptbutton-dropdown.png`); LoginForm sets both through the designer and builds (T `ProjectBuildTest`).
- [ ] Reset — **Done (unverified)**.
- [ ] Multi-selection shared properties — **Partial**: an edit applies to every selected control in one undo step (verified: OK + Cancel selected, Cursor changed → `btnOK.Cursor = Cursors.Arrow;` and `btnCancel.Cursor = Cursors.Arrow;`). The grid still lists the primary control's properties, not only the shared ones.
- [x] (Name) rename incl. code — **Verified (host level)**: T `CodeGenerationTest.renaming…` (field, `+=` line, handler, a use in Form1.cs). Goes through the document, so Ctrl+Z works; Rider's rename refactoring for *other* files is not used (Known gap).
- [x] Events tab — **Verified**: S `stage2-light-events-tab.png` (events by category, Click → btnOK_Click), `stage2-light-event-double-click-creates-handler.png` (double-click Enter → btnOK_Enter created, cursor inside). Typing a name + Enter **verified** (`CloseWindow` created and wired, S `stage2-light-event-typed-name.png`); clearing it **verified** (the `+=` line goes, the method stays). Drop-down of existing matching handlers **verified** (btnOK_Click, CloseWindow offered for Cancel.Click; S `stage2-light-event-existing-handlers-dropdown.png`).

### E. Code generation (Must)
- [x] Double-click control creates default handler, jumps into it — **Verified**: S `story-dark-05-handler-in-code.png` (button1_Click, cursor inside, indented). Also seen: form → `Form1_Load`, CheckedListBox → `SelectedIndexChanged`.
- [x] Existing handler: no second one — **Verified (host level)**: T `CodeGenerationTest.binding an existing handler…`. Jump-to-existing in the IDE: not screenshot-tested.
- [x] Designer.cs written exactly as VS writes it — **Verified for the tested cases**: new style (T `HostClientTest`, `CodeGenerationTest`) and old `this.` style kept (T `CodeGenerationTest.the old this-dot style…`). Default sizes now match VS thanks to Formwork Sans (`host/fonts/README.md`: 7 of 11 controls identical, the rest within 2 px). Not diffed against a real VS run (no Windows here).
- [x] Deleting a control keeps handlers — **Verified (host level)**: T `CodeGenerationTest.deleting…`.
- [x] VS names — **Verified (host level)**: T `CodeGenerationTest.new controls get…`; menu items named from their text (patch 0003, MenuDemo sample).

### F. Format menu (Should)
- [x] Align — **Verified** (Lefts): S `stage4-light-format-align-lefts-find-action.png`, `stage4-light-format-align-lefts-result.png`. Others + to Grid: Done (unverified).
- [ ] Make Same Size — **Done (unverified)** (Format menu → NetForms' align helper; Size to Grid added).
- [ ] Horizontal / Vertical Spacing — **Done (unverified)**: Make Equal, Increase, Decrease, Remove (grid step = settings grid, 8).
- [ ] Center in Form — **Done (unverified)**.
- [ ] Order — **Done (unverified)** (Bring to Front / Send to Back).
- [ ] Lock Controls — **Partial**: toggles NetForms' designer-wide lock; no padlock glyph, and VS's per-control `Locked` property isn't written.

### G. Menus, strips, smart tags (Should)
- [ ] MenuStrip / ContextMenuStrip "Type Here" — **Partial, verified for the core**: click a menu on the canvas → its drop-down opens with a Type Here box (S `stage4-dark-menu-open-type-here.png`); type `&Save` + Enter → `saveToolStripMenuItem` added at the end, box ready for the next (S `stage4-dark-menu-type-here-added-save.png`; added to NetForms patch 0002 — NetForms had no Type Here). `-` makes a separator (host behaviour, tested via MenuDemo sample). Not done: Type Here for a new sub-menu to the right, drag to reorder.
- [ ] Smart-tag Tasks panel — **Partial, verified for TextBox**: the ◂ glyph on the selected control opens "TextBox Tasks — MultiLine" (S `stage4-light-smart-tag-textbox-tasks.png`; NetForms feature). The MultiLine checkbox is hard to see in the light page style; other controls' tasks (Insert Standard Items, Edit Columns, Add/Remove Tab…) are NetForms features, not checked one by one.
- [ ] ToolStrip / StatusStrip item-type drop-down — Not started

### H. Special containers (Should)
- [x] TabControl tab headers — **Verified**: clicking "Layouts" shows that page (S `stage4-dark-tabcontrol-switch-page-and-table-cells.png`).
- [ ] TableLayoutPanel cells — **Partial, verified for cells + drop**: cells drawn dashed (same S); a Button dropped into the bottom-right cell is written `tableLayoutPanel1.Controls.Add(button4, 1, 1);` (S `stage4-dark-drop-into-table-cell.png`). Dragging row/column borders: NetForms feature, untested.
- [ ] FlowLayoutPanel insertion/reorder — **Done (unverified)** (NetForms has an insertion bar and arrow-key reorder).
- [ ] SplitContainer splitter — **Done (unverified)** (NetForms drags the splitter).

### I. Tab order and Document Outline (Should)
- [x] View → Tab Order — **Verified**: numbers on each control; clicking the Password box made it `txtPassword.TabIndex = 0;`; toggling off with the same command (S `stage4-light-tab-order.png`).
- [x] Document Outline tool window — **Verified**: native tool window docked under the Toolbox with the whole control tree; dragging button1 onto groupBox1 moves it there (canvas and tree) (S `stage4-light-document-outline-drag-into-groupbox.png`). Reordering among siblings by drag: not implemented.

### J. Errors and safety (Must)
- [x] Broken Designer.cs: error page with line, never overwrites — **Verified**: S `stage1-dark-broken-designer-file.png`; the broken file is byte-identical afterwards (`tools/checks.sh` prints BROKEN FILE LEFT UNTOUCHED); T `HostClientTest`. "Open as text" opens the file at the error's line and column (not screenshot-tested).
- [x] Failing control: red cross — **Verified**: a control whose OnPaint throws is drawn as a red error box with the exception text, the rest of the form keeps working (same S; T `ProjectControlsTest` checks the red pixels and that other controls are still there). NetForms behaviour.
- [ ] Missing .NET 10 SDK notification — **Done (unverified)**: `DotnetNotifier` with a "Show me how" link.

### K. Projects and running (Must)
- [ ] Rider Run/Debug start the app on Linux — **Partial**: `dotnet build` + `dotnet run` of a converted project works and the click handler fires (S `stage1-app-running-on-linux.png`). Rider's Run button: 🔎 Jacob.
- [x] "Make this project work on Linux too" — **Verified for SDK-style projects**: golden files for the VS .NET 8/9/10 templates + custom settings (T `ProjectConverterTest`); every converted sample builds on Linux (T `ProjectBuildTest`, 8/8); in the IDE: notice → preview → apply, backup byte-identical, result = golden (S `stage3-dark-make-linux-preview.png`). **Windows half verified in CI** (GitHub Actions run 7, https://github.com/JacobGoodchild/formwork/actions/runs/41939192580, 00:14): on `windows-latest` all 9 converted samples build for their `-windows` target with the real Windows Forms, without NetForms, and pass a command-line design-time build. Old .NET Framework projects: **Done (unverified on Windows)** — NetForms' converter on a temp copy, then split into `net48x`+WinForms / `net10.0`+NetForms, warning shown (T `LegacyProjectConverterTest`, golden file). Limit: other old-style projects in the same solution must be converted too.
- [x] Add → Windows Form / User Control (+ templates) — **Verified**: New menu has Windows Form…, User Control (Windows Forms)…, and NetForms' Dialog / About Box / Login Form / Splash Screen (shipped in the plugin, no download). Form and UserControl text byte-identical to Microsoft's SDK templates (T `AddFormTemplateTest`); NetForms templates load (T `NetFormsTemplatesTest`); Add → Dialog creates Dialog1 and opens it in the designer (S `stage3-dark-add-dialog-template.png`).
- [ ] New project template — **Done (unverified in the UI)**: File → "Windows Forms App (Formwork)…" (a menu command; adding to Rider's New Solution dialog would need Rider's backend template system, not attempted). Makes Microsoft's SDK template (`dotnet new winforms -f net8.0`, offline) and converts it; result equals the golden converted template (T `NewProjectTest`), then opens it.

### L. Settings (Later)
- [x] Settings page — **Verified**: Settings → Tools → Formwork with layout mode (SnapLines / SnapToGrid), grid size 8 × 8, show grid, snap while dragging, .NET path (shows the one found), host path (S `stage5-light-settings-page.png`); grid mode draws dots every grid step on the form (S `stage5-light-grid-mode-dots.png`; dots also cover controls — the form is one picture). Snap-to-grid while *dragging* is not implemented (the grid is used by Format → Align/Size to Grid).
- [ ] Data binding pickers — **Partial**: DataSource is a drop-down of the form's data components (bindingSource1), DataBindings.* rows exist; DisplayMember/ValueMember are plain text (VS lists the source's members). Host-level check only.

## Fresh-eyes review, end of Stage 1 (21:02, sub-agent that saw only the references, screenshots and checklist)

> **Verdict: Formwork works, but it does not look like Visual Studio.** A VS user will find the right parts,
> but they won't feel at home. The behaviour is further along than the appearance.
> - Toolbox and Properties are panes inside the editor tab, not docked tool windows.
> - Toolbox: "Toolbox | Outline" tabs instead of a title; "Search toolbox" lower case; extra "+ Control
>   Library..." row and "(tray)" suffixes; only Pointer has an icon.
> - Form title bar looks like a focused text field; Form1 (800 wide) is cut off so its caption buttons and
>   right/bottom handles never show.
> - Button is grey, not white; handles look dark/filled.
> - Properties: no "Properties" title, no selection drop-down, text tabs instead of the 5-icon toolbar,
>   a search box VS doesn't have, no description pane.
> - Design and code are one tab with "Design | Text" switches (VS: two tabs).
> - "IDE error occurred" toast visible in several screenshots.
> - Checklist: several forms, toolbox order, click-to-place, back to Pointer, handler with cursor, Linux
>   preview = Verified. F7/Shift+F7, outside-edit reload, broken file = Partly (evidence not explicit).
>   **Ctrl+Z = Not shown** (07 looks like 06; the button is still there).
> Top 5: real tool windows; proper form title bar + whole form visible + white handles; VS Properties header
> with selection drop-down, toolbar, description pane; toolbox icons and VS wording; fix the error toast and
> re-take the Ctrl+Z / F7 / "never overwritten" evidence.

My response: agreed on all points. On Ctrl+Z: screenshot 07 shows the undo of the *handler* (the last step),
so the button correctly stays; the file-level proof is in the log (byte-identical after 2× Ctrl+Z) but the
screenshot doesn't show it — I'll re-take it showing the button disappearing. The error toast was IDEA's own
OS-integration daemon (not Formwork; now disabled in the test IDE). These are the Stage 2 to-do list.

## Fresh-eyes review 2, after Stages 2–4 (00:40, sub-agent, references + current screenshots only)

> **Verdict: much closer to Visual Studio. A VS user would now find their way around and mostly feel at home, but
> parts still look unfinished.** Layout now right (docked Toolbox with Search Toolbox, VS group order, Pointer, icons;
> Properties with selection drop-down, 5-icon toolbar, categories, bold; `Form1.cs [Design]*`; light form on dark canvas;
> light and dark both work). Still different: Form1 cut off by the canvas (caption buttons never show); Toolbox about
> twice VS's width; no description pane in any shot; BackColor "Control" swatch beige; form handles stay on when a child is
> selected; no dark handles on secondary selections; grey buttons; Design/Text sub-tabs instead of two tabs; an extra canvas
> toolbar and status line; environment toasts in shots; preview dialog without a frame.
> Claims: Toolbox/Properties/Events both themes, project-controls group, Align Lefts, TabControl, table cells, Type Here,
> Linux preview = **Verified**; story, red error box, handles, Ctrl+Left, Add Dialog = **Partly**.

My response: fixing the form fit (narrower default Toolbox), description pane, swatch colours, form handles and
the extra canvas toolbar now (00:45). The frameless dialog is the test screen having no window manager (not Formwork).

## Security review (00:25, sub-agent, code only) — verdict and what I changed

Rules: 1 backups **PARTIAL**, 2 no phone-home **PASS**, 3 argument lists **PASS**, 4 trust **PASS**, 5 pinning **PARTIAL**,
6 no orphans **PASS (one race)**, 7 no secrets **PASS**. Most important finding: the plugin wrote any path the
host named in `writes` (and the host runs project code in trusted projects).

Fixed (00:30): writes limited to the form's three files, in the plugin **and** in the host; the .csproj backup is
taken outside the undoable command (after saving the document, so it equals the preview); page edits are
allow-listed and picture paths must come from the file chooser; the page's Run only in trusted projects; a host
that finishes starting after its editor closed is closed at once; host started in its own folder; relative PATH
entries ignored when looking for `dotnet`; saved page state escaped inside `<script>`; a Content-Security-Policy on
the page; the old-project converter always writes NetForms 0.1.5.
Not fixed (Known gaps): no NuGet lock files / Gradle dependency verification; CI actions pinned by tag, not SHA.

## Performance (measured 00:15, `PerformanceTest`, TabsDemo sample, 18 controls; build/perf.txt)

| Target | Measured | Result |
|---|---|---|
| Form opens < 3 s | host cold start + read + paint: **0.31 s** (was 5.5 s before publishing the host ReadyToRun) | met (host side; JCEF page load adds ~1 s in the IDE, not measured precisely) |
| Drag redraws ≥ 30/s | the page moves the drag outline itself every frame (no host call per frame) — not measured in a browser; the commit on release is **36 ms** median (28/s) | met by design, **not measured** |
| Property edit → canvas < 200 ms | **29 ms** median, 36 ms worst (host side) | met |

## Interruption

The session was stopped from 21:28 to 00:06 UK (usage limit / worker restart); the wake-up chain brought it back. Nothing was lost (all committed).

## Known gaps

1. **Sandbox Rider can't be tested here** (login needed). UI tested in IntelliJ IDEA 2026.2.4; Rider-only
   checks are listed in `TESTING-FOR-JACOB.md`.
2. **The running app's background is beige (#ECE9D8) on Linux**, not Windows' #F0F0F0. This is NetForms'
   deliberate choice (its PLAN.md decision 10: keep .NET's own Linux system colours so code comparing
   `SystemColors.Control` keeps working). The designer shows #F0F0F0 like VS. Not changed.
3. **No WebBrowser control** in the toolbox (Internet Explorer ActiveX, Windows-only; NetForms has none).
4. **Sub-tab is called "Text", not "Code"** (it's Rider's own C# editor; renaming means wrapping it).
5. **Renaming a control** updates `Form1.Designer.cs` and `Form1.cs` (Roslyn-accurate), but not other
   files that use it (VS's rename would). Rare for forms.
6. **Text sizes**: the designer measures with Formwork Sans (Segoe UI's open replacement, adjusted).
   Some widths differ by 1–2 px from VS (Label 37 vs 38, RadioButton 96 vs 94), ListBox 96 vs 94 high.
7. **Old .NET Framework projects** convert (with a clear "check it at college" warning), but a solution
   whose *other* projects are also old-style needs each converted, and the Windows half is only checked by
   the golden file, not by a Windows build yet.
8. The form's title bar now has an icon and caption glyphs, but they're flat (Windows 10 style), not
   the boxed Windows 7-style buttons of one reference screenshot.
9. **Button face** is NetForms' grey (#E1E1E1, Windows 10 style), not the white Windows 11 face in
   Jacob's screenshot: it's what the app really looks like when it runs on Linux, so the designer keeps it.
12. **No magenta text-baseline snaplines** (NetForms has edge, margin and centre lines only).
11. **Dependency checksums:** versions are pinned and the Gradle wrapper is checksum-verified, but NuGet
   packages and Maven artifacts aren't checksum-locked (no lock files / verification metadata yet).
10. Toolbox icons are Formwork's own simple drawings (NetForms has none for built-in controls; VS's
   can't be copied).

## What I tried that failed

- Sandbox **Rider** (licence login), see BLOCKER.
- IntelliJ IDEA *Community* as the free IDE: no longer published since 2025.3; the unified IntelliJ IDEA's
  free tier works without login.
- JCEF under root: needs `ide.browser.jcef.run-under-superuser.allowed` (test runs only).
- NetForms' toolbox drag used HTML5 drag-and-drop, which JCEF's out-of-process mode doesn't deliver →
  replaced by mouse-event dragging (patch 0002).
- Separate undo commands for the handler stub and its indentation caused an "Undo last operation?"
  prompt → merged into one command.
- Running the sandbox IDE straight out of Gradle's cache modified it; Gradle then refused the cache entry.
  Fix: the IDE lives in `/opt/ide/idea-2026.2.4`, used via `localIde` in `~/.gradle/gradle.properties`.
- Disk allowance (~40 GB) filled up at 20:38 with Rider SDK + 3 IDE copies; freed by dropping the Rider SDK
  (build against IDEA by default; `-PbuildAgainst=rider` still works).

## Next step

Finish Stage 1 checks (F7 screenshot, text edit reload, two forms), fresh-eyes review, then Stage 2
(native Toolbox / Properties / Document Outline tool windows, light theme, form title bar).

---

# Part 2 — The brief (copied verbatim)

# Formwork – Claude Code brief

Nov 4, 2026 · @Jacob

## Brief for Claude Code

### 1. The goal

Build **Formwork**: a JetBrains Rider plugin that gives Rider on Linux the Windows Forms drag-and-drop designer, so it looks and works like the WinForms designer in Visual Studio 2022 on Windows.

The user is Jacob, a sixth-form student. At college he uses Visual Studio on Windows with WinForms. At home he uses Rider on Linux Mint 21.3 (Ubuntu 22.04 base), where Rider's own WinForms designer does not work: JetBrains documents it as Windows-only. He wants:

- To open a form in Rider on Linux and get the **same designer experience as Visual Studio**: toolbox, a canvas to drag controls onto, properties and events, double-click a button to get `button1_Click`, F7 / Shift+F7, and so on.
- His **code to be identical** to the Visual Studio version: the same `Program.cs`, `Form1.cs`, `Form1.Designer.cs` and `InitializeComponent()`, the same `using System.Windows.Forms;`. A form he designs at home must open unchanged in Visual Studio at college, and the other way round.
- To press Run in Rider and see the app open as a real window on Linux.

He is not very technical. He doesn't follow tool jargon, so anything he has to do himself must be explained in plain English, step by step.

**Definition of done:** Jacob copies a WinForms project from college onto his Linux laptop, opens it in Rider, double-clicks `Form1.cs`, sees the form in a designer that looks like his Visual Studio screenshots, drags on a new button, double-clicks it, writes code in the handler, presses Run, and it works. He then takes the project back to college and it opens and runs in Visual Studio with no edits.

### 2. Usage limits, wake-ups and the progress log (do this FIRST)

This is a multi-night job. Work until 7:00am UK time (so about 11 hours work, no stopping), then and only then stop cleanly.

**Progress log.** Create `CLAUDE.md` in the repo root on your first step. It holds a copy of this brief, then a progress log: the stage you're on, a checklist of every feature in section 6 with its status, what you tried that failed, and the next step. Update it and commit after every small working step, not only at the end. Every session, including after a restart, begins by reading `CLAUDE.md` and `git log`. Never redo committed work.

**Wake-ups.** The 5-hour usage limit can stop you mid-session and you cannot see it coming, so keep a dead man's switch running:

1. Right at the start, use your scheduling tool (`send_later`, or `ScheduleWakeup`; search your tools for "schedule" if neither shows) to schedule a message back into **this same session** about 60 minutes ahead. Text: `WAKE-UP CHECK: if you were stopped by a usage limit, re-read CLAUDE.md (brief + progress log), check git status, and carry on from the first unticked item until the deadline. If you are already mid-task, ignore this, but make sure a new wake-up is scheduled.`
2. After every commit, schedule a fresh wake-up about 60 minutes ahead, and cancel the older one if your tool allows it.
3. Also keep a backup chain: at the start, schedule extra wake-ups roughly every 90 minutes up to the deadline. Extra ones arriving while you're busy are harmless.
4. In the last 20 minutes, cancel any wake-ups after the deadline, write a short "where I got to" summary at the top of the progress log, commit and push.
5. If no scheduling tool exists in this session, say so in the first line of the progress log, so Jacob knows to restart it by hand.

Commit and push after every small working step, so nothing is lost if you're cut off.

### 3. Ground rules

**Honesty is the most important rule.** Jacob's biggest worry is that you build something clunky that looks nothing like Visual Studio and then claim it does. So:

- Every feature in the checklist (section 6) has one of four statuses: `Not started`, `Partial`, `Done (unverified)`, `Verified`. **Verified** needs evidence: a passing automated test and, for anything visible, a screenshot committed under `docs/screenshots/` whose path is written next to the item.
- Never write "done", "works" or "looks like Visual Studio" without that evidence. If a screenshot doesn't look right next to the reference, say so plainly in the log.
- When something is hard, cut, or faked with a placeholder, write it in the log under **Known gaps**. A smaller thing that truly works beats a big thing that half works.
- Don't mark a stage finished until every item in it is Verified, or listed in Known gaps with a reason.

**Safety rules for the code:**

- The plugin must never delete or overwrite Jacob's files without a backup. Before changing a `.csproj`, keep the original as `<name>.csproj.formwork.bak`. Edits to `.cs` files go through Rider's document APIs, inside a write action and a command, so **Ctrl+Z in Rider undoes them**.
- No telemetry, analytics or "phone home". The plugin makes no network requests on its own. The only network use is when Jacob explicitly runs a command that needs NuGet, such as creating a new project. Say so in the README.
- Start processes with an argument list (`GeneralCommandLine` with separate parameters), never by gluing strings into a shell command. Never pass file contents or user text to a shell.
- The designer host runs code from the user's project (custom controls), just as Visual Studio does. Only load project build output when Rider considers the project **trusted** (Rider's Trusted Projects check). In an untrusted project, show built-in controls only and explain why in one line.
- Pin every dependency to an exact version. Any binary you download during the build is checked against a SHA-256 written in the build script.
- Kill the designer host process when the editor closes, the project closes, or Rider exits. No orphan processes.
- No secrets in the repo. Publishing to the JetBrains Marketplace is out of scope; Jacob will decide that later.

**Licences:**

- NetForms is MIT-licensed. Keep its `LICENSE` and `THIRD-PARTY-NOTICES.md` in the plugin, and credit it in the README and the plugin's About text.
- Don't copy Microsoft's Visual Studio icons, images or code. Recreate the *layout and behaviour*; use JetBrains' built-in icons (`AllIcons`) or icons you draw yourself as SVG, and NetForms' own toolbox bitmaps where it ships them.
- Formwork's own licence: MIT.
- JetBrains asks that plugin names don't start with or imitate their product names, so the plugin is called "Formwork", not "Rider WinForms".

**When you're unsure,** pick the option closest to how Visual Studio behaves, write the decision and reason in the log, and carry on. Don't stop to ask.

### 4. Architecture: don't rebuild WinForms, wrap NetForms

The hard part (WinForms running on Linux) is already solved by **NetForms**, an MIT-licensed reimplementation of `System.Windows.Forms` and `System.Drawing` for .NET 10, painted with SkiaSharp, using Avalonia only to reach the OS. Code compiles against it unchanged. Read these before writing any code, and write what you learn into `docs/RESEARCH.md`:

- Repo: https://github.com/Go-Forms/.NetForms (read its `CLAUDE.md`, `docs/PLAN.md`, `designer/`, `src/NetForms.Design*`, `tools/`)
- Designer docs: https://go-forms.github.io/.NetForms/docs/designer.html
- Compatibility guide: https://go-forms.github.io/.NetForms/docs/compatibility.html
- NuGet: https://www.nuget.org/packages/NetForms (0.1.5 was the latest on 16 Oct 2026; use the newest stable release you find)

NetForms also ships a **designer for VS Code**. It has two halves:

1. **A design-time host** (a .NET 10 process: `NetFormsDesigner.Host`). It reads `InitializeComponent()` with Roslyn without compiling the project, builds the live form, paints it, and writes edits back to `*.Designer.cs` in exactly the form Visual Studio writes (their CI checks this against the real WinForms serializer).
2. **A VS Code front end** (TypeScript extension plus a webview with the canvas, toolbox and property grid).

**Formwork's plan: keep half 1, replace half 2 with Rider.**

- **Plugin type:** a Rider front-end plugin in Kotlin, built with the IntelliJ Platform Gradle Plugin 2.x, targeting the latest stable Rider release. No ReSharper back-end part unless you hit something that truly needs it (log why if so).
- **The host:** add NetForms as a git submodule pinned to a release tag. The Gradle build runs `dotnet publish` on the host and bundles the output inside the plugin zip. At runtime the plugin starts the host as a child process and talks to it using the same protocol the VS Code extension uses. Learn that protocol by reading `designer/src`, and document it in `docs/PROTOCOL.md` before using it.
- **The canvas:** first try reusing NetForms' webview UI inside a JCEF browser (`JBCefBrowser`), replacing VS Code's `acquireVsCodeApi().postMessage` bridge with a `JBCefJSQuery` bridge. If that code is too tied to VS Code, write your own canvas page that talks to the host. Note which route you took and why.
- **Rider-native pieces:** a `FileEditorProvider` so a form opens with a **Design** tab and a **Code** tab; a **Toolbox** tool window; a **Properties** tool window (with the Events lightning-bolt tab); a **Document Outline** tool window; a **Format** menu; F7 / Shift+F7 actions. Get the whole flow working inside the webview first, then move Toolbox, Properties and Document Outline into real Rider tool windows (Swing, JetBrains UI components) so they dock like Visual Studio's and follow Rider's theme. Property pop-up editors (Anchor, Dock, colour, font, collection editors) may stay as web pop-ups if they look right.
- **All file writes go through Rider.** If the host writes files directly, change the integration so it returns the new text (or text edits) and the plugin applies them to Rider's `Document`. That keeps Rider's undo, unsaved-changes markers and file watcher correct. Editing `Form1.Designer.cs` as text, or a git checkout, reloads the canvas.
- **Running the app:** a NetForms project is a normal `net10.0` console-style app, so Rider's own Run and Debug already work. Don't build a custom runner; just check it works and fix anything that stops it.
- **NetForms bugs:** if you find one, fix it on a branch of the submodule, keep a clean patch in `upstream-patches/` with a note, and log it. Don't silently change NetForms' behaviour.

### 5. The look: match Visual Studio 2022, in light and dark

Jacob sent screenshots of Visual Studio 2022's WinForms designer (dark theme). You can't see them, so here is exactly what they show. Match this layout and behaviour.

**Toolbox (docked on the left):**

- A title bar reading "Toolbox" with a small drop-down, pin and close icons.
- A "Search Toolbox" box with a magnifying-glass icon.
- Collapsible groups with ▷ / ◢ arrows, in this order: **All Windows Forms**, **Common Controls**, **Containers**, **Menus & Toolbars**, **Components**, **Printing**, **Dialogs**, **Data**, **General**.
- Common Controls, expanded, lists each control with a small icon on its left, starting: Pointer, Button, CheckBox, CheckedListBox, ComboBox, DateTimePicker, Label, LinkLabel, ListBox, ListView, MaskedTextBox, MonthCalendar, then NotifyIcon, NumericUpDown, PictureBox, ProgressBar, RadioButton, RichTextBox, TextBox, ToolTip, TreeView, WebBrowser (alphabetical, with Pointer first in every group).
- An empty General group shows grey centred text: "There are no usable controls in this group. Drag an item onto this text to add it to the toolbox."
- The hovered or selected item is highlighted across the full row.

**Editor tab:** titled `Form1.cs [Design]`, with `*` added when there are unsaved changes.

**Canvas:**

- The area around the form uses the IDE's editor background (dark grey in dark theme).
- The form itself is always drawn in the **light Windows style**, even in dark theme, exactly as Visual Studio does: a title bar with a small app icon and "Form1" on the left, minimise / maximise / close buttons on the right, and a light grey client area (`SystemColors.Control`, #F0F0F0).
- A new Button reads "button1": white face, thin grey border, centred text.
- The selected form or control has a thin dotted outline and small white square grab handles with dark borders. A form shows handles only on its right edge, bottom edge and bottom-right corner, since only those sides resize. A control shows eight handles. A docked control shows handles only on its free edge.

**Properties (docked bottom-right, under Solution Explorer):**

- A title "Properties".
- A drop-down naming the selection, such as `Form1 System.Windows.Forms.Form`.
- A small toolbar: Categorized, Alphabetical, Properties, Events (lightning bolt), Property Pages.
- A two-column grid: name on the left, value on the right, grouped by category (Accessibility, Appearance, Behavior, Data, Design, Focus, Layout, Misc, Window Style). Values that differ from the default are **bold**.
- A description pane at the bottom showing the selected property's name and help text.

**Solution Explorer** (Rider's own) should show `Form1.cs` with a form icon, with `Form1.Designer.cs` and `Form1.resx` nested under it. Double-clicking `Form1.cs` opens the designer, as in Visual Studio.

**Light and dark:** your tool windows must follow Rider's current theme automatically and switch live when Jacob changes theme. Test both Rider's default light and default dark themes. Only the form itself stays light, as in Visual Studio.

**References:** search the web for more screenshots of the Visual Studio 2022 WinForms designer in both themes (Microsoft Learn's WinForms pages have many). List the URLs in `docs/REFERENCES.md`, download a few into `docs/reference/` if the site allows, and compare your screenshots against them at every stage.

### 6. Feature checklist (everything Visual Studio's WinForms designer does)

Copy this list into the progress log and track each line. Groups are marked **Must** (needed for the definition of done), **Should** (part of feeling like Visual Studio) or **Later** (only once everything else is Verified). Where this list and NetForms' VS Code designer disagree, follow Visual Studio and check Microsoft's docs.

#### A. Opening and switching (Must)

- [ ] Double-clicking `Form1.cs` (or a UserControl) in Solution Explorer opens the designer; `Form1.Designer.cs` stays openable as text
- [ ] Design and Code tabs on the editor; tab title `Form1.cs [Design]` plus `*` when unsaved
- [ ] F7 = View Code, Shift+F7 = View Designer; also in the right-click menu and Find Action
- [ ] Several forms open at once, each with its own canvas
- [ ] Editing `Form1.Designer.cs` as text, or a git checkout, reloads the canvas

#### B. Toolbox (Must)

- [ ] Groups, order and contents as in section 5; Pointer first in each group
- [ ] Search box filters as you type
- [ ] Click a tool, then click the canvas to place it at default size, or drag out a rectangle to size it
- [ ] Drag a tool onto the canvas or into a container
- [ ] Double-click a tool to add it to the selected container
- [ ] After placing, the toolbox returns to Pointer
- [ ] The project's own controls and UserControls appear in their own group after a build
- [ ] Non-visual components (Timer, ToolTip, ImageList, OpenFileDialog, BindingSource, ContextMenuStrip...) go to the component tray under the form

#### C. Canvas: select, move, resize (Must)

- [ ] Click selects; Ctrl/Shift+click adds to the selection; drag a rectangle on empty space to select several
- [ ] Primary selection has white handles and is the reference for Format commands; others have dark handles
- [ ] Drag to move; drag handles to resize; the form resizes from its right, bottom and corner handles
- [ ] Snaplines while dragging: blue for edges, magenta for text baselines, and spacing lines for margins and padding
- [ ] Keyboard, matching Visual Studio: arrows nudge 1 px, Ctrl+arrows jump to the next snapline, Shift+arrows resize 1 px, Ctrl+Shift+arrows resize to the next snapline (check Microsoft's snaplines walkthrough)
- [ ] Tab / Shift+Tab move selection between controls; Escape selects the parent
- [ ] Drop into containers: Panel, GroupBox, TabPage, SplitContainer panels, FlowLayoutPanel, TableLayoutPanel cells; a selected container moves by its four-way-arrow glyph
- [ ] Delete, Ctrl+X / C / V (also between forms), Ctrl+A
- [ ] Undo / redo with Ctrl+Z / Ctrl+Y, joined up with Rider's undo
- [ ] Docked controls: dotted outline, resize only from their free edge

#### D. Properties and Events (Must)

- [ ] Selection drop-down; Categorized / Alphabetical; non-default values bold; description pane
- [ ] Editors: text, numbers, true/false and enum drop-downs, flags, expandable Size / Point / Padding / Font
- [ ] Pop-up editors: Anchor (four arms), Dock (six places), Color (Custom / Web / System tabs), Font dialog, ContentAlignment grid, multi-line Text, Cursor list
- [ ] Collection editors: Items (one per line), TabPages, Columns, menu DropDownItems, TreeView Nodes, ListView Items
- [ ] Image, BackgroundImage and Icon pickers that store into `Form1.resx` the way Visual Studio does
- [ ] AcceptButton / CancelButton drop-downs listing the form's buttons
- [ ] Right-click a property → Reset
- [ ] Multi-selection shows shared properties and edits all of them
- [ ] (Name) renames the field, its handlers and every use in `Form1.cs` (through Rider's rename, so Ctrl+Z works)
- [ ] Events tab: double-click an event to create `button1_Click` and jump to it; drop-down of existing matching handlers; type a name and press Enter; clearing it unbinds but leaves the method

#### E. Code generation (Must)

- [ ] Double-clicking a control creates its default event handler (Button.Click, Form.Load, TextBox.TextChanged, CheckBox.CheckedChanged, ComboBox.SelectedIndexChanged, Timer.Tick...) in `Form1.cs`, switches to the Code tab and puts the cursor inside it
- [ ] Double-clicking a control whose handler exists jumps to it instead of making a second one
- [ ] `Form1.Designer.cs` is written exactly as Visual Studio writes it, in the style the file already uses (old `this.button1` style or the newer .NET style). Diff your output against real Visual Studio output in tests
- [ ] Deleting a control removes its designer code but keeps its handler methods, as Visual Studio does
- [ ] New controls get Visual Studio's names: button1, button2, textBox1, label1...

#### F. Format menu (Should)

- [ ] Align: Lefts, Centers, Rights, Tops, Middles, Bottoms, to Grid
- [ ] Make Same Size: Width, Height, Both, Size to Grid
- [ ] Horizontal Spacing and Vertical Spacing: Make Equal, Increase, Decrease, Remove
- [ ] Center in Form: Horizontally, Vertically
- [ ] Order: Bring to Front, Send to Back
- [ ] Lock Controls (padlock glyph on locked controls; also the Locked property)

#### G. Menus, strips and smart tags (Should)

- [ ] MenuStrip and ContextMenuStrip "Type Here" boxes: type to add items, `&` mnemonics, `-` makes a separator, sub-menus open to the right, drag to reorder
- [ ] Smart-tag arrow (◂) on supported controls with its Tasks panel: Insert Standard Items, Edit Items, Choose Image, MultiLine, Dock in Parent Container, Add Tab / Remove Tab, Edit Columns
- [ ] ToolStrip and StatusStrip item-type drop-down

#### H. Special containers (Should)

- [ ] TabControl: clicking a tab header shows that page for editing
- [ ] TableLayoutPanel: cells drawn dashed, drop into a cell, drag column and row borders
- [ ] FlowLayoutPanel: insertion mark, drag to reorder
- [ ] SplitContainer: drag the splitter

#### I. Tab order and Document Outline (Should)

- [ ] View → Tab Order: numbers drawn on each control; click in order to set TabIndex; toggle off
- [ ] Document Outline tool window: the control tree, click to select, drag to reorder or move between containers

#### J. Errors and safety (Must)

- [ ] A broken `Designer.cs` shows a clear error page with the message and a link to the line, and never overwrites the file
- [ ] A control that fails to draw shows as a red cross with the error, and the rest of the form still works
- [ ] Missing .NET 10 SDK: a plain-English notification with a link to setup steps

#### K. Projects and running (Must)

- [ ] Rider's Run and Debug start the app on Linux
- [ ] "Make this project work on Linux too" command (section 7), with a preview and a backup
- [ ] Add → Windows Form / User Control (and NetForms' dialog, About box, login and splash templates) in Rider's Add menu
- [ ] New project: "Windows Forms App (Formwork)" in Rider's New Solution dialog if Rider allows it, otherwise a menu command

#### L. Settings (Later)

- [ ] Settings page: layout mode (snaplines or grid), grid size (default 8, 8), show grid, snap to grid, host path, .NET path
- [ ] Data binding pickers (DataSource, DisplayMember, DataBindings)

### 7. One project that works at college and at home

The code files never change. Only the project file differs between the two worlds: Visual Studio's WinForms needs a Windows target with `UseWindowsForms`, while NetForms needs `net10.0` plus a package reference. NetForms' own converter rewrites the project for Linux only, which would break it at college. So Formwork's command **"Make this project work on Linux too"** should write a project file that picks the right one automatically:

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>WinExe</OutputType>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>

  <!-- On Windows (college): the real WinForms, exactly as Visual Studio made it -->
  <PropertyGroup Condition="'$(OS)' == 'Windows_NT'">
    <TargetFramework>net8.0-windows</TargetFramework> <!-- keep whatever it was -->
    <UseWindowsForms>true</UseWindowsForms>
  </PropertyGroup>

  <!-- On Linux (home): NetForms -->
  <PropertyGroup Condition="'$(OS)' != 'Windows_NT'">
    <TargetFramework>net10.0</TargetFramework>
  </PropertyGroup>
  <ItemGroup Condition="'$(OS)' != 'Windows_NT'">
    <PackageReference Include="NetForms" Version="[pinned version]" />
    <Using Include="System.Drawing" />
    <Using Include="System.Windows.Forms" />
  </ItemGroup>
</Project>
```

This is a starting point, not a finished answer. Prove it works:

- Keep every other setting from the original project untouched. Show Jacob a before/after preview, and save the original as `.csproj.formwork.bak`.
- Test it against real Visual Studio 2022 template projects (the "Windows Forms App" template for .NET 8, 9 and 10). Build them on Linux, run them, and open them in the designer.
- Check the Windows half too. If you can push to GitHub and read Actions results, add a workflow that builds the project on `windows-latest` and confirms the Visual Studio design-time build still works. If you can't, write the workflow anyway and add a "check at college" step to `TESTING-FOR-JACOB.md`.
- **Old .NET Framework projects:** if a college project is .NET Framework 4.x (an old-style `.csproj` with `ToolsVersion` and `<Reference Include="System.Windows.Forms" />`), the Windows half has to become an SDK-style `net48` project with `UseWindowsForms`. Visual Studio 2022 supports that, but it is riskier. Support it, warn clearly, and tell Jacob to test it at college before relying on it.
- When Rider on Linux opens a project that uses `UseWindowsForms`, show a small notification offering the command. Never change anything without his click.

In `TESTING-FOR-JACOB.md`, explain in plain English how he can tell which kind his college projects are: open the `.csproj` in a text editor and look at the first line.

### 8. Testing and proof

You may search the web, read docs and source code, run Rider, and take screenshots as often as you like. Look at every screenshot yourself (open the image) before calling anything Verified.

**Sample projects** in `samples/`: the Visual Studio "Windows Forms App" template for .NET 8, 9 and 10; a Calculator (grid of buttons and a TextBox); a Login form (labels, text boxes, AcceptButton / CancelButton); a Menu demo (MenuStrip, ContextMenuStrip, StatusStrip, OpenFileDialog); a Tabs demo (TabControl, GroupBox, Panel, TableLayoutPanel, FlowLayoutPanel, SplitContainer); and a Timer + PictureBox demo. These are the kind of thing Jacob makes at college.

**Automated tests:**

- Unit tests (JUnit 5) for the host protocol client, applying edits to documents, and the project-file command (golden before/after files for every sample).
- Round-trip tests: open each sample, apply a scripted list of edits (add, move, resize, rename, set properties, add an event, delete), save, and compare `Designer.cs` against the expected output. Reuse NetForms' Visual Studio golden files where they fit.
- Canvas golden images: render each sample form headless and compare to stored PNGs.
- Rider UI tests: run the sandbox Rider under Xvfb (`xvfb-run -s "-screen 0 1920x1080x24"`) with JetBrains' Remote Robot (`runIdeForUiTests`). Script the definition-of-done story: open a sample, double-click `Form1.cs`, drag a Button from the Toolbox, double-click it, check `button1_Click` exists and the cursor is inside it, then build and run. Screenshot each step in light and dark themes into `docs/screenshots/`.

**If the sandbox Rider won't run here** (a licence screen that needs a JetBrains account, blocked downloads, no display): don't log in to anything, and don't fake results. Write the blocker in the log. As a fallback, keep the plugin's UI parts depending only on the core IntelliJ Platform where possible, so they can be screenshot-tested in the free IntelliJ IDEA Community, and list the Rider-only checks for Jacob in `TESTING-FOR-JACOB.md`.

**Fresh-eyes review at the end of every stage:** if you can start a sub-agent, give it the reference screenshots, your new screenshots and the checklist, but not your notes. Ask it whether a Visual Studio user would feel at home, and which items are really Verified. Fix what it finds, and paste its verdict into the log. If you can't start one, do the review yourself, as a harsh outsider.

**Performance targets** (measure and log them): a form opens in under 3 seconds; dragging a control redraws at least 30 times a second; typing in the Properties window updates the canvas in under 200 ms.

**Security review before the final stage:** check every rule in section 3 against the code (ideally with a sub-agent), and fix anything that fails.

### 9. Build order

Do the stages in order. Don't start a stage until the one before is Verified, or its leftovers are in Known gaps with a reason. At the end of each stage: screenshots, fresh-eyes review, update the log, commit, push, and put **"Stage N ready for Jacob to test"** at the top of the log with what to try.

1. **Stage 0: research and a proof that the pipeline works.** First check the network: can Gradle download the Rider SDK, and can `dotnet` reach NuGet? If not, stop and write exactly which domains were blocked at the top of the log. Read NetForms and write `docs/RESEARCH.md` and `docs/PROTOCOL.md`. Set up the Gradle plugin project and the NetForms submodule. Spike: a sandbox Rider opens a sample, `Form1.Designer.cs` shows as a canvas in a JCEF editor tab, and the host draws the form. Screenshot it. **Go / no-go:** if this can't be made to work, write down why, what you tried and the realistic options, and stop the stage there.
2. **Stage 1: the definition-of-done story, end to end.** Groups A, B and E, plus the basics of C and D (select, move, resize, edit Text and Name). Double-click `Form1.cs` → designer → drag a Button → double-click it → handler in code → Run. Write `TESTING-FOR-JACOB.md`, version 1.
3. **Stage 2: make it look and feel like Visual Studio.** Move Toolbox, Properties and Document Outline into native Rider tool windows; light and dark themes; the rest of groups C, D and J. Compare against the references hard.
4. **Stage 3: college and home.** Group K and all of section 7, with the golden tests and the Windows check.
5. **Stage 4: the rest of Visual Studio.** Groups F, G, H and I.
6. **Stage 5: finish.** Group L, the performance targets, the security review, and packaging (section 10).

### 10. Finishing: packaging and guides

- `./gradlew buildPlugin` produces one zip that installs through Rider's **Settings → Plugins → ⚙ → Install Plugin from Disk**. Attach it to a GitHub Release, with its SHA-256 in the release notes.
- `README.md`: what Formwork is, a screenshot in each theme, what works, what doesn't yet (copied honestly from Known gaps), credit to NetForms, and the licence.
- `TESTING-FOR-JACOB.md`, written for someone who isn't technical, step by step with no unexplained jargon:
  1. Install the .NET 10 SDK on Linux Mint 21.3 (exact commands, what each does, and how to check it worked).
  2. Install the plugin zip in Rider.
  3. Open a sample, try the definition-of-done story, and what he should see at each step.
  4. Make one of his own college projects work on Linux, then check it at college.
  5. How to report a problem: what to copy from Rider's logs (**Help → Show Log in Files**), and where to paste it.
- `CLAUDE.md` ends with a final honest summary: what's Verified, what's Partial, the known gaps, and the three most useful next steps.

When everything is done before the deadline, spend the remaining time on Known gaps, in order of how often a student would hit them.
