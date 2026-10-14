# QREX — UI/UX, source hygiene, migration and release audit

**Date:** 14 October 2026  
**Scope:** `bren-wp/QREX` PR #8; Kotlin/Compose Android, SwiftUI iOS, legacy Flutter compatibility, localization, PDF/ZIP/PNG export, public release documents, automated QA.
**Method:** Review of source files, prior audit reports and exact-commit GitHub Actions results. This is a source-level audit, not a physical-device certification.

## Confirmed changes

| Area | Identified problem | Remediation and regression coverage |
| --- | --- | --- |
| Android/iOS legacy history | When QR data occurs in both native and older Flutter history, an unsaved record could override a saved/favorite entry. | Merge saved flags without dropping records; Android device-backed and iOS XCTest cases cover duplicates and restart persistence. |
| Android local history | Duplicate serialized rows could silently lose a later `saved: true` flag. | Canonicalize records while preserving favorites, commit the upgraded history; Android instrumentation regression added. |
| iOS PNG export | The Export PNG button opened a generic image share sheet rather than exposing a .png file destination. | Use a FileDocument with PNG signature validation and iOS Files exporter; add XCTest for PNG image data/document type. |
| iOS appearance | Fixed dark surfaces and cyan foreground conflicted with the light theme. | Use trait-aware light/dark surface and accent colors; adapt scanner text to dynamic foregrounds. Requires visual device review. |
| History deletion | Android deleted rows immediately; iOS cleared unsaved history without confirmation. | Require destructive confirmation; preserve saved records when clearing unsaved history. |
| History search | Only short labels were searchable and favorites lacked a direct filter. | Add saved-only filters on Android/iOS, search non-sensitive raw text while excluding Wi-Fi passwords; Android unit regressions. |
| iOS QR creation | Result sheet depended on mutable input, which could change after result selection. | Capture and present the selected QR payload as an immutable result. |
| Legal/release links | README, apps and older release notes linked to the previous repository. | Point links to `bren-wp/QREX`, update version metadata/documentation and enforce regression tests. |
| Obsolete source | The iOS UIKit image-share representable became unused after adding the actual PNG exporter. | Remove that component and its associated state; retain required FileDocument/PhotosPicker classes. |

## Dependency / dead-code review

- **Retained deliberately:** legacy Flutter `lib/`, `test/`, `pubspec.yaml`, `mobile.yml`, and `tool/prepare_platforms.py` remain used for the last published Flutter APK, regression coverage and migration compatibility. Removing them would not be a safe dead-code optimization.
- **Retained deliberately:** Android CameraX, ML Kit, ZXing, Keystore and iOS AVFoundation, Vision, Core Image, Keychain and PhotosPicker have active scanner, generator, history and privacy consumers.
- **Retained deliberately:** 23 generated Android/iOS language resources remain outputs of `l10n/catalog.json`; resource parity checks must remain in CI.
- **Removed:** unused iOS `QRImageShareSheet` and `showImageShare` implementation, replaced by genuine PNG document export.
- **Not removed without consumer confirmation:** original QREX brand materials and preview SVG assets. Static absence from the current README does not prove absence from past release/marketing consumers.
- **Import screening:** reviewed Kotlin production source imports; Compose delegated-property `getValue`/`setValue` operator imports are necessary even when their symbols do not occur explicitly. No broad import cleanup was performed without compile verification.

## Remaining acceptance gates (not yet proven)

1. All Android Kotlin unit/instrumented tests, lint, R8 APK/AAB and Android/iOS screenshot capture must pass on the **final PR SHA**, as must Swift XCTest/archive, Flutter CI, 23-language parity and release checksum checks.
2. Test scanning, gallery decode, permissions, torch, back/foreground, the new PNG picker, Wi-Fi saving, favorites, document export and all gestures on physical Android/iPhone devices.
3. Validate accessible contrast and Dynamic Type/large fonts in both themes on compact and large displays; manually compare actual screenshots to approved visuals. Single-screen capture does not establish pixel-perfect parity.
4. Test full Flutter-to-Kotlin upgrades, protected history corruption/recovery and iOS Keychain restore/reinstall under actual install/update conditions.
5. Review dependency vulnerabilities, generated manifest/privacy declarations, Android 16 KB native-library compatibility and actual binary permissions; production signing requires the existing Android upload credentials and authorized Apple provisioning.
6. Do not claim a release is Google Play/App Store-ready or universally crash-free based on successful compilation alone.

**Conclusion:** Specific dormant code, broken actions and UI/data-integrity defects have been corrected. A complete proven dead-code elimination and production-readiness sign-off remain blocked by dynamic device tests and pending CI; removing remaining compatibility code by guesswork is explicitly out of scope.
