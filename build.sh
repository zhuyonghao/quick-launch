#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk" ]]; then
  echo "Set ANDROID_SDK_ROOT to your Android SDK directory." >&2
  exit 1
fi

tools="${ANDROID_BUILD_TOOLS_DIR:-$sdk/build-tools/35.0.0}"
platform="$sdk/platforms/android-35/android.jar"
out="$PWD/build"
keystore="${QUICKLAUNCH_KEYSTORE:-$out/local-signing.jks}"
mkdir -p "$out"
tmp="$(mktemp -d "$out/staging.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
mkdir "$tmp/classes" "$tmp/dex" "$tmp/generated"

"$tools/aapt2" compile --dir app/src/main/res -o "$tmp/resources.zip"
"$tools/aapt2" link -I "$platform" --manifest app/src/main/AndroidManifest.xml \
  --java "$tmp/generated" -o "$tmp/resources.apk" "$tmp/resources.zip"

sources=()
while IFS= read -r -d '' source; do
  sources+=("$source")
done < <(find app/src/main/java "$tmp/generated" -name '*.java' -print0)
javac -encoding UTF-8 --release 8 -classpath "$platform" \
  -d "$tmp/classes" "${sources[@]}"
jar cf "$tmp/classes.jar" -C "$tmp/classes" .
"$tools/d8" --min-api 26 --lib "$platform" --output "$tmp/dex" "$tmp/classes.jar"

cp "$tmp/resources.apk" "$tmp/unsigned.apk"
(cd "$tmp/dex" && zip -q "$tmp/unsigned.apk" classes.dex)
"$tools/zipalign" -f 4 "$tmp/unsigned.apk" "$tmp/aligned.apk"

if [[ ! -f "$keystore" ]]; then
  keytool -genkeypair -keystore "$keystore" -storepass android -keypass android \
    -alias quicklaunch -dname 'CN=QuickLaunch Local' -keyalg RSA -keysize 2048 -validity 10000
fi
"$tools/apksigner" sign --ks "$keystore" --ks-key-alias quicklaunch \
  --ks-pass pass:android --key-pass pass:android --out "$out/QuickLaunch.apk" "$tmp/aligned.apk"
"$tools/apksigner" verify --verbose "$out/QuickLaunch.apk"
