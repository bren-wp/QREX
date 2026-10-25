# QREX v1.0.16 — screen-by-screen production reference review

## Reference selection
1. Dark Android Q/QR, cyan brackets, blue accents — selected for Splash, Scanner, Result, Create, History and More.
2. White Android double-phone composition — informative for Create QR choice labels, visibility and Gallery/flash, **not** a production second theme.
3. Alternate dark scanner-square and iPhone phone chrome — secondary creative variation, **not** a replacement icon or Android platform reference.

## Examined routes and states
| Route/state | Main matching implementation | Difference from non-rectified marketing art |
| --- | --- | --- |
| Splash | `QREXSplash.kt`, shared vector `qrex_icon.xml` | Phone chrome and marketing blue waves differ from actual device |
| Scanner / denied camera / gallery | `QREXScanner.kt`, CameraX live scan, picker, torch | Actual camera frames vary by hardware/permission |
| Result | `MainActivity.kt` Result card and actions | Secondary rows preserve safe actions rather than ornamental UI |
| Create URL / Wi-Fi / 6 advanced kinds | `MainActivity.kt`, live type choices, pinned Save/Generate | Additional formats expand on demand for compact phones |
| History & Saved | `MainActivity.kt`, search magnifier, filters and local rows | Dark reference shows five tabs; stable Android release intentionally retains four top-level destinations |
| More / About | `MainActivity.kt`, settings tiles, real installed-version About dialog | Theme tile is absent because Android app is dark-only |
| Language | `QREXMoreDetails.kt`, 23 radio rows + System, 52dp Back | White reference is a different theme; now correct TalkBack description |
| PDF Export | `QREXMoreDetails.kt`, untruncated privacy warning, real PDF SAF picker | ZIP was intentionally removed in prior privacy simplification |

## Measurable QA and acceptance
- Source-level Android Kotlin/JUnit, Android Lint/R8, localization consistency, Android API 26/31/34/35/36 stress/emulator matrix, ten genuine 1080×1920 screen captures plus compact/large-font QA.
- Preserve security boundaries: no Internet permission, no automatic Wi-Fi credential history, no QR-content logcat in crash triage, SAF for export, no silent external URL open.
- Do not label a screen `pixel-perfect` on the strength of a perspective mockup. True numeric 1:1 requires original straight, individual screenshots with same resolution, status/nav bars, copy, input content, font scale and system theme. The repository includes a strict image comparison script that can then produce exact mismatch counts and coordinates.
