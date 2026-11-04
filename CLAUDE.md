# CLAUDE.md — Formwork

Read this file and `git log` first, every session. Never redo committed work.
Part 1 is the progress log (kept up to date). Part 2 is a copy of Jacob's brief.

Wake-ups: scheduled with `send_later` (one ~60 min ahead after each commit, plus a backup
chain at 21:05, 22:35, 00:05, 01:35, 03:05, 04:35, 06:05 UK). Deadline: 07:00 UK, 5 Nov 2026.

---

# Part 1 — Progress log

## Status

**Current stage:** Stage 0 (research and pipeline proof). Started 19:33 UK, 4 Nov 2026.

## Network check (Stage 0, 19:35)

| What | Result |
|---|---|
| .NET SDK download (dot.net install script) | OK — .NET SDK 10.0.403 installed to /opt/dotnet |
| NuGet (api.nuget.org) | OK — NetForms versions up to 0.1.5 listed |
| JetBrains Rider SDK (intellij-repository) | OK (metadata reachable) |
| Gradle plugin portal, Maven Central | OK |
| GitHub (git clone of Go-Forms/.NetForms) | OK over git; plain https page browsing is blocked by the session proxy, which is fine |

## Decisions (with reasons)

1. NetForms pinned to tag `v0.1.5` (newest stable release; same as newest NuGet `0.1.5`).
2. Canvas route: reuse NetForms' webview (`designer/media/designer.js`) in JCEF. Reason: it only
   touches VS Code through `acquireVsCodeApi()` and `window` messages, so a small shim is enough.

## Feature checklist (statuses: Not started / Partial / Done (unverified) / Verified)

### A. Opening and switching (Must)
- [ ] Double-click Form1.cs opens designer; Designer.cs openable as text — Not started
- [ ] Design and Code tabs; tab title `Form1.cs [Design]` + `*` — Not started
- [ ] F7 / Shift+F7; right-click menu; Find Action — Not started
- [ ] Several forms open at once — Not started
- [ ] Editing Designer.cs as text / git checkout reloads canvas — Not started

### B. Toolbox (Must)
- [ ] Groups, order, contents; Pointer first — Not started
- [ ] Search filters as you type — Not started
- [ ] Click tool then click/drag on canvas — Not started
- [ ] Drag a tool onto canvas / into container — Not started
- [ ] Double-click tool adds to selected container — Not started
- [ ] Toolbox returns to Pointer after placing — Not started
- [ ] Project's own controls group after build — Not started
- [ ] Non-visual components go to the tray — Not started

### C. Canvas (Must)
- [ ] Click / Ctrl/Shift+click / rubber-band select — Not started
- [ ] Primary selection white handles, others dark — Not started
- [ ] Move, resize, form resizes from right/bottom/corner — Not started
- [ ] Snaplines (blue edges, magenta baselines, spacing) — Not started
- [ ] Keyboard nudge/resize as VS — Not started
- [ ] Tab / Shift+Tab, Escape selects parent — Not started
- [ ] Drop into containers; move glyph — Not started
- [ ] Delete, Ctrl+X/C/V, Ctrl+A — Not started
- [ ] Undo/redo joined with Rider's undo — Not started
- [ ] Docked controls: free-edge resize only — Not started

### D. Properties and Events (Must)
- [ ] Selection drop-down; Categorized/Alphabetical; bold non-defaults; description pane — Not started
- [ ] Editors: text, numbers, bool/enum, flags, expandable Size/Point/Padding/Font — Not started
- [ ] Pop-up editors: Anchor, Dock, Color, Font, ContentAlignment, multi-line Text, Cursor — Not started
- [ ] Collection editors — Not started
- [ ] Image/BackgroundImage/Icon pickers into .resx — Not started
- [ ] AcceptButton / CancelButton drop-downs — Not started
- [ ] Right-click property → Reset — Not started
- [ ] Multi-selection shared properties — Not started
- [ ] (Name) rename incl. code (Ctrl+Z works) — Not started
- [ ] Events tab — Not started

### E. Code generation (Must)
- [ ] Double-click control creates default handler, jumps into it — Not started
- [ ] Existing handler: jump instead — Not started
- [ ] Designer.cs written exactly as VS writes it — Not started
- [ ] Deleting a control keeps its handler methods — Not started
- [ ] VS names: button1, button2, textBox1… — Not started

### F. Format menu (Should)
- [ ] Align — Not started
- [ ] Make Same Size — Not started
- [ ] Horizontal / Vertical Spacing — Not started
- [ ] Center in Form — Not started
- [ ] Order — Not started
- [ ] Lock Controls — Not started

### G. Menus, strips, smart tags (Should)
- [ ] MenuStrip / ContextMenuStrip "Type Here" — Not started
- [ ] Smart-tag Tasks panel — Not started
- [ ] ToolStrip / StatusStrip item-type drop-down — Not started

### H. Special containers (Should)
- [ ] TabControl tab headers — Not started
- [ ] TableLayoutPanel cells — Not started
- [ ] FlowLayoutPanel insertion/reorder — Not started
- [ ] SplitContainer splitter — Not started

### I. Tab order and Document Outline (Should)
- [ ] View → Tab Order — Not started
- [ ] Document Outline tool window — Not started

### J. Errors and safety (Must)
- [ ] Broken Designer.cs: error page with link, never overwrites — Not started
- [ ] Failing control: red cross, rest works — Not started
- [ ] Missing .NET 10 SDK notification — Not started

### K. Projects and running (Must)
- [ ] Rider Run/Debug start the app on Linux — Not started
- [ ] "Make this project work on Linux too" command — Not started
- [ ] Add → Windows Form / User Control (+ templates) — Not started
- [ ] New project "Windows Forms App (Formwork)" — Not started

### L. Settings (Later)
- [ ] Settings page — Not started
- [ ] Data binding pickers — Not started

## Known gaps

(none recorded yet)

## What I tried that failed

(nothing yet)

## Next step

Read NetForms, write docs/RESEARCH.md and docs/PROTOCOL.md, set up the Gradle plugin project.

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
