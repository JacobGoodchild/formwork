#!/usr/bin/env bash
# The definition-of-done story in the sandbox IDE, with a screenshot of each step (Xvfb :99, 1920x1080).
# Usage: tools/story.sh <sample folder under samples/> <output dir>
# Coordinates fit the default IDE layout (Project view open on the left); see docs/screenshots.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
sample="${1:-VsTemplate.Net8}"; out="${2:-$root/build/story}"
mkdir -p "$out"
work="$(mktemp -d)/story"
cp -r "$root/samples/$sample" "$work"
proj="$(dirname "$(find "$work" -name '*.csproj' | head -1)")"
"$root/tools/run-idea.sh" "$proj" "$out/runidea.log" "$proj/Form1.cs" >/dev/null
export DISPLAY=:99
shot() { "$root/tools/shot.sh" "$out/$1.png" 100 1400x960+260+40; }
for i in $(seq 1 60); do sleep 2; grep -q "Project frame set to" $root/.intellijPlatform/sandbox/formwork/*/log_runIdea/idea.log 2>/dev/null && [ "$i" -gt 10 ] && break; done
sleep 8
sleep 6
shot 02-designer-open
xdotool mousemove 802 327 click 1; sleep 1          # Button in Common Controls
shot 03-button-tool-selected
xdotool mousemove 1117 397 click 1; sleep 3         # click on the form
shot 04-button-placed
xdotool mousemove 1153 408 click --repeat 2 --delay 80 1; sleep 5   # double-click button1
shot 05-handler-in-code
xdotool key ctrl+s; sleep 2
cp "$proj/Form1.cs" "$out/Form1.cs.after-save"; cp "$proj/Form1.Designer.cs" "$out/Form1.Designer.cs.after-save"
xdotool key shift+F7; sleep 3
shot 06-back-to-designer
xdotool mousemove 1300 700 click 1; sleep 1; xdotool key ctrl+z; sleep 3
shot 07-after-ctrl-z
echo "$proj"
