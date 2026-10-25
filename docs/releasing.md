# Preparing a release

The repository is private and has no published APK release. Keep it private until the maintainer explicitly decides otherwise. A merged PR or successful debug build is not a published or signed release.

## Decisions before public use

- Confirm the [MIT license](../LICENSE) and review bundled photo and dependency notices.
- Confirm that the private contact in [SECURITY.md](../SECURITY.md) is monitored and test delivery through the maintainer before publishing.
- Choose the release signing key, its secure storage, and who may sign updates. Do not commit keys, passwords, or signing credentials.
- Decide the distribution channel and which physical Android devices will be checked.

GitHub's [private vulnerability-reporting feature](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/configure-vulnerability-reporting/configure-for-a-repository) is documented for public repositories. Recheck availability and enable it only when appropriate; do not advertise an inactive reporting form. A maintainer-provided private contact can be used before that.

## Verify a candidate

1. Start from the current primary branch and record the exact commit. Preserve unrelated work.
2. Set `versionCode` and `versionName` in `app/build.gradle.kts`; update [CHANGELOG.md](../CHANGELOG.md).
3. Run the local build, unit tests, lint, and device checks in [CONTRIBUTING.md](../CONTRIBUTING.md#build-and-test). Check host resources first and serialize heavy jobs.
4. Walk setup/default home, real app launch, search, aliases/hiding/recovery, fonts/themes, launch pause, offline photo failure, and available work/private profiles on the candidate. Include a physical device before claiming physical-device compatibility.
5. Rebuild the release APK from the checked source:

   ```sh
   ./gradlew :app:assembleRelease --max-workers=1 --no-parallel
   ```

   Current output is `app/build/outputs/apk/release/app-release-unsigned.apk`. It is not installable as a release until signed. Release signing is not configured in this repository.
6. Sign through the chosen secure process, verify the signing certificate and APK with Android's signing tools, and install that signed artifact on the test device. Record its SHA-256 and source commit. Test updating an existing install; update compatibility depends on retaining the signing identity and increasing the version code.
7. Review dependency changes, bundled media notices, and repository history for credentials or private material before any visibility change. A file's absence from the current tree does not remove it from Git history.
8. Refresh README captures when the UI changes. Check its installation links and ensure it describes the version being distributed.

## Publish only after approval

Create release notes that describe user-visible changes, supported Android versions, tested devices, known limits, and the source commit. Attach the verified signed APK and checksum only after the maintainer authorizes publishing. Do not substitute a debug APK or an unsigned APK for a signed release.

Update the README's early-development/private status and download link only after a real release exists. Recheck live repository visibility, reporting settings, and any rulesets rather than inferring them from documentation. Hosted CI remains disabled by current maintainer choice; adding it is a separate decision.
