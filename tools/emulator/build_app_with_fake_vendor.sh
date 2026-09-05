#!/usr/bin/env bash

set -euo pipefail

tool_dir="$(cd "$(dirname "$0")" && pwd)"
project_dir="$(cd "$tool_dir/../.." && pwd)"
sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
build_tools_version="${ANDROID_BUILD_TOOLS_VERSION:-36.0.0}"
android_platform="${ANDROID_PLATFORM:-android-36.1}"
build_tools="$sdk_dir/build-tools/$build_tools_version"
android_jar="$sdk_dir/platforms/$android_platform/android.jar"
build_dir="$project_dir/build/emulator-app"
classes_dir="$build_dir/classes"
dex_dir="$build_dir/dex"
compiled_res_dir="$build_dir/compiled-res"
output_apk="$build_dir/xh25l-keystone-presets-emulator.apk"
keystore="$project_dir/build/debug.keystore"
sources_file="$build_dir/sources.txt"

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

for command_path in "$javac_bin" "$jar_bin" "$keytool_bin"; do
  if [[ -z "$command_path" || ! -x "$command_path" ]]; then
    echo "error: JDK tools were not found; set JAVA_HOME to a JDK" >&2
    exit 1
  fi
done

rm -rf "$build_dir"
mkdir -p "$classes_dir" "$dex_dir" "$compiled_res_dir"
{
  find "$project_dir/app/src" -name '*.java' -print
  find "$tool_dir/fake-xgimi-vendor/src" -name '*.java' -print
} | sort > "$sources_file"
for resource in "$project_dir"/app/res/drawable/*.xml; do
  "$build_tools/aapt2" compile -o "$compiled_res_dir" "$resource"
done
resource_args=()
for compiled_resource in "$compiled_res_dir"/*.flat; do
  resource_args+=(-R "$compiled_resource")
done

"$build_tools/aapt2" link \
  -o "$build_dir/base-unsigned.apk" \
  --manifest "$project_dir/app/AndroidManifest.xml" \
  -I "$android_jar" \
  "${resource_args[@]}" \
  --min-sdk-version 26 \
  --target-sdk-version 26

"$javac_bin" \
  --release 8 \
  -classpath "$android_jar" \
  -d "$classes_dir" \
  @"$sources_file"

"$jar_bin" cf "$build_dir/classes.jar" -C "$classes_dir" .
"$build_tools/d8" --release --min-api 26 \
  --output "$dex_dir" "$build_dir/classes.jar"
cp "$build_dir/base-unsigned.apk" "$build_dir/with-dex-unsigned.apk"
(
  cd "$dex_dir"
  zip -q "$build_dir/with-dex-unsigned.apk" classes.dex
)
"$build_tools/zipalign" -f 4 \
  "$build_dir/with-dex-unsigned.apk" \
  "$build_dir/aligned-unsigned.apk"

if [[ ! -f "$keystore" ]]; then
  "$keytool_bin" -genkeypair \
    -keystore "$keystore" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=XH25L Emulator App,O=Local,C=GB" \
    -noprompt
fi

"$build_tools/apksigner" sign \
  --ks "$keystore" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$output_apk" \
  "$build_dir/aligned-unsigned.apk"

"$build_tools/apksigner" verify --verbose "$output_apk"
echo "$output_apk"
