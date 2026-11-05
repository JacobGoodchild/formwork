#!/usr/bin/env bash
# Stage 1 checks in the sandbox IDE (Xvfb :99): F7, two forms, outside edits reload the canvas, broken file.
# Test-machine helper (a throwaway cloud machine with a virtual screen). Not needed on your own computer.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/build/checks}"; mkdir -p "$out"
work="$(mktemp -d)/checks"; cp -r "$root/samples/TwoForms" "$work"
proj="$work/TwoForms"
"$root/tools/run-idea.sh" "$proj" "$out/runidea.log" "$proj/Form1.cs" >/dev/null
export DISPLAY=:99
shot() { "$root/tools/shot.sh" "$out/$1.png" 100 1400x960+260+40; }
for i in $(seq 1 60); do sleep 2; grep -q "Designer page ready" $root/.intellijPlatform/sandbox/formwork/*/log_runIdea/idea.log 2>/dev/null && [ "$i" -gt 5 ] && break; done
sleep 6
shot 01-form1-design
xdotool mousemove 1300 700 click 1; sleep 1; xdotool key F7; sleep 3
shot 02-after-F7
xdotool key shift+F7; sleep 3
shot 03-after-shift-F7
# Open Form2 with Go to File (Ctrl+Shift+N), as a user would.
xdotool key ctrl+shift+n; sleep 2; xdotool type --delay 60 "Form2.cs"; sleep 2; xdotool key Return; sleep 8
shot 04-two-forms-open
# An outside change to Form1.Designer.cs (like a git checkout), then File → Reload All from Disk.
sed -i 's/btnOpen.Text = "Open Form2";/btnOpen.Text = "Changed outside";/' "$proj/Form1.Designer.cs"
xdotool key ctrl+shift+n; sleep 2; xdotool type --delay 60 "Form1.cs"; sleep 2; xdotool key Return; sleep 4
xdotool key ctrl+alt+y; sleep 5
shot 05-after-outside-change
# A broken designer file: the designer shows an error and doesn't write anything.
cp "$proj/Form1.Designer.cs" "$out/Form1.Designer.cs.before-break"
sed -i 's/ClientSize = new Size(\([0-9]*\), \([0-9]*\));/ClientSize = new Size(\1, \2;/' "$proj/Form1.Designer.cs"
cp "$proj/Form1.Designer.cs" "$out/Form1.Designer.cs.broken"
xdotool key ctrl+alt+y; sleep 5
shot 06-broken-file
sleep 2
cmp "$proj/Form1.Designer.cs" "$out/Form1.Designer.cs.broken" && echo "BROKEN FILE LEFT UNTOUCHED"
echo "$proj"
