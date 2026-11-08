# QREX v1.0.42 — Source-level security, dead-code and dependency audit

**Audit date:** 2026-11-08  
**Audited branch:** release/qrex-v1.0.42-result-ci  
**Release target:** com.brendigo.qrex, versionName 1.0.42, versionCode 142  
**Status:** Source review and hardening committed; **production certification is conditional on all four exact-commit workflows passing**. This report is not a penetration-test certificate and does not claim zero vulnerabilities.

## 1. Audit coverage, verification boundaries

Reviewed all ten first-party production Kotlin files (about 5,388 lines) under platforms/android/app/src/main/java/com/brendigo/qrex, application and merged-manifest rules, app/root Gradle definitions, Android CI/release automation, release-signing policy, history migration, privacy copy/export consent flows, local security storage, camera/gallery image processing, PDF and PNG exports, URL dispatch and localization. The repositories also include numerous unit, instrumented and Python source-contract tests, checked by existing CI. All five Android emulator targets (API 26, 31, 34, 35 and 36) remain mandatory.

New first-party audit tool: tool/audit_android_security.py, called by both native CI and the new v1.0.42 publisher. Test suite: tool/test_security_audit.py. This checks source-level invariants, not independently audited binaries, unknown transitive CVEs, proprietary OEM behavior, live camera hardware, or external penetration tests. False positives and unobserved reflection are possible.

## 2. Source security findings and remediation

| Priority | Area | Verified source observation | Resolution |
| --- | --- | --- | --- |
| High defense in depth | External URL opening | QRContent.safeUrl only accepts HTTP(S), requires hostname and blocks userinfo/control characters. Previous openLink trusted callers to use this validator. | The external ACTION_VIEW dispatch now independently calls QRContent.safeUrl(url.toString()); non-HTTP(S), userinfo and malformed destinations are refused. |
| Medium | Local Wi-Fi encrypted record parsing | Saved Wi-Fi history uses non-exportable Android Keystore AES-256-GCM, 12-byte IV and 128-bit authentication tag. A corrupt stored Base64 value previously had no explicit size ceiling. | Limit UTF-8 plaintext to 48 KB and stored ciphertext envelope to 65,536 characters; reject oversized decoded ciphertext before decryption. Existing normal QR values remain supported. |
| Medium | Supply-chain aging | CameraX, Activity Compose, Lifecycle, ZXing, AndroidX instrumentation and the Android build toolchain lagged stable releases. | Upgrade stable direct dependencies and enable weekly Dependabot reviews for Gradle and GitHub Actions. Require test matrix and APK/AAB signing/integrity checks. |
| Defense in depth | Gallery import | Uses scoped URI streams, image dimension/pixel bounds and bitmap recycling, with local ZXing decoding. | Preserve bounds, and maintain direct stable ExifInterface dependency (1.4.2). |
| Existing protection | Secrets at rest | Saved Wi-Fi records use AndroidKeyStore-backed authenticated encryption; failed decryption does not rewrite unreadable history. | Retain fail-closed history migration/Keystore semantics; never export plaintext credentials without consent. |
| Existing protection | Permissions and OS surface | Source manifest requests CAMERA only, forbids app INTERNET/network-state via merge rules; no requested broad storage, location or ad-ID. Only launcher MainActivity is exported. Backups and cleartext traffic are disabled. | New audit gate plus existing merged debug/release-manifest check fail if transitive SDKs add prohibited permissions or expose components. |
| Existing protection | Share/copy/export consent | Wi-Fi QR copy, share, favorites save and PNG export require explicit confirmation; PDF/PNG exports use Android Storage Access Framework picker destinations. | Preserve test coverage and never log private payloads. |
| Existing protection | Release integrity | CI requires all four green workflows from exact merged main commit, APK/AAB cert match, ZIP alignment, aapt version/build ID, real 12 captures, SHA256 checksums, and Google Play unsigned QA bundle separation. | Retain fail-closed publisher. Previous v1.0.41 publisher is archived and disabled. |

**Residual concerns:** GitHub releases use disposable CI signing keys and cannot update an existing differently signed installation. Neither an unsigned QA AAB nor the GitHub AAB should be uploaded as a Google Play production update. Gradle/AndroidX resolved dependency graphs and known CVEs require ongoing vendor/advisory monitoring; a complete remote Dependabot alert listing was not available from the current connector. Test on physical devices before asserting camera hardware compatibility, no crashes, or 0% screen-pixel difference.

## 3. Dead-code audit and performance checks

First-party Kotlin source inspection included declaration/reference analysis across all ten production files (about 5,388 lines). There were **zero obvious one-use function declarations** in the production-source lexical scan (framework lifecycle callbacks and overridden analyzer functions were appropriately exempted). Local composable callback references, reflection, generated code and framework dispatch cannot be fully proven reachable or unreachable by simple lexical references. Therefore **no potentially live function was deleted** solely to reduce line count. The new audit script produces a review-only list of unreferenced candidates, including tests, without automatically modifying source.

The release build retains Android R8 optimization, minification and resource shrinking; real unused bytecode/resource elimination is checked by the release variant build. Maintaining the existing tests for QR camera frame disposal, UTF-8 input bounds, image sampling, 30-cycle Create/Result navigation, privacy and Storage Access Framework is more important than blindly removing apparently unused code.

Historical versioned publishers under docs/releases/workflows are intentional immutable release records, **not runnable active CI workflows**; do not delete these archived release recipes as dead code. The only enabled version-specific publisher is github-release-1.0.42.yml.

## 4. Dependency modernization matrix

| Dependency | Previous | Proposed stable release |
| --- | --- | --- |
| Android Gradle Plugin | 9.0.1 | 9.5.0 |
| Gradle running in CI | 9.1.0 | 9.9.0 |
| Kotlin Compose compiler plugin | 2.2.10 | 2.4.21 |
| Android compile SDK | 36 | 37 (Android SDK package platforms;android-37.0) |
| Android target SDK | 36 | 36 (preserved published behavior) |
| Compose BOM | 2026.06.01 | 2026.09.00 (verified stable BOM; **not** alpha/beta BOM) |
| androidx.camera core/camera2/lifecycle/view | 1.4.2 | 1.6.2 |
| androidx.activity:activity-compose | 1.10.1 | 1.13.0 |
| androidx.lifecycle:lifecycle-runtime-compose | 2.9.2 | 2.11.0 |
| androidx.fragment:fragment | 1.9.1 | 1.9.1 (already stable; preserved) |
| androidx.exifinterface:exifinterface | transitive | 1.4.2 (explicit stable dependency) |
| com.google.zxing:core | 3.5.3 | 3.5.4 |
| androidx.test.ext:junit | 1.2.1 | 1.3.0 |
| androidx.test:runner | 1.6.2 | 1.7.0 |
| androidx.test:rules | 1.6.1 | 1.7.0 |
| junit:junit | 4.13.2 | 4.13.2 (unchanged current 4.x stable) |

The AGP/Gradle/Kotlin combination must be tested, since Kotlin documentation lists a narrower **fully tested** Gradle/AGP matrix than the individually latest versions. The project must stay on a compatible combination if the real CI rejects the combination. Android platform 37 is published as the versioned package android-37.0; the dedicated SDK preparation script installs that package and creates a compatibility path for legacy integer compileSdk lookup.

Authoritative version sources (checked 2026-11-08):
- https://developer.android.com/build/releases/about-agp
- https://docs.gradle.org/current/release-notes.html
- https://kotlinlang.org/docs/releases.html
- https://developer.android.com/develop/ui/compose/bom
- https://developer.android.com/jetpack/androidx/releases/camera
- https://developer.android.com/jetpack/androidx/releases/activity
- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://developer.android.com/jetpack/androidx/releases/test
- https://central.sonatype.com/artifact/com.google.zxing/core/versions

## 5. Release acceptance and remaining certification

1. Static security/dead-code scanner and Python regression tests both return zero blocking findings.
2. Kotlin source and instrumentation compile; JVM tests, Lint and R8 release shrinker pass with resolved dependencies.
3. Merged debug/release Android manifests contain no prohibited permissions; 16 KiB native-library alignment checks pass.
4. All 23 localization resources pass; twelve authentic 1080×1920 Android screenshots are verified, plus compact/landscape/large-font samples.
5. P03 connected instrumentation succeeds on API 26, 31, 34, 35 and 36 with sanitized crash/ANR checks.
6. All four workflow conclusions are success for the exact PR-head SHA before merge, and again for the exact main SHA after merge.
7. Publish v1.0.42 with APK, AAB, unsigned Play QA AAB, 12 genuine captures and SHA256SUMS; verify release tag target SHA, signatures and all 16 release assets.

**Do not report these criteria as complete until GitHub Actions confirms them.** The audit is scoped to this repository's source code and CI. Hardware-specific OEM testing, dynamic penetration testing, third-party vulnerability advisories, external signing identity and zero-pixel-image parity remain separate acceptance tasks.
