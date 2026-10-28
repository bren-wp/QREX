# QREX — Result-screen Wi-Fi PNG export consent (2026-10-28)

## Scope

A narrowly scoped Android privacy fix on top of released v1.0.17 (versionCode 117). No history schema, favorites, Android Keystore, app identity, translations, signing keys, or released artifacts are changed. The provided visual references remain the visual targets; this patch does not claim measured pixel-perfect equivalence.

## Finding

The Create screen warned before exporting a Wi-Fi QR to a PNG, and Result warned before sharing a Wi-Fi QR or adding it to Favorites. However, Result's **Show QR → Export PNG** used `ActivityResultContracts.CreateDocument("image/png")` without Wi-Fi consent. Saving or distributing this QR may reveal the network password.

The Result PNG callback also used the composable's current `raw` rather than an explicitly captured launch payload. The SAF picker can outlive navigation or recomposition; a previously approved export must not silently change the QR data written to the selected document.

## Change

- Display an explicit localized warning for Wi-Fi PNG exports on Result, before invoking Android's file picker; Cancel/dismiss does not launch the picker.
- Reuse the existing guarded `approvedWifiQrExport` helper for the authorization boundary.
- Bind the SAF callback to `pendingResultPngPayload`, set only for a specific approved QR export, and clear it on callback. If Result is replaced during the picker, do not export an unrelated QR.
- Ordinary non-Wi-Fi PNG exports keep the existing one-step flow.
- Keep the existing on-device PNG generation and temporary bitmap recycling. No new permissions, network services, strings or external dependencies are added.
- Add Compose instrumentation coverage for the Create → Wi-Fi Result → Show QR → Export PNG → Cancel → retry path.

## Required verification

The new Android test is `QREXResultWifiPngConsentTest`. The PR must pass Kotlin debug compilation, JUnit, connected Android instrumentation, Android Lint, R8 release and AAB builds, 23-language checks, actual Android screenshot checks, and GitHub Actions before merge or a new release. Validate manually on small devices and API 26/31/34/35/36 with a genuine QR, including returning from a cancelled SAF picker.

**Status:** source changes and regression test submitted for CI; do not mark device QA or a new public release complete until the actual workflow results confirm them. No new key or Play-ready signature is implied.
