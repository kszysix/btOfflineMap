# 0002. Java (not Kotlin) for the Track B backend

## Context

`docs/PROJECT_PLAN.md` §66 originally specified Kotlin + Spring Boot for the sync backend,
reasoned in §73: coroutines on the backend, under connection-pool pressure, as a deliberate
contrast to coroutines on Android under lifecycle/cancellation pressure — same language, two
different concurrency pressures.

The developer wants to use this project to refresh Java specifically, ahead of Track B
actually starting (Track A Phase 2 is the trigger, per §71.2 — not started yet).

## Decision

Use **Java** (not Kotlin) for the backend, still on Spring Boot, still Postgres via
Spring Data JDBC/JDBC/jOOQ (not full JPA/Hibernate — that reasoning in §66 is language-
independent and still holds).

## Why

- Direct, stated reason: the developer wants Java practice, and a real backend with genuine
  production concerns (this project's whole point) is a better way to refresh it than
  toy exercises.
- Spring Boot's backend-relevant lessons (§66–73: Postgres, Docker Compose, Kubernetes, ECS
  Fargate, CI/CD, load testing, observability) are identical in Java and Kotlin — none of
  it depends on the JVM language chosen.

## What changes as a result

- **§73's "same language, two concurrency models" framing no longer applies as originally
  written** — Android stays Kotlin coroutines, but the backend is now Java. The
  concurrency-under-load learning target in §68/§73 is still fully intact; the comparison
  is now **virtual threads (Java 21+ / JEP 444, Spring Boot 3.2+
  `spring.threads.virtual.enabled`) vs. Kotlin coroutines vs. traditional thread-per-request**
  rather than "coroutines in two runtimes." Same underlying questions (what actually limits
  concurrency — thread model or connection pool? what happens to a blocked call mid-cancel?),
  different mechanism on the backend side.
- JDK version target for the backend should be Java 21 LTS or newer specifically *because*
  virtual threads are the interesting mechanism to measure under §68's load tests — confirm
  the actual current LTS at Track B kickoff per §66's own "pin versions at setup time" rule.

## What was rejected

**Kotlin for the backend**, as originally planned in §66. Rejected not because Kotlin is a
worse fit technically, but because it doesn't serve the developer's own learning goal as
directly as Java does right now.

## Status

Decided ahead of Track B starting. Nothing to build yet — `backend/` doesn't exist until
Track A Phase 2 is done (§71.2). This ADR exists so that decision point doesn't need
re-litigating when Track B actually kicks off.
