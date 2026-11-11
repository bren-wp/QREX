# QREX native Android — dead-code and reference-screen audit (2026-11-11)

**Scope:** The 11 production Kotlin files under `platforms/android/app/src/main/java/com/brendigo/qrex/`, Android Gradle metadata, 23 localization sources, manifest security settings, maintained release workflows and historical publisher archive, public README/release records, and existing JVM/connected Android regression gates.

## Confirmed failures (post-merge)

- Main workflow `42936163124` failed before Android compilation in Python's source-quality gate.
- Missing exact README published-release identifier caused four unit failures (`test_google_play_badge`, `test_release_archive`, and two `test_release_integrity` cases).
- Following the marker fix, `42937916542` still failed due to an absent v1.0.44 entry in `docs/releases/INDEX.md`. The release-notes publication record was also absent, although the v1.0.44 GitHub assets already existed.
- Fixed README release marker, outdated screenshot link, verified release index and historical release notes. Avoid blaming the app runtime for these failures.

## Static reachability inventory

- **11 Kotlin production files** reviewed, approximately **272,000 source characters**.
- **121 Kotlin function declarations** identified by a textual signature inventory. Names with only a declaration and one use cannot be safely deleted: several are UI callbacks, cross-file composables or implicitly invoked Android framework overrides. CameraX's `ImageAnalysis.Analyzer.analyze` is overridden by the runtime (not an unused function).
- Every import in the 11 production Kotlin files checked for textual references. Five imports in `MainActivity.kt` were provably redundant (unused `painterResource`, `Translate`, `Archive`, `PictureAsPdf`, `Palette`) and removed.
- The existing nested QR result screen has its own valid BackHandler with focus/QR-preview cleanup. A second, outer `BackHandler(enabled = currentCode != null)` was redundant and removed. Single-activity navigation, system back behavior and screenshot fixtures remain.
- The `QREXCore.kt` imports `androidx.compose.runtime.getValue` and `setValue` were **retained** because Kotlin delegates `by mutableStateOf` use those implicit operator functions. Static identifier counts alone would incorrectly mark them dead; deleting them would break compilation.
- Original Android IDs, keystore and saved history schema were not modified. None of the eight production QR creation formats, CameraX/gallery scanners, result actions, local history/favorites, secure Wi-Fi handling, export functionality or 23 supported languages were removed.

## Release/workflow audit

- Previously published v1.0.44 pipeline is archived, not deleted from historical reproducibility documents.
- New GitHub-only v1.0.45 pipeline targets `versionCode 145`, `versionName 1.0.45` and fails closed unless the four main-SHA workflows pass, APK/AAB signatures and identity match, hashes verify, and twelve **actual Android emulator PNGs** are present.
- GitHub signing key is disposable; Google Play upload key stays outside the public publisher. Existing Play installations cannot be updated with this GitHub certificate.
- v1.0.44 README screenshot links remain grounded to actual published files until a verified v1.0.45 release exists; no marketing illustration is represented as an emulator capture.

## Six reference screens, visual/functional review

| Screen | Source correction / focus | Quantified true pixel diff |
| --- | --- | --- |
| Splash | Preserved native layered wave paths, 3.5s on-screen Splash, scalable branded logo and progress; geometry audit exists | **Unknown** |
| Scanner | Live CameraX viewfinder, correct bracket glyph and torch/gallery controls, responsive tap targets retained | **Unknown** |
| Result | Removed duplicate BackHandler; kept live action buttons, return cleanup and shared bottom navigation | **Unknown** |
| Create | Eight QR input formats and consent-aware previews remain; shared button typography increased | **Unknown** |
| History | Increased shared title/action typography, preserved safe record excerpts and scrolling | **Unknown** |
| More | Increased shared title and six settings tile label hierarchy; retained 2×3 tiles plus accessibility fallback | **Unknown** |

**Reference constraint:** User images are framed 941×1672 marketing mockups with phone bezel and exterior background. Exact pixel comparison against native 1080×1920 emulator frames is not meaningful. Scanner scenery is real camera input and cannot be reproduced as a static backdrop. Never claim 1:1 or 0% mismatch from these assets.

## Remaining production gates — not claimed as complete by this static audit

The new v1.0.45 native build, Kotlin compiler, R8 dead-resource shrinking, Android lint, 5-API instrumentation, actual screenshot capture and SHA-256 release signing checks must all finish **green on the exact versioned commit**. Physical device tests, store upload-key signing, Google Play approval and exact pixel-comparable flat reference screenshots are separate acceptance criteria. An unobserved result is not a pass.

## Reproduction and code evidence

- Regression suite: `python3 -m unittest discover -s tool -p 'test_*.py'`.
- Native compile, unit and static QA: `gradle -p platforms/android :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease` (with appropriate Java/Android SDK).
- Android API matrix / authentic screenshots: `.github/workflows/p03-stability.yml` and `capture-screenshots.yml`.
- Pixel verification *only with unframed identical-viewport source PNGs*: `python3 tool/compare_ui_pixels.py --reference-dir <flat-reference-dir> --actual-dir docs/screenshots --screen splash --screen scan --screen result --screen create --screen history --screen more --channel-tolerance 0 --max-mismatch-percent 0`.
