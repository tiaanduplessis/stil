# Development

## Structure

`core/LauncherState.kt` holds immutable preferences and pure rules for pin limits, search, profile isolation, setup, and pause/wallpaper timing. JVM tests exercise these without Android.

`data/SettingsStore.kt` uses one application-scoped DataStore with versioned JSON. Writes are transactional, bounds are normalized, and corrupt storage can recover to defaults. App identity combines a component with the Android user serial, so duplicate names and work copies remain separate.

`data/AppCatalog.kt` refreshes on package/profile callbacks and activity resume. Private apps are omitted while locked and excluded from global search and home pins. Failed or incomplete enumeration never deletes home preferences. Removed apps are pruned only after a complete enumeration; paused work pins are retained.

`LauncherViewModel` owns user actions and navigation. The launch pause uses elapsed real time, survives configuration changes through the ViewModel, and requires explicit confirmation. App launches recheck current profile and activity availability.

`data/WallpaperRepository.kt` serializes updates, bounds download size and decoded dimensions, writes a new cache file before applying, and retains the previous image on provider/decode failures. Worker and foreground actions share this repository. WorkManager uses unique periodic and catch-up requests, with network constraints. Mode Off/Keep cancels automatic work. Native scheduling is inexact; no exact alarm is requested.

`ui/StilApp.kt` renders Compose screens. Android owns system indicators and role selection. Home uses the selected typeface and optional dimmed photo; other screens use solid backgrounds. Text uses scalable units, rows have minimum touch targets, and toggle/radio controls expose semantics.

## Quality gates

Run the commands in the README with JDK 17. Builds use one worker and a bounded heap by default. Test reports live under `app/build/reports`; task receipts belong in ignored `.artifacts/`.

Lint treats correctness and accessibility warnings as errors. Dependency availability checks remain visible as informational hints. The SDK 36/AGP 8.13 toolchain and compatible dependency versions are pinned; newer core libraries require SDK 37 and AGP 9.1. Upgrade them together after testing that toolchain, rather than making every build depend on the newest remote version.

No CI is configured, by explicit request. No release signing key, deployment, or publishing is configured.

## Device tests

The instrumentation APK provides two exported launcher activities in its test-only manifest. They establish real catalog discovery and cross-package launch behavior without relying on a third-party app. The production APK has no test activities or test hooks.

Storage and wallpaper tests use separate temporary DataStore files and injected photo sources. They establish persistence, invalid-state recovery, network/decode failure handling, and disabled-worker behavior. Live photo delivery is a separate manual check and depends on the provider/network.

API 26–27 work-profile controls open Android settings because the public quiet-mode API begins at API 28. Private-space controls require the default-home role and supported Android/user profiles. Physical OEM navigation and accessibility service behavior need device-specific follow-up beyond emulator proof.

## Motion

Navigation uses a 160 ms entrance fade from 92% opacity with an 8 dp shift that follows navigation depth and layout direction. Initial rendering and the launch-pause flow stay immediate. Each new screen disposes the old content at once, including private app content and input fields; navigation never waits for an animation.

Wallpaper changes crossfade over 240 ms, with 120 ms dimming and font-selection feedback. There are no repeating or idle animations. Compose's platform motion-duration scale controls these effects, including Android's disabled-animation setting. Device tests use a controlled animation clock to check interrupted navigation, interaction during transitions, and zero-duration motion.
