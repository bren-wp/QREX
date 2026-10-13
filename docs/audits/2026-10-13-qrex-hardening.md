# QREX — Source, Dead-code, Privacy and Lifecycle Audit

**Date:** 2026-10-13  
**Target:** Kotlin/Jetpack Compose and Swift/SwiftUI source in `platforms/`, existing Flutter release in `lib/`, mobile CI workflows, translation tooling, and device test suites.  
**Basis:** Direct source inspection and GitHub Actions history; this is not a certification of visual parity or crash-free operation.

## Confirmed issues and source changes

| Area | Verified finding | Change |
| --- | --- | --- |
| Android history loader | One undecryptable entry was previously skipped; the next save could overwrite the remaining ciphertext. | Mark history read-only on corruption/Keystore failure; never persist a partial load. Explicit user erasure remains possible. |
| Android Delete all | Clearing `qrex.user` did not clear pending Flutter SharedPreferences values, allowing re-import after restart. | Remove QREX-specific Flutter history, theme and recording settings before clearing current preferences; confirm both commits before updating UI state. |
| iOS Delete all | The Keychain delete return status was ignored, and pending Flutter values survived the reset. | Treat absent Keychain entries as success, propagate genuine deletion failures, erase known legacy keys, and only then clear the view model. |
| Android gallery scanning | The bitmap decode/file inspection ran on the Compose UI thread and did not bound decoded image dimensions. | Move provider access and decoding to `Dispatchers.IO`; preflight byte count and up to 24 megapixels before opening the ML Kit input; cancel UI callbacks after leaving the screen. |
| Android camera analysis | CameraX analyzer ran on the UI executor with a redundant local `process()` wrapper. | Run analysis on a dedicated single-thread executor; release analyzer, scanner, and executor on disposal; guard stale callbacks. |
| iOS camera | QR camera session and torch could continue while the scan result sheet was presented. | Tear down camera view for a presented result, switch torch off on queued shutdown and do not construct unused camera inputs on re-entry. |
| iOS QR rendering | Re-created `CIContext` for every preview. | Reuse a shared Core Image context; keep generation on a background task for interactive editing. |
| Dead parameter | Android `migrateFlutterV1(context)` no longer needed the context after using stored legacy preferences. | Remove unused method argument and call-site parameter. |
| Stale GitHub automation | The screenshot and localization jobs had hardcoded checkout/push commands for an already merged development branch. | Make both workflows read-only PR quality gates. No automated writes to an obsolete branch; preserve screenshot artifacts. |
| CI regression coverage | Stale-branch writes could return after future workflow edits. | Add Python regression tests requiring read-only permissions, valid screenshot gates and no obsolete branch names. |

## Deliberately retained code and dependencies

- **Flutter `lib/`, `test/`, and `pubspec.yaml`:** still build and deliver the published **v0.1.3** APK; they are not dead code while that release and its migration compatibility matter. The `mobile.yml`/release workflow still builds the Flutter distribution.
- **Kotlin CameraX, ML Kit, ZXing and Android Keystore:** each has an actual use in scanning, generation, camera lifecycle or protected Wi-Fi history.
- **Swift AVFoundation, Vision, PhotosPicker, Core Image, Security and iOS Keychain:** used by the independent Swift app; do not remove based on import counts alone.
- **`tool/prepare_platforms.py` and `tool/verify_platforms.py`:** required by the Flutter release build and/or by the iOS platform app-icon step.
- **23-language generated platform resources:** validated against `l10n/catalog.json`, not duplicated hand-maintained source.
- **Existing brand and documentation assets:** not removed without confirming their release and marketing consumers.

## Regression coverage added

- Android emulator storage tests for corrupted ciphertext protection and erasure of unmigrated Flutter history.
- iOS XCTest for a legacy secret that must stay erased after restart.
- Existing QR parsing, language parity, Keychain/Keystore, screenshot integrity and emulator tests remain enabled.
- CI runs for this PR must be assessed at its **latest head SHA**. A cancelled older run is not a pass.

## Outstanding release blockers / additional QA

1. Execute all Kotlin unit and instrumented tests, Swift XCTest and screenshot workflows on the latest changes; investigate failures rather than relaxing checks.
2. Validate Android gallery decoding/permission cancellation, camera lifecycle, repeated open/close and large-image rejection on physical devices.
3. Validate iOS camera/torch teardown, QR export, photo selection and Keychain error handling on a physical iPhone.
4. Complete feature parity, including QR event creation, and compare every screen against original design references on representative screen sizes.
5. Test migration with real-format Flutter v0.1.3 data, backup/reinstall and unavailable Keystore/Keychain. Unreadable Android history is deliberately kept read-only but still needs a user-facing recovery/error flow.
6. The current **published APK remains Flutter v0.1.3**. Its privacy guarantees are not retroactively upgraded by native Kotlin/Swift commits.
7. Require an audited Android release AAB/signing key and a signed iOS distribution archive before replacing the public build. No release/version bump is justified by static inspection alone.

**Audit conclusion:** Specific dead/duplicated code and obsolete automation were removed or consolidated. This does **not** prove all executable code is used or that the independent apps are release-ready; remaining issues are tracked above.
