#!/usr/bin/env bash
# Applies upstream-patches/*.patch to the NetForms submodule, once. The submodule stays pinned to the
# public release tag; these patches sit on top in its working tree. Each patch is explained in
# upstream-patches/README.md.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
netforms="$root/external/NetForms"
if [ ! -f "$netforms/NetForms.slnx" ]; then
  echo "NetForms submodule is missing. Run: git submodule update --init" >&2
  exit 1
fi
for patch in "$root"/upstream-patches/*.patch; do
  if git -C "$netforms" apply --reverse --check "$patch" 2>/dev/null; then
    echo "already applied: $(basename "$patch")"
  else
    git -C "$netforms" apply "$patch"
    echo "applied: $(basename "$patch")"
  fi
done
