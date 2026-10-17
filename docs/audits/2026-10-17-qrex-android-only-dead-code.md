# QREX — Android-only architecture, removal and dead-code audit

**Date:** 17 October 2026  
**Current implementation:** Native Android Kotlin / Jetpack Compose only  
**Pull request:** #16  
**Previous main baseline:** `f05a40cb48bf683671624c1dd4df9d7f6b70aae0`

## Removal scope and actual repository changes

The user requested complete deletion of the iOS app and exclusive Android
development. A subsequent source-level review found the old Flutter runtime
was no longer an active target: native Kotlin Android has its own scanner,
Create, Result, History, Settings, persistence, encryption, PDF/ZIP/PNG export
and 23 language catalogs.

**Deleted 57 obsolete tracked files** as one tree commit, including:

- All `platforms/ios/` SwiftUI, AVFoundation, PrivacyInfo, Xcode project,
  iOS translations and XCTest sources.
- The iPhone screenshot artifact and simulator screenshot/compile jobs.
- Legacy `lib/`, `test/`, `pubspec.yaml`, `analysis_options.yaml` and
  `.github/workflows/mobile.yml` Flutter code and duplicate QA apps.
- `tool/prepare_platforms.py`, `tool/verify_platforms.py` and the Apple
  production signing script; Apple signing secrets are no longer referenced.

The old Flutter **Android binary** may still be installed by users; its
source was removed only after verifying the native Android `QREXStore`
already includes full upgrade logic. Source deletion **does not uninstall**
previously installed binaries or revoke historical external distributions.

## Android-only build/release gates

- One active Kotlin Gradle app at `platforms/android/`.
- Native CI includes Kotlin compile, JUnit, Android instrumented test APK,
  lint, R8/minified release APK/AAB and the screenshot workflow; iOS/macOS and
  Flutter builds removed.
- Offline 23-language catalog now generates **Android-only** locale XML and
  `QREXStrings.kt`. Compilation/translation checks remain required.
- Manual protected `v1.0.0` workflow signs and publishes only
  `QREX-Android-1.0.0.aab` and `SHA256SUMS.txt` — **no IPA**.
- Protected post-publication cleanup checks that the stable release has the
  correct main commit, no draft/prerelease status, and **exactly those two
  nonempty assets** before removing previous GitHub releases/tags.
- Production still fails closed without real authorized existing signing
  credentials, explicit store/device QA, exact SHA successful CI, and protected
  environment approvals.

## Full dead-code audit results and retained code

**Removed as confirmed obsolete:** SwiftUI/iOS runtime and tests, Xcode
project metadata, old Flutter Android runtime and its old test suite,
duplicate Flutter CI, iOS screenshot/Apple distribution tasks, generated
iOS language resources, obsolete platform preparation and verification tools,
and cross-platform assertions that no longer apply.

**Retained deliberately:**

- `QREXCore.kt` `migrateFlutterV1()` plus legacy
  `FlutterSharedPreferences` parser, duplicate/favorite merge behavior and
  failure-safe migration flags. Deleting this *would destroy upgrade continuity*
  for existing Android customers. Device-backed tests exercise migration.
- `QREXSecretCipher.kt` and protected-key history metadata required to
  decrypt manually saved Wi-Fi credentials across app launches.
- System-invoked Android `MainActivity`, Compose callbacks, camera lifecycle
  and AndroidX Activity Result callbacks even if literal symbol usage appears
  uncommon. Reflection/framework entry points are not dead code.
- Original release-history documents under `docs/releases/v0.*` and audit
  history, useful for provenance; these are *documentation*, not shipped
  runtime code. Brand assets and approved design reference SVGs retained for
  provenance/marketing and 1:1 visual QA.
- Security guard workflows, image/foreground screenshot validation and
  production signing/release integrity scripts — they are called by CI, not
  Android runtime.

**Static audit boundary:** a source audit, text search and successful
compilation cannot prove that *all* reachable code paths are used, that R8
strips every dead instruction, or that every OEM device behaves identically.
The Android R8 shrinker is already enabled for release builds; JVM tests,
device instrumentation, lint, code review and physical-device profiling are
required before claiming zero dead code, zero crashes or 1:1 UI parity.

## Additional protection and tests

New `tool/test_android_only_architecture.py` rejects reintroduced
`platforms/ios`, Flutter runtime, iOS SDK/IPA jobs and old platform
preparation scripts, and asserts Flutter-to-Kotlin history migration
code is still present. `tool/test_native_release.py`,
`tool/test_release_integrity.py`, `tool/test_production_gate.py`,
`tool/test_customer_product_readiness.py`, `tool/test_responsive_layout.py`
and `tool/test_privacy_stability.py` now exercise Android behavior without
referencing deleted projects.

## Outstanding Android-only acceptance tests

- [ ] All current PR-head Android CI workflows complete successfully, not
      just older cancelled or superseded checks.
- [ ] Test 280–430 dp narrow/regular Android layouts, landscape, tablets,
      foldables and font scaling with real reference-image comparisons.
- [ ] Run camera/scanner/photo/torch/permissions, Wi-Fi sharing/clipboard,
      encrypted history, exports and low-memory/background lifecycle flows
      on actual Pixel, Galaxy and other Android OEM devices.
- [ ] Install older Flutter Android APK then **update in place** to the
      approved signed native QREX release, confirming history and saved flags.
- [ ] Verify Play upload certificate lineage, signed AAB and Google Play
      compliance; do not claim release success before independent approval.

**No final 1.0.0 release has been published by this refactor.**
