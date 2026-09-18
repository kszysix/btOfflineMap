# Garmin <-> Android protocol (v1)

Source of truth for the wire format. See `docs/PROJECT_PLAN.md` §33/§34 for the full message
list this will grow into; this file only documents what's actually implemented so far
(Phase 1: `HELLO`, `HELLO_ACK`, `GPS_UPDATE`).

There is no shared code between the watch (Monkey C) and phone (Kotlin) — they're different
languages/runtimes — so this document, not a shared library, is what keeps both sides in
sync. Update it in the same change as any wire-format edit on either side.

## Transport

Not raw BLE. Connect IQ apps communicate with a phone through the Garmin Connect Mobile
(GCM) app as a relay:

    watch app --Toybox.Communications--> GCM app --(Bluetooth, opaque)--> GCM app
        --Connect IQ Mobile SDK (com.garmin.android.connectiq)--> our Android app

Messages are native Monkey C `Dictionary`/`Array`/primitive values (not JSON text, not raw
bytes) — `Communications.transmit()` serializes them, and the Mobile SDK hands the phone side
back an equivalent `List`/`Map` in `onMessageReceived`.

## Message envelope

Every message is a dictionary/map with at least:

| key | type | meaning |
|---|---|---|
| `v` | Number | protocol version, currently `1` |
| `type` | String | message type, e.g. `"HELLO"` |
| `seq` | Number | sequence number (§34) — meaning is per-message-type, see below |

Extra keys are added per message type.

## Messages

### `HELLO` (watch -> phone)

Sent once when the watch app starts, to announce it's alive and trigger the phone side to
respond.

    { "v": 1, "type": "HELLO", "seq": 0 }

`seq` is always `0` for `HELLO` — it's not part of the GPS sequence.

### `HELLO_ACK` (phone -> watch)

Sent by Android in response to a received `HELLO`.

    { "v": 1, "type": "HELLO_ACK", "seq": 0 }

### `GPS_UPDATE` (watch -> phone)

Sent on every GPS fix the watch receives (§28: Garmin is the primary GPS source).

    {
      "v": 1,
      "type": "GPS_UPDATE",
      "seq": 1042,
      "lat": 52.123456,
      "lon": 16.123456,
      "alt": 78.3,
      "ts": 123456789
    }

- `lat`/`lon`: degrees (WGS84), as returned by `Position.Info.position.toDegrees()`.
- `alt`: meters, may be absent/invalid depending on GPS fix quality — treat missing as
  "no altitude yet," not an error.
- `seq`: monotonically increasing per watch-app session, starting at 0. Phase 1 does not yet
  do anything with gaps in this sequence (no store-and-forward/resync) — that's §34, later.
- `ts`: watch-side monotonic timer (`System.getTimer()`, milliseconds since boot), **not**
  wall-clock time. Not yet used for anything on the Android side in Phase 1; kept for when
  activity timing (§29) needs it.

## Application identity

Both sides must reference the same Connect IQ application id for `registerForAppEvents`/
`sendMessage` (phone side) to match the running watch app:

    c68fb880-5658-43be-8abf-14eb037cab7a

Defined in `garmin/fr255-map-app/manifest.xml` (`iq:application id="..."`) and in
`android/companion-app` as `GarminConnectionManager.WATCH_APP_ID`. If the watch app is ever
recreated via the SDK's own project-creation tooling with a fresh generated id, this doc and
both source locations must be updated together.

## Not yet implemented

Everything in §33 beyond the three messages above (`MAP_REQUEST`/`MAP_UPDATE`,
`ZOOM_IN`/`ZOOM_OUT`, the `ACTIVITY_*` messages, `TRACK_APPEND`, `ERROR`) — later phases.
