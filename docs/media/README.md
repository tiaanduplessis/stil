# README media

These are captures of the native Android app on an Android 16 emulator. No HTML mockup or generated phone UI is used. Home, Settings, and the original walkthrough were captured on 25 October 2026 and show the typography and spacing refinements in the 0.2.1 preview.

- `home.png`: Serif home with five apps in Dark appearance, using gestures instead of navigation buttons.
- `apps.png`: refreshed on 26 October 2026, showing Calendar and Camera for `ca` in Dark appearance with Serif. The on-screen keyboard has been dismissed.
- `settings.png`: Dark appearance selected; the saved setting remains compatible with existing installs.
- `walkthrough.mp4`: hold empty home space → Settings → Font → Serif → Preview home → swipe up → Clock search. Android's touch indicators show the gestures. Motion runs at normal speed; trailing idle time was removed.
- `app-search.mp4`: captured on 26 October 2026 from the unreleased search update. Swipe up → tap Search apps → tap Gboard's `c` and `l` keys → Clock opens automatically. The 240 ms drawer transition and keyboard motion run at normal speed. The 0.3.0 download uses the earlier search behavior.
The README embeds the video through GitHub's hosted player. The MP4 is also kept here for local playback and as a portable fallback. Access to the hosted attachment follows GitHub's repository permissions. Recheck playback after a fork or visibility change.

These captures use a plain background. Earlier README media in Git history used [James Forbes's forest photo](https://unsplash.com/photos/jrzvClypPq8), downloaded through Lorem Picsum (photo 190), under the [Unsplash License](https://unsplash.com/license). The app's offline fallback and prototype sample credits are listed in [third-party notices](../../THIRD_PARTY_NOTICES.md).

`fold-wide.png` and `fold-tabletop.png` were captured on 26 October 2026 from the Android 16 Pixel Fold emulator. They show the fold layouts released in 0.3.0, with five apps, Dark appearance, Serif, and right alignment. The wide capture uses the open inner display; tabletop uses the half-open state rotated so the fold is horizontal. They are native app captures with no drawn hinge or device frame.

Refresh these assets after visible UI changes. Use a dedicated emulator without personal data and capture at least home, app search, and settings. Keep text alternatives and the [text walkthrough](../usage.md#walkthrough) current. Check video playback in the rendered README rather than assuming an embed works.

These files are intentional documentation assets requested by the maintainer. Raw recordings, capture scripts, and verification logs remain in ignored `.artifacts/`.
