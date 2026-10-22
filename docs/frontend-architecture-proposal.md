# Frontend Web Proposal - Rehab Scheduler

## 1. Purpose and scope
This document defines a technical base for implementing the web frontend in WebStorm, aligned with the backend already implemented (Phases 1-5).

Primary objective for first delivery:
- Prioritize the reservation flow screen (search availability, choose slot, create reservation, update status).

Current backend scope:
- Professionals, weekly windows, date exceptions, calendar events, availability search and reservations.
- SQLite local persistence.
- REST API under `/api/v1`.

Out of scope for this frontend stage:
- Clinical or patient-identifying data.
- Institutional integrations.
- Authentication/authorization (unless explicitly requested later).

## 2. Functional context already available
Implemented endpoints:
- `GET/POST/PATCH /api/v1/professionals`
- `GET/PUT /api/v1/professionals/{id}/working-windows`
- `GET/POST/DELETE /api/v1/professionals/{id}/exceptions`
- `GET/POST/PATCH/DELETE /api/v1/calendar-events`
- `POST /api/v1/availability/search`
- `GET/POST /api/v1/reservations`
- `PATCH /api/v1/reservations/{id}/status`

Data seed available for functional tests:
- `src/main/resources/db/seed/functional_seed.sql`

Backend rules that frontend must respect:
- Interval semantics are half-open `[start, end)`.
- Overlap condition: `a.start < b.end && b.start < a.end`.
- Reservation statuses and transitions:
  - `HOLD -> CONFIRMED`
  - `HOLD -> CANCELLED`
  - `CONFIRMED -> CANCELLED`
- Error contract RFC 9457 with stable codes:
  - `VALIDATION_ERROR`, `NOT_FOUND`, `SCHEDULE_CONFLICT`, `OUTSIDE_WORKING_HOURS`, `INVALID_STATUS_TRANSITION`

## 3. Recommended technology stack
Recommended option (balanced speed + maintainability):
- React 18+
- TypeScript
- Vite
- React Router
- TanStack Query (server state)
- React Hook Form + Zod (forms and validation)
- Day.js (date-time handling in `America/Santiago`)
- UI library: MUI or shadcn/ui

Reason:
- Fast MVP implementation.
- Good UX for interactive booking flow.
- Strong ecosystem for long-term iteration.

## 4. Proposed frontend architecture
## 4.1 Folder structure
```text
frontend/
  src/
    app/
      router.tsx
      providers.tsx
    modules/
      booking/
        pages/
          BookingPage.tsx
        components/
          BookingSearchForm.tsx
          AvailabilitySlotsList.tsx
          ReservationPanel.tsx
          ReservationStatusActions.tsx
        api/
          availability.api.ts
          reservations.api.ts
        hooks/
          useAvailabilitySearch.ts
          useCreateReservation.ts
          useUpdateReservationStatus.ts
        schemas/
          booking.schemas.ts
        types/
          booking.types.ts
      reservations/
        pages/
          ReservationsPage.tsx
        api/
          reservations.api.ts
      support/
        pages/
          ProfessionalsLookupPage.tsx
    shared/
      api/
        client.ts
        problem-details.ts
      components/
      config/
        env.ts
      utils/
        timezone.ts
        date-format.ts
```

## 4.2 State strategy
- Server state: TanStack Query for reads/mutations and cache invalidation.
- UI local state: component-level state or lightweight store only when needed.
- No global state manager beyond Query unless proven necessary.

## 4.3 API client strategy
- Single HTTP client wrapper (`fetch` or `axios`) in `shared/api/client.ts`.
- Standardized handling for `application/problem+json`.
- Map backend error `code` to user-friendly messages.

## 5. Priority UX flow: reservation screen
Main screen `BookingPage`:
1. Select professionals (active only).
2. Select date range, duration, optional step.
3. Search availability (`POST /availability/search`).
4. Select slot.
5. Create reservation (`POST /reservations`) with `referenceCode`.
6. Show resulting `HOLD` and allow transition actions:
   - Confirm (`PATCH /reservations/{id}/status` => `CONFIRMED`)
   - Cancel (`PATCH /reservations/{id}/status` => `CANCELLED`)

Minimum backend interactions for this flow:
- `GET /professionals?active=true`
- `POST /availability/search`
- `POST /reservations`
- `PATCH /reservations/{id}/status`
- Optional: `GET /reservations?from=...&to=...`

## 6. Error and validation behavior in UI
Frontend behavior guidelines:
- Render field-level errors on `VALIDATION_ERROR`.
- Show conflict banner on `SCHEDULE_CONFLICT` with retry suggestion.
- Show explicit message for `OUTSIDE_WORKING_HOURS`.
- Disable invalid status transitions in UI (while still handling backend errors).
- Keep original backend `detail` for support logs (not necessarily full display).

## 7. Date/time handling rules
- Use local timestamps exactly as backend contract expects: `YYYY-MM-DDTHH:mm:ss`.
- Use `America/Santiago` as display and input basis.
- Avoid converting to UTC in MVP payloads.

## 8. Functional test scenarios for frontend
Seed-driven scenarios using `functional_seed.sql`:
- Search availability for 1 professional with blocks and events.
- Search common availability for 2 professionals.
- Create `HOLD` reservation on available slot.
- Try creating conflicting reservation and verify `SCHEDULE_CONFLICT`.
- Transition `HOLD -> CONFIRMED -> CANCELLED` and verify list updates.
- Attempt invalid transition and verify `INVALID_STATUS_TRANSITION` handling.

## 9. Implementation roadmap (incremental)
Iteration 1 (priority demo):
- Booking page end-to-end (search + create + confirm/cancel).
- Basic reservations list for verification.

Iteration 2:
- Schedule support views (exceptions/events visibility).
- Better filtering and feedback states.

Iteration 3:
- Polishing, accessibility, i18n, loading skeletons, e2e tests.

## 10. WebStorm setup reference
Initial commands (example):
```powershell
# in repo root
# create separate frontend folder
# npm create vite@latest frontend -- --template react-ts
# cd frontend
# npm install
# npm install @tanstack/react-query react-router-dom react-hook-form zod dayjs
# optional UI library install
```

Runtime setup:
- Backend: `http://localhost:8080`
- Frontend dev server: `http://localhost:5173`
- Configure API base URL via env (`VITE_API_BASE_URL=http://localhost:8080/api/v1`).

## 11. AI context block for WebStorm
Use this as a base prompt for coding assistants in the frontend project:

```text
Project: Rehab Scheduler frontend (MVP).
Backend: Java 17, Spring Boot, REST API under /api/v1, SQLite.
Current backend scope: professionals, schedule windows, exceptions, calendar events, availability search, reservations.
Primary UI priority: booking screen (search availability, pick slot, create reservation, status transitions).
Critical rules:
- Intervals are [start,end).
- Reservation transitions: HOLD->CONFIRMED, HOLD->CANCELLED, CONFIRMED->CANCELLED.
- Timestamps local format YYYY-MM-DDTHH:mm:ss in America/Santiago.
- Handle ProblemDetail errors with code: VALIDATION_ERROR, NOT_FOUND, SCHEDULE_CONFLICT, OUTSIDE_WORKING_HOURS, INVALID_STATUS_TRANSITION.
Constraints:
- No patient-identifying data.
- No auth unless explicitly requested.
- Do not invent business rules outside backend contract/docs.
```

## 12. Open decisions
Pending product/technical confirmations:
- Final UI library (`MUI` vs `shadcn/ui`).
- Whether frontend lives in monorepo root (`frontend/`) or separate repository.
- If a dedicated endpoint is needed for inverse assignment query (given slot -> available professionals).
- MVP language and copy style for end users.

