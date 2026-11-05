# QREX — native Android

The actively supported QREX application is in [android](android/). It uses
**Kotlin, Jetpack Compose, CameraX, ZXing and Android Keystore**. Scanning and QR
creation run on-device; **Google ML Kit and Google Play Services are not used
for scanning**.

The retired Flutter and iOS application runtimes were removed. Keep the
Flutter-to-native Android history and preferences migration in `QREXCore.kt`:
removing it could cause data loss for existing users.

## Android build prerequisites

- **JDK 17** and **Android SDK 36** (Build Tools 36.0.0).
- **Gradle 9.1.0**, matching the GitHub Actions setup. There is no checked-in
  Gradle wrapper; install Gradle 9.1.0 or use the CI environment.
- **Android Gradle Plugin 9.0.1**, **Kotlin Compose plugin 2.2.10** and
  the dependency versions declared in the Gradle build files.
- Android `applicationId` **`com.brendigo.qrex`**, `minSdk 26`,
  `compileSdk 36`, `targetSdk 36`.

From the repository root:

```bash
python3 tool/generate_localizations.py --check
python3 tool/check_translation_usage.py
python3 -m unittest discover -s tool -p 'test_*.py'

gradle -p platforms/android --no-daemon \
  :app:testDebugUnitTest :app:lintDebug \
  :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:assembleRelease :app:bundleRelease
```

Android emulator/instrumented camera, storage and screenshot checks run in the
GitHub Actions workflows. They are separate from the JVM tests and should not
be treated as passed until their runs succeed.

## Privacy, distribution and upgrades

The Android manifest does not request Internet access. CameraX + ZXing perform
QR decoding locally, the gallery uses the system photo picker and Wi-Fi QR
history uses an Android Keystore-backed AES-GCM key. An exported PDF is **not
encrypted**. See [Privacy](../docs/PRIVACY.md).

The automated GitHub APK and AAB use an **ephemeral signing certificate**,
not the Google Play upload key. They cannot replace an installation signed
with a different certificate, including prior GitHub builds; uninstalling can
erase local QR history and saved data. The QA unsigned Play AAB is **not**
suitable for a store upload. Production AAB signing requires the existing
approved upload key through a protected environment, never a committed secret.
See [Play submission](../docs/PLAY_STORE_SUBMISSION.md).

ZXing and CameraX replace proprietary scanning SDKs. **Official F-Droid
acceptance is not yet verified**: review all direct/transitive dependencies,
licenses, metadata, and reproducible build requirements before submitting.
Do not represent a GitHub release as a verified F-Droid package.
