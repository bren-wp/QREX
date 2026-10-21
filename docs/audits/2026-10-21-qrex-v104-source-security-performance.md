# QREX 1.0.4 — Source, dead-code, security, battery and distribution audit

**Baseline:** QREX main `5b811a9df51a395b5f4294d580124830067a9248` (GitHub v1.0.3 published).
**Scope:** seven production Kotlin modules (MainActivity, QREXCore, QREXScanner, QREXQrDecoder, QREXDocuments, QREXSecretCipher, QREXStrings), AndroidManifest, Gradle dependencies, 23 locale resources, test suites, Android/F-Droid/Play release workflows and public docs. Static review **cannot substitute** physical-device QA, Play Console credential verification, exhaustive legal license analysis or repeated production crash-rate observation.

## Code and dead-code review

| Finding | Result |
| --- | --- |
| ML Kit scanner source and dependency | Already **removed in main** before 1.0.4; shared open-source ZXing QR decoder is used for camera/gallery. No duplicate proprietary scanner pipeline retained. |
| Old `responsiveQrSideDp` | Already removed in v1.0.3; actual Compose `widthIn` layout remains in use. |
| Qt/Flutter/iOS source | Old Flutter runtime and iOS implementation already retired; Android historical Flutter **data migration must remain**, as must Keystore code. |
| Framework/Compose callbacks, import operator extensions | **Retained**; static text-only reference counting would incorrectly flag invoked-by-framework methods and Kotlin delegate operators. |
| PDF and ZIP export | **Retained**; user-visible actions in More call streaming `QREXDocuments`, QR bitmaps are recycled after drawing, ZIP member name is fixed and cannot incorporate a traversal path. |
| Old release workflows/docs | Source for historical immutable versions is **retained** for signature continuity, provenance and audit; outdated ML Kit manifest commentary and stale F-Droid/README claims are corrected. |
| Image rendering in Compose | Existing lazy QR bitmap generation and generation off the UI thread retained. No arbitrary recycling of displayed bitmaps added. |

No additional Kotlin declaration was proven unreachable with confidence sufficient for removal in this pass. Removal solely to increase dead-code counts would put real features/data at risk.

## Security and privacy review

- **Permissions:** app requests camera only, drops transitive INTERNET/NETWORK_STATE; no photo-browsing, contact, GPS or broad storage permission. Final *merged manifest* checks remain required.
- **Untrusted QR payloads:** URL opening is user initiated and constrained to HTTP(S) without whitespace, backslashes or userinfo; there is no assertion of malware scanning or 100% link safety.
- **Local data:** history persisted on device, manually saved Wi-Fi records AES-GCM encrypted with an Android Keystore key; corrupted/undecryptable history aborts overwrite rather than silently erasing old content. Historical Flutter migration is preserved.
- **Copy and share:** Wi-Fi payloads flagged sensitive for Android clipboard and require sharing confirmation. The OS/other apps may still retain copied data.
- **PDF/ZIP:** files are exported only to Android's user-selected document destination and are **not encrypted**; retain warnings. Fixed ZIP entry name and stream-based writing prevent attacker-controlled ZIP entry traversal or full-PDF byte buffering.
- **CI secrets:** public GitHub release uses a disposable temporary key, not private Play/F-Droid secrets. Protected `multi-store-release.yml` requires authorized secrets, verified fingerprints, physical-device approval and review; no private signing keys belong in source or public artifacts.
- **Release checks:** v1.0.4 assets include a genuinely **unsigned** AAB for Play upload signing and an **unsigned** FLOSS APK for F-Droid QA, alongside a separately disposable-signed GitHub APK/AAB. None is described as official Play/F-Droid accepted output.
- **Exposure:** Screenshot fixtures are accepted **only if the app is debuggable**; release builds ignore screenshot extras. Neutral demo History records are in memory and do not persist to SharedPreferences. CI validates foreground QREX package and rejects system ANR overlays.

## Battery/performance changes

Existing CameraX KEEP_ONLY_LATEST strategy drops queued camera frames while allowing the analyzer to keep running. **New:** a monotonic `cameraFrameDue` check performs ZXing decoding at most roughly once every 180 milliseconds (~5.6 Hz), closing all frames even when skipped. Gallery decode remains user triggered and unthrottled. A JVM test checks the timing boundaries. This is a measurable **work-frequency limit**, not a measured device-wide battery saving; measure power, CPU and heat on real Android phones before making numerical marketing claims.

## UI/UX, captures and test gates

QREX's actual Compose Scan/Create/Result/History/Saved-filter/More and Wi-Fi screen are captured on an emulator into **seven 1080 × 1920 PNG screenshots**. Debug-only demo records are non-sensitive and visibly marked as demonstration data in documentation; UI is drawn by the real running app, not an AI image. The screenshot workflow fails if any required screen, foreground package, PNG integrity/dimensions or ANR validation fails. The README links only those verified release assets.

Required main checks: Kotlin unit / instrumentation / lint / R8, Android 26/31/34/35/36 navigation/lifecycle stress matrix (PR), 23-language parity, screenshots and independent F-Droid source/dependency/metadata audit. Physical-device camera and Play in-place upgrade still require external verification.

## Release blockers outside GitHub

1. **Google Play:** inspect Play Console for existing versionCodes (104 may be unavailable), compare the registered **upload** certificate and sign the **unsigned** AAB through a protected workflow. The GitHub temporary certificate is incompatible with previous installations.
2. **Official F-Droid:** clean reproducible rebuild and comprehensive license review, physical-camera tests, then fdroiddata MR/RFP subject to maintainers' acceptance. Passing CI does not mean officially listed.
3. **Brendigo F-Droid repository:** establish HTTPS server/repo index and retain an **independent permanent** APK and repository-index signing key, kept offline or in protected secrets. No live subscription link exists in the source.
4. **QA:** physical Android device rotation, keyboard, TalkBack, split screen, low light, battery/power trace, corrupted history and in-place migration need manual real-device validation.

**Audit conclusion:** concrete camera CPU-load mitigation, release signing separation and real screenshot generation implemented; no claim of eliminating every possible defect or store acceptance.
