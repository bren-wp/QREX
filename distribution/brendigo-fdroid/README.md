# Brendigo F-Droid-compatible repo — starter, NOT LIVE

The proposed public repo address is **not active or verified**:
`https://bren-wp.github.io/brendigo-fdroid/repo`

Do not share this as a functioning subscription address until the site and signed index exist.

## Prepare on a trusted Ubuntu/Debian machine

1. Install fdroidserver and its prerequisites according to the official F-Droid docs.
2. Create a **new private working directory**, NOT your checked-out public QREX project:
   ```sh
   mkdir -p "$HOME/brendigo-fdroid-work" && cd "$HOME/brendigo-fdroid-work"
   fdroid init
   mkdir -p repo metadata
   ```
3. Adjust the *generated* `config.yml` according to `config.yml.example`. Check `repo_url` matches the actual host path, not an assumed URL. Protect the generated repo signing keystore and config; keep secret files outside the public hosting directory.
4. Build and sign an **APK** with a persistent, private QREX APK key. The QREX 1.0.3 GitHub APK uses a disposable key and is **not** suitable for updates. Do not copy the Play AAB. Do not reuse the Google Play upload certificate without understanding the different signing roles.
5. Copy ONLY the final signed APK into `repo/`, and the metadata template to `metadata/com.brendigo.qrex.yml`. Revise descriptions to accurately disclose bundled nonfree libraries when relevant:
   ```sh
   fdroid update --create-metadata
   fdroid update
   ```
6. Inspect index files and repository fingerprint; import into an F-Droid client on a test device. Produce and test a **second, higher-versionCode APK signed with the same key** to validate updates.
7. Publish ONLY public `repo/` and public metadata/icons to a static HTTPS host. For GitHub Pages you can use a separate repository named `brendigo-fdroid`, with its `repo/` in Pages' deployed root. Confirm Pages settings and URL. Never publish `config.yml`, secrets or signing keystores.

## Two different signing keys (essential)
- **Repo signing key**: created by `fdroid init`, signs the repository index; client trusts its fingerprint.
- **APK signing key**: persistent Android keystore, signs QREX APK updates.
Neither is the Google Play upload key, and neither should be in public GitHub source.

## Channel incompatibility
The Google Play-installed APK, official F-Droid APK and Brendigo-repo APK can have different certificates despite identical `com.brendigo.qrex`. An in-place channel change will generally be rejected by Android. Uninstalling QREX can destroy saved history and Wi-Fi QR secrets. Retain backups/exports; do not encourage uninstall as a quick fix.

Official guide: https://f-droid.org/en/docs/Setup_an_F-Droid_App_Repo/
