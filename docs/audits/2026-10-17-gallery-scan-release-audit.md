# QREX — Gallery memory, release integrity and scope-limited code audit

**Date:** 17 October 2026  
**PR:** [#10](https://github.com/bren-wp/QREX/pull/10)  
**Base:** `main` after merged PR #9, commit `ffff8820a362ea2edf082ae353487e7ab9f5adfd`.

This document records **source-reviewed findings and changes**, not a claim of complete UI/UX parity or physical-device certification. CI status must be checked at the latest PR head SHA.

## Confirmed findings and changes

| Scope | Source-reviewed problem | Change / regression check |
| --- | --- | --- |
| Android photo QR scanning | The old flow checked width, height and input length but then used `InputImage.fromFilePath`, allowing an accepted 24 MP photo to be decoded at full resolution. | Downsample via `BitmapFactory.Options.inSampleSize` before ML Kit receives an `InputImage`, target no more than about 6 MP decoded. Kotlin regression checks sample boundaries and the common EXIF rotations. |
| iOS photo QR scanning | The old Vision handler processed full-resolution photo data. Large accepted frames created avoidable memory pressure. | Build an orientation-aware ImageIO thumbnail (up to 3072 px) off the UI thread, then feed Vision the smaller CGImage. XCTest checks size and integer-overflow safety. |
| Malformed photo metadata | Untrusted dimensions can be invalid or oversized; naive multiplication risks integer overflow. | Reject non-positive dimensions and use checked range-by-division for the Swift megapixel limit. |
| Release versioning | `v0.2.3-rc.1` already used version 0.2.3, code/build 8. The previous publishing workflow targeted a branch used by a completed PR. | Prepare 0.2.4 build 9, update the documented QA branch/tag, strengthen static reused-build gating, and retain SHA/provenance and prerelease checks. |
| Public release notes | Top-level `RELEASE_NOTES.md` still identified 0.2.2 as the latest published preview even though 0.2.3 was published. | Correct the published-preview reference and distinguish the pending 0.2.4 candidate. |

## Source hygiene / dead code

Reviewed the touched Kotlin, Swift, Python and workflow paths together with existing audits. Replaced the old full-resolution decoder call; no new image-storage permissions, internet access or user-data collection were introduced. Existing `lib/` Flutter source, migration paths, translation generator, brand assets and release QA scripts are **intentionally retained** because they have compatibility or build consumers. Static review does not establish that every unrelated symbol and resource is unused, so no speculative cross-repository cleanup was performed.

## Unresolved risks and required evidence

1. Actual large JPEG/HEIC/PNG/EXIF orientation and mirrored image scans must be exercised on real devices; static unit checks do **not** establish decoding success for every provider.
2. Android instrumented history tests and iOS XCTest must pass on the **latest** head SHA. Android lint/R8, iOS unsigned device archive, Flutter checks, localization parity, and captured screenshots must also be inspected.
3. Current automated screenshot verification covers a limited screen sample rather than exhaustive Android/iPad/tablet, dark/light, landscape, Dynamic Type and all-language pixel comparisons.
4. Physical device stability, interruption/permission scenarios, memory measurements and long-running background foreground cycles remain separate manual acceptance tests.
5. AAB/IPA production signing has **not** been performed. Do not claim Play/App Store readiness without authorized signing credentials and release review.
6. A comprehensive transitive dependency CVE assessment and exhaustive dead-code graph verification are not established by these source edits.

**Release policy:** Do not merge on unresolved CI failures or present an unsigned/debug QA artifact as production signed. Publish only a prerelease candidate after the exact-commit checks actually finish.
