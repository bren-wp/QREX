#!/usr/bin/env bash
# Publish native QA artifacts only when all independent GitHub checks pass.
set -euo pipefail
BRANCH="${QREX_RC_BRANCH:-fix/qrex-rc-integrity-and-scanner-0.2.3-20261014}"
TAG="${QREX_RC_TAG:-v0.2.3-rc.1}"
VERSION="${TAG#v}"
NOTES="docs/releases/${TAG}.md"
case "$TAG" in
  v*-rc.*) ;;
  *) echo "This workflow only publishes verified release candidates." >&2; exit 1;;
esac
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
  for workflow in native-platforms.yml capture-screenshots.yml generate-localizations.yml mobile.yml; do
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
test -s "$NOTES"
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

cp dist/android/debug/app-debug.apk publish/QREX-Android-debug-${VERSION}.apk
cp dist/android/aab/app-release.aab publish/QREX-Android-unsigned-${VERSION}.aab
cp dist/screens/android/android-create.png publish/QREX-Android-actual-screen-${VERSION}.png
cp dist/screens/ios/ios-create.png publish/QREX-iPhone-actual-screen-${VERSION}.png
(cd dist/ios/archive && zip -qr "$GITHUB_WORKSPACE/publish/QREX-iOS-unsigned-${VERSION}.xcarchive.zip" .)
test -s publish/QREX-iOS-unsigned-${VERSION}.xcarchive.zip
(cd publish && sha256sum QREX-* > SHA256SUMS.txt)
test -s publish/SHA256SUMS.txt
check_current_head
gh release create "$TAG" publish/QREX-* publish/SHA256SUMS.txt \
  --repo "$GITHUB_REPOSITORY" --target "$CI_SHA" \
  --title "QREX ${VERSION} — Android & iPhone preview, not store-signed" \
  --notes-file docs/releases/v${VERSION}.md --prerelease
release_id="$(gh api "repos/$GITHUB_REPOSITORY/releases/tags/$TAG" --jq .id)"
# GitHub can infer a release as 'latest' even with an RC tag. Pin its QA status.
gh api -X PATCH "repos/$GITHUB_REPOSITORY/releases/$release_id" \
  -F prerelease=true -f make_latest=false \
  -f name="QREX ${VERSION} — Native QA preview (not store-signed)" > /dev/null
test "$(gh api "repos/$GITHUB_REPOSITORY/releases/$release_id" --jq .prerelease)" = true
gh release view "$TAG" --repo "$GITHUB_REPOSITORY" --json url,tagName,isPrerelease,assets
