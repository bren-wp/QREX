# QREX — Document Scanner and QR scanning guide

## Overview

QREX is an **offline Android** QR and document utility. CameraX and FLOSS ZXing decode QR codes without a cloud backend. The Document Scanner is accessible from the existing **Scan** tab, not a separate root tab.

## Scan a document

1. Open **Scan → Document scan**. Choose camera capture (CAMERA permission required), Android Photo Picker, or **Import PDF** through Android's system document picker (no broad media permission). PDF import is available from Camera and Pages.
2. Review the page. Choose **Crop** for four draggable perspective corners or rectangular crop. **Auto edges** makes a conservative, local suggestion and can decline if background contrast is insufficient; you can always adjust manually.
3. Apply crop or return to Review. Rotate by 90 degrees, enhance with **Auto enhance** (adaptive contrast), grayscale or high-contrast black/white, and use **Undo** for the immediately preceding bitmap edit. Undo does not preserve a limitless history.
4. Accept a page. Repeat for up to **eight** pages. Re-edit, move up/down or remove pages from the Pages view.
5. Select **Export PDF**, choose a destination through Android's system document picker, and explicitly save. The PDF retains portrait/landscape A4 orientation based on each page. On the Pages screen, **Export PNG** saves an individual page using Android's file picker.

### Memory, privacy and limitations

- The editor bounds page bitmaps to **1,800,000 pixels** and processes photographs locally. JPEG EXIF orientation, including mirrored orientations, is respected.
- PDF import renders selected pages locally using Android PdfRenderer. It rejects invalid, encrypted, >48 MiB or over-capacity PDFs without overwriting existing pages. Up to eight imported/captured pages may be combined in one session; the original PDF is never silently modified.
- DocuScan's adapted auto-level histogram algorithm uses one bounded output bitmap. It does not introduce OpenCV, internet access, server uploads, a new theme or a second camera. See [third-party license](THIRD_PARTY_DOCUSCAN.md).
- Temporary document bitmaps live **in memory**; closing the session discards them. A process crash/death loses the session. An explicit PDF export is written by Android's document provider and **is not encrypted**.
- Closing the editor while CameraX capture finishes must not retain or show the orphaned frame. The app uses a main-thread ownership handoff to recycle orphaned capture bitmaps and revision guards against stale crop/filter/rotation results.
- External document providers may reject a save or impose storage limits. Keep the destination selected in the Android picker; QREX does not automatically synchronize copies.

## Scan or create a QR code

- Live camera: grant CAMERA permission, hold the QR code in the preview bracket, and review the decoded result before opening links. Flash and zoom depend on device hardware.
- Gallery: open the system Photo Picker; QREX decodes QR images in memory, applies JPEG EXIF orientation and tries both standard and white-on-black QR, plus a centered fallback for large photos.
- Creation: choose URL, text, Wi-Fi, contact, email, telephone, location or event. You may generate and export an individual PNG.
- Privacy: history/favorites are encrypted locally with Android Keystore. Wi-Fi secrets require user approval for sensitive actions; there are no ads or user accounts.

## Real UI and release verification

The [README](../README.md) embeds **12 genuine main-screen emulator captures** for the latest already published release. When [v1.0.51](https://github.com/bren-wp/QREX/releases/tag/v1.0.51) is verified and published, it will also host **8 real editor screenshots** (dark/light Review, Crop, Pages and Export) from the same exact-commit screenshot workflow. Their paper content is synthetic debug fixture content; the UI pixels are from the running app.

The GitHub publisher must verify APK/AAB signatures, SHA256 checksums, real image files, Kotlin/R8/Lint, 23-language localization and the API 26/31/34/35/36 emulator matrix. An existing Google Play installation cannot be upgraded with the **disposable GitHub signing key**.

**Visual limits:** 1080 × 1920 screenshots, responsive emulators and instrumented tests are not a mathematical proof of 1:1 parity with perspective-framed marketing art or proof of physical-camera functionality on all OEM devices. Exact comparison requires original flat PNGs with matching device dimensions and font scale.

## Troubleshooting

- **No camera:** grant permission or use Photo Picker; restarting the app is unnecessary after returning from Android permissions/settings.
- **Auto edges not found:** manually place all four corners in good lighting.
- **QR from gallery not found:** crop tightly around the QR code in the gallery app, avoid glare and test adequate image resolution. Scanned content is never opened automatically.
- **PDF import failed:** confirm that the PDF is readable, not password-protected and has no more pages than the remaining eight-page session limit. Files over 48 MiB are rejected.
- **PDF save failed:** check the destination app/provider and available space; retry the Android save dialog.
- **Lost a session:** unsaved document bitmaps cannot be recovered after process termination; export when finished.

See [Privacy Policy](PRIVACY.md), [Google Play submission](PLAY_STORE_SUBMISSION.md) and [release history](releases/INDEX.md).
