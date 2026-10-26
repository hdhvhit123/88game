#!/usr/bin/env bash
# Launches each game on a connected device/emulator, taps around and checks for crashes.
# Usage: tools/smoke.sh [game-id ...]   (screenshots go to build-offline/shots)
set -uo pipefail
export PATH="$PATH:/opt/sdk/platform-tools"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG=com.hgame.arcade
OUT="$ROOT/build-offline/shots"; mkdir -p "$OUT"
IDS=("$@")
if [ ${#IDS[@]} -eq 0 ]; then
  mapfile -t IDS < <(grep -oE 'g\("[a-z0-9-]+"' "$ROOT/app/src/main/java/com/hgame/games/Catalog.kt" | cut -d'"' -f2)
fi
W=720; H=1440
fail=0
for id in "${IDS[@]}"; do
  adb logcat -c
  adb shell am force-stop $PKG
  adb shell am start -n $PKG/com.hgame.app.MainActivity --es game_id "$id" >/dev/null
  sleep "${WAIT:-9}"
  adb exec-out screencap -p > "$OUT/$id-0.png"
  for k in $(seq 1 "${TAPS:-12}"); do
    x=$((RANDOM % (W - 40) + 20)); y=$((RANDOM % (H - 400) + 300))
    if (( k % 4 == 0 )); then adb shell input swipe $x $y $(( (x + 200) % W )) $y 120
    else adb shell input tap $x $y; fi
    sleep 0.4
  done
  sleep 2
  adb exec-out screencap -p > "$OUT/$id-1.png"
  if adb logcat -d -s AndroidRuntime:E | grep -q "FATAL EXCEPTION"; then
    echo "CRASH $id"; adb logcat -d -s AndroidRuntime:E | head -30; fail=1
  else
    echo "ok    $id"
  fi
done
exit $fail
