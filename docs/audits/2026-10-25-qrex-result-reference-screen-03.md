# QREX — referentni zaslon 03: Rezultat

## Izvori i izbor dizajna

Korisničke tri reklamne kompozicije za QREX sadrže različite koncepte: dvije varijante plavog Q/QR logotipa i različite iOS/Android, svijetle/tamne te 4-/5-tab navigacije. Za aktualnu **Android aplikaciju** odabrana je dosljedna **tamna Android referenca**, iz kompozicija „QREX: Sve što može QR” i „QREX: Sve QR funkcije na jednom mjestu”. Ne kopira se iOS statusna traka ni nepostojeća peta kartica. Samostalni rezultat nema donju navigaciju, jednako kao u stvarnoj referenci.

Referentni zaslon redom prikazuje: gumb Natrag (lijevo), naslov Rezultat, izbornik (desno); **jednu veliku tamnoplavu karticu** s plavom ikonom vrste sadržaja, bijelim naslovnim URL-om i manjim sekundarnim URL-om; punu plavu glavnu radnju; ravne zaobljene redove Kopiraj, Podijeli, Prikaži QR kod, Spremi u favorite/Uredi te crvenkastu destruktivnu radnju. Druga kompozicija izostavlja favorite, a prva izostavlja uređivanje. **Obje postojeće stvarne funkcije zadržane su** radi UX konzistentnosti i zaštite podataka.

## Mjerne vrijednosti iz stvarnog Compose rasporeda

| Element | Android pravilo |
| --- | --- |
| Pozadina | `#061020` / `#030A18`, kartice `#111E32`, obrub `#27405F` |
| Zaglavlje | Centriran naslov s `21.sp`, priključeni Natrag `52.dp`, izbornik `52.dp` |
| Vanjski razmak | `16.dp` horizontalno, `10.dp` vertikalno na pomičnom sadržaju |
| Objedinjena kartica | 100% dostupne širine, radijus `18.dp`, minimalna visina `178.dp`, unutarnja vodoravna margina `16.dp` |
| Ikona sadržaja | `62.dp` plavi kvadrat s radijusom `14.dp`; bijeli piktogram `32.dp` |
| Tekst i podnaslov | `16.sp` bold, do 2 reda; `12.sp` sekundarni URL, ellipsis za vrlo dug sadržaj |
| Primarni gumb | `54.dp` minimum, radijus `14.dp`, pristupačni `#086BD8` s bijelim tekstom |
| Sekundarne radnje | `56.dp` najmanje, radijus `15.dp`, bijeli znak i tekst, `8.dp` vertikalni razmak |
| Brisanje | Stvarna interaktivna crvena akcija `56.dp` s crvenim tekstom/piktogramom i suptilnim crvenim obrubom, **potvrda je obavezna** |

## Funkcionalni i pristupačni uvjeti

- `qrex-result-summary`, `qrex-result-menu`, `qrex-result-primary`, `qrex-result-toggle-qr`, `qrex-result-delete` daju postojane oznake za automatiziranu provjeru stvarnih kontrola.
- Sve glavne klikabilne zone imaju fizičku visinu **barem 48dp**, a Natrag **52dp** da izdrži zaokruživanje na Androidu API 35.
- Gumb Natrag ostaje iznad scroll prikaza i tipkovnice. Dugi URL-ovi te povećani fontovi ne smiju prekrivati kontrole; sadržaj se pomiče, primarna ikona i kartica ostaju centrirane.
- Wi-Fi lozinka ne prikazuje se bez radnje Prikaži; spremanje i dijeljenje osjetljivih podataka ostaju iza potvrde. PDF/PNG i QR podaci ne cure u logove.
- Kopiranje koristi stvarni Android clipboard, dijeljenje Android chooser, otvaranje URL-a korisnikovo izričito pritiskanje, prikaz QR-a generira stvarni ZXing bitmap, favoriti i uređivanje zadržavaju izvorne callbackove, a brisanje traži potvrdu.

## Granica vizualne potvrde

Marketing-kompozicije s uređajima u perspektivi **nisu** izvorne ravne 1080×1920 referentne snimke. Omjeri i stvarna Compose geometrija mogu se provjeriti instrumentiranim Android testovima, no matematička tvrdnja o identičnosti **svakog piksela** zahtijeva izvornu sliku ekrana, potpuno identičan viewport i stanje uređaja te pokretanje `tool/compare_ui_pixels.py`. Nije zamijenjena slikovna ilustracija funkcionalnim UI-jem niti je generirana nova slika. Dosad usklađeni zasloni: **01 Splash/ikona** i **02 Skener**; ovaj PR donosi **03 Rezultat**. Ostali zasloni (04 Stvori, 05 Povijest, 06 Više) ostaju odvojeni koraci za detaljno usklađivanje.
