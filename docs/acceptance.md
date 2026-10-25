# Initial native release

The baseline is the HTML prototype. No Android application existed before this implementation.

| Requirement | Expected result | Proof |
| --- | --- | --- |
| Default home and setup | Choose 1–6 apps, preview, request Android home role; a visible route changes the default later | Instrumented setup tests and emulator role check |
| App discovery and launch | Enumerate real launcher activities by component and user serial; react to package changes; start the selected activity | Catalog tests and actual app launch on emulator |
| Search | Find original names and aliases; confirm before opening; hide excluded apps | Domain tests and Compose tests |
| Customization | Pin, unpin, rename, reorder, hide, recover; keep 1–6 valid pins | Domain invariants, persistence and UI tests |
| Appearance | Light/black/system, four font presets, alignment, type sizes; survive process restart | DataStore and device checks |
| Pause | Optional per app, off initially; wait three seconds and require Open; Cancel never launches | Timing and Compose tests, device journey |
| Wallpapers | Optional daily random photo, skip, keep, off, dimming; cache and retain last good image on network failure | Provider, policy, repository and worker tests; live provider/device check |
| Profiles | Separate work/private containers; paused/locked profiles cannot launch or leak into global search | Domain isolation tests and available profile/device checks |
| Privacy and accessibility | No accounts/analytics; local core, optional photo networking; scalable type, RTL, semantics and touch targets | Manifest/lint, source review, large-text/device checks |
| Delivery | Reproducible wrapper build, debug APK, tests, lint, scoped commit and PR | Build receipts, exact revision, PR checks when hosting is available |

Minimum Android version: 8.0 (API 26). Target and compile SDK: 36. The wallpaper provider is Lorem Picsum, with photographer/source attribution. It requires no account or secret. Core use and the bundled fallback remain offline. Photo delivery by this third-party service is best effort.

Generated run artifacts belong in ignored `.artifacts/`, not in source commits. Profile and OEM behavior that cannot be exercised must be reported separately from passing tests.
