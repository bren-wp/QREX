# QREX Screen 10 — Export Archive: UI, privacy and responsive verification

**PR:** [#83](https://github.com/bren-wp/QREX/pull/83)  
**Branch:** `feat/splash-scanner-reference-v127`  
**Version candidate:** 1.0.27, versionCode 127

## Native UI and accessibility
- Keep actual Android Compose Export page, a fixed 52dp Back target, PDF
  action, no-saved empty state and vertically scrollable privacy disclosures.
- PDF export remains disabled when there are no saved entries. A localized
  accessibility state explains why the control cannot be activated.
- Allow long translated warning and privacy text to consume remaining Row
  width, avoiding clipped trailing text on narrow phones.
- Continue using SAF `CreateDocument(application/pdf)`; only explicit
  consent can freeze a snapshot of saved records and launch the picker.
  The picker may be cancelled without writing or deleting any records.
- Do not modify signing keys, data encryption, storage format or permissions.

## Required real-device-emulator tests
- `QREXExportScreenTest`: actual Compose PDF enabled/disabled semantics,
  pinned Back, scrollable warning/privacy text, consent screen, cancellation
  preserving previously saved data, and 48dp dialog touch height.
- `tool/test_more_about_reference.py`: protected, noninteractive exported
  disclosures and no implicit document creation.
- `tool/capture_responsive_android.sh`: actual on-emulator snapshots
  now cover small (~280dp), compact (320dp), reference phone,
  tablet (800dp), landscape and 200% font profiles, with exact size checks,
  foreground-app checks and no system ANR overlays.
- `tool/verify_screenshots.py`: published screenshots retain strict
  portrait minimum resolution; responsive-mode validation permits genuine
  landscape screenshots while rejecting tiny/corrupt PNGs.
- These are representative density/size states, **not a claim to test every
  physically possible screen resolution, OEM skin, device or OS release**.

## Visual claim and release gate
The user reference is a perspective board, not a rectified Export screenshot,
so mathematical 1:1 matching cannot yet be verified. This pass changes actual
code and automated UI coverage, not generated marketing images.

All exact-head Kotlin/JVM, Lint/R8, 23-language, authentic screenshot and
P03 API 26/31/34/35/36 checks must pass; PR stays Draft until verified.
v1.0.27 is not released or signed for Google Play by these changes.
