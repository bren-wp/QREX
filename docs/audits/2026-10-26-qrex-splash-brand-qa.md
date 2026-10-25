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

## Follow-on screen order

Scan → Result → Create → History / Saved → More → Language → Export →
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
