#!/usr/bin/env bash
# Manual, fail-closed production gate. Never build or publish QA previews here.
set -euo pipefail
: "${GH_TOKEN:?GitHub token required}"
: "${GITHUB_REPOSITORY:?GitHub repository required}"
: "${GITHUB_SHA:?Exact commit required}"
test "$GITHUB_REF" = "refs/heads/main"
test "${PRODUCTION_APPROVED:-}" = "PLAY_UPLOAD_1_0_1"
test "${DEVICE_QA_CONFIRMED:-}" = "true"
test "${STORE_REVIEW_CONFIRMED:-}" = "true"
test "$(gh api "repos/$GITHUB_REPOSITORY/commits/main" --jq .sha)" = "$GITHUB_SHA"
# The previous GitHub-only v1.0.0 tag is immutable; it is NOT a Play upload
# credential. Play's 1.0.1 bundle is prepared independently with the existing
# authorized upload key and is not auto-published to GitHub or Google Play.
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
    echo "Missing successful exact-commit checks; production publishing blocked." >&2
    exit 1
  fi
done
