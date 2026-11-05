# BAFF Launcher

A lightweight, ad-free Android home screen with an iOS-inspired visual style.

> Early development — version 0.2.0. This milestone introduces an Android 9+ baseline, a wallpaper-backed home screen, a separated dock, an app-drawer entry point, and a first launcher settings screen.

## Goals

- Android 9.0 (API 28) and newer.
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

## Compatibility and first launch

The app declares `minSdk 28`. MainActivity is available both as a regular app (`MAIN` + `LAUNCHER`) and as a HOME activity. On first launch, BAFF offers to open Android's default-home-app settings; the user must select BAFF there.

The home screen requests the existing system wallpaper rather than replacing it.

## License

License not selected yet. Contributions and redistribution terms will be defined before the first public release.
