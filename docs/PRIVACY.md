# QREX Privacy Policy

**Last updated:** October 13, 2026  
**Developer:** Brendigo · https://brendigo.com

QREX is an Android and iOS application for scanning and generating QR codes locally. No account, registration or sign-in is required.

## Information processed on your device

- **Camera:** used to detect QR codes. The camera stream is processed locally; it is not uploaded to a QREX server. You may revoke camera permission in system settings.
- **Images:** you select an image through the operating system's photo picker. QR detection runs locally on the selected image and QREX does not upload it.
- **QR contents:** text, URLs, Wi-Fi credentials, contact details, locations and other input are processed locally when scanning or creating a code.
- **Optional history:** if enabled, QR contents and scan time can be stored in the application's local data, capped at 250 items. Wi-Fi QR codes are **not automatically recorded**. Manually saving a Wi-Fi code may also save its password.
- **Preferences:** your selected display theme, language and history preference are saved locally.

**Storage protection in the new Kotlin/Swift projects:** manually saved Wi-Fi payloads use AES-GCM encryption with a non-exportable Android Keystore key; iOS saves QR history in the device-only Keychain. The new projects attempt an on-device migration from the published Flutter history format. Previous plaintext records are removed only after migration, or unsafe secrets are excluded if encryption fails. The previously published Flutter release (v0.1.3) does **not** automatically gain these protections. Device access and sharing QR contents with other apps still pose privacy risks.

## Your PDF and ZIP exports

In the newer standalone Android and iOS editions, Settings offers optional PDF and ZIP export actions. After you explicitly confirm either format, QREX creates printable PDF pages containing only your **manually saved QR codes**. You can save the standalone PDF or a ZIP containing that PDF using your device's system file picker.

**The exported documents are not encrypted.** A manually saved Wi-Fi QR code can carry its password inside the QR artwork, and other saved codes may contain private data. Keep the ZIP secure, do not upload it to untrusted services, and share it only with authorized recipients. No QR document or archive is exported automatically. The ZIP is created on the device without QREX uploading its contents to a developer server. The chosen storage provider, Files app, cloud drive, or recipient may handle the exported document under its own privacy practices.

Deleting application data **does not delete previously exported PDF/ZIP documents**. Users should remove exported copies separately if they no longer need them.

## Data collection and sharing

QREX does not provide accounts, advertising, an analytics service or its own backend to collect QR content. The developer does not receive QR scan contents or device-local history through the app.

If you choose to open a URL, start a call, send an email, open a map or share information, another application or website may receive the content and apply its own privacy policy. QR URLs are not opened automatically.

## Retention and deletion

Local data persists until you delete it, uninstall QREX or the operating system removes application data. In **More → Settings** you may disable scan history and select **Delete all data** to remove local history, saved codes and preferences. Device or operating-system backups may contain copies depending on platform settings; the Android application backup is disabled in its manifest.

## Permissions

QREX requests camera permission for live scanning. Image access uses the platform-provided picker where available; scanning does not require the contacts, location or account permissions.

## Contact

For privacy-related requests, use the contact information at https://brendigo.com.

## Policy changes

Changes to this policy appear in the public repository and future app versions. An offline summary is available in the app. Store privacy declarations must be reevaluated for the exact binary and SDK versions submitted for review.
