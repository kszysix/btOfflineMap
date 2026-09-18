# Garmin 255 Offline Maps & Activity Companion

## 0. How to work with me on this project

**The goal of this entire project is that I learn - not that the agent produces a working
app as fast as possible.** If those two ever trade off against each other, choose learning.
A finished app I don't understand is a failed outcome for this project, even if it works.

Concretely, this means:

- **Explain before implementing, at every phase and stage boundary** (§39, §60, §71.3).
  Before writing code for a new phase/stage, state: what is about to be built, why this
  approach and what the alternatives were, and what I should expect to see when it works.
  I should be able to explain the choice afterwards without re-reading the diff.
- **I am in the loop for decisions, not just informed after the fact.** Anything that isn't
  already pinned down explicitly in this document (a library choice, a schema shape, a
  naming convention, how to structure a Kotlin coroutine scope) is a decision, not a detail
  - surface it and let me choose, or propose one option with the reasoning and let me
  confirm, rather than silently picking the first reasonable thing.
- **Stop at checkpoints.** Each MVP phase (§39) and each Track B stage (§71.3) ends with
  something concrete I can run/read myself - a build, a demo, a FINDINGS.md entry - before
  moving to the next one. Do not chain several phases together in one uninterrupted run.
- **Write the decision down, not just the code** (§74). Every "why this and not that" choice
  becomes a short ADR in `docs/decisions/`. That record is *for me* as much as for a future
  agent session - it's how I stay able to defend every choice later.
- **Explain failures before fixing them.** When something breaks, tell me what the error
  means and what you'd check, and let me reason about it with you before patching it
  silently. The debugging is the learning, not just the fix.
- **Do not pre-optimize or add abstractions "for later."** This project deliberately
  includes stages that are naive on purpose (§68's "deliberate breakage," the intentionally
  simple Stage A backend in §66) so that I see things break and understand why. Don't smooth
  those over out of good instincts - ask first if something looks like it's supposed to be
  naive.

This mode applies to both tracks and to §74's agent-friendly-setup work itself - the skills,
CLAUDE.md files, and ADRs are as much a way to keep *me* oriented as they are for a future
agent session.

---

## 1. Project vision

Build a private, experimental Android + Garmin Forerunner 255 application.

The project has two layers:

### Foundation — Offline Maps

The Android phone stores offline vector map data using PMTiles.

The Garmin Forerunner 255 acts as a lightweight map display.

The phone performs:

- offline map storage;
- map querying;
- map rendering decisions;
- viewport calculation;
- geometry simplification;
- activity storage;
- GPS processing;
- statistics.

The Garmin performs:

- GPS;
- display;
- physical button input;
- lightweight map rendering;
- activity controls.

### Activity Layer — Lightweight Strava-like experience

When the user starts an activity on the Garmin:

- Android creates an activity;
- Garmin GPS points are sent to Android;
- Android stores the complete GPS history;
- the current activity track appears on the Garmin;
- elapsed time is tracked;
- distance/elevation/pace statistics are calculated on Android;
- the completed activity is saved locally;
- Android can display the activity and its track afterward.

This is a private educational project, not a commercial product.

---

# 2. Core architectural principle

The Garmin 255 is a constrained client.

The Android phone is the brain.

Do NOT attempt to place a full Poland/Italy map database on the Garmin.

Architecture:

    Garmin 255
        |
        | GPS + user commands
        v
    Android
        |
        +-- offline PMTiles
        +-- map engine
        +-- activity engine
        +-- statistics
        +-- track database
        |
        | simplified map geometry
        v
    Garmin 255

The Garmin does not need to understand:

- OpenStreetMap;
- PMTiles;
- vector tiles;
- map databases;
- map styling;
- map downloading.

It only needs to understand a small rendering protocol.

---

# 3. Hardware target

Primary target:

- Garmin Forerunner 255
- Android phone

Do not initially optimize for other Garmin devices.

The Forerunner 255 has:

- 260x260 display;
- 64-color display;
- physical buttons;
- no touchscreen;
- constrained Connect IQ memory.

Therefore the Garmin UI must remain extremely lightweight.

---

# 4. Technology

## Android

Use:

- Kotlin;
- Android;
- Jetpack Compose;
- Coroutines;
- Flow where useful;
- Room/SQLite for activity data;
- Android foreground service for active tracking;
- Garmin Connect IQ Mobile SDK.

Use a modern stable Android version/toolchain.

Do not introduce unnecessary frameworks.

## Garmin

Use:

- Garmin Connect IQ SDK;
- Monkey C;
- Connect IQ Watch App;
- Garmin Mobile SDK communication.

The Garmin application should remain small.

---

# 5. Repository

Create a GitHub monorepo:

    project-root/
    |
    ├── .claude/
    │   ├── skills/
    │   └── settings.json
    |
    ├── android/
    │   ├── companion-app/
    │   └── CLAUDE.md
    |
    ├── garmin/
    │   ├── fr255-map-app/
    │   └── CLAUDE.md
    |
    ├── backend/
    │   └── CLAUDE.md
    |
    ├── shared/
    │   └── protocol/
    |
    ├── tools/
    │   └── map-data/
    |
    ├── docs/
    │   ├── architecture.md
    │   ├── protocol.md
    │   ├── sync-protocol.md
    │   ├── map-data.md
    │   ├── activity-model.md
    │   ├── aws.md
    │   ├── development.md
    │   ├── STATUS.md
    │   └── decisions/
    │       └── 0001-pmtiles-over-mapsforge.md
    |
    ├── README.md
    ├── CLAUDE.md
    ├── LICENSE
    └── .gitignore

`backend/` and its `CLAUDE.md` are created when Track B starts (§66), not at Phase 0 -
there is no backend yet. See §74 for what belongs in each `CLAUDE.md`, `.claude/skills/`,
`docs/decisions/`, and `docs/STATUS.md`.

Exact Garmin/Gradle structure may be adjusted to current SDK requirements.

---

# 6. GitHub initialization

If GitHub CLI is authenticated:

1. create the repository;
2. initialize git;
3. add remote;
4. create initial commit;
5. push.

If GitHub authentication is unavailable:

- create the repository locally;
- prepare everything;
- tell the user exactly what remains to be done.

Never invent credentials.

Never commit secrets.

---

# 7. AWS strategy

AWS is part of the long-term architecture.

However:

## AWS is NOT required for MVP runtime.

The MVP must work completely offline after map data has been manually placed/downloaded onto the Android device.

Future architecture:

    AWS S3
       |
       | HTTPS
       v
    Android Map Manager
       |
       v
    local PMTiles
       |
       v
    offline Map Engine

Do not add AWS networking to the MVP unless it is useful for development.

The purpose of using AWS is also educational: the project should eventually teach the developer how to use AWS for static data distribution.

---

# 8. AWS future map storage

Use Amazon S3 as the initial map storage service.

Do NOT create a database for maps.

Do NOT create a backend API initially.

The S3 bucket can contain:

    maps/
        manifest.json

        poland/
            poland.pmtiles

        italy/
            italy.pmtiles

        regions/
            wielkopolskie.pmtiles

Example manifest:

    {
      "version": 1,
      "maps": [
        {
          "id": "poland",
          "name": "Poland",
          "file": "maps/poland/poland.pmtiles",
          "sizeBytes": 1900000000,
          "version": "2026.08",
          "minZoom": 5,
          "maxZoom": 15
        }
      ]
    }

The exact schema can evolve.

---

# 9. AWS phases

## Phase A — MVP

No AWS runtime dependency.

Map files are local development assets.

Example:

    local-maps/
        test.pmtiles

## Phase B — AWS distribution

Create:

    S3 bucket
       |
       +-- manifest.json
       +-- poland.pmtiles
       +-- italy.pmtiles

Android:

    GET manifest
       ↓
    show available maps
       ↓
    download PMTiles
       ↓
    store locally

## Phase C — Production-style distribution

Potentially add:

- CloudFront;
- HTTPS custom domain;
- cache headers;
- versioning;
- checksum validation;
- resumable downloads;
- download progress;
- automatic update detection.

Do not implement these until the basic S3 solution works.

---

# 10. Why S3 rather than a custom server

Prefer S3 because map files are static.

The application does not initially need:

- user accounts;
- authentication;
- map-generation requests;
- server-side routing;
- databases;
- server-side map rendering.

S3 is enough.

Later, if the project needs dynamic functionality, add:

- Lambda;
- API Gateway;
- DynamoDB;
- CloudFront;

only when there is a real requirement.

Do not create cloud infrastructure merely for the sake of creating it.

---

# 11. Offline map format

Use **PMTiles as the MVP map archive format**.

Do not make Mapsforge the primary architecture.

PMTiles is preferred because:

- it packages a complete tile pyramid;
- it naturally supports zoom levels;
- it can be read from local files;
- it can later be served through HTTP range requests;
- it works well with object storage;
- it allows a simple "download one file" UX;
- the same map artifact can support future online access.

Use PMTiles v3 or the current supported version.

Abstract the implementation behind:

    MapDataSource

so the underlying format can theoretically be replaced later.

---

# 12. Map source

Use OpenStreetMap-derived data.

Initial source:

- Geofabrik regional/country `.osm.pbf` extracts.

Do NOT bulk-download tiles from the standard OpenStreetMap tile server.

The project must comply with OSM licensing and attribution requirements.

The project is private/non-commercial, but this does not remove OSM licensing obligations.

Maintain attribution in the Android application and documentation.

---

# 13. Map preprocessing

Do not put a huge `.osm.pbf` directly into the Android app.

Use a PC/development machine preprocessing pipeline:

    OSM PBF
       |
       v
    preprocessing
       |
       v
    vector tiles
       |
       v
    PMTiles archive

The resulting PMTiles file is the distribution artifact.

Example:

    Poland.osm.pbf
          |
          v
    map generation tools
          |
          v
    Poland.pmtiles

---

# 14. Map contents

Initial map should support:

### Required

- roads;
- footways;
- paths;
- trails;
- rivers;
- lakes;
- forests/woodland;
- parks;
- railways where practical.

### Later

- buildings;
- farmland;
- cycleways;
- POIs;
- contour lines;
- terrain;
- hiking route relations;
- place names;
- amenities.

Do not attempt to support every OSM feature.

Prioritize walking/navigation usefulness.

---

# 15. Map styling

Map data and map style are separate.

OSM-derived data describes features.

The Android renderer decides how to display them.

Example:

    natural=water
        -> water fill

    natural=wood
        -> forest fill

    highway=primary
        -> major road

    highway=secondary
        -> secondary road

    highway=path
        -> trail

The Android map can have a richer style.

The Garmin map can have a simplified style.

---

# 16. Android map architecture

Create:

    MapRepository
        |
        v
    MapDataSource
        |
        v
    PMTilesReader
        |
        v
    Vector tile features
        |
        v
    MapEngine
        |
        +---- Android rendering
        |
        +---- Garmin rendering model

The map implementation must not force Garmin to understand PMTiles.

---

# 17. PMTiles local storage

Installed maps should live in application-managed storage.

Example:

    /maps/
        /poland/
            map.pmtiles
            metadata.json

        /italy/
            map.pmtiles
            metadata.json

The user can:

- install a map;
- delete a map;
- see map size;
- see map version;
- later update a map.

The Android application must never assume that Poland is always installed.

---

# 18. Map manager UI

Future phone UI:

    Offline Maps

    Poland
    1.9 GB
    [Download]

    Italy
    2.1 GB
    [Download]

    Wielkopolskie
    180 MB
    [Download]

Installed:

    ✓ Poland
      [Delete]

For MVP, this UI can be extremely simple.

---

# 19. MVP map installation

Initially avoid building AWS into the application.

Support a developer/test mechanism such as:

- bundled small PMTiles file;
- copied local PMTiles file;
- Android development storage.

Then implement S3 download later.

The first map should be a small development area around Poznań.

Do not start with Poland or Italy during development.

---

# 20. Zoom architecture

Zoom is required for MVP.

Use discrete zoom levels.

Initial Garmin controls:

    UP
        zoom in

    DOWN
        zoom out

Android owns canonical zoom state.

Example:

    zoom = 14

Garmin:

    ZOOM_IN

Android:

    zoom = 15

Then Android retrieves the appropriate vector tiles from local PMTiles.

---

# 21. Viewport

Initial behavior:

    map follows current GPS location.

The viewport contains:

    center latitude
    center longitude
    zoom

Later support:

- manual pan;
- follow mode;
- recenter.

Do not implement manual pan initially.

---

# 22. Garmin rendering pipeline

Android:

    PMTiles
       |
       v
    vector features
       |
       v
    viewport clipping
       |
       v
    geometry simplification
       |
       v
    Garmin render model
       |
       v
    BLE
       |
       v
    Garmin renderer

Garmin receives only what is needed for its current screen.

---

# 23. Garmin render primitives

Initial protocol should support:

    DRAW_POLYLINE
    DRAW_POLYGON
    DRAW_POINT

with simple styles:

    WATER
    FOREST
    ROAD_MAJOR
    ROAD_MINOR
    TRAIL
    TRACK
    CURRENT_POSITION

Do not implement a generic CSS-like styling engine.

Keep it simple.

---

# 24. Garmin viewport size

Target the 260x260 display.

The Garmin renderer must:

- clip geometry;
- simplify geometry;
- avoid excessive drawing;
- avoid large allocations;
- avoid full-map storage.

The Garmin must remain responsive.

---

# 25. Activity lifecycle

Before activity:

    map visible
    activity track hidden

Start from Garmin:

    START
       |
       v
    Android creates Activity
       |
       v
    ACTIVE

During activity:

    map
    current location
    activity track
    timer

Stop:

    STOP
       |
       v
    Android finalizes activity
       |
       v
    statistics
       |
       v
    COMPLETED

---

# 26. Activity data

Android stores:

    Activity
    ---------------------
    id
    startTime
    endTime
    elapsedTime
    movingTime
    distanceMeters
    elevationGainMeters
    elevationLossMeters
    averagePaceSecondsPerKm
    movingPaceSecondsPerKm
    averageSpeed
    maxSpeed
    status

    ActivityPoint
    ---------------------
    id
    activityId
    timestamp
    latitude
    longitude
    altitude
    accuracy
    speed
    bearing

At minimum store:

- timestamp;
- latitude;
- longitude;
- altitude.

Keep the data model extensible.

---

# 27. Raw track vs display track

Maintain two conceptual representations.

## Raw activity track

Contains all useful GPS points.

Used for:

- statistics;
- activity history;
- future GPX;
- future FIT;
- analysis.

## Garmin display track

Contains simplified/clipped geometry.

Used only for:

- Garmin display;
- efficient communication.

Never throw away raw activity data merely to optimize Garmin rendering.

---

# 28. GPS source

The Garmin is the primary GPS source.

The Garmin sends GPS updates to Android.

Android stores them.

Do not continuously use Android GPS for the main tracking pipeline.

Potential future fallback to phone GPS may be considered, but not MVP.

---

# 29. Activity timer

Simple timer.

On activity start:

    startTime = current time

Elapsed:

    now - startTime

Display:

    HH:MM:SS

Android is authoritative.

Garmin only displays the value.

---

# 30. Statistics

Calculate on Android.

MVP:

- distance;
- elapsed time;
- moving time;
- average pace;
- moving pace;
- average speed;
- max speed;
- elevation gain;
- elevation loss.

Use geographic distance calculations.

Do not treat latitude/longitude as Cartesian coordinates.

---

# 31. Elevation

Initial MVP:

Use Garmin-provided altitude samples.

Calculate:

    ascent
    descent

Apply a configurable noise threshold.

Future:

Integrate DEM/elevation data.

Potential future architecture:

    GPS
      +
    DEM
      ↓
    improved elevation profile

Do not make DEM a dependency for MVP.

---

# 32. Moving time

Initial simple algorithm:

    moving if speed > configurable threshold

Then:

    movingTime
    movingDistance

and:

    movingPace =
        movingTime / movingDistance

This does not need to reproduce Garmin/Strava algorithms.

---

# 33. Communication protocol

Create a versioned protocol.

Support:

    HELLO
    HELLO_ACK

    GPS_UPDATE

    MAP_REQUEST
    MAP_UPDATE

    ZOOM_IN
    ZOOM_OUT

    ACTIVITY_START
    ACTIVITY_STARTED

    ACTIVITY_PAUSE
    ACTIVITY_RESUME
    ACTIVITY_STOP
    ACTIVITY_SAVED

    TRACK_APPEND

    ACTIVITY_STATE

    ERROR

Important messages should have:

    messageId
    protocolVersion
    sequenceNumber where useful

---

# 34. Reliability

Bluetooth communication can temporarily fail.

The activity must not become corrupted because of a map-rendering failure.

Use GPS sequence numbers.

Example:

    GPS #1001
    GPS #1002
    GPS #1003

Android acknowledges or records the latest received sequence.

Design for synchronization after reconnect.

---

# 35. Activity priority

If resources are constrained:

    activity tracking
        >
    map rendering

Never let a slow map operation cause loss of activity data.

---

# 36. Android background execution

Activity tracking must continue when:

- screen is off;
- Android application is backgrounded;
- user switches apps.

Use an appropriate Android foreground service.

Separate:

    ActivityTrackingService

from:

    Android UI

from:

    GarminConnectionManager

from:

    MapEngine

---

# 37. Garmin state machine

Implement explicitly:

    DISCONNECTED
        |
        v
    CONNECTED
        |
        v
    MAP_MODE
        |
       START
        |
        v
    STARTING
        |
        v
    ACTIVE
       / \
    PAUSE STOP
      |     |
      v     v
    PAUSED SAVING
      |     |
    RESUME  v
      |  COMPLETED
      v

Keep it simple.

---

# 38. Android activity state machine

    IDLE
      |
      v
    STARTING
      |
      v
    ACTIVE
      |
      +----> PAUSED
      |          |
      |          v
      |        ACTIVE
      |
      v
    STOPPING
      |
      v
    COMPLETED

Persist activity state where appropriate.

---

# 39. MVP phases

## Phase 0 — Tooling, repository, and agent-friendly setup

Inspect:

- JDK;
- Android SDK;
- Gradle;
- Garmin Connect IQ SDK;
- Monkey C;
- Garmin simulator;
- Git;
- GitHub CLI;
- AWS CLI if installed.

Create:

- GitHub repo;
- monorepo per §5, including `.claude/`, root `CLAUDE.md`, `android/CLAUDE.md`,
  `garmin/CLAUDE.md`, `docs/STATUS.md`, `docs/decisions/`;
- at least one repo-specific skill in `.claude/skills/` (§74) — start with whichever build
  is most annoying to type by hand, likely `run-android` or `run-garmin-sim`;
- documentation;
- Android project;
- Garmin project.

Both projects must build.

Acceptance for the agent-friendly part specifically: open a brand-new Claude Code session
in this repo with no other context, and confirm it can answer "what phase are we on and
what's next" from `docs/STATUS.md` alone, and "how do I build each part" from the
`CLAUDE.md` files alone, without exploring the whole tree first.

---

## Phase 1 — Garmin ↔ Android

Implement:

    HELLO
    HELLO_ACK
    GPS_UPDATE

First physical-hardware goal:

Garmin:

    GPS:
    52.xxxxx
    16.xxxxx

Android receives and displays it.

Do not implement maps yet.

---

## Phase 2 — Activity

Implement:

- Start;
- Stop;
- Activity ID;
- activity database;
- timer.

First vertical slice:

    Start on watch
       ↓
    Activity #1
       ↓
    GPS points
       ↓
    database
       ↓
    Stop
       ↓
    summary

---

## Phase 3 — Track

Implement:

- raw GPS points;
- track processing;
- track visualization on Android;
- simplified track on Garmin.

Acceptance:

Walk approximately 100–500 meters.

Garmin displays the walked path.

Android displays the same path.

---

## Phase 4 — PMTiles map

Create a tiny OSM-derived PMTiles dataset.

Suggested development area:

    Poznań

Implement:

- local PMTiles storage;
- PMTiles reader;
- vector tile extraction;
- map feature model;
- basic Android map rendering;
- Garmin rendering model.

Acceptance:

No internet connection.

Map remains available.

---

## Phase 5 — Zoom

Implement:

    UP -> zoom in
    DOWN -> zoom out

Acceptance:

Map changes detail level correctly.

Higher zoom should expose more detailed map geometry.

---

## Phase 6 — Full map + activity combination

Combine:

    offline map
    +
    current GPS
    +
    activity track
    +
    timer

Acceptance:

Walk outdoors with internet disabled.

The watch displays:

- forest;
- roads;
- trails;
- rivers/lakes;
- current position;
- walked track.

---

## Phase 7 — Statistics

Implement:

- distance;
- ascent;
- descent;
- average pace;
- moving pace;
- moving time;
- average speed;
- maximum speed.

---

## Phase 8 — Activity history

Android:

    Activities
        ↓
    Activity details
        ↓
    map + track + statistics

Persist across application restarts.

---

# 40. AWS Phase 2

Once local maps work:

## S3 setup

Create an S3 bucket.

Store:

    maps/
        manifest.json
        poland.pmtiles
        italy.pmtiles
        ...

Use appropriate bucket security.

Prefer public-read distribution only if appropriate; otherwise investigate controlled access.

For this private project, simplicity is preferred.

Do not put AWS credentials inside the Android app.

If the map files are public, Android needs no AWS credentials.

---

# 41. AWS manifest

Example:

    {
      "maps": [
        {
          "id": "poland",
          "name": "Poland",
          "version": "2026.08",
          "sizeBytes": 1900000000,
          "downloadUrl": "...",
          "minZoom": 5,
          "maxZoom": 15
        }
      ]
    }

Android downloads the manifest.

Then:

    user selects Poland
       ↓
    Android downloads PMTiles
       ↓
    checksum validation
       ↓
    atomic move into installed-maps directory
       ↓
    map available offline

---

# 42. AWS learning objectives

The AWS implementation should intentionally teach:

- S3 buckets;
- object storage;
- IAM basics;
- bucket policies;
- AWS CLI;
- object uploads;
- versioning;
- lifecycle policies;
- CloudFront later;
- monitoring/cost awareness.

Do not create unnecessary AWS services.

---

# 43. Future AWS architecture

Potential final system:

    Android
       |
       +---- maps manifest
       |
       +---- map download
       |
       v
    CloudFront
       |
       v
    S3
       |
       +---- PMTiles

Potentially later:

    Android
       |
       v
    API Gateway
       |
       v
    Lambda
       |
       v
    DynamoDB

But only add the API/backend when the application actually needs server-side data.

---

# 44. Future online map mode

Not MVP.

Because PMTiles supports tiled access, later investigate:

    online map mode

where Android does not need the entire PMTiles archive.

Conceptually:

    Android
       |
       | HTTP range requests
       v
    S3 / CloudFront
       |
       v
    PMTiles

The application would request only the required portions.

Offline mode remains:

    Android
       |
       v
    local PMTiles

The map engine should abstract these two sources.

---

# 45. Map source abstraction

Implement:

    MapDataSource

Potential implementations:

    LocalPMTilesDataSource
    RemotePMTilesDataSource   // future

The map engine should not care whether data came from:

- local file;
- HTTP;
- another future source.

For MVP implement only:

    LocalPMTilesDataSource

---

# 46. Map download abstraction

Future:

    MapDownloadManager

Responsibilities:

- discover maps;
- download;
- resume;
- progress;
- verify checksum;
- install atomically;
- delete;
- update.

MVP may only need local map installation.

---

# 47. Map package metadata

Each map should eventually have:

    id
    name
    version
    fileSize
    checksum
    minZoom
    maxZoom
    boundingBox
    attribution
    generatedAt

This will support the future download manager.

---

# 48. Licensing and attribution

Every OSM-derived map distribution must comply with OSM licensing.

Document:

- OpenStreetMap attribution;
- ODbL requirements;
- source data;
- map-generation process;
- any additional licenses used by map tooling/data.

If a third-party map source is used in the future, verify its offline redistribution license before using it.

Private/non-commercial use does not automatically remove attribution/licensing requirements.

---

# 49. Future contour/elevation maps

Potential later feature:

    DEM data
       |
       v
    elevation tiles
       |
       v
    Android
       |
       +---- terrain visualization
       +---- elevation profile
       +---- improved activity statistics

Keep this independent of the base OSM map.

---

# 50. Future activity features

After MVP:

- pause/resume;
- auto-pause;
- laps;
- GPX export;
- FIT export;
- route import;
- route following;
- off-route detection;
- waypoints;
- heart rate;
- cadence;
- temperature;
- calories;
- elevation profile.

---

# 51. Future Garmin features

Potential:

- manual map panning;
- recenter;
- route navigation;
- waypoint markers;
- distance to waypoint;
- heading indicator;
- turn instructions;
- compass mode;
- breadcrumb mode;
- configurable map layers.

Do not implement before the basic map works.

---

# 52. Future Android features

Potential:

- map download manager;
- map region selector;
- activity calendar;
- weekly/monthly statistics;
- activity search;
- GPX import;
- GPX export;
- FIT export;
- route planner;
- route library.

---

# 53. Privacy

Default:

    GPS data
        |
        v
    local Android storage

No automatic upload.

No account.

No cloud activity storage.

AWS is initially only for static map distribution.

Do not upload activity data to AWS unless explicitly added as a future feature.

---

# 54. Testing

Android tests:

- geographic distance;
- elevation;
- pace;
- moving time;
- activity lifecycle;
- protocol;
- PMTiles map queries;
- viewport;
- geometry simplification.

Use GPS fixtures:

    test-data/
        short-walk.json
        elevation-walk.json
        stopped-walk.json
        noisy-gps.json

Test GPS noise and impossible jumps.

---

# 55. Performance

Priority:

1. activity reliability;
2. communication reliability;
3. battery efficiency;
4. responsiveness;
5. map rendering quality.

Never let map rendering block activity recording.

Never transmit the entire map to Garmin.

Never transmit the complete activity track on every GPS update.

---

# 56. MVP definition

MVP is complete when:

- [ ] Android builds.
- [ ] Garmin app builds.
- [ ] FR255 app installs.
- [ ] Garmin communicates with Android.
- [ ] Garmin GPS reaches Android.
- [ ] Start activity works from watch.
- [ ] Android creates activity.
- [ ] GPS points persist.
- [ ] latitude persists.
- [ ] longitude persists.
- [ ] altitude persists.
- [ ] timer works.
- [ ] track appears on Garmin.
- [ ] track appears on Android.
- [ ] Stop activity works.
- [ ] statistics are calculated.
- [ ] small PMTiles map works offline.
- [ ] map shows roads.
- [ ] map shows paths/trails.
- [ ] map shows water.
- [ ] map shows forests/woodland.
- [ ] map follows GPS.
- [ ] UP zooms in.
- [ ] DOWN zooms out.
- [ ] previous activities persist.
- [ ] OSM attribution is present.
- [ ] no internet is required during an activity.

AWS is NOT required for MVP completion.

---

# 57. First complete demonstration

The first end-to-end demonstration should be:

1. Install a small Poznań PMTiles file on Android.
2. Disable phone internet.
3. Connect FR255.
4. Open map on Garmin.
5. Garmin receives GPS.
6. Garmin shows offline map.
7. Press Start.
8. Android creates Activity #1.
9. Walk approximately 500 meters.
10. Garmin displays:
    - roads;
    - trails;
    - forest/water where applicable;
    - current position;
    - activity track;
    - timer.
11. Press Stop.
12. Android calculates:
    - distance;
    - elapsed time;
    - moving time;
    - pace;
    - moving pace;
    - ascent/descent.
13. Activity is stored locally.
14. Reopen the Android app.
15. Activity #1 is still present.
16. Open it and see:
    - map;
    - track;
    - statistics.

This is the first real product milestone.

---

# 58. Second major demonstration — AWS

After MVP:

1. Create AWS S3 bucket.
2. Upload PMTiles.
3. Upload manifest.
4. Android discovers available maps.
5. User presses Download.
6. Android downloads PMTiles.
7. User disables internet.
8. Map continues working.
9. User deletes the map.
10. Map is no longer available.
11. User can download it again.

Only after this works should CloudFront or more sophisticated AWS infrastructure be considered.

---

# 59. Claude Code execution rules

You are the lead implementation agent.

Start by inspecting the local environment.

Check:

- Java/JDK;
- Android SDK;
- Gradle;
- Garmin Connect IQ SDK;
- Monkey C;
- Garmin simulator;
- Git;
- GitHub CLI;
- AWS CLI.

Do not assume installation paths.

If a required tool is missing:

- document it;
- provide installation instructions;
- continue with tasks that do not depend on it.

Do not start map implementation before the Garmin ↔ Android communication and activity vertical slice works.

Do not create AWS infrastructure before the local PMTiles MVP works.

Do not introduce cloud activity storage.

Do not introduce user authentication.

Do not build a custom backend unless a later requirement justifies it.

Keep commits small and meaningful.

Keep the repository buildable.

Update documentation as architectural decisions are made.

---

# 60. Development order

The required order is:

    Repository
       ↓
    Agent-friendly setup (CLAUDE.md, skills, docs/STATUS.md, docs/decisions/ — §74)
       ↓
    Toolchain verification
       ↓
    Android + Garmin skeleton
       ↓
    Garmin ↔ Android communication
       ↓
    GPS
       ↓
    Activity lifecycle
       ↓
    Activity database
       ↓
    Track
       ↓
    Local PMTiles
       ↓
    Map rendering
       ↓
    Garmin map renderer
       ↓
    Zoom
       ↓
    Statistics
       ↓
    Activity history
       ↓
    AWS S3 map distribution
       ↓
    Future online PMTiles mode

Do not skip ahead.

---

# 61. Product philosophy

The final application should feel like:

    Garmin 255
       +
    Android phone
       +
    offline map
       +
    lightweight activity tracker

The Garmin should feel surprisingly capable despite its limitations.

But the implementation should remain intentionally simple.

The phone is the brain.

The watch is the GPS/display/input device.

PMTiles is the offline map archive.

AWS S3 is the future map distribution system.

Activities remain local unless cloud synchronization is deliberately added later.

The first objective is not to build Strava.

The first objective is to make the Garmin 255 display a useful offline map and record a walk reliably.

Everything else builds on that foundation.
---

# PART II - Learning Extensions (post-MVP)

Everything below is **deliberately out of scope until the §56 MVP is complete**. It does
not change the development order in §60, and it does not change the MVP definition.

These sections exist for a different reason from the rest of this document. The MVP is
driven by what the application needs. Part II is driven by what the developer wants to
learn: cloud (AWS), Kubernetes, backend scalability, and observability. That motivation is
stated openly here so a future reader does not mistake it for a product requirement.

The privacy default in §53 still holds: activity data stays local unless the user
explicitly opts in.

---

# 62. Cloud sync backend - purpose and honest framing

**Product justification:** device migration. When the user changes phone, activity history
and settings survive. This is a real feature of every activity tracker.

**Actual motivation:** it creates a backend, and a backend is what teaches cloud. The
offline-first application on its own touches almost no cloud surface - S3 static hosting
and nothing else. A sync API introduces compute, a managed database, secrets, identity,
logging, deployment, and load. That is the learning target.

**Design consequence:** the application must remain fully functional with sync disabled.
Sync is additive, never load-bearing. If the backend is unreachable, activities record
exactly as before. This constraint is not a compromise for the sake of privacy - it is
also what makes the sync problem interesting, because it forces offline-first reconciliation
rather than a simple client-server CRUD app.

**Non-goals:** no social features, no public activity feeds, no leaderboards, no sharing.
Single-user data, synced across that user's own devices.

---

# 63. Sync scope

Synced:

- user account;
- activity summaries (statistics, timestamps, status);
- activity tracks (GPS point series);
- installed-map preferences (which maps, not the map files themselves).

Never synced:

- PMTiles archives (they come from S3 separately, per §40);
- raw sensor data beyond what §26 defines.

Explicit user controls:

- sync off by default;
- enable/disable sync;
- delete all cloud data (must actually delete, not soft-hide);
- delete account.

Implementing real deletion is deliberately included. Data deletion paths are a genuine
backend skill and are usually skipped in learning projects.

---

# 64. Authentication

Use **AWS Cognito** rather than hand-rolled authentication.

Reasons: rolling your own identity is a bad idea in any real system, and Cognito is the
component that teaches AWS identity concepts - user pools, JWT issuance, token refresh,
and how an API validates a token it did not issue.

What this should teach:

- user pool vs identity pool;
- JWT structure, expiry, refresh flow;
- validating tokens at the API boundary;
- how a mobile client stores tokens safely (Android Keystore / EncryptedSharedPreferences);
- what happens on token expiry mid-sync.

Explicitly out of scope: social login, MFA, custom auth flows. One email/password pool is
enough.

---

# 65. Sync protocol design

This is the part with real engineering content. The client is offline-first, so this is
reconciliation between two independently-mutating stores, not CRUD.

**Client-generated identity.** Activity ids are UUIDs created on the phone, not
server-assigned. The server never allocates ids. This makes uploads naturally idempotent
and lets an activity exist before it has ever been online.

**Idempotent upload.** Re-uploading the same activity must be a no-op, not a duplicate.
Combine a client-generated id with an idempotency key per request.

**Batched point upload.** A 2-hour walk at 1Hz is roughly 7,200 points. Do not send one
request per point, and do not send one enormous request either. Batch with a sequence
number, support resume from the last acknowledged batch, and make partial upload a valid
state - an activity may be summary-complete and track-incomplete.

**Incremental sync via cursor.** The client asks "what changed since cursor X" rather than
fetching everything. Teaches pagination, opaque cursors, and monotonic change tracking.

**Conflict policy, stated explicitly.** Activities are append-only and effectively
immutable once completed, so genuine conflicts are rare - which is a good place to learn.
Mutable fields (name, notes, deletion) need a rule: last-write-wins with a version field
is sufficient, provided the rule is written down and tested.

**Tombstones.** Deleting on one device must propagate. A deleted activity cannot simply
vanish from the server, or the other device will re-upload it.

**Compression.** Track payloads compress extremely well. Measure it, then decide.

Write the resulting design into `docs/sync-protocol.md`, versioned like §33.

---

# 66. Backend implementation and deployment path

Deliberately staged, so each step teaches one thing:

**Backend technology.** **Kotlin + Spring Boot** (Spring Web MVC to start; Spring WebFlux
is a later, optional swap once the concurrency model in §73 is understood, not a day-one
choice). Kotlin because it is modern and stable on the JVM, and because coroutines give a
second, deliberately different concurrency model to compare against Android's use of them
(§73) - the same language, two very different runtime pressures. Postgres via Spring Data
JDBC or plain JDBC/jOOQ; avoid full JPA/Hibernate magic for this project so query behaviour
under load (§68) stays visible rather than hidden behind an ORM.

**Versions: pin to the current LTS/stable release at setup time, not to a number written
here.** This document is a living plan, not a lockfile - by the time Track B actually
starts, whatever is "newest LTS" today may have moved on. At Phase 0/B1 kickoff, check and
record the actual versions chosen in `docs/decisions/` (§74):

- **JDK**: the current Java LTS release (as of this writing, Java 21 or 25 - confirm
  whichever is newest at setup time; the local machine currently has JDK 17, so this needs
  an explicit upgrade before Track B, not an assumption that 17 is fine).
- **Kotlin**: the current stable Kotlin release (2.x line as of this writing) - confirm the
  latest stable version, not a pinned old one, when the Gradle build is first created.
- **Spring Boot**: whatever current stable Spring Boot release supports the chosen Kotlin
  and JDK versions - check compatibility explicitly rather than assuming the newest of each
  aligns.

**Stage A - local, docker compose.** API plus Postgres. No cloud, no Kubernetes. Get the
sync protocol correct where iteration is fastest.

**Stage B - local Kubernetes (k3s or kind).** Same containers, now as manifests. Learn:
Deployment, Service, Ingress, ConfigMap, Secret, liveness/readiness probes, resource
limits, `kubectl logs/describe/exec`, rollout and rollback, and debugging a pod that will
not start (image pull, crash loop, OOM kill, failing probe).

Starting with compose and *then* moving to Kubernetes is intentional: it makes visible
what Kubernetes adds and what it costs.

**Stage C - AWS.** Deploy to **ECS Fargate** with **RDS Postgres**, secrets in **Secrets
Manager**, logs in **CloudWatch**, image in **ECR**, and a task IAM role.

Do **not** use EKS. The control plane costs roughly $72/month whether idle or not, and
Kubernetes mechanics are already covered free in Stage B. Local k3s for Kubernetes, ECS
Fargate for cloud.

Budget alarm before creating anything. Teardown script from the start.

---

# 67. The six questions

Stage C is complete when these can be answered from memory, about this system:

1. Where does my service run, and how does my code physically get there?
2. Where do my secrets come from at runtime?
3. How do I read my service's logs in production?
4. My service needs to write to a bucket - what has to be true for that to work?
5. My service cannot reach the database - what do I check, in what order?
6. What in my design costs money as traffic grows?

These are the bar for listing AWS honestly on a CV.

---

# 68. Scalability work

A single-user application has no load, so load must be synthesised. Use **k6** with
simulated devices.

**Baseline.** One task. Find the request rate at which p95 exceeds 500ms, and identify
what breaks first.

**Horizontal scaling.** Multiple tasks behind a load balancer. Throughput will not scale
linearly - expect to hit the Postgres connection limit. Learn connection pooling, and why
more application instances can make a database slower.

**Write contention.** Many devices uploading track batches concurrently. Watch for hot
rows, lock contention, and index write amplification on the points table.

**Read path.** Activity history listing with pagination over a large dataset. Then add
pre-computed summaries so listing never touches the points table - the same rollup pattern
as the analytics work at Uplink, in SQL rather than MongoDB.

**Caching (optional extension, if the read path is worth it).** ElastiCache Redis in front
of the activity-history/cursor-sync read path. Measure p95 with and without cache, and the
hit ratio, then decide the invalidation strategy explicitly and state what staleness is
being accepted. Adapted from `learning/cloud-project-prompt.md` stage 3 - worth doing here
only if §65's read path is actually the bottleneck found in the baseline; do not add a
cache pre-emptively.

**Queue-based load levelling (optional extension).** SQS (or a Postgres-backed job table if
staying free-tier) between batched-point ingest and the database writer. Measure the effect
on write throughput and on upload latency; explain backpressure, at-least-once delivery, and
why the writer needs idempotency it likely already has from §65. Adapted from
`learning/cloud-project-prompt.md` stage 4.

**Autoscaling (optional extension).** Target tracking on request count or CPU for the ECS
service. Measure how long scale-out actually takes and what happens to error rates during
the lag - this is the part nobody sees until production. Adapted from
`learning/cloud-project-prompt.md` stage 6.

**Deliberate breakage.** Kill a task mid-upload. Exhaust the connection pool. Set a memory
limit too low. Add artificial database latency. For each: what did the client see, where
did you look, how did you diagnose it. The mobile client must survive all of it, since sync
is non-load-bearing by design.

Record every result in `docs/FINDINGS.md`:

    ## <stage>
    Setup:        <what changed>
    Load applied: <k6 scenario, RPS, duration>
    Result:       <p50 / p95 / error rate / throughput>
    Bottleneck:   <what broke first, and how it was found>
    Fix:          <what changed, and the number afterwards>
    Cost:         <monthly cost if left running>

That file is the actual output. Specific bottlenecks hit personally, with measured numbers,
are worth more than any amount of reading.

---

# 69. Observability

Instrument the backend with **OpenTelemetry**, and trace a sync request end to end: mobile
client to API to database.

Target understanding:

- what a trace, span, and trace context propagation actually are;
- how a trace id crosses the client/server boundary;
- reading p95/p99 per endpoint rather than guessing;
- finding which span dominates a slow request.

Any OTLP-compatible backend works. Free-tier Grafana Cloud or a local Jaeger is enough; no
paid APM required.

Practical exercise: make one endpoint slow on purpose, then find it from the traces alone
without reading the code.

---

# 70. Map pipeline as a cloud job

Independent of sync, and genuinely justified rather than contrived: converting a Poland
`.osm.pbf` to PMTiles is slow and memory-hungry on a laptop.

Run the §13 preprocessing pipeline as an **AWS Batch or Fargate task**: read the OSM
extract from S3, write the PMTiles artifact and manifest back to S3.

Teaches long-running containerised jobs, task IAM roles, S3 as pipeline storage, and the
cost of compute-by-the-minute - with a real reason to exist, which the rest of `tools/`
already implies.

---

# 71. Master order of work (supersedes the Part II sketch)

## 71.1 The problem with gating everything behind the MVP

An earlier version of this section put all of Part II after the §56 MVP. That was wrong.

The §56 MVP is large: two platforms, a protocol, a PMTiles map engine, geometry
simplification, zoom, statistics, and history. Realistically months of evenings, with the
map engine (Phase 4) by far the hardest part. Gating every cloud, Kubernetes, CI/CD,
observability and scalability lesson behind it means that if the project stalls on PMTiles -
the most likely place to stall - none of the learning that actually matters happens.

**The sync backend does not depend on the map engine at all.** It depends only on activities
existing in a database, which is true at the end of Part I Phase 2. So the two run in
parallel from that point.

## 71.2 Two tracks

**Track A - the device application.** Part I §60, unchanged. Phases 0 through 8.

**Track B - backend, cloud, and delivery.** Starts once Track A Phase 2 is done (activity
lifecycle plus database). Never blocks Track A, and Track A never blocks it after that point.

    Track A: Phase 0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8
                             |
                             | activities exist in a database
                             v
    Track B:                 B1 -> B2 -> ... -> B10

## 71.3 Track B order

    B1. Backend skeleton, docker compose (API + Postgres)
        One real endpoint. Unit tests. No cloud, no Kubernetes.
        |
        v
    B2. CI: pull request pipeline (§72.4)
        Lint, unit tests, integration tests against a service-container Postgres.
        Branch protection on main. No deployment yet.
        |
        v
    B3. Sync protocol correct and tested (§65)
        Idempotent upload, batched points with resume, cursor sync, tombstones,
        written-down conflict policy. All local, all fast to iterate.
        |
        v
    B4. Observability, locally (§69)
        OpenTelemetry in the API, Jaeger in docker compose. Trace one sync request
        end to end. Cheap here, and it makes everything after it debuggable.
        |
        v
    B5. Local Kubernetes (§66 Stage B)
        Same containers as manifests on k3s/kind. Probes, ConfigMap, Secret,
        resource limits, rollout and rollback, and deliberately breaking pods.
        |
        v
    B6. First AWS step: map pipeline as a job (§70)
        Small, self-contained, genuinely justified. ECR, S3, a task IAM role.
        Trigger is need: do this when a Poland-sized map becomes painful locally
        (around Track A Phase 4), not on a fixed date.
        |
        v
    B7. AWS deployment of the sync API, done by hand (§66 Stage C)
        ECS Fargate, RDS, Secrets Manager, CloudWatch, task role.
        Answer the six questions (§67) before continuing.
        |
        v
    B8. CD: deploy on merge to main (§72.3)
        OIDC federation, SHA-tagged images, migrations, smoke test, rollback.
        Automating a deployment that is already understood by hand.
        |
        v
    B9. Load testing and scalability (§68)
        Synthetic devices with k6. FINDINGS.md. Needs B4 - load numbers are
        much harder to interpret without traces.
        |
        v
    B10. Cognito authentication (§64)
        Last on purpose. Least interesting technically, and everything above
        develops fine against a fixed test user id. Required before the
        device-migration demo is honest.

## 71.4 Why this order, and the reasoning worth keeping

**Docker compose before Kubernetes, and both after there is a real service.** A partially
correct instinct is "set up docker and Kubernetes first, then build the app." Compose does
belong early - at B1, alongside the very first endpoint, not after the API is finished.
Kubernetes does not. Writing manifests for a hello-world service teaches YAML syntax and
nothing else; probes, config, secrets, rollouts and pod debugging only become real lessons
when there is a service that has dependencies, state, and a way to fail. Hence B5, after
the protocol works.

**CI before CD, and both split.** The pull request pipeline (B2) comes early, as soon as
there are tests worth running - it is what keeps the protocol work honest during B3.
Deployment automation (B8) comes late, because it needs somewhere to deploy and a process
already understood manually.

**Observability before load testing.** B4 before B9, deliberately. Load-testing an
uninstrumented service produces numbers with no explanation. With traces, a bottleneck is
visible rather than inferred.

**One manual deployment before any automated one.** B7 before B8. A pipeline built around
a process never performed by hand is a black box that fails at the worst moment.

**Auth last.** B10. It gates nothing technically and is the least interesting part.

## 71.5 Two honest notes

**Local Kubernetes is off the delivery path.** The deployment target is ECS Fargate (§66),
so the k3s manifests from B5 never get deployed by the pipeline. That is deliberate: EKS
costs roughly $72/month for the control plane alone, and B5 already teaches everything a
backend interview probes. B5 is a learning exercise, not part of the delivery chain, and
that is a fine trade.

**Part I §60 already ends with AWS S3 map distribution**, which overlaps B6 and §40. Treat
§60's final two steps as the boundary between the tracks rather than as separate work.

## 71.6 If time runs short

Priority order if the project has to be cut, given the goal is closing real gaps:

1. B1-B3 (a working, tested sync API) - the backend and testing gap;
2. B7 (AWS by hand) plus §67 - the cloud gap, and the six questions;
3. B2 and B8 (CI then CD) - the CI/CD gap;
4. B5 (local Kubernetes) - the Kubernetes gap;
5. B4 and B9 (observability, load testing) - the reliability gap;
6. Track A Phases 4-8 (the map engine) - the part that makes the project *fun* but closes
   no gap on the CV.

That last line is uncomfortable but true. The map engine is the reason to build this; the
backend is the reason it helps a job search. Knowing which is which prevents
disappointment later.

# 72. CI/CD - GitHub Actions

GitHub Actions is the equivalent of Bitbucket Pipelines: YAML workflows in
`.github/workflows/`, triggered by repository events, running jobs in containers.

**Goal:** merge to `main` deploys the sync backend to production automatically. One
environment only for now. A `develop`/staging environment can be added later, but two
environments before one works is premature.

**Sequencing:** deploy manually once first (§66 Stage C), by hand, understanding every
step. Only then automate it. A pipeline that automates a process you do not understand is
a black box that breaks at the worst possible time.

---

## 72.1 Authentication - use OIDC, never stored keys

This is the most important decision in the whole pipeline.

Do **not** create an IAM user, generate an access key, and paste it into GitHub secrets.
That is a long-lived credential sitting in a third-party system with no expiry.

Instead use **GitHub's OIDC provider** with an IAM role:

    GitHub Actions job
        |
        | requests a short-lived OIDC token
        v
    AWS IAM (OIDC identity provider, trust policy)
        |
        | assumes role, returns temporary credentials
        v
    Job can now call AWS (15-60 minute credentials)

What this teaches, and it is genuinely valuable: IAM trust policies, federated identity,
`sts:AssumeRoleWithWebIdentity`, and scoping a trust policy to a specific repository and
branch so another repo cannot assume the role.

Scope the trust policy tightly:

    "token.actions.githubusercontent.com:sub":
        "repo:<owner>/<repo>:ref:refs/heads/main"

Then the role only needs the permissions the deploy actually requires: push to ECR,
register a task definition, update the ECS service, and pass the task execution role.

**Application secrets are separate.** The container reads database credentials from AWS
Secrets Manager at runtime (§66 Stage C). CI never sees them. GitHub secrets should hold
only non-sensitive configuration like the role ARN and region.

---

## 72.2 Monorepo path filtering

This repository holds Android, Garmin, shared protocol, tools, and the backend. A backend
deploy must not trigger on an Android-only change.

Use `paths:` filters on the workflow trigger, plus a change-detection job for anything more
complex. Learn the failure mode too: a shared-protocol change affects *both* backend and
clients, so `shared/` must trigger both.

---

## 72.3 Backend pipeline

Trigger: push to `main` affecting `backend/` or `shared/`.

    1. checkout
    2. lint + unit tests
    3. integration tests            <- real Postgres via service container
    4. build container image
    5. push to ECR, tagged with the git SHA
    6. run database migrations
    7. update ECS service to the new image
    8. wait for the deployment to stabilise
    9. smoke test the deployed endpoint
   10. roll back if the smoke test fails

Notes on the parts that actually matter:

**Integration tests in CI** (step 3). GitHub Actions service containers give you a real
Postgres for the job's lifetime - the same idea as Testcontainers. This is the step that
makes the pipeline worth having, and it is the habit currently missing from day-to-day work.

**Tag images with the git SHA, never `:latest`** (step 5). `latest` is unrollbackable and
ambiguous about what is actually running. A SHA tag means "roll back" is just redeploying
the previous tag.

**Migrations before deploy** (step 6), and this is the classic trap: during a rolling
deploy, old and new code run simultaneously against one database. So migrations must be
backward compatible - add columns, do not rename or drop them in the same release. Dropping
a column the old version still reads causes errors on the instances that have not yet been
replaced. Run migrations as a one-off ECS task rather than in the container entrypoint, so
concurrent tasks cannot race each other.

**Smoke test and rollback** (steps 9-10). A health endpoint plus one real read. Without
this, a successful *deploy* of a broken build reports green.

**Concurrency control.** Set `concurrency` on the workflow to cancel superseded runs, so
two quick merges cannot deploy out of order.

---

## 72.4 Pull request pipeline

Trigger: pull request targeting `main`.

Runs lint, unit tests, and integration tests. Never deploys. Enable branch protection on
`main` requiring these checks to pass - the pipeline is worthless if it can be bypassed.

---

## 72.5 Mobile and watch artifacts

Lower priority. The backend pipeline is the one with cloud learning in it.

**Android:** build a debug APK on every PR touching `android/`, upload it as a workflow
artifact. Signed release builds need a keystore in GitHub secrets (base64-encoded) - worth
doing once for the experience of handling signing material in CI, on a tag trigger rather
than every merge.

**Garmin/Monkey C:** the Connect IQ SDK in CI is awkward - check current licensing and
availability before attempting it. Treat as optional; local builds are acceptable.

---

## 72.6 Deliberately out of scope for now

- multiple environments (add `develop`/staging once prod works);
- blue/green or canary deploys;
- manual approval gates;
- automatic dependency updates;
- matrix builds across versions.

Each is a reasonable next step, and each is a distraction before one branch deploys
reliably to one environment.

---

## 72.7 Where this fits

See §71.3. The pull request pipeline (§72.4) is **B2**, early - it keeps the sync protocol
work honest. The deployment pipeline (§72.3) is **B8**, after the first manual AWS
deployment in B7, because automating an unfamiliar process produces a black box.

**CV consequence, stated plainly:** authoring a full GitHub Actions pipeline with OIDC
federation, integration tests, SHA-tagged images, migrations, and automated rollback is a
real CI/CD credential. Current experience is limited to using Bitbucket Pipelines that
someone else built. This closes that gap with something buildable in a weekend.

---

# 73. Concurrency - explicit learning objective

Concurrency work happens implicitly all over this project (coroutines on Android, Postgres
connection limits in §68, batched uploads in §65) but was never named as its own target. It
is one now, alongside cloud, Kubernetes, and scalability, because it is asked about directly
in senior backend interviews and is easy to gesture at without ever having measured it.

**Two concurrency models, deliberately compared, same language:**

- **Android (Kotlin coroutines, client side).** Structured concurrency for GPS ingestion,
  BLE communication with the Garmin, and the foreground tracking service (§36). The
  discipline here is cancellation correctness: a coroutine scope tied to the wrong lifecycle
  either leaks (keeps tracking after the service should have stopped) or dies early (drops
  GPS points mid-activity). Learn `SupervisorJob`, `CoroutineScope` lifecycle, and what
  happens to in-flight work when a scope is cancelled.
- **Backend (Kotlin + Spring Boot, server side, §66).** The same coroutine primitives under
  a completely different pressure: many concurrent requests sharing a small, finite resource
  (the Postgres connection pool, §68). This is where connection pool sizing, thread-per-
  request vs event-loop/coroutine dispatch, and backpressure stop being abstract.

**What to be able to answer afterwards, from having measured it, not read about it:**

1. What decides how many requests the API can actually run concurrently - the HTTP thread
   pool, the coroutine dispatcher, or the connection pool? (Answer it by shrinking each one
   independently under k6 load in §68 and watching which change moves p95.)
2. What happens to a request whose coroutine is cancelled mid-database-call - does the
   connection get returned to the pool, or leaked? Prove it, don't assume it.
3. Where does a lock or hot row actually show up under the write-contention test in §68, and
   how is that different from a coroutine merely waiting on a dispatcher?
4. On Android: what happens to an in-progress GPS write if `ActivityTrackingService` is
   killed by the OS mid-coroutine? Is the partial write safe (§27, raw track vs display
   track)?

**Do not** turn this into a separate stage in the Track B order (§71.3) - it is a lens
applied to B3 (sync protocol), B5 (Kubernetes resource limits, which cap concurrency
indirectly), and B9 (load testing), not new work on its own.

---

# 74. Agent-friendly project setup - explicit learning objective

Named as its own objective, alongside cloud, Kubernetes, scalability, and concurrency,
because "working well with an AI coding agent on a real project" is itself a skill being
practised here, not a side effect of using one. Done at Phase 0 (§39), before any product
code, because retrofitting it onto an already-large repo is exactly the situation it exists
to prevent.

**The honest motivation**, stated the same way §62 states it for the cloud backend: a tiny
solo project does not *need* CLAUDE.md files or custom skills to get built - one person
holds the whole thing in their head. It needs them anyway, because the skill being learned
is making a codebase legible to an agent that has no memory between sessions, which is
exactly the constraint a real team's shared codebase imposes on any single engineer,
human or agent.

**The bar**, in the same shape as the six cloud questions (§67) and the concurrency
questions (§73) - answerable about *this* repo, by observation, not by having written the
docs and trusting they're right:

1. Dropped into a fresh agent session with zero prior context, can it say what phase the
   project is on and what's next, without reading source code first?
2. Can it build and run each component (Android, Garmin, backend once it exists) from
   `CLAUDE.md` instructions alone, without trial-and-error?
3. Can it find *why* a past decision was made (PMTiles over Mapsforge, ECS over EKS,
   Kotlin over Java) without asking, because it's written down and not just in your head?
4. If two agent sessions work on this a week apart, does the second one pick up where the
   first left off, or does it re-derive context from git log and guesswork?

**Concretely, what to build:**

- **Root `CLAUDE.md`.** Architecture in one paragraph (the diagram in §2), the monorepo
  layout (§5), where the source of truth for each domain lives (this file for
  planning/why; `docs/*.md` for the current state of each subsystem), and the commands
  that build/test/run each part.
- **Per-directory `CLAUDE.md`** (`android/`, `garmin/`, `backend/` once it exists).
  Directory-scoped context: conventions, how to run just that piece, gotchas specific to
  that toolchain (Connect IQ simulator quirks, Gradle variants, Spring profiles). This is
  what lets an agent working only inside `garmin/` skip re-discovering Android context it
  doesn't need, and vice versa.
- **`.claude/skills/`.** Repo-specific, reusable procedures, not one-off prompts. Build
  these as the annoyance appears rather than up front:
      run-android        - build + launch the companion app on emulator/device
      run-garmin-sim     - build + launch the Connect IQ simulator
      gen-map <region>   - run the §13 preprocessing pipeline for an .osm.pbf region
      load-test <stage>  - run the matching k6 scenario from §68 and append the result
                           block to docs/FINDINGS.md in the right format automatically
  A skill earns its place when the same multi-step procedure has been typed by hand more
  than twice. Do not pre-build a skill for a procedure that hasn't been run yet.
- **`docs/decisions/NNNN-title.md` (lightweight ADRs).** One file per non-obvious choice
  already made in this document - PMTiles over Mapsforge (§11), S3 over a custom backend
  for map distribution (§10), ECS Fargate over EKS (§66), Kotlin+Spring Boot over
  Java (§66), OIDC over stored IAM keys (§72.1). Short: context, decision, why, what was
  rejected. This is the actual mechanism behind bar-question 3 - a decision written down
  once is not re-litigated by a later session that wasn't there for the discussion.
- **`docs/STATUS.md`.** One page, kept current, not historical: current phase (Track A and
  Track B independently, per §71.2), what's done, what's next, what's blocked and why. This
  is the answer to bar-question 1 and bar-question 4. Update it as part of finishing a
  phase, not as a separate remembered chore - treat a stale STATUS.md as a bug.
- **This file (`learning/garmin_offline_map.md`) stays the plan/why document**; `docs/*.md`
  become the current-state documents once each subsystem exists, per the earlier discussion
  on splitting by consumption rather than by shortening. Do not duplicate content between
  them - `docs/STATUS.md` points back here for the reasoning, this file does not restate
  what's already built.

**Explicitly out of scope for this objective:** memory/context persistence *across
different projects* is a Claude Code account-level feature, not something this repo
configures - nothing to build here. Hooks that enforce lint/tests on commit are a
reasonable later addition once there's code worth protecting, not a Phase 0 requirement.

**Where this fits:** Phase 0 only for the initial setup (§39, §60). The skills and ADRs
accumulate continuously across both tracks after that - there is no later "do agent setup"
phase to return to.
