# QREX — Android 1.0.0 production readiness

**Android application ID:** `com.brendigo.qrex`  
**Current production candidate:** version **1.0.0** / build number **100**  
**Build metadata:** marketing version `1.0.0`, build `100`  
**Distribution:** Google Play Android App Bundle (AAB), **existing, approved upload key only**

The discontinued iOS and Flutter application runtimes, Apple provisioning/signing and dual-platform CI were deleted. **Flutter Android-to-Kotlin migration logic remains**, to preserve user data during upgrades. Do not remove it in dead-code cleanups.

## Source and CI checks

- [x] One native Kotlin/Jetpack Compose Android runtime at `platforms/android`.
- [x] Android SDK compile/target API 36 and JDK 17.
- [x] 23 offline languages sourced from `l10n/catalog.json`; Android resource parity checks.
- [ ] Exact final commit: `native-platforms.yml`, `capture-screenshots.yml` and `generate-localizations.yml` all completed successfully.
- [ ] Gradle Kotlin compile, JVM tests, device instrumentation, lintDebug, R8 assembleRelease and bundleRelease.
- [ ] Real Android screen captures and physical visual comparison to reference UI across sizes.
- [ ] Tests for critical QR paths, denied permissions, migration, sensitive clipboard, Wi-Fi confirmation and low-memory conditions.

## Privacy and Play requirements

- [x] No developer backend, account, advertising, analytics or internet permission in the app source.
- [x] Android Keystore AES-GCM for manually saved Wi-Fi QR payloads and nonautomatic Wi-Fi history.
- [x] Camera permission only when requested by user, Android system photo picker without broad storage permissions.
- [ ] Validate actual merged manifest, transitive SDK privacy behavior and Play Data Safety declarations.
- [ ] Accessibility, TalkBack, dynamic font sizing, short/long localized strings and OEM testing.
- [ ] Validate 16-KiB native page compatibility and target Play SDK policy at actual submission date.
- [ ] Confirm retention/data deletion and exported unencrypted PDF/ZIP warning.

## Protected, production-only release

1. Protect the GitHub Actions `production` environment with required reviewers.
2. Configure the **previously approved** signing identity and secrets: `QREX_UPLOAD_KEYSTORE_BASE64`, `QREX_UPLOAD_STORE_PASSWORD`, `QREX_UPLOAD_KEY_ALIAS`, `QREX_UPLOAD_KEY_PASSWORD`, and `QREX_UPLOAD_CERT_SHA256`.
3. Match the expected SHA-256 fingerprint of the authorized Play Console upload certificate; never invent or rotate identities casually.
4. On the exact `main` commit, require every mandatory CI workflow to have succeeded.
5. Complete physical Android device QA, in-place Flutter-to-Kotlin migration testing and store disclosures. Explicitly authorize the manual `.github/workflows/production-release.yml` only after those are complete.
6. Verify the workflow generated `QREX-Android-1.0.0.aab`, signed with the correct certificate, and `SHA256SUMS.txt`. An unsigned AAB or debug APK is **not** stable. An AAB is not an installable APK.
7. Only **after** signed stable v1.0.0 publication, `tool/retire_legacy_releases.sh` deletes all older Releases and Git tags, retaining the approved `v1.0.0` pair. Manual recovery workflow is also production-protected.

- [ ] Approved signed AAB actually published.
- [ ] SHA256 file matches actual binary and release metadata.
- [ ] Only the `v1.0.0` Release and tag remain after cleanup.
- [ ] Internal Google Play testing, signing continuity, migration and Play acceptance validated.

**Important:** CI success is not store approval. Never publish a signed distribution without the required authorized signing key, real-device QA and consent.
