#!/usr/bin/env bash
# Starts the sandbox IntelliJ IDEA (free tier) with Formwork on the Xvfb display :99, opening $1.
# Used for UI screenshots; see CLAUDE.md "BLOCKER" for why not Rider.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
pgrep -x Xvfb >/dev/null || (Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >/dev/null 2>&1 &)
pkill -f "idea-2026.2.4" || true
sleep 2
export DISPLAY=:99 PATH="/opt/dotnet:$PATH"
cd "$root"
extra=()
[ -n "${3:-}" ] && extra=(-PopenFile="$3")
nohup ./gradlew runIdea -PopenPath="$1" "${extra[@]}" --console=plain > "${2:-/tmp/formwork-runidea.log}" 2>&1 &
echo "started"
