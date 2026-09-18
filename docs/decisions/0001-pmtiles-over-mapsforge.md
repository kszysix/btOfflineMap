# 0001. PMTiles over Mapsforge as the offline map archive format

## Context

The Android phone needs an offline map archive format for OSM-derived vector data covering
regions like Poland or Italy, readable from local storage with no network connection.
Mapsforge is the traditional choice for offline Android map rendering; PMTiles is a newer
single-file tile archive format designed for both local reads and HTTP range requests.

## Decision

Use PMTiles (v3 or current stable) as the MVP map archive format. Abstract access behind a
`MapDataSource` interface so the concrete format could theoretically be swapped later.

## Why

- PMTiles packages a complete tile pyramid (all zoom levels) in one file, which matches the
  discrete-zoom-level design (§20).
- The same archive format that works for local file reads today can later be served via
  HTTP range requests without re-processing the source data (§44) — relevant once AWS
  S3/CloudFront distribution is added (§40).
- It's simpler to reason about as "one file per region" for install/delete UX (§18) than
  Mapsforge's format, and pairs naturally with object storage (S3) as the future
  distribution mechanism (§8–§10).
- The Garmin never touches the archive directly either way — only Android's `MapEngine`
  does — so this choice doesn't affect the Garmin side.

## What was rejected

**Mapsforge.** Mature and Android-native, but tied to local file rendering with no natural
path to remote/partial access later, and pulls in an existing full map-rendering stack that
this project would rather build itself in a small `MapEngine` (learning purpose, §61 —
understanding the pipeline is part of the point).

## Status

Decided. See `docs/PROJECT_PLAN.md` §11 for the source statement of this decision; this ADR
exists so a future session doesn't need to re-read the whole plan to find the reasoning.
