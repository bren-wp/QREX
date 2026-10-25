# QREX — ekran 04: Stvaranje QR koda, Android Compose

**Datum pregleda:** 25. 10. 2026.  
**Izvorna referenca:** tamnoplava korisnička QREX marketinška kompozicija s prikazom „Stvori QR kod”. Svijetla kompozicija prikazuje više vrsta QR koda u malim karticama, no nije odabrani produkcijski Android dark-first dizajn. Treća marketinška kompozicija prikazuje alternativni iOS-brending; ne mijenja Android Q/QR identitet.

## Zatečeno stanje

- Funkcionalni odabir četiri vrste (URL, Tekst, Wi-Fi, Kontakt) bio je vidljiv kao uski red ili responsivna 2×2 mreža; preostale četiri (E-mail, Telefon, Lokacija, Događaj) bile su dostupne **samo u izborniku zaglavlja**. To je problem otkrivanja funkcije, ne gubitak samih QR formata.
- Odabrani napredni format iz zaglavlja nije imao istaknuto stanje u mreži vidljivih vrsta.
- Polje za E-mail/Telefon koristilo je generičku tekstualnu tipkovnicu.
- Format datuma događaja uzimao je `Locale.getDefault()` bez obzira na korisnikov izbor jezika unutar QREX-a.
- Očuvani su postojeći fiksirani gumbi „Generiraj QR kod” i „Spremi na uređaj”, sigurnosna potvrda prije izvoza Wi-Fi podataka, QR pregled i responzivna forma uz IME.

## Implementirane promjene na radnoj grani

| Područje | Stvarni Compose ispravak | Mjerljiv kriterij |
|---|---|---|
| Dostupnost svih vrsta | Lokaliziran vidljivi „Više”/„More” gumb ispod četiri brze vrste prikazuje druge četiri vrste u 2×2 mreži; izbornik u zaglavlju ostaje dostupan | `qrex-create-more-types` ≥52dp visine; sve vrste imaju stvarni `Surface(onClick)` |
| Odabrani format | Semantički `selected` za sve napredne kartice; kada se iz zaglavlja odabere napredni format mreža se sama proširi | `qrex-create-type-3/4/5/7` semantički odabrane po pritisku |
| Tipkovnica | `KeyboardType.Uri` / `Email` / `Phone` ovisno o vrsti | Polja ostaju funkcionalna uz IME |
| Jezik datuma | `LocalLanguage.current` / `Locale.forLanguageTag`, ili sustavni jezik kada je odabrano „System” | Čuva svih 23 prijevoda bez novih neprijevodivih tekstova |
| Privatnost | Ponovno skrivanje Wi-Fi lozinke pri promjeni vrste; potvrda spremanja Wi-Fi QR-a ostaje | Nikada se ne izvozi Wi-Fi QR bez postojeće potvrde |
| Responzivnost | Isti `qrexLayoutProfile` za širine i povećani font; formatna mreža unutar scrollable forme, Generate/Save izvan nje | Akcijski gumbi dostupni uz mali viewport i otvorenu tipkovnicu |

## Što referenca ne dokazuje

Promotivni Android uređaji imaju umjetne okvire/perspektivu i različite gustoće. Ne postoji izdvojena neiskrivljena tamna snimka „Create” s poznatim Android viewportom, stanjem i fontScaleom. Zato ova promjena **nije verificirano podudaranje 1:1 za svaki piksel**; ispravljene su opipljive funkcionalne i geometrijske razlike. Nikakva nova marketinška grafika ili lažni emulator screenshot nije generiran.

## Regresijski kriteriji i release gate

- `tool/test_create_reference.py`: statičke provjere dostupnosti svih osam formata, visine disclosure gumba, fiksnih akcija, IME tipova, odabranog jezika i Wi-Fi sigurnosti.
- `QREXNavigationStressTest.advancedCreateTypesAreDiscoverableSelectedAndGenerateRealQr`: stvarni Android Compose odabir naprednih vrsta i generiranje E-mail QR rezultata.
- `QREXNavigationStressTest.createAndResultControlsHaveMeasuredTouchTargets`: prošireno mjerenje dodirne površine gumba „Više”.
- Obvezan prolaz `native-platforms` (Kotlin + JUnit, Android Lint, R8), lokalizacija, screenshot gate i API 26/31/34/35/36 prije mergea ili javnog izdanja.
- Potrebne su dodatne stvarne referentne ravne snimke zaslona za numeričku pixel-per-pixel usporedbu, TalkBack i provjera na fizičkim OEM uređajima.

**Izdanja:** nema promjene `versionName/versionCode` niti objave tijekom rada na ovom pojedinom ekranu. Posljednje objavljeno izdanje ostaje v1.0.15/115 dok sveobuhvatni release gate ne prođe.
