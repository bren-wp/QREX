# QREX — Screen 03 Result / Rezultat: standalone reference alignment

Date: 2026-11-08. Baseline: merged QREX v1.0.41 source (PR #99,
`af6b61d80a6880610e43a9776c891cb25f8fd5b8`). Public v1.0.41
APK/AAB release remains subject to the original exact-main CI gate.

## Supplied reference vs genuine Android 1.0.41 capture

User-provided visual: the dark Result handset in the 1672×941 QREX collage
and its extracted 259×504 perspective-framed crop. The reference is a
marketing composition, not a perfectly rectified device PNG. The genuine
QREX screenshot comes from CI's `android-result.png` captured on a
1080×1920 Android emulator.

Confirmed **structural** deviations:

1. Reference Result is a standalone route with Back/overflow header, *no
   five-tab navigation*. Current app kept the five-tab NavigationBar on Result.
2. Reference visible actions are Open → Copy → Share → Show QR → Favorite →
   Delete; the app inserted Edit between Favorite and Delete. On an ordinary
   phone this pushed Delete below the initial viewport. Editing remains a
   genuine feature but must not distort the reference action stack.
3. Reference URL summary has a distinct subdued secondary origin line.
   The app used only the full QR payload. Repeating a tokenized full URL would
   disclose credentials in a second area; the secondary line must contain
   **scheme and hostname only**, without userinfo, path, fragment or query.
4. Existing fixed summary minimum did not adjust to normal vs short phones.
   Use a readable reference-style summary at normal phone/tablet widths,
   while retaining a compact fallback at <360dp, height <650dp or >=135% font.

## Native implementation

- Scaffold's five-tab navigation remains exactly on Scan, Create, History,
  Saved and More, **hidden only when a QR Result is open**.
- Both Back header and Android system Back return to the invoking destination,
  preserving the previous tab; overlay popups and consent remain native.
- Move Edit to a real overflow `DropdownMenuItem` with a test tag. It still
  routes back to Create with the QR's payload to edit.
- The result summary displays the original data once plus a sanitized URL
  origin in 12sp. No second sensitive QR/password/text payload is introduced.
- The Result icon remains native vector with existing color hierarchy;
  summary minimum: 154dp compact, 180dp normal phone, 192dp wide tablet.
  The action list remains scrollable and IME-safe.
- Preserve Wi-Fi copy/share/save/PNG confirmations, Android Keystore state,
  history deletion confirmation, actual generated QR preview, and 23 locales.

## Verification plan

- Kotlin/Android instrumented test asserts no `qrex-bottom-navigation` or
  nav tabs while Result is visible, checks summary/primary/favorite/delete
  accessibility, overflow Edit action and return to real Create navigation.
- Existing privacy and static reference-contract tests must pass, with
  updated height constraints.
- PR-head green native Kotlin/JVM, Lint/R8, 23-language generator, genuine
  screenshot capture (including tiny/landscape/large fonts/tablets), P03
  API 26/31/34/35/36 emulator matrix are mandatory.
- The PR must not be merged or advertised as production before all CI gates
  pass; any future APK/AAB publisher must be separately versioned, tied to
  the exact merged main SHA, verified, and never presented as Google Play
  upload-key signed.

### 0% pixel deviation

Numerical pixel-perfect parity cannot be asserted against the perspective
marketing collage without flat source frames and matching viewport, density,
font, theme, OS bars, app state and actual capture. This PR improves
**structural correspondence** rather than inventing a 0% figure.
