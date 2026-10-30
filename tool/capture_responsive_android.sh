#!/usr/bin/env bash
# Verify all 11 real Compose screens on different Android window/density
# profiles. Run only after the main debug APK and screen captures succeed.
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
# 320dp compact profile: capture all eleven screens at compact and reference size,
# plus a large-font
# accessibility sample of every functional destination.
profiles=(
  "compact|720x1280|360|1.0|splash,scan,create,wifi,result,history,saved,more,language,export,about"
  "reference|1080x2340|440|1.0|splash,scan,create,wifi,result,history,saved,more,language,export,about"
  "largefont|1080x2340|440|1.6|scan,create,result,history,saved,more,language,export,about"
  "light|1080x2340|440|1.0|create,scan,more,about"
)

for spec in "${profiles[@]}"; do
  IFS='|' read -r profile pixels dpi scale screens <<< "$spec"
  timeout -k 2s 12s adb shell wm size "$pixels"
  timeout -k 2s 12s adb shell wm density "$dpi"
  timeout -k 2s 12s adb shell settings put system font_scale "$scale"
  # Only debug builds accept this non-persistent reference screenshot override.
  theme_args=(--es qrex_theme dark)
  if [[ "$profile" == "light" ]]; then
    theme_args=(--es qrex_theme light)
  fi
  IFS=',' read -r -a targets <<< "$screens"
  for screen in "${targets[@]}"; do
    file="docs/screenshots/responsive/${profile}-${screen}.png"
    hierarchy="docs/screenshots/responsive/${profile}-${screen}.xml"
    rm -f "$file" "$hierarchy"
    for attempt in 1 2 3; do
      timeout -k 2s 12s adb shell am force-stop com.brendigo.qrex || true
      timeout -k 3s 25s adb shell am start -W -n com.brendigo.qrex/.MainActivity \
        --es qrex_screenshot "$screen" --es qrex_language hr "${theme_args[@]}"
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
            "largefont": (1080, 2340),
            "light": (1080, 2340)}
actual = verify(Path(sys.argv[2]))
if actual != expected[sys.argv[1]]:
    raise ValueError(f"Wrong responsive device size: {actual}")
PY
  done
done
echo "Verified eleven real Android screens at compact/reference sizes, nine large-font and four light variants."
