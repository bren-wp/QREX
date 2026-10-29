# QREX 1.0.22 — Google Play submission guide

**Platform:** Android · **package:** `com.brendigo.qrex` · **versionName:** `1.0.22` · **versionCode:** `122` · **target/compile SDK:** 36 · **min SDK:** 26.

**This document describes a proposed QREX 1.0.22 Play upload, not a published or approved store release.** Google Play policy and Play Console eligibility must be reviewed again on the date of submission.

## Separate the two release channels

| Channel | Intended users | Signing key | Update continuity |
| --- | --- | --- | --- |
| GitHub `v1.0.0` | Direct download | Disposable CI certificate | Lost after original run; not compatible with ordinary updates |
| GitHub `v1.0.22` (after publication) | Direct-download distribution | Disposable CI certificate | Not compatible with earlier differently signed packages or Play installs |
| Google Play `1.0.22` (proposed) | Existing / new Play users | **Authorized pre-existing Play upload certificate** | Play App Signing enforces the existing app identity; compatible updates require unchanged Play package and valid upload certificate |

**Do not submit the public GitHub AAB** to Play Console or attempt to replace earlier release files. The Play bundle must be produced from `.github/workflows/production-release.yml` with the authorized upload-key secrets.

## Build the Play-upload-signed AAB

1. In repository Settings → Environments, require reviewer approval for `production`.
2. Provision GitHub Actions secrets **without exposing them in chat or commits**: `QREX_UPLOAD_KEYSTORE_BASE64`, `QREX_UPLOAD_STORE_PASSWORD`, `QREX_UPLOAD_KEY_ALIAS`, `QREX_UPLOAD_KEY_PASSWORD`, `QREX_UPLOAD_CERT_SHA256`.
3. From Play Console → App integrity / App signing, compare the **upload** certificate SHA-256 (not the separate Play app-signing certificate) with `QREX_UPLOAD_CERT_SHA256`.
4. Run and review three Android workflows at the exact latest `main` SHA: `native-platforms.yml`, `capture-screenshots.yml`, `generate-localizations.yml`.
5. Run the protected workflow `QREX authorized Google Play upload` on `main` with version `1.0.22`, confirmation `PLAY_UPLOAD_1.0.22`, real device-QA confirmation and the completed Play checklist. Never approve unperformed tests.
6. Download `QREX-1.0.22-Google-Play-Upload-Signed-AAB`. Confirm package ID, version 122, upload certificate and SHA-256; upload **that** AAB to the existing Google Play app and appropriate test track.
7. Verify actual Google Play processing, signing continuity, pre-launch report, supported devices, policy notices, test eligibility and store approval.

If the Play Console app has already used versionCode 122 or higher, **increment it again** before uploading. Version codes are never reusable within the same Play application.

## Google Play policy items to verify

- **Target API:** Android 16 / API 36 required for new apps and updates from 31 August 2026 (QREX targets 36). Official: https://support.google.com/googleplay/android-developer/answer/12226878
- **16 KB pages:** audit all native/transitive `.so` files in the actual finalized Play bundle and test on a 16 KB Android emulator or real device; Play Console's device compatibility analysis is authoritative. Official: https://developer.android.com/guide/practices/page-sizes
- **Permissions:** camera only, granted on an explicit tap. Use system photo picker without broad storage permissions. Inspect final **merged** manifest and third-party SDK manifests; source manifest checks alone are insufficient.
- **Data safety / privacy:** fill the Play Console questionnaire and publish a reachable public HTTPS policy. Check any SDK network transmission, analytics, logs, off-device user exports and Play user-data declarations. Source: https://support.google.com/googleplay/android-developer/answer/10787469
- **Content and store listing:** accurate app name, short/full description, screenshots captured from the running app in each listing locale, feature graphic, category, content rating, target audience, app access, ads declaration and support contact. No promises of unsupported features or promotional boards submitted as in-app screens.
- **Testing and access:** if using a personal developer account created after 13 November 2023, confirm whether the required closed test with at least 12 opted-in testers over 14 continuous days applies. Source: https://support.google.com/googleplay/android-developer/answer/14151465
- **Permissions and SDKs:** compare the actual dependency graph and Play SDK Index warnings; confirm minimal SDKs and no advertising identifiers. Recheck camera/photo path, PDFs and Android 16 edge-to-edge behavior.
- **User protections:** in-place upgrade/migration tests from the **actual Play-signed prior install**; on-device AES-GCM secrets, intentional WI-FI copy/share disclosure, no credentials in logs; unencrypted exports require user discretion.
- **Stability and accessibility:** real Pixel, Galaxy and low-memory device testing; landscape, tablets/foldables, 200% font, TalkBack, offline behavior and foreground/background transitions; no app launch crashes or restricted permission loops.

## Known blockers

- A successful unsigned or disposable-signed GitHub AAB **cannot** substitute for the authorized Play-upload-signed AAB.
- Play Console account access, signing certificates, real device tests, privacy declarations and review outcomes are external steps that have **not** been independently verified by repository source inspection.
- A published GitHub `v1.0.0` release is an immutable historical binary; its certificate cannot be changed retroactively without creating a different artifact/signing identity. The supported fix is a distinct Play-signed build/version and correct install/update path.

[Privacy policy](PRIVACY.md) · [QA matrix](QA_MATRIX.md) · [Version 1.0.22 notes](releases/v1.0.22.md)

