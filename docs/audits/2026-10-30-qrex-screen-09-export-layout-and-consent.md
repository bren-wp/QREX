# QREX Android — Screen 09: Export (PDF / SAF), 30 October 2026

**Branch/PR:** `ui/favorites-compact-single-action-20261030`, [PR #80](https://github.com/bren-wp/QREX/pull/80). This is the next screen after Language; no separate pull request, release, or signing change.

## Reference and design acceptance

The original marketing boards show an Export destination in the same QREX design family as More and Language. They are perspective-transformed rather than flat and pixel-aligned PNGs; zero-difference "pixel-perfect 1:1" against those images **cannot yet be measured or certified**.

The native production screen now maintains the detail screen's 52dp Back touch target in a fixed header while its content alone scrolls. The PDF action is a native Material3 surface (minimum 56dp from the shared design system); the local PDF warning and complete, untranslated-by-remote-service 23-language privacy statement are separate branded cards with consistent padding, icons and radii.

For the empty state, the PDF control is explicitly disabled and the existing localized `no_saved` message explains why. The existing export warning and full privacy statement remain untruncated even at accessibility font scales. `qrex-export-content`, `qrex-export-warning`, `qrex-export-privacy`, `qrex-export-pdf` and `qrex-export-no-saved` identify the layout in automated UI tests.

## Non-negotiable data-flow security

- Export is allowed only for **saved** QR records. Nothing is uploaded to QREX or a third-party server.
- PDF export still uses Android Storage Access Framework `CreateDocument("application/pdf")` and writes to the user-selected destination on `Dispatchers.IO`. It does not create a plaintext temp PDF.
- Pressing PDF with saved codes now reveals an explicitly tagged `qrex-export-consent` modal. **Cancel** closes it without launching SAF. **Confirm** freezes the approved saved-record snapshot *before* launching SAF; only the frozen list is written on successful picker return. If the picker is cancelled, nothing is written.
- The dialog warning explicitly covers Wi-Fi password-bearing saved codes, using the original localized string; no permissions, app data schema, encryption behavior or signing keys are changed.
- Existing `QREXDocuments.valid` enforces non-empty, saved and QR-renderable records, limited to 250, on the export side.

## Tests

- `QREXExportScreenTest.exportHasPinnedBackAndScrollableUntruncatedPrivacy`: Android Compose bounds for PDF >=48dp, empty-state disabled where applicable, both warning cards, scroll last card and verify Back remains visible.
- `QREXExportScreenTest.savedPdfRequiresExplicitConsentAndCancelRetainsAllHistory`: create a unique synthetic QR through real app UI, save it, navigate to Export, verify enabled button and consent dialog, cancel (no Android picker), ensure prior history is retained, and remove **only** this synthetic entry in a `finally` block.
- `tool/test_more_about_reference.py`: static checks for preserved SAF, snapshot consent, pinned header, warning/empty states and all new tags.
- Exact-head Android Kotlin/JUnit/R8/Lint, 23-language resource checks, Android 26/31/34/35/36 connected test matrix and actual emulator screenshots remain required. Existing capture workflows render `android-export.png` plus compact, reference and large-font Export captures.

## Visual validation limits

`tool/compare_ui_pixels.py` can measure differences only with matching original unmodified reference PNGs (same viewport, Android version, density, language, font, keyboard, state). Real emulator screenshots prove that the UI rendered; they do **not** by themselves prove 1:1 matching to the original marketing images.

## Next

Complete all tests, address actual CI failures and only then consider merging PR #80 and starting a Play-signed release using the authorized upload key.
