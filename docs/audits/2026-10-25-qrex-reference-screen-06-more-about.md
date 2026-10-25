# QREX — ekran 06: Više, O aplikaciji i izvoz

**Referenca:** tamnoplavi Android QREX prikaz kartica „Više”, uz svijetli primjer „More / Settings” i „Export”. Android Q/QR tamni sustav ostaje produkcijski; marketinški iOS raspored ne implementira se kao Android 1:1.

## Stvarni nalazi

- U oba layouta (`compactSettings` i normalni grid) kartica **O aplikaciji** prethodno je **izravno otvarala brendigo.com**, bez podataka o instaliranoj aplikaciji. Takav klik nije odgovarao očekivanju naziva kartice ni referentnom „O aplikaciji — QREX v...”.
- Na stranici izvoza sigurnosno objašnjenje `privacy_summary` ograničavalo se na **četiri retka**, što posebno kod duljih prijevoda ili velikog fonta skriva dio upozorenja.

## Implementacija

- **O aplikaciji** sada otvara stvarni Material3 `AlertDialog` sa službenim `QrexBrandMark`, lokaliziranim naslovom/izdavačem/sloganom i **versionName iz PackageManager metapodataka instaliranog APK-a** (ne hardkodiranu verziju).
- Postojeća mogućnost otvaranja `https://brendigo.com` nije uklonjena; sada je **sekundarna korisnikova izričita radnja** s lokaliziranim `tr("open_link")` gumbom, bez automatskog izlaska iz aplikacije.
- Gumbi `qrex-more-about` imaju iste dostupne surface kontrole u oba responsivna layouta; otvoreni dijalog i web akcija imaju stabilne semantičke tagove.
- Stranica izvoza ostaje `verticalScroll`, a cijeli prevedeni `privacy_summary` se ispisuje bez `maxLines = 4`. Ostale PDF/SAF i Wi-Fi sigurnosne potvrde nisu mijenjane.

## Testovi

- `QREXMoreAboutTest.aboutOpensNativeDialogAndKeepsWebsiteAnExplicitSecondaryAction`: stvarna Compose navigacija i mjerena dodirna površina ≥48dp, bez internetskog poziva tijekom testa.
- `tool/test_more_about_reference.py`: provjera nenametnutog website akcijskog gumba, čitanja instalirane verzije i netrunciranog Privacy teksta.
- Obvezni CI gateovi prije mergea: Kotlin/JUnit, Lint, R8, lokalizacije 23 jezika, emulator screenshotovi i API 26/31/34/35/36.

**Granica:** za pixel-exact dokaz potrebne su neiskrivljene snimke u istim dimenzijama; trenutni marketing prikazi nisu temelj za numeričku nultu razliku.
