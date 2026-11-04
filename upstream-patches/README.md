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
