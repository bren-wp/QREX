#!/usr/bin/env bash
# Manual, fail-closed production gate. Never build or publish QA previews here.
set -euo pipefail
: "${GH_TOKEN:?GitHub token required}"
: "${GITHUB_REPOSITORY:?GitHub repository required}"
: "${GITHUB_SHA:?Exact commit required}"
: "${RELEASE_VERSION:?VersionName required}"
test "$GITHUB_REF" = "refs/heads/main"
[[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
test "${PRODUCTION_APPROVED:-}" = "PLAY_UPLOAD_$RELEASE_VERSION"
test "${DEVICE_QA_CONFIRMED:-}" = "true"
test "${STORE_REVIEW_CONFIRMED:-}" = "true"
test "$(gh api "repos/$GITHUB_REPOSITORY/commits/main" --jq .sha)" = "$GITHUB_SHA"

gradle_file=platforms/android/app/build.gradle.kts
source_version="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' "$gradle_file" | head -1)"
source_code="$(sed -n 's/^[[:space:]]*versionCode = \([0-9]*\).*/\1/p' "$gradle_file" | head -1)"
test "$source_version" = "$RELEASE_VERSION"
[[ "$source_code" =~ ^[0-9]+$ ]] && test "$source_code" -ge 105
echo "Authorized Play candidate: $source_version / versionCode $source_code"
python3 tool/check_native_release.py
python3 -m unittest discover -s tool -p 'test_*.py'
python3 tool/generate_localizations.py --check
python3 tool/check_translation_usage.py

# Every required workflow must have succeeded on exactly the release commit.
for workflow in native-platforms.yml capture-screenshots.yml generate-localizations.yml; do
  result="$(gh api "repos/$GITHUB_REPOSITORY/actions/workflows/$workflow/runs?branch=main&head_sha=$GITHUB_SHA&per_page=30" |
    jq -r '[.workflow_runs[] | select(.head_sha == env.GITHUB_SHA)] | sort_by(.id) | last |
      if . == null then "" else (.status + ":" + (.conclusion // "pending")) end')"
  echo "$workflow: ${result:-missing}"
  if [ "$result" != "completed:success" ]; then
    echo "Missing successful exact-commit checks; production build blocked." >&2
    exit 1
  fi
done

