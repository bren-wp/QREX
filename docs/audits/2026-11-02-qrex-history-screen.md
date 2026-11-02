# QREX screen 06 — History (Povijest) implementation and QA

## Scope
Continue in open draft PR #83 for Android versionCode 127 / v1.0.27.
The user-supplied device reference is a perspective marketing composite, not
a rectified screen-by-screen pixel baseline. A verified mathematical 1:1
pixel match cannot be claimed without a flat aligned reference.

## Native Compose improvements
- History keeps its header, search, filter menu, scrollable list, icons and
  localized relative timestamps. Favorites remains a separate root tab.
- History row subtitles display localized QR categories rather than arbitrary
  raw QR bodies. URL query tokens and event-location notes are not echoed.
- Unknown/plain-text QR rows display the localized Text category as the title,
  not potentially private free-form contents; links still show their host.
  Full payloads remain accessible only by opening Result.
- Every History delete action is reachable through the real overflow menu
  and requires a dedicated confirmation dialog. The Delete and Cancel
  actions have a 48dp minimum height and stable Compose semantics targets.
- No automatic deletion, history migration, permissions or external service
  added. User data, encrypted Wi-Fi storage and private signing unchanged.

## Regression coverage
- `tool/test_history_reference.py`: enforces 23-language filter contracts,
  safe category-based History rows and explicit delete dialog controls.
- `tool/test_ux_dead_code.py`: ensures title truncation remains responsive
  while generic text QR bodies are not printed in list titles.
- `QREXHistoryPrivacyTest`: creates an isolated synthetic URL, verifies search
  does not reveal its query token, Saved/All filter works, Cancel preserves
  the row, Delete removes only that record, and cleans it up on failure.
- All exact-commit Native CI, Lint/R8, actual screenshot capture and emulator
  API 26/31/34/35/36 gates must pass before merge or release publication.

## Release
The public GitHub APK/AAB publishing identity is not the protected Play
upload key. v1.0.27 remains a candidate until every gate passes and the
actual GitHub Release tag plus assets are confirmed.
