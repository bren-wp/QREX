# QREX native About, Privacy and Terms — UI and responsive QA

**Same open PR:** #83, branch `feat/splash-scanner-reference-v127`.
**Target:** v1.0.27 / versionCode 127 — not yet a published release.

## Screen 11 — About
- Preserve branded native QREX dialog, installed APK version, translated
  tagline, developer information and separate intentional external website
  action. The brand/tagline/domain are verified in real Compose tests.
- Long translated copy scrolls independently from 48dp Close and website
  buttons without discarding private QR records or replacing the original UI.

## Screen 12 — Privacy
- The privacy summary now uses weighted Row text, preventing its trailing
  lines from overflowing the panel at ~280dp and high text scales.
- History switch persists the local preference, never clears existing
  History/favorites, and remains reversible. The existing full policy link
  opens only after a direct tap; local summary always stays inside QREX.
- Instrumentation checks text visibility, scroll, switch and both actions.

## Screen 13 — Terms
- Native modal opens from the existing More card; title stays visible and
  the complete translated summary scrolls, including at 200% font scaling.
- Close and optional external Terms document actions now have independent
  48dp minimum targets and stable UI testTags. External browser never
  launches automatically. `QREXTermsScreenTest` checks these interactions
  and confirms the existing History is unchanged on Close.
- Debug-only screenshot route `qrex_screenshot=terms` cannot be enabled
  in release builds, cannot modify the persisted theme or history, and
  uses the actual Compose dialog, never a generated image.

## Actual screen-size matrix
- Existing twelve 1080×1920 Play-sized emulator screenshots stay unchanged.
- Eight responsive device/font/theme profiles (compact, reference, 160%
  fonts, light, 280dp small-phone, 800dp tablet, landscape, and 200% fonts)
  include the real Terms dialog: **70 additional actual emulator screenshots**.
- Source screenshot checker requires QREX foreground, localized terms summary
  and Close action, rejects system ANR, validates PNG CRC and exact profile
  dimensions; no screenshots are stretched or fabricated.
- Landscape Language validation uses the first two genuinely visible
  localized language options (`Jezik uređaja` and English), because Croatian
  is correctly scrollable but below the fold at 720px device height.
  This check remains stricter on tall portrait devices.
- Every requested physical screen dimension, folding posture, density and
  OEM configuration cannot be exhaustively emulated; verified profiles
  provide meaningful automated coverage but not a universal guarantee.

## Shipping gate
Do not merge PR or publish v1.0.27 until exact-head CI, localization,
Lint/R8, 82-screen real screenshot capture and five-API instrumented tests
are all successful. None of the screenshots provides a quantitative 1:1
pixel match to perspective-warped reference artwork.
