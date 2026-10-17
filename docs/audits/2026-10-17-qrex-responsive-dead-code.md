# QREX 1.0.0 — responsive UI and dead-code audit

**Date:** 17 October 2026  
**Source:** `bren-wp/QREX`; baseline `main` commit `c3a3756b43c5b8e57d9e4ada50cf4768fecfa593`.  
**Scope:** Kotlin/Jetpack Compose Android, Swift/SwiftUI iOS, Flutter compatibility app, resources and CI.  
**Acceptance:** This is a **source-level and CI-backed review**, not a claim of visual pixel-for-pixel parity on every physical mobile phone. True 1:1 sign-off requires comparing approved reference images against the actual screens on multiple devices.

## Files and tooling actually reviewed

- **12 native production source files:** 6 Android Kotlin and 6 iOS Swift files, including UI, camera/gallery scanning, local history, QR generation, local encryption, document export and localization.
- **10 Flutter production Dart files:** app startup, navigation, scanner, Create, Result, History, More, payload, persistent store and shared theme.
- Existing Android JUnit/instrumentation tests, Swift XCTest, Flutter widget/unit tests, Android screenshot gates and localization tests were examined for coverage.
- Examined `.github/workflows/native-platforms.yml`, `mobile.yml`, `capture-screenshots.yml`, `generate-localizations.yml`, and the production-only release gate via the current repository.
- Reviewed explicit Kotlin imports and top-level/member declaration usage across native and Flutter source. A textual reference-count audit cannot prove absence of reflection, Android manifest references, framework callbacks, route access, compiled resources, or external consumers.

## Confirmed UI issues and changes

| Component | Verified issue in source | Implementation |
| --- | --- | --- |
| Android Create QR | QR image forced to 238 dp regardless of content width; could exceed its padded container on narrow phones or split screen | Use Compose `BoxWithConstraints` and `responsiveQrSideDp` to cap rendered size to the measured width |
| Android Result QR | Result image forced to 230 dp regardless of viewport | Match `BoxWithConstraints` measured-width rule, preserving 230 dp on regular screens |
| iOS Create / Result QR | QR images were assigned fixed width/height 210/240 pt | Change to maximum frame dimensions so SwiftUI can shrink within constrained widths while retaining normal sizes |
| Flutter Create QR | 214 px QR plus 36 px inset could exceed the width of the inner card | Use `LayoutBuilder` to cap barcode and empty-state widths; preserve original 214/250 px dimensions on regular screens |
| Flutter Result QR modal | Fixed 220 px image ignored short landscape viewport height and compact width | Limit image size using current screen dimensions and available horizontal inset |
| Flutter bottom navigation | 4 labels lacked overflow constraints under large accessibility fonts | Constrain labels to a single line with ellipsis and retain full accessible semantics text |
| Flutter scanner | An outer `LayoutBuilder` supplied an unused `layout` argument | Remove **one demonstrably redundant wrapper**; retain the inner `LayoutBuilder` used to size the scan frame from available width/height |

The approved QREX palette, QR contents, navigation destinations, on-device privacy model, persisted data schema and action destinations were not intentionally changed. The UI retains its original preferred dimensions wherever space permits.

## Regression coverage

- New Kotlin/JUnit viewport-bound tests for 176, 238, 400 and invalid available widths.
- New Flutter unit tests for QR size caps and modal horizontal insets.
- Flutter widget coverage added for Create at widths **280, 320, 360 and 430 logical pixels**, with real form input and scroll-to-preview.
- New source contract regression checks for responsive QR frames on Android/iOS/Flutter, and navigation width guards.
- Existing CI provides Flutter static analysis and tests, Kotlin JUnit/Android instrumentation, Android lint/R8, Swift build/XCTest, a real emulator/simulator screenshot smoke test and 23-language resource validation. Review the CI result on the *final PR SHA* before merging.

## Dead-code audit findings

**Removed safely:** unused outer Flutter scanner `LayoutBuilder` and its unused callback parameter. This removes needless layout work without touching camera/preview controls.

**Native candidate review:** `MainActivity` appeared unreferenced in a text-only search, but is the Android manifest launch activity, and must be kept. `makeCoordinator`, `makeUIView`, `updateUIView` and `metadataOutput` are SwiftUI/UIKit/AVFoundation protocol hooks, invoked by the system and must be kept. No verified unused explicit Kotlin import remained after excluding delegated-property compiler operators such as `getValue`/`setValue`.

**Flutter candidate review:** no safely deletable function was established by static cross-source symbol searches of the 10 runtime Dart files. The Flutter implementation and tests remain essential for migration/version compatibility and continued CI coverage; keeping them is intentional, not a lack of audit.

**Assets/resources:** the 23 language catalogs are generated and validated from `l10n/catalog.json`, not dead translations. Existing `assets/brand` and `assets/previews` may have external documentation or marketing consumers, so removing them based solely on internal references risks breaking published assets. Privacy/legal documents, old release notes, upgrade compatibility, screenshot validation and production signing scripts remain required despite infrequent runtime invocation.

**Limitations:** a source/CI audit cannot establish all dynamic calls, every dependency's unused transitive bytecode, physical-device memory/performance, or absolute absence of dead code. Gradle R8 already shrinks the Android release variant, `flutter analyze --fatal-infos` catches common Dart issues, and Swift compiler/Xcode build checks catch type-level problems. Do not delete migration or system callback code based on simple symbol counts.

## Remaining acceptance and risk

- [ ] All latest-commit CI checks are green and both Android/iOS screens are actually captured and checked against approved visual references.
- [ ] Physical compact Android phones, Pixel/Galaxy devices, iPhone SE/mini/Plus/Pro Max, tablets/split screen, keyboard overlays and OS safe areas are exercised.
- [ ] Accessibility font scaling (200%), light/dark themes and landscape check across 23 languages.
- [ ] Camera/torch/gallery QR reading, Wi-Fi history encryption/migration, PDF/ZIP/PNG export, share sheets, lifecycle and cancellation validated across devices.
- [ ] Dependency CVEs, 16 KiB Android native libraries, privacy reports and actual signing certificate lineage reviewed before public stable release.
- [ ] Existing **production-only** `1.0.0` policy maintained: do not publish debug APK, unsigned AAB/IPA or claim App Store/Google Play acceptance before approved signing and store verification.

This cycle implements concrete responsive fixes and a bounded, documented code review. It **does not certify literal 1:1 visual identity on every handset** without multi-size visual diff evidence or physical-device testing.
