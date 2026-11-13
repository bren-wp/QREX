#!/usr/bin/env python3
"""Fail-closed QREX screenshot audit for six or all twelve native screens.

The November 2026 user references are framed 941x1672 illustrations,
not flat 1080x1920 Android screenshots. Never equate these illustrations
with actual emulator pixels. A true mismatch score is only calculated
for the same viewport, locale, mode, density and exact pixel dimensions.
No images are produced, resized, or uploaded by this script.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from compare_ui_pixels import read_png, compare, SCREENS as ALL_SCREENS

SCREENS = ("splash", "scan", "result", "create", "history", "more")
# Terms is an actual dialog capture in the reference responsive profile, not
# a regular 1080x1920 Play Store image. Count it as its own screen in QA.
ALL_VIEW_STATES = ALL_SCREENS + ("terms",)
REFERENCE_NOTE = (
    "Illustrated smartphone with exterior wallpaper and bezel; "
    "no matching unframed reference screenshot was supplied"
)


def audit(
    actual_dir: Path,
    reference_dir: Path | None = None,
    screens: tuple[str, ...] = SCREENS,
    responsive_dir: Path | None = None,
) -> dict:
    # The legacy six-screen contract remains the default. The CI workflow
    # additionally audits the entire twelve-screen genuine screenshot set.
    if not screens or len(set(screens)) != len(screens) or any(
        screen not in ALL_VIEW_STATES for screen in screens
    ):
        raise ValueError("Only unique known QREX screens may be measured")
    if "terms" in screens and responsive_dir is None:
        raise ValueError("Terms requires a genuine responsive screenshot directory")
    rows: dict[str, dict] = {}
    for screen in screens:
        name = f"android-{screen}.png"
        # All twelve Play Store captures use the native screenshot directory.
        # Terms is obtained from the already-captured real reference profile.
        path = (responsive_dir / "reference-terms.png"
                if screen == "terms" else actual_dir / name)
        recorded_name = (
            "responsive/reference-terms.png" if screen == "terms" else name
        )
        actual = read_png(path)
        record = {
            "screen": screen,
            "android_capture": recorded_name,
            "viewport_px": [actual.width, actual.height],
            "actual_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "exact_mismatched_pixels": None,
            "exact_mismatch_percent": None,
            "max_channel_delta": None,
            "status": "illustrated_reference_not_pixel_comparable",
            "reference_limitation": REFERENCE_NOTE,
        }
        if reference_dir is not None:
            reference = read_png(reference_dir / name)
            # Refuse dimensional mismatch rather than scaling or cropping
            # away the phone bezel and falsely claiming 0% deviation.
            diff = compare(reference, actual, tolerance=0)
            record.update(
                exact_mismatched_pixels=diff.mismatched,
                exact_mismatch_percent=round(diff.percent, 6),
                max_channel_delta=diff.max_channel_delta,
                changed_bounds=diff.bounds,
                status="verified_exact_pixel_comparison",
                reference_limitation=None,
            )
        rows[screen] = record
    return {
        "source": f"actual Android emulator screenshots for {len(screens)} QREX screens",
        "reference_type": ("flat_equal_size_png" if reference_dir
                           else "framed_phone_reference_artwork"),
        "all_exact_1_to_1": (
            all(rows[s]["exact_mismatched_pixels"] == 0 for s in screens)
            if reference_dir is not None else None
        ),
        "screens": rows,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--actual-dir", type=Path, required=True)
    parser.add_argument("--flat-reference-dir", type=Path)
    parser.add_argument("--all-screens", action="store_true",
                        help="Audit the twelve genuine standard QREX captures")
    parser.add_argument("--include-terms", action="store_true",
                        help="Also audit the real Terms modal captured in the responsive profile")
    parser.add_argument("--responsive-dir", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.include_terms and (not args.all_screens or args.responsive_dir is None):
        parser.error("--include-terms requires --all-screens and --responsive-dir")
    selected = (ALL_VIEW_STATES if args.include_terms
                else ALL_SCREENS if args.all_screens else SCREENS)
    report = audit(args.actual_dir, args.flat_reference_dir,
                   screens=selected, responsive_dir=args.responsive_dir)
    content = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    print(content)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    return 0 if report["all_exact_1_to_1"] is not False else 1


if __name__ == "__main__":
    raise SystemExit(main())
