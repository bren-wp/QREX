# QREX Privacy Policy

**Last updated:** 13 November 2026  
**Developer:** Brendigo · https://brendigo.com  
**Platform:** Android only

QREX scans and creates QR codes on your Android device. It does not require registration, an account, advertising, analytics, tracking, or an internet connection.

## Data handled on your device

- **Camera:** QREX asks for camera access only when you tap **Enable camera**. Camera frames are decoded locally with CameraX and ZXing. Denial does not block scanning a selected gallery image.
- **Photos:** select a photo using the Android system picker; decoding stays on device. Images are size-bounded and decoded in a sampled form to reduce memory use.
- **QR contents:** scanned and generated URLs, text, Wi-Fi details, contacts, locations and calendar events are processed locally. URLs are not opened automatically. If you tap a recognized web link, QREX hands it to an app you choose; any network activity then takes place outside QREX.
- **Optional history:** up to 250 items can be stored locally if enabled; Wi-Fi QR codes are **never automatically recorded**.
- **History and favorites:** starting with the 1.0.48 candidate, all stored QR payload types (including URLs, free text, contacts, Wi-Fi and scanned history) are encrypted with AES-GCM using the existing Android Keystore key. Previous protected Wi-Fi records remain decryptable without rotating the key. Plaintext history from older native or Flutter versions is migrated locally using a durable preferences commit. If decryption or encryption fails, QREX will not silently overwrite unreadable history.
- **Preferences:** language, History and System/Dark/Light appearance settings are stored only on your device. Preference changes are confirmed after a durable local save.

QREX does not operate a cloud backend that receives QR scans or history. Android backup is disabled in the app manifest. No internet, contact, location or broad storage permission is requested.

## Sharing, copying and calendar actions

When you tap **Share**, QREX sends the selected QR text to the app you choose; QREX does not upload it to a server. Wi-Fi QR content may include a plaintext password. QREX asks for explicit confirmation before sharing Wi-Fi content. **Every copied QR code** (including private URLs, contacts, text and Wi-Fi passwords) is marked sensitive so Android 13+ can hide its system clipboard preview. Copying still places the original readable text on the operating-system clipboard; this flag does **not** encrypt or automatically clear it, and other apps may access copied or shared content.

Calendar-event QR content is processed on device. If you tap **Add to calendar**, QREX passes the event title, start/end time and any location or details you entered to the calendar app selected on your device using Android's standard event-insert action. QREX does not request calendar read or write permission. The selected calendar app controls its own storage and privacy practices.

## Document Scanner (development branch PR #120; not in v1.0.49)

The proposed document-scanning workflow captures paper pages with CameraX or reads images selected through the Android Photo Picker. Images and edited page bitmaps remain in application memory for the document session, including ordinary Activity recreation. QREX does not upload these images and does not create intermediate plaintext photo files on disk. Up to eight pages are allowed, with each held image bounded in resolution. A full Android process termination may discard a session; unsaved scans are **not** a durable backup. Leaving the session requests confirmation before discarding pages.

PDF creation starts only after you select an output destination with Android's Storage Access Framework. Exported PDFs are **not encrypted** with the app's protected QR history key. Files are readable by the destination provider and by apps you intentionally grant access to; cloud destinations may upload them independently of QREX. Review the destination before exporting or sharing documents that contain personal data. A partially written export may remain if the destination fails during writing.

## PDF and PNG exports

Exports are created only after you choose to export. A PDF archive contains only your manually saved codes and **is not encrypted**. A saved PNG likewise contains readable QR data and **is not encrypted**. QREX warns before exporting a Wi-Fi QR image because it may encode a password. Cloud providers or recipients selected by you may store it separately. Deleting data in QREX does **not** erase already-exported copies.

## Rights and deletion

Settings allows disabling history and deleting local history and saved QR codes. Clearing history also removes its legacy QR records so deleted information does not reappear; language choice remains saved. If protected records cannot be decrypted, QREX avoids silently overwriting them. This on-device encryption does not protect exported PDFs or PNGs, the Android clipboard, or a compromised/unlocked device. You may revoke camera permission in Android Settings.

## Support and updates

For privacy inquiries, use https://brendigo.com. This policy and changes appear in the public repository. [Terms of Use](TERMS.md).

