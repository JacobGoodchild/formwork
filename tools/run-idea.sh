#!/usr/bin/env bash
# Starts the sandbox IntelliJ IDEA (free tier) with Formwork on the Xvfb display :99, opening $1.
# Test-machine helper (a throwaway cloud machine with a virtual screen). Not needed on your own computer.
# Used for UI screenshots; see CLAUDE.md "BLOCKER" for why not Rider.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
pgrep -x Xvfb >/dev/null || (Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >/dev/null 2>&1 &)
pkill -f "formwork/.intellijPlatform/sandbox" || true  # only the test IDE started from this repo
sleep 2
export DISPLAY=:99 PATH="/opt/dotnet:$PATH"
cd "$root"
extra=()
[ -n "${3:-}" ] && extra=(-PopenFile="$3")
nohup ./gradlew runIdea -PopenPath="$1" "${extra[@]}" --console=plain > "${2:-/tmp/formwork-runidea.log}" 2>&1 &
# No window manager on Xvfb: once the IDE window exists, make it fill the screen (as a maximised window would).
( for i in $(seq 1 120); do sleep 2
    for w in $(xdotool search --onlyvisible --name "\[Design\]| – " 2>/dev/null); do
      g=$(xdotool getwindowgeometry "$w" | grep -o '[0-9]*x[0-9]*'); [ "$g" = "1920x1080" ] && exit 0
      [ "${g%x*}" -gt 800 ] 2>/dev/null && xdotool windowmove "$w" 0 0 windowsize "$w" 1920 1080
    done; done ) >/dev/null 2>&1 &
echo "started"
