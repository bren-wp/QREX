# QREX — 2026-10-26 Splash / canonical brand follow-up

## Baseline

Primary reference: user-provided dark QREX marketing composition (white Q / QR modules,
cyan scanner corners, blue round-square tile). The alternate scanner-square identity
on a separate board is **not** the canonical launcher artwork.

Public baseline: v1.0.16 (versionCode 116) was already published before this
branch began. This QA change **does not modify** v1.0.16, any release tag,
Play signer, package ID or saved records.

## Confirmed code discrepancies and corrections

1. The static centered Splash column and independently bottom-anchored footer
   could intersect on short screens with large accessibility text. Compose now
   uses a single vertically scrollable layout below 640dp height or from a
   1.5× system font scale. Normal-height/normal-font reference geometry and
   background waves are unchanged.
2. The root Splash screen had an English-only, redundant spoken content
   description. It has been removed, while the QREX wordmark, localized tagline
   and localized bottom message remain accessible as distinct elements.
   Callers now have to supply a localized subtitle.
3. `assets/brand/qrex-icon.svg` used a separately inset tile and rounded
   finder modules not present in the production Android `qrex_icon.xml`.
   The SVG now uses the same **13 path shapes and geometry**, color stops,
   scanner corners, Q and QR modules as the Android drawable. Launcher
   foreground, native splash configuration and monochrome artwork are left
   unchanged pending real OEM/Android screenshot comparisons.

## Coverage

- Added JVM tests for standard 393×852dp, compact 320×568dp and 1.6×/2× font
  scale selection, including threshold boundaries.
- Added source-contract tests checking canonical SVG ↔ Android vector paths,
  color tokens, adaptive/round/monochrome launcher wiring, Android 12+
  native splash wiring and localized Splash usage.
- Real device/emulator pixels, contrast overlays, OEM launcher masks and
  TalkBack reading order remain **unverified** until Android CI artifacts
  and device testing are inspected. These tests do not prove visual 1:1
  from perspective-transformed marketing art.

## Scanner / screen 02 continuation in the same PR

The original live CameraX PreviewView, offline ZXing analysis, gallery picker
and torch callback are retained; no mock photo or network scanning was added.

- The bottom hint, camera actions and available zoom chips now share one
  measured Compose Column within the preview instead of separately fixed
  bottom offsets. This is a layout correction, not a camera feed change.
- ScannerCornerOverlay reserves a bottom UI region, limited to 45% of its
  available height, so short screens can still show a guide and camera actions.
- CameraX min/max zoom ratios filter the preset chips. Devices with maximum
  zoom between 1× and 2× expose that real maximum rather than a disabled 2×.
- Gallery/Torch labels allow a third line with increased system font scale;
  the controls remain at least 64dp compact, 80dp normal and 88dp at ≥1.5×
  font scale. The Settings recovery action now has a 48dp minimum target.
- Existing permitted/denied camera behavior, gallery cancellation guard,
  main-thread torch confirmation and camera frame closure are unchanged.
- New device-independent Kotlin tests cover zoom bounds (normal/ultrawide/
  capped/invalid) and an instrumentation test measures actual gallery, torch
  and camera recovery action hit regions on the running Compose screen.
- Existing Python source-guard tests were updated to assert the new layouts
  *instead of* asserting the removed fixed offsets; do not skip tests.

Screen 03 Result: the pinned 52dp Back icon now announces the localized
`back` string instead of "Close"; it continues to use the same navigation
callback and retains the original screenshot fixture behavior.

### Validation notes

- Initial scanner branch CI identified two stale source-contract checks
  (`test_1_0_1_quality` / `test_ux_dead_code`); those were updated after
  reading the actual failed logs, without disabling the underlying checks.
- This document does **not** claim final emulator, screenshot or full API-matrix
  success until exact-HEAD CI checks are complete.
- Camera-specific zoom, flash, focus, denied permissions and OEM launcher
  masks still need physical-device verification.

## Follow-on screen order

Create → History / Saved → More → Language → Export →
dialogs and accessibility states. Each must undergo screenshot diffing
against properly rectified reference captures at matched locale, DPI,
viewport and UI state before any zero-pixel-difference claim.

## Reproduction

```sh
python3 -m unittest tool.test_splash_brand_parity
cd platforms/android
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

The CI equivalent runs on a GitHub-hosted Android SDK build runner.
Do not interpret a successful source test as a completed emulator run.
