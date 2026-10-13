# QREX — Android and iOS

Standalone Kotlin / Jetpack Compose and Swift / SwiftUI projects. QR scanning and creation are local-first, with no registration, ads or analytics.

| Feature | Android | iOS |
|---|---|---|
| Language and UI | Kotlin + Compose | Swift + SwiftUI |
| QR camera | CameraX + on-device ML Kit | AVFoundation |
| Photo QR | OS photo picker + ML Kit | PhotosPicker + Vision |
| QR generation | ZXing | Core Image |
| Protected history | SharedPreferences; Android Keystore AES-GCM for Wi-Fi values | Device-only Keychain |
| Account needed | No | No |
| Language support | 23 locales | 23 locales |

The last public v0.1.3 release still uses Flutter. The standalone projects require full device and visual verification before replacing it.

## Build

Android: open `platforms/android` in Android Studio using JDK 17 / SDK 36, or run `gradle :app:assembleDebug :app:testDebugUnitTest` inside that directory.

iOS: on macOS install XcodeGen, run `xcodegen generate --spec project.yml` under `platforms/ios`, and open the generated Xcode project. iPhone distribution requires valid Apple signing.

## Screenshot integrity

The [screenshot workflow](../.github/workflows/capture-screenshots.yml) starts both apps in real emulators/simulators. Android screenshots additionally require a UI hierarchy assertion that the QREX create screen is in the foreground. A valid PNG alone does not prove this: an earlier capture showed a system ANR dialog and was rejected during visual review.

## Release gates

- [x] Independent Android and iOS builds and simulator tests have passed on GitHub Actions.
- [x] Real iOS screen captured with English text.
- [ ] Android foreground capture passes and is visually inspected.
- [ ] Scan, Create, Result, History and More match the supplied design on real screen sizes.
- [ ] Verify physical camera, flash, permissions and photo picker on real devices.
- [ ] Complete QR type and image-export parity, migration testing and store signing.
- [ ] Deliver Android AAB and Apple-signed archive after passing release checks.

**Developed by Brendigo** · https://brendigo.com
