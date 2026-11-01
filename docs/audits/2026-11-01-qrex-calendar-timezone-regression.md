# QREX — scanned iCalendar time zone regression (2026-11-01)

## Verified code defect
Before this change, `QRContent.eventTimeMillis()` always interpreted a parsed
`DTSTART` or `DTEND` as UTC, even if it had no trailing `Z`. It also
failed to recognize common `DTSTART;TZID=Europe/Zagreb:...` / `DTEND;TZID=...`
properties. The Result screen passes these values to Android's calendar insert
Intent, so a scanned floating-local or time-zone-specific event could be
created at the wrong time or fail to open.

## Implementation
- Retain UTC behavior only for timestamp strings with an explicit `Z` suffix.
- Interpret floating iCalendar time as device-local time.
- Interpret `TZID` using the IANA time zone database, including summer/winter
  UTC offset changes.
- Reject malformed date-time strings, unknown time zones, date-only values,
  ambiguous TZID+Z combinations, and unrelated field requests.
- No QR payload, user preferences, saved history, encryption, UI layout,
  Android permissions, dependencies, or package identity are changed.

## Regression tests
`QREXCoreTest` adds four JVM tests for:
1. Explicit UTC start and end instants.
2. Floating calendar times in the device zone.
3. Named `Europe/Zagreb` TZID summer/winter offsets and parameterized properties.
4. Malformed / unsupported date and time-zone combinations.

### Acceptance still required
GitHub Actions `QREX Android native CI` (compile, JVM tests, lint, R8),
`QREX P03 Android API lifecycle matrix`, localizations, and real screenshot
jobs must pass at the final PR head before merging. No pixel-perfect match to
unavailable original reference PNGs is claimed. No new release has been made.
