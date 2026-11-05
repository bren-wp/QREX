# QREX — visual gap assessment and implementation sequence (2026-11-05)

## Evidence and limitations

Reference: user-supplied 1672 × 941 marketing collage (multiple framed Android phones).
Available repository-owned real PNG: `docs/screenshots/android-create.png`
(older capture; not proof of current v1.0.31 appearance).
Last publicly verified complete screenshot asset set: GitHub v1.0.30, twelve
1080 × 1920 genuine emulator captures. The publicly listed v1.0.31 assets
currently contain only APK, AAB and SHA256SUMS. Do not silently claim a
current v1.0.31 1:1 visual baseline.

An objective *ranking across every screen* requires loading aligned live
captures and the original full-resolution flat reference for each screen.
The supplied collage includes phone bezels, perspective, resampling and
non-app imagery. Therefore no numeric claim of zero-pixel deviation or
precise full-screen pixel count is authorized.

## First priority — Create QR

The clearest structural discrepancy is the **form/preview grouping**:
the reference shows the active Link input, QR bitmap and preview label
inside a single dark card with rounded edges. The existing Compose source
placed the input and QR preview in distinct sibling surfaces. The fix
preserves a single raised `QrexPanel` encompassing the input and the
genuine locally generated QR preview and the Wi-Fi/contact/event inputs.

Keep the header Back / centered title / overflow functional; keep four
quick type chips and four accessible advanced QR types. Do not turn
buttons into bitmap decoration. Keep the Generate and Save actions pinned
outside the scrollable form to prevent keyboard occlusion, and protect
the 48dp URL-clear control.

### Regression / correctness criteria

- One visible `qrex-create-unified-card` contains the real editable
  `qrex-create-content` and optionally `qrex-create-preview-card`.
- On a valid payload the QR displayed is computed locally, never a stock
  or reference image; no invalid/empty QR state consumes content height.
- Link, Text, Wi-Fi, Contact and four advanced content types all retain
  their original creation paths. Wi-Fi secrets require consent for PNG export.
- The primary and secondary Create actions are reachable with enlarged font,
  320dp width and visible IME. Preserve real touch semantics and locale labels.
- 12 genuine emulator screenshots and per-screen tests must pass on the
  latest exact commit. Compare pixel alignment, typography and gradients
  only against flat screenshot crops at matching viewport and density.

## Subsequent priority order (reassess with newest real captures)

1. **Splash:** wave contours, badge/halo, QREX typography and footer progress.
2. **Scanner/Home:** true live CameraX preview, 4 neon corners, torch and gallery,
   title status, bottom navigation. A photographic scene in the marketing
   collage is an illustration and cannot be used as a live camera baseline.
3. **Result:** category glyph depth, blue action gradient, Wi-Fi privacy.
4. **History/Favorites:** spacing, category color, detail truncation, menu,
   state and deletion confirmation.
5. **More:** six tiles, accessible 3D icon plates and destructive-action isolation.
6. **Language/Export/About/Privacy/Terms:** responsive details and consent states.

For each screen: source inspection → UI implementation → JVM + emulator tests →
real screenshots → comparison with an aligned reference → exact-commit CI →
GitHub release only after verified assets. Preserve previous passing screens.
