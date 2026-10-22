import { AxiosError } from 'axios';
import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  isProblemDetail,
  toProblemMessage,
} from '../../../shared/api/problem-details';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  useActiveProfessionalsQuery,
  useAvailabilitySearchMutation,
  useCreateReservationMutation,
  useUpdateReservationStatusMutation,
} from '../hooks/useBookingData';
import type { AvailabilitySlot, Reservation } from '../types/booking.types';

type SlotsByDate = Record<string, AvailabilitySlot[]>;

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

function getSlotDateKey(slot: AvailabilitySlot): string {
  return slot.startsAt.slice(0, 10);
}

function formatDateLabel(dateKey: string): string {
  const date = new Date(`${dateKey}T00:00:00`);
  return date.toLocaleDateString('es-CL', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
  });
}

function formatSlotTime(slotDateTime: string): string {
  return slotDateTime.slice(11, 16);
}

function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function getWeekStartMonday(dateKey: string): Date {
  const baseDate = new Date(`${dateKey}T00:00:00`);
  const day = baseDate.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  return addDays(baseDate, diffToMonday);
}

function formatDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function buildWeekDateKeys(referenceDateKey: string): string[] {
  const monday = getWeekStartMonday(referenceDateKey);
  return Array.from({ length: 7 }, (_, index) =>
    formatDateKey(addDays(monday, index)),
  );
}

function groupSlotsByDate(slots: AvailabilitySlot[]): SlotsByDate {
  return slots.reduce<SlotsByDate>((acc, slot) => {
    const dateKey = getSlotDateKey(slot);
    if (!acc[dateKey]) {
      acc[dateKey] = [];
    }
    acc[dateKey].push(slot);
    return acc;
  }, {});
}

export function BookingPage() {
  const professionalsQuery = useActiveProfessionalsQuery();
  const availabilityMutation = useAvailabilitySearchMutation();
  const createReservationMutation = useCreateReservationMutation();
  const updateReservationStatusMutation = useUpdateReservationStatusMutation();

  const [selectedProfessionalIds, setSelectedProfessionalIds] = useState<string[]>([]);
  const [startDate, setStartDate] = useState<string>(toDateInput(new Date().toISOString()));
  const [endDate, setEndDate] = useState<string>(toDateInput(new Date().toISOString()));
  const [searchedStartDate, setSearchedStartDate] = useState<string | null>(null);
  const [durationMinutes, setDurationMinutes] = useState<number>(45);
  const [stepMinutes, setStepMinutes] = useState<number>(15);
  const [selectedSlot, setSelectedSlot] = useState<AvailabilitySlot | null>(null);
  const [referenceCode, setReferenceCode] = useState<string>('DEMO-UI-001');
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const slots = availabilityMutation.data?.slots ?? [];
  const groupedSlots = groupSlotsByDate(slots);
  const weekReferenceDateKey = useMemo(() => {
    const baseDate = searchedStartDate ?? startDate;
    if (slots.length === 0) {
      return baseDate;
    }

    const minSlotDateKey = slots
      .map(getSlotDateKey)
      .sort()[0];

    return minSlotDateKey ?? baseDate;
  }, [searchedStartDate, startDate, slots]);

  const weekDateKeys = useMemo(
    () => buildWeekDateKeys(weekReferenceDateKey),
    [weekReferenceDateKey],
  );
  const warning = availabilityMutation.data?.warning;

  const canSearch = useMemo(() => {
    return (
      selectedProfessionalIds.length > 0 &&
      startDate.length > 0 &&
      endDate.length > 0 &&
      durationMinutes > 0
    );
  }, [selectedProfessionalIds, startDate, endDate, durationMinutes]);

  const onToggleProfessional = (professionalId: string, checked: boolean) => {
    setSelectedProfessionalIds((current) => {
      if (checked) {
        return [...current, professionalId];
      }
      return current.filter((id) => id !== professionalId);
    });
  };

  const onSearch = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    setReservation(null);
    setSelectedSlot(null);

    if (!canSearch) {
      setFormError('Selecciona profesionales, rango de fechas y duracion.');
      return;
    }

    try {
      await availabilityMutation.mutateAsync({
        professionalIds: selectedProfessionalIds,
        startDate,
        endDate,
        durationMinutes,
        stepMinutes,
      });
      setSearchedStartDate(startDate);
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onCreateReservation = async () => {
    setFormError(null);
    if (!selectedSlot) {
      setFormError('Selecciona un bloque antes de crear la reserva.');
      return;
    }

    try {
      const created = await createReservationMutation.mutateAsync({
        professionalIds: selectedProfessionalIds,
        startsAt: selectedSlot.startsAt,
        endsAt: selectedSlot.endsAt,
        referenceCode,
      });
      setReservation(created);
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onUpdateStatus = async (status: 'CONFIRMED' | 'CANCELLED') => {
    setFormError(null);
    if (!reservation) {
      return;
    }

    try {
      const updated = await updateReservationStatusMutation.mutateAsync({
        reservationId: reservation.id,
        status,
      });
      setReservation(updated);
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  return (
    <main className="page">
      <div className="page-header">
        <h1>Reserva de horas</h1>
        <Link to="/availability">Definir disponibilidad</Link>
      </div>
      <p className="subtitle">
        Busca disponibilidad, selecciona un bloque y crea una reserva.
      </p>

      <section className="card">
        <h2>Busqueda</h2>
        {professionalsQuery.isLoading && <p>Cargando profesionales...</p>}
        {professionalsQuery.error && (
          <p className="error">{errorToMessage(professionalsQuery.error)}</p>
        )}

        {!professionalsQuery.isLoading && professionalsQuery.data && (
          <form onSubmit={onSearch}>
            <div className="grid">
              <label>
                Fecha inicio
                <input
                  type="date"
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                />
              </label>
              <label>
                Fecha fin
                <input
                  type="date"
                  value={endDate}
                  onChange={(event) => setEndDate(event.target.value)}
                />
              </label>
              <label>
                Duracion (minutos)
                <input
                  type="number"
                  min={15}
                  step={15}
                  value={durationMinutes}
                  onChange={(event) => setDurationMinutes(Number(event.target.value))}
                />
              </label>
              <label>
                Paso (minutos)
                <input
                  type="number"
                  min={5}
                  step={5}
                  value={stepMinutes}
                  onChange={(event) => setStepMinutes(Number(event.target.value))}
                />
              </label>
            </div>

            <fieldset>
              <legend>Profesionales</legend>
              <div className="professionals">
                {professionalsQuery.data.map((professional) => (
                  <label key={professional.id}>
                    <input
                      type="checkbox"
                      checked={selectedProfessionalIds.includes(professional.id)}
                      onChange={(event) =>
                        onToggleProfessional(professional.id, event.target.checked)
                      }
                    />
                    {professional.displayName} ({professional.specialty ?? 'Sin especialidad'})
                  </label>
                ))}
              </div>
            </fieldset>

            <button
              type="submit"
              disabled={availabilityMutation.isPending || !canSearch}
            >
              {availabilityMutation.isPending
                ? 'Buscando...'
                : 'Buscar disponibilidad'}
            </button>
          </form>
        )}

        {formError && <p className="error">{formError}</p>}
      </section>

      <section className="card">
        <h2>Bloques disponibles</h2>
        {warning && <p className="warning">{warning}</p>}
        {slots.length > 0 && (
          <p className="subtitle">Semana mostrada desde {weekDateKeys[0]} hasta {weekDateKeys[6]}.</p>
        )}
        {slots.length === 0 ? (
          <p>No hay bloques disponibles para los filtros actuales.</p>
        ) : (
          <div className="calendar-scroll">
            <div className="calendar-grid">
              {weekDateKeys.map((dateKey) => {
                const daySlots = groupedSlots[dateKey] ?? [];
                return (
                  <div key={dateKey} className="calendar-day">
                    <h3 className="calendar-day-title">{formatDateLabel(dateKey)}</h3>
                    {daySlots.length === 0 ? (
                      <p className="calendar-day-empty">Sin bloques</p>
                    ) : (
                      <ul className="slots">
                        {daySlots.map((slot) => {
                          const selected =
                            selectedSlot?.startsAt === slot.startsAt &&
                            selectedSlot?.endsAt === slot.endsAt;
                          return (
                            <li key={`${slot.startsAt}-${slot.endsAt}`}>
                              <button
                                type="button"
                                className={selected ? 'slot selected' : 'slot'}
                                onClick={() => setSelectedSlot(slot)}
                              >
                                {formatSlotTime(slot.startsAt)} - {formatSlotTime(slot.endsAt)}
                              </button>
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

      <section className="card">
        <h2>Crear reserva</h2>
        <label>
          Codigo de referencia
          <input
            type="text"
            value={referenceCode}
            onChange={(event) => setReferenceCode(event.target.value)}
          />
        </label>
        <button
          type="button"
          disabled={createReservationMutation.isPending || !selectedSlot}
          onClick={onCreateReservation}
        >
          {createReservationMutation.isPending ? 'Creando...' : 'Crear HOLD'}
        </button>

        {reservation && (
          <div className="reservation-result">
            <p>
              Reserva <strong>{reservation.id}</strong>
            </p>
            <p>
              {reservation.startsAt} - {reservation.endsAt}
            </p>
            <p>Estado: {reservation.status}</p>
            <div className="actions">
              <button
                type="button"
                disabled={
                  updateReservationStatusMutation.isPending ||
                  reservation.status !== 'HOLD'
                }
                onClick={() => onUpdateStatus('CONFIRMED')}
              >
                Confirmar
              </button>
              <button
                type="button"
                disabled={
                  updateReservationStatusMutation.isPending ||
                  (reservation.status !== 'HOLD' &&
                    reservation.status !== 'CONFIRMED')
                }
                onClick={() => onUpdateStatus('CANCELLED')}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}
      </section>
    </main>
  );
}
