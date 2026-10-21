<div align="center">

<img src="assets/brand/qrex-logo.svg" alt="QREX — Scan. Create. Share." width="440">

### Scan. Create. Share.

**Your QR codes. Your device. Your choice.**

A clean, capable QR scanner and creator designed exclusively for **Android**.

[Explore features](#features) · [See the app](#preview) · [Privacy](#privacy-by-design) · [Releases](https://github.com/bren-wp/QREX/releases)

<img src="assets/brand/qrex-icon.svg" alt="QREX Android icon" width="86">

**Free to use · No accounts · No ads · No tracking**

</div>

---

## A simple way to do more with QR

Scan a code, create one of your own, or keep the information you actually need. QREX makes common tasks easy without moving your personal QR content to a QREX server.

<img src="assets/previews/qrex-hero.svg" alt="QREX Android visual identity — Scan. Create. Share.">

## Preview

<div align="center">

<img src="docs/screenshots/android-create.png" alt="Actual Android emulator screenshot of the QREX Create screen" width="290">

<sub>Actual Android emulator capture, verified by CI. Device appearance and layout can vary. Brand visuals below are design illustrations, not app screenshots.</sub>

<img src="assets/previews/qrex-showcase.svg" alt="QREX design showcase with six conceptual app screens">

</div>

## Features

| | What QREX does |
|:--|:--|
| 📷 **Scan** | Read QR codes with the camera or choose a photo from your gallery. |
| ✨ **Create** | Make QR codes for links, text, Wi-Fi access, contacts, email, phone numbers and locations. |
| 🔗 **Review before opening** | Check the scanned destination before deciding whether to open it. |
| 🔖 **History & favorites** | Keep, find and delete locally stored codes on your own terms. |
| 🛡️ **Private Wi-Fi codes** | Encrypt manually saved Wi-Fi QR contents using Android Keystore; ask before sharing Wi-Fi QR credentials. |
| 📄 **Export** | Export QR images as PNG and manually saved QR codes as PDF or ZIP using Android's file picker. |
| 🌐 **23 languages** | Choose English, Croatian or another supported interface language. |
| 🌗 **Comfortable UI** | Light/dark appearance and layouts designed to adapt to different Android display sizes. |

## Privacy by design

QREX scans and generates codes locally. **No account, advertising SDK, analytics SDK, or internet permission is required.** Camera permission is requested only when you choose to enable live camera scanning. Gallery selection uses Android's system photo picker instead of broad media access.

Wi-Fi QR codes are not added to history automatically. If you explicitly save one, its contents are protected using AES-GCM with Android Keystore. When copying a Wi-Fi QR code, Android receives a *sensitive clipboard* flag; sharing a Wi-Fi code asks for confirmation.

**Important:** exported PDF/ZIP files are *not encrypted*. Treat exports that contain private information or Wi-Fi credentials as sensitive. The destination app or file provider you select may keep copies.

[Read the Privacy Policy](docs/PRIVACY.md) · [Terms of Use](docs/TERMS.md)

## Languages

QREX includes 23 offline interface languages:

English · Hrvatski · Deutsch · Français · Español · Italiano · Português · Nederlands · Polski · Čeština · Slovenčina · Slovenščina · Magyar · Română · Български · Ελληνικά · Türkçe · Українська · Русский · Svenska · Dansk · Suomi · Norsk bokmål.

No download or online translation service is needed to switch languages.

## Built for Android

| | |
|:--|:--|
| **Platform** | Android only |
| **Application ID** | `com.brendigo.qrex` |
| **UI** | Kotlin + Jetpack Compose |
| **Scanning** | CameraX + on-device ML Kit |
| **QR generation** | ZXing |
| **Private storage** | Android Keystore / AES-GCM |
| **Android SDK** | Compile/target API 36 |
| **Latest GitHub release** | Version `1.0.3` · versionCode `103` (not a Play release) |

The former iOS and Flutter app source trees have been retired. The Android application **retains migration support** for users upgrading from the earlier Flutter-based Android version.

### Build from source

Requirements: JDK 17, Android SDK API 36 and Gradle 8.13.

```bash
python3 tool/generate_localizations.py --check
python3 tool/check_translation_usage.py
python3 -m unittest discover -s tool -p 'test_*.py'

cd platforms/android
gradle --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

GitHub Actions also exercises Android instrumentation tests, minified release build/AAB generation and a genuine Android emulator screenshot.

## F-Droid distribution (preparation)

QREX is **not yet available** in the official F-Droid repository. The current Android scanner depends on Google's non-free ML Kit, which must be replaced or isolated in a Play-only build before the official F-Droid project can accept it. An independent Brendigo F-Droid-compatible repository is also being prepared; **there is no live subscription link yet**.

[Official F-Droid eligibility and independent repo plan](docs/FDROID_DISTRIBUTION.md) · [Brendigo repository starter](distribution/brendigo-fdroid/README.md)

## Download QREX 1.0.3 for Android

[**GitHub Releases — APK, AAB and SHA-256 checksums →**](https://github.com/bren-wp/QREX/releases)

| File | Purpose |
| --- | --- |
| **`QREX-Android-1.0.3.apk`** | Standalone Android release APK for GitHub distribution |
| **`QREX-Android-1.0.3.aab`** | Android App Bundle; not directly installable and not Play-upload signed |
| **`SHA256SUMS.txt`** | Checksums to verify release downloads |

QREX 1.0.3 improves UI/UX, camera flashlight feedback, history legibility and accessibility, and adds further Android lifecycle crash regression coverage.

**Signing warning:** GitHub generates a new temporary certificate for each version. This 1.0.3 APK **cannot update any existing 1.0.2, 1.0.1, 1.0.0 or Google Play installation in place.** Uninstalling QREX can irreversibly delete local history, favorites and Wi-Fi QR data. Safeguard all needed records before choosing a different-signed installation. Earlier [v1.0.0](https://github.com/bren-wp/QREX/releases/tag/v1.0.0), [v1.0.1](https://github.com/bren-wp/QREX/releases/tag/v1.0.1) and [v1.0.2](https://github.com/bren-wp/QREX/releases/tag/v1.0.2) remain preserved.

A real Google Play update requires separate checks for available versionCode and the existing approved upload certificate; this GitHub-signed AAB **must not** be uploaded to Play Console. The historical manual 1.0.1 Play workflow is not a Play 1.0.3 publisher. [Google Play guide](docs/PLAY_STORE_SUBMISSION.md).

Automatic GitHub publishing requires the exact source commit to pass native Android CI, screenshot and localization checks. [QREX 1.0.3 release notes](docs/releases/v1.0.3.md) · [v1.0.3 GitHub release](https://github.com/bren-wp/QREX/releases/tag/v1.0.3).

[All releases](https://github.com/bren-wp/QREX/releases) · [Android QA checklist](docs/STORE_CHECKLIST.md) · [Support](https://brendigo.com)

---

<div align="center">

<img src="assets/brand/qrex-logo.svg" alt="QREX by Brendigo" width="245">

**Scan. Create. Share.**

Created by [Brendigo](https://brendigo.com) · [Support](https://brendigo.com) · [MIT License](LICENSE)

</div>
