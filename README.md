# Stil

A minimal Android launcher with a short app list, custom fonts, and daily wallpapers.

**Early development.** Android 8.0 or later. The repository is private, and no APK release has been published yet.

<p>
  <img src="docs/media/home.png" width="220" alt="Stil home with five text app shortcuts, a clock, and a dimmed forest wallpaper">
  <img src="docs/media/apps.png" width="220" alt="All apps filtered by name, with an explicit result to open">
  <img src="docs/media/settings.png" width="220" alt="Stil settings with Dark appearance selected and controls for fonts and wallpapers">
</p>

## See it in use

<a href="docs/media/walkthrough.mp4"><img src="docs/media/walkthrough.gif" width="320" alt="Native walkthrough: open settings, change the font, and return home"></a>

[Watch the walkthrough with playback controls (MP4)](docs/media/walkthrough.mp4) · [Text walkthrough](docs/usage.md#walkthrough)

These are captures of the Android app, not the HTML prototype. [Media notes and photo credits](docs/media/README.md).

## Try Stil

For now, build the debug APK from source. Repository access is required while Stil is private.

1. Install JDK 17 and Android SDK 36. Set `ANDROID_HOME`, or add `sdk.dir=/absolute/path/to/sdk` to an untracked `local.properties` file.
2. Clone the repository and build:

   ```sh
   git clone https://github.com/tiaanduplessis/stil.git
   cd stil
   ./gradlew :app:assembleDebug --max-workers=1 --no-parallel
   ```

3. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone. Android may ask you to allow installation from the browser or file manager you used.
4. Open Stil, choose one to six home apps, then select **Use Stil** in Android's default-home picker.

The debug APK is for testing. Signed releases and store distribution are not configured. To switch launchers later, use **Settings → Default home app**. More setup and recovery steps are in the [user guide](docs/usage.md).

## What it does

- **A short home screen.** Pin up to six apps, rename and reorder them, or hide apps and restore them later. Search finds original names and aliases.
- **Your type and background.** Choose System, Sans, Serif, or Mono; Light, Dark, or System appearance; alignment and text size. Use daily photos, keep one, or turn wallpaper off.
- **A deliberate pause.** Selected apps can wait three seconds before an explicit **Open**. **Cancel** returns immediately.
- **Separate profiles.** Work apps have profile controls. On supported devices, Private space has its own lockable container and search. Private apps stay out of global search and home pins.
- **Quiet motion.** Short transitions respond to navigation and selection. Android's animation settings control them.

Private space requires Android 15 or later and selection as the default home app. Profile availability and system gestures vary by device. Runtime verification currently uses an Android 16 emulator; physical devices have not yet been tested.

## Privacy

No account, ads, analytics, usage access, accessibility service, or notification access. Core launcher features work offline. Optional photo downloads use Lorem Picsum and retain photographer/source attribution. Failed downloads keep the current photo; Android may delay background updates.

Preferences and cached photos stay on the device. Read the [privacy notes](docs/privacy.md) for network requests, permissions, and wallpaper behavior. Bundled photo credits are in [third-party notices](THIRD_PARTY_NOTICES.md).

## Build, contribute, and get help

- [Build and test](CONTRIBUTING.md#build-and-test)
- [Report a bug or suggest a change](https://github.com/tiaanduplessis/stil/issues/new/choose)
- [Usage and troubleshooting](docs/usage.md)
- [Report a security issue privately](SECURITY.md)
- [Architecture and motion](docs/development.md)
- [Release preparation](docs/releasing.md)

Verification stays local by maintainer choice; no hosted CI workflow is configured. Kotlin, Jetpack Compose, DataStore, and WorkManager power the native app. The [HTML prototype](mockups/index.html), [research](docs/research.md), and [design notes](docs/design.md) remain development references. The Android app does not use a WebView, and no source was copied from Olauncher.

## License

Stil's source code is licensed under [MIT](LICENSE). Third-party photos and libraries retain their own terms; see [third-party notices](THIRD_PARTY_NOTICES.md).
