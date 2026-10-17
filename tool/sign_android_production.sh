#!/usr/bin/env bash
# Produce a Google Play upload-signed AAB using ONLY the existing authorized key.
# No unsigned/debug asset is eligible for a GitHub stable release.
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
  :app:testReleaseUnitTest :app:lintRelease :app:bundleRelease
bundle="platforms/android/app/build/outputs/bundle/release/app-release.aab"
test -s "$bundle"
jarsigner -verify -strict "$bundle" > /dev/null
binary_cert="$(keytool -printcert -jarfile "$bundle" |
  sed -n 's/^[[:space:]]*SHA256: //p' | head -n 1 | normalize_fingerprint)"
if [ "$binary_cert" != "$expected" ]; then
  echo "AAB certificate fingerprint differs from approved upload certificate." >&2
  exit 1
fi
mkdir -p production
cp "$bundle" production/QREX-Android-1.0.0.aab
echo "Authorized Android AAB certificate verified against known fingerprint."
