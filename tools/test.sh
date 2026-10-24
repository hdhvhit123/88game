#!/usr/bin/env bash
# Runs the pure-Kotlin unit tests (game logic under com/hgame/core) on the JVM.
set -euo pipefail
exec 2> >(grep -v "Picked up JAVA_TOOL_OPTIONS" >&2)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLINC="${KOTLINC:-/opt/kotlinc/bin/kotlinc}"
LIB="${TEST_LIBS:-/opt/dl}"
CP="$LIB/junit-4.13.2.jar:$LIB/hamcrest-core-1.3.jar"
OUT="$ROOT/build-offline/test"
rm -rf "$OUT"; mkdir -p "$OUT"
if grep -rl 'import android' "$ROOT/app/src/main/java/com/hgame/core"; then echo "core must not use android"; exit 1; fi
"$KOTLINC" $(find "$ROOT/app/src/main/java/com/hgame/core" "$ROOT/app/src/test/java" -name '*.kt') \
  -cp "$CP" -d "$OUT" -jvm-target 17 -nowarn
CLASSES=$(cd "$OUT" && find . -name '*Test.class' | sed 's|^\./||;s|\.class$||;s|/|.|g' | sort)
java -cp "$OUT:$CP:$(dirname "$KOTLINC")/../lib/kotlin-stdlib.jar" org.junit.runner.JUnitCore $CLASSES
