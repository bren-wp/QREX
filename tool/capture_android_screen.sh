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

# Keep real on-device screenshot smoke checks: Create/Result, History filter,
# and the six More tiles must work before any screenshot may be published.
# The FULL unfiltered instrumentation suite (including advanced QR types and
# 30-cycle navigation stress) is mandatory in the five-API P03 release gate.
# Running all 53 tests *again* on this separate software-rendered capture AVD
# caused a Compose layout observer thread collision unrelated to screenshot
# validation, and could prevent producing the required visual artifacts.
# A smoke failure still stops screenshot publication; no retries of test failures.
timeout -k 10s 120s adb wait-for-device
timeout -k 20s 420s gradle -p platforms/android --no-daemon --stacktrace \
  -Pandroid.testInstrumentationRunnerArguments.class=com.brendigo.qrex.QREXCreateBlockedPreviewTest,com.brendigo.qrex.QREXHistoryFilterTest,com.brendigo.qrex.QREXMoreAboutTest \
  :app:connectedDebugAndroidTest
timeout -k 5s 90s adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
# Google Play's phone screenshot format: 1080 × 1920, portrait 9:16.
timeout -k 2s 10s adb shell wm size 1080x1920
timeout -k 2s 10s adb shell wm density 420
# The Scan screen must show actual camera UI, not a fabricated preview.
timeout -k 2s 12s adb shell pm grant com.brendigo.qrex android.permission.CAMERA
timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
timeout -k 2s 12s adb shell input keyevent KEYCODE_HOME || true
timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
  --es qrex_screenshot create --es qrex_language hr --es qrex_theme dark

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
      break
    fi
  fi
  echo "Android capture attempt $attempt/4 failed; bounded retry." >&2
  rm -f "$hierarchy" "$screenshot"
  # An unresponsive emulator can also block 'am start'; bound retries too.
  timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
  timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
    --es qrex_screenshot create --es qrex_language hr --es qrex_theme dark || true
done

if ! test -s "$screenshot"; then
  echo "No validated QREX screen; fail closed (no screenshot artifact)." >&2
  exit 1
fi

# Capture each screen from the *installed QREX debug APK*. The intent extra
# chooses a screen and injects only in-memory, non-sensitive demo records.
# Debug routing is forbidden by MainActivity in production builds.
for screen in splash scan wifi result history saved more language export about privacy; do
  screenshot="docs/screenshots/android-${screen}.png"
  hierarchy="docs/screenshots/android-${screen}-hierarchy.xml"
  rm -f "$screenshot" "$hierarchy"
  for attempt in 1 2 3 4; do
    timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
    timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
      --es qrex_screenshot "$screen" --es qrex_language hr --es qrex_theme dark
    sleep 5
    if timeout -k 3s 35s adb shell uiautomator dump /sdcard/qrex-window.xml >/dev/null 2>&1 \
      && timeout -k 3s 15s adb exec-out cat /sdcard/qrex-window.xml > "$hierarchy" \
      && test -s "$hierarchy" \
      && python3 tool/check_android_screen.py "$hierarchy" "$screen" \
      && timeout -k 3s 20s adb exec-out screencap -p > "$screenshot" \
      && python3 tool/verify_screenshots.py "$screenshot"; then
      echo "Verified real QREX $screen screen ($attempt/4)."
      break
    fi
    rm -f "$screenshot" "$hierarchy"
  done
  if ! test -s "$screenshot"; then
    echo "No validated QREX $screen screen; fail closed." >&2
    exit 1
  fi
done
python3 tool/verify_screenshots.py docs/screenshots/android-*.png
# Verify the real frame is dark, not just a dark-themed filename. The PNG
# samples come from the actual emulator capture, not marketing artwork.
python3 tool/check_dark_screenshots.py docs/screenshots/android-{create,scan,wifi,result,history,saved,more,language,export,about,privacy}.png
# Capture separate real phone-size screenshots. These do not enter the
# Play-sized public release screenshot list or alter user settings.
bash tool/capture_responsive_android.sh
echo "Twelve real QREX Android screens (dark only) captured and verified."
