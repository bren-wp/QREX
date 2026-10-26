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


## User-reported blocker: empty QR preview obscures Generate

Reported on the live Create screen after this branch began: an empty QR
preview consumed layout height, covering or displacing the user's path to
create a QR code. This was a real usability blocker, not an icon discrepancy.

**Root layout cause:** Create used a weighted scrolling form followed by
an *unweighted* QR preview panel, followed by Generate and Save buttons.
The preview was displayed even with no valid QR bitmap, showing an inert
placeholder; on shorter viewports this competed with form/action height.

**Fix:**
- Only show a preview card after `previewVisible && qr != null`.
  No empty QR box or placeholder is shown for a blank URL, invalid
  payload, during bitmap generation, or with the keyboard open.
- Move the preview panel **inside the weighted, vertically scrollable form**.
  It can no longer reserve non-scrollable height ahead of the actions.
- Keep Generate and Save outside the scroll container as fixed footer
  actions, with validation-driven enabled states independent of QR
  bitmap timing. Existing QR format handling remains unchanged.
- The Android Compose integration test
  `QREXCreateBlockedPreviewTest` checks the empty initial state,
  visible Generate/Save hit targets and actual Generate click from a
  valid URL through to Result while the IME may still be visible.
- The screenshot hierarchy gate now requires *both* translated
  `Generiraj QR kod` and `Spremi na uređaj` labels on Create,
  including the compact and large-font responsive captures; it
  fails closed instead of certifying a screen whose buttons vanished.

**Release gate:** validate Python source regressions, Kotlin JUnit,
Android instrumentation, native build, Lint/R8 and screenshot jobs on
the final PR HEAD. Physical-device reproduction at user viewport/font
settings is still required for a final 1:1 claim.
