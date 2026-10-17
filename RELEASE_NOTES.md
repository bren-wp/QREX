# QREX — Scan. Create. Share.

Scan, create, organize and export QR codes with a private and easy-to-use experience on Android and iPhone.

## Latest published native QA preview — v0.2.3 RC1

The [QREX 0.2.3 RC1 release](https://github.com/bren-wp/QREX/releases/tag/v0.2.3-rc.1) contains a debug-signed Android APK, an unsigned Android AAB, an unsigned iOS device archive, actual Android and iPhone simulator screenshots and SHA-256 checksums. It is a **testing pre-release**; it is not signed for Google Play or the App Store.

## Next testing candidate — v0.2.4 RC1 (not yet published)

Android and iPhone gallery scanners now decode sampled images instead of allocating full-resolution photos in memory. Both platforms have regression checks for image-size limits. Version **0.2.4 / build 9** is reserved for this candidate because build 8 was already used by v0.2.3 RC1. No preview or production release is considered approved until exact-SHA CI, screenshot checks and required device testing are reviewed.

## Earlier Android edition — v0.1.3

The earlier Flutter-based APK remains available for existing users. [View v0.1.3](https://github.com/bren-wp/QREX/releases/tag/v0.1.3).

## Designed for privacy

QREX does not require an account and processes QR codes on the device. Scanning does not open an external link automatically. A user-exported PDF/ZIP archive can contain private QR content (including Wi-Fi passwords), and **is not encrypted**.

[Privacy Policy](docs/PRIVACY.md) · [Terms of Use](docs/TERMS.md) · [All QREX releases](https://github.com/bren-wp/QREX/releases)

Made by [Brendigo](https://brendigo.com).
