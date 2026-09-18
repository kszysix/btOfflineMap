# Status

One page, kept current. See `docs/PROJECT_PLAN.md` for the full plan and reasoning behind
any of this — this file only tracks where things stand right now.

## Track A — device application (Part I, §39/§60)

**Phase 0 — Tooling, repository, and agent-friendly setup: complete.**

Done:
- GitHub repo created and remote configured (`origin` -> `kszysix/btOfflineMap`).
- Root `CLAUDE.md`, `android/CLAUDE.md`, `garmin/CLAUDE.md`.
- `docs/STATUS.md` (this file), `docs/decisions/` (0001 PMTiles, 0002 Java for backend).
- `.claude/skills/run-android` skill.
- Android SDK installed (`~/Android/Sdk`) and `android/companion-app` **build-verified**:
  `./gradlew assembleDebug` -> `BUILD SUCCESSFUL`.
- Connect IQ SDK 9.2.0 installed plus the FR255 device profile, and
  `garmin/fr255-map-app` **build-verified**: `monkeyc -f monkey.jungle -d fr255 ...` ->
  `BUILD SUCCESSFUL`.

Not yet done (not blockers, just not started):
- `.claude/skills/run-garmin-sim` — deferred until it's been typed by hand a couple of times
  (§74's rule: don't pre-build a skill for an unused procedure).

**Phase 1 — Garmin <-> Android communication: code complete, not yet hardware-verified.**

Done:
- `shared/protocol/protocol.md` — HELLO / HELLO_ACK / GPS_UPDATE wire format (§33).
- Watch side (`garmin/fr255-map-app`): `GarminLink`/`GarminLinkService` sends `HELLO` on
  launch, streams `GPS_UPDATE` on every GPS fix, displays link status + last fix on screen.
  Build-verified (`monkeyc` -> `BUILD SUCCESSFUL`).
- Android side (`android/companion-app`): `GarminConnectionManager` using the Connect IQ
  Mobile SDK (`com.garmin.android.connectiq`, vendored — see `android/CLAUDE.md`), replies
  to `HELLO` with `HELLO_ACK`, displays connection status + last GPS fix.
  Build-verified (`./gradlew assembleDebug` -> `BUILD SUCCESSFUL`).

Not yet done:
- **Real end-to-end verification on hardware** — install both apps and confirm a live GPS
  fix from the FR255 actually reaches and displays on the Android app. This is the actual
  Phase 1 acceptance test (§39) and hasn't been run yet; everything above is compiled but
  unverified against a real Bluetooth link.

Next: side-load `garmin/fr255-map-app` onto the FR255 and install `android/companion-app`
on a phone with Garmin Connect Mobile paired to it, then verify the end-to-end GPS flow.
Do not start map work before Phase 1–3 (communication, activity, track) are done — see §60.

## Track B — backend, cloud, delivery (Part II, §71)

**Not started.** Track B begins once Track A Phase 2 (activity lifecycle + database) is
done — it does not depend on the map engine. See `docs/PROJECT_PLAN.md` §71.2/§71.3 for the
full B1–B10 order.

## Toolchain (as of Phase 0 completion, 2026-09-17)

| Tool | Status |
|---|---|
| JDK | 17.0.20 (OpenJDK) present |
| Gradle | No system-wide install; each project uses the committed Gradle wrapper instead |
| Android SDK | Installed at `~/Android/Sdk` (platform-tools, platforms;android-34, build-tools;34.0.0); `android/companion-app` build-verified |
| Garmin Connect IQ SDK / Monkey C / simulator | SDK 9.2.0 installed (`~/.Garmin/ConnectIQ/Sdks/...`) plus FR255 device profile; `garmin/fr255-map-app` build-verified |
| Git | 2.34.1, remote configured and reachable |
| GitHub CLI (`gh`) | Not installed (not currently needed — remote already exists, push works via git) |
| AWS CLI | 2.23.8 present (not needed until Track B / §40) |

See `android/CLAUDE.md` and `garmin/CLAUDE.md` for the install steps, for reference on a
fresh machine.
