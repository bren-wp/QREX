#!/usr/bin/env bash
# Retire legacy QREX GitHub releases/tags only AFTER an authenticated, signed
# v1.0.0 release is published. This is deliberately not a QA publisher.
set -euo pipefail

: "${GH_TOKEN:?GitHub API token required}"
: "${GITHUB_REPOSITORY:?GitHub repository required}"
: "${GITHUB_SHA:?Release commit SHA required}"
test "$GITHUB_REPOSITORY" = "bren-wp/QREX"
test "$GITHUB_REF" = "refs/heads/main"
test "${QREX_LEGACY_RETIRE_APPROVED:-}" = "after-signed-v1.0.0"
[[ "$GITHUB_SHA" =~ ^[a-f0-9]{40}$ ]]

stable="$(gh api "repos/$GITHUB_REPOSITORY/releases/tags/v1.0.0")"
# Never delete historic downloads before both expected files are published
# on a non-draft, non-prerelease tag pointing at THIS audited commit.
jq -e --arg sha "$GITHUB_SHA" '
    .tag_name == "v1.0.0"
    and .target_commitish == $sha
    and .prerelease == false
    and .draft == false
    and ([.assets[].name] | sort) ==
        (["QREX-Android-1.0.0.aab", "SHA256SUMS.txt"] | sort)
    and all(.assets[]; .size > 0)
' <<<"$stable" >/dev/null
test "$(gh api "repos/$GITHUB_REPOSITORY/git/ref/tags/v1.0.0" --jq .object.sha)" = "$GITHUB_SHA"
test "$(gh api "repos/$GITHUB_REPOSITORY/commits/main" --jq .sha)" = "$GITHUB_SHA"

# First remove *other* GitHub Releases. Keep the stable release at all times.
# All input comes from GitHub API; IDs are numeric and never eval'ed.
while IFS=$'\t' read -r id tag; do
  test -n "$id" || continue
  [[ "$id" =~ ^[0-9]+$ ]]
  test "$tag" != "v1.0.0"
  echo "Retiring legacy GitHub Release $tag (id $id)"
  gh api --method DELETE "repos/$GITHUB_REPOSITORY/releases/$id" >/dev/null
done < <(
  gh api --paginate "repos/$GITHUB_REPOSITORY/releases?per_page=100" |
    jq -rs 'add | .[] | select(.tag_name != "v1.0.0") |
      [.id, .tag_name] | @tsv'
)

# Delete all obsolete Git tag refs, including tags without a release.
while IFS= read -r ref; do
  test -n "$ref" || continue
  [[ "$ref" == refs/tags/* ]]
  name="${ref#refs/tags/}"
  test -n "$name"
  if [ "$name" != "v1.0.0" ]; then
    echo "Retiring legacy Git tag $name"
    gh api --method DELETE "repos/$GITHUB_REPOSITORY/git/refs/tags/$name" >/dev/null
  fi
done < <(
  gh api --paginate "repos/$GITHUB_REPOSITORY/git/matching-refs/tags" |
    jq -r '.[] | .ref'
)

# Confirm exactly one published GitHub Release and one tag remain.
remaining_releases="$(gh api --paginate "repos/$GITHUB_REPOSITORY/releases?per_page=100" |
  jq -rs '[add[] | .tag_name]')"
remaining_tags="$(gh api --paginate "repos/$GITHUB_REPOSITORY/git/matching-refs/tags" |
  jq -rs '[.[][] | .ref]')"
jq -e '. == ["v1.0.0"]' <<<"$remaining_releases" >/dev/null
jq -e '. == ["refs/tags/v1.0.0"]' <<<"$remaining_tags" >/dev/null
echo "Retired obsolete releases and tags; only signed QREX v1.0.0 remains."
