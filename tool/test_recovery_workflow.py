"""Manual QREX documentation recovery must never rebuild or alter release assets."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/recover-published-docs.yml"


class PublishedDocsRecoveryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text("utf-8")

    def test_only_explicit_operator_recovery_is_allowed(self):
        self.assertIn("workflow_dispatch:", self.workflow)
        self.assertNotIn("  push:", self.workflow)
        self.assertNotIn("  workflow_run:", self.workflow)
        self.assertIn("git merge-base --is-ancestor", self.workflow)
        self.assertIn('git/ref/tags/v$RELEASE_VERSION', self.workflow)
        self.assertIn(".target_commitish == $sha", self.workflow)

    def test_only_documentation_is_updated_after_tests(self):
        self.assertIn("tool/sync_published_release.py", self.workflow)
        self.assertIn("python3 -m unittest discover -s tool -p 'test_*.py'", self.workflow)
        self.assertIn("git add README.md RELEASE_NOTES.md docs/releases/INDEX.md", self.workflow)
        for forbidden in ("gh release create", "gh release upload", "gradle -p", "keytool -genkeypair", "git push --force"):
            self.assertNotIn(forbidden, self.workflow)


if __name__ == "__main__":
    unittest.main()
