#!/usr/bin/env bash

set -euo pipefail

fixture_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$fixture_dir/../../.." && pwd)"
sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
build_tools_version="${ANDROID_BUILD_TOOLS_VERSION:-36.0.0}"
android_platform="${ANDROID_PLATFORM:-android-36.1}"
build_tools="$sdk_dir/build-tools/$build_tools_version"
android_jar="$sdk_dir/platforms/$android_platform/android.jar"
build_dir="$project_dir/build/mock-xgimi-service"
keystore="$project_dir/build/debug.keystore"

if [[ -z "$sdk_dir" ]]; then
  echo "error: set ANDROID_SDK_ROOT (or ANDROID_HOME) to your Android SDK" >&2
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  javac_bin="$JAVA_HOME/bin/javac"
  jar_bin="$JAVA_HOME/bin/jar"
  keytool_bin="$JAVA_HOME/bin/keytool"
else
  javac_bin="$(command -v javac || true)"
  jar_bin="$(command -v jar || true)"
  keytool_bin="$(command -v keytool || true)"
fi

for required in "$build_tools/aapt2" "$build_tools/d8" \
  "$build_tools/zipalign" "$build_tools/apksigner" "$android_jar"; do
  if [[ ! -f "$required" ]]; then
    echo "error: required file not found: $required" >&2
    exit 1
  fi
done

rm -rf "$build_dir"
mkdir -p "$build_dir/classes" "$build_dir/dex"

"$build_tools/aapt2" link \
  -o "$build_dir/base.apk" \
  --manifest "$fixture_dir/AndroidManifest.xml" \
  -I "$android_jar" \
  --min-sdk-version 26 \
  --target-sdk-version 36

"$javac_bin" \
  --release 8 \
  -classpath "$android_jar" \
  -d "$build_dir/classes" \
  "$fixture_dir/src/com/xgimi/xgimiservice/services/XgimiServices.java"

"$jar_bin" cf "$build_dir/classes.jar" -C "$build_dir/classes" .
"$build_tools/d8" --release --min-api 26 \
  --output "$build_dir/dex" "$build_dir/classes.jar"
cp "$build_dir/base.apk" "$build_dir/with-dex.apk"
(
  cd "$build_dir/dex"
  zip -q "$build_dir/with-dex.apk" classes.dex
)
"$build_tools/zipalign" -f 4 \
  "$build_dir/with-dex.apk" "$build_dir/aligned.apk"
if [[ ! -f "$keystore" ]]; then
  "$keytool_bin" -genkeypair \
    -keystore "$keystore" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=XH25L Emulator Mock,O=Local,C=GB" \
    -noprompt
fi
"$build_tools/apksigner" sign \
  --ks "$keystore" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$build_dir/mock-xgimi-service.apk" \
  "$build_dir/aligned.apk"

echo "$build_dir/mock-xgimi-service.apk"
