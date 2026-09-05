#!/usr/bin/env bash

# Read-only probe for an XGIMI XH25L projector.
# This script does not call setprop, remount, push, reboot, or any update command.

set -euo pipefail

adb_bin="${ADB_BIN:-adb}"
serial="${1:-}"

if ! command -v "$adb_bin" >/dev/null 2>&1; then
  echo "error: adb was not found (set ADB_BIN if it is installed elsewhere)" >&2
  exit 1
fi

adb_args=()
if [[ -n "$serial" ]]; then
  adb_args=(-s "$serial")
fi

run_adb() {
  "$adb_bin" "${adb_args[@]}" "$@"
}

if [[ "$(run_adb get-state 2>/dev/null || true)" != "device" ]]; then
  echo "error: no authorized projector is available to adb" >&2
  echo "Connect it first, then confirm it appears as 'device' below:" >&2
  "$adb_bin" devices -l >&2
  exit 1
fi

print_prop() {
  local key="$1"
  local value
  value="$(run_adb shell getprop "$key" | tr -d '\r')"
  printf '%-42s %s\n' "$key" "${value:-<empty>}"
}

echo "== Device identity =="
for key in \
  ro.product.model \
  ro.product.device \
  ro.product.name \
  ro.build.display.id \
  ro.build.version.incremental \
  ro.build.version.release \
  ro.build.fingerprint; do
  print_prop "$key"
done

echo
echo "== Keystone and trapezoid properties =="
for key in \
  persist.trapezoidCorrect.type \
  persist.mstar.pointABCDoffset \
  persist.mstar.pointAoffset_x \
  persist.mstar.pointAoffset_y \
  persist.mstar.pointBoffset_x \
  persist.mstar.pointBoffset_y \
  persist.mstar.pointCoffset_x \
  persist.mstar.pointCoffset_y \
  persist.mstar.pointDoffset_x \
  persist.mstar.pointDoffset_y \
  mstar.test.correctType \
  mstar.test.pointAoffset_x \
  mstar.test.pointAoffset_y \
  mstar.test.pointBoffset_x \
  mstar.test.pointBoffset_y \
  mstar.test.pointCoffset_x \
  mstar.test.pointCoffset_y \
  mstar.test.pointDoffset_x \
  mstar.test.pointDoffset_y; do
  print_prop "$key"
done

echo
echo "== Relevant Binder services (read-only listing) =="
run_adb shell service list | tr -d '\r' | grep -E -i 'xgimi|gimi|trapezoid|display' || true

echo
echo "== Vendor binaries =="
run_adb shell ls -l /vendor/bin/trapezoidServer /vendor/bin/trapezoidTest 2>&1 | tr -d '\r' || true

echo
echo "Probe finished. No projector settings were changed."
