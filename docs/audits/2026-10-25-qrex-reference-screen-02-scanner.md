# QREX — ekran 02: Skener uživo

**Referenca:** korisnikova tamna Android marketing kompozicija `QREX_ Sve što može QR(3).png`; sekundarno svijetla Android kompozicija `QREX – Skeniraj, izradi, podijeli(5).png`. Uzimamo Q/QR tamni identitet i donje gumbe u kamernom prikazu iz prve slike. Svijetla kompozicija ilustrira drugi koncept, ali nije razlog za vraćanje uklonjenog light-modea. Moguće razlike proporcija proizlaze i iz marketinškog okvira uređaja.

| Dio zaslona | Provedena korekcija | Provjera |
|---|---|---|
| Gornje zaglavlje | Zadržan stvarni QREX brand i aktivne ikone zaštite/izbornika | Postojeća lokalizacija i 48dp overflow |
| Prikaz kamere | CameraX `PreviewView` puni zaobljeni tamni panel, bez umjetnih pozadinskih kadrova | Živi prikaz, stvarni dekoder, permission handling |
| Kutovi skeniranja | Povećani na 61% kraće dimenzije, 3 sloja bijele i cijan svjetlosti | `ScannerCornerOverlay`, bez promjene obrađenih frameova |
| QR uputa | Unutar kamere, neposredno iznad gumba | `qrex-scan-hint`, lokalizirani tekst |
| Galerija i svjetiljka | Pomaknute u dno **unutar prikaza kamere**, dva široka zaobljena gumba s tamnom poluprozirnom pozadinom i cijan/white ikonom | Aktivni galerijski picker i asinkroni CameraX torch; testTag ID-jevi očuvani |
| Dostupnost bez kamere | Isti gumb za galeriju izvan zaštićenog područja dozvola, a torch isključen | `scannerActions(false)`, bez obvezne CAMERA dozvole za galeriju |
| Optički/digitalni zoom | Prikazuje se samo kad kamera potvrdi stvarni raspon >1×, i ne preklapa uputu ili gumbe | CameraX zoom bounds i ack ostaju nepromijenjeni |
| Responzivnost | 64dp visina kontrola na kompaktnom, 80dp na redovnom Android telefonu; tekst do dvije linije | Zadržan ≥48dp klik target |

**Sljedeći ekran:** Rezultat / detalji QR koda; zatim Stvori QR, Povijest i Više. **1:1** po brojčanim pikselima nije potvrdiv samo iz ukošenih marketinških mockupova; provjerava se stvarnim emulator screenshotovima i 10-ekranskim gateovima. Nema generiranih slika ili PNG zamjenskih kadrova.
