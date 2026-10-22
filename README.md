# Rehab Scheduler Backend (Fases 1-5)

Backend en Java 17 + Spring Boot 4 con avance hasta Fase 5 del plan:

- Fase 1: CRUD de profesionales.
- Fase 2: working windows semanales y excepciones (`BLOCK`/`ADD`).
- Fase 3: calendar events bloqueantes.
- Fase 4: motor de disponibilidad (`merge`, `subtract`, `intersect`, generacion de slots).
- Fase 5: reservas atomicas multi-profesional (`HOLD`, `CONFIRMED`, `CANCELLED`).

## Stack y base tecnica

- Monolito modular por dominio.
- SQLite local con `PRAGMA foreign_keys=ON`.
- Migraciones SQL versionadas en `src/main/resources/db/migration`.
- Errores RFC 9457 (`application/problem+json`) con `code` estable.
- Pruebas unitarias e integracion con SQLite.

## Endpoints principales

- `GET/POST/PATCH /api/v1/professionals`
- `GET/PUT /api/v1/professionals/{id}/working-windows`
- `GET/POST/DELETE /api/v1/professionals/{id}/exceptions`
- `GET/POST/PATCH/DELETE /api/v1/calendar-events`
- `POST /api/v1/availability/search`
- `GET/POST /api/v1/reservations`
- `PATCH /api/v1/reservations/{id}/status`

## Configuracion de disponibilidad

En `src/main/resources/application.properties`:

- `availability.max-range-days=30`
- `availability.max-professionals=7`
- `availability.max-returned-slots=500`
- `availability.default-step-minutes=15`

## Datos ficticios para pruebas funcionales

- Script: `src/main/resources/db/seed/functional_seed.sql`
- Cubre: profesionales, working windows, excepciones, calendar events, reservas y enlaces multi-profesional.
- El script hace reset seguro (orden FK) y luego inserta un dataset completo para pruebas manuales.

## Ejecutar

```powershell
.\gradlew.bat bootRun
```

## Ejecutar pruebas

```powershell
.\gradlew.bat test
```

## API y OpenAPI

- Base path: `/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Nota sobre migraciones

Se usa un runner de scripts SQL versionados (sin Flyway) para compatibilidad explicita con SQLite. Cada script `Vxxx__*.sql` se ejecuta una sola vez y se registra en `schema_migration`.
