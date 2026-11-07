# QREX targeted screen-by-screen layout audit — 01 / 02 / launch icon

Baseline: published [QREX v1.0.37](https://github.com/bren-wp/QREX/releases/tag/v1.0.37),
merged commit `1abcd84d4b658f53be152bbc8bcd2f2e376c8ac7`.
User's older QREX mockups combine illustrated phone frames and
alternating launcher identities; retain the explicitly selected Q/QR
symbol already present in the app and reference brand SVG.

## Confirmed problems before this change

- Splash logo radius was an absolute 24dp and glow radius 27dp across
  both 156dp compact and 214dp standard badges, giving inconsistent
  corner proportions. A 256dp tablet badge would have been worse.
- Splash non-scroll layout threshold was 640dp / 1.5× font, leaving
  640–699dp windows and 1.4× fonts vulnerable to overlapping translated
  footer text and centered brand. Standard reference phone unaffected.
- Scanner used `Modifier.weight(1f)` to size its live camera preview
  even when the available window was too short for action controls.
  On landscape, floating/resized windows or large system fonts the
  viewport could be compressed instead of being reachable by scrolling.
- Camera framing guide used a width-proportional ratio without a
  large-tablet maximum side length.
- Adaptive launcher symbol and monochrome themed symbol had matching
  canonical Q/QR paths, but neither carried the inset luminance edge
  around the actual mark. Both now share that contour within the
  0.91× safe zone; the Splash drawable itself is unchanged.

## Implemented size contracts (dp, not pixels)

| State | Splash badge | Splash layout | Home camera |
| --- | --- | --- | --- |
| 320×568, font 1× | 156 | Scroll, full footer | Scroll, 304dp |
| 393×852, font 1× | 214 | Centered reference | Flex / fill remaining height |
| 800×1280, font 1× | 256 | Centered tablet | Flex, finder <=440dp |
| 393×852, font 1.6× | 214 | Scroll, full footer | Scroll, responsive panel |
| 800×600 landscape, font 1× | 214 | Scroll, full footer | Scroll, 378dp |

## Preserved behavior

CameraX/ZXing offline QR frame analysis and gallery, protected Wi-Fi
history, permissions, navigation, five-tab ordering, saved QR state,
font translations and release signing are unchanged. Frame previews
are live; icon paths and light overlays remain native Android vector
and Jetpack Compose drawing, not fake screenshots.

## Final proof before claiming release quality

Require the exact-HEAD 4 workflow gates, actual emulator screenshot
review (Splash and Scanner), all five P03 Android API stress jobs,
APK/AAB signature + checksum validation and 12 authentic screenshot
assets. Mathematical pixel-level 1:1 is **not** established by source
geometry contracts alone. For a defensible 0% error claim, obtain flat
original reference PNGs for the intended DPI, screen size, theme,
localization and camera/permission state and compare them directly
with genuine captures, including adaptive launcher variants.
