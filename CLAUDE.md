# btOfflineMap

Private, non-commercial learning project. Full plan, rationale, and working style are in
`docs/PROJECT_PLAN.md` — read that first in any new session; this file is the map to the
rest of the repo, not a restatement of the plan.

## What this is

A Garmin Forerunner 255 acts as a lightweight GPS/display/input device. An Android phone is
the brain: it stores offline vector map data (PMTiles), decides what to render, runs the
activity-tracking pipeline, and stores everything locally.

    Garmin 255 <--BLE--> Android (map engine, activity engine, PMTiles storage, statistics)

The Garmin never understands OSM/PMTiles/vector tiles — only a small render protocol sent
to it by Android. Full architecture: `docs/PROJECT_PLAN.md` §2.

## Where things live

- **`docs/PROJECT_PLAN.md`** — the plan and the *why* behind every non-obvious choice. Read
  before making any architectural decision; don't re-litigate what's already decided there.
- **`docs/STATUS.md`** — current phase, what's done, what's next, what's blocked. Read this
  first in a new session to know where the project actually is right now.
- **`docs/decisions/`** — one short ADR per non-obvious choice already made. Check here
  before asking "why did we pick X" or before revisiting a settled choice.
- **`android/CLAUDE.md`** — Android build/run instructions and conventions.
- **`garmin/CLAUDE.md`** — Garmin (Connect IQ / Monkey C) build/run instructions and
  conventions.
- **`backend/CLAUDE.md`** — does not exist yet. Created when Track B starts (see
  `docs/PROJECT_PLAN.md` §71.2); there is no backend before then.
- **`.claude/skills/`** — repo-specific procedures (e.g. building/launching each component).
  Prefer these over re-deriving a multi-step command from scratch.

## Two tracks

- **Track A** — the device application (Android + Garmin). Phases 0–8, `docs/PROJECT_PLAN.md`
  §39/§60. This is the current work.
- **Track B** — backend, cloud, CI/CD, observability, scalability. Starts once Track A Phase
  2 (activity lifecycle + database) is done. See §71.

`docs/STATUS.md` tracks both tracks' progress independently.

## Repository layout

    android/       Kotlin/Compose companion app
    garmin/        Connect IQ (Monkey C) watch app
    backend/       (Track B only, not yet created)
    shared/        protocol definitions shared between Android and Garmin
    tools/         map-data preprocessing pipeline (OSM -> PMTiles)
    docs/          plan, current-state docs, decisions, status
    .claude/       skills and settings for working with this repo via Claude Code

## Working style

See `docs/PROJECT_PLAN.md` §0 before doing any non-trivial work in this repo: explain before
implementing, stop at phase/stage checkpoints, treat every non-pinned-down choice as a
decision to surface rather than silently pick, write ADRs for the "why," and don't
pre-optimize or add abstractions ahead of what the current phase needs.
