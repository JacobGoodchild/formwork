#!/usr/bin/env bash
# The definition-of-done story in the sandbox IDE, with a screenshot of each step (Xvfb :99, 1920x1080).
# Test-machine helper (a throwaway cloud machine with a virtual screen). Not needed on your own computer.
# Usage: tools/story.sh <sample folder under samples/> <output dir>
# Coordinates fit the default IDE layout (Project view open on the left); see docs/screenshots.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
sample="${1:-VsTemplate.Net8}"; out="${2:-$root/build/story}"
mkdir -p "$out"
work="$(mktemp -d)/story"
cp -r "$root/samples/$sample" "$work"
# Already converted (as after "Make it work on Linux too"), so its notice does not cover the screen.
[ -d "$root/build/converted-samples/$sample" ] && rm -rf "$work" && cp -r "$root/build/converted-samples/$sample" "$work"
proj="$(dirname "$(find "$work" -name '*.csproj' | head -1)")"
log="$(ls $root/.intellijPlatform/sandbox/penguin-forms/*/log_runIdea/idea.log 2>/dev/null | head -1)"
before=$( [ -f "$log" ] && wc -l < "$log" || echo 0 )
"$root/tools/run-idea.sh" "$proj" "$out/runidea.log" "$proj/Form1.cs" >/dev/null
export DISPLAY=:99
shot() { "$root/tools/shot.sh" "$out/$1.png" 75; }
# Wait for this run's designer page (only log lines written after the start count).
for i in $(seq 1 90); do sleep 2; tail -n +"$((before + 1))" "$log" 2>/dev/null | grep -q "Designer page ready" && break; done
sleep 4
# No window manager on Xvfb: make the IDE window fill the screen, as a maximised window.
for w in $(xdotool search --onlyvisible --name "." 2>/dev/null); do
  g=$(xdotool getwindowgeometry "$w" | grep -o '[0-9]*x[0-9]*'); [ "${g%x*}" -gt 800 ] 2>/dev/null && xdotool windowmove "$w" 0 0 windowsize "$w" 1920 1080
done
sleep 4
shot 02-designer-open
xdotool mousemove 132 214 click 1; sleep 1          # Button in the native Toolbox (Common Controls)
shot 03-button-tool-selected
xdotool mousemove 500 300 click 1; sleep 3          # click on the form
shot 04-button-placed
xdotool mousemove 537 311 click --repeat 2 --delay 80 1; sleep 5   # double-click button1
shot 05-handler-in-code
xdotool key ctrl+s; sleep 2
cp "$proj/Form1.cs" "$out/Form1.cs.after-save"; cp "$proj/Form1.Designer.cs" "$out/Form1.Designer.cs.after-save"
xdotool key shift+F7; sleep 3
shot 06-back-to-designer
xdotool mousemove 1200 800 click 1; sleep 1; xdotool key ctrl+z; sleep 3
shot 07-after-ctrl-z-handler-gone
xdotool key ctrl+z; sleep 3
shot 08-after-second-ctrl-z-button-gone
echo "$proj"
