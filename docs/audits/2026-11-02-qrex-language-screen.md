# QREX screen 09 — Language / Jezik

## Status and reference boundary
Open Draft PR #83, `feat/splash-scanner-reference-v127`,
candidate `v1.0.27` / Android `versionCode 127`.
The supplied marketing composite contains **no standalone Language page**;
therefore a mathematically proven pixel-for-pixel clone of that page cannot
be claimed. Do not substitute generated images for live Compose controls.

## Native UI implementation
- Preserve the real full-screen Language page with an always-visible
  52dp Back touch target, scrollable LazyColumn of 23 supported offline
  translations and an Android System default option.
- Each row remains at least 64dp tall, with an aligned 36dp flag/globe
  slot, native language name, code, trailing radio indicator and visibly
  highlighted currently saved selection.
- Remove **duplicate accessibility nodes** from the decorative glyph and
  radio icon using `clearAndSetSemantics {}`. The real clickable Surface
  alone exposes the selected state and performs the persistent action.
  Rendering and text styling are intentionally unchanged.
- Language updates still persist via `store.updateLanguage(code)` before
  updating the displayed preference; failures retain the previous choice.
  No migration, history reset, network SDK or signing change.

## Verification
- `QREXLanguageLayoutTest` measures actual Compose emulator bounds:
  each flag slot is 36dp × 36dp (±1dp tolerance), all slots align, remain
  within their 64dp touch row, and existing width/overlap and scrolling
  assertions remain intact.
- Existing instrumented selection-persistence test selects Croatian or
  German, asserts the persisted setting, verifies History record counts
  and restores the prior language on completion or failure.
- `tool/test_more_about_reference.py` guards structural, selection and
  single-click-target semantics without replacing emulator tests.
- Require exact-head Kotlin/native, Lint/R8, 23-language resource checks,
  actual emulator screenshots and full API 26/31/34/35/36 instrumentation.
  The earlier More pass had successful native CI and captures, four API jobs
  successful and API 36 still pending when this new screen pass began.

## Release caution
No GitHub `v1.0.27` tag or signed assets may be claimed without checking
the published release. The public CI certificate is not a Google Play
upload signature; preserve the authorized Play key and app data.
