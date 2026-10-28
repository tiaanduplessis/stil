# Changelog

## 0.5.0 - 2026-10-27

Release candidate with version code 6, prepared to update the signed 0.4.0 preview.

- Open the sole search match while a predictive keyboard is still composing the word. Preserve launch pauses and ignore cursor-only changes.
- Add on-device launch stats under Settings with Today, 7-day, and 30-day views, daily activity, and per-app open counts.
- Count successful launcher opens once. Exclude canceled pauses, failed launches, and Private space; keep hidden and removed apps out of stats.
- Scroll the foldable search-continuity test to its result before checking visibility in a short pane.
- Remove the bundled forest photo. With Daily or Keep, Home shows a plain black background until a photo downloads; the Wallpaper credit appears only once a photo exists, and the HTML prototype uses generated gradients.
- Omit Gradle dependency metadata from the APK signing block.
- Fix the foldable search test to scroll the lazy app list to its result.
- Retry clearing logcat in the device-test script when an API 26 emulator rejects it, and carry on if it never succeeds.
- Wait for the emulator's package service before the device-test script starts Gradle.
- Make the Android 8 device tests pass: bring Stil back through the shell after a fixture launch, and let the settings scroll offset clamp when a pane crosses the compact-height threshold. Place the tabletop test's fold at the middle so the All apps pane is the lower one on a 720x1600 phone at 2.75 density.
- Upload device diagnostics even when a hung emulator job is cancelled, and cap that job at 20 minutes.
- Pin the emulator to 280 dpi in the device-test script, since hosted runs came up at 440 dpi. The book-posture test now changes density only on a device too narrow for two panes, which removes the mid-suite density flip that preceded the API 36 hangs.
- Log every thread's stack from a device test that runs past 90 seconds, and wait up to 30 seconds for a fixture launch.
- Add store listing text, icon, and screenshots under `fastlane/` for F-Droid and IzzyOnDroid.
- Update repository documents for public use and add Gradle to Dependabot.

## 0.4.0 - 2026-10-26

Signed [preview release](https://github.com/tiaanduplessis/stil/releases/tag/v0.4.0). Updates the signed 0.3.0 preview with the same key and version code 5.

- Slide All apps up from the bottom, following Android's animation setting.
- Open the only available search match as you type, including aliases. Keep launch pauses and wait for IME composition to finish.

## 0.3.0 - 2026-10-26

Signed [preview release](https://github.com/tiaanduplessis/stil/releases/tag/v0.3.0). Updates the signed 0.2.1 preview with the same key and version code 4.

- Adapt Home to narrow, wide, and tabletop windows. Keep content clear of separating folds and hinges.
- Keep search, cursor selection, unsaved names, scroll position, and launch pauses through posture and configuration changes.
- Fit navigation and scrolling content into short panes and large system text.
- Make book-posture tests use a readable viewport and improve launch-fixture delivery on busy emulators.

## 0.2.1 - 2026-10-26

Signed [preview release](https://github.com/tiaanduplessis/stil/releases/tag/v0.2.1). Updates the signed 0.2.0 preview without removing settings.

- Unify screen headers, typography, spacing, setting rows, and neutral control colours.
- Improve app editing and wallpaper controls at large text sizes; keep Back and Preview home reachable.
- Refresh native screenshots and the walkthrough to match the refined UI.
- Fix emulator CI scripts, diagnostic cleanup, and setup-test timing.

## 0.2.0 - 2026-10-25

Signed [preview release](https://github.com/tiaanduplessis/stil/releases/tag/v0.2.0). Updates the signed 0.1.0 preview without removing settings.

- Replace home navigation buttons with swipe up for All apps and a hold on empty space for Settings. Keep app editing, accessibility actions, and keyboard navigation available.

## 0.1.0 - 2026-10-25

First signed [preview release](https://github.com/tiaanduplessis/stil/releases/tag/v0.1.0).

- Native Android launcher with one to six home apps, search, aliases, ordering, and hidden-app recovery.
- Local font families, Light/Dark/System appearance, alignment, and text size.
- Optional three-second launch pause with explicit confirmation.
- Daily cached wallpapers, attribution, keep/off controls, and dimming.
- Work-profile controls and a separate lockable Private space container on supported devices.
- Short navigation transitions, wallpaper crossfades, and selection feedback that follow Android's animation settings.
