# QREX — History / Favorites continuation within PR #66

This follows the merged screen 01–03 PR #65 and the Create preview/consent
changes already on the open PR #66 branch. Neither release tags nor public
download assets, stored QR content, encryption or archive formats are changed.

## Findings

1. History's Compose `LazyColumn` previously used `items(found, key = {
   it.raw })`. These keys can participate in saved scroll UI state, and
   raw Wi-Fi payloads contain network passwords. Raw QR payloads must not
   become UI list keys, even on a device-only offline application.
2. History title and subtitle each permitted only a single line even with
   large Android accessibility font scaling, truncating useful text in the
   reference-style cards.
3. The empty state had no stable test tag, preventing targeted instrumented
   checks of empty History and Favorites conditions.

## Corrections

- `QRHistory.uiRowKey(record, visibleIndex)` contains only the non-secret
  timestamp and visible row index. Different rows with identical timestamps
  remain distinguishable. The key is intentionally ephemeral relative to
  filtering/reordering; the record remains correctly identified by its
  original `item` in all action callbacks. No QR payload, prefix, SSID,
  password, personal text or content-derived hash is placed in the key.
- `LazyColumn` uses `itemsIndexed` with the safe key function. The
  History/Favorites filters and actual `store.setSaved` / `store.remove`
  actions are unchanged.
- For font scale ≥1.35×, title and subtitle allow two lines, with 88dp
  minimum row height instead of 68dp; 48dp favorite/menu actions remain.
- Empty-state panel has `qrex-history-empty` tag for UI regression tests.

## Test plan / verified claims

- JVM `QREXHistoryUiKeyTest`: no Wi-Fi credentials, names or arbitrary
  personal content in Compose keys, distinct keys for equal timestamps.
- Existing History source contracts now verify `itemsIndexed`, the absence
  of the former raw-key expression, the empty state and large-font rules.
- Real emulator screenshot and Kotlin/JUnit/R8/API-matrix checks must pass
  on the *final* PR head. This documentation is not a passing-check claim.
- Pixel-for-pixel design comparison is not established by the composite
  perspective source images; record measured device/reference differences
  separately before claiming a numerical 1:1 match.
