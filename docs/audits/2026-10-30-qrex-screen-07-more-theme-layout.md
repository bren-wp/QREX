# QREX Android — screen-by-screen parity: More / Settings (30 October 2026)

## Target and evidence
The next native QREX screen after Favorites is **More**. The previously supplied dark Android concept uses a 3 × 2 array of six settings tiles and a distinct full-width destructive action. The supplied light Android concept uses full-width settings rows. Both are valid presentation states; they must not be blended into one visual variant.

Original source reference boards are perspective-transformed marketing art, not aligned, same-density PNG reference screenshots. Therefore exact 0-pixel difference has **not** been measured or claimed.

## Implemented in existing PR #80
- The existing **dark** appearance keeps its original 3 × 2 grid when at least 300dp wide and font scale below 1.30; the final **Clear history** row remains full-width and confirmation-protected.
- The **light** appearance now uses six full-width settings rows, matching the layout family of the actual supplied light reference rather than inadvertently reusing the dark grid.
- On dark screens narrower than 300dp or at font scale 1.30 and above, settings switch to rows to avoid tiny grid tiles and truncated translations.
- The layout decision uses the **actual rendered theme** from the app shell, including debug-only light screenshot capture, not just the persisted preference. This avoids a false dark grid on `qrex_theme=light` emulator captures.
- The current design tokens are unchanged: dark navy background, violet/cyan accents, rounded cards, restrained shadows and native Material3 controls. No image-based fake controls or remote assets.
- Added stable dialog and button test tags for Clear history. Confirm remains explicit and only Cancel is used by the new regression test.

## Tested / required
- Kotlin JVM layout rules: dark standard grid, light row preference, narrow/large-font fallback.
- Compose regression `QREXMoreThemeLayoutTest`: open real More, set Light, measure all six real row bounds (equal width, aligned left edges, no overlap, at least 48dp each), open Clear history dialog, cancel, check no history removed, restore original theme.
- Preexisting `QREXMoreAboutTest`: dark grid 3 × 2 aligned, controls accessible, About native modal and secondary website button.
- Existing screenshot workflow already captures actual dark More at 1080 × 1920, compact/reference and large-font More, plus a separate light More QA image. These are **real emulator captures** and must pass CI; none constitute source-reference pixel identity by themselves.
- Required CI still includes Kotlin/JUnit, Lint/R8, localization, screenshot checks, and Android API 26/31/34/35/36 lifecycle matrix.

## Non-regressions
Preserve all six features (Theme, Language, PDF export, Privacy, Terms, About), History clearing confirmation, Wi-Fi Keystore protection, 23 offline languages, saved QR records, bottom navigation, package identity and Google Play signer. This PR does not bump versionCode or create/re-sign store artifacts.

## Next screen
After More passes all required checks, proceed separately to **Language** screen polish within the same pull request. Do not declare 1:1 before flat, pixel-aligned originals are available and the existing `tool/compare_ui_pixels.py` reports measured differences.
