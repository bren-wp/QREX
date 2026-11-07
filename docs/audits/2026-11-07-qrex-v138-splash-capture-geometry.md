# QREX v1.0.39 — verified screen 01 Splash layout bug and fix

**Scope:** Splash screen only, before changing Home / Scanner again. Previous
verified release: [v1.0.38](https://github.com/bren-wp/QREX/releases/tag/v1.0.38).
Native Android source remains in `QREXSplash.kt`; no static screenshot or
marketing rendering is substituted for the actual UI.

## Evidence from genuine v1.0.38 emulator capture

Inspected the existing `qrex-actual-android` artifact from successful
screenshot workflow run 42414625199; `android-splash.png` is a genuine
1080×1920 RGBA capture (not a generated replacement). On that emulator,
Splash is a compact/short-height **scroll** layout, not the regular
centered layout. The existing scroll Column packs the logo, QREX name,
translated tagline and progress immediately from its top padding. The
bottom progress ends above the lower wave area with an excessive empty
region beneath it. This differs from the supplied Splash marketing phone
reference in which the footer/progress sits close to the bottom.

## Root cause

The old branch used:

- `Modifier.fillMaxSize().verticalScroll(...).padding(...)`
- `verticalArrangement = Arrangement.spacedBy(26.dp)`
- Two consecutive children: `QrexSplashBrand`, `QrexSplashFooter`

That code never distributed the remaining vertical viewport height;
its footer therefore followed the logo instead of remaining at the
bottom on otherwise-fitting compact windows.

## Corrective implementation

- Keep the same outer scroll container and original canonical
  Q/QR vector, cyan blue waves, QREX wordmark, localization and
  progress asset/semantics.
- Add an inner Column with `heightIn(min = maxHeight)` measured from
  the actual `BoxWithConstraints` viewport, 20dp horizontal and
  24dp vertical safe padding.
- Use three children (zero-height flexible leading spacer, brand,
  footer) with `Arrangement.SpaceBetween` to center the brand in
  the remaining space and keep the footer near the bottom.
- The content grows beyond `maxHeight` when system accessibility
  font scale or long translated content requires additional height;
  the single outer verticalScroll stays functional. This does
  **not** impose fixed text sizes or truncate foreign languages.

## Regression validation

- `QREXSplashReferenceTest`: check logo/title/tagline/footer order
  and assert the compact scroll-footer bottom gap is 0–64dp on
  the real Android test viewport. The v1.0.38 capture showed a
  substantially larger gap, making this a meaningful regression.
- `test_splash_brand_parity.py`: reject top-packed scroll code.
- The existing pure layout tests cover 320×568, 393×852, 800×1280,
  larger system font settings and branded vector paths.
- All four branch and exact merged main CI workflows must succeed:
  Kotlin/JVM plus Lint/R8, 23 localized resources, authentic emulator
  PNG capture, and full API 26/31/34/35/36 P03 instrumentation.
- GitHub release must verify APK/AAB signatures and all 16 expected
  assets, including 12 genuine emulator screenshots and SHA256SUMS.

## Boundaries

The fix addresses a specific, observed geometry defect. It does not
establish numerically **0% pixel deviation** versus an angled/illustrated
marketing collage. Such certification requires an original flat screenshot
at exactly matching DPI, locale, OS bars, UI state and size. Home/Scanner
live CameraX frame content is intentionally not modified in this pass.
