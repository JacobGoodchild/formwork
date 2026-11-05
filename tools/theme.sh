#!/usr/bin/env bash
# Picks the sandbox IDE's theme before it starts: tools/theme.sh light|dark (the 2026.2 defaults, Islands Light/Dark).
# Test-machine helper (a throwaway cloud machine with a virtual screen). Not needed on your own computer.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
for opts in "$root"/.intellijPlatform/sandbox/formwork/*/config_runIdea/options; do
  mkdir -p "$opts"
  if [ "${1:-dark}" = light ]; then theme="Islands Light"; scheme=Light; else theme="Islands Dark"; scheme="Islands Dark"; fi
  printf '<application>\n  <component name="LafManager" autodetect="false">\n    <laf themeId="%s" />\n  </component>\n</application>\n' "$theme" > "$opts/laf.xml"
  printf '<application>\n  <component name="EditorColorsManagerImpl">\n    <global_color_scheme name="%s" />\n  </component>\n</application>\n' "$scheme" > "$opts/colors.scheme.xml"
done
