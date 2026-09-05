#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
sdk_dir="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
build_tools_version="${ANDROID_BUILD_TOOLS_VERSION:-36.0.0}"
android_platform="${ANDROID_PLATFORM:-android-36.1}"
vendor_dir="${XGIMI_VENDOR_DIR:-$project_dir/local-vendor}"
build_tools="$sdk_dir/build-tools/$build_tools_version"
android_jar="$sdk_dir/platforms/$android_platform/android.jar"
build_dir="$project_dir/build"
classes_dir="$build_dir/classes"
dex_dir="$build_dir/dex"
output_apk="$project_dir/outputs/xh25l-keystone-presets-debug.apk"
keystore="$build_dir/debug.keystore"

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

for required in \
  "$build_tools/aapt2" \
  "$build_tools/d8" \
  "$build_tools/zipalign" \
  "$build_tools/apksigner" \
  "$android_jar" \
  "$vendor_dir/libdisplaymanager_jni.xgimi.so" \
  "$vendor_dir/libgmpfdisplaymanager_hidl.xgimi.so" \
  "$vendor_dir/setting_classes.dex"; do
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

rm -rf "$build_dir/classes" "$build_dir/dex" "$build_dir/apk-extra"
mkdir -p "$classes_dir" "$dex_dir" "$project_dir/outputs"

# Compile app resources explicitly before linking. Raw aapt2 does not scan
# app/res when invoked with only --manifest, so launcher vectors would
# otherwise be absent from the APK resource table.
resources_zip="$build_dir/resources.zip"
rm -f "$resources_zip"
"$build_tools/aapt2" compile \
  --dir "$project_dir/app/res" \
  -o "$resources_zip"

"$build_tools/aapt2" link \
  -o "$build_dir/base-unsigned.apk" \
  --manifest "$project_dir/app/AndroidManifest.xml" \
  -I "$android_jar" \
  -R "$resources_zip" \
  --min-sdk-version 26 \
  --target-sdk-version 26

"$javac_bin" \
  --release 8 \
  -classpath "$android_jar" \
  -d "$classes_dir" \
  "$project_dir/app/src/com/xgimirom/presets/MainActivity.java" \
  "$project_dir/app/src/com/xgimirom/presets/SettingsActivity.java" \
  "$project_dir/app/src/com/xgimirom/presets/DeviceCompatibility.java" \
  "$project_dir/app/src/com/xgimirom/presets/GmpfKeystoneBridge.java" \
  "$project_dir/app/src/com/xgimirom/presets/KeystoneDataValidator.java" \
  "$project_dir/app/src/com/xgimirom/presets/KeystoneOffsetParser.java" \
  "$project_dir/app/src/com/xgimirom/presets/MainFocusNavigation.java" \
  "$project_dir/app/src/com/xgimirom/presets/PresetIntegrity.java" \
  "$project_dir/app/src/com/xgimirom/presets/ProjectionPreset.java" \
  "$project_dir/app/src/com/xgimirom/presets/ProjectionTransaction.java" \
  "$project_dir/app/src/com/xgimirom/presets/XgimiEnvironmentBridge.java"

"$jar_bin" cf "$build_dir/classes.jar" -C "$classes_dir" .
"$build_tools/d8" \
  --release \
  --min-api 26 \
  --output "$dex_dir" \
  "$build_dir/classes.jar"

cp "$build_dir/base-unsigned.apk" "$build_dir/with-dex-unsigned.apk"
mkdir -p "$build_dir/apk-extra/lib/arm64-v8a"
cp "$vendor_dir/libdisplaymanager_jni.xgimi.so" \
  "$build_dir/apk-extra/lib/arm64-v8a/"
cp "$vendor_dir/libgmpfdisplaymanager_hidl.xgimi.so" \
  "$build_dir/apk-extra/lib/arm64-v8a/"
cp "$vendor_dir/setting_classes.dex" \
  "$build_dir/apk-extra/classes2.dex"
(
  cd "$dex_dir"
  zip -q "$build_dir/with-dex-unsigned.apk" classes.dex
)
(
  cd "$build_dir/apk-extra"
  zip -qr "$build_dir/with-dex-unsigned.apk" classes2.dex lib
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
    -dname "CN=XH25L Presets,O=Local,C=GB" \
    -noprompt
fi

"$build_tools/apksigner" sign \
  --ks "$keystore" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$output_apk" \
  "$build_dir/aligned-unsigned.apk"

"$build_tools/apksigner" verify --verbose "$output_apk"
cp "$output_apk" "$output_apk"1
echo "$output_apk"
