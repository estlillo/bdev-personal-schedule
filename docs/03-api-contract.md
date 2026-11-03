# 03 — REST API contract (v1)

Base path: `/api/v1`. All IDs are UUID strings. Dates `YYYY-MM-DD`, times `HH:mm`, timestamps local ISO-8601 `YYYY-MM-DDTHH:mm:ss`, timezone `America/Santiago` for MVP. Document that timestamps are local and not UTC; future multi-zone deployment requires offset-aware timestamps.

## Professionals
- `GET /professionals` optional `active=true|false`: list.
- `POST /professionals` body `{ "displayName": "Profesional A", "specialty": "FONOAUDIOLOGIA" }` -> 201 + Location.
- `GET /professionals/{id}` -> 200 or 404.
- `PATCH /professionals/{id}` body `{ "displayName": "...", "specialty": "...", "active": true }` -> 200.

## Working windows
- `GET /professionals/{id}/working-windows` -> 200.
- `PUT /professionals/{id}/working-windows` replaces weekly windows atomically, body:
```json
{"windows":[{"dayOfWeek":1,"startTime":"08:00","endTime":"12:00"},{"dayOfWeek":1,"startTime":"13:00","endTime":"17:00"}]}
```
-> 200. Reject overlapping windows for the same day (400/422 consistently).

## Exceptions
- `GET /professionals/{id}/exceptions?from=2026-10-12&to=2026-10-18`
- `POST /professionals/{id}/exceptions` body `{ "date":"2026-10-16", "startTime":"08:00", "endTime":"12:00", "mode":"BLOCK", "reason":"Permiso" }` -> 201.
- `DELETE /professionals/{id}/exceptions/{exceptionId}` -> 204.

## Calendar events
- `GET /calendar-events?professionalId={uuid}&from=2026-10-12&to=2026-10-18`
- `POST /calendar-events` body `{ "professionalId":"uuid", "startsAt":"2026-10-16T08:00:00", "endsAt":"2026-10-16T08:45:00", "category":"MEETING" }` -> 201.
- `PATCH /calendar-events/{id}` -> 200; validate conflicts and working hours.
- `DELETE /calendar-events/{id}` -> 204.

## Availability
- `POST /availability/search` body:
```json
{"professionalIds":["uuid-1","uuid-2"],"startDate":"2026-10-19","endDate":"2026-10-30","durationMinutes":45,"stepMinutes":15}
```
-> 200:
```json
{"slots":[{"startsAt":"2026-10-19T08:00:00","endsAt":"2026-10-19T08:45:00"}],"asOf":"2026-10-12T15:00:00","warning":"Disponibilidad basada en registros locales; verificar agenda oficial"}
```
The example UUID placeholders are illustrative only; actual requests must use valid UUID values. `endDate` is inclusive. `stepMinutes` is a candidate start increment, not necessarily the duration. Enforce configurable maximum date range and maximum number of professionals.

## Reservations
- `GET /reservations?from=2026-10-12&to=2026-10-18&status=HOLD`
- `POST /reservations` body:
```json
{"professionalIds":["uuid-1","uuid-2"],"startsAt":"2026-10-19T08:00:00","endsAt":"2026-10-19T08:45:00","referenceCode":"CASO-001"}
```
- Optional `description` (short free text, non-identifying) may be included in create payload and response.
-> 201 with status HOLD; 409 on conflict.
- `PATCH /reservations/{id}/status` body `{ "status":"CONFIRMED" }` -> 200. Allowed: HOLD->CONFIRMED, HOLD->CANCELLED, CONFIRMED->CANCELLED. No transition out of CANCELLED.
- `DELETE /reservations/{id}` is not exposed; use CANCELLED for auditability.

## Error contract
Use `application/problem+json` (RFC 9457) with `type`, `title`, `status`, `detail`, `instance`, and extension `code`. Codes: `VALIDATION_ERROR`, `NOT_FOUND`, `SCHEDULE_CONFLICT`, `OUTSIDE_WORKING_HOURS`, `INVALID_STATUS_TRANSITION`.
