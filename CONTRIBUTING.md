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

Expect `OK` with no failures. JVM and Gradle device reports live in `app/build/reports`. Keep extra logs and raw captures in ignored `.artifacts/`. No hosted CI is configured; include local results in the PR.

Before a large build or device run, check CPU load, available memory, memory pressure, and swap activity. Use one Gradle worker and one test device. Do not stop another project's emulator or service. Gradle uses a bounded heap by default.

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
