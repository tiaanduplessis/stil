# Stil

Stil is a native Android launcher in one Kotlin/Compose application module. Read [development notes](docs/development.md) for architecture and [acceptance criteria](docs/acceptance.md) for behavior.

## Commands and layout

- Use JDK 17, Android SDK 36, and the checked-in Gradle wrapper.
- Local gates: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --no-parallel`.
- Android tests: follow [CONTRIBUTING.md](CONTRIBUTING.md#build-and-test). Use a dedicated emulator; tests change app preferences and wallpaper. Target a serial explicitly when other devices are connected.
- `core/` contains pure launcher rules; `data/` contains settings, app discovery and wallpaper work; `ui/` contains Compose screens and motion.
- Test code is in `app/src/test` and `app/src/androidTest`. Production code must not acquire test activities or test hooks.

## Constraints

- Before editing, inspect the working tree and fetch the primary branch. Preserve unrelated work.
- Keep verification local. Do not add hosted CI, publish releases, or change repository visibility without an explicit request.
- Check CPU load, RAM, memory pressure and swap before expensive work. Serialize heavy jobs, keep Gradle to one worker, and do not stop unrelated services or devices.
- Keep UI copy short. Preserve scalable type, RTL, contrast, touch targets, and Android's disabled-animation setting.
- Remove outgoing screen content immediately. Never retain private app content or duplicate input fields for a transition.
- Preserve stored settings: Dark is the UI label; `ThemeMode.BLACK` remains the serialized value for compatibility.
- Keep core use offline and photo networking optional. Preserve attribution and the last good photo on failure.
- Keep raw verification evidence in ignored `.artifacts/`. README screenshots and video under `docs/media/` are intentional documentation assets requested by the maintainer.
- Do not choose a project license or invent a security contact. Record unresolved release decisions honestly.
