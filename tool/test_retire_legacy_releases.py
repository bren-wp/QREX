#!/usr/bin/env python3
"""Execute legacy release/tag cleanup against an isolated fake GitHub API.

No real GitHub API, release, tag or credential is accessed by this test.
"""
from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
CLEANUP = ROOT / "tool/retire_legacy_releases.sh"
SHA = "a" * 40
STABLE = {
    "id": 1000,
    "tag_name": "v1.0.0",
    "target_commitish": SHA,
    "prerelease": False,
    "draft": False,
    "assets": [
        {"name": "QREX-Android-1.0.0.aab", "size": 100},
        {"name": "QREX-iOS-1.0.0.ipa", "size": 100},
        {"name": "SHA256SUMS.txt", "size": 100},
    ],
}
FAKE_GH = r'''#!/usr/bin/env python3
import json, os, pathlib, sys

args = sys.argv[1:]
if not args or args[0] != "api":
    sys.exit("unexpected non-API GitHub command")
args = args[1:]
state_file = pathlib.Path(os.environ["MOCK_QREX_GH_STATE"])
state = json.loads(state_file.read_text())
def save():
    state_file.write_text(json.dumps(state))
if "--method" in args:
    method = args[args.index("--method") + 1]
else:
    method = "GET"
if "--paginate" in args:
    endpoint = args[args.index("--paginate") + 1]
else:
    endpoint = next((a for a in args if a.startswith("repos/")), "")
if not endpoint:
    sys.exit("missing API URL")

if method == "DELETE":
    if endpoint.endswith(os.environ.get("MOCK_QREX_FAIL_DELETE", "#no-match")):
        sys.exit(7)
    if "/releases/" in endpoint:
        old_id = int(endpoint.rsplit("/", 1)[-1])
        state["releases"] = [x for x in state["releases"] if x["id"] != old_id]
    elif "/git/refs/tags/" in endpoint:
        ref = "refs/tags/" + endpoint.split("/git/refs/tags/", 1)[1]
        state["tags"] = [x for x in state["tags"] if x != ref]
    else:
        sys.exit("unexpected DELETE API")
    save()
    sys.exit(0)
if endpoint.endswith("/releases/tags/v1.0.0"):
    current = next((x for x in state["releases"] if x["tag_name"] == "v1.0.0"), None)
    if not current:
        sys.exit(1)
    print(json.dumps(current))
elif endpoint.endswith("/git/ref/tags/v1.0.0"):
    if "refs/tags/v1.0.0" not in state["tags"]:
        sys.exit(1)
    print(SHA if False else state["main_sha"])
elif endpoint.endswith("/commits/main"):
    print(state["main_sha"])
elif "/releases?per_page" in endpoint:
    print(json.dumps(state["releases"]))
elif endpoint.endswith("/git/matching-refs/tags"):
    print(json.dumps([{"ref": tag} for tag in state["tags"]]))
else:
    sys.exit("unexpected GET API: " + endpoint)
'''


class LegacyReleaseCleanupTests(unittest.TestCase):
    def state(self):
        return {
            "main_sha": SHA,
            "releases": [
                STABLE.copy(),
                {"id": 98, "tag_name": "v0.1.3"},
                {"id": 99, "tag_name": "v0.2.4-rc.1"},
            ],
            "tags": [
                "refs/tags/v1.0.0",
                "refs/tags/v0.1.3",
                "refs/tags/v0.2.4-rc.1",
                "refs/tags/orphan",
            ],
        }

    def perform(self, state, **env_updates):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            gh = folder / "gh"
            gh.write_text(FAKE_GH)
            gh.chmod(0o755)
            saved = folder / "state.json"
            saved.write_text(json.dumps(state))
            env = dict(os.environ)
            env.update({
                "PATH": f"{folder}:{env.get('PATH', '')}",
                "MOCK_QREX_GH_STATE": str(saved),
                "GH_TOKEN": "nonproduction-test-token",
                "GITHUB_REPOSITORY": "bren-wp/QREX",
                "GITHUB_REF": "refs/heads/main",
                "GITHUB_SHA": SHA,
                "QREX_LEGACY_RETIRE_APPROVED": "after-signed-v1.0.0",
            })
            env.update(env_updates)
            result = subprocess.run(
                ["bash", str(CLEANUP)], cwd=ROOT, env=env,
                text=True, capture_output=True, timeout=15,
            )
            return result, json.loads(saved.read_text())

    def test_removes_every_obsolete_release_and_tag_but_not_1_0_0(self):
        outcome, state = self.perform(self.state())
        self.assertEqual(outcome.returncode, 0, outcome.stderr)
        self.assertEqual([x["tag_name"] for x in state["releases"]], ["v1.0.0"])
        self.assertEqual(state["tags"], ["refs/tags/v1.0.0"])

    def test_stable_release_missing_does_not_delete_old_assets(self):
        original = self.state()
        original["releases"] = original["releases"][1:]
        outcome, changed = self.perform(original)
        self.assertNotEqual(outcome.returncode, 0)
        self.assertEqual(original, changed)

    def test_stable_release_wrong_commit_does_not_delete(self):
        original = self.state()
        original["releases"][0]["target_commitish"] = "b" * 40
        outcome, changed = self.perform(original)
        self.assertNotEqual(outcome.returncode, 0)
        self.assertEqual(original, changed)

    def test_stable_release_unsigned_or_missing_assets_does_not_delete(self):
        original = self.state()
        original["releases"][0]["assets"] = [{"name": "QREX-Android-debug.apk", "size": 99}]
        outcome, changed = self.perform(original)
        self.assertNotEqual(outcome.returncode, 0)
        self.assertEqual(original, changed)

    def test_stop_on_tag_delete_error_keep_signed_release(self):
        result, state = self.perform(self.state(), MOCK_QREX_FAIL_DELETE="refs/tags/orphan")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([x["tag_name"] for x in state["releases"]], ["v1.0.0"])
        self.assertIn("refs/tags/v1.0.0", state["tags"])
        # Safe recovery retries only remaining old tags.
        retry, state = self.perform(state)
        self.assertEqual(retry.returncode, 0, retry.stderr)
        self.assertEqual(state["tags"], ["refs/tags/v1.0.0"])


if __name__ == "__main__":
    unittest.main()
