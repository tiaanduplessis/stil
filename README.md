# Stil

A minimal native Android launcher. Pin a few apps, find the rest by name, and optionally pause before opening selected apps.

## Features

- One to six pinned apps, clock and date, local search, aliases, ordering, hidden apps and recovery.
- Light, black or system appearance; System, Sans, Serif or Mono fonts; alignment and type size.
- Optional three-second launch pause with explicit Open and immediate Cancel.
- Daily random wallpapers from Lorem Picsum, skip/keep/off, dimming, Wi-Fi downloads by default, and an offline fallback. Photographer credit links to the source.
- Default-home setup, app install/removal updates, work-profile controls, and a separate lockable private-space container on supported Android versions.
- No account, ads, analytics, usage access, accessibility service, or notification access. Core use stays offline.

Android 8.0 (API 26) or later. Target SDK 36. Private space requires Android 15 or later and selection as the default home app. Private apps stay in their own container; they are excluded from global search and home pins. System gestures and work/private profile availability depend on the device.

## Build and test

Use JDK 17 and Android SDK 36. Set `ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir=/absolute/path/to/sdk`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --no-parallel
./gradlew :app:connectedDebugAndroidTest --max-workers=1 --no-parallel
```

The second command requires an Android device or emulator. Install `app/build/outputs/apk/debug/app-debug.apk`, choose home apps, and select Stil as the default home. Change it later under Settings > Default home app. The debug APK is for local testing; release signing is not configured.

Verification stays local by request. No hosted CI workflow is configured. Generated test reports, APKs, screenshots, and logs are ignored. Acceptance criteria are in [docs/acceptance.md](docs/acceptance.md).

## Implementation

Kotlin, Jetpack Compose, DataStore and WorkManager in one application module. Pure launcher rules and provider parsing have JVM tests. Device tests exercise real app discovery/launch, setup, settings, search, hiding, pause timing, DataStore, and wallpaper failure recovery. The instrumentation APK supplies two launcher activities as intentional test fixtures.

Daily updates use Android background work and a date check on resume. Android may delay background work. Failed downloads retain the current image; the image cache keeps the current and previous download. Off restores Stil's plain home background. Rotation changes the system home wallpaper and leaves the lock-screen target alone. Existing system wallpaper can still be seen elsewhere in Android.

Photos use an unauthenticated third-party service. Availability is best effort. The bundled fallback is [Alvin Engler's photo](https://unsplash.com/photos/aerial-view-of-trees-LnbWqObX0TQ). See [mockups/assets/README.md](mockups/assets/README.md) for prototype photo credits. No source was copied from Olauncher.

## Design reference

Open `mockups/index.html` in a browser with `mockups/assets` alongside it. These remain design references; the Android application does not use a WebView. Research and visual decisions are in `docs/research.md` and `docs/design.md`.
