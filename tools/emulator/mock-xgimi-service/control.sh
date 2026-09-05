#!/usr/bin/env bash

set -euo pipefail

adb_bin="${ADB_BIN:-adb}"
serial="${ADB_SERIAL:-}"
package_name="com.xgimi.xgimiservice"

usage() {
  cat <<'EOF'
Usage:
  control.sh reset
  control.sh scenario success
  control.sh scenario no_common
  control.sh scenario timeout [delay_ms]
  control.sh scenario env_write_failure [key]
  control.sh scenario env_readback_mismatch [key]
  control.sh env KEY VALUE

Environment:
  ADB_BIN       adb executable, default: adb
  ADB_SERIAL    optional emulator/device serial
EOF
}

if ! command -v "$adb_bin" >/dev/null 2>&1; then
  echo "error: adb was not found (set ADB_BIN if it is installed elsewhere)" >&2
  exit 1
fi

adb_args=()
if [[ -n "$serial" ]]; then
  adb_args=(-s "$serial")
fi

broadcast() {
  "$adb_bin" "${adb_args[@]}" shell am broadcast -p "$package_name" "$@"
}

command="${1:-}"
case "$command" in
  reset)
    broadcast -a com.xgimi.xgimiservice.mock.RESET
    ;;
  scenario)
    scenario="${2:-}"
    if [[ -z "$scenario" ]]; then
      usage >&2
      exit 1
    fi
    args=(-a com.xgimi.xgimiservice.mock.SET_SCENARIO --es scenario "$scenario")
    case "$scenario" in
      success|no_common)
        ;;
      timeout)
        if [[ -n "${3:-}" ]]; then
          args+=(--el delay_ms "$3")
        fi
        ;;
      env_write_failure|env_readback_mismatch)
        args+=(--es fail_key "${3:-kst_ofs}")
        ;;
      *)
        echo "error: unknown scenario: $scenario" >&2
        usage >&2
        exit 1
        ;;
    esac
    broadcast "${args[@]}"
    ;;
  env)
    key="${2:-}"
    value="${3:-}"
    if [[ -z "$key" || -z "$value" ]]; then
      usage >&2
      exit 1
    fi
    broadcast -a com.xgimi.xgimiservice.mock.SET_ENV --es key "$key" --es value "$value"
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
