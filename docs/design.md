# Design direction

Stil uses typography and empty space to make a short list of chosen apps the main interaction. Home is deliberately quiet. Controls become denser only when someone asks to find an app or change a setting.

## Tokens

- Light background: `#F7F8FA`; light foreground: `#20242B`.
- Dark background: `#000000`; dark foreground: `#F3F4F6`.
- Muted light text: `#626873`; muted dark text: `#A3A8B1`.
- Controls use neutral borders and inverse fills; no attention-seeking brand accent.
- Font: System, Sans, Serif, or Mono across the launcher. HTML uses local font stacks without remote downloads. Android uses its local system font families.
- App labels: 28, 32, or 36 CSS pixels in the prototype, with at least 52px rows. Native type should use scalable `sp` units and adapt the list when system type grows.
- Home: left aligned by default, optional right alignment; chosen apps sit in the lower portion. The clock anchors the otherwise open upper area.
- Native list and settings screens share one structure: a 48dp navigation bar, a headline title, then rows. The one-time home prompt stays centered. App names use the title-large role; setting labels use body-large with muted supporting text or values. Spacing, not dividers, separates rows. Chips and buttons share a 40dp visual height with 48dp touch targets.

## Foldable Home

Home follows the available window, not a device-model list. Narrow windows retain a single column. Wide windows place apps on the chosen side and the clock across from them. Tabletop posture places the clock above the fold and apps below. Separating folds and hinges have a 12dp clear margin. The 56dp bottom gesture strip stays outside the app scroll area; book posture exposes it on both safe bottom edges. No extra labels, controls, or posture animations are added.

Large type can fall back to one safe pane. App lists scroll. Other screens retain one reading column, capped at 600dp; short panes combine navigation and title in one header row.

## Layout sketch

```text
Home                       All apps
time / system status       time / system status
                           Back           Edit
09:41                      All apps
Sunday, 25 October         local results
                           ...
                           ...
Phone                      ...
Messages                   ...
Maps                       ...
Camera                     Search apps
Spotify                    Home / system navigation
swipe up for apps
```

The first design pass considered a decorative wordmark inside the home screen and a focus-session badge. Both were removed: they occupied space without helping someone open an app. The brand belongs in setup and settings. Black and white are deliberate choices for this launcher, not a theme applied to every screen around it.

## Review the prototype

Open `mockups/index.html`. The navigation above the phone switches between independent mockup states. Inside the phone, normal buttons link the flows.

1. Home: open an essential app and return. Switch Day/Night outside the phone.
2. Apps: search `insta`, then tap Instagram. A three-second pause ends with an explicit Open button; Cancel returns without launching.
3. Apps: enter a nonsense query to see the empty state. Search an existing app and press Enter to launch it.
4. Settings: change alignment and text size; open Home apps.
5. Home apps: select an app, rename it, move it, unpin it, hide it, or add a pause. Hidden apps can be restored from Settings.
6. Setup: choose up to six essentials and simulate choosing Stil as the default home. This does not change the browser or the device launcher.
7. Reload: supported browser storage retains demo preferences. Reset demo returns to the original sample state.

The browser's own keyboard handles text input; the mockup does not draw a fake keyboard. Phone hardware, system indicators, and Android app launches are illustrative. Swipe-up and long-press shortcuts are not implemented; visible controls cover every demonstrated action.

The current native home removes those navigation controls. Swipe up in the empty bottom area to open All apps; hold empty space to open Settings. Holding a pinned app still edits it. The bottom gesture area sits above Android's navigation bar and outside the scrollable app list. Home exposes both routes as accessibility actions and supports keyboard navigation. Setup explains the gestures once; home adds no hint text or icons.

The mockup is responsive at narrow widths and supports keyboard focus and reduced motion. Native accessibility, system font scaling, profiles, and OEM compatibility still require Android implementation and device testing.

## UI copy

Use short action labels. Keep review notes and research collapsed by default. The pause screen shows the app, countdown, and Open/Cancel controls. Setup asks users to choose home apps.

## Font and wallpaper controls

Settings > Font previews each family. The choice persists across reloads. Settings > Wallpaper provides Off, Daily, and Keep modes, Next image, Preview home, and Dim. Only home shows the wallpaper; settings and search keep solid backgrounds.

Try Daily, preview home, then return and use Next day below the phone to simulate daily rotation. Keep holds the current image. Off restores the selected light or dark theme. The three sample backgrounds are generated gradients; no photo files or downloads are needed for the mockup.
