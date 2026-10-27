# Privacy

Stil has no accounts, advertising, analytics SDK, or automatic crash-reporting service. Core launcher features work offline.

## On the device

Stil reads launchable app names and components through Android's launcher APIs. App identity includes the Android user serial so personal and work copies stay separate. Unlocked Private space entries are held in the app catalog; locked private apps are omitted and private entries are excluded from global search and home pins.

Preferences include home apps, aliases, hidden apps, launch-pause choices, appearance, and wallpaper settings. They are stored in the app's local DataStore. Downloaded wallpaper files stay in app storage; the cache keeps the current and previous image. Photo source, author, and last-update date are also stored locally.

Launch stats store daily counts of successful app opens through Stil, by component and user serial. They show the last 30 local calendar dates; older buckets are removed on the next recorded open. Private space launches are never recorded. Hidden and uninstalled apps are omitted from stats, including totals and charts. Hiding an app does not erase its existing counts; restoring it can show counts still within the period. Stil does not measure screen time or opens from notifications, recents, or other launchers, and never sends launch counts off the device.

Android backup and device-transfer rules exclude Stil's app data. Clearing Stil's storage or uninstalling it removes local preferences, launch counts, and cached files. A photo already applied as Android's system wallpaper is managed separately by Android and is not restored automatically when Stil is uninstalled.

## Network requests

Photo downloads are optional. **Daily** schedules requests to [Lorem Picsum](https://picsum.photos/), and **Next image** requests a photo manually. Requests may follow the provider's image redirects. The service receives your IP address and normal HTTP request details. Stil does not send your app catalog, aliases, hidden-app list, or launcher preferences to the photo service.

**Off** and **Keep** cancel automatic photo work. **Next image** remains available in Keep mode. Wi-Fi downloads use Android's unmetered-network capability, so availability depends on the device's network settings.

Photo-credit links open in your browser. Its requests and the linked site's behavior are outside Stil.

## Android access

- Internet and network-state access support optional photo downloads and network constraints.
- Wallpaper access applies a system/home wallpaper through Android.
- Hidden-profile access supports Private space on compatible Android versions when Stil holds the default-home role.
- WorkManager supplies Android's background-work scheduling support.

Stil does not request usage access, an accessibility service, or notification-listener access. It does not need your contacts, messages, microphone, or location to function as a launcher.

## Bug reports

Reports and media you submit to GitHub are separate from the app's local data. Share only what is needed to reproduce the problem. Use the private contact in [SECURITY.md](../SECURITY.md) for vulnerability reports; sensitive security details should not be filed as ordinary issues.
