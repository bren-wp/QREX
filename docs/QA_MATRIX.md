# QREX — Android Quality Assurance Matrix

This release checklist covers the **single Kotlin/Jetpack Compose Android application**. A green CI build or emulator screenshot is **not proof** of literal 1:1 reference-image parity or error-free behavior on every phone.

## UI, design and accessibility
- [x] Actual Android emulator Create screen captured and PNG integrity validated in CI.
- [x] Reject system UI/ANR screenshots without QREX foreground accessibility hierarchy.
- [ ] Compare Scan, Create, Result, History and More to approved references on multiple Android sizes.
- [ ] Verify compact 280–360 dp, common 393–430 dp, foldables, tablets, landscape and split screen.
- [ ] Check keyboard/IME, display cutouts, gesture navigation insets, font scale up to 200%, large controls and TalkBack.
- [ ] Check all 23 locales in both light and dark modes.

## Functionality and lifecycle
- [ ] Fresh install and upgrade from the earlier Flutter Android APK preserve history and saved flags.
- [ ] Explicit camera consent; gallery QR scanning works without CAMERA permission.
- [ ] Live camera scan, denied/granted permission, Settings recovery, background/resume, torch and camera interruptions.
- [ ] QR from gallery image, malformed/oversized photos, EXIF rotated/mirrored photos, repeated selections and memory pressure.
- [ ] URL/text/Wi-Fi/contact/email/phone/location QR creation and visual scanning of generated codes.
- [ ] Safe URL behavior, malformed QR, 1500-byte rendering bound and clipboard/share flows.
- [ ] Confirmation before sharing Wi-Fi QR credentials; cancellation does not open chooser.
- [ ] Local history search, saved favorites, remove/delete all, Keystore unavailable/corrupt data recovery.
- [ ] PNG, PDF and ZIP exports, cancellation, revoked picker permission and contents of exported records.

## Security and privacy
- [x] Check manifest for disabled backup and cleartext and reject INTERNET, unrestricted storage, location and contacts.
- [x] Verify Wi-Fi QR saved payload uses Android Keystore AES-GCM, never silently writes plaintext on failure (instrumented tests).
- [x] Source-level regression checks for opt-in camera, clipboard sensitive metadata and sampled image lifecycle.
- [ ] Recheck transitive SDK behavior, Play Data Safety, dependency advisories and 16-KiB native page compatibility.
- [ ] Verify no analytics/advertising SDK and no network permission in final merged manifest.
- [ ] Confirm no QR secrets in logs, exceptions, crash reports or Android auto-backup.

## Production
- [x] CI covers Kotlin compile, JVM unit tests, Android instrumentation, lint, R8 bundle and emulator Create capture.
- [ ] Test real devices, including small and large Android phone models and varied OEM implementations.
- [ ] Verify Android Play App Signing upload certificate lineage and manual production approvals.
- [ ] Run the exact-commit production workflow to publish **only signed AAB and checksums**.
- [ ] Check old releases/tags retired **after** verified signed v1.0.0 publication.

**Status:** Open acceptance checklist; nothing unchecked is reported as complete. No iOS or Flutter runtime is built.
