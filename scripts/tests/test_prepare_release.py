"""Exercise release signing with real Android tools and a disposable key."""

import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest


@unittest.skipUnless(os.environ.get("STIL_TEST_APK") and os.environ.get("ANDROID_BUILD_TOOLS"),
                     "Set STIL_TEST_APK and ANDROID_BUILD_TOOLS to run real signing tests")
class ReleaseSigningTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="stil-release-tests-")
        cls.root = Path(cls.temp.name)
        cls.tools = Path(os.environ["ANDROID_BUILD_TOOLS"])
        cls.apk = Path(os.environ["STIL_TEST_APK"]).resolve()
        cls.script = Path(__file__).resolve().parents[1] / "prepare_release.py"
        cls.key = cls.root / "disposable.jks"
        cls.password = "disposable-test-password"
        subprocess.run(["keytool", "-genkeypair", "-keystore", str(cls.key), "-alias", "test",
                        "-storepass", cls.password, "-keypass", cls.password, "-keyalg", "RSA",
                        "-keysize", "2048", "-validity", "2", "-dname", "CN=Stil signing test"],
                       check=True, capture_output=True)
        certificate = subprocess.check_output(["keytool", "-exportcert", "-keystore", str(cls.key),
                                               "-alias", "test", "-storepass", cls.password])
        cls.fingerprint = hashlib.sha256(certificate).hexdigest()
        badging = subprocess.check_output([str(cls.tools / "aapt"), "dump", "badging", str(cls.apk)], text=True)
        cls.tag = "v" + re.search(r"versionName='([^']+)'", badging)[1]

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def setUp(self):
        self.case = tempfile.TemporaryDirectory(dir=self.root)
        self.addCleanup(self.case.cleanup)
        self.output = Path(self.case.name) / "release"
        self.env = dict(os.environ, STIL_KEYSTORE_BASE64=base64.b64encode(self.key.read_bytes()).decode(),
                        STIL_KEYSTORE_PASSWORD=self.password, STIL_KEY_PASSWORD=self.password,
                        STIL_KEY_ALIAS="test", STIL_SIGNING_CERT_SHA256=self.fingerprint)

    def prepare(self, tag=None, commit="a" * 40):
        return subprocess.run([sys.executable, str(self.script), "--apk", str(self.apk),
                               "--tag", tag or self.tag, "--commit", commit,
                               "--build-tools", str(self.tools), "--output", str(self.output)],
                              env=self.env, capture_output=True, text=True)

    def assertRejected(self, result, message):
        self.assertNotEqual(0, result.returncode)
        self.assertIn(message, result.stderr)
        self.assertFalse(self.output.exists())
        self.assertEqual([], list(self.output.parent.glob("stil-sign-*")))
        self.assertNotIn(self.password, result.stderr)
        self.assertNotIn(self.env.get("STIL_KEYSTORE_BASE64", "not-present"), result.stderr)

    def test_valid_apk_has_expected_certificate_checksum_and_source_metadata(self):
        result = self.prepare()
        self.assertEqual(0, result.returncode, result.stderr)
        apk = next(self.output.glob("*.apk"))
        subprocess.run([str(self.tools / "apksigner"), "verify", str(apk)], check=True, capture_output=True)
        digest = hashlib.sha256(apk.read_bytes()).hexdigest()
        self.assertEqual(f"{digest}  {apk.name}\n", (self.output / "SHA256SUMS").read_text())
        metadata = json.loads((self.output / "release.json").read_text())
        self.assertEqual("a" * 40, metadata["commit"])
        self.assertEqual(self.fingerprint, metadata["signingCertificateSha256"])
        self.assertEqual(digest, metadata["apkSha256"])
        self.assertEqual(3, len(list(self.output.iterdir())))
        self.assertEqual([], list(self.output.parent.glob("stil-sign-*")))
        damaged = bytearray(apk.read_bytes())
        damaged[len(damaged) // 2] ^= 1
        apk.write_bytes(damaged)
        self.assertNotEqual(0, subprocess.run([str(self.tools / "apksigner"), "verify", str(apk)],
                                            capture_output=True).returncode)

    def test_wrong_certificate_cannot_produce_release_files(self):
        self.env["STIL_SIGNING_CERT_SHA256"] = "0" * 64
        self.assertRejected(self.prepare(), "certificate does not match")

    def test_wrong_version_cannot_produce_release_files(self):
        self.assertRejected(self.prepare(tag="v999.999.999"), "tag must match")

    def test_invalid_tag_is_rejected_without_shell_execution(self):
        self.assertRejected(self.prepare(tag="v0.1.0; touch /tmp/stil-unwanted"), "Use a version tag")

    def test_full_source_commit_is_required(self):
        self.assertRejected(self.prepare(commit="main"), "full Git SHA")

    def test_missing_credentials_fail_clearly(self):
        del self.env["STIL_KEY_PASSWORD"]
        self.assertRejected(self.prepare(), "Missing STIL_KEY_PASSWORD")

    def test_invalid_keystore_encoding_is_rejected_and_cleaned_up(self):
        self.env["STIL_KEYSTORE_BASE64"] = "invalid base64!"
        self.assertRejected(self.prepare(), "Release preparation failed")

    def test_wrong_password_is_rejected_without_leaking_credentials(self):
        self.env["STIL_KEYSTORE_PASSWORD"] = "incorrect-test-password"
        self.assertRejected(self.prepare(), "Android signing tool rejected")

    def test_existing_release_files_are_not_overwritten(self):
        self.output.mkdir()
        sentinel = self.output / "existing.apk"
        sentinel.write_bytes(b"previous release")
        result = self.prepare()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("must not already exist", result.stderr)
        self.assertEqual(b"previous release", sentinel.read_bytes())
        self.assertEqual([sentinel], list(self.output.iterdir()))


if __name__ == "__main__":
    unittest.main()
