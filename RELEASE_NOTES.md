# QREX — Scan. Create. Share.

**Android only.** Native Kotlin / Jetpack Compose QR scanner and creator.

## Next signed production release: v1.0.0

**QREX 1.0.0 / build 100** is the Android candidate. Only an authorized Play-upload-signed Android App Bundle (AAB), accompanied by `SHA256SUMS.txt`, may be attached to a GitHub stable release. A successful CI build alone does not prove Play acceptance or proper signing. Before publishing, physical-device QA, upgrade testing from the older Flutter Android version and release approval remain mandatory.

[QREX 1.0.0 release notes](docs/releases/v1.0.0.md) · [Production readiness checklist](docs/STORE_CHECKLIST.md)

## Previous versions

Earlier Flutter Android and native RC builds remain available until signed `v1.0.0` genuinely replaces them. The signed production workflow then deletes the old GitHub Releases and Git tags. The Android migration code remains to preserve historical QR data; iOS and Flutter app sources are no longer maintained.

## Privacy and safety

QR scanning and creation run on the Android device. QREX does not require an account, ad SDK, analytics, or internet permission. Wi-Fi QR history requires explicit saving and uses Android Keystore encryption, with a confirmation before sharing sensitive network credentials. QR links are not opened automatically. User-exported PDFs/ZIPs are not encrypted; store them safely.

[Privacy](docs/PRIVACY.md) · [Terms](docs/TERMS.md) · [Releases](https://github.com/bren-wp/QREX/releases)

Made by [Brendigo](https://brendigo.com).
