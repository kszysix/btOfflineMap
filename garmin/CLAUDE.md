# garmin/ — FR255 watch app

Connect IQ (Monkey C) app for the Garmin Forerunner 255. This is the constrained client
(§2, §3 of `docs/PROJECT_PLAN.md`): GPS, display, buttons, lightweight map rendering,
activity controls. It never touches PMTiles/OSM/map databases directly — only a small
render protocol from Android.

## Layout

    garmin/fr255-map-app/     Connect IQ project (manifest.xml, monkey.jungle, source/, resources/)

## Prerequisites

- **Connect IQ SDK — installed.** SDK 9.2.0 at
  `~/.Garmin/ConnectIQ/Sdks/connectiq-sdk-lin-9.2.0-2026-06-09-92a1605b2/`, with
  `CONNECTIQ_HOME` and `PATH` set in `~/.bashrc`. `monkeyc`/`connectiq` work from any shell
  after `source ~/.bashrc`.
- **FR255 device profile — installed** via the SDK Manager's separate "Devices" tab (this is
  not part of the SDK download itself). Lives at `~/.Garmin/ConnectIQ/Devices/fr255/`.
- `garmin/fr255-map-app` is **build-verified**: `monkeyc -f monkey.jungle -d fr255 ...`
  produces `BUILD SUCCESSFUL` (Phase 0).

### Installing the Connect IQ SDK (for reference / a fresh machine)

1. Download the **Connect IQ SDK Manager** from
   https://developer.garmin.com/connect-iq/sdk/ (Linux tarball) and extract it, e.g. to
   `~/connectiq-sdk-manager/`.
2. Run it — this needs a real display (it's a GUI Java app, no headless/CLI mode):

       cd ~/connectiq-sdk-manager
       ./bin/sdkmanager

3. In the **SDK** tab: accept the license, install the current stable Connect IQ SDK.
4. In the separate **Devices** tab: download the **Forerunner 255** device profile. This step
   is easy to miss — the SDK install alone does not include any device profiles, and
   `monkeyc -d fr255 ...` fails with `Invalid device id specified: 'fr255'` until this is
   done.
5. Add to `~/.bashrc` (adjust the SDK folder name to whatever version you installed):

       export CONNECTIQ_HOME="$HOME/.Garmin/ConnectIQ/Sdks/<installed-sdk-folder>"
       export PATH="$PATH:$CONNECTIQ_HOME/bin"

   Verify: `monkeyc --version` should print a compiler version.

6. **Developer key** — required to sign a `.prg` for the simulator or a real device. Monkey C
   wants a **PKCS8 DER**-encoded key, which is a two-step `openssl` process (`genrsa` alone
   cannot produce this format directly, despite what its `-outform` flag suggests):

       openssl genrsa -out developer_key.pem 4096
       openssl pkcs8 -topk8 -inform PEM -outform DER -in developer_key.pem -out developer_key.der -nocrypt

   The root `.gitignore` already excludes `garmin/**/developer_key.*`, `garmin/**/*.der` and
   `garmin/**/*.pem`, so a key generated anywhere under `garmin/` won't get committed by
   accident. Store it there (e.g. `garmin/fr255-map-app/developer_key.der`), not in `/tmp` —
   the same key must be reused across builds once you're signing for a real device, since a
   different key produces a different app identity.

The simulator (`monkeyc` + `connectiq`) is enough to validate that the Monkey C code compiles
and runs its own logic (e.g. drawing the view, GPS callback wiring). It **cannot** validate
the actual watch<->phone link, though — that's mediated by the real Garmin Connect Mobile app
on a real phone (see `android/CLAUDE.md` "Connect IQ Mobile SDK"), which the simulator doesn't
replicate. So end-to-end Phase 1 verification (§39: watch GPS reaching Android) needs
physical hardware: the app side-loaded onto a real FR255, paired to a phone running Garmin
Connect Mobile, with `android/companion-app` installed on that same phone.

## Build & run

    cd garmin/fr255-map-app
    monkeyc -f monkey.jungle -d fr255 -o bin/app.prg -y <path-to-developer_key.der> -w
    connectiq                       # launch simulator, then load bin/app.prg

`bin/` is gitignored — it's a build output directory, regenerate it with the command above.

This will be wrapped in a `run-garmin-sim` skill once this has been typed by hand a couple
of times (`docs/PROJECT_PLAN.md` §74) — not created yet, per the "don't pre-build a skill for
a procedure that hasn't been run yet" rule.

## Conventions

- Monkey C, Connect IQ Watch App project type (not a widget/data-field).
- Keep the app minimal: render primitives only (`docs/PROJECT_PLAN.md` §23) — no
  general-purpose styling engine, no attempt to understand map data formats.
- Target device: Forerunner 255 (260x260, 64-color, no touchscreen) — don't build UI that
  assumes touch input or a larger palette.
- `method(:symbolName)` (used to hand a callback to `Communications`/`Position` APIs) needs
  an object instance as `self` — it does not work from a bare top-level `module` function.
  This is why `GarminLink.mc` is a thin `module` (singleton accessor for the View) wrapping
  a `class GarminLinkService` that does the actual work.

## Wire protocol (Phase 1+)

`source/GarminLink.mc` implements the watch side of `shared/protocol/protocol.md`: sends
`HELLO` on app start, streams `GPS_UPDATE` on every `Position` fix, and reacts to
`HELLO_ACK`. The manifest's `iq:application id` must stay in sync with
`GarminConnectionManager.WATCH_APP_ID` on the Android side — see that doc's "Application
identity" section before changing either.
