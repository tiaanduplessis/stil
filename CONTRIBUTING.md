# Contributing

Stil is in early development. Repository access is required while it is private. Use an issue to discuss substantial changes before starting them.

## Build and test

Use JDK 17 and Android SDK 36. On Windows, use `gradlew.bat` in place of `./gradlew`. Install SDK Build Tools 35.0.0 if the Android tooling requests it. Set `ANDROID_HOME`, or put your SDK path in an untracked `local.properties` file.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --no-parallel
./gradlew :app:assembleDebugAndroidTest --max-workers=1 --no-parallel
```

With one test device connected, run the Android tests:

```sh
./gradlew :app:connectedDebugAndroidTest --max-workers=1 --no-parallel
```

Device tests install a fixture APK, launch activities, and change Stil preferences and the test device's wallpaper. Use a dedicated emulator without personal data. If several devices are connected, target only the test device:

```sh
adb devices -l
# Set this to the dedicated test device's serial from the list above.
stil_device=emulator-5556
adb -s "$stil_device" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$stil_device" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$stil_device" shell am instrument -w -r app.stil.launcher.test/androidx.test.runner.AndroidJUnitRunner
```

Expect `OK` with no failures. JVM and Gradle device reports live in `app/build/reports`. Keep extra logs and raw captures in ignored `.artifacts/`.

Before a large build or device run, check CPU load, available memory, memory pressure, and swap activity. Use one Gradle worker and one test device. Do not stop another project's emulator or service. Gradle uses a bounded heap by default.

## GitHub Actions

App, build, tooling, or workflow changes pushed to `main` build the debug and minified release APKs, run unit tests and lint, test the signing tool with a disposable key, and run device tests on API 36. PRs, scheduled runs, and documentation-only pushes do not run CI. **Actions → CI → Run workflow** on `main` can run the full API 26/36 device matrix on demand; its default is API 36 only. Device jobs run one at a time, with normal animations enabled.

Successful runs provide a debug APK. Reports, logcat, and a final device capture are retained for seven days, including available diagnostics from failed runs. Debug signing keys are generated on each runner: a new CI APK may require uninstalling an older debug build, which removes its settings. Release updates use a separate durable signing key.

The Gradle wrapper JAR is validated by `setup-gradle`, and the Gradle distribution checksum is pinned. Actions use full commit SHAs; Dependabot proposes weekly action updates. CI jobs have read-only repository permissions and no release credentials. Only `main` writes Gradle caches. Superseded main-branch CI runs are canceled. Private repository runs consume the account's Actions allowance; keep artifact retention and device matrices small.

The emulator job removes unused .NET, Swift, and NDK toolchains from its disposable runner. It checks for 10 GiB free before booting: the API 36 image needs more space than a small userdata setting suggests. This app does not build native code with the NDK.

For workflow script changes, run `actionlint`, `shellcheck scripts/run_device_tests.sh`, and `python3 -m unittest discover -s scripts/tests -p test_emulator_scripts.py -v`. These checks cover disk limits, diagnostics, logger cleanup, and failure propagation without starting an emulator or building the app. The emulator runner inputs call scripts because the action executes each input line in a separate shell.

To run signing-tool checks locally after building the unsigned release APK, set the paths for your SDK:

```sh
./gradlew :app:assembleRelease --max-workers=1 --no-parallel
export ANDROID_BUILD_TOOLS="$ANDROID_HOME/build-tools/35.0.0"
export STIL_TEST_APK=app/build/outputs/apk/release/app-release-unsigned.apk
python3 -m unittest discover -s scripts/tests -v
```

These tests create and delete a disposable key. They verify real signatures, metadata, checksums, tamper detection, and rejection of invalid release inputs. They do not need production signing credentials. See [release setup](docs/releasing.md) for manual distribution.

## Keep the launcher quiet

- Prefer short labels and visible controls. Keep home focused on opening apps.
- Preserve font scaling, RTL layout, touch targets, contrast, and disabled-animation behavior.
- Keep private apps out of global search and home pins, including during transitions.
- Keep the launch pause monotonic and require explicit confirmation. Animations must not delay launch or cancellation.
- Keep preferences compatible with existing installs. The Dark label still uses the persisted `BLACK` enum value.
- Keep core use offline. Photo downloads are optional and must retain the last good image on failure.

Use the existing single-module structure. New dependencies and abstractions should solve a concrete problem. Add tests for changed behavior or a reproduced regression; avoid tests that only repeat labels or implementation details.

## Submit a change

Explain the problem, resulting behavior, and local verification. For visible changes, include a small native capture with test data. Update the user guide and checked-in README media when they no longer match the app. Documentation media is intentional source content; unrelated test evidence remains ignored.

Never commit signing keys, credentials, device data, build outputs, or private vulnerability details. Publishing a release or changing repository visibility requires an explicit maintainer decision.
