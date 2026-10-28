#!/usr/bin/env bash
# Pre-install/validate the runner SDK emulator before Android Emulator Runner.
# A truncated SDK download ("ZipFile unknown archive") is an infrastructure
# error, never evidence of a QREX application crash. Retry ONLY known transient
# package transport/archive errors; do not suppress emulator/instrumentation QA.
set -Eeuo pipefail
manager="${QREX_SDKMANAGER:-sdkmanager}"
emulator="${QREX_EMULATOR_BIN:-emulator}"
retry_sleep="${QREX_RETRY_SLEEP_SECONDS:-15}"
log_file="$(mktemp "${RUNNER_TEMP:-/tmp}/qrex-emulator-sdk.XXXXXXXX")"
trap 'rm -f "$log_file"' EXIT
for attempt in 1 2 3; do
  if timeout -k 5s 180s "$manager" --install emulator --channel=0 >"$log_file" 2>&1; then
    if timeout -k 2s 15s "$emulator" -version >/dev/null 2>&1; then
      echo "Android Emulator SDK is installed and executable."
      exit 0
    fi
    echo "Android Emulator SDK install returned success but emulator is unusable." >&2
    exit 1
  fi
  # Never print the raw sdkmanager output: a failed dependency URL may carry
  # access parameters. Print only the known, safe failure classification.
  if ! grep -Eiq 'ZipFile unknown archive|Error on ZipFile|ZipException|zip END header not found|Unexpected end of ZLIB input stream|Read timed out|Connection reset|HTTP (429|500|502|503|504)' "$log_file"; then
    echo "Non-transient Android SDK install failure (not retried)." >&2
    exit 1
  fi
  if (( attempt == 3 )); then
    echo "Android Emulator SDK transport/archive failure persisted after 3 attempts." >&2
    exit 1
  fi
  echo "Transient Android Emulator SDK archive/transport error; retry ${attempt}/3." >&2
  sleep "$((attempt * retry_sleep))"
done
exit 1
