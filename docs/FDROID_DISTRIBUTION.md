# QREX — official F-Droid and Brendigo F-Droid repository

Status (2026-10-21): **PREPARATION ONLY; not accepted by or released on F-Droid.**

## Verified against QREX main / v1.0.3
- Upstream: https://github.com/bren-wp/QREX — publicly accessible, MIT licensed.
- Package: `com.brendigo.qrex`; source version 1.0.3, versionCode 103.
- Android: `platforms/android` Kotlin/Compose, Gradle, CameraX.
- Historical main/v1.0.3 blocker: `com.google.mlkit:barcode-scanning:17.3.0` was proprietary. The preparation branch now removes it and implements a ZXing decoder; resolved dependency and device validation are pending.
- Historical v1.0.3 `QREXScanner.kt` used ML Kit for live camera and gallery; the preparation branch changes both paths to ZXing, with CI and device validation still required.
- ZXing core 3.5.3 is now also used for decoding in the preparation branch. The decoder includes CameraX Y-plane row/pixel stride processing, rotation, bitmap bounds, and inverted QR support.
- GitHub v1.0.0–v1.0.3 signed APKs use **different disposable signing keys**. They cannot provide an in-place update path.
- The provided Google Play AAB is not an F-Droid APK. The Play upload key is not the APK installation-signing identity.

## Phase A — official F-Droid, not yet submit-ready
1. **Source updated in draft PR #30:** ZXing/CameraX decoder replaces ML Kit for camera and gallery. **Not yet verified:** successful Gradle build, full regression/device behavior (EXIF, lifecycle, cancellation, permissions, torch and performance).
2. Add a `fdroid` product flavor that contains **no ML Kit, Play Services, Firebase, other non-FOSS libraries or binary blobs**. Audit **resolved transitive** runtime and build dependencies and all assets/licenses; merely grepping Gradle files is insufficient.
3. Test live camera (portrait/landscape, rotation, low light, camera pause/resume), gallery scans (EXIF rotations/mirroring), error/permission paths, 23 locales, history/Keystore migration, performance and accessibility on real devices. Tests are REQUIRED, not claimed complete.
4. Run CI `.github/workflows/fdroid-audit.yml`: Gradle build, lint, unit tests, resolved transitive release dependency audit and `fdroid scanner`/`fdroid lint`; also do instrumentation and physical-device tests. CI addition is not evidence the checks passed. Release builds need internet to resolve dependencies unless caches are prepopulated.
5. Add release tags for exact source commits, maintain Fastlane metadata and complete `distribution/official-fdroid/metadata/com.brendigo.qrex.yml.example` **only after** a successful build. Define accurate `subdir`, `gradle`, `commit`, `versionCode` and `versionName` fields based on that tested source/tag; don't submit an untested recipe.
6. Submit to https://gitlab.com/fdroid/fdroiddata as an MR; or file https://gitlab.com/fdroid/rfp/-/issues asking for packaging assistance. The F-Droid maintainers independently decide inclusion.

Reference: https://f-droid.org/docs/Inclusion_Policy/
Reference: https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/

## Phase B — separate Brendigo-operated F-Droid-compatible repository
This is a **third-party repo**; its rules differ from the official repo. Its distributed APK must be a properly licensed, installable APK; a Play `.aab` cannot be published as the installable package.

1. Create a public repository (suggested name `bren-wp/brendigo-fdroid`) and a GitHub Pages deployment for the public `repo/` directory. Do not assume the Pages URL works until deployment is verified.
2. **On a trusted private machine**, install fdroidserver, run `fdroid init`, save the repository signing keystore and recovery secrets offline. The repository signing key is **separate** from the Android APK signing key.
3. Create a **persistent, protected APK signing key** for the QREX direct-install channel. Sign every future Brendigo repo APK with the **same** certificate. Never use QREX historical throwaway GitHub build keys.
4. Assign the APK a new monotonically increasing `versionCode` (the source currently uses 103). Keep `applicationId` `com.brendigo.qrex` only if the ramifications of other signing keys are understood.
5. Put only the **signed APK** (never an AAB, JKS or secrets) into `repo/`. Add accurate package metadata, run `fdroid update --create-metadata`, then `fdroid update` and verify generated signed index and app info.
6. Publish the public `repo/` (plus public icons/metadata) over HTTPS; keep `config.yml`, `keystore.*` and all passwords **outside** the published directory and ideally outside the public git repo.
7. Test F-Droid client repository import, fingerprint, APK installation and **in-place upgrade with a newly signed version** on a clean test phone before sharing a QR subscription link.
8. The official F-Droid, Google Play and Brendigo APK channels may use **different signing certificates**. With the same package ID, users generally cannot switch channels in place; uninstalling may erase QREX history, favorites and protected Wi-Fi data. Do not suggest uninstalling without warning and export/backups.

Starter templates: `distribution/brendigo-fdroid/`.
Reference: https://f-droid.org/en/docs/Setup_an_F-Droid_App_Repo/

## Release/security gates
- Never upload or publish private .jks, .p12, key passwords, GitHub secrets or `config.yml`.
- Do not call either channel "published" until the public HTTPS index is live and the official F-Droid listing (if accepted) is visible.
- Report bundled proprietary ML Kit and any relevant anti-features transparently when using the **third-party** repository; these are not acceptable in mainline official F-Droid.
- Existing 1.0.3 GitHub release remains unchanged; this branch does not alter Google Play distribution or keys.
