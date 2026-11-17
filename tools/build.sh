#!/usr/bin/env bash
# Build, sign and (optionally) install Hyper120 without Gradle.
# Usage: tools/build.sh [--install]
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_SDK:-$LOCALAPPDATA/Android/Sdk}"
BT="$SDK/build-tools/36.0.0"
JAR="$SDK/platforms/android-36.1/android.jar"
EXE=""; [[ -f "$BT/aapt2.exe" ]] && EXE=".exe"
D8="$BT/d8"; [[ -f "$BT/d8.bat" ]] && D8="$BT/d8.bat"
SIGN="$BT/apksigner"; [[ -f "$BT/apksigner.bat" ]] && SIGN="$BT/apksigner.bat"

python tools/sync_games.py   # data/games.json -> res/raw, web, README
rm -rf build/res build/gen build/classes build/dex
mkdir -p build/res build/gen build/classes build/dex
"$BT/aapt2$EXE" compile --dir res -o build/res/res.zip
"$BT/aapt2$EXE" link -o build/unsigned.apk -I "$JAR" --manifest AndroidManifest.xml \
  --java build/gen build/res/res.zip --auto-add-overlay
javac -nowarn -source 11 -target 11 -classpath "$JAR" -d build/classes \
  $(find src build/gen -name '*.java') 2>&1 | grep -v "^warning\|^Note:\|^1 warning" || true
[[ -f build/classes/com/custom/game120hz/MainActivity.class ]] || { echo "javac failed"; exit 1; }
"$D8" --min-api 26 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')
cp build/unsigned.apk build/unaligned.apk
(cd build/dex && jar uf ../unaligned.apk classes.dex)
"$BT/zipalign$EXE" -f 4 build/unaligned.apk build/aligned.apk
# Release signing: if the release key exists, sign with key rotation (legacy key -> release key)
# so installs signed with the old key keep updating. Otherwise fall back to the local debug key.
KEYDIR="${HYPER120_KEYS:-$HOME/.hyper120-signing}"
command -v cygpath >/dev/null && KEYDIR="$(cygpath -m "$KEYDIR")"   # Java on Windows needs C:/ paths
if [[ -f "$KEYDIR/release.jks" && -f "$KEYDIR/lineage.bin" ]]; then
  "$SIGN" sign --ks "$KEYDIR/legacy-debug.jks" --ks-pass pass:android --key-pass pass:android \
    --next-signer --ks "$KEYDIR/release.jks" --ks-pass "file:$KEYDIR/release.pass" \
    --lineage "$KEYDIR/lineage.bin" --out build/Hyper120.apk build/aligned.apk
  echo "signed with release key (rotation from legacy key)"
else
  "$SIGN" sign --ks build/debug.jks --ks-pass pass:android --key-pass pass:android \
    --out build/Hyper120.apk build/aligned.apk
  echo "signed with local debug key"
fi
echo "built build/Hyper120.apk"
if [[ "${1:-}" == "--install" ]]; then
  "$SDK/platform-tools/adb$EXE" install -r build/Hyper120.apk
fi
