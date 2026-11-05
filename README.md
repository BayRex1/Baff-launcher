# BAFF Launcher

A lightweight, ad-free Android home screen with an iOS-inspired visual style.

> Early development — version 0.4.0. This milestone separates the normal BAFF app/settings screen from the HOME launcher, adds a full-screen app drawer with swipe-to-dismiss, AMOLED/dark/light drawer themes, and snap-style horizontal home pages.

## Compatibility

- Android 9.0 (API 28) and newer.
- Native Kotlin UI; no third-party runtime dependencies.
- Rounded-square app icons and existing system wallpaper.
- No ads, trackers, or subscriptions.

## Navigation

- Swipe left/right across the home icons to switch home pages. A new page is created when more than 12 apps are pinned.
- Swipe up anywhere on the home screen to open the app drawer.
- Swipe down anywhere in the app drawer, or tap ×, to close it.
- Long-press an app to pin/unpin it, rename it, choose a custom image icon, or request uninstall through Android.
- The regular BAFF Launcher app icon opens a dedicated settings screen; the HOME activity is separate.

## Appearance

- App drawer themes: AMOLED black (default), dark, and light.
- Icon styles: original, monochrome, preset tints, custom HEX tint, and per-app custom image.
- Translucent dock and short app launch animations.

## Build

Requirements: JDK 17 and Gradle 8.9.

```bash
gradle assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

This is an early launcher implementation. App uninstall is delegated to Android's system confirmation flow. License terms have not yet been selected.
