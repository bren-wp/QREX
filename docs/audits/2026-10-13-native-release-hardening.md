# QREX — Native Release, UX, Performance and Dead-code Audit

**Date:** October 13, 2026  
**Baseline:** `main` commit `99443c24740dec445d32bc116da035940c4f5256` (previous privacy hardening merged).  
**Scope:** Standalone Kotlin/Compose Android and SwiftUI iOS apps, the Flutter compatibility release, 23-language catalogs, CI workflows, signing configuration and store metadata.

## Verified changes in this pull request

| Area | Root cause / observed issue | Implementation |
| --- | --- | --- |
| iOS signing | The project disabled signing by default and declared `keychain-access-groups` without a developer-team prefix, although no cross-app sharing was used. | Permit real signing, give iOS marketing version 0.2.0/build 5, remove unnecessary entitlement file and use the automatically assigned private app Keychain group. |
| Android signing | No configured route for injecting the existing Play upload keystore into Kotlin release builds. | Optional release signing from four protected environment variables, fail when partially configured; never commit/upload credentials. |
| Android release QA | Only debug APK, JVM tests and instrumentation APK were verified. | Add `:app:lintDebug`, R8-minified release APK and AAB builds, check unsigned AAB artifact presence; mark artifact **QA only**, not store ready. |
| iOS release QA | A simulator app is not an iOS distribution archive. | Add unsigned device-archive build and plist check; label the artifact as an unsigned archive, not an IPA. |
| Native metadata | Independent version/bundle identifiers, API levels and privacy declarations could drift silently. | Add a fail-closed, standard-library Python metadata gate and regression tests for accidental API, signing, ID and permission changes. |
| Android QR history performance | Updating one entry could re-encrypt every manually saved Wi-Fi record via Android Keystore. | Reuse authenticated ciphertext for unchanged Wi-Fi entries, update cache only after a durable commit and wipe it on explicit erasure; add an instrumentation regression. |
| Wi-Fi QR compatibility | Android and Swift differed in escaping quotation marks in SSIDs/passwords. | Escape quotes in Kotlin and add a JVM regression; keep semantics consistent. |
| Storage error UX | Save/delete failures could be silent, including failures caused by unreadable encrypted history. | Return Bool from native stores and present a localized failure message. Native apps ask for confirmation before saving Wi-Fi credentials from both Create and Result views. |
| Localization | New feedback must respect all existing 23 supported languages. | Add `operation_failed` to the source catalog, synchronously regenerate Android/iOS resources and Android resource dispatch, and enforce parity in CI. |
| Android gallery | Generic content selection did not use the dedicated OS visual-media picker. | Switch to `PickVisualMedia` with image-only access, retaining background decoding and image-size limits. |

## Retained code — not dead

- Flutter `lib/`, `test/`, `pubspec.yaml`, the Flutter `mobile.yml` build and its helper scripts remain needed because the last **published v0.1.3** APK was built from Flutter, and the older saved-data format must remain migratable. Do not delete these yet.
- Kotlin CameraX/ML Kit/ZXing and Android Keystore remain required for independent scanner/QR generation/secure history.
- SwiftUI/AVFoundation/Vision/PhotosPicker/Core Image/Keychain code is used by the independent iOS build.
- Generated 23-language strings are build outputs from `l10n/catalog.json`, not redundant hand-maintained business logic.
- Verified app PNG screenshots, branding, source documentation and instrumented tests are preserved.

## Release limitations

**No claim of pixel-perfect 1:1 parity, zero crashes, App Store acceptance, Google Play approval or signed public distribution is made by this audit.** Current screenshot workflows confirm real device pixels and basic screen authenticity, not manual image-diff parity for every screen and size.

The Android/iOS CI is the necessary first stage; additionally required before native production:
1. Pass all tests on the **last commit of this PR**, including Android lint/R8/AAB, iOS XCTest and device archive, screenshot capture and 23-language parity.
2. Verify platform privacy and signing using the actual release artifacts: compiled merged Android permissions, Play data safety, 16 KiB native-library compatibility, Apple required-reason API/privacy manifest declarations and certificate/provisioning status.
3. Run real-device click-by-click tests for Scan, Create, Result, History, More, privacy, settings and permissions, including denial, background/foreground, camera interruptions, gallery and QR export.
4. Confirm visual 1:1 against approved reference images on small/large Android and iPhone displays, tablets, light/dark themes and enlarged text. Existing screenshots alone are insufficient.
5. Complete both the Flutter-to-Kotlin migration checks and native Keychain persistence/erasure behavior on real install/update/reinstall scenarios.
6. Sign Android with the existing Google Play upload key and validate the signing chain. Archive/export iOS using the authorized Apple Developer team and TestFlight first.
7. Submit store metadata, privacy policies, support links, screenshots, age ratings and final declarations from the actual signed binaries.
8. Publish a new GitHub release **only after** all blocking requirements are complete. The native QA AAB and unsigned iOS archive must never be presented as fully signed store builds.

## Official platform references

- Google Play Android 16 / API 36 requirement (from August 31, 2026): https://support.google.com/googleplay/android-developer/answer/11926878
- Apple App Store build upload guide: https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/
- Apple default private Keychain access groups: https://developer.apple.com/documentation/security/sharing-access-to-keychain-items-among-a-collection-of-apps

See `docs/STORE_CHECKLIST.md` for detailed signing and release commands.

**Audit conclusion:** The listed unused entitlement and duplicate/repeated operations have been removed or consolidated. A source-level audit cannot prove all code paths are reachable or correctly exercised; CI, instrumentation, device and visual QA remain mandatory.
