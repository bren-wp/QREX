#!/usr/bin/env bash
# Manual, fail-closed production gate. Never build or publish QA previews here.
set -euo pipefail
: "${GH_TOKEN:?GitHub token required}"
: "${GITHUB_REPOSITORY:?GitHub repository required}"
: "${GITHUB_SHA:?Exact commit required}"
test "$GITHUB_REF" = "refs/heads/main"
test "${PRODUCTION_APPROVED:-}" = "RELEASE_1_0_0"
test "${DEVICE_QA_CONFIRMED:-}" = "true"
test "${STORE_REVIEW_CONFIRMED:-}" = "true"
test "$(gh api "repos/$GITHUB_REPOSITORY/commits/main" --jq .sha)" = "$GITHUB_SHA"
if gh api "repos/$GITHUB_REPOSITORY/git/ref/tags/v1.0.0" >/dev/null 2>&1; then
  echo "v1.0.0 already tagged; refusing to overwrite production history." >&2
  exit 1
fi
if gh release view v1.0.0 --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  echo "v1.0.0 already released; refusing duplicate publication." >&2
  exit 1
fi
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
