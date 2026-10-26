#!/usr/bin/env bash
# Offline build without Gradle/AGP: aapt2 + kotlinc + d8 + apksigner.
# Used to verify the project in environments without access to Google Maven.
# Normal builds should use ./gradlew assembleDebug.
set -euo pipefail
exec 2> >(grep -v "Picked up JAVA_TOOL_OPTIONS" >&2)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK:-/opt/sdk}"
BT="$SDK/build-tools/36.1.0"
AJAR="$SDK/platforms/android-36/android.jar"
KOTLINC="${KOTLINC:-/opt/kotlinc/bin/kotlinc}"
STDLIB="$(dirname "$KOTLINC")/../lib/kotlin-stdlib.jar"
OUT="$ROOT/build-offline"
MAIN="$ROOT/app/src/main"
rm -rf "$OUT"; mkdir -p "$OUT"/{res,classes,dex}

echo "[1/5] aapt2"
"$BT/aapt2" compile --dir "$MAIN/res" -o "$OUT/res.zip"
sed 's|<manifest |<manifest package="com.hgame.arcade" |' "$MAIN/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$AJAR" --manifest "$OUT/AndroidManifest.xml" \
  --min-sdk-version 24 --target-sdk-version 36 --version-code 1 --version-name 1.0 \
  --auto-add-overlay "$OUT/res.zip"

echo "[2/5] kotlinc"
find "$MAIN/java" -name '*.kt' > "$OUT/sources.txt"
"$KOTLINC" @"$OUT/sources.txt" -cp "$AJAR" -d "$OUT/classes" -jvm-target 17 -nowarn
test -n "$(find "$OUT/classes" -name '*.class' | head -1)" || { echo "compile failed"; exit 1; }

echo "[3/5] d8"
"$BT/d8" --release --min-api 24 --lib "$AJAR" --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class') "$STDLIB"

echo "[4/5] package"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -j "$OUT/unsigned.apk" classes*.dex)
"$BT/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "[5/5] sign"
KS="$ROOT/tools/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
  -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -validity 10000 >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --out "$OUT/hgame.apk" "$OUT/aligned.apk"
ls -la "$OUT/hgame.apk"
