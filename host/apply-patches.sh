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
patches=("$root"/upstream-patches/*.patch)
for i in "${!patches[@]}"; do
  patch="${patches[$i]}"
  if git -C "$netforms" apply --reverse --check "$patch" 2>/dev/null; then
    echo "already applied: $(basename "$patch")"
  elif git -C "$netforms" apply --check "$patch" 2>/dev/null; then
    git -C "$netforms" apply "$patch"
    echo "applied: $(basename "$patch")"
  else
    # Neither way round fits: a later patch changed the same lines, so this one is in already - provided that
    # later patch is applied. Anything else is a real conflict.
    later=no
    for next in "${patches[@]:$((i + 1))}"; do
      if git -C "$netforms" apply --reverse --check "$next" 2>/dev/null; then later=yes; break; fi
    done
    if [ "$later" = yes ]; then
      echo "already applied (changed again by a later patch): $(basename "$patch")"
    else
      git -C "$netforms" apply "$patch" # fails and shows why
    fi
  fi
done
