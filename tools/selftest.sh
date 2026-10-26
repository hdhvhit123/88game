#!/usr/bin/env bash
# Runs the in-app self test over every game on a connected device and collects screenshots.
# Usage: tools/selftest.sh [from-index]
set -uo pipefail
export PATH="$PATH:/opt/sdk/platform-tools"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG=com.hgame.arcade
OUT="$ROOT/.shots"; mkdir -p "$OUT"
adb shell am force-stop $PKG
adb shell rm -rf /sdcard/Android/data/$PKG/files/shots
adb logcat -c
adb shell am start -n $PKG/com.hgame.app.MainActivity --ez selftest true --ei from "${1:-0}" >/dev/null
while true; do
  sleep 10
  LOG=$(adb logcat -d -s HGameSelfTest:I AndroidRuntime:E)
  if echo "$LOG" | grep -q "FATAL EXCEPTION"; then echo "$LOG" | grep -A25 "FATAL EXCEPTION" | head -40; break; fi
  if echo "$LOG" | grep -q " done"; then break; fi
done
echo "$LOG" | grep -E " ok | start " | tail -200 | sed 's/.*HGameSelfTest: //' | paste -sd' ' | fold -w 200
adb pull /sdcard/Android/data/$PKG/files/shots/. "$OUT" >/dev/null 2>&1
ls "$OUT" | wc -l
