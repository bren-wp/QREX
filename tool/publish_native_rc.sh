#!/usr/bin/env bash
# Publish native QA artifacts only when all independent GitHub checks pass.
set -euo pipefail
BRANCH="fix/native-release-hardening-20261013"
TAG="v0.2.0-rc.1"
test -n "$GH_TOKEN"
test -n "$CI_SHA"
test -n "$GITHUB_REPOSITORY"
python3 tool/check_native_release.py
python3 -m unittest discover -s tool -p 'test_*.py'

check_current_head() {
  local head
  head="$(gh api "repos/$GITHUB_REPOSITORY/commits/$BRANCH" --jq .sha)"
  if [ "$head" != "$CI_SHA" ]; then
    echo "Branch head changed. Refusing stale pre-release."
    exit 1
  fi
}
check_current_head
platform_run=""
screen_run=""
for attempt in $(seq 1 100); do
  check_current_head
  ready=true
  for workflow in native-platforms.yml capture-screenshots.yml generate-localizations.yml; do
    entry="$(gh run list --repo "$GITHUB_REPOSITORY" \
      --workflow "$workflow" --branch "$BRANCH" --limit 50 \
      --json databaseId,headSha,status,conclusion |
      jq -r --arg sha "$CI_SHA" \
      '[.[] | select(.headSha == $sha)] | sort_by(.databaseId) | last |
       if . == null then "" else [.databaseId, .status, (.conclusion // "pending")] | @tsv end')"
    if [ -z "$entry" ]; then
      echo "Pending $workflow for $CI_SHA"
      ready=false
      continue
    fi
    IFS=$'\t' read -r run_id status result <<< "$entry"
    echo "$workflow: $run_id $status $result"
    if [ "$status" = "completed" ] && [ "$result" != "success" ]; then
      echo "Required CI failed or was cancelled; no release."
      exit 1
    fi
    if [ "$status" != "completed" ]; then
      ready=false
    elif [ "$workflow" = "native-platforms.yml" ]; then
      platform_run="$run_id"
    elif [ "$workflow" = "capture-screenshots.yml" ]; then
      screen_run="$run_id"
    fi
  done
  if [ "$ready" = true ]; then break; fi
  if [ "$attempt" -eq 100 ]; then
    echo "QA timeout; no release."
    exit 1
  fi
  sleep 50
done

test -n "$platform_run"
test -n "$screen_run"
check_current_head
if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  echo "Candidate tag already published; will not replace it."
  exit 1
fi
test -s docs/releases/v0.2.0-rc.1.md
mkdir -p dist/android/debug dist/android/aab dist/ios/archive dist/screens/android dist/screens/ios publish
gh run download "$platform_run" --repo "$GITHUB_REPOSITORY" \
  --name QREX-Kotlin-Android-debug --dir dist/android/debug
gh run download "$platform_run" --repo "$GITHUB_REPOSITORY" \
  --name QREX-Kotlin-Android-unsigned-AAB-QA --dir dist/android/aab
gh run download "$platform_run" --repo "$GITHUB_REPOSITORY" \
  --name QREX-iOS-unsigned-archive-QA --dir dist/ios/archive
gh run download "$screen_run" --repo "$GITHUB_REPOSITORY" \
  --name qrex-actual-android --dir dist/screens/android
gh run download "$screen_run" --repo "$GITHUB_REPOSITORY" \
  --name qrex-actual-ios --dir dist/screens/ios
test -s dist/android/debug/app-debug.apk
test -s dist/android/aab/app-release.aab
test -s dist/ios/archive/Products/Applications/QREX.app/QREX
test -s dist/screens/android/android-create.png
test -s dist/screens/ios/ios-create.png
python3 tool/verify_screenshots.py dist/screens/android/android-create.png dist/screens/ios/ios-create.png

cp dist/android/debug/app-debug.apk publish/QREX-Android-debug-0.2.0-rc.1.apk
cp dist/android/aab/app-release.aab publish/QREX-Android-unsigned-0.2.0-rc.1.aab
cp dist/screens/android/android-create.png publish/QREX-Android-actual-screen-0.2.0-rc.1.png
cp dist/screens/ios/ios-create.png publish/QREX-iPhone-actual-screen-0.2.0-rc.1.png
(cd dist/ios/archive && zip -qr "$GITHUB_WORKSPACE/publish/QREX-iOS-unsigned-0.2.0-rc.1.xcarchive.zip" .)
test -s publish/QREX-iOS-unsigned-0.2.0-rc.1.xcarchive.zip
(cd publish && sha256sum QREX-* > SHA256SUMS.txt)
test -s publish/SHA256SUMS.txt
check_current_head
gh release create "$TAG" publish/QREX-* publish/SHA256SUMS.txt \
  --repo "$GITHUB_REPOSITORY" --target "$CI_SHA" \
  --title "QREX 0.2.0 RC 1 — Native Android & iOS QA preview" \
  --notes-file docs/releases/v0.2.0-rc.1.md --prerelease
gh release view "$TAG" --repo "$GITHUB_REPOSITORY" --json url,tagName,isPrerelease,assets
