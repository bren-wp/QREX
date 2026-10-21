# QREX — official F-Droid and Brendigo F-Droid repository

Status (2026-10-21): **PREPARATION ONLY; not accepted by or released on F-Droid.**

## Source and CI status for planned v1.0.4

- QREX is MIT-licensed, Android-only and uses `com.brendigo.qrex`. The source on `main` **already uses CameraX + ZXing** for both live and gallery QR detection, and no longer includes Google ML Kit in the declared runtime dependencies.
- The independent F-Droid readiness workflow `.github/workflows/fdroid-audit.yml` has **passed on main at SHA `5b811a9`**: it ran the F-Droid source scan, Kotlin unit/lint/release build, resolved release dependency denylist, `fdroid lint` and `fdroid scanner`. These are concrete CI checks, not a claim of full FLOSS legal review, reproducibility or physical-device QA.
- The new `v1.0.4` source is being separately tested and will require its **own** green CI and release-tag verification before any official packaging recipe is submitted. Existing signed GitHub versions use unrelated one-time certificates and are not compatible in-place upgrades.
- The source currently does not need an artificial `fdroid` flavor merely to exclude ML Kit, because the **default build has no ML Kit**. A flavor is warranted only if a future Play-only proprietary SDK is introduced. Re-audit both flavors if one is added.

## Official F-Droid submission checklist

1. Finish QREX v1.0.4 CI on the immutable tag and verify on real phones: low-light/rotated CameraX scanning, gallery EXIF orientation, lifecycle, torch/cancellation, 23 locales, Keystore/Flutter migration and battery measurements. Do not mark unperformed device tests as passed.
2. Reproduce Gradle release output using a clean checkout of the published tag. Inspect the entire resolved transitive build/runtime graph, licenses, binaries and non-free components, not only a denylist.
3. Run `fdroid scanner --exit-code` and `fdroid lint` on the final candidate metadata and verified tag. Use `distribution/official-fdroid/metadata/com.brendigo.qrex.yml.example` as a *proposal*, not as proof of inclusion.
4. Submit an independently verified app proposal via [fdroiddata](https://gitlab.com/fdroid/fdroiddata) merge request or [F-Droid RFP](https://gitlab.com/fdroid/rfp/-/issues). Official approval, indexing and scheduling are controlled by the F-Droid maintainers.
5. Publish a listing link **only after** acceptance and public listing verification.

Relevant references: https://f-droid.org/docs/Inclusion_Policy/ and https://fdroid.gitlab.io/jekyll-fdroid/docs/Submitting_to_F-Droid_Quick_Start_Guide/

## Phase B — separate Brendigo-operated F-Droid-compatible repository
This is a **third-party repo**; its rules differ from the official repo. Its distributed APK must be a properly licensed, installable APK; a Play `.aab` cannot be published as the installable package.

1. Create a public repository (suggested name `bren-wp/brendigo-fdroid`) and a GitHub Pages deployment for the public `repo/` directory. Do not assume the Pages URL works until deployment is verified.
2. **On a trusted private machine**, install fdroidserver, run `fdroid init`, save the repository signing keystore and recovery secrets offline. The repository signing key is **separate** from the Android APK signing key.
3. Create a **persistent, protected APK signing key** for the QREX direct-install channel. Sign every future Brendigo repo APK with the **same** certificate. Never use QREX historical throwaway GitHub build keys.
4. Assign the APK a new monotonically increasing `versionCode` (the planned source release uses 104). Keep `applicationId` `com.brendigo.qrex` only if the ramifications of other signing keys are understood.
5. Put only the **signed APK** (never an AAB, JKS or secrets) into `repo/`. Add accurate package metadata, run `fdroid update --create-metadata`, then `fdroid update` and verify generated signed index and app info.
6. Publish the public `repo/` (plus public icons/metadata) over HTTPS; keep `config.yml`, `keystore.*` and all passwords **outside** the published directory and ideally outside the public git repo.
7. Test F-Droid client repository import, fingerprint, APK installation and **in-place upgrade with a newly signed version** on a clean test phone before sharing a QR subscription link.
8. The official F-Droid, Google Play and Brendigo APK channels may use **different signing certificates**. With the same package ID, users generally cannot switch channels in place; uninstalling may erase QREX history, favorites and protected Wi-Fi data. Do not suggest uninstalling without warning and export/backups.

Starter templates: `distribution/brendigo-fdroid/`.
Reference: https://f-droid.org/en/docs/Setup_an_F-Droid_App_Repo/

## Release/security gates
- Never upload or publish private .jks, .p12, key passwords, GitHub secrets or `config.yml`.
- Do not call either channel "published" until the public HTTPS index is live and the official F-Droid listing (if accepted) is visible.
- Recheck all runtime/build dependencies and any relevant anti-features before each distribution. The current ZXing source no longer requires ML Kit.
- Existing GitHub v1.0.0–v1.0.3 releases remain unchanged; this work does not create or modify private Play/F-Droid signing keys.
