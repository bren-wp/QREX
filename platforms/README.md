# QREX — native Android

The sole actively supported application lives in [android](android/) and uses Kotlin, Jetpack Compose, CameraX, ML Kit, ZXing and Android Keystore.

The iOS/SwiftUI and Flutter application runtimes, generated hosts, store tooling and tests were intentionally deleted. Migration support for the earlier Flutter Android history/preferences is part of `QREXCore.kt` and must be preserved.

## Build and verify

Use Java 17, SDK API 36 and Gradle 8.13.

```bash
python3 tool/generate_localizations.py --check
python3 tool/check_translation_usage.py
python3 -m unittest discover -s tool -p 'test_*.py'
cd platforms/android
gradle :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
gradle :app:lintDebug :app:assembleRelease :app:bundleRelease
```

Android instrumented storage/camera checks and emulator screenshot capture run in CI. Debug/unsigned artifacts are for verification only. The protected manual production release signs the Android AAB with the existing Play upload key after physical-device and store approval. No iOS build pipeline or IPA distribution exists.
