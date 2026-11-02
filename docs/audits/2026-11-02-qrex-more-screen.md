# QREX screen 08 — More / Više: native screen pass

**PR:** #83, `feat/splash-scanner-reference-v127`  
**Target:** v1.0.27, Android versionCode 127

## Existing UI preserved
- Dark appearance retains three paired rows of native settings tiles.
  Light appearance and narrow/large-font viewports use responsive full-width
  rows with the same live actions, plus a separate destructive history action.
- All six options remain actionable: Theme, Language, Export, Privacy,
  Terms, About. Language and Export open native detail pages; the other
  options open native, locally rendered dialogs.
- No external UI, advertising SDK or image mockup is used.
- Clear History remains a separate destructive operation behind explicit
  user confirmation, not a control that activates on a normal page visit.

## Improvements
- The Theme dialog exposes its current selection via accessible Compose
  `selected` semantics for each 52dp option and a tagged 48dp Close button.
- Both PDF export-consent actions and both Clear History confirmation
  actions have 48dp minimum touch height, preserving their actual callbacks.
- `QREXMoreRootTest` exercises the live More tab, theme selection and
  no-write dismissal, then cancellation of the clear-history dialog while
  retaining the existing records. No stored history or credentials are
  printed or mutated by the test.
- `tool/test_more_about_reference.py` locks in the theme semantics,
  confirmation touch targets, protected PDF snapshot, and non-destructive
  cancellation behavior.

## Test and visual verification limits
- The supplied branding reference is a perspective marketing board, not
  independently rectified 1080x1920 device screenshots. No numerical pixel-
  by-pixel equality claim is possible from that source.
- CI must remain green on the exact PR head: 23 language resources, native
  Kotlin/JVM, Lint/R8, authentic 12-screen capture, and API 26/31/34/35/36
  instrumented checks. A test error is never waived.
- Public CI APK/AAB signing is not the authorized Google Play upload key.

## Release
The PR remains draft until all CI checks pass, and v1.0.27 remains a
candidate until actual GitHub Release tag and nonempty assets are confirmed.
