# 05 — Implementation plan for Copilot

## Phase 1 — Scaffold and professionals
Generate Maven project, package-by-feature modules, SQLite datasource with foreign keys, versioned schema migrations, professionals CRUD, validation, OpenAPI and tests.

## Phase 2 — Weekly schedule and exceptions
Implement working windows replacement and exception endpoints. Validate windows and overlap rules; add unit and integration tests.

## Phase 3 — Calendar events
Implement blocking events and CRUD. Validate overlaps with active reservations and working windows. Tests.

## Phase 4 — Availability engine
Pure domain interval operations (merge, subtract, intersect, candidate slot generation). Service reads repositories. Endpoint, edge-case tests, pagination or range limits.

## Phase 5 — Reservations
Implement atomic multi-professional reservation creation, status transitions, conflict handling and concurrency tests with real SQLite.

## Phase 6 — Hardening
Backups, log hygiene, configuration, README for local startup, OpenAPI review, end-to-end tests.

## First Copilot prompt
Read `.github/copilot-instructions.md` and `docs/01-requirements.md` through `docs/05-implementation-plan.md`. Implement ONLY Phase 1. Before writing code, propose package structure and list any ambiguous technical assumptions. Then create the minimal compiling Spring Boot project, SQL migrations, professional endpoints, validation, tests, and a local startup README. Do not implement other phases yet.
