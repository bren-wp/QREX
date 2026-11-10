# QREX — Splash + Home only: 2026-11-10 reference-measurement pass

Scope: **only** the native launch screen and the scanner Home screen. Do not touch Create, Result, History, Favorites, More, authentication, package/signing, localization files, or the app's QR scanning/decoding backend.

## Supplied reference measurements (framed 941×1672 marketing PNGs)

Both uploaded images show an illustrated phone, its bezel, Android status bar and a decorative outside background. These are not flat original Android captures. Approximate *inner display* bounds: x=146..782, y=130..1486 (Splash) and x=147..781, y=130..1486 (Home).

| Feature | Approximate observed reference bounds | Normalized target |
|---|---|---|
| Splash illuminated logo | x=286..641 (exclude glow), y=457..810 | Width 355/636 = 55.82%; vertical center 37.1% of display |
| Splash upper wave begins | y≈254 | 9% of display height |
| Splash lower foreground wave | y≈1100 | 71.5% of display height |
| Splash progress track | x≈311..621, y≈1401..1414 | Width 310/636 = 48.74% |
| Home preview panel | x≈147..781, y≈348..1280 | Live CameraX, not a stock bitmap |
| Home neon corner frame | x≈268..668, y≈556..940 | Width 400/635 = 62.99% of camera |
| Home frame vertical center | y≈748 within 348..1280 camera | 42.92% down camera |
| Home bottom tab bar | begins y≈1280 | Five native tabs preserved |

## Source implementation

- In-app Splash displays for **3500 ms** from the first Compose frame, using a monotonic remembered timestamp so rotation does not restart the delay. The OS-managed Android 12+ system splash runs separately and cannot be specified to an exact wall-clock duration by this Compose code.
- Standard phone 393×852dp layout: vector logo width 214dp; proportional brand offset (about -50dp at 393×852dp and -35dp at 411×731dp); restored native upper and lower wave positions; footer track follows 56% of the available footer area and retains width/height bounds. Narrow, short, wide, tablet and 1.4×+ font windows retain accessible fallback layout.
- Home live viewfinder: scanner corners target **63%** of available width and are vertically centered at **42%** of the preview height, clamped above the hint/buttons. CameraX, hardware torch acknowledgement, scoped gallery picker, camera permissions and the five bottom tabs remain fully functional.
- Text/header and scan hint sizes are increased only on Home/Splash; touch targets remain at least 48dp.

## Quantified source geometry deviations

Run:
```bash
python3 tool/measure_splash_home_geometry.py
```

This computes reference vs *source-geometry* normalized discrepancies (in **percentage points**) and prints machine-readable JSON. Every genuine Android screenshot capture CI run also executes:
```bash
python3 tool/measure_splash_home_geometry.py \
  --actual-dir docs/screenshots \
  --output docs/screenshots/splash-home-measurements.json
```
The artifact named `qrex-splash-home-measurements` includes actual PNG dimensions, source-derived geometry deltas and an explicit **null pixel difference** until comparable flat references are available.

For **real, zero-tolerance pixel-by-pixel differences** supply *unframed, unwarped* Splash/Home screenshots matching the Android emulator's resolution, density, status/nav bars, font scaling and locale. Run:
```bash
python3 tool/compare_ui_pixels.py \
  --reference-dir /path/to/flat-reference-screens \
  --actual-dir docs/screenshots \
  --screen splash --screen scan \
  --channel-tolerance 0 --max-mismatch-percent 0
```

Alternatively `tool/measure_splash_home_geometry.py --reference-dir ... --actual-dir ...` embeds exact mismatch counts/percents in the JSON output, but refuses differing image sizes. **No screenshot from the user's framed phone artwork is rescaled or silently claimed to match pixels.** The camera's live scene cannot be compared byte-for-byte to the promotional plant/laptop image unless identical camera input is provided. UI overlay geometry can still be evaluated.

## Mandatory QA

- Kotlin JVM unit tests including Splash duration, reference geometry, frame clamping and compact/tablet/large-font windows.
- Android Lint/R8, privacy/dead-code source checks; screenshot workflow including 1080×1920 and separate responsive profiles.
- Android instrumented Home/Splash and camera permission/torch/gallery controls, API 26/31/34/35/36 stress matrix.
- Preserve existing installed signing identity and do not publish new APK/AAB before exact-commit checks pass. A mathematically certified 0% pixel mismatch is **not established** from these illustrations.
