#!/usr/bin/env bash
# Makes the sandbox IDE window fill the Xvfb screen (there is no window manager to maximise it).
# Test-machine helper (a throwaway cloud machine with a virtual screen). Not needed on your own computer.
export DISPLAY=:99
for w in $(xdotool search --onlyvisible --name "." 2>/dev/null); do
  g=$(xdotool getwindowgeometry "$w" | grep -o '[0-9]*x[0-9]*'); [ "${g%x*}" -gt 800 ] 2>/dev/null && xdotool windowmove "$w" 0 0 windowsize "$w" 1920 1080
done
sleep 3
