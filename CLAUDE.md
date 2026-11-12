# CLAUDE.md — Penguin Forms developer notes

Read this file and `git log` first. Never redo committed work. Keep the "Status" section at the end up to date
(what changed, how it was checked, what couldn't be checked), and commit after each working step.

## What this is

A JetBrains Rider plugin (Kotlin) that gives Rider on Linux a Windows Forms designer that looks and works like
Visual Studio 2022's. Users' code stays exactly as Visual Studio writes it, so a project opens unchanged in Visual
Studio on Windows. Built on [NetForms](https://github.com/Go-Forms/.NetForms) (MIT): a cross-platform
System.Windows.Forms plus a design-time host.

- **Plugin** (`src/main`): Design tab (`FormDesignFile`, `penguinforms-design://` file system), JCEF page, native
  Toolbox / Properties / Document Outline tool windows (available only once a form is opened in the designer),
  Format menu, F7 / Shift+F7, "Make This Project Work on Linux Too", Add Form, New Project, settings.
- **Host** (`host/Formwork.Host`, .NET 10): wraps NetForms' designer host; reads the editor's buffers and returns
  file writes as text, which the plugin applies through Rider documents (one undoable command per gesture). Talks
  JSON lines over stdin/stdout: `docs/development/PROTOCOL.md`. Internal names keep "Formwork" (`Formwork.Host`,
  `formwork.hello`, the `Formwork Sans` font); users never see them.
- **Page**: NetForms' `designer/media/designer.js` + `geometry.js`, with `src/main/resources/penguinforms/webview/`
  (`bridge.js` replaces VS Code's messaging, `penguinforms.js`).
- **NetForms** is a git submodule pinned to `v0.1.5` (`external/NetForms`), changed only through
  `upstream-patches/NNNN-*.patch`, each explained in `upstream-patches/README.md`. `host/apply-patches.sh` applies
  them in the build. When you change NetForms files, add a new patch (or regenerate the one that owns the lines):
  the patch is made against the tree with all earlier patches applied, and must apply cleanly.
- **Project converter** (`ProjectConverter.kt`): writes a two-world `.csproj` (real WinForms when `'$(OS)' ==
  'Windows_NT'`, NetForms otherwise), minimal text edits, backup `<name>.csproj.penguinforms.bak` first. Golden
  files in `src/test/resources/golden/projects`.

## Rules

- **Honest statuses.** "Verified" needs a test and, for anything visible, a screenshot in `docs/screenshots/` that
  you have looked at. Say plainly what wasn't checked.
- Never log in to anything. No telemetry or network requests of the plugin's own.
- Start processes with argument lists (`GeneralCommandLine`), never a shell; never pass user text to a shell.
- Load a project's build output (custom controls) only in trusted projects.
- Back up a `.csproj` before changing it; `.cs` edits go through Rider documents so Ctrl+Z works.
- Pin every dependency to an exact version (NuGet is lock-file checked; Gradle wrapper checksum-pinned).
- No orphan host processes (`HostRegistry`), no secrets in the repo (signing / publishing read env vars only).
- Don't copy Visual Studio's icons, images or code; recreate layout and behaviour. Microsoft's open-source
  dotnet/winforms (MIT) may be read as a specification.
- Everything users see is plain English, without jargon, and escaped (`UiText`) because the IDE renders HTML.
- MIT licence. No other companies' trademarks in the product name. Publishing to the Marketplace is the
  maintainer's decision: never publish, tag a release or open a pull request unless asked.

## Building and testing

- `./gradlew buildPlugin` → `build/distributions/penguin-forms-<version>.zip` (needs Java 21, .NET 10 SDK).
  Built against IntelliJ IDEA 2026.1.5.1 (the oldest supported platform); `-PriderOnly=true` for the Marketplace
  build; CI also compiles against 2026.2 and runs JetBrains' plugin checker against Rider 2026.1 / 2026.2 by hand
  (`verify-plugin`).
- `./gradlew test` (JUnit 5): host protocol, code generation, converter goldens, shared rows, .NET locator, VS
  designer extras. `FORMWORK_BUILD_TESTS=1` also builds every converted sample.
- UI checks run in the free tier of IntelliJ IDEA (the sandbox Rider needs a login): `tools/run-idea.sh <project>
  <log> [file]` on an Xvfb display `:99` (no window manager), `tools/shot.sh` for screenshots. `UISCALE=2` simulates a
  200 % screen; `RENDERSCALE=1` forces the old 1× canvas. Stop the test IDE before `./gradlew test` (Gradle waits
  for it). These scripts are for a throwaway test machine.
- CI (`.github/workflows/ci.yml`): tests + plugin zip on Linux; every converted sample builds on Windows with the
  real Windows Forms, including Visual Studio's design-time build.

## Status (10 Nov 2026)

Stages 0–5 of the original plan are done; the feature checklist was verified in IntelliJ IDEA 2026.1 / 2026.2 and the
plugin was then tested in real Rider 2026.1. Fixes from that Rider test (all checked in the test IDE, screenshots
`docs/screenshots/fix-*`):

1. Sharp canvas on scaled screens: the host paints at the page's pixel density (patch 0007), shown at CSS size.
2. `.NET` locator collects every candidate and uses the first with a .NET 10+ runtime; errors list where it looked
   (`DotnetLocatorTest`).
3. Tab / Shift+Tab reach the page (Swing focus traversal keys off on the browser). Not reproducible in the test IDE
   (Tab already worked there); Ctrl+Tab still opens the Switcher.
4. Right-click → Properties shows and focuses the Properties tool window (patch 0008).
5. Multi-select: Properties always shows the shared rows (blank where values differ); a late single-control answer
   no longer replaces them, and stale shared answers are dropped.
6. Running app: first ComboBox drop-down filled a scaled screen (reproduced with Cinnamon's window manager, muffin,
   at 2×). Fixed in NetForms' Avalonia window (patch 0009). Apps use NetForms from NuGet, so they only get it once
   NetForms ships it.
7. Messages escaped (`<UseWindowsForms>` was swallowed as HTML).
8. Tool window hints wrap and are centred (`HintCard`).
9. Toolbox / Properties / Document Outline unavailable until a form is opened in the designer.
10. Repository tidied for publishing: user guide `docs/USER-GUIDE.md`, developer docs in `docs/development/`.

**Pre-publishing check (10 Nov):** no secrets, keys, emails or private paths in the repo; no user-visible "Formwork"
left (settings dialog title and plugin description fixed); code safety rules re-checked (no shell, no network but the
user-clicked help link, CSP on the page, project code only in trusted projects, deletes only in our own temp folders,
CI read-only with no secrets in pull requests). Found and fixed: the third-party libraries bundled in the host (Avalonia,
SkiaSharp/Skia, HarfBuzzSharp/HarfBuzz, Roslyn, System.*, MicroCom, Tmds.DBus) shipped without their notices; now
`THIRD-PARTY-NOTICES.md` plus each package's own licence files go into the plugin's `licenses/` folder (the build
fails if one is missing). Trademarks are only used to say what the plugin works with; the icon is our own drawing.

**Not yet checked in real Rider:** all of the above (the test IDE is IntelliJ IDEA). Rider-only things worth checking
after any big change: Solution Explorer nesting, Run / Debug, only one designer opening, the two-tab layout and its
restore after a restart.

**Known gaps:** no WebBrowser control; rename doesn't reach files other than the form's own; a few default text
widths differ 1–2 px from Visual Studio; old .NET Framework projects convert but the Windows half is only checked by
golden file and CI; buttons in the *running* app on Linux are Windows 10 grey (the white Windows 11 face is patch 0004,
designer only); snap-to-grid while dragging isn't implemented; Visual Studio opens a closed menu when you hover over
it while dragging a menu item (here you drop on its title).

**Published:** version 0.1.0 approved on the JetBrains Marketplace on 12 Nov 2026,
<https://plugins.jetbrains.com/plugin/35992-penguin-forms>. It was built signed by CI's hand-started `verify-plugin`
job (artifact `penguin-forms-marketplace`), and the plugin checker found it compatible with Rider 2026.1 and 2026.2
(warnings only: `TrustedProjects.isTrusted` and `ActionUtil.invokeAction`). Updates need a higher `pluginVersion`
and a change-notes entry; publishing them from CI needs a Marketplace token as the `PUBLISH_TOKEN` secret (see
`docs/development/MARKETPLACE.md`, "Later updates").
