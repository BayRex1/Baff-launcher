# BAFF Launcher — architecture

## Product constraints

- Minimum Android version: 7.0 (API 24).
- No advertising, analytics SDKs, trackers, or subscription gates.
- Performance is a product feature: avoid permanent services, heavy blur, and unnecessary redraws.
- iOS-inspired visual language, implemented with native Android components.
- App icons are presented in a rounded-square frame. Adaptive icon foreground/background layers are rendered separately where supported; legacy icons retain their source artwork inside the same frame.

## Runtime layers

| Package | Responsibility |
| --- | --- |
| `apps` | Discover launchable activities and expose a small app model. |
| `icons` | Render and cache app icons into a consistent rounded-square bitmap. |
| `launcher` | Home screen, favorites, app drawer, and user interaction. |
| `animation` | Motion policy and reusable transitions (next milestone). |
| `settings` | Persistent user preferences (next milestone). |

The first prototype intentionally keeps the runtime dependency-free and uses Android Views. This makes the initial UI straightforward to profile on API 24 devices and avoids pulling a large UI stack into a lightweight launcher.

## Threading and memory

- Package discovery currently runs during activity creation; move it to a background executor as the app list grows.
- Icon rendering is dispatched to a single worker thread.
- A bounded LRU cache avoids repeatedly decoding the same app icons.
- UI updates are posted back to the main thread.
- The worker executor is shut down with the activity.

## Compatibility

- `minSdk = 24`.
- Launcher intent is declared with `MAIN`, `HOME`, and `DEFAULT`.
- Launcher activity discovery is declared in `<queries>` for Android 11+ package visibility.
- API-specific adaptive icon handling is guarded by an Android version check.
- The current prototype uses a conservative target SDK while behavior is tested across Android versions. Raise it after validating package discovery, edge-to-edge behavior, and system gesture navigation.

## First milestones

1. **0.1** — HOME role, launchable app discovery, rounded-square icon renderer, basic home screen and searchable app drawer.
2. **0.2** — persistent favorites, user-controlled grid, app sorting, reload on package install/remove.
3. **0.3** — settings screen, icon size, label visibility, light/dark/system theme.
4. **0.4** — gesture actions and short, interruptible transitions; add a reduced-motion switch.
5. **0.5** — profile memory/CPU and frame pacing on low-end devices; verify Android 7 through current Android releases.

## Build

The GitHub Actions workflow uses JDK 17 and Gradle 8.9 to assemble a debug APK and upload it as an artifact. No APK is committed to the repository.
