# QREX screen 07 — Favorites alignment and API 26 CI correction

**Pull request:** #83, open Draft  
**Branch:** feat/splash-scanner-reference-v127  
**Target candidate:** v1.0.27 / versionCode 127

## User interface and behavior
- Favorites is a separate saved-only root tab with the existing search field,
  glyphs, responsive cards and 48dp direct un-save buttons; unlike History,
  it has no Delete or All/Saved overflow controls.
- Card subtitles now consistently display the translated QR category and
  localized relative timestamp, instead of displaying only a date.
- Passwords, arbitrary text QR bodies and URL query tokens remain excluded
  from the summary. Full payloads remain in the Result view on user action.
- Favorites removal still only toggles `saved`; the original History record
  survives, preserving existing user data.
- A Compose semantics state description now reports Saved or Add favorite,
  resolved during composition outside the non-composable semantics modifier.
  Screen-reader descriptions update with the stored saved state.

## Root-cause analysis for the failing pull request
P03 API 26 emulator job 125018912359 failed
`QREXNavigationStressTest.closeAfterTextInputHidesImeAndRestoresCreate`
because `qrex-result-toggle-qr` was offscreen following Create text input
and Result transition, and the test asserted immediate on-screen visibility.
There was no fatal crash or ANR in the sanitized API 26 buffer.
The test now performs `performScrollTo()` to scroll the *real* Result
actions into view, verifies visibility, physically clicks and awaits an
actual `qrex-result-qr-ready` bitmap. No checks are skipped.

## Regression and limitations
- `QREXFavoritesScreenTest` verifies the subtitle and existing real
  favorite add/un-save flow; it also asserts the History record survives.
- `tool/test_history_reference.py` guards accessible state and both
  History/Favorites subtitle tags.
- P03 API 26/31/34/35/36, Kotlin/JVM, lint/R8, 23-language resources and
  actual emulator screen capture must pass on the exact final commit.
- The provided marketing collage contains a History mockup, but **no flat
  Favorites device screenshot**. A mathematically verified 1:1 pixel match
  of Favorites to that absent source is not possible. These are real
  Compose improvements, not image mockups.

## Distribution
Do not merge or publish a GitHub release until exact-head checks are green.
Any public CI-distributed APK/AAB certificate is not the authorized
Google Play upload signature.
