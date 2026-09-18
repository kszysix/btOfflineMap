# android/ — companion app

Kotlin + Jetpack Compose Android app. This is the "brain" (§2 of `docs/PROJECT_PLAN.md`):
offline map storage/rendering decisions, activity tracking, statistics, and the Garmin BLE
link all live here.

## Layout

    android/companion-app/    Gradle project (single `app` module for now)

## Prerequisites

- JDK 17+ (present on this machine: OpenJDK 17.0.20 — no action needed).
- Android SDK — installed at `~/Android/Sdk`, with `ANDROID_HOME`/`ANDROID_SDK_ROOT` set in
  `~/.bashrc`. `./gradlew assembleDebug` verified working (Phase 0).
- No system-wide Gradle needed — the project uses the committed Gradle wrapper (`gradlew`).

### Installing the Android SDK (command-line only, no Android Studio)

1. Download the command-line tools (Linux) from
   https://developer.android.com/studio#command-tools — grab the "Command line tools only"
   zip for Linux.
2. Unpack it into the SDK root, in the exact nested layout `sdkmanager` expects:

       mkdir -p ~/Android/Sdk/cmdline-tools
       unzip commandlinetools-linux-*.zip -d ~/Android/Sdk/cmdline-tools
       mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

3. Set environment variables (add to `~/.bashrc` or `~/.zshrc`):

       export ANDROID_HOME="$HOME/Android/Sdk"
       export ANDROID_SDK_ROOT="$ANDROID_HOME"
       export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

4. Install the packages this project needs, and accept licenses:

       sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
       sdkmanager --sdk_root="$ANDROID_HOME" --licenses

5. Verify: `sdkmanager --list_installed` should show the three packages above, and
   `adb --version` should work (a system `adb` already exists at `/usr/bin/adb`, but the
   SDK's own copy under `platform-tools/` is what Gradle will use).

No emulator system-image install is required to just run a Gradle build; only for actually
launching the emulator (`sdkmanager "system-images;android-34;google_apis;x86_64"` plus
`avdmanager create avd ...`, or use a physical device over USB/`adb`).

## Build & run

    cd android/companion-app
    ./gradlew assembleDebug          # build only
    ./gradlew installDebug           # build + install to a connected device/emulator

Or use the `run-android` skill (`.claude/skills/run-android`), which wraps this.

## Conventions

- Kotlin, Jetpack Compose, Coroutines/Flow — no other UI or DI framework introduced without
  a stated reason (see root `CLAUDE.md` working style / §0).
- Package/application id: `com.btofflinemap.companion`.
- `minSdk 26`, `compileSdk`/`targetSdk 34`. Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20 — see
  `android/companion-app/gradle/libs.versions.toml` for the pinned versions; bump
  deliberately, not silently, and note why in a commit if it's more than a patch bump.
- Architecture layers to build up over the phases (`docs/PROJECT_PLAN.md` §16, §36):
  `MapRepository` -> `MapDataSource` -> `PMTilesReader` -> `MapEngine`, and a separate
  `ActivityTrackingService` / `GarminConnectionManager` / UI split (§36). Nothing here yet —
  Phase 0 only creates the empty-shell app; these appear starting Phase 1.
- Don't add Room or networking code until the phase that needs it (§39/§60).

## Connect IQ Mobile SDK (Phase 1+)

`GarminConnectionManager` (`app/src/main/kotlin/.../GarminConnectionManager.kt`) depends on
`com.garmin.android.connectiq`, distributed as a single `.aar` (not on Maven). Vendored at
`app/libs/connectiq-mobile-sdk-android-1.5.aar`, **gitignored** — not committed as vendor
binary policy.

To set up on a fresh checkout:

    mkdir -p android/companion-app/app/libs
    curl -o android/companion-app/app/libs/connectiq-mobile-sdk-android-1.5.aar \
      https://developer.garmin.com/downloads/connect-iq/sdks/connectiq-mobile-sdk-android-1.5.aar

Referenced in `app/build.gradle.kts` as `implementation(files("libs/connectiq-mobile-sdk-android-1.5.aar"))`.

This SDK requires the **Garmin Connect Mobile** app installed on the test phone, with the
FR255 paired through it — it talks to the watch via that app as a relay, not raw BLE (see
`shared/protocol/protocol.md` "Transport"). `ConnectIQ.IQConnectType.WIRELESS` (what we use)
needs a real paired watch; `TETHERED` mode (connects to the desktop simulator over ADB
instead) exists in the SDK but isn't wired up here — could be worth it later for faster
iteration without a physical device.
