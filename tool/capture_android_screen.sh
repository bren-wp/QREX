#!/usr/bin/env bash
# This script runs as ONE process inside android-emulator-runner.
# Every ADB operation has a hard deadline: frozen emulators must fail CI, not
# hold the screenshot job for 50 minutes or publish a system ANR as a QREX image.
set -Eeuo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"
mkdir -p docs/screenshots
screenshot="docs/screenshots/android-create.png"
hierarchy="docs/screenshots/android-hierarchy.xml"
rm -f "$screenshot" "$hierarchy"

# Print only sanitized health/identity diagnostics. Do not upload arbitrary
# device logcat; it may contain user data or credentials from scanned QR codes.
capture_diagnostics() {
  echo "Android emulator screenshot failed: bounded diagnostics" >&2
  timeout -k 2s 8s adb get-state >&2 || true
  timeout -k 2s 8s adb shell pidof com.brendigo.qrex >&2 || true
  timeout -k 2s 8s adb shell getprop sys.boot_completed >&2 || true
}
on_exit() {
  local status=$?
  if (( status != 0 )); then
    rm -f "$screenshot" "$hierarchy"
    capture_diagnostics
  fi
}
trap on_exit EXIT

# Instrumentation deliberately precedes screenshot capture. A test may
# interrupt the app, so always force a fresh start afterward.
timeout -k 10s 120s adb wait-for-device
timeout -k 20s 900s gradle -p platforms/android --no-daemon --stacktrace :app:connectedDebugAndroidTest
timeout -k 5s 90s adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
timeout -k 2s 12s adb shell input keyevent KEYCODE_HOME || true
timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
  --es qrex_screenshot create

# Never trust just a PNG signature. The *actual* foreground accessibility tree
# must prove QREX Create is visible and exclude system crash/ANR overlays.
for attempt in 1 2 3 4; do
  sleep 5
  rm -f "$hierarchy" "$screenshot"
  if timeout -k 3s 35s adb shell uiautomator dump /sdcard/qrex-window.xml >/dev/null 2>&1 \
    && timeout -k 3s 15s adb exec-out cat /sdcard/qrex-window.xml > "$hierarchy" \
    && test -s "$hierarchy" \
    && python3 tool/check_android_screen.py "$hierarchy"; then
    if timeout -k 3s 20s adb exec-out screencap -p > "$screenshot" \
      && python3 tool/verify_screenshots.py "$screenshot"; then
      echo "Verified real QREX Create screen on Android ($attempt/4)."
      exit 0
    fi
  fi
  echo "Android capture attempt $attempt/4 failed; bounded retry." >&2
  rm -f "$hierarchy" "$screenshot"
  # An unresponsive emulator can also block 'am start'; bound retries too.
  timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
  timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
    --es qrex_screenshot create || true
done

echo "No validated QREX screen; fail closed (no screenshot artifact)." >&2
exit 1
