#!/usr/bin/env bash
# Screenshot of the Xvfb display used for UI checks: tools/shot.sh out.png [scale%] [crop WxH+X+Y]
set -euo pipefail
out="$1"; scale="${2:-100}"; crop="${3:-}"
tmp="$(mktemp --suffix=.png)"
import -display "${DISPLAY:-:99}" -window root "$tmp"
if [ -n "$crop" ]; then convert "$tmp" -crop "$crop" +repage -resize "$scale%" "$out"; else convert "$tmp" -resize "$scale%" "$out"; fi
rm -f "$tmp"
