#!/usr/bin/env bash
# Produce verified QREX Play AAB, Play QA APK, and persistent-key Brendigo APK.
# No disposable signing keys and no claim that a third-party repo is official F-Droid.
set -euo pipefail
umask 077

if [ "$#" -ne 2 ]; then echo "Usage: $0 VERSION_NAME VERSION_CODE" >&2; exit 2; fi
VERSION="$1"
CODE="$2"
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ && "$CODE" =~ ^[0-9]+$ ]]; then
  echo "Invalid QREX release version or code." >&2; exit 2
fi
for name in QREX_UPLOAD_KEYSTORE_BASE64 QREX_UPLOAD_STORE_PASSWORD \
  QREX_UPLOAD_KEY_ALIAS QREX_UPLOAD_KEY_PASSWORD QREX_UPLOAD_CERT_SHA256 \
  QREX_FDROID_APK_KEYSTORE_BASE64 QREX_FDROID_APK_STORE_PASSWORD \
  QREX_FDROID_APK_KEY_ALIAS QREX_FDROID_APK_KEY_PASSWORD \
  QREX_FDROID_APK_CERT_SHA256; do
  if [ -z "$(printenv "$name" 2>/dev/null || true)" ]; then
    echo "Missing protected distribution secret $name. Nothing will be published." >&2
    exit 1
  fi
done
: "$RUNNER_TEMP" # Available on GitHub hosted runners.
BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
test -x "$BUILD_TOOLS/apksigner"
test -x "$BUILD_TOOLS/zipalign"
test -x "$BUILD_TOOLS/aapt"
mkdir -p distribution-output
WORK="$(mktemp -d "$RUNNER_TEMP/qrex-distribution.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT
printf '%s' "$QREX_UPLOAD_KEYSTORE_BASE64" | base64 --decode > "$WORK/play.jks"
printf '%s' "$QREX_FDROID_APK_KEYSTORE_BASE64" | base64 --decode > "$WORK/brendigo.jks"
test -s "$WORK/play.jks" && test -s "$WORK/brendigo.jks"

normalized() { tr -d '[:space:]:' | tr '[:lower:]' '[:upper:]'; }
cert_fingerprint() {
  local file="$1" alias="$2" pass_env="$3"
  keytool -exportcert -rfc -keystore "$file" -alias "$alias" -storepass:env "$pass_env" |
    openssl x509 -noout -fingerprint -sha256 | cut -d= -f2 | normalized
}
play_expected="$(printf '%s' "$QREX_UPLOAD_CERT_SHA256" | normalized)"
brendigo_expected="$(printf '%s' "$QREX_FDROID_APK_CERT_SHA256" | normalized)"
[[ "$play_expected" =~ ^[A-F0-9]{64}$ && "$brendigo_expected" =~ ^[A-F0-9]{64}$ ]]
test "$play_expected" != "$brendigo_expected" || {
  echo "Play upload and Brendigo APK certificates must be different." >&2; exit 1;
}
play_actual="$(cert_fingerprint "$WORK/play.jks" "$QREX_UPLOAD_KEY_ALIAS" QREX_UPLOAD_STORE_PASSWORD)"
brendigo_actual="$(cert_fingerprint "$WORK/brendigo.jks" "$QREX_FDROID_APK_KEY_ALIAS" QREX_FDROID_APK_STORE_PASSWORD)"
test "$play_actual" = "$play_expected" || { echo "Play upload cert mismatch"; exit 1; }
test "$brendigo_actual" = "$brendigo_expected" || { echo "Brendigo APK cert mismatch"; exit 1; }

# Unset all Gradle signing variables: produce a fresh unsigned APK and AAB,
# then sign each binary for its own distribution channel.
unset QREX_UPLOAD_STORE_FILE QREX_UPLOAD_STORE_PASSWORD_GRADLE
python3 tool/generate_localizations.py --check
python3 tool/check_native_release.py
gradle -p platforms/android --no-daemon --stacktrace \
  :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease

UNSIGNED_APK="platforms/android/app/build/outputs/apk/release/app-release-unsigned.apk"
UNSIGNED_AAB="platforms/android/app/build/outputs/bundle/release/app-release.aab"
test -s "$UNSIGNED_APK" && test -s "$UNSIGNED_AAB" || {
  echo "Expected unsigned Gradle outputs missing; refused to publish." >&2; exit 1;
}
test "$("$BUILD_TOOLS/aapt" dump badging "$UNSIGNED_APK" |
  sed -n "s/^package: name='\([^']*\)' versionCode='\([^']*\)' versionName='\([^']*\)'.*/\1 \2 \3/p" | head -1)" = "com.brendigo.qrex $CODE $VERSION"

PLAY_AAB="distribution-output/QREX-Android-$VERSION-Google-Play.aab"
PLAY_APK="distribution-output/QREX-Android-$VERSION-Google-Play-QA.apk"
BR_APK="distribution-output/QREX-Android-$VERSION-Brendigo-FDroid.apk"
jarsigner -keystore "$WORK/play.jks" \
  -storepass:env QREX_UPLOAD_STORE_PASSWORD -keypass:env QREX_UPLOAD_KEY_PASSWORD \
  -sigalg SHA256withRSA -digestalg SHA-256 -signedjar "$PLAY_AAB" \
  "$UNSIGNED_AAB" "$QREX_UPLOAD_KEY_ALIAS"
jarsigner -verify "$PLAY_AAB" > "$WORK/bundle-check.log"
grep -F 'jar verified.' "$WORK/bundle-check.log" >/dev/null

"$BUILD_TOOLS/zipalign" -p -f 4 "$UNSIGNED_APK" "$WORK/unsigned-aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$WORK/play.jks" \
  --ks-key-alias "$QREX_UPLOAD_KEY_ALIAS" \
  --ks-pass env:QREX_UPLOAD_STORE_PASSWORD --key-pass env:QREX_UPLOAD_KEY_PASSWORD \
  --out "$PLAY_APK" "$WORK/unsigned-aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$WORK/brendigo.jks" \
  --ks-key-alias "$QREX_FDROID_APK_KEY_ALIAS" \
  --ks-pass env:QREX_FDROID_APK_STORE_PASSWORD --key-pass env:QREX_FDROID_APK_KEY_PASSWORD \
  --out "$BR_APK" "$WORK/unsigned-aligned.apk"

for apk in "$PLAY_APK" "$BR_APK"; do
  "$BUILD_TOOLS/zipalign" -c -v 4 "$apk" >/dev/null
  "$BUILD_TOOLS/apksigner" verify --verbose --min-sdk-version 26 "$apk" >/dev/null
done
aab_fingerprint="$(keytool -printcert -jarfile "$PLAY_AAB" |
  sed -n 's/^[[:space:]]*SHA256: //p' | head -1 | normalized)"
play_apk_fingerprint="$("$BUILD_TOOLS/apksigner" verify --print-certs "$PLAY_APK" |
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -1 | normalized)"
br_apk_fingerprint="$("$BUILD_TOOLS/apksigner" verify --print-certs "$BR_APK" |
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -1 | normalized)"
test "$aab_fingerprint" = "$play_expected" && test "$play_apk_fingerprint" = "$play_expected"
test "$br_apk_fingerprint" = "$brendigo_expected"

# A third-party F-Droid repo can publish this signed APK after fdroid update.
# The official F-Droid repo MUST rebuild from source and must not include ML Kit.
mkdir -p "$WORK/fdroid-starter/repo" "$WORK/fdroid-starter/metadata"
cp "$BR_APK" "$WORK/fdroid-starter/repo/com.brendigo.qrex_$CODE.apk"
cp distribution/brendigo-fdroid/metadata/com.brendigo.qrex.yml "$WORK/fdroid-starter/metadata/"
cp distribution/brendigo-fdroid/README.md "$WORK/fdroid-starter/README.md"
cp distribution/brendigo-fdroid/config.yml.example "$WORK/fdroid-starter/config.yml.example"
(cd "$WORK/fdroid-starter" && zip -q -r "$GITHUB_WORKSPACE/distribution-output/QREX-$VERSION-Brendigo-FDroid-starter.zip" .)

cat > distribution-output/DISTRIBUTION-NOTES.txt <<EOF
QREX $VERSION / versionCode $CODE / package com.brendigo.qrex
Google Play upload certificate SHA-256: $play_expected
Brendigo direct APK certificate SHA-256: $brendigo_expected
Google-Play.aab: signed for uploading ONLY if the same upload fingerprint is registered in Play Console.
Google-Play-QA.apk: locally installable test APK; may not update Play-installed APKs.
Brendigo-FDroid.apk: independently signed APK for Brendigo's own F-Droid-compatible repository.
Brendigo-FDroid-starter.zip: APK and metadata; NOT a signed live F-Droid index and NOT a listing on official F-Droid.
Official F-Droid: NOT eligible while Google ML Kit proprietary barcode scanning is in the source.
Android installs with the same package ID but different certificates CANNOT normally update in place.
EOF
(cd distribution-output && sha256sum QREX-Android-* QREX-*-Brendigo-FDroid-starter.zip DISTRIBUTION-NOTES.txt > SHA256SUMS.txt)
echo "All release binaries verified; no private keys were copied into distribution-output/."
