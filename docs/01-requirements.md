# 01 — Product requirements (MVP backend)

## Context
A single coordinator manages availability for approximately seven rehabilitation professionals. Today she reviews paper weekly schedules updated by hand and searches for simultaneous free time for one or more professionals. Patient identification and clinical decisions happen outside this application. A nurse confirms appointments by telephone; the official institutional appointment system is separate.

## Goal
Maintain an auxiliary local scheduling database, search available time for one or multiple selected professionals, and provisionally reserve time without internal conflicts.

## In scope
1. CRUD and activation/deactivation of professionals.
2. Weekly working windows per professional (e.g. Mondays 08:00–12:00, 13:00–17:00), supporting multiple windows per day.
3. Date-specific exceptions (leave, holidays, one-off additional working windows).
4. Calendar events that occupy professional time (appointment, meeting, workshop, administrative block), with optional recurrence postponed.
5. Availability search over a date range, with one or more professionals, duration in minutes and optional slot step.
6. Reservation lifecycle: HOLD, CONFIRMED, CANCELLED. A HOLD blocks availability until explicitly cancelled; automatic expiry is deferred.
7. Ability to link multiple professionals to the same reservation.
8. Last-updated timestamp per professional schedule or a computed last change timestamp.
9. Backup/restore documented for the local SQLite database.

## Out of scope
Clinical data, patient names/RUT/diagnoses, institutional integration, automatic telephone calls, OCR from photographed paper, AI, multi-user access, recurring multi-session scheduling, PDF and Excel exports.

## Important limitation
The source paper schedules change manually. Availability results reflect only data entered into this app and must be labeled accordingly.

## Acceptance criteria
- Selecting one professional returns only time within working windows and outside blocking events/exceptions.
- Selecting two or more returns only intersections of their free intervals.
- A reservation blocks all assigned professionals atomically.
- Attempting a conflicting reservation returns HTTP 409 with a stable error code.
- Cancelled reservations do not block availability.
- Date range and duration are validated and bounded by configurable limits.
