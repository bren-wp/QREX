#!/usr/bin/env python3
"""QREX Splash/Home-only reference geometry audit.

The supplied 941x1672 PNGs contain a rendered handset and surroundings, not
flat device screenshots. This reports *geometry percentage-point differences*
from measured landmarks; it never mislabels them as pixel-mismatch percentages.
Exact pixel comparison is performed ONLY when equal-size, unframed reference
PNGs are explicitly passed. No visual images are generated.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
MAIN = ROOT / "platforms/android/app/src/main/java/com/brendigo/qrex"
# Handset boundaries were measured from the two user-supplied 941x1672 boards.
REFERENCE = {
    "splash_logo_width_fraction": 355 / 636,
    "splash_logo_center_y_fraction": ((457 + 353 / 2) - 130) / 1356,
    "splash_progress_width_fraction": 310 / 636,
    "home_bracket_width_fraction": 400 / 635,
    "home_bracket_center_in_camera_fraction": ((556 + 940) / 2 - 348) / (1280 - 348),
}
PHONES = ((320, 568), (393, 852), (800, 1280))


def geometry_report() -> dict:
    splash = (MAIN / "QREXSplash.kt").read_text("utf-8")
    scanner = (MAIN / "QREXScanner.kt").read_text("utf-8")
    app = (MAIN / "MainActivity.kt").read_text("utf-8")
    duration = re.search(r"QREX_SPLASH_DURATION_MS\s*=\s*([0-9_]+)L", splash)
    logo_multiplier = re.search(r"logoDp = minOf\(\(widthDp \* ([0-9.]+)f\)", splash)
    progress_multiplier = re.search(r"return \(availableWidthDp \* ([0-9.]+)f\)", splash)
    side_fraction = re.search(r"widthPx \* ([0-9.]+)f, usableHeightPx \* \.83f", scanner)
    center_fraction = re.search(r"previewHeightPx \* ([0-9.]+)f - sidePx \* \.5f", scanner)
    if not all((duration, logo_multiplier, progress_multiplier, side_fraction, center_fraction)):
        raise ValueError("Splash/Home measurement constants missing; audit cannot infer geometry")
    if "delay(qrexSplashRemainingMs(" not in app:
        raise ValueError("Splash timer not wired to production Compose UI")
    values = {
        "splash_duration_ms": int(duration.group(1).replace("_", "")),
        "home_bracket_width_fraction": float(side_fraction.group(1)),
        "home_bracket_center_in_camera_fraction": float(center_fraction.group(1)),
    }
    # The reference phone uses an uncapped 393dp logo (244dp upper bound).
    standard_width = 393
    logo = min(int(standard_width * float(logo_multiplier.group(1))), 244)
    # Noncompact UI's footer horizontal padding is 30dp on each edge.
    available_footer = standard_width - 60
    progress = min(max(int(available_footer * float(progress_multiplier.group(1))), 80), 220)
    values["splash_logo_width_fraction"] = logo / standard_width
    values["splash_progress_width_fraction"] = progress / standard_width
    # Splash logo's vertical center requires actual Compose bounds; do not
    # fabricate a geometric value from approximate font metrics.
    deltas = {
        key: round(100 * abs(value - REFERENCE[key]), 3)
        for key, value in values.items() if key in REFERENCE
    }
    return {
        "reference_source": "2026-11-10 supplied framed phone illustrations",
        "screen_geometry": "approximate normalized landmarks, not exact flat screenshots",
        "reference_landmarks": REFERENCE,
        "derived_source_landmarks": values,
        "absolute_geometry_gap_percentage_points": deltas,
        "pixel_mismatch_percent": None,
        "pixel_measurement_status": "requires matching unframed screenshots",
        "responsive_test_profiles_dp": [list(p) for p in PHONES],
    }


def exact_pixel_results(actual_dir: Path, reference_dir: Path) -> dict:
    from compare_ui_pixels import read_png, compare
    result = {}
    for screen in ("splash", "scan"):
        name = f"android-{screen}.png"
        ref = read_png(reference_dir / name)
        actual = read_png(actual_dir / name)
        delta = compare(ref, actual, 0)
        result[screen] = {
            "pixels_different": delta.mismatched,
            "pixels_total": delta.width * delta.height,
            "mismatch_percent": round(delta.percent, 6),
            "max_channel_delta": delta.max_channel_delta,
            "changed_bounds": delta.bounds,
        }
    return result


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--actual-dir", type=Path, default=None)
    parser.add_argument("--reference-dir", type=Path, default=None)
    parser.add_argument("--output", type=Path, default=None)
    args = parser.parse_args(argv)
    if args.reference_dir and not args.actual_dir:
        parser.error("--reference-dir requires --actual-dir")
    report = geometry_report()
    if args.actual_dir:
        # Check actual production screenshot dimensions; neither screenshots
        # nor their pixels are rewritten, scaled or composited.
        from compare_ui_pixels import read_png
        report["real_emulator_captures"] = {}
        for screen in ("splash", "scan"):
            image = read_png(args.actual_dir / f"android-{screen}.png")
            report["real_emulator_captures"][screen] = [image.width, image.height]
        if args.reference_dir:
            report["exact_pixel_comparison"] = exact_pixel_results(
                args.actual_dir, args.reference_dir)
            report["pixel_measurement_status"] = "exact, equal-size flat PNG comparison"
    output = json.dumps(report, indent=2, ensure_ascii=False)
    print(output)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
