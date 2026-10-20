# QREX — post-1.0.2 UX, performance and dead-code audit

**Date:** 20 October 2026  
**Baseline:** `main` at `b15a95a18820cbe8588b4d098a0719a3a5ce7b4a` (published GitHub v1.0.2).  
**Change scope:** `improve/post-102-ux-deadcode-audit`. No release tag, signing material, app ID, versionCode or persisted schema changes.

## Audit inventory and method

- Inspected **all six tracked production Kotlin source files**: `MainActivity.kt`, `QREXScanner.kt`, `QREXCore.kt`, `QREXDocuments.kt`, `QREXSecretCipher.kt`, `QREXStrings.kt`. Checked top-level/helper declarations and cross-file references, imports, lifecycle/resource ownership and Compose call sites.
- Inspected the repository's entire Git tree (not truncated), the Android app Gradle configuration, presence of manifest and 23 generated locale files, Kotlin unit and Android instrumentation suites, Python quality checks, the eight tracked GitHub Actions workflows, and previous code/dead-code audit reports.
- Searched Kotlin source-level references to functions and imports. **Textual counts are advisory**, not a Kotlin compiler/R8 reachability result; code reached via Compose, Android manifest, Java interoperability or framework callbacks may appear unused to a text matcher.
- GitHub CI performs Python regression tests, Kotlin compilation and JVM tests, Android lint and R8 minified variant, permission and ELF checks, screenshot capture, and the separate five-API 26/31/34/35/36 emulator lifecycle matrix when the changed source paths trigger them.
- Neither this audit nor emulator CI certifies OEM camera hardware, battery drain on real phones, Play Console approvals or in-place upgrade/signature compatibility. No automatic app release is implied.

## Confirmed dead code and cleanup

| Finding | Evidence | Action |
| --- | --- | --- |
| `responsiveQrSideDp(availableWidthDp, preferredDp)` | Only referenced in obsolete tests; Create/Result render directly with `widthIn(max = 238.dp/230.dp).fillMaxWidth().aspectRatio(1f)`. | **Removed** from production. Replaced its redundant Kotlin unit test with an actual Wi-Fi SSID/password-label privacy regression; adjusted compact UI contract test. |
| Unused-import heuristic flagged `androidx.compose.runtime.getValue` / `setValue` | Kotlin `by mutableStateOf(...)` invokes operator extension methods implicitly. Simple textual reference counting is misleading. | **Retained**; these are required for delegated Compose properties. |
| `ImageAnalysis.Analyzer.analyze`, `MainActivity.onCreate` | Framework callbacks are invoked externally, not from other Kotlin source lines. | **Retained**. |
| Flutter history migration and AES-GCM helpers | Legacy `FlutterSharedPreferences` and encrypted `history` data must survive upgrades; `QREXSecretCipher` is used by storage paths. | **Retained** all migration, safety guard and keystore code. |
| `github-release-1.0.1.yml`, 1.0.1 tests and old release notes | Preserve historical release evidence and independently authorized Play-upload process; prior GitHub publisher is source-SHA guarded. | **Retained**, not silently deleted as “dead”. |
| PDF/ZIP writer and APK/Play checks | Used from More-screen exports, CI and manually gated release paths. | **Retained**. No duplicated unused export pipeline established. |

No other production Kotlin declaration was proven unreachable with sufficient confidence to remove safely.

## UX and stability changes

1. **Light mode accessibility:** Select a legible dark-blue navigation color instead of light cyan on a light navigation surface. Use current theme primary for branding text in Result and Scanner.
2. **History:** Bound long Wi-Fi SSIDs / QR titles to two lines with ellipsis and timestamps to one line, keep Delete reachable. Give empty history/search state a clear, padded surface. No password revealed in Wi-Fi history labels.
3. **More/settings:** Align save-history/theme controls, document-export actions and language control to comfortable minimum heights. Make destructive Delete All a full-width, error-colored action; keep it available to recover after unreadable local history. Display the Privacy and Terms actions as compact, balanced bottom links.
4. **Scanner:** Round camera preview bounds while retaining gallery access without camera permission. Treat CameraX torch enable/disable as an asynchronous future: disable repeat taps until completion, update UI only on confirmed success, show the existing localized camera-error message on failure, and reset transient torch state on camera disposal.
5. **Documentation:** Correct README's old 1.0.1 “current” release text to the actually published GitHub 1.0.2. Preserve explicit disclosure that GitHub disposable signing cannot perform in-place upgrades of existing GitHub/Play installations.
6. **Regressions:** Add `tool/test_ux_dead_code.py` covering relevant source contracts; retain all existing Kotlin, Python, screenshot and emulator gates.

## Residual risks / checks before another Android release

- Source contracts cannot measure actual color contrast under every dynamic theme or assess all font scales: inspect real devices at 200% font, landscape, split screen and accessibility touch exploration.
- Verify CameraX torch behavior, cancellation, camera permissions and failed-gallery fallback on real Pixel/Galaxy/low-memory hardware. Check battery/CPU with actual traces rather than implying an emulator number.
- Retest history corruption protection, Wi-Fi ciphertext survival, Flutter preference migration and PDF/ZIP export with non-destructive physical-device upgrades.
- Publish a **new** version (not a mutable v1.0.2 tag) if these post-release changes are to be distributed. Existing v1.0.2 GitHub APK/AAB assets must remain immutable. Use the proper signing identity for any Google Play update.
