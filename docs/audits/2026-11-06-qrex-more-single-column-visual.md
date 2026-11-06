# QREX UI audit — Postavke / More: seven native vertical cards

Date: 2026-11-06. Baseline: verified GitHub v1.0.35 on main commit
`aa2cbce244f9500c40f80d36aa6b0b7e3d10825a`.

## Confirmed highest-discrepancy layout

The prior `MoreScreen` made dark Settings a two-column 3×2 grid. In light
appearance or below a narrow-width/font threshold, it changed to rows.
This violated the user's explicit requirement that the policy and settings
entries be one below another in **all** modes. UI properties and the
existing tests were therefore inconsistent between themes.

## Implemented correction and measurable layout contract

- One `Column`, tagged `qrex-more-settings-stack`, always contains seven
  individually clickable `QrexSettingsListRow` cards. No old tiled branch
  or `useCompactMoreList` dead layout helper remains.
- Order: Theme → Language → Export → Privacy → Terms → About →
  separated Clear History. All existing localized strings, routes and
  confirmation callbacks remain, no fake buttons or mock screenshots.
- Card geometry: `fillMaxWidth`, `heightIn(min=qrexSettingsCardHeightDp(fontScale).dp)`, 15dp radius,
  3dp elevation, 10dp vertical inter-card gap; destructive action has
  an additional spacer.
- Icon depth: 40dp icon plate, 12dp radius, native 4dp elevation, 22dp
  vector glyph, gradient RGB highlight and a real border. Icon itself
  is decorative; the entire card remains the only accessible click target.
- Padding: 14dp horizontal, 11dp vertical in each card, 14dp page inset.
  Titles are 15sp semibold; subtitles 12sp; label may span up to three
  lines and the card grows without overlapping siblings.
- Screen remains scrollable so expanded fonts do not hide Privacy or Terms
  behind the bottom navigation. The existing History deletion confirmation
  is unchanged and reading legal pages cannot mutate any QR record.

## Verification

Source tests validate seven unique callbacks, full-width stack and absence
of obsolete grid code. A new Android Compose instrumentation test measures
all card bounds in the rendered tree for both themes, requires minimum height
and aligned widths, checks no overlaps, and opens Privacy/Terms.

A genuine APK screenshot is required for color, typography, gradient and
visual inspection on the final commit. **No zero-pixel / 1:1 equivalence is
claimed** without a flat, same-size source reference. Prior marketing
collages were perspective-composited and cannot yield a valid 1080×1920
pixel error metric by direct subtraction. No screenshot is generated in chat.

Never merge an untested branch or publish 1.0.36 before exact-main Kotlin,
R8/lint, emulator API 26/31/34/35/36, localization and screenshot passes.
