# Stil: recommendation and research

Research date: 25 October 2026. This is a product proposal, not an agreed Android implementation scope.

## Recommendation

Build a text-first launcher for people who want less visual noise while keeping their phone useful. Start with five chosen apps, a clock and date, and fast local app search. Make settings easy to find. Offer a short pause for selected apps as an optional experiment.

Stil's distinction should be restraint with clear controls: essential actions stay quick, customization stays discoverable, and the user decides where to add friction. Text-only home screens already exist; that alone is not a new product advantage. Whether the pause changes habits needs user testing. Do not promise a reduction in screen time without evidence.

Assumptions: Android phones first; personal use first; offline operation; no account, ads, or analytics. Core launching works offline; optional daily wallpaper downloads use the network. Pricing, distribution, supported Android versions, and source license remain product decisions.

## What existing products teach us

| Product | Observed approach | What Stil should take from it |
| --- | --- | --- |
| [Olauncher](https://github.com/tanujnotes/Olauncher) | Minimal launcher with daily wallpapers. Its README deliberately points to in-app documentation and hides niche features. | Keep the home screen sparse, but expose important settings normally. |
| [Olauncher’s own guide](https://tanujnotes.substack.com/p/olauncher-minimal-af-launcher) | Renaming, configurable gestures, keyboard launch, and hidden apps; several controls depend on long presses. | Keep renaming and keyboard launch. Provide visible paths to customization and hidden-app recovery. |
| [Niagara](https://niagaralauncher.com/) | List navigation, an alphabet wave, home-screen replies, and optional widgets and themes. | Design for one hand and quick access. Avoid making Stil another widget platform. |
| [Before](https://beforelabs.com/) | A minimal home screen with app search, work profiles, notification filtering, and organization features. | Reliability and profile support matter. Notification filtering can be considered separately from launching. |
| [KISS](https://kisslauncher.com/) | Emphasizes speed, search, and a small footprint. | App discovery should remain fast even with many apps. Avoid background work that does not serve the launcher. |
| [minimalist phone](https://faq.minimalistphone.com/en/article/what-do-mindful-launch-delay-and-mindful-extension-delay-features-do-o5krfw/) | Optional delays before launching an app or extending an app session. | A pause is an established interaction worth testing. Keep it optional and clearly scoped. |

These are product descriptions, not independent evidence of wellbeing benefits. No competitor pricing or performance claim is used as a benchmark.

## Proposed first release

| Capability | Proposed behavior | Why it belongs |
| --- | --- | --- |
| Quiet home | Five apps by default; allow one to six. Show clock and date. No icons, feeds, badges, or suggested apps. | A predictable starting point without visual competition. |
| All apps and search | Alphabetical list. Search original names and user aliases locally. Explicit tap or keyboard confirmation to launch. | Find rarely used apps without cluttering home or triggering accidental launches. |
| Home customization | Pin, unpin, rename, and reorder. Explicit editing screen; long press is a shortcut only. | Personal names and stable ordering without hidden controls. |
| Appearance | Configurable font family, light/black/system theme, left/right alignment, and text size. Respect system font scaling. | Personal typography without adding home-screen controls. |
| Daily wallpaper | Optional random daily photo, skip, keep current image, and dimming. Keep the current image when a download fails or the device is offline. | Adds visual variety while preserving readable app names. |
| Hidden apps | Hide from Stil's home and search; restore through Settings > Hidden apps. Explain that hiding does not secure an app. | Reduce clutter without trapping the user. |
| Optional launch pause | Off by default. Select apps and require a short pause followed by an explicit Open button. Back out immediately. | Explore deliberate use without delaying every task. |
| Clear setup | Choose essentials, preview, then request Android's default-home role. Permit previewing before commitment. Provide a change-launcher path in settings. | Make adopting and leaving Stil straightforward. |
| Reliable Android behavior | Handle app installs/removals, duplicate labels, work profiles, unavailable apps, back navigation, keyboard insets, and locked private profiles. | A daily launcher must remain dependable. |
| Accessibility and privacy | TalkBack labels, logical focus, 48dp targets, contrast, reduced motion, RTL, large text, and fully local core operation. | Minimal appearance must not reduce usability. |

For the pause experiment, start with a three-second pause and an explicit confirmation. The mockup enables Instagram as sample data so this behavior can be reviewed. Native setup should leave the feature off until the user chooses it. Do not delay Phone or other user-selected essentials by default.

## Later, only if use supports it

1. Local export/import, with clear handling for apps missing on the new device.
2. Pinned Android app shortcuts, such as a specific conversation or navigation destination.
3. An optional next-alarm line that opens the system clock.
4. A small usage summary, only after the user grants usage access.
5. Notification filtering as a separate, opt-in feature with its own permission explanation and recovery design.

Do not put weather, news, AI suggestions, streaks, widgets, folders, or device-wide app blocking in the first release. Those features add dependencies or product complexity without strengthening the core proposition. Haptics and gestures can be small refinements once basic navigation is proven.

## Android constraints that affect the design

- **Default home:** use Android's home role and system chooser. The HTML prototype can only simulate that chooser. [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)
- **App identity and profiles:** use `LauncherApps`; identify an entry by component and user/profile, not its visible name. Show a work label where needed and respect paused profiles. [LauncherApps](https://developer.android.com/reference/android/content/pm/LauncherApps)
- **Private space:** Android 15 requires the default-home role and `ACCESS_HIDDEN_PROFILES` for launcher access. If supported, it needs a separate container with hide/show and lock/unlock controls. Locked entries must disappear from search. Do not mix private apps into the ordinary list. This needs native design and verification beyond these personal-profile mockups. [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all#private-space)
- **Pause scope:** the proposed pause runs before Stil starts an app. It does not intercept notifications, recent apps, links from other apps, or another launcher. This follows from where Stil places the confirmation in its own launch flow; it is not a device-wide restriction. Broader blocking would require a separate feasibility and permission review.
- **Usage totals:** counting launches through Stil does not measure device screen time. Android usage statistics require separate user-granted access through Settings. Keep that permission out of core setup. [Usage permission](https://developer.android.com/reference/android/Manifest.permission#PACKAGE_USAGE_STATS)
- **Touch and type:** Android recommends at least 48dp interactive targets. Large font sizes, translated labels, small phones, and the on-screen keyboard need actual device checks. [Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- **OEM behavior:** Olauncher's own guide reports gesture-navigation limitations on some manufacturers. Plan physical-device checks on Pixel, Samsung, and Xiaomi rather than promising uniform system gestures. [Olauncher guide](https://tanujnotes.substack.com/p/olauncher-minimal-af-launcher)

## Implementation direction after design approval

Recommend a fresh native Kotlin and Jetpack Compose implementation with local preferences, a cached app catalog refreshed from Android callbacks, and no core network dependency. This keeps launch behavior and profile handling close to Android's APIs. The HTML is a design artifact, not a proposed WebView implementation.

Olauncher identifies its license as GPLv3. Use it as a reference; choose Stil's license and approach to code reuse before copying any source. [Olauncher repository and license](https://github.com/tanujnotes/Olauncher)

Before building, settle the minimum Android version, distribution channel, source license, and whether private-space support is required in the first usable release. A native release gate should include app lifecycle changes, work/private profiles where supported, TalkBack, RTL, large text, screen rotation, process recreation, and returning from launched apps.

## What these mockups establish

They demonstrate screen hierarchy, navigation, search, customization, font presets, wallpaper modes, setup, and the pause interaction. They use a fixed sample clock and app catalog. They do not establish native launch performance, battery use, system navigation, Android permissions, profile support, or improved wellbeing.

## Fonts and daily wallpapers

Font selection and optional random daily wallpapers are now part of the proposed first release. The mockup offers System, Sans, Serif, and Mono. These use local browser font stacks; native equivalents need selected font assets. The selected family applies throughout Stil, while system status indicators keep their system typeface.

Wallpaper modes are Off, Daily, and Keep. Daily picks another image on a new local date; Next image skips immediately; Keep stops rotation. Switching Off restores the plain theme. Dimming ranges from 60% to 85% so white app names remain legible even over light areas. Keep other launcher screens on solid backgrounds. Show photo credit in wallpaper settings.

The mockup uses three local sample photos. Its Next day control simulates a new date. It checks the date during interactions and when the page becomes visible; it does not install an Android background task or change the system wallpaper.

For native delivery, propose Wi-Fi downloads by default, a bounded local cache, and retaining the last good image when offline or when a download fails. The source and its distribution terms remain a product decision. Downloads are optional; fonts and core launching remain local. Android periodic work can be delayed by battery optimization, so daily rotation should not promise an exact midnight change. Check the date when the launcher resumes as well. [Android periodic work](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)

Use the system home-wallpaper API with the wallpaper permission and check whether wallpaper changes are supported and allowed. Home and lock-screen targets have distinct flags; changing the lock screen should be a separate user choice. [WallpaperManager](https://developer.android.com/reference/kotlin/android/app/WallpaperManager)
