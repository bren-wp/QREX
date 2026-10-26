# QREX — Screen 04 Create: preview performance and Wi-Fi export consent

**Basis:** merged UI PR #65 (`main` at `760b30a`), the existing native Compose Create screen, Q/QR identity, and user-supplied Android design references. This change does not modify the app identity, local QR history, encrypted Wi-Fi store, QR encodings, theme, Play upload key or release tags.

## Observed issues in existing code

1. `produceState(key1 = payload)` attempted to render a 680 × 680
   bitmap after every payload update even while the keyboard hid the preview,
   the viewport was too short, or the payload could not be rendered. This
   wasted CPU and allocations while typing or rotating a smaller phone.
2. For Wi-Fi PNG export, the warning was displayed before launching Android's
   file picker, but its confirmation handler read the **current mutable**
   `payload` rather than the exact one for which the warning appeared. The
   warning approval and the exported content must be bound to one value.

## Corrections

- `shouldGenerateCreateQrPreview` enforces all three conditions: IME is
  hidden, layout permits preview, and payload is valid. The bitmap producer
  is keyed by payload **and visibility**, debounced, and generated on
  `Dispatchers.Default` only when necessary.
- Retain the genuine Compose QR preview; no illustration or fabricated
  screenshot is used. Generate/Save actions stay pinned and can be used with
  the IME open. The callback still generates the saved image on an IO worker.
- Record `pendingWifiExportPayload` on Save action. The confirmation
  explicitly approves only that bounded Wi-Fi payload via
  `approvedWifiQrExport`, then clears the transient candidate. Cancel and
  dismiss also clear it. Changing other draft fields cannot change the
  already approved export. Wi-Fi export still requires a warning and uses
  Android Storage Access Framework.
- Keep payloads and Wi-Fi secrets out of diagnostics, release screenshots,
  Bundle saved state, GitHub build logs and test failures.

## Tests

- `QREXCreatePreviewAndExportTest`: combinations of preview visibility,
  short-screen eligibility and validity; exact payload consent snapshot,
  cancellation and non-Wi-Fi rejection.
- `tool/test_create_reference.py`: source-contract assertions for the
  actual preview gate and stable approved-payload export path.
- Existing native JUnit tests, 23-language check, Lint, R8 and Android
  screenshot/real-emulator gates remain mandatory.

## Validation limits

Passing compilation and programmatic tests does **not** certify mathematical
1:1 pixel equality against composite / perspective marketing art.
A numerical pixel-difference assessment requires genuine same-state,
same-locale, same-dimension reference images, plus emulator and hardware QA
for IME transitions, repeated gallery exports, and accessibility.
No new public release should be issued from this unfinished QA change.
