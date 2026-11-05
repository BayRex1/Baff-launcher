# BAFF Launcher

A lightweight, ad-free Android home screen with an iOS-inspired visual style.

> Early development — version 0.1.0. The first milestone focuses on a working HOME activity, app discovery, rounded-square icons, and a minimal app drawer.

## Goals

- Android 7.0 (API 24) and newer.
- Lightweight native Kotlin UI with no third-party runtime dependencies.
- Rounded-square app icon presentation (not forced circular masks).
- Subtle, short animations with performance-first defaults.
- No ads, trackers, subscriptions, or unnecessary background services.
- APK builds through GitHub Actions.

## Build

Requirements: JDK 17 and Gradle 8.9.

```bash
gradle assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

- `apps/` — installed app discovery.
- `icons/` — icon rendering and rounded-square mask.
- `launcher/` — home screen and app drawer UI (being expanded).
- `animation/` — motion behavior (planned).
- `settings/` — user preferences (planned).

## Compatibility note

The app declares `minSdk 24`. Package visibility is declared for launcher activities. The first prototype uses a conservative target SDK while the Android 7–15 behavior is tested.

## License

License not selected yet. Contributions and redistribution terms will be defined before the first public release.
