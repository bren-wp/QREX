# QREX — Google Play and Apple App Store Checklist

This is an engineering checklist, **not** a statement that either store has approved the app.

## Google Play / Android

- [x] Standalone Kotlin/Jetpack Compose Android project without login or ads.
- [x] Target and compile Android API 36.
- [x] Camera permission requested for scanning; system image picker instead of broad media permission.
- [x] Android backup and unencrypted cleartext traffic disabled in the manifest.
- [x] Privacy policy published in English, with an offline localized summary in app Settings.
- [x] GitHub Actions builds an Android debug APK and executes unit tests.
- [ ] Validate each locale's UI and screenshots on multiple display sizes and Android versions.
- [ ] Confirm all transitive library privacy and permission requirements.
- [ ] Protect manually saved credentials before production.
- [ ] Sign the Kotlin release AAB with the **existing Play upload key**, and verify the signature and Play App Signing certificate lineage. The CI QA AAB is unsigned and must not be uploaded as a final signed artifact.
- [ ] Fill in the Google Play Data safety, age rating and store listing forms with real screenshots.
- [ ] Complete internal/closed testing on physical devices.

## Apple App Store / iOS

- [x] Standalone SwiftUI application using AVFoundation, Vision, PhotosPicker and Core Image.
- [x] Privacy manifest supplied and bundled by XcodeGen.
- [x] Camera and photo picker purpose strings declared in English.
- [x] GitHub Actions builds an unsigned iOS simulator application.
- [ ] Verify packaged language resources and execute all XCTest tests on an available simulator.
- [ ] Confirm required-reason API declarations and privacy manifests for any SDK dependencies.
- [ ] Protect manually saved credentials before production.
- [ ] Complete App Store Connect metadata, privacy disclosure, ratings, contact and support information.
- [ ] Sign an **iOS device archive** with the correct Apple Developer Team ID, App Store distribution certificate and provisioning profile; verify bundle ID, version, build number, exported IPA and TestFlight install.
- [ ] Use authentic runtime screenshots for App Store Connect; mockups are not substitutes.

## Settings, PDF and ZIP export validation

- [x] New PDF/ZIP export implementation added on Kotlin Android and SwiftUI iOS, using user-selected system file destinations rather than broad device-storage permissions.
- [x] Warning before exporting saved codes, including those that may encode Wi-Fi passwords; exported PDF/ZIP files are **not encrypted**.
- [x] Settings contain the Privacy Policy, Terms of Use, Brendigo attribution, and English as default plus Croatian and 21 other translations.
- [x] Android adaptive and themed launcher resources added; Xcode icon packaging remains in simulator/device archive CI.
- [ ] Verify PDF readability, ZIP extraction, sharing and cancellation on real Google and Apple devices.
- [ ] Confirm the actual compiled Android manifest, Play policy forms and app-store listings accurately describe sensitive exported content.
- [ ] Confirm localization quality and accessibility for the new legal/export text in each language.

## Official sources

- Google: https://support.google.com/googleplay/android-developer/answer/11926878
- Google privacy: https://support.google.com/googleplay/android-developer/answer/10144311
- Apple review guidelines: https://developer.apple.com/app-store/review/guidelines/
- Apple requirements: https://developer.apple.com/news/upcoming-requirements/
- Apple privacy manifest: https://developer.apple.com/documentation/bundleresources/privacy-manifest-files

Recheck all requirements immediately before submitting a new version.


## Native Kotlin/Swift candidate — signing and publication procedure

The public GitHub v0.1.3 APK is built with Flutter, while the next standalone Kotlin and Swift builds use version **0.2.1** / build number **6**. The previous native preview used **0.2.0** / build **5**. Do not publish both build paths under the same version or mistake successful simulator tests for signed release qualification.

### Android — existing Play upload key only

On a trusted build machine, set these **environment variables** using a secret manager (never paste their values into GitHub issues, logs or commits):

- `QREX_UPLOAD_STORE_FILE` — absolute path to the existing upload keystore
- `QREX_UPLOAD_STORE_PASSWORD` — keystore password
- `QREX_UPLOAD_KEY_ALIAS` — established upload alias
- `QREX_UPLOAD_KEY_PASSWORD` — upload key password

Run `cd platforms/android && gradle --no-daemon :app:lintRelease :app:testReleaseUnitTest :app:bundleRelease`. The build fails if only some signing variables are present. Without them, CI may build an **unsigned QA bundle only**. Inspect the resulting `app/build/outputs/bundle/release/app-release.aab` with `jarsigner -verify -verbose -certs`, confirm the signing certificate fingerprint belongs to the correct Play upload identity, and test an internal Google Play track with a monotonically increasing version code. Do not generate a new upload key unless an account-approved key reset/replacement procedure requires it. Handle the keystore only on trusted machines or dedicated protected GitHub environments.

The Google Play submission must include Data safety declarations based on the exact compiled dependencies, target API compliance, device screenshots, store listing, contact/support/privacy URLs, age and content disclosures, and required testing. Confirm the final signed package ID is `com.brendigo.qrex`.

### iOS — signed device archive

Open `platforms/ios` after running `xcodegen generate --spec project.yml`. In Xcode, set the authorized development team and correct App Store provisioning, and archive using the **Any iOS Device** destination. Use Xcode Organizer to validate and distribute the signed archive to TestFlight before App Store submission.

The Xcode project deliberately permits signing and uses the app's **default private Keychain access group**; no Keychain-sharing entitlement is necessary. The simulator CI passes `CODE_SIGNING_ALLOWED=NO` only for unsigned test builds. A simulator `.app` is never an App Store `.ipa`. The current native project uses bundle ID `com.brendigo.qrex`, marketing version `0.2.1`, build `6` and iOS deployment target 16.0.

### Additional blocking gates

- [ ] Verify the *actual compiled* permissions, Android merged manifest, dependency/native-library 16 KiB page-size compatibility and current SDK privacy manifests. 
- [ ] Run real-device camera, flashlight, photo picker, sharing/export, dynamic-type, small/large display, background/foreground and storage-migration smoke tests.
- [ ] Capture genuine screens for all primary tabs and compare manually against the approved design. QR screenshot CI proving that the app launched is not a 1:1 visual comparison.
- [ ] Complete localized App Store metadata, privacy nutrition labels, age/content rating, support and contact URLs, TestFlight and Google Play release testing.
- [ ] Confirm the existing Flutter install can update **without data loss** on Android, and that the iOS bundle/provisioning matches any previous production distribution.
- [ ] Do not tag or publish native builds until every mandatory acceptance gate above is confirmed.

### Current platform references

- Google Play target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Apple upload builds: https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds/
- Apple app signing: https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases
- Apple private Keychain groups: https://developer.apple.com/documentation/security/sharing-access-to-keychain-items-among-a-collection-of-apps
