# QR Scanner

A private, camera-first Android QR scanner built with Kotlin, Jetpack Compose, CameraX, and ML Kit.

## MVP features

- Live CameraX preview with QR-only ML Kit detection
- Camera permission flow
- Scan reticle and flashlight control
- Result card with copy, safe web-link opening, and scan-again actions
- Haptic feedback after a successful scan
- Instrumented launch test on an Android emulator

## Requirements

- Android Studio with JDK 17
- Android SDK 36
- Android device running Android 6.0 (API 23) or newer

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Verify

With an emulator or Android device connected:

```bash
./gradlew connectedDebugAndroidTest lintDebug assembleDebug
```
