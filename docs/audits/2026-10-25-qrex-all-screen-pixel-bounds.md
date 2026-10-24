# QREX — measured, not assumed, screen-by-screen pixel / touch audit

Scope: Android QREX current `main` after stable public **v1.0.14**. This is a continuing QA branch, **not** part of the signed v1.0.14 distribution.

## Measured emulator controls

Android Compose instrumentation checks the real `getUnclippedBoundsInRoot()` of clickable controls and requires **≥48dp in both dimensions**. It exercises all four root tabs, Scan gallery/flash, Create's four quick QR types and pinned Generate/Save, Result back, History All/Saved filter, More's Language/Export, Language native-name row/back and PDF Export/back. Each measurement uses density-independent bounds of a rendered semantic node, not a CSS/source proxy. No clipboard, scanned content or Wi-Fi secret is emitted in test failures.

Other source-level controls (History favorite and overflow, QR action lists, launcher/splash vectors, monochrome safe-zone scaling, compact and large font layouts) retain their existing regression assertions. Ten genuine screenshot paths remain validated by Android-emulator UI-hierarchy checks; screenshots are never fabricated to satisfy the gate.

## Actual pixel difference measurement

Use `tool/compare_ui_pixels.py` to compare **all ten** actual app screenshot PNG files against user-owned PNG references. Identical dimensions, viewport, font scale, language, screen state, animation settings and permissions are prerequisites. No resampling/interpolation occurs, as that can conceal 1px errors. The tool uses a bounded, CRC-checked, 8-bit RGB/RGBA PNG decoder with proper filter reconstruction. It reports mismatch count, percentage, maximum channel difference and bounding rectangle, and fails if an image cannot be decoded, differs in dimensions or exceeds the threshold.

Example:

```sh
python3 tool/compare_ui_pixels.py \
  --reference-dir /path/to/original-qrex-reference-pngs \
  --actual-dir /path/to/real-android-1080x1920-captures \
  --channel-tolerance 0 --max-mismatch-percent 0
```

Files must be named `android-{splash,scan,create,wifi,result,history,saved,more,language,export}.png`. For a single screen add `--screen create`; for multiple screens repeat `--screen`.

**Reference limitation:** The original concept-board image bitmaps are not checked into this GitHub repository and are not available in this CI run. Therefore a **numerical 1:1 claim against those images is unverified**. Do not compare QREX v1.0.13 screenshots and claim they are the original user art. When genuine reference PNGs are supplied, run this audit and log actual pass/fail scores, and fix each nonzero difference. Launcher differences under OEM-adaptive masks need separate device reference baselines.

**Signing/data safety:** The audit does not change native history, encryption, app identity, UI theme policy, Play signer or published v1.0.14 assets.
