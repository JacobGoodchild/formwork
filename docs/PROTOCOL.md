# The designer host protocol

Penguin Forms talks to a child process, **Formwork.Host** (`host/Formwork.Host`), which wraps NetForms'
`DesignerProtocol` (`external/NetForms/src/NetForms.Design/DesignerProtocol.cs`), the same protocol
NetForms' VS Code extension uses (`external/NetForms/designer/src/hostClient.ts`).
Learned by reading those two files and by driving the host by hand (Stage 0).

## Transport

- Started as `dotnet Formwork.Host.dll` with an argument list (never through a shell).
- **One JSON object per line** on stdin, **one JSON object per line** on stdout, UTF-8, no BOM.
- Answers come **in order**, matched by `id`. stderr is free-form log text (anything a control
  prints with `Console.WriteLine` goes there, so it can't corrupt stdout).
- One host process per open form (as in VS Code). `close` makes it exit after answering.

## Request / response

```json
{"id": 3, "method": "apply", "params": {"ops": [ ... ]}, "files": {"/abs/Form1.Designer.cs": "..."}}
{"id": 3, "result": { ...DesignerView... }, "writes": {"/abs/Form1.Designer.cs": "...", "/abs/Form1.cs": "..."}}
{"id": 3, "error": {"kind": "edit" | "code" | "internal", "message": "...", "file": "...", "line": 12, "column": 5}}
```

- `kind: "code"`: the designer file can't be read (with its position). Nothing is written.
- `kind: "edit"`: an edit was refused; nothing changed.
- `kind: "internal"`: a bug; `detail` has the stack trace.

### Penguin Forms additions (not in NetForms' host)

| Field | Direction | Meaning |
|---|---|---|
| `files` | request | `{path: text or null}`: what Rider's editor holds for the form's files right now (unsaved edits included). The host reads these instead of the disk. `null` = the file does not exist. |
| `writes` | response | `{path: text}`: the full new text of every file the request changed. **The host never writes to disk.** Penguin Forms applies each write to Rider's `Document` inside a write command, so Ctrl+Z, the `*` marker and the file watcher all stay correct. |
| `formwork.hello` | method | `{name, protocol, formwork}` |

These rest on a 16-line NetForms patch (`upstream-patches/0001-designer-file-overrides.patch`):
`DesignSurface.ReadFileOverride` / `WriteFileOverride`. The host runs with `autoSave` on, so every
change is "saved" straight into `writes`.

The host also switches on NetForms' Windows 10/11 system colour table (`SystemColors.Control` =
`#F0F0F0`) for the designer, since VS always draws the form in light Windows style.

## Methods (from DesignerProtocol)

| Method | Params | Result |
|---|---|---|
| `hello` | – | `{name, protocol: 1}` |
| `open` | `{path, autoSave = true}` (path = the `*.Designer.cs`) | view |
| `render` | – | view |
| `menu` | `{id}` – open that menu item's drop-down on the canvas; `""` closes | view |
| `apply` | `{ops: [op…]}` – one undoable step, all or nothing | view |
| `undo` / `redo` | – (Penguin Forms uses Rider's undo instead, see below) | view |
| `copy` | `{ids}` | `{text}` (clipboard text, marker `netforms/components-1`) |
| `properties` | `{id}` | property rows |
| `events` | `{id}` | event rows |
| `toolbox` | `{libraries?}` | toolbox categories |
| `libraries` | `{assemblies: [path…]}` – load the project's build output | `{assemblies, errors, view?, error?}` |
| `scan` / `scanPackage` | `{paths}` / `{path}` | what an assembly offers, without running it |
| `save` | – | `{saved}` |
| `close` | – | `{closed}`; process exits |

Component ids are names: `""` is the root (the form), `button1`, `splitContainer1.Panel1`.

### Ops (`DesignerOp`)

`setBounds {id,x,y,w,h}` · `setForm {w,h}` · `setProp {id,prop,value}` · `resetProp {id,prop}` ·
`setItems {id,prop,values,ids?}` · `setEvent {id,event,handler}` ("" unbinds; a missing method is
added to `Form1.cs`) · `add {type,parent,x,y,w?,h?,column?,row?,index?}` · `remove {id}` ·
`setParent {id,parent,x,y | column,row | index}` · `setImage {id,prop,value=path}` ·
`setImages` · `reorder {id,ids}` · `setCell {id,column,row}` · `setStyles` · `rename {id,value}` ·
`paste {value,parent,x?,y?}` · `duplicate {ids}` · `bringToFront {id}` · `sendToBack {id}`.

### The view (`DesignerView`)

`className, rootType, text, width, height` (client size), `png` (base64 PNG of the client area,
painted by NetForms), `items[]` (every component: `id, type, parent, kind, x, y, w, h`, client
offsets, `container, movable, resizable, dock, anchor, margin, padding`, tab headers, table grid…),
`tray[]` (non-visual components), `canUndo, canRedo`, `handler?` (`{file, method, line, created}`
after an event was bound), `select?` (new ids after paste), `dropDowns?`, `openMenu?`.

## How Penguin Forms uses it

1. Editor opens → start host → `formwork.hello` → `libraries` (trusted projects only) → `open`
   with `files` = the current text of `Form1.Designer.cs`, `Form1.cs`, `Form1.resx`.
2. A canvas gesture → `apply` (with `files` again if a buffer changed) → apply `writes` to the
   documents as **one** Rider command (one Ctrl+Z step) → show the new view.
3. Ctrl+Z / Ctrl+Y on the canvas run **Rider's** undo/redo on those documents; the documents change
   → Penguin Forms sends `open` with the new `files` → the canvas shows the old state. (The host's own
   undo stack is not used, so there is only one undo history.)
4. `Form1.Designer.cs` edited as text, or changed by git → same reload.
5. Editor closes → `close`, then the process is killed after 2 s if still alive. Project close and
   IDE exit kill every host.
