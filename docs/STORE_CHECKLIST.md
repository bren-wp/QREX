# QREX — Google Play 1.0.5 readiness checklist

**Package:** `com.brendigo.qrex` · **versionName:** `1.0.5` · **versionCode:** `105` · **target/compile SDK:** 36 · **minSdk:** 26.

## Repository and automated checks

- [x] Only native Kotlin / Jetpack Compose Android runtime; iOS and obsolete Flutter runtime removed.
- [x] Kept local upgrade migration from the earlier Flutter Android app; never treat as dead code.
- [x] Offline 23-language catalogs, source manifest minimum permissions, API 36.
- [ ] Android JUnit, instrumentation, lint, R8, bundle and screenshot CI all pass at the **exact final main commit**.
- [ ] Actual merged manifest and dependencies have been audited for accidental permissions, remote SDK traffic and third-party data sharing.
- [ ] Review and validate edge-to-edge, compact phones, tablets, foldables, TalkBack and 200% display fonts.
- [ ] Test gallery/permission/camera lifecycle, password storage, PDF/ZIP export and app data migration on physical Android devices.

## Google Play authorization

- [ ] Enable reviewer approval for the protected GitHub Actions `production` environment.
- [ ] Supply the **existing approved** Play upload keystore/alias/passwords securely as GitHub Secrets; never paste secrets into chat, documentation or source control.
- [ ] Compare `QREX_UPLOAD_CERT_SHA256` against Play Console's registered **upload** certificate.
- [ ] From `.github/workflows/production-release.yml`, choose the version set in Gradle and confirm `PLAY_UPLOAD_<version>` only after real QA and required Play declarations.
- [ ] Download the matching **separately, correctly signed** Google Play AAB artifact and verify `SHA256SUMS-PLAY.txt`.
- [ ] Confirm `versionCode 105` is unused in the existing Play application; if already used, raise it before submission.
- [ ] Complete Play Console privacy policy URL, Data safety, app access, ads, audience, content rating, listing and store screenshots.
- [ ] Verify 16-KiB page compatibility of native SDK libraries using actual final Play bundle and supported devices.
- [ ] Complete any applicable new-personal-account closed testing requirements.
- [ ] Upload, review pre-launch report, resolve policy warnings and receive Google Play approval.

## Historical signing caution

The [GitHub v1.0.0](https://github.com/bren-wp/QREX/releases/tag/v1.0.0) APK/AAB were signed with a disposable certificate. GitHub 1.0.1 packages likewise use an ephemeral key, **different from 1.0.0 and from the Play upload key**. Neither GitHub bundle is suitable as an update to an existing Play application, and direct APK replacement cannot maintain installation identity. Uninstalling may erase saved QR information.

For exact steps and official links, use [PLAY_STORE_SUBMISSION.md](PLAY_STORE_SUBMISSION.md). A successful source build is **not** evidence that Google has approved an app.

