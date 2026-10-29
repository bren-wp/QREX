#!/usr/bin/env bash
# One bounded Bash process for android-emulator-runner, which runs inline
# newline-separated commands in *separate* /bin/sh sessions.
set -Eeuo pipefail

api="${1:?Android API is required}"
case "$api" in 26|31|34|35|36) ;; *) echo "Unsupported P03 API" >&2; exit 64 ;; esac

mkdir -p p03-results
report="p03-results/crash-summary-api-${api}.md"
crash_raw="${RUNNER_TEMP:-/tmp}/qrex-p03-crash-${api}.log"
events_raw="${RUNNER_TEMP:-/tmp}/qrex-p03-events-${api}.log"

cleanup() {
  rm -f "$crash_raw" "$events_raw"
}
trap cleanup EXIT
timeout -k 10s 120s adb wait-for-device
timeout -k 3s 15s adb logcat -c || true

# Preserve nonzero instrumentation status, but always attempt redacted
# diagnostics. Empty or unavailable logs must never be counted as success.
test_status=0
# Release-gating P03 always exercises the FULL unfiltered instrumentation
# suite: navigation stress + Create/Result, History, Settings, storage and
# privacy regressions. These tests replace the redundant full-suite replay
# on the separate software-rendered screenshot capture AVD.
timeout -k 15s 1200s gradle -p platforms/android --no-daemon --stacktrace \
  :app:connectedDebugAndroidTest \
  || test_status=$?

timeout -k 3s 20s adb logcat -b crash -d -v threadtime > "$crash_raw" || {
  echo "P03 crash buffer unavailable" >&2
  exit 1
}
timeout -k 3s 20s adb logcat -b events -d -v threadtime > "$events_raw" || {
  echo "P03 ANR event buffer unavailable" >&2
  exit 1
}
triage_status=0
python3 tool/p03_crash_triage.py "$crash_raw" "$report" "$api" "$events_raw" \
  || triage_status=$?
test -s "$report" || {
  echo "No sanitized report produced" >&2
  exit 1
}
cat "$report"
if (( test_status != 0 || triage_status != 0 )); then
  echo "P03 rejected: instrumentation=${test_status}, sanitized-triage=${triage_status}" >&2
  exit 1
fi
echo "P03 emulated API ${api} tests and sanitized crash/ANR triage passed."
