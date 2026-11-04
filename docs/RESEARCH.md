# Research notes (Stage 0)

What I learned reading NetForms v0.1.5 (`external/NetForms`, MIT) and its docs, 4 Nov 2026.
NetForms' own notes (`CLAUDE.md`, `docs/PLAN.md`) are written in Russian; summaries here are mine.

## What NetForms is

- A reimplementation of `System.Windows.Forms` + `System.Drawing` for **.NET 10 only**
  (`lib/net10.0`). Same namespaces, same API, so WinForms code compiles unchanged.
- Its own control tree, painted with SkiaSharp. Avalonia 12 is used only as the OS layer (window,
  input, IME, clipboard, native dialogs). Its theme "looks like WinForms but cleaner"; pixel
  compatibility with the Win32 theme is **not** a goal.
- NuGet package `NetForms`, newest stable **0.1.5** (16 Oct 2026). Git tag `v0.1.5` =
  commit `073cb13`. That is what the submodule is pinned to.
- Runs on Linux x64/arm64 with X11 (or XWayland). Needs the .NET 10 runtime, fontconfig, libX11,
  libICE, libSM, a TrueType font. Mint 21.3 desktop has all of these.
- Not supported: `net8.0-windows`-style TFMs on Linux (needs the real Windows Desktop runtime),
  VB.NET, BinaryFormatter in .resx (except ImageList streams, which NetForms reads itself),
  WebView2/CefSharp/ActiveX/COM, third-party WinForms NuGet controls (strong-name issue).
- When Segoe UI isn't installed, the nearest sans-serif font is used, so text sizes differ a little.

## The designer (two halves)

1. **Host** (`tools/NetFormsDesigner.Host`, logic in `src/NetForms.Design` and
   `src/NetForms.Design.Serialization`): reads `InitializeComponent()` with Roslyn (no compile),
   builds the live form under a headless platform (no windows, timers don't tick), paints it to PNG,
   and writes `*.Designer.cs` back in the style the file already uses (old `this.button1` style or
   the newer .NET style). Handler stubs go into `Form1.cs`; renames use Roslyn symbols so only real
   references change. Images go into `Form1.resx`. Protocol: see `docs/PROTOCOL.md`.
2. **VS Code front end** (`designer/src/*.ts` + webview `designer/media/designer.js`, `.css`,
   `geometry.js`). The webview is the whole editing chrome: toolbox, canvas overlays (selection,
   handles, snaplines, drag), property grid, events, outline, tab order, format commands.

### How tied to VS Code is the webview?

Barely: `acquireVsCodeApi()` (postMessage / getState / setState), `window` `message` events, and
about 40 `--vscode-*` CSS colour variables. **Decision: reuse it inside JCEF** with a JS shim that
provides `acquireVsCodeApi` over a `JBCefJSQuery`, and Formwork sets the `--vscode-*` variables
from Rider's current theme colours. The TypeScript half (`designerEditorProvider.ts`) is rewritten
in Kotlin.

### Things that needed changing for Rider

- **The host writes files to disk itself** (`DesignSurface.Save`, after every edit when autoSave
  is on), and reads them from disk before each edit. The VS Code extension even saves your open
  editors first. That would break Rider's undo and the `*` marker. Fix: a small NetForms patch
  (`upstream-patches/0001-designer-file-overrides.patch`) adds read/write hooks, and Formwork's
  host (`host/Formwork.Host`) uses them so files come from Rider's buffers and writes go back to
  Rider as text.
- **Beige form.** Off Windows .NET's system colour table is Windows XP's (`Control = #ECE9D8`).
  NetForms ships the Windows 10/11 table but only its tests switch it on. Formwork's host switches
  it on (designer only), so the form is `#F0F0F0` as in Visual Studio. Checked: rendered PNG
  pixel is `#F0F0F0`.

## Converter (`tools/NetForms.Convert`)

Rewrites a WinForms project for NetForms only (`UseWindowsForms` → `PackageReference NetForms`,
`net8.0-windows` → `net10.0`), backup `*.csproj.winforms.bak`. Handles old .NET Framework projects
too. It makes a Linux-only project, which would break Jacob's college build, so Formwork writes the
dual-target project from brief section 7 instead (its own code, using the converter's findings as
a guide).

## Templates (`templates/`)

`netforms-app`, `-form`, `-usercontrol`, `-dialog`, `-aboutbox`, `-login`, `-splash`
(NuGet package `NetForms.Templates`). The plain Microsoft "Windows Forms App" template is in the
.NET SDK itself (`dotnet new winforms -f net8.0|net9.0|net10.0`); it's the same template Visual
Studio 2022 uses for modern .NET, so `samples/VsTemplate.Net*` were generated with it.

## Measured in Stage 0

- Host start + open + add a button + render: **0.56 s** total (cold process).
- Output for "add Button at (300,100)" in the .NET 8 template matches Visual Studio's new-style
  serializer output (field `private Button button1;`, `// button1` comment block, `Location`,
  `Name`, `Size`, `TabIndex`, `Text`, `UseVisualStyleBackColor`, `Controls.Add`, `Name = "Form1"`,
  `SuspendLayout/ResumeLayout(false)`).
