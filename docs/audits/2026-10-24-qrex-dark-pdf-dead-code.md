# QREX source-level dead code & localization audit — 24 October 2026

**Scope:** Kotlin Compose/CameraX/ZXing native Android source, QREXStore migration, document exports, 23-language canonical catalog, generated Android XML/Kotlin string selectors, current UI tests, screenshots, GitHub workflows and user-facing privacy/terms.

**Removed:** ZIP archive UI, MIME launcher, confirmation, `ZipOutputStream` and `exportSavedPdfZip` method; light palette, theme picker, debug force-light mode, dead alternate Create QR grid/preview, `lightQrSizeDp`, obsolete imported theme preferences and five unused translation keys (`light_theme`, `dark_theme`, `theme`, `archive_failed`, `archive_ready`).

**Retained:** Secure PDF via Android SAF, PNG QR image export, 23 offline languages and system language, four-tab navigation, History/Favorites filter, actual Q/QR launcher and splash, encrypted manually saved Wi-Fi QR contents, Flutter QR history migration, live CameraX/gallery decoding, event/contact QR, privacy controls and responsive compact layouts. Legacy `lightMode` preference bytes are ignored, not deleted or overwritten. Existing GitHub tags and APK/AAB binaries untouched.

**Localization:** regenerated all 23 locale XML resources and the Kotlin lookup source from the same canonical JSON; all 23 language entries contain precisely the same 94 nonempty string keys. Updated translated PDF security warnings, translated PDF captions and splash copy, privacy theme references and terms that previously described ZIP. Automated catalog structural checks are not equivalent to independent native-speaker proofreading in every language.

**Verification:** compile/lint/R8 and unit/instrumented tests, real emulator captures and 5-API P03 stress matrix must pass on the exact final PR head. Ten production-layout dark captures and seven responsive QA captures are required. Source-level review alone cannot prove every transitive Android/Gradle class is unreachable. Pixel-level 1:1 conformity across every manufacturer/handset has not been validated.

**Safety:** no new dangerous Android permissions; Play signing keys do not enter repository. Older differentially signed GitHub APKs cannot install as in-place updates. Do not uninstall existing installations casually: local QR history, favorites and Android Keystore secrets may be lost.
