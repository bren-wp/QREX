# QREX — Scan. Create. Share.

## Android v1.0.0 · versionCode 100

A GitHub Android 1.0.0 release with both an **installable APK** and an **AAB**, plus SHA-256 checksums. The APK and AAB are built in release mode from the exact tested commit, signed with a disposable, generated certificate—not with the original Google Play upload key. **This is a GitHub distribution, not a Play-approved release.**

**Upgrade limitation:** an APK signed using this one-time GitHub key cannot update existing QREX installations signed using a different key. Do not uninstall an earlier QREX app without exporting or backing up important local data; uninstalling may erase saved QR information.

[Read complete release notes](docs/releases/v1.0.0.md) · [Download from GitHub Releases](https://github.com/bren-wp/QREX/releases) · [Privacy policy](docs/PRIVACY.md)

### Features

On-device camera/photo QR scanning, creation, favorites/history, protected Wi-Fi history, consent before sharing Wi-Fi credentials, PNG/PDF/ZIP export, adaptive Kotlin/Compose UI and 23 offline languages.

### Historical releases

Older previews were QA artifacts, not certified Play releases. Android-only development retains the Flutter-to-native migration implementation but successful in-place upgrades still require matching signing identities.

Made by [Brendigo](https://brendigo.com).
