<div align="center">

<img src="assets/brand/qrex-logo.svg" alt="QREX — Scan. Create. Share." width="380">

### Scan. Create. Share.

**Your QR codes. Your device. Your choice.**

Scan, create and organize QR codes with an Android app built for simplicity and privacy.

**Free · Offline · No ads · No account**

[See the app](#see-qrex-in-action) · [Features](#everything-you-need) · [Privacy](#privacy-comes-first) · [Get QREX](#get-qrex)

</div>

---

## See QREX in action

These are **real Android emulator captures of the running QREX app**, generated and checked by CI using non-sensitive demonstration data. They are *not* AI illustrations or simulated device interfaces. Screenshots are attached to the matching GitHub release.

<div align="center">

<table>
<tr>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-splash.png" width="210" alt="Actual QREX dark splash screen"><br><b>Dark splash</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-create.png" width="210" alt="Actual QREX Create screen"><br><b>Create QR</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-scan.png" width="210" alt="Actual QREX scanner screen"><br><b>Scan QR</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-result.png" width="210" alt="Actual QREX QR result screen"><br><b>Review results</b></td>
</tr>
<tr>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-history.png" width="210" alt="Actual QREX History screen with demo records"><br><b>History</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-saved.png" width="210" alt="Actual QREX saved QR history filter"><br><b>Saved codes</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-more.png" width="210" alt="Actual QREX settings and export screen"><br><b>Settings</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-wifi.png" width="210" alt="Actual QREX Wi-Fi QR creation screen with demo SSID"><br><b>Wi-Fi QR</b></td>
</tr>
<tr>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-language.png" width="210" alt="Actual QREX Language settings with offline language selection"><br><b>Language</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-export.png" width="210" alt="Actual QREX PDF and ZIP Export settings screen"><br><b>Export</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-light-create.png" width="210" alt="Real light-mode QREX Create QR screen"><br><b>Light Create</b></td>
<td align="center"><img src="https://github.com/bren-wp/QREX/releases/download/v1.0.8/android-light-more.png" width="210" alt="Real light-mode QREX Settings screen"><br><b>Light More</b></td>
<td colspan="2"></td>
</tr>
</table>

[**Explore all twelve full-size screenshots →**](https://github.com/bren-wp/QREX/releases/tag/v1.0.8)

</div>

## Everything you need

| | Feature |
|:--|:--|
| 📷 **Scan from camera or photos** | Point your camera at a QR code or select an image from Android's system photo picker. |
| ✨ **Create QR codes** | Make QR codes for links, text, Wi-Fi, contact details, email, phone numbers, places and calendar events. |
| 🔎 **Review before opening** | Choose whether to open a scanned website. No automatic URL opening. |
| 🔖 **History & saved codes** | Browse, search, save and remove locally stored QR codes. |
| 🔐 **Protect Wi-Fi details** | Wi-Fi codes are never recorded automatically; manually saved Wi-Fi details use Android Keystore encryption. |
| 📄 **Save and export** | Export individual PNG images or your manually saved QR collection as PDF/ZIP. |
| 🌍 **23 languages** | Including English and Croatian, all without an online translation service. |
| 🌗 **Make it yours** | Light and dark appearances, accessible controls and responsive layouts. |

## Privacy comes first

QREX scans and generates QR codes **on your phone**, without a developer cloud account, advertising, analytics or a required Internet connection. Camera access is requested only for camera scanning; you can choose a gallery image without granting camera permission.

QREX asks before sharing Wi-Fi credentials. Saved Wi-Fi QR contents use AES-GCM backed by Android Keystore.

**Exported PDF and ZIP files are not encrypted.** Protect those files when they include private information.

[Privacy Policy](docs/PRIVACY.md) · [Terms of Use](docs/TERMS.md) · [Support](https://brendigo.com)

## Get QREX

[**Download the latest GitHub release →**](https://github.com/bren-wp/QREX/releases/latest)

[**All historical QREX releases (v1.0.0–v1.0.10) →**](docs/releases/INDEX.md)

Latest published release: **v1.0.10** (`com.brendigo.qrex`, versionCode 110). Next QA candidate: **v1.0.11** (`versionCode 111`) — scanner-first navigation, a unified QREX icon in the headers, cleaner live-camera controls and visible History/Favorites filters. v1.0.11 remains unpublished until its exact-commit CI gates pass. **The launcher and splash now use the new Q/QR reference mark. Dark navy remains the primary theme**, with an optional light mode that respects existing user settings. The release includes a signed standalone GitHub APK, a GitHub-signed AAB for analysis (**not a Google Play upload artifact**), twelve real Android screenshots and checksums.

**Important update warning:** GitHub APK versions use *different disposable signing certificates*. They **cannot update prior GitHub or Google Play installations in place**. Uninstalling may erase local history, favorites and Keystore-protected QR information; back up anything important first. Google Play updates require the original approved upload identity.

**Google Play:** [Signing and submission guidance](docs/PLAY_STORE_SUBMISSION.md). Do not upload the public GitHub AAB as a Play update.

Earlier versions [v1.0.0](https://github.com/bren-wp/QREX/releases/tag/v1.0.0), [v1.0.1](https://github.com/bren-wp/QREX/releases/tag/v1.0.1), [v1.0.2](https://github.com/bren-wp/QREX/releases/tag/v1.0.2), [v1.0.3](https://github.com/bren-wp/QREX/releases/tag/v1.0.3), [v1.0.4](https://github.com/bren-wp/QREX/releases/tag/v1.0.4) and [v1.0.5](https://github.com/bren-wp/QREX/releases/tag/v1.0.5) remain archived and downloadable.

---

<div align="center">

<img src="assets/brand/qrex-icon.svg" alt="QREX by Brendigo" width="85">

**QREX — Scan. Create. Share.**

Created by [Brendigo](https://brendigo.com) · [Open-source license](LICENSE)

[Developer documentation](platforms/README.md)

</div>
