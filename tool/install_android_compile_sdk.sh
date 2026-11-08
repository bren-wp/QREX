#!/usr/bin/env bash
# Android SDK 37 is distributed as platforms;android-37.0, not android-37.
# Explicit compatibility path protects Gradle API 37 lookup on hosted runners.
set -Eeuo pipefail
: "$ANDROID_HOME"
sdk_root="$ANDROID_HOME"
sdkmanager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
test -x "$sdkmanager"
if [[ ! -s "$sdk_root/platforms/android-37.0/android.jar" ]]; then
  "$sdkmanager" --install "platforms;android-37.0"
fi
test -s "$sdk_root/platforms/android-37.0/android.jar"
if [[ ! -e "$sdk_root/platforms/android-37" ]]; then
  ln -s "android-37.0" "$sdk_root/platforms/android-37"
fi
test -s "$sdk_root/platforms/android-37/android.jar"
echo "Android SDK 37.0 installed and available for compileSdk 37."
