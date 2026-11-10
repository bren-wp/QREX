# QREX — Six-screen reference audit and post-merge API 26 failure

**Scope:** Splash · Scanner (Home) · Result · Create QR · History · More. All six supplied references are **941 × 1672 framed phone illustrations**, rather than unframed same-density Android screenshots. All actual CI screenshots are independent Android emulator captures, not manufactured visuals.

## Verified post-merge regression

- The previous PR #113 was merged into `main` on 2026-11-10, commit `9d0663414073d06bed56581c0a4639ddd0f2934c`.
- Native Kotlin/JVM, security, Lint/R8 and localization workflows succeeded.
- P03 Android API 26 connected instrumentation failed and redacted triage reported **one fatal QREX exception**. API 31 and 34 passed; do not claim the five-API matrix was successful.
- Existing safe diagnostics suppressed private QR payloads but also failed to print the failing test identifier. `tool/report_failed_instrumentation_tests.py` now prints only validated QREX test-class and method identifiers from `TEST-*.xml` when instrumentation fails; exception messages, QR payloads, stack traces and system log data remain private. The actual P03 failure gate is never disabled. The precise app-crash root cause still requires the newly identified test and private, locally inspected crash details.

## Reference discrepancy fixes staged in PR #114

| Screen | Observed on genuine main screenshot | Change in PR | Proof still required |
| --- | --- | --- | --- |
| Splash | Wave/logo proportions differ from artwork | Keep existing native gradients and 3.5-second in-app timer | Exact clock/viewport + matching flat baseline |
| Scanner/Home | Test emulator's live preview differs from illustration; small navigation scanner symbol | Native corner-bracket scanner nav glyph; preserve live CameraX/torch/gallery | Genuine phone camera capture, clipping checks |
| Result | Bottom navigation missing despite latest supplied reference | Preserve five tabs while Result is open; keep pinned Back and scrolling action column; link-specific Copy label | Instrumented navigation and small-screen Result fit |
| Create QR | Quick tabs too tall; Croatian URL type shows "Poveznica" | Normal phone quick tabs 60→52 dp, Croatian title "Link"; all eight QR types remain working | QR preview alignment and button usability |
| History | Fifth row partially clipped at reference viewport | Normal row min 78→64 dp, row gap 6→4 dp; larger-font row minimum remains 100 dp | Five visible real fixture rows and accessible scrolling |
| More | Square icon plates, missing helper captions | Circular 48dp icon plates, six captioned 2×3 tiles, shorter grid gap, separate destructive clear row | 23-locale wrapping, touch targets and screenshot geometry |

Localized new captions are present in the canonical catalog and synchronized generated Android string resources for all 23 supported locales.

## Auditable measurement output per CI run

`tool/measure_six_screen_reference.py --actual-dir docs/screenshots --output docs/screenshots/six-screen-measurements.json` records the six genuine captures, each resolution and SHA256. CI uploads the `qrex-six-screen-measurements` artifact.

Each `exact_mismatch_percent` is `null` until an **unframed, unwarped, identically sized** PNG reference has been supplied for that screen. Supply these as `android-splash.png`, `android-scan.png`, `android-result.png`, `android-create.png`, `android-history.png`, `android-more.png`, and run `python3 tool/measure_six_screen_reference.py --actual-dir docs/screenshots --flat-reference-dir /path/to/real-references`. The checker fails on different dimensions and refuses a nonzero pixel-mismatch result as 1:1. Never stretch/trim a phone mockup and assert 0% error.

**Scanner special case:** a live CameraX scene is determined by the user's camera and cannot match the reference's staged laptop/plant scene pixel-for-pixel. Compare only deterministic UI overlays with a reproducible camera source, never replace live scanner frames with marketing imagery.

No APK/AAB or exact 0% claim until all exact-commit gates, including API 26, are green.
