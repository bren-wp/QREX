# QREX Privacy Policy

**Last updated:** 22 October 2026  
**Developer:** Brendigo · https://brendigo.com  
**Platform:** Android only

QREX scans and creates QR codes on your Android device. It does not require registration, an account, advertising, analytics, tracking, or an internet connection.

## Data handled on your device

- **Camera:** QREX asks for camera access only when you tap **Enable camera**. Camera frames are decoded locally with CameraX and ZXing. Denial does not block scanning a selected gallery image.
- **Photos:** select a photo using the Android system picker; decoding stays on device. Images are size-bounded and decoded in a sampled form to reduce memory use.
- **QR contents:** scanned and generated URLs, text, Wi-Fi details, contacts, locations and calendar events are processed locally. URLs are not opened automatically. If you tap a recognized web link, QREX hands it to an app you choose; any network activity then takes place outside QREX.
- **Optional history:** up to 250 items can be stored locally if enabled; Wi-Fi QR codes are **never automatically recorded**.
- **Favorites:** manually saved Wi-Fi QR codes can contain passwords. Their contents are encrypted with AES-GCM using an Android Keystore key. Existing Flutter Android history is migrated locally, only after a durable protected save.
- **Preferences:** language, theme and history settings are held on your device.

QREX does not operate a cloud backend that receives QR scans or history. Android backup is disabled in the app manifest. No internet, contact, location or broad storage permission is requested.

## Sharing, copying and calendar actions

When you tap **Share**, QREX sends the selected QR text to the app you choose; QREX does not upload it to a server. Wi-Fi QR content may include a plaintext password. QREX asks for confirmation before sharing Wi-Fi content and marks Wi-Fi clipboard content as sensitive so Android can suppress preview displays. Clipboard flags cannot prevent all other software from accessing clipboard data; shared or copied content may persist in recipient apps.

Calendar-event QR content is processed on device. If you tap **Add to calendar**, QREX passes the event title, start/end time and any location or details you entered to the calendar app selected on your device using Android's standard event-insert action. QREX does not request calendar read or write permission. The selected calendar app controls its own storage and privacy practices.

## PDF/ZIP and PNG exports

Exports are created after you choose to export. A PDF/ZIP archive contains only your manually saved codes and **is not encrypted**. Cloud providers or recipients selected by you may store it separately. Deleting data in QREX does **not** erase already-exported copies.

## Rights and deletion

Settings allows disabling history and deleting local history and saved QR codes. Clearing history also removes its legacy QR records so deleted information does not reappear; theme and language choices remain saved. If protected records cannot be decrypted, QREX avoids silently overwriting them. You may revoke camera permission in Android Settings.

## Support and updates

For privacy inquiries, use https://brendigo.com. This policy and changes appear in the public repository. [Terms of Use](TERMS.md).

