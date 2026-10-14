# QREX — Mobile Quality Assurance Matrix

This checklist defines release acceptance for the separate Kotlin/Android and SwiftUI/iOS apps. A green CI build does **not** prove visual 1:1 parity, accessibility or crash-free behavior on every device.

## Visual reference, design and screenshot integrity

- [x] Capture genuine Android emulator and iPhone simulator Create screens, validated by the last release's QA gate.
- [x] Reject Android ANR/system screens unless the QREX foreground UI hierarchy is confirmed.
- [ ] Capture and compare every other main screen at representative display sizes.
- [ ] Keep original PNGs committed to `docs/screenshots` without marketing retouching; validate PNG chunks, image dimensions and completeness automatically.
- [ ] Compare Scan / Create / Result / History / More on both platforms against the supplied QREX design, at representative mobile screen sizes.
- [ ] Confirm there are no large black letterboxing bands, clipped content, text overlapping the bottom navigation, or layout jumps after keyboard dismissal.
- [ ] Confirm blue/cyan/purple accents, icons, bottom navigation, typography, scan-frame dimensions and dark surfaces remain consistent.
- [ ] Verify at least one smaller display, one larger display and a tablet layout; do not rely on a single emulator screenshot.
- [ ] Review both light and dark themes and large accessibility text sizes.

## QR functionality

| Workflow | Android | iOS |
|---|---|---|
| Fresh launch, no account required | Pending device QA | Pending device QA |
| Allow camera and deny/re-enable permission | Pending | Pending |
| Scan QR via rear camera | Pending | Pending |
| Background/foreground camera lifecycle | Pending | Pending |
| Torch on/off and graceful unsupported flash | Pending | Pending |
| Read QR from selected gallery image | Pending | Pending |
| Reject malformed/oversized photos safely | Pending | Pending |
| Create URL/text/Wi-Fi/contact/email/phone/location | Pending | Pending |
| Display QR and scan it with another device | Pending | Pending |
| Confirm malicious URL does not open automatically | Pending | Pending |
| Copy/share result through system UI | Pending | Pending |
| Save/search/delete history | Pending | Pending |
| Clear all persisted local data | Pending | Pending |
| Preserve existing user data when upgrading | Pending | Pending |

## Security and privacy release gates

- [ ] Wi-Fi passwords and other sensitive QR contents must not be written into unencrypted general-purpose preference stores. Use Android Keystore / iOS Keychain or prevent such storage with clear disclosure.
- [ ] Test new installs, legacy-storage migration and Keychain/Keystore failure paths without unexpected data loss.
- [ ] Test offline behavior, consent boundaries, denial of permissions and external URL schemes.
- [ ] Audit permissions, manifest, Apple's privacy report and transitive SDKs before app store submission.
- [ ] No login, forced signup, analytics, ad SDK or unrequested tracking.
- [ ] Test camera interruption, system low-memory pressure and a user leaving during photo processing.

## Localization

- [x] English is the source/default language; 22 further languages are catalogued.
- [x] Automatically check all 23 catalogs for 80 message keys, missing labels and stale generated files.
- [ ] Manually inspect every language in Settings plus Arabic- and CJK-like long-string stress cases (not currently supported locales), with dynamic text sizing.
- [ ] Test persistent language changes across app restart, including follow-system fallback.
- [ ] Check that system camera permissions, error dialogs and share labels use the correct default device language.

## CI and packaging

- [x] Standalone Android Kotlin/Compose and iOS SwiftUI applications compile in GitHub Actions.
- [x] Android Gradle unit tests execute in CI.
- [x] Swift XCTest executed on an available iPhone simulator in the verified 0.2.2 RC1 run.
- [x] Actual Android and iPhone simulator Create screenshots passed the verified 0.2.2 RC1 pipeline.
- [ ] Physical device smoke tests and App Store/Play Store signing completed.
- [ ] Only promote the next GitHub release after all blocking items are resolved.

**Status:** Open QA checklist; do not present these unchecked items as completed.
