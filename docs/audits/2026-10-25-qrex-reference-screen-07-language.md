# QREX — ekran 07: Jezik, pristupačna navigacija

**Referenca:** svijetla Android kompozicija s vertikalnim odabirom jezika, ali zadržava se produkcijski tamnoplavi QREX sustav sa svim jezicima. Ovim se ispravkom ne kopira alternativna iOS marketinška grafika.

## Nalaz
U stvarnom `QrexDetailHeader` koji koriste zasloni **Jezik** i **Izvoz**, gumb sa strelicom ulijevo bio je veličine 48×48dp i nosio TalkBack opis „Zatvori” (`tr("close")`). Za povratak je ispravan naziv „Natrag”, a raniji fractional-dp problem na API 35 već je pokazao da je preciznih 48dp ruban slučaj.

## Stvarna izmjena
- Povećan stvarni `IconButton` na 52×52dp; ikona unutar kontrole zadržava Material Icons `ArrowBack`, ne mijenja se hijerarhija Compose-a.
- Uklonjen semantički netočan `tr("close")` iz strelice, umjesto njega `tr("back")`.
- Dodan `back` ključ u kanonski katalog, 23 generirana Android `strings.xml` i `QREXStrings.kt`; svih 23 prijevoda ostaje offline i bez mreže.
- Oba detaljna zaslona zadržavaju `QrexDetailHeader`, potvrđene postojeće `qrex-detail-back` testTagove i pristup izvornim opcijama.
- Popis s 23 jezika + „sustavni” i dalje koristi `LazyColumn`, stvarne `Surface(onClick)` vrste ≥64dp i semantički odabran `RadioButton`.

## Validacija
- `tool/test_language_back_reference.py` statički potvrđuje 52dp, ispravan opis, svih 23 jezika i zajedničku navigacijsku komponentu za Language/Export.
- Postojeći stvarni Android `QREXNavigationStressTest` već provjerava `qrex-detail-back` na oba zaslona; potrebno je ponoviti instrumentirani test na API 26/31/34/35/36.
- Tek nakon exact-head native CI, 23 locale, 10 emulator snimki i API matrice promjena je spremna za merge.

**Ograničenje vizualnog zaključka:** nije numerički dokazano 1:1 s perspektivnim marketinškim referencama.
