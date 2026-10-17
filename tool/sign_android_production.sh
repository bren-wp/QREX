#!/usr/bin/env bash
# Produce BOTH Android AAB (Play upload) and standalone APK with the existing key.
# Both binaries must verify against the approved upload certificate; no debug builds.
set -euo pipefail
umask 077
for name in QREX_UPLOAD_KEYSTORE_BASE64 QREX_UPLOAD_STORE_PASSWORD \
  QREX_UPLOAD_KEY_ALIAS QREX_UPLOAD_KEY_PASSWORD QREX_UPLOAD_CERT_SHA256; do
  if [ -z "${!name:-}" ]; then
    echo "Missing approved Android production signing configuration: $name" >&2
    exit 1
  fi
done
test "$GITHUB_REF" = refs/heads/main
keystore="$RUNNER_TEMP/qrex-upload-existing.keystore"
trap 'rm -f "$keystore"' EXIT
printf '%s' "$QREX_UPLOAD_KEYSTORE_BASE64" | base64 --decode > "$keystore"
test -s "$keystore"
export QREX_UPLOAD_STORE_FILE="$keystore"

normalize_fingerprint() {
  tr -d '[:space:]:' | tr '[:lower:]' '[:upper:]'
}
expected="$(printf '%s' "$QREX_UPLOAD_CERT_SHA256" | normalize_fingerprint)"
if ! [[ "$expected" =~ ^[A-F0-9]{64}$ ]]; then
  echo "Expected authorized Play upload certificate SHA-256 must be provided." >&2
  exit 1
fi
key_cert="$(keytool -exportcert -rfc -keystore "$keystore" \
  -storepass "$QREX_UPLOAD_STORE_PASSWORD" -alias "$QREX_UPLOAD_KEY_ALIAS" |
  openssl x509 -noout -fingerprint -sha256 | cut -d= -f2 | normalize_fingerprint)"
if [ "$key_cert" != "$expected" ]; then
  echo "Upload key differs from approved Play Console upload certificate." >&2
  exit 1
fi

python3 tool/check_native_release.py
gradle -p platforms/android --no-daemon --stacktrace \
  :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
bundle="platforms/android/app/build/outputs/bundle/release/app-release.aab"
apk="platforms/android/app/build/outputs/apk/release/app-release.apk"
test -s "$bundle"
test -s "$apk"

# AAB uses the JAR signature format; APK uses Android v2/v3 signing.
# Verifying both prevents an unsigned package slipping into GitHub Releases.
jarsigner -verify -strict "$bundle" > /dev/null
binary_cert="$(keytool -printcert -jarfile "$bundle" |
  sed -n 's/^[[:space:]]*SHA256: //p' | head -n 1 | normalize_fingerprint)"
if [ "$binary_cert" != "$expected" ]; then
  echo "AAB certificate fingerprint differs from approved upload certificate." >&2
  exit 1
fi

build_tools="${ANDROID_HOME:?Android SDK required}/build-tools/36.0.0"
test -x "$build_tools/apksigner"
test -x "$build_tools/zipalign"
test -x "$build_tools/aapt"
"$build_tools/zipalign" -c -v 4 "$apk" > /dev/null
"$build_tools/apksigner" verify --verbose --print-certs --min-sdk-version 26 "$apk" > "$RUNNER_TEMP/qrex-apk-signature.txt"
apk_cert="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p'   "$RUNNER_TEMP/qrex-apk-signature.txt" | head -n 1 | normalize_fingerprint)"
rm -f "$RUNNER_TEMP/qrex-apk-signature.txt"
if [ "$apk_cert" != "$expected" ]; then
  echo "APK certificate fingerprint differs from approved upload certificate." >&2
  exit 1
fi
"$build_tools/aapt" dump badging "$apk" |
  grep -F "name='com.brendigo.qrex' versionCode='101' versionName='1.0.1'" > /dev/null

mkdir -p production
cp "$bundle" production/QREX-Android-1.0.1.aab
cp "$apk" production/QREX-Android-1.0.1.apk
echo "Authorized Play upload AAB and Android APK signatures verified against the approved certificate."
