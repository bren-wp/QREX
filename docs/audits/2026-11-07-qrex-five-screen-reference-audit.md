# QREX — Privacy / Terms / About / Home / Splash native UI audit

**Source baseline:** v1.0.36, verified 2026-11-07, tag
`https://github.com/bren-wp/QREX/releases/tag/v1.0.36`.
The independently captured v1.0.36 screenshot assets include Splash,
Scan/Home, Privacy and About. No dedicated full-resolution flat Terms
reference is provided by the user; it is still covered by actual Android
instrumentation and responsive capture workflow.

## Highest-impact visual changes applied

1. Privacy: dark/light adaptive summary Surface, native 40dp
   highlighted Shield glyph, outlined border and real scrolling disclosure.
2. Terms: the previous bare summary text is now a matching bordered
   native policy Surface with raised Description vector glyph.
3. About: explicit Brendigo author/domain is grouped into a matching
   raised card; actual installed APK version and canonical QREX logo remain.
4. Home: live CameraX preview retains brackets and action controls;
   in-preview guidance is an elevated native, readable hint card.
5. Splash: same localized QREX vector identity and responsiveness;
   refined native cyan lower-wave reflection and tagged logo halo.

## Behavior protected

- All retained Android test tags for Privacy/Terms/About dialogs.
- Preference write success and History data integrity; no History deletion
  merely by opening disclosures.
- External policy/website links never open without explicit user action.
- Offline QR creation/decoding, saved favorites, image picker, PDF/PNG
  privacy disclosures and camera permission prompts are unchanged.
- Native icon glyphs remain decorative, not duplicate TalkBack targets.
- Actual release signature and screenshot artifacts must be verified.

## Verification limit

Marketing collage perspective, unequal density and phone framing make a
numeric claim of mathematically pixel-exact 1:1 unsound. Complete
reference parity requires aligned original flat screens, normal/large font
tests, physical device inspection and genuine CI screenshots; do not
claim zero difference from source code review alone.
