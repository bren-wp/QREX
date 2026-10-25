# QREX — ekran 01: Splash i glavna ikona

**Vizualne reference korisnika (25. 10. 2026.):** `QREX_ Sve što može QR(3).png`, `QREX – Skeniraj, izradi, podijeli(5).png` i `QREX_ Sve QR funkcije na jednom mjestu(4).png`. To su **marketinške kompozicije sa slikama unutar okvira telefona**, a ne izvorni pojedinačni screenshotovi jednakih dimenzija. Prve dvije koriste bijelo Q/QR i cijan uglove; treća koristi alternativni kvadratni scanner simbol. U produkciji se zadržava odabrani **QREX Q/QR identitet**; varijante iz kompozicija nisu dopuštenje za mijenjanje brenda ili dodavanje iOS prikaza u Android aplikaciju.

## Usklađena produkcijska implementacija (screen 01)

| Element | Referentni namjerni izgled | Implementacija |
|---|---|---|
| Pozadina | Gotovo crno-plava s električno-plavim valovima i tankim svijetlim rubovima | Compose Canvas, dva gradijentna Bezier vala i svijetle rubne linije; bez statične fotografije ili mrežnog zahtjeva |
| Glavna ikona | Visoki zaobljeni električno-plavi kvadrat sa svijetlim Q, QR modulima, četiri cijan kutna nosača | Vektorski `qrex_icon.xml`, prilagođen zaobljeni gradijent, isto Q/QR crtanje kao adaptive launcher |
| Naslov | Veliki centrirani **QRE** bijelo i **X** plavo/cijan | Compose tekst; svedena razmaknica, dvobojni X; nikakav tekst unutar PNG-a |
| Slogan | Neposredno ispod natpisa | Pravi `tr("tagline")` u 23 jezika |
| Donji opis | Izdvojen ispod logotipa, iznad indikatorske linije | Pravi `tr("splash_message")`, ne zasebno hardkodirano ime razvojnog studija |
| Indikator | Kratka cijan traka preko tamnoplave | Stalan 42/94 dp indikator, bez izmišljenog učitavanja ili inertne animacije |
| Responzivnost | Jedan identitet na kompaktnim, standardnim i visokim ekranima | Funkcija `qrexSplashMetrics` za širinu, visinu i povećani font |
| Dostupnost | Tekst čitljiv, ikona bez duplog TalkBack opisa | Sadržaj ikone dekorativan, semantički naslov QREX, stvarni tekstovi i mjerljivi testTag ID-jevi |

## Testni kriteriji

- `qrexSplashMetrics(320, 568, 1f)`: logotip 156dp, naslov 50sp, donji razmak 26dp.
- `qrexSplashMetrics(393, 852, 1f)`: logotip 214dp, naslov 60sp, donji razmak 58dp.
- `qrexSplashMetrics(393, 852, 1.6f)`: naslov 38sp kako bi dugačak tekst ostao u okviru.
- Prema produkcijskom kodu, logotip je identičan na splash ekranu i drugim površinama; launcher zadržava maskiranje Android sustava, stoga ne može imati identičan crop na svim OEM uređajima.
- Screenshot mora biti stvaran Android capture na API 34, uz 10-ekranski CI gate i Kotlin + Python regresijske testove. Ni jedna marketinška kompozicija nije matematčki dokaz nulte razlike piksela; za takvu tvrdnju trebaju pojedinačne slike istih dimenzija i stanja.

**Sljedeći ekran:** Skener — okvir skeniranja, kamerni pregled, kontrola bljeskalice i galerija, dimenzije i pristupačnost.
