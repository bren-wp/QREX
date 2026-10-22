# QREX reference-image UI alignment — Android implementation contract

**Reference:** the four user-provided QREX images (dark master board, dark phone UI, blue/light banner and earlier light gallery). **Primary authority:** dark master board and dark phone UI, with blue/light references only for branding and secondary theme.

## Visual specification

- **Default dark:** midnight navy `#061020`, deeper navigation `#030A18`, panels `#111E32`, raised cards `#152641`; electric blue `#087BFA`, bright cyan `#22C7FF`, purple `#9866FF`. White foreground and muted slate subtitles. System/light appearance remains an explicit user preference, **not** the primary visual direction.
- **Bottom navigation:** Scan, Create, History, Favorites, More in exactly that order, dark dock and cyan active icon/label.
- **Scan:** compact QREX/tagline header, live camera preview and scan frame, with gallery and flashlight actions.
- **Result:** no bottom navigation; result icon and payload, Open link or Add to calendar, Copy, Share, Show QR, Favorite, Edit and Delete actions. Confirm sharing or saving a Wi-Fi password.
- **Create:** four visible type chips (Link, Text, Wi-Fi, Contact), remaining types in the overflow menu, input and compact real QR preview, Generate and Save to device actions.
- **History/Favorites:** search and filter control, compact category-colored rows with readable labels and dates, row overflow for show/save/delete.
- **More:** six two-column tiles for Theme, Language, Export, Privacy, Terms and About. Clear history is a separate red action. History-saving control belongs in Privacy.
- **Supported payloads:** links, text, Wi-Fi, contacts, email, phone, locations and calendar events. Keep each label and action tied to an implemented flow.
- **Screenshot policy:** capture the running app on Android for the matching store locale. Do not submit promotional boards, device mockups or rendered UI as app screenshots.

## Implementation / QA status

The dark theme is the new-install default. A visual review is still required after the emulator run: compare each in-app screen against the supplied phone screens at the same aspect ratio, then fix remaining spacing, colors, content order and clipping before calling the implementation 1:1.

Preserve all encrypted history, CameraX ZXing QR scanning, Flutter data migrations and v1.0.4 release tags.

