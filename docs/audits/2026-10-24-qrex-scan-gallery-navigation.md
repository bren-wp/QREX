# QREX scan/gallery and responsive-navigation audit — 2026-10-24

Source base: v1.0.12 `main` (`94a1c714b27d1d0afc3a63e8d88b730805df27d4`).

## Findings and changes

- **Camera/gallery race:** A live CameraX frame could win while the user was selecting or processing an image. Add an atomic gallery-active exclusion checked before camera decode, after decoder completion and again in queued main-thread callbacks.
- **Repeated picker actions:** Disable the gallery action while the picker or bitmap decoder is active. Restore its state after cancellation, decode failure, successful decode, launch failure or coroutine cancellation.
- **Narrow-phone navigation:** Keep all four localized bottom-tab text labels visible at any font scale; reduce scaled text size modestly for compact widths instead of removing the labels.
- **Documentation drift:** Update README and release archive to reference real v1.0.12 release assets and emulator captures, rather than the old unpublished-v1.0.12 candidate wording.

## Regression gates

Kotlin unit tests include gallery-camera priority, handled-result exclusion and frame throttle; Python checks verify the actual Compose/CameraX source contracts. GitHub CI must compile, run unit tests, lint, build R8 release variants, validate all locales and capture real emulator screens. Manual device checks are still required for photo picker cancellation, TalkBack, rapid repeated scans, landscape layout, API 26 and OEM camera behavior.

No user history, Keystore alias, storage schema, package ID, permission manifest, signing identity or published APK/AAB is changed. This source audit does not claim universal zero-crash certification.
