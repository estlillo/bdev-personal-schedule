import { AxiosError } from 'axios';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  isProblemDetail,
  toProblemMessage,
} from '../../../shared/api/problem-details';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  useAllProfessionalsQuery,
  useReservationsByRangeQuery,
} from '../hooks/useBookingData';
import type { Reservation } from '../types/booking.types';

type ReservationsByDate = Record<string, Reservation[]>;

function errorToMessage(error: unknown): string {
  if (!error) {
    return 'Ocurrio un error inesperado.';
  }
  if (error instanceof AxiosError && isProblemDetail(error.response?.data)) {
    return toProblemMessage(error.response.data as ProblemDetail);
  }
  return 'No se pudo completar la solicitud.';
}

function toDateInput(dateIso: string): string {
  return dateIso.slice(0, 10);
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function formatDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getWeekStartMonday(dateKey: string): Date {
  const baseDate = new Date(`${dateKey}T00:00:00`);
  const day = baseDate.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  return addDays(baseDate, diffToMonday);
}

function buildWeekDateKeys(referenceDateKey: string): string[] {
  const monday = getWeekStartMonday(referenceDateKey);
  return Array.from({ length: 7 }, (_, index) =>
    formatDateKey(addDays(monday, index)),
  );
}

function formatDateLabel(dateKey: string): string {
  const date = new Date(`${dateKey}T00:00:00`);
  return date.toLocaleDateString('es-CL', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
  });
}

function formatTime(dateTime: string): string {
  return dateTime.slice(11, 16);
}

function groupReservationsByStartDate(reservations: Reservation[]): ReservationsByDate {
  return reservations.reduce<ReservationsByDate>((acc, reservation) => {
    const dateKey = reservation.startsAt.slice(0, 10);
    if (!acc[dateKey]) {
      acc[dateKey] = [];
    }
    acc[dateKey].push(reservation);
    return acc;
  }, {});
}

function reservationTitle(reservation: Reservation): string {
  if (reservation.referenceCode && reservation.referenceCode.trim().length > 0) {
    return reservation.referenceCode;
  }
  return `Reserva ${reservation.id.slice(0, 8)}`;
}

export function ReservationsCalendarPage() {
  const [weekAnchor, setWeekAnchor] = useState<string>(toDateInput(new Date().toISOString()));

  const weekDateKeys = useMemo(() => buildWeekDateKeys(weekAnchor), [weekAnchor]);
  const weekFrom = weekDateKeys[0];
  const weekTo = weekDateKeys[6];

  const reservationsQuery = useReservationsByRangeQuery(weekFrom, weekTo);
  const professionalsQuery = useAllProfessionalsQuery();

  const reservations = useMemo(
    () => (reservationsQuery.data ?? []).filter((item) => item.status !== 'CANCELLED'),
    [reservationsQuery.data],
  );

  const reservationsByDate = useMemo(
    () => groupReservationsByStartDate(reservations),
    [reservations],
  );

  const professionalNamesById = useMemo(() => {
    const map = new Map<string, string>();
    for (const professional of professionalsQuery.data ?? []) {
      map.set(professional.id, professional.displayName);
    }
    return map;
  }, [professionalsQuery.data]);

  const onMoveWeek = (deltaDays: number) => {
    const nextDate = addDays(new Date(`${weekAnchor}T00:00:00`), deltaDays);
    setWeekAnchor(formatDateKey(nextDate));
  };

  return (
    <main className="page">
      <div className="page-header">
        <h1>Calendario general de reservas</h1>
        <div className="actions">
          <Link to="/booking">Ir a reservas</Link>
          <Link to="/availability">Definir disponibilidad</Link>
          <Link to="/professionals">Gestionar profesionales</Link>
        </div>
      </div>

      <p className="subtitle">
        Vista semanal con horas ya reservadas (HOLD y CONFIRMED).
      </p>

      <section className="card">
        <div className="actions">
          <button type="button" onClick={() => onMoveWeek(-7)}>
            Semana anterior
          </button>
          <button type="button" onClick={() => onMoveWeek(7)}>
            Semana siguiente
          </button>
        </div>
        <p className="subtitle">
          Semana mostrada desde {weekFrom} hasta {weekTo}.
        </p>
      </section>

      <section className="card">
        <h2>Reservas por dia</h2>

        {reservationsQuery.isLoading || professionalsQuery.isLoading ? (
          <p>Cargando calendario...</p>
        ) : reservationsQuery.error ? (
          <p className="error">{errorToMessage(reservationsQuery.error)}</p>
        ) : professionalsQuery.error ? (
          <p className="error">{errorToMessage(professionalsQuery.error)}</p>
        ) : (
          <div className="calendar-scroll">
            <div className="calendar-grid">
              {weekDateKeys.map((dateKey) => {
                const dayReservations =
                  (reservationsByDate[dateKey] ?? []).slice().sort((a, b) =>
                    a.startsAt.localeCompare(b.startsAt),
                  );

                return (
                  <div key={dateKey} className="calendar-day">
                    <h3 className="calendar-day-title">{formatDateLabel(dateKey)}</h3>
                    {dayReservations.length === 0 ? (
                      <p className="calendar-day-empty">Sin reservas</p>
                    ) : (
                      <ul className="slots">
                        {dayReservations.map((reservation) => {
                          const professionalNames = reservation.professionalIds.map((id) =>
                            professionalNamesById.get(id) ?? `Profesional ${id.slice(0, 8)}`,
                          );

                          return (
                            <li key={reservation.id}>
                              <article className="reservation-card">
                                <p className="reservation-card-time">
                                  {formatTime(reservation.startsAt)} - {formatTime(reservation.endsAt)}
                                </p>
                                <p className="reservation-card-title">
                                  {reservationTitle(reservation)}
                                </p>
                                <p className="reservation-card-description">
                                  {reservation.description?.trim() || 'Sin descripcion'}
                                </p>
                                <p className="reservation-card-professionals">
                                  Profesionales: {professionalNames.join(', ')}
                                </p>
                              </article>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </section>
    </main>
  );
}

