# QREX — complete Android release archive

Every entry below is an **existing, verified GitHub Release** with an immutable `v...` tag, its own APK/AAB files, and `SHA256SUMS.txt`. Earlier releases remain available; do **not** replace them with builds from another commit. See [all releases](https://github.com/bren-wp/QREX/releases).

| Release | Original release page | Available Android packages |
| --- | --- | --- |
| 1.0.0 | [v1.0.0](https://github.com/bren-wp/QREX/releases/tag/v1.0.0) | APK · AAB |
| 1.0.1 | [v1.0.1](https://github.com/bren-wp/QREX/releases/tag/v1.0.1) | APK · AAB |
| 1.0.2 | [v1.0.2](https://github.com/bren-wp/QREX/releases/tag/v1.0.2) | APK · AAB |
| 1.0.3 | [v1.0.3](https://github.com/bren-wp/QREX/releases/tag/v1.0.3) | APK · AAB |
| 1.0.4 | [v1.0.4](https://github.com/bren-wp/QREX/releases/tag/v1.0.4) | APK · AAB · Play-unsigned QA AAB · F-Droid-unsigned QA APK |
| 1.0.5 | [v1.0.5](https://github.com/bren-wp/QREX/releases/tag/v1.0.5) | APK · AAB · Play-unsigned QA AAB · actual Android screenshots |
| 1.0.6 | [v1.0.6](https://github.com/bren-wp/QREX/releases/tag/v1.0.6) | APK · AAB · Play-unsigned QA AAB · actual Android screenshots |
| 1.0.7 | [v1.0.7](https://github.com/bren-wp/QREX/releases/tag/v1.0.7) | APK · AAB · Play-unsigned QA AAB · 10 screenshots |
| 1.0.8 | [v1.0.8](https://github.com/bren-wp/QREX/releases/tag/v1.0.8) | APK · AAB · Play-unsigned QA AAB · 12 screenshots |
| 1.0.9 | [v1.0.9](https://github.com/bren-wp/QREX/releases/tag/v1.0.9) | APK · AAB · Play-unsigned QA AAB · 12 screenshots |
| 1.0.10 | [v1.0.10](https://github.com/bren-wp/QREX/releases/tag/v1.0.10) | APK · AAB · Play-unsigned QA AAB · 12 screenshots |

All eleven archive tags and all original APK/AAB assets above were checked through GitHub's release/tag API on 2026-10-23. Version `1.0.11` is **not** included until it is actually published.

## Preserve old app installations and signing identity

The historic GitHub APKs are *not guaranteed* to share the same signing certificate: CI used disposable signing identities in the later release workflows. Android rejects upgrades when the signature differs. **Never uninstall an existing QREX install simply to switch versions**, because local QR history, favorite entries and Keystore-backed Wi-Fi credentials may be lost. Use Play Console's authorized signing path for upgrades to existing Play installs. A signed inspection AAB from GitHub is not automatically valid for Play; the `Play-UNSIGNED-QA` bundles are unsigned and must not be uploaded without the authorized signing process. Likewise, the archived F-Droid QA APK does not prove F-Droid publication.

## Audit integrity

Choose the desired release, download its assets, verify hashes against **that release's** `SHA256SUMS.txt`, and inspect its source at the same tag. Do not mix APK/AAB or checksum files between versions.

This archive is a navigation index only and leaves every historical tag, release and binary untouched.
