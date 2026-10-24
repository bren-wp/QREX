# QREX Android dark UI — current source-of-truth visual audit

**Release candidate:** 1.0.14 (build 114). Previous concept boards are historical inspiration, not a literal byte-for-byte screenshot baseline. Some older design notes referred to light mode and a fifth Favorites tab; **the approved current implementation has a single dark theme and four root destinations**. Compare like-for-like viewport, text scale, state, locale and selected tab when checking real screenshot pixels. A genuine user-supplied reference bitmap is required for any numerical pixel-difference claim; CI screenshots alone cannot prove 1:1 with an unseen reference.

## Brand / icon parity

- Identity: QREX blue Q with diagonal tail, four cyan scanner brackets, five white QR squares including one hollow finder; **Brendigo** publisher.
- The full-color in-app/splash drawable `qrex_icon.xml` and launcher adaptive background `qrex_launcher_background.xml` share the same gradient stops **#19CAFF → #0868EE → #07187E**. The adaptive foreground retains the safe-zone Q and modules without a redundant background tile. Android OEM masks may crop outer edges and cannot all produce identical pixels.
- The monochrome Android themed icon retains the same negative-space finder center using an even-odd cutout; never replace it with a filled white block.
- The one dark palette uses backgrounds #061020 / #030A18, surface #111E32, raised #152641, accents #22C7FF, #19E3F5 and purple #9866FF. The action-blue token #086BD8 guarantees >4.5:1 normal-text white contrast.

## Screen-by-screen Android implementation contract

| Screen / state | Composition / typography | Functional checks |
|---|---|---|
| Splash | Real vector icon (not a marketing mockup), QRE/X wordmark, localized tagline, dark blue canvas | Launch dismisses once, debug screenshot fixture cannot be injected into production |
| Scan — granted camera | QREX wordmark, luminous white/cyan camera brackets, zoom chips with ≥48dp touch targets, Gallery primary and Torch secondary | CameraX/ZXing, torch async confirmation, camera/gallery arbitration, no preview frame retained |
| Scan — permission denied | Equivalent dark camera placeholder, camera grant button ≥48dp and Settings fallback | Gallery remains available without camera permission |
| Scan — gallery busy | Same layout, real spinner in Gallery action, bounded bitmap decoding | No concurrent double picker; finish or cancel restores state |
| Create — compact / regular | Four top choices (URL, Text, Wi-Fi, Contact), advanced types in More; for 320–419dp width or ≥1.3× fonts a **2×2 grid** avoids truncation; all choices show a selected state in semantics | Eight QR types encode correctly, 23 languages, long labels do not disappear, Generate and Save always accessible |
| Create — form/keyboard | Scrollable form, optional Wi-Fi password, contact, event fields, QR preview (hidden for short viewport/IME), docked Generate/Save | Event local date handling, password reveal, secure export, no hidden submit |
| Result | Pinned back control, scrolling value/QR preview, Open / calendar, Copy, Share, Favorite, PNG, Edit, Delete actions; no bottom tabs | Explicit URL review, Wi-Fi password consent, history updates, safe Android calendar fallback |
| History / Favorites | Segmented All/Saved filter, search, category glyph, labeled entries, 48dp favorite and overflow icons | Favorites reversible, Wi-Fi secrets not searchable, delete requires confirmation |
| More / Settings | Dark responsive two-column tiles on wider phones, full-width rows on narrow phones, localized labels, consistent card heights ≥104dp | Language, PDF export, Privacy, Terms, About, clear history are actual actions |
| Language | Dedicated vertical list with 23 offline languages plus System, flag, native name and radio selected state; list rows ≥64dp | Persists selected locale, no hardcoded user-facing missing keys |
| Export | Dedicated PDF screen, action card and privacy warning, no ZIP collection export | Disabled when nothing saved; PDF/PNG uses Storage Access Framework |
| Dialogs / errors | Material3 dark dialogs, localized confirm/cancel and failure messages, safe keyboard layouts | No secret data in logs, no silently ignored user actions |

## Mandatory validation before release

Run source-contract tests, 23-locale validation, Kotlin/JVM tests, Android Lint, R8 and bundled-permission scan. Capture the ten genuine 1080×1920 dark screens, plus 320dp, 393dp and 1.6×-font responsive captures, using the actual running Compose app. Run the Android API 26/31/34/35/36 lifecycle matrix. Compare actual screen states to the available user references at their own dimensions; do **not** classify visual similarity as mathematically proven 1:1 absent the corresponding original images and a documented image-registration process. Physical OEM launcher masking, TalkBack and real-camera checks remain device QA items.

**Data and release safeguards:** Keep `com.brendigo.qrex`, encrypted Wi-Fi saved records, existing preference/migration formats, Android API 26+, all historical GitHub tags and the original Play signing key separate from disposable GitHub binaries. An APK with a new disposable signer cannot upgrade an existing signed installation in place.
