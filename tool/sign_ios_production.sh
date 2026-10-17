#!/usr/bin/env bash
# Authorized App Store archive + export. Refuse an unsigned IPA or a wrong team.
set -euo pipefail
umask 077
for name in QREX_APPLE_CERT_P12_BASE64 QREX_APPLE_CERT_PASSWORD \
  QREX_APPLE_CERT_SHA256 QREX_APPLE_PROVISION_PROFILE_BASE64 \
  QREX_APPLE_TEAM_ID QREX_IOS_PROVISION_PROFILE_NAME; do
  if [ -z "${!name:-}" ]; then
    echo "Missing approved Apple distribution credential: $name" >&2
    exit 1
  fi
done
test "$GITHUB_REF" = refs/heads/main
work="$(mktemp -d "$RUNNER_TEMP/qrex-signing.XXXXXX")"
keychain="$work/qrex.keychain-db"
profile_destination=""
cleanup() {
  if [ -n "$profile_destination" ]; then rm -f "$profile_destination"; fi
  security delete-keychain "$keychain" >/dev/null 2>&1 || true
  rm -rf "$work"
}
trap cleanup EXIT
printf '%s' "$QREX_APPLE_CERT_P12_BASE64" | base64 --decode > "$work/distribution.p12"
printf '%s' "$QREX_APPLE_PROVISION_PROFILE_BASE64" | base64 --decode > "$work/profile.mobileprovision"
test -s "$work/distribution.p12"
test -s "$work/profile.mobileprovision"

normalize_fingerprint() {
  tr -d '[:space:]:' | tr '[:lower:]' '[:upper:]'
}
expected="$(printf '%s' "$QREX_APPLE_CERT_SHA256" | normalize_fingerprint)"
[[ "$expected" =~ ^[A-F0-9]{64}$ ]] || {
  echo "Missing valid approved Apple Distribution SHA-256 fingerprint" >&2
  exit 1
}
actual="$(openssl pkcs12 -in "$work/distribution.p12" -clcerts -nokeys \
  -passin env:QREX_APPLE_CERT_PASSWORD |
  openssl x509 -noout -fingerprint -sha256 | cut -d= -f2 | normalize_fingerprint)"
test "$actual" = "$expected" || {
  echo "Apple certificate fingerprint is not approved." >&2
  exit 1
}

security cms -D -i "$work/profile.mobileprovision" > "$work/profile.plist"
team=$(/usr/libexec/PlistBuddy -c 'Print :TeamIdentifier:0' "$work/profile.plist")
name=$(/usr/libexec/PlistBuddy -c 'Print :Name' "$work/profile.plist")
uuid=$(/usr/libexec/PlistBuddy -c 'Print :UUID' "$work/profile.plist")
app_id=$(/usr/libexec/PlistBuddy -c 'Print :Entitlements:application-identifier' "$work/profile.plist")
debug_entitlement=$(/usr/libexec/PlistBuddy -c 'Print :Entitlements:get-task-allow' "$work/profile.plist")
test "$team" = "$QREX_APPLE_TEAM_ID"
test "$name" = "$QREX_IOS_PROVISION_PROFILE_NAME"
test "$app_id" = "$QREX_APPLE_TEAM_ID.com.brendigo.qrex"
test "$debug_entitlement" = false

keychain_password="$(uuidgen)-$(uuidgen)"
security create-keychain -p "$keychain_password" "$keychain"
security set-keychain-settings -lut 21600 "$keychain"
security unlock-keychain -p "$keychain_password" "$keychain"
security import "$work/distribution.p12" -k "$keychain" \
  -P "$QREX_APPLE_CERT_PASSWORD" -T /usr/bin/codesign
security set-key-partition-list -S apple-tool:,apple: -s \
  -k "$keychain_password" "$keychain"
security list-keychains -d user -s "$keychain"

mkdir -p "$HOME/Library/MobileDevice/Provisioning Profiles"
profile_destination="$HOME/Library/MobileDevice/Provisioning Profiles/$uuid.mobileprovision"
cp "$work/profile.mobileprovision" "$profile_destination"
(cd platforms/ios && xcodegen generate --spec project.yml)
xcodebuild -project platforms/ios/QREX.xcodeproj -scheme QREX \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath "$work/QREX.xcarchive" \
  CODE_SIGN_STYLE=Manual CODE_SIGN_IDENTITY='Apple Distribution' \
  DEVELOPMENT_TEAM="$QREX_APPLE_TEAM_ID" \
  PROVISIONING_PROFILE_SPECIFIER="$QREX_IOS_PROVISION_PROFILE_NAME" archive

# Use export (not upload) so the IPA is inspectable and independently validated.
python3 - "$work/ExportOptions.plist" <<'PY'
import os, plistlib, sys
options = {
    "method": "app-store-connect",
    "destination": "export",
    "signingStyle": "manual",
    "teamID": os.environ["QREX_APPLE_TEAM_ID"],
    "provisioningProfiles": {
        "com.brendigo.qrex": os.environ["QREX_IOS_PROVISION_PROFILE_NAME"]
    },
    "manageAppVersionAndBuildNumber": False,
}
with open(sys.argv[1], "wb") as fp:
    plistlib.dump(options, fp)
PY
xcodebuild -exportArchive -archivePath "$work/QREX.xcarchive" \
  -exportOptionsPlist "$work/ExportOptions.plist" -exportPath "$work/Export"
ipa="$(find "$work/Export" -maxdepth 1 -type f -name '*.ipa' -print -quit)"
test -n "$ipa"
test -s "$ipa"
mkdir -p "$work/extracted"
unzip -q "$ipa" -d "$work/extracted"
app="$work/extracted/Payload/QREX.app"
test -d "$app"
codesign --verify --strict --deep "$app"
bundle=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app/Info.plist")
version=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$app/Info.plist")
build=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$app/Info.plist")
signed_team="$(codesign -dv --verbose=4 "$app" 2>&1 | sed -n 's/^TeamIdentifier=//p' | head -n 1)"
test "$bundle" = "com.brendigo.qrex"
test "$version" = "1.0.0"
test "$build" = "100"
test "$signed_team" = "$QREX_APPLE_TEAM_ID"
mkdir -p production
cp "$ipa" production/QREX-iOS-1.0.0.ipa
echo "Authorized iOS IPA signature, team, bundle and version verified."
