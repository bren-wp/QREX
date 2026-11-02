# QREX — new branding reference: screens 01–02

Date: 2026-11-02. Source: user-supplied composite `QREX_ Sve što može QR(20261102-200600).png`.

## Scope

This is a **native Android Compose implementation pass**, not a generated picture,
a screenshot pasted behind controls, or a change to functional QR payload handling.
The source is a marketing board with device frames and an illustrated camera
view, not independently aligned device screenshots; zero-pixel deviation cannot
be asserted from this composite.

### 01 — Splash

- Keep the production Q/QR Android vector and the same two-tone QRE/X wordmark.
- Add a restrained cyan vector-drawn halo around the real icon, matching the
  luminous rim in the supplied reference without bundling a bitmap.
- The old progress track was fixed at 94dp and visibly undersized relative to
  the reference phone. Its width is now approximately half the available
  footer column (166dp for a 333dp available region), capped at 184dp and
  never wider than the container. Cyan fill occupies 51% of that track.
- Preserve translated subtitle and footer, scroll-mode fallback at <640dp
  viewport height or >=1.5x font scale, and existing debug-only capture mode.

### 02 — Home / Scanner

- Keep live, on-device CameraX/ZXing preview, existing framing corners and
  working Gallery / Torch buttons.
- Add the missing **round 56dp floating flash button** at the camera viewport's
  top-right with 14dp padding and a navy translucent surface.
- Both flash controls use the existing single asynchronous CameraX toggle
  callback, are disabled if no camera/flash is available and wait for hardware
  success before indicating the new state.
- The new button carries a localized accessible label, toggleable state and
  `qrex-scan-floating-torch` test tag.
- Do not replace the live camera with the blurred laptop/plant picture from
  the marketing artwork; the actual scene must come from the user's camera.

## Verification

- `QREXSplashLayoutTest`: standard, compact, larger-font and progress widths.
- `QREXSplashReferenceTest`: actual Compose Splash element visibility,
  separation, bounds and track size.
- `QREXScannerControlsTest`: 48dp+ native actions, now including the
  top-right torch when the real camera is available.
- Required before merge: Kotlin/JVM tests, Android emulator instrumentation,
  Lint, R8 and 12 genuine responsive screenshot fixtures; compare screenshots
  on the same viewport, locale, display density, status/navigation insets and
  camera state. On devices without a flash, the control is correctly disabled.

## CI incident and remediation

- The initial screenshot CI failed **before emulator boot** because upstream
  `sdkmanager` returned `Error on ZipFile unknown archive`. That same
  screenshot job had already compiled `:app:assembleDebug` successfully.
- The API 31 matrix exposed a **test-source compilation error**: a Compose
  `DpRect` was queried for the nonexistent `width` property. Fixed by
  calculating `right - left` in `QREXSplashReferenceTest`.
- The screenshot workflow now reuses the already-existing bounded
  `tool/prepare_android_emulator.sh` preflight from P03. It retries only
  classified upstream package/archive failures and never ignores failed
  Android tests or screenshot mismatches.
- Earlier failed CI runs are not evidence that the new head is green;
  final exact-head CI must succeed before merge.

## Additional native screen-by-screen visual pass

### 03 — Result

- The active type glyph keeps the reference's 62dp icon plate and 32dp
  category vector, now with a vertical blue gradient, 1dp illuminated
  edge and 5dp native elevation; QR values and sensitive Wi-Fi rules remain
  unchanged.
- Primary blue actions use a three-stop vertical gradient, 1dp colored
  rim and pressed-state elevation. They remain real Material buttons,
  with the same callbacks and minimum 54dp touch height.
- The Summary / action cards gain only restrained surface shadows; result
  Copy, Share, QR, Favorite, Edit and Delete actions remain independently
  accessible and guarded by their existing consent checks.

### 04 — Create

- The selected QR type (Link, Text, Wi-Fi, Contact or advanced type) uses
  the shared gradient, highlighted border and raised card shadow.
- Compact and enlarged-text layouts still use 2x2 choices to avoid
  truncating translated labels. Selection resets the correct form state.
- QR previews remain bounded and never generated for an off-screen keyboard
  state; Wi-Fi export still requires explicit approval.

### 05 — History / 06 — Favorites

- History rows use 3dp surface depth, while category glyphs have their own
  subtle edge and 2dp elevation. Live card taps and favorite/menu affordances
  are not overlaid by decorative layers.
- Privacy-preserving category labels remain intentional: arbitrary QR
  credentials/tokens must not appear in a preview just to mimic illustrative
  text on the marketing composite.

### 07 — More

- Settings cards retain the existing two-column responsive layout with real
  actions, now with 36dp category icon plates, subtle tinted borders and
  2dp native elevation for the intended 3D hierarchy.
- Destructive clearing remains separate and confirmation-gated.

### Verification and status

- The previous API 26/31 instrumentation failure was a test-host issue:
  `createComposeRule` tried to launch an undeclared default
  `androidx.activity.ComponentActivity`. The test now uses the registered
  QREX `MainActivity` and replaces its Compose content on the UI thread.
- Screenshot references supplied here are perspective-composited marketing
  images, **not** independently aligned source screenshots. This code pass
  does not establish measured zero-pixel difference.
- Tests now assert the resulting gradients, depth tokens, and both working
  scanner flash callbacks. The final exact-commit CI status must be reviewed
  before the draft PR can be merged or a release created.

## Unchanged

Application ID `com.brendigo.qrex`, versionCode 127, signing keys, history,
encrypted Wi-Fi records, migration formats, locale catalog, Play / F-Droid
metadata, camera/gallery decoding and all existing screens.
