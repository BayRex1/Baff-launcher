# BAFF Launcher

A lightweight, ad-free Android home screen with an iOS-inspired visual style.

> Early development — version 0.3.0. The current milestone adds horizontal home pages, swipe-up app drawer, long-press app actions, persistent home-screen selection and labels, custom icon images, icon color styles, a translucent dock, and app launch animations.

## Compatibility

- Android 9.0 (API 28) and newer.
- Native Kotlin UI; no third-party runtime dependencies.
- Rounded-square app icons.
- System wallpaper is retained behind the launcher.
- No ads, trackers, or subscriptions.

## Home screen

- Swipe horizontally to move between home pages.
- Swipe upward to open all apps.
- Long-press an app to add/remove it from the home screen, rename it, choose a custom image icon, or request uninstall through Android.
- Icon styles include original, monochrome, preset tints, custom HEX tint, and a user-selected image.
- The bottom dock uses a translucent glass-like panel.

## Build

Requirements: JDK 17 and Gradle 8.9.

```bash
gradle assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

This is an early launcher implementation. The dock is translucent rather than a true real-time blurred surface to retain compatibility with Android 9. App removal is confirmed by Android's system uninstall flow. License terms have not yet been selected.
