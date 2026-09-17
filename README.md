# Feedback Safety App

Personal safety and self-monitoring application designed for Android 13 Go Edition (Target device: Redmi A2+).

## Features
- Detects when Instagram is in the foreground using UsageStatsManager.
- Records screen captures (1 frame every 2 seconds) while Instagram is active.
- Uses Android's official MediaProjection API.
- Stores JPEG images at 70% quality in app-specific storage.
- Auto-cleanup for old captures via RetentionManager.
- Minimalist Material 3 Jetpack Compose UI.

## Build Requirements
- JDK 17
- Android SDK (minSdk 26, targetSdk 33)
- Gradle 8.2

## How to Build
1. Open this project in Android Studio.
2. Wait for Gradle sync to complete.
3. Run `Build -> Build Bundle(s) / APK(s) -> Build APK(s)`
4. Install the resulting `app-debug.apk` on your device.

## Testing on Device
1. Grant **Usage Access** via the main screen.
2. Click **Start Safety Session** and accept the system Screen Capture consent dialog.
3. Open Instagram - the app will begin taking screenshots silently in the background (visible via the foreground service notification).
4. Close Instagram - the app will pause capturing.
5. You can view storage usage and delete captures from the main app interface.
