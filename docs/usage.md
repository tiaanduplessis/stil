# Using Stil

Stil replaces your Android home screen. It does not replace Android's notifications, lock screen, recent-apps view, or system settings.

## Set it as home

Open Stil, choose one to six apps, and continue. Tap **Use Stil**, select it in Android's picker, and confirm. You can preview the app with **Not now** and choose the default later under **Settings → Default home app**.

If the picker does not appear, open Android's default-app settings and choose Stil as the home app. The exact Android settings labels vary by device.

## Walkthrough

The original [README video](media/walkthrough.mp4) shows the native app's settings and earlier search flow:

1. Hold an empty area of home to open **Settings**.
2. Open **Font**, choose **Serif**, and select **Preview home**.
3. Swipe up from the bottom of home and type **Clock** to filter the app list.

Home contains a clock, date, and chosen apps. Swipe up from the empty area just above Android's navigation bar for **All apps**. Hold empty space for **Settings**. Holding an app opens its edit controls. Tapping the clock opens the device's alarm app when one is available.

TalkBack exposes **All apps** and **Settings** as actions on Home. With a keyboard, focus Home and press Enter or Space for apps, or the Menu key for settings.

These gestures are available in the [0.2.1 preview](https://github.com/tiaanduplessis/stil/releases/tag/v0.2.1). Install it over the signed 0.2.0 preview to keep your home apps and preferences.

## Change your home apps

Long-press a home app, or open **Settings → Home apps** and choose an app. Rename it, move it, remove it from home, hide it, or add a launch pause. Stil keeps at least one home app and allows at most six.

Search matches the original app name and any alias. When your text leaves one available match, Stil opens it. Apps with a launch pause still wait for confirmation. With several matches, keep typing, tap a result, or use the keyboard's search action to open the first available match. Empty searches and unavailable apps do not auto-launch.

Predictive keyboards can leave the current word underlined. Single-match launch now works with that composing text too; it does not wait for a space or the search key. Moving the cursor or returning to a saved search does not open an app.

The slide-up drawer and automatic search launch are available in the [0.4.0 preview](https://github.com/tiaanduplessis/stil/releases/tag/v0.4.0). The [search video](media/app-search.mp4) shows the new flow. Install it over the signed 0.3.0 preview to keep your settings.

**Hide from Stil** removes an app from Stil's lists; it does not uninstall or disable it. Restore it under **Settings → Hidden apps**. These controls do not hide an app from Android settings or other launchers.

## Fonts and appearance

Choose **Light**, **Dark**, or **System** in Settings. Dark uses a black background. Font choices are local Android System, Sans, Serif, and Mono families. Alignment and text size affect the home list; Android's own font scaling also applies.

Navigation and selection use short transitions. Disable animations in Android's accessibility or developer settings to reduce motion. The launch-pause screen and cancellation stay immediate.

## Wallpapers

- **Off** uses Stil's plain theme background.
- **Daily** schedules random photos and checks for a new local date when Stil resumes. Android may delay background work.
- **Keep** stops automatic changes. **Next image** still fetches another photo when you request one.
- **Dim** keeps text legible over the photo. Downloads use unmetered networking by default through **Wi-Fi downloads**.

The bundled forest appears until a downloaded photo is available. A failed download keeps the current image. Photographer credit in Wallpaper settings opens the photo's source.

Stil requests a system/home wallpaper update. It does not request a lock-screen update. **Off** changes Stil's background but does not restore the previous system wallpaper; the last applied photo may still appear elsewhere in Android.

## Launch pause

Enable **Launch pause** for an app through its edit screen. Wait three seconds, then tap **Open**. **Cancel** returns without launching. This applies only to launches made through Stil.

## Launch stats

Open **Settings → Launch stats** to see how often you open apps through Stil. Choose **Today**, **7 days**, or **30 days**. Each period includes today and uses your device's local dates. The total, daily chart, and ranked app counts update together. Counts start when you install a build with this feature; earlier opens cannot be recovered.

Home taps, drawer taps, keyboard search, and automatic single-match launches all count once after Stil opens the app. A launch pause counts only after **Open** succeeds. Canceled pauses and failed launches do not count. These are open counts, not screen time; notifications, recents, and other launchers are outside the totals.

Counts stay on this device. Private space is never recorded. Hidden and uninstalled apps are excluded from every view; restoring a hidden app can reveal counts still within the last 30 days. Personal and work copies remain separate. The current source includes this feature; the signed 0.4.0 preview predates it.

## Work apps and Private space

Work apps have a visible **Work** label and their own profile controls. On Android 8.0–8.1, work-profile controls open Android settings. Paused or unavailable profiles cannot launch apps through Stil.

Private space requires Android 15 or later, an existing private profile, and Stil as the default home app. Expand its container to search within it; **Lock** removes its app entries. Private apps never appear in global search or home pins. **Show Private space** controls whether the container is shown.

Profile setup, unlock prompts, gestures, and availability depend on Android and your device.

## Troubleshooting

| Problem | What to try |
| --- | --- |
| Home still opens another launcher | Choose Stil in **Settings → Default home app**, then press Android's Home control. |
| An app is missing | Check **Hidden apps**, its work/private profile, and whether it is installed. Return to Stil to refresh the list. |
| A wallpaper did not change | Check the network and **Wi-Fi downloads** setting. Use **Next image**. Background work may be delayed; the previous image stays usable. |
| Private space is missing | Confirm Android 15+, a private profile, the default-home role, and **Show Private space**. |
| You want to stop using Stil | Select another home app in Android's default-app settings before uninstalling. |

For reproducible bugs, [open an issue](https://github.com/tiaanduplessis/stil/issues/new/choose) with Android version, device model, steps, and the expected result. Keep personal app names, account details, and message contents out of captures.

## Fold and flip devices

Available in the signed [0.3.0 preview](https://github.com/tiaanduplessis/stil/releases/tag/v0.3.0).

Open or close the device as usual. Narrow windows use one column; wider windows place the clock beside your apps. In tabletop posture, the clock sits above the fold and apps sit below it. The left/right alignment setting chooses the app side. Large text and short panes scroll when needed.

Swipe up at either safe bottom edge on a book fold to open All apps. Hold empty space for Settings. Your search, cursor position, unsaved name, and launch pause continue when the window changes. Cover-display access depends on Android and the device maker; Stil does not install a separate cover-screen widget.
