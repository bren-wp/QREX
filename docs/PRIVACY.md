# QREX Privacy Policy

**Last updated:** 17 October 2026  
**Developer:** Brendigo · https://brendigo.com  
**Platform:** Android only

QREX scans and creates QR codes on your Android device. It does not require registration, an account, advertising, analytics, tracking, or an internet connection.

## Data handled on your device

- **Camera:** QREX asks for camera access only when you tap **Enable camera**. Camera frames are processed locally through on-device ML Kit. Denial does not block scanning a selected gallery image.
- **Photos:** select a photo using the Android system picker; decoding stays on device. Images are size-bounded and decoded in a sampled form to reduce memory use.
- **QR contents:** scanned and generated URLs, text, Wi-Fi details, contacts and other payloads are processed locally. URLs are not opened automatically.
- **Optional history:** up to 250 items can be stored locally if enabled; Wi-Fi QR codes are **never automatically recorded**.
- **Favorites:** manually saved Wi-Fi QR codes can contain passwords. Their contents are encrypted with AES-GCM using an Android Keystore key. Existing Flutter Android history is migrated locally, only after a durable protected save.
- **Preferences:** language, theme and history settings are held on your device.

QREX does not operate a cloud backend that receives QR scans or history. Android backup is disabled in the app manifest. No internet, contact, location or broad storage permission is requested.

## Sharing and copying

Wi-Fi QR content may include a plaintext password. QREX asks for confirmation before sharing it to another app and marks Wi-Fi clipboard content as sensitive so Android can suppress preview displays. Clipboard flags cannot prevent all other software from accessing clipboard data; shared copies may persist in recipient apps.

## PDF/ZIP and PNG exports

Exports are created after you choose to export. A PDF/ZIP archive contains only your manually saved codes and **is not encrypted**. Cloud providers or recipients selected by you may store it separately. Deleting data in QREX does **not** erase already-exported copies.

## Rights and deletion

Settings allows disabling history and deleting locally saved QR data. Clearing all data also removes known pre-migration Flutter preferences to prevent old information from reappearing. If protected records cannot be decrypted, QREX avoids silently overwriting them. You may revoke camera permission in Android Settings.

## Support and updates

For privacy inquiries, use https://brendigo.com. This policy and changes appear in the public repository. [Terms of Use](TERMS.md).
