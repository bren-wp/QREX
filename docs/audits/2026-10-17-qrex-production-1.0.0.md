# QREX — 1.0.0 production-only release audit

**Date:** 17 October 2026  
**Scope:** GitHub `bren-wp/QREX`, standalone Android/iOS, legacy Flutter compatibility, GitHub Actions, release integrity and version metadata.  
**Review type:** Source-level release-policy hardening, not completed production signing or device acceptance.

## Confirmed changes

| Area | Previous behavior | New behavior |
| --- | --- | --- |
| Native versions | Android and iOS marketing version `0.2.4`, build `9` | Both target `1.0.0` / build `100` |
| Public automation | A branch push could generate a debug APK, unsigned AAB, unsigned iOS archive and GitHub RC release | Automatic preview publishing workflow and script removed; **production** workflow is manual only |
| Production provenance | QA gate on old branch plus prerelease status | Production gate requires the **exact current main SHA**, successful four independent workflows, explicit device QA and store review confirmation |
| Android distribution | Preview AAB intentionally unsigned | Production workflow demands existing Play upload keystore, verifies approved upload certificate SHA-256, builds and validates signed AAB |
| Apple distribution | Unsigned Xcode archive in QA | Production workflow demands an authorized Apple Distribution P12 and App Store profile; verifies Team ID, app bundle ID, version, build and signed IPA |
| Release assets | Debug APK, unsigned AAB, unsigned archive and screenshots attached to GitHub RC releases | Stable release publishes **signed AAB**, **signed IPA** and **SHA256SUMS.txt** only |
| Historical artifacts | v0.2.0 through v0.2.4 RC | Historical releases retained as prereleases; no new RC/beta editions |
| Regression checks | RC publication tests | Production signing/provenance, no automatic preview, version monotonicity and missing-secret/approval negative-path tests |

## Security and architecture decisions

- Application ID and bundle ID remain `com.brendigo.qrex`. Do not alter the publishing identity during a major marketing-version bump.
- No replacement developer certificates, keystores or account identities are generated.
- Production credentials must be stored only in a protected GitHub Actions `production` environment, preferably requiring reviewers. The workflow also requires manual release confirmation and passes through exact-SHA CI checks.
- Only the signed binary artifacts enter a public stable GitHub release. Unsigned CI build artifacts are development diagnostics, not releases.
- Existing Flutter sources and history-migration implementation remain necessary for published customer compatibility. Removing them without comprehensive install/upgrade tests could cause data loss.
- 23 Android/iOS translations and existing on-device privacy constraints remain part of the mandatory CI.
- Code review does not establish actual Google Play/App Store certification. Binary signature checks do not substitute for store approval.

## Remaining blockers before publishing v1.0.0

1. Confirm the latest `main` SHA is the tested release commit; all four mandatory CI workflows completed **successfully**.
2. Independently review physical Android and iPhone/iPad tests for camera permission, live scanning, torch, gallery, PDF/ZIP/PNG, invalid inputs and foreground/background transitions.
3. Verify current Play Console upload certificate lineage, Apple Team ID and provisioning profile and configure protected signing credentials. Until then, the production workflow **must fail closed**.
4. Confirm Flutter-to-Kotlin upgrade behavior against an actual previously installed version; check saved favorites and encrypted Wi-Fi data.
5. Complete accessibility, Dynamic Type, landscape and multi-display checks, dependency CVE and privacy-manifest audits.
6. Prepare real store submissions, content ratings, Google Play Data safety and Apple privacy nutrition labels, then verify store internal/TestFlight distribution.
7. Never merge failing PR checks or claim that 1.0.0 is available in app stores before the stores actually accept it.

## Regression and dead-code audit scope

Removed only the obsolete automatic RC publisher and its script, replaced RC-only tests with production guards, and removed a duplicate version-code test. This is **not** a claim of full project dead-code elimination. The Android/iOS/Flutter feature code, generated localizations and compatibility resources were retained and must not be removed speculatively.
