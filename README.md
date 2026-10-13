# Rehab Scheduler Backend (Phase 1)

Implementacion inicial del backend en Java 17 + Spring Boot 4 para la Fase 1:

- Estructura modular monolito por dominio.
- Configuracion SQLite local con `PRAGMA foreign_keys=ON`.
- Migraciones SQL versionadas (`src/main/resources/db/migration`).
- CRUD de profesionales (`/api/v1/professionals`).
- Errores RFC 9457 (`application/problem+json`) con `code` estable.
- Pruebas unitarias e integracion.

## Estructura de paquetes (Fase 1)

- `com.barbatosdev.bdevpersonalschedule.professional`
  - `api`, `application`, `domain`, `persistence`
- `com.barbatosdev.bdevpersonalschedule.infrastructure`
  - `config`, `migration`, `openapi`
- `com.barbatosdev.bdevpersonalschedule.shared`
  - `error`
- Paquetes reservados para fases siguientes:
  - `schedule`, `calendar`, `availability`, `reservation`

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

Para Fase 1 se usa un runner de scripts SQL versionados (sin Flyway), siguiendo el criterio de compatibilidad explicita con SQLite. Cada script `Vxxx__*.sql` se ejecuta una sola vez y se registra en `schema_migration`.

