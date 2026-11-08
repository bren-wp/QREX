# QREX 1.0.41 — supplied visual reference, screen 06 More
Date: 2026-11-08. Baseline: published v1.0.40, release commit `efc60c11b6e6bfb7ed9a182ba1ccf09c69d824b6`.

## Actual user reference
The supplied 1672×941 collage depicts six phone screens and branding. The **More**
phone at the far right shows a title/subtitle, a **2-column × 3-row settings
grid** with cards Theme/Language, Export/Privacy, Terms/About, then a
**full-width separate destructive Clear History row**, and five-tab bottom
navigation. It is an angled mockup and does not establish a per-pixel flat
1080×1920 image baseline.

## Confirmed structural difference
The current v1.0.40 More Compose screen renders all seven settings entries in
one full-width vertical list; its geometry diverges markedly from the
supplied 2×3 reference. The prior all-row layout was based on an older
conflicting instruction. This newly supplied image is the active reference.

## Implementation in v1.0.41
- Use real Compose `FlowRow` for the six functional 2×3 tiles, with 10dp
  horizontal and vertical spacing and a constrained 560dp maximum panel width.
- Each tile is a true full-card accessible click target, 124dp minimum
  height; 40dp raised RGB icon plate, localized title and optional subtitle.
- Use one readable row per item below 360dp width, below 500dp viewport
  height (landscape) or at fontScale >=1.30. Preserve 72/88dp accessible
  vertical rows in these profiles, including screen readers.
- Keep Clear History separate, full-width, destructive and behind explicit
  confirmation; preserve all real Theme/Language/Export/Privacy/Terms/About
  callbacks and real installed app version.
- No decorative screenshot or generated QR artwork is substituted for the
  running app. No QR records, Wi-Fi secrets, encryption, migrations or
  CameraX scanning behavior are changed.

## Tests and outstanding verification
- Pure geometry tests: compact 320×568, reference 393×852, width 360,
  800dp tablet, high font 130%/200%, landscape 800×420.
- Compose instrumented tests: dark/light side-by-side card geometry,
  real clickable privacy/terms callbacks and no history deletion.
- Require **all four green exact-PR-head checks** before merge, then four
  green exact-merged-main checks, actual 12 emulator PNGs, verified GitHub
  APK/AAB hashes/signature checks and `SHA256SUMS.txt`.
- Do **not** assert mathematically 0% pixel error without original flat
  reference screenshots at matched viewport, Android bars and locale.
