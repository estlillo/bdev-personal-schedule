# GitHub Copilot — Backend instructions

Project: Rehab Scheduler, a local scheduling assistant for rehabilitation professionals.

## Stack
- Java 21, Spring Boot 3.x, Maven.
- REST API, JSON, Jakarta Bean Validation, Spring JDBC JdbcClient, SQLite.
- Flyway migrations if SQLite compatibility is verified in the selected versions; otherwise use versioned SQL migration scripts with explicit documentation.
- JUnit 5, Mockito, integration tests against SQLite.
- OpenAPI 3 documentation.

## Architecture
- Modular monolith organized by domain: professional, schedule, calendar, availability, reservation.
- Controller -> Service -> Repository. DTOs must not expose persistence entities directly.
- Use LocalDate and LocalTime for weekly schedules; LocalDateTime for local appointment timestamps in America/Santiago.
- Store intervals as half-open [start, end). Adjacent intervals do not overlap.
- Use parameterized SQL. Use consistent RFC 9457 ProblemDetail errors.
- No patient RUT, names, diagnoses, or clinical records in MVP. Reservations may use an opaque external reference without identifiable data.
- Never assume data in the app is synchronized with the institution's scheduling system.

## Workflow
- Read docs/01-requirements.md through docs/05-implementation-plan.md before implementing a module.
- Implement one phase at a time; provide tests and migration scripts.
- Do not introduce authentication, AI, multi-user features, Excel import, or recurring multi-session search unless explicitly requested.
- Do not invent unspecified business rules; implement configurable behavior or document an assumption.
