# QREX — ekran 05: Povijest i spremljeni QR kodovi

**Dan pregleda:** 25. listopada 2026.  
**Primarna referenca:** QREX tamnoplava kompozicija „Povijest” s tamnim karticama i cijan ikonama. **Sekundarna referenca:** svijetli Android skup zaslona „History” i „Favorites”. Nisu identični produkcijski skinovi, a perspektivni marketing prikazi nisu izravni screenshotovi Android emulatora.

## Stvarni nalazi i ispravci

| Element | Prije promjene | Nakon promjene | Provjera |
|---|---|---|---|
| „Sve” | Prvi `FilterChip` prikazivao je „Povijest”, što ne opisuje sve/samo spremljeno stanje | `tr("all")`, sva 23 jezika | `qrex-history-all` ≥52dp |
| „Spremljeno” | Drugi `FilterChip` prikazivao je „Favoriti”; u vizualnoj referenci upotrebljava se „Spremljeno” | `tr("saved")`, već postojeći prijevod u svih 23 jezika | `qrex-history-saved` ≥52dp |
| Prazna Povijest | Prazni popis pod „Sve” pogrešno je prikazivao „Nema spremljenih kodova” | Nova lokalizirana poruka `tr("no_history")` samo kada `savedOnly == false` | Prazno „Spremljeno” i dalje koristi `tr("no_saved")` |
| Pretraživanje | Pretraga je radila ali nije imala stabilan testTag | Dodan `qrex-history-search` za instrumentirani test | Stvarni Compose minimum 48×48dp |
| Lokalizacija | Nije postojao zasebni ključ `all` ni `no_history` | 23 jezika u `l10n/catalog.json`, svih 23 Android `strings.xml` i `QREXStrings.kt` | Generator je kanonski izvor, `--check` u CI-u |

## Zaštita postojeće funkcionalnosti

- `QRHistory.matches` i dalje filtrira prema tekstu i spremljenom stanju; ne mijenjaju se oblici pohrane ni migracija.
- Sve kartice zadržavaju svoj QR tip, oznaku, datum, radnju otvaranja, favorit srce, dodatne radnje i potvrdu brisanja.
- Wi-Fi tajne ne dodaju se u tekstualni indeks; zadržana je zasebna sigurnosna obrada u `QRHistory.matches`.
- Pojedinačni gumbi u svakoj kartici već imaju `Modifier.size(48.dp)`; postojeći API35 issue s fractional dp mora se nadzirati stvarnim instrumentiranim testovima.
- Tamna paleta `QrexDesign` i postojeća četverostruka navigacija ostaju netaknute.

## Testni i pixel gate

- `tool/test_history_reference.py` statički provjerava svih 23 jezika, semantiku filtera i zasebne poruke praznog stanja.
- Novi `QREXHistoryFilterTest` otvara stvarnu Povijest, provjerava „Sve” → „Spremljeno” → „Sve”, te mjeri stvarne `qrex-history-*` touch targete najmanje 48dp.
- Potrebni su Kotlin/JUnit, Android Lint, R8, 23 jezika, 10 stvarnih emulator snimki i API 26/31/34/35/36.
- Za numeričku 1:1 razliku treba neiskrivljena referenca iste rezolucije, jezika, teme i podataka. Perspektivni marketinški prikaz nije takva referenca.

**Izdanje:** promjene su na radnoj grani bez promjene `versionName/versionCode`. Nema tvrdnje da je projekt ili release završen prije uspješnih CI/emulator provjera.
