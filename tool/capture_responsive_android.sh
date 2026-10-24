#!/usr/bin/env bash
# Verify that production Compose screens respond to real window/density/font
# changes. Run only after the normal debug APK and 12 screenshots succeed.
# Captures come from a real Android emulator; NEVER resize 1080x1920 marketing
# images or inject static UI artwork.
set -Eeuo pipefail
mkdir -p docs/screenshots/responsive
restore_device() {
  timeout -k 2s 10s adb shell wm size reset >/dev/null 2>&1 || true
  timeout -k 2s 10s adb shell wm density reset >/dev/null 2>&1 || true
  timeout -k 2s 10s adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
}
trap restore_device EXIT

# profile | actual pixel width x height | density DPI | Android font scale |
# screens: 320dp compact, ~393dp reference handset, enlarged text handset
profiles=(
  "compact|720x1280|360|1.0|create,scan,more"
  "reference|1080x2340|440|1.0|create,more"
  "largefont|1080x2340|440|1.6|create,more"
)

for spec in "${profiles[@]}"; do
  IFS='|' read -r profile pixels dpi scale screens <<< "$spec"
  timeout -k 2s 12s adb shell wm size "$pixels"
  timeout -k 2s 12s adb shell wm density "$dpi"
  timeout -k 2s 12s adb shell settings put system font_scale "$scale"
  IFS=',' read -r -a targets <<< "$screens"
  for screen in "${targets[@]}"; do
    file="docs/screenshots/responsive/${profile}-${screen}.png"
    hierarchy="docs/screenshots/responsive/${profile}-${screen}.xml"
    rm -f "$file" "$hierarchy"
    for attempt in 1 2 3; do
      timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
      timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
        --es qrex_screenshot "$screen" --es qrex_language hr
      sleep 5
      if timeout -k 3s 35s adb shell uiautomator dump /sdcard/qrex-responsive.xml >/dev/null 2>&1 \
        && timeout -k 3s 15s adb exec-out cat /sdcard/qrex-responsive.xml > "$hierarchy" \
        && test -s "$hierarchy" \
        && python3 tool/check_android_screen.py "$hierarchy" "responsive-${screen}" \
        && timeout -k 3s 20s adb exec-out screencap -p > "$file" \
        && python3 tool/verify_screenshots.py "$file"; then
        break
      fi
      rm -f "$file" "$hierarchy"
    done
    if ! test -s "$file"; then
      echo "Responsive QREX ${profile}/${screen} validation failed" >&2
      exit 1
    fi
    python3 - "$profile" "$file" <<'PY'
import sys
from pathlib import Path
from tool.verify_screenshots import verify
expected = {"compact": (720, 1280),
            "reference": (1080, 2340),
            "largefont": (1080, 2340)}
actual = verify(Path(sys.argv[2]))
if actual != expected[sys.argv[1]]:
    raise ValueError(f"Wrong responsive device size: {actual}")
PY
  done
done
echo "Verified 7 genuine dark responsive screenshots: 320dp, 393dp and 1.6x font."
