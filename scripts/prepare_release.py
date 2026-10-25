#!/usr/bin/env python3
"""Sign an APK and prepare verified release files without publishing them."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile


def run(*command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT)


def prepare(apk, tag, commit, build_tools, output):
    if not re.fullmatch(r"v\d+\.\d+\.\d+(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?", tag):
        raise ValueError("Use a version tag such as v0.1.0 or v0.1.0-beta.1")
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("The source commit must be a full Git SHA")
    required = ("STIL_KEYSTORE_BASE64", "STIL_KEYSTORE_PASSWORD", "STIL_KEY_ALIAS",
                "STIL_KEY_PASSWORD", "STIL_SIGNING_CERT_SHA256")
    for name in required:
        if not os.environ.get(name):
            raise ValueError(f"Missing {name}")
    expected_cert = os.environ["STIL_SIGNING_CERT_SHA256"].replace(":", "").lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_cert):
        raise ValueError("STIL_SIGNING_CERT_SHA256 must be a SHA-256 certificate fingerprint")
    metadata = run(str(build_tools / "aapt"), "dump", "badging", str(apk))
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", metadata)
    if not package or package[1] != "app.stil.launcher" or int(package[2]) < 1:
        raise ValueError("Expected a Stil APK with a positive version code")
    if package[3] != tag[1:]:
        raise ValueError("The tag must match versionName in the built APK")
    if output.exists():
        raise ValueError("The output directory must not already exist")
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="stil-sign-", dir=output.parent) as directory:
        staging = Path(directory)
        keystore = staging / "signing.jks"
        with keystore.open("xb") as file:
            keystore.chmod(0o600)
            file.write(base64.b64decode(os.environ["STIL_KEYSTORE_BASE64"], validate=True))
        artifacts = staging / "artifacts"
        artifacts.mkdir()
        signed = artifacts / f"stil-{tag[1:]}.apk"
        run(str(build_tools / "zipalign"), "-c", "-P", "16", "4", str(apk))
        run(str(build_tools / "apksigner"), "sign", "--ks", str(keystore),
            "--ks-pass", "env:STIL_KEYSTORE_PASSWORD", "--ks-key-alias", os.environ["STIL_KEY_ALIAS"],
            "--key-pass", "env:STIL_KEY_PASSWORD", "--out", str(signed), str(apk))
        verification = run(str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs", str(signed))
        certificates = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$", verification, re.M)
        if [cert.lower() for cert in certificates] != [expected_cert]:
            raise ValueError("The APK signing certificate does not match STIL_SIGNING_CERT_SHA256")
        digest = hashlib.sha256(signed.read_bytes()).hexdigest()
        (artifacts / "SHA256SUMS").write_text(f"{digest}  {signed.name}\n", encoding="utf-8")
        (artifacts / "release.json").write_text(json.dumps({
            "tag": tag, "commit": commit, "versionName": package[3], "versionCode": int(package[2]),
            "signingCertificateSha256": expected_cert, "apkSha256": digest,
        }, indent=2) + "\n", encoding="utf-8")
        # The APK signature is embedded; apksigner's optional sidecar is not distributed.
        signed.with_suffix(".apk.idsig").unlink(missing_ok=True)
        artifacts.rename(output)
    print(f"Verified release files: {output}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--build-tools", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        prepare(args.apk, args.tag, args.commit, args.build_tools, args.output)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        # Tool output can contain keystore details. Keep it out of CI logs.
        parser.exit(1, f"Release preparation failed: {error if not isinstance(error, subprocess.CalledProcessError) else 'Android signing tool rejected the APK or credentials'}\n")


if __name__ == "__main__":
    main()
