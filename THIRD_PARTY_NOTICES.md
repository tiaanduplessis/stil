# Third-party notices

## Photos

The following bundled photos are by [Alvin Engler](https://unsplash.com/@englr) and are used under the [Unsplash License](https://unsplash.com/license). The project's MIT license does not replace those terms.

| Photo | Files |
| --- | --- |
| [Forest](https://unsplash.com/photos/aerial-view-of-trees-LnbWqObX0TQ) | `app/src/main/res/drawable-nodpi/wallpaper_forest.jpg`, `mockups/assets/forest.jpg` |
| [Lake](https://unsplash.com/photos/grass-near-body-of-water-uB1RyCeJl5A) | `mockups/assets/lake.jpg` |
| [Ferns](https://unsplash.com/photos/green-ferns-during-daytime-vIf2fJN1vdA) | `mockups/assets/ferns.jpg` |

The application downloads optional photos through [Lorem Picsum](https://picsum.photos/). It retains each photo's author and source URL and exposes that credit in Wallpaper settings. Downloaded photos retain their own rights and terms.

Earlier README screenshots and video in Git history show [James Forbes's forest photo](https://unsplash.com/photos/jrzvClypPq8), downloaded through Lorem Picsum (photo 190). That photo remains under the Unsplash License; its use in the documentation is separate from the project's MIT license. Current captures use a plain background.

## Gradle wrapper

The checked-in Gradle wrapper JAR and generated scripts are Gradle project material under Apache-2.0. The JAR includes its license at `META-INF/LICENSE`, and the scripts retain their copyright/license headers. The project MIT license does not relicense them.

## Libraries and tools

Stil uses Kotlin, Kotlin coroutines/serialization, AndroidX Jetpack libraries, and OkHttp. Their licenses remain with their respective authors. Dependency coordinates and pinned versions are in `app/build.gradle.kts` and `build.gradle.kts`.

Before distributing a signed APK, review the resolved runtime dependency graph and include any required license texts and notices for that artifact. This file records bundled-photo attribution; it is not a claim that a complete release dependency-license review has been performed.

No source was copied from Olauncher. Its repository informed the product research.
