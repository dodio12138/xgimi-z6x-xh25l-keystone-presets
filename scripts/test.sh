#!/usr/bin/env bash

set -euo pipefail

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
test_build_dir="$project_dir/build/unit-tests"

if [[ -n "${JAVA_HOME:-}" ]]; then
  javac_bin="$JAVA_HOME/bin/javac"
  java_bin="$JAVA_HOME/bin/java"
else
  javac_bin="$(command -v javac || true)"
  java_bin="$(command -v java || true)"
fi

for command_path in "$javac_bin" "$java_bin"; do
  if [[ -z "$command_path" || ! -x "$command_path" ]]; then
    echo "error: JDK tools were not found; set JAVA_HOME to a JDK" >&2
    exit 1
  fi
done

rm -rf "$test_build_dir"
mkdir -p "$test_build_dir"

"$javac_bin" --release 8 -d "$test_build_dir" \
  "$project_dir/app/src/com/xgimirom/presets/DeviceCompatibility.java" \
  "$project_dir/app/src/com/xgimirom/presets/KeystoneDataValidator.java" \
  "$project_dir/app/src/com/xgimirom/presets/KeystoneOffsetParser.java" \
  "$project_dir/app/src/com/xgimirom/presets/MainFocusNavigation.java" \
  "$project_dir/app/src/com/xgimirom/presets/PresetIntegrity.java" \
  "$project_dir/app/src/com/xgimirom/presets/ProjectionPreset.java" \
  "$project_dir/app/src/com/xgimirom/presets/ProjectionTransaction.java" \
  "$project_dir/tests/com/xgimirom/presets/UnitTests.java"

"$java_bin" -cp "$test_build_dir" com.xgimirom.presets.UnitTests
