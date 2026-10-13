#!/usr/bin/env bash
# Run as one process from android-emulator-runner's script input.
# That action executes newline-separated commands independently: shell loops
# must live in a real script to preserve their control flow.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

adb wait-for-device
# Instrumentation is intentionally run before the screenshot. A test can
# interrupt the foreground activity or open a system dialog.
gradle -p platforms/android --no-daemon --stacktrace :app:connectedDebugAndroidTest
adb install -r platforms/android/app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.brendigo.qrex || true
adb shell input keyevent KEYCODE_HOME
adb shell am start -W -n com.brendigo.qrex/.MainActivity --es qrex_screenshot create
mkdir -p docs/screenshots

# A valid PNG can still be a system error / ANR dialog. Never publish a
# screenshot without validating the live application's accessibility tree.
for attempt in 1 2 3 4; do
  sleep 7
  rm -f docs/screenshots/android-hierarchy.xml
  if timeout 40s adb shell uiautomator dump /sdcard/qrex-window.xml >/dev/null 2>&1 \
    && adb exec-out cat /sdcard/qrex-window.xml > docs/screenshots/android-hierarchy.xml \
    && python3 tool/check_android_screen.py docs/screenshots/android-hierarchy.xml; then
    adb exec-out screencap -p > docs/screenshots/android-create.png
    python3 tool/verify_screenshots.py docs/screenshots/android-create.png
    echo "Verified real QREX Create screen on Android ($attempt/4)."
    exit 0
  fi
  echo "QREX foreground check failed ($attempt/4); relaunching" >&2
  adb shell am start -n com.brendigo.qrex/.MainActivity --es qrex_screenshot create || true
done

echo "Refusing to publish a System UI, ANR or non-QREX screenshot." >&2
exit 1
