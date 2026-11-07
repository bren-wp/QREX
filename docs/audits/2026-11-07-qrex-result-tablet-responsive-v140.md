# QREX v1.0.40 — Screen 03 Result responsive geometry audit

## Scope and evidence
Verified base: released v1.0.39, Android `com.brendigo.qrex`, code 139.
This change **only changes Result screen geometry and tests**, not the QR
content, camera, navigation destinations, persistence, Wi-Fi consents,
export security policy, or existing default 393dp phone typography.

Earlier reference notes identify one primary navy summary card, accessible
action rows, a pinned header, and scroll-safe five-tab navigation. Exact pixel
comparison is not possible against the perspective marketing collage without
unaltered flat reference PNGs in the same window size/density/locale/UI state.

## Identified responsive mismatch
The Result scroll column previously occupied 100% of all available device
width, including 800–1024dp tablets. This stretches the single reference-style
content card and action rows across a landscape/full-sized window. The QR
preview remained 230dp even on large screens.

## Implemented
- Preserve the complete **phone** layout at widths up to 520dp.
- For wide windows cap Result content column to 520dp using a proportional
  `fillMaxWidth` fraction and center it inside the existing top-level Column.
  At 800dp, fraction is 0.65; at 1024dp, 0.5078125.
- On widths >=600dp increase summary minimum from 154dp to 178dp, category
  plate from 54dp to 62dp, vector glyph from 28dp to 32dp, and QR preview
  maximum from 230dp to 290dp.
- Preserve pinned Back/overflow header, IME padding, internal scrolling,
  real preview generation, and five-tab navigation. No new raster/illustration
  UI is introduced.

## Verification requirements
1. JVM width-fraction tests for 320/393/520/800/1024dp and font-size checks.
2. Android Compose checks that summary <=520dp, centered on >=600dp windows,
   and cannot overlap the pinned bottom navigation even after scrolling.
3. All existing QR/privacy/CameraX/Keystore tests and all 23 localizations.
4. Exact PR-head and merged-main native CI, Lint/R8, 12 genuine screenshots,
   Android API 26/31/34/35/36 P03 matrix, signed GitHub APK/AAB and SHA256SUMS.

**Not certified:** 0% absolute pixel deviation. Wide-window emulator tests
must execute and be checked before any claim of full tablet visual validation.
