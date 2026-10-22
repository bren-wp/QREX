#!/usr/bin/env bash
# Build the Play bundle using the existing authorized upload key.
set -euo pipefail
umask 077
: "${RELEASE_VERSION:?VersionName required}"
for name in QREX_UPLOAD_KEYSTORE_BASE64 QREX_UPLOAD_STORE_PASSWORD \
  QREX_UPLOAD_KEY_ALIAS QREX_UPLOAD_KEY_PASSWORD QREX_UPLOAD_CERT_SHA256; do
  if [ -z "${!name:-}" ]; then
    echo "Missing approved Android production signing configuration: $name" >&2
    exit 1
  fi
done
test "$GITHUB_REF" = refs/heads/main
[[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
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
  echo "Approved Play upload certificate SHA-256 must be provided." >&2
  exit 1
fi
key_cert="$(keytool -exportcert -rfc -keystore "$keystore" \
  -storepass "$QREX_UPLOAD_STORE_PASSWORD" -alias "$QREX_UPLOAD_KEY_ALIAS" | \
  openssl x509 -noout -fingerprint -sha256 | cut -d= -f2 | normalize_fingerprint)"
if [ "$key_cert" != "$expected" ]; then
  echo "Upload key differs from approved Play Console upload certificate." >&2
  exit 1
fi

gradle_file=platforms/android/app/build.gradle.kts
source_version="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$gradle_file" | head -1)"
source_code="$(sed -n 's/^[[:space:]]*versionCode = \([0-9]*\).*/\1/p' "$gradle_file" | head -1)"
test "$source_version" = "$RELEASE_VERSION"
[[ "$source_code" =~ ^[0-9]+$ ]] && test "$source_code" -ge 105

python3 tool/check_native_release.py
gradle -p platforms/android --no-daemon --stacktrace \
  :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
bundle="platforms/android/app/build/outputs/bundle/release/app-release.aab"
apk="platforms/android/app/build/outputs/apk/release/app-release.apk"
test -s "$bundle"
test -s "$apk"

# AAB uses the JAR signature format; APK uses Android v2/v3 signing.
jarsigner -verify "$bundle" > "$RUNNER_TEMP/qrex-aab-verify.txt"
grep -F 'jar verified.' "$RUNNER_TEMP/qrex-aab-verify.txt" >/dev/null
rm -f "$RUNNER_TEMP/qrex-aab-verify.txt"
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
apk_cert="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' \
  "$RUNNER_TEMP/qrex-apk-signature.txt" | head -n 1 | normalize_fingerprint)"
rm -f "$RUNNER_TEMP/qrex-apk-signature.txt"
if [ "$apk_cert" != "$expected" ]; then
  echo "APK certificate fingerprint differs from approved upload certificate." >&2
  exit 1
fi
badging="$("$build_tools/aapt" dump badging "$apk" |
  sed -n "s/^package: name='\([^']*\)' versionCode='\([^']*\)' versionName='\([^']*\)'.*/\1 \2 \3/p" |
  head -n 1)"
test "$badging" = "com.brendigo.qrex $source_code $source_version"

mkdir -p production
cp "$bundle" "production/QREX-Android-$RELEASE_VERSION.aab"
echo "Authorized Play upload AAB signature verified against the approved certificate."

