---
name: run-android
description: Build and install the btOfflineMap companion Android app on a connected device or emulator.
---

# run-android

Build and launch `android/companion-app` on a connected device/emulator.

## Steps

1. Confirm a device/emulator is attached:

       adb devices

   If nothing is listed, either start an emulator (`emulator -avd <name>`) or connect a
   physical device with USB debugging enabled.

2. Build and install:

       cd android/companion-app
       ./gradlew installDebug

3. Launch the app (adjust the application id if it changes from `com.btofflinemap.companion`):

       adb shell am start -n com.btofflinemap.companion/.MainActivity

4. To just verify the build compiles without installing anything (e.g. no device attached):

       ./gradlew assembleDebug

## Notes

- Requires the Android SDK to be installed — see `android/CLAUDE.md` for setup if
  `./gradlew` fails with an SDK-location error.
- Uses the project's own Gradle wrapper; no system-wide Gradle install needed.
