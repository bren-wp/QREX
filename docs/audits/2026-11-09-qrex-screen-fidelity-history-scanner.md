# QREX — reference-driven all-screen pass (2026-11-09)

Baseline: `main` commit `43d2e0a9270996023ec1f785143b0d80e0b664f7`, Android package `com.brendigo.qrex`, v1.0.42 / build 142. This pass is on `feat/qrex-reference-history-scanner-layout-20261109` and MUST NOT be advertised as an already shipped release.

## Reference and measurement limitations

The user-supplied 1672×941 image is a six-handset **perspective marketing composition**, not flat reference screenshots of identical 1080×1920 Android frames. It supports structural/visual prioritization, but exact per-screen pixel mismatch percentages would be invented. GitHub contains twelve *genuine* v1.0.42 emulator PNG captures; these must be re-captured on this exact source head and compared with original unframed user references before claiming numerical 1:1 parity. No image is generated or substituted for the native UI.

## Screen-by-screen status and priority

| Screen | Comparison finding | Work in this pass | Remaining |
|---|---|---|---|
| **History / Povijest** — **highest confirmed structural gap** | Perspective reference: 5 colored item cards with title, supporting text, separate colored category tag + time and **one overflow** per card. Previous source: title and category/time only plus extra visible heart *and* overflow; missing supporting line made cards less faithful. | Three-tier real Compose History/Favorites rows with safe supporting previews, colored category chip, overflow-only History, retained direct unsave on Favorites. The five reference-like QR types are represented using *debug-only in-memory screenshot fixtures*; no synthetic history enters production storage. | Pixel-coordinate differences for spacing and glyphs still require flat captures |
| **Scanner / Skener** — high | Reference: plain instruction text over live camera and an unstruck lightning icon. Prior source: solid outlined hint card, crossed lightning in off state. | Retain CameraX/ZXing/gallery/zoom/permission/torch callbacks; make hint a lightweight translucent text scrim with shadow, remove heavy cyan border, retain the unstruck lightning pictogram and convey state via tint after CameraX confirms. | Real camera subject necessarily varies; device-specific torch/zoom testing remains |
| **Splash** — high | The sample contains three layers of curved blue wave depth; native Canvas had upper and lower broad ribbons with a large visually flat middle interval. | Add a subdued, vector-drawn middle ribbon. Keep existing logo/wordmark, cyan halo, brand colors, responsive footer and progress. | Absolute pixel comparison with original flat Splash screenshot still unavailable |
| **Create QR** — high | Current source has the reference's 4 quick type tabs, unified input + real generated preview card, explicit URL clear, pinned Generate and Save, advanced types in overflow. | Source rechecked; no speculative replacement of functioning UI. | Compare active Link form and Wi-Fi/contact/event states with aligned device captures |
| **Result** — high | Existing v1.0.42 Result is standalone with Back and action stack; previous PR #107 added safe email/phone/location/contact dispatches. | Preserve existing behavior; the History changes do not affect actions or privacy consent. | Flat visual diff for summary-card dimensions, icon plates and action spacing |
| **Favorites** — medium | Saved History variant is a dedicated tab; direct heart is useful and present in the reference. | Preserve visible direct save toggle, add safe supporting text and tag. | Check narrow phone / large font semantics |
| **More / Settings** — medium | 2×3 native cards and separate destructive action from v1.0.41 match source structure. | Preserve responsive one-column fallback and all callbacks. | Check long translations, status/nav bars and tablet |
| **Language, Export, About, Privacy, Terms** — medium | Native detail screens with scroll and consent; no comparable dedicated flat reference in current composite. | No speculative layout changes. | Need per-screen reference images for geometric parity |
| **Launcher / branded icon** — medium | Adaptive vector paths and branded QREX wordmark exist; OEM launcher masks differ. | Keep vector drawables unchanged. | OEM launcher + light/themed icon device test |

## Privacy, security and dead-code gates

- `QRHistory.preview` returns bounded, sanitized supporting text: only URL **scheme+host** (never path/query/token), Wi-Fi **SSID** (never password), a valid email address without QR-supplied mail headers, validated phone number, or an explicit map search. Calendar location / arbitrary note / malformed payload are not previewed.
- History keeps its menu item for reversible Favorite toggling; the extra visible heart-only branch was removed, rather than leaving an unreachable visual control in source. Favorites still has direct unsave.
- No app or network permission change; no modifications to QR storage schema, migration, Android Keystore encryption, consent gates, core decode logic, build signing or published releases.
- Existing `tool/audit_android_security.py` is a first-party static source gate. R8 provides actual production dead-code/resource shrinkage. Neither one proves absence of vulnerabilities, exhaustive dead code or physical-camera compatibility.
- Regression updates cover History source-contract assertions, privacy redaction unit tests and Android Compose display checks. Existing API 26/31/34/35/36, 23-language, Kotlin/JVM/Lint/R8 and genuine screenshot capture workflows must pass at **the exact PR-head commit** before merge.

## Acceptance

1. Re-run all mandated GitHub CI workflows on the final commit; block merge if any are red or not completed.
2. Compare real Android captures against flat reference screenshots at exactly matching resolution/density/font scale/theme/locale/content and OS insets.
3. Manually verify CameraX preview, flashlight (with and without hardware flash), gallery, QR creation and exports, navigation, History search/favorites, high-font/compact/landscape and TalkBack behavior.
4. Preserve app ID `com.brendigo.qrex` and separate GitHub disposable signing key from the Google Play upload key. Do not advertise v1.0.42 artifacts as a new release of these unmerged changes.

**Current status:** Source edits and regression coverage committed to a separate branch. Unmeasured pixel-perfect parity and dynamic security certification are not claimed.