# Releases

Stil uses private GitHub build downloads and manual GitHub Releases. The repository remains private. No signed release has been published, and the durable release key is not configured yet. Publishing a release or changing repository visibility requires an explicit maintainer decision.

## Test builds

Open a successful [CI run](https://github.com/tiaanduplessis/stil/actions/workflows/ci.yml) and download its `stil-debug` artifact. Extract the ZIP and install the APK. Downloads require repository access and expire after seven days. Each runner generates a debug signing key, so updating a previous debug install may require uninstalling it first; that removes its settings. A debug install cannot be upgraded to the release signing identity.

PRs and pushes to `main` run builds, unit tests, lint, signing-tool checks, and API 36 emulator tests. Nightly runs at 03:23 UTC also test API 26. Actions use Ubuntu runners, pinned commits, Gradle caching, one Gradle worker, bounded timeouts, and short artifact retention. Private runs consume the account's Actions allowance.

## Configure release signing once

Choose a durable Android signing key before distributing release APKs. Keep an encrypted backup outside GitHub and record the certificate fingerprint. Updates need the same signing identity and a higher `versionCode`. Losing this key can prevent updates to existing installs. If Play distribution is added later, plan the signing identity across channels before enrolling in Play App Signing.

Create a GitHub environment named `release` under **Settings → Environments**, restricted to the `main` branch. Add these **environment secrets**, using GitHub's secure entry fields or `gh secret set --env release`; do not paste them into issues, PRs, chat, or committed files:

| Secret | Value |
| --- | --- |
| `STIL_KEYSTORE_BASE64` | Base64 encoding of the binary release keystore |
| `STIL_KEYSTORE_PASSWORD` | Keystore password |
| `STIL_KEY_ALIAS` | Signing key alias |
| `STIL_KEY_PASSWORD` | Key password |

Add the **environment variable** `STIL_SIGNING_CERT_SHA256` with the certificate's SHA-256 fingerprint, with or without colons. Read it from the trusted keystore, for example with `keytool -list -v -keystore /secure/path/stil.jks -alias <your-alias>`. The password prompt keeps it out of shell history. Back up the key before relying on CI. Environment approval features depend on the GitHub plan; the workflow does not assume they are available.

Credentials are passed only to the signing step and its configuration check. The signing tool decodes the key to a temporary file with restricted permissions and deletes it on completion or failure. It verifies APK alignment, version, signature, and the expected certificate before producing distributable files. Missing credentials or a mismatched certificate stop release creation. The real key is never used by CI tests.

Android signing background: [app signing](https://developer.android.com/studio/publish/app-signing). GitHub background: [environments](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments).

## Prepare a candidate

1. Start from current `main`; record the commit. Update `versionName` and increase `versionCode` in `app/build.gradle.kts`. Update [CHANGELOG.md](../CHANGELOG.md).
2. Merge the candidate through CI. Walk setup/default home, real app launch, search, aliases/hiding/recovery, fonts/themes, pause, offline photo failure, and available work/private profiles on a physical device. Emulator checks do not establish OEM compatibility.
3. Review bundled media and resolved dependency notices. Review repository history for credentials or private material before any visibility change. Confirm the [security contact](../SECURITY.md) is monitored and test delivery through the maintainer.
4. Create and push an annotated tag matching `versionName`, for example `v0.1.0`. The workflow accepts `vX.Y.Z` and prerelease forms such as `v0.1.0-beta.1`. The tag must point to a commit already on `main`.
5. Open **Actions → Release → Run workflow**, select branch **main**, and enter the existing tag. Dispatching the workflow authorizes preparation of a draft, not publication.

The release workflow resolves the tag to an exact commit, reruns the build/unit/lint/signing checks and both API 26 and 36 device suites on that commit, then builds its minified release APK. It signs the APK and verifies that its embedded version matches the tag and its certificate matches the configured fingerprint. A moved tag stops draft creation.

The draft receives:

- `stil-<version>.apk`, the signed APK.
- `SHA256SUMS`, its SHA-256 checksum.
- `release.json`, its source commit, version code, certificate fingerprint, and APK digest.
- `mapping.txt`, the R8 mapping needed to interpret obfuscated crash traces.

Verified files and mapping are also retained as a private Actions artifact for 30 days. Draft release assets persist independently of that artifact retention. Existing releases are not overwritten. No release runs automatically on a merge or tag push.

## Review and publish manually

Download the signed APK from the draft, verify its checksum and certificate, and install that exact artifact on the test device. Test updating an earlier signed release when one exists. Check default-home selection and app launching on the minified build; debug instrumentation tests exercise the debug variant.

Edit the generated release notes to describe changes, supported Android versions, tested devices, and known limits. Publish only after the maintainer approves the candidate. GitHub Releases inherit repository visibility, so publishing a release in this private repository does not make the repository public.

Update README status and download links only after a release exists. Recheck reporting settings before public use: GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/configure-vulnerability-reporting/configure-for-a-repository) is documented for public repositories. Do not advertise an inactive form.
