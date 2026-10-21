# QREX reference-image UI alignment — Android implementation contract

**Reference:** the four user-provided QREX images (dark master board, dark phone UI, blue/light banner and earlier light gallery). **Primary authority:** dark master board and dark phone UI, with blue/light references only for branding and secondary theme.

## Visual specification

- **Default dark:** midnight navy `#061020`, deeper navigation `#030A18`, panels `#111E32`, raised cards `#152641`; electric blue `#087BFA`, bright cyan `#22C7FF`, purple `#9866FF`. White foreground and muted slate subtitles. System/light appearance remains an explicit user preference, **not** the primary visual direction.
- **Bottom navigation:** Scan, Create, History, Favorites, More in exactly that order, dark dock, rounded active pill, legible blue/cyan selected icons and labels. Keep interactive elements >=48dp with TalkBack semantics.
- **Scan:** dark top brand, camera preview with illuminated scan brackets, flash + gallery actions, dark rounded panel.
- **Result:** left-aligned title, central link/QR symbol and readable payload, primary blue action, secondary stacked full-width rows; danger red only for deletion. Preserve explicit confirmation for sensitive Wi-Fi content.
- **Create:** compact dark QR-type chips, user input, real QR preview, large blue Generate action and secondary Save. QR types limited to those actually supported; **Events / Calendar** shown in concepts is NOT yet an implemented feature.
- **History/Favorites:** dark rounded cards, category-specific color icons, two-line truncated labels, timestamp, delete actions, search and saved filters.
- **More:** two-column tiles for theme/language/privacy, functional PDF/ZIP export, dangerous Delete All in a distinct red-styled panel.
- **Icons/branding:** reuse actual QREX source assets. Never convert AI concept mockups into claims of actual screenshots or actual implemented app features.

## Implementation / QA status

QREX v1.0.4 already has the dark design tokens and dark-first default (`QREXStore.lightMode=false` for new users). This PR begins the pixel-alignment effort by correcting dark navigation accent/indicator, result heading alignment and History row touch target; extends source regressions. Exact 1:1 parity for every reference screen, all subflows and full design-system verification remain **pending** until real emulator screenshots are compared screen-by-screen and reviewed at the reference device size. Do not describe this initial PR as already achieving complete pixel parity.

Preserve all encrypted history, CameraX ZXing QR scanning, Flutter data migrations and v1.0.4 release tags.
