# 04 — Business rules and algorithm

## Interval semantics
- All intervals are half-open `[start, end)`. `[08:00,08:45)` does not conflict with `[08:45,09:30)`.
- Overlap test: `a.start < b.end && b.start < a.end`.
- Working windows may be split by lunch breaks; no overnight shifts in MVP.
- Blocking calendar events and HOLD/CONFIRMED reservations subtract from working availability.
- CANCELLED reservations never block.
- BLOCK exceptions subtract time; ADD exceptions add working time for a specific date. When ADD and BLOCK overlap, BLOCK takes precedence.
- For each date and professional, compute `(weekly working windows ∪ ADD windows) − BLOCK windows − calendar events − active reservations`.
- For multiple professionals, intersect their free interval sets.
- Generate candidate starts on `stepMinutes` boundaries that fit `durationMinutes` fully inside each common interval; sort ascending.
- Never return a candidate for inactive or nonexistent professionals; respond with validation/not-found errors as appropriate.

## Atomic reservation creation
1. Begin a SQLite write transaction that serializes writers (BEGIN IMMEDIATE or verified framework equivalent).
2. Recompute and validate requested interval for every selected professional against working hours, exceptions, calendar events and all HOLD/CONFIRMED reservations.
3. Insert reservation and all reservation_professional rows in the same transaction.
4. Commit; on conflict rollback and return 409.

## Data quality
- Paper calendars are the external source of truth. Show last-updated timestamp.
- Local reservations do not automatically appear in institutional systems.
- No patient identifiers in event notes, reservation references, logs, or test fixtures.

## Mandatory tests
- Single-professional availability; two- and three-professional intersection.
- Lunch gap, holiday block, date-specific added hours, inactive professional.
- Adjacent slots not conflicting; partial overlaps conflicting.
- 30/45/60-minute requests, step 15 minutes.
- Event vs reservation conflict and reservation vs reservation conflict.
- HOLD blocks; CONFIRMED blocks; CANCELLED releases.
- Concurrent requests for the same slot: at most one succeeds.
- DST/timezone edge cases in America/Santiago; for MVP restrict to normal daytime schedules and document any unsupported ambiguous times.
