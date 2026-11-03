# Rehab Scheduler Frontend

Frontend inicial para el flujo prioritario de reservas.

## Requisitos
- Node.js 20+
- Backend levantado en `http://localhost:8080`

## Configuracion
Archivo `.env`:

```dotenv
VITE_API_BASE_URL=/api/v1
```

Con esta configuracion, Vite usa proxy local hacia el backend y evita problemas de CORS en desarrollo.

## Scripts

```powershell
npm install
npm run dev
npm run build
```

## Flujo implementado (Booking)
- Cargar profesionales activos.
- Buscar disponibilidad por rango, duracion y paso.
- Seleccionar slot.
- Crear reserva en `HOLD`.
- Confirmar o cancelar la reserva creada.

## Pantalla de disponibilidad por profesional
- Ruta: `/availability`
- Permite definir horario semanal (`working windows`).
- Permite consultar, crear y eliminar excepciones (`BLOCK`/`ADD`).

## Pantalla de profesionales (CRUD simple)
- Ruta: `/professionals`
- Permite crear profesionales.
- Permite editar nombre/especialidad.
- Permite activar o desactivar profesionales.

## Archivos clave
- `src/modules/booking/pages/BookingPage.tsx`
- `src/modules/booking/hooks/useBookingData.ts`
- `src/modules/booking/api/*.ts`
- `src/modules/professionals/pages/ProfessionalsCrudPage.tsx`
- `src/modules/professionals/hooks/useProfessionalsData.ts`
- `src/modules/professionals/api/professionals.api.ts`
- `src/modules/schedule/pages/ProfessionalAvailabilityPage.tsx`
- `src/modules/schedule/hooks/useScheduleData.ts`
- `src/modules/schedule/api/schedule.api.ts`
- `src/shared/api/client.ts`
- `src/shared/api/problem-details.ts`
