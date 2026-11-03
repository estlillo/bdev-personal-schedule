import { AxiosError } from 'axios';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchActiveProfessionals } from '../../booking/api/professionals.api';
import type { Professional } from '../../booking/types/booking.types';
import {
  isProblemDetail,
  toProblemMessage,
} from '../../../shared/api/problem-details';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  useCreateScheduleExceptionMutation,
  useDeleteScheduleExceptionMutation,
  useReplaceWorkingWindowsMutation,
  useScheduleExceptionsQuery,
  useWorkingWindowsQuery,
} from '../hooks/useScheduleData';
import type {
  ScheduleException,
  WorkingWindowRequest,
} from '../types/schedule.types';

type ExceptionDraft = {
  date: string;
  startTime: string;
  endTime: string;
  mode: 'BLOCK' | 'ADD';
  reason: string;
};

function toDateInput(dateIso: string): string {
  return dateIso.slice(0, 10);
}

function plusDays(dateInput: string, days: number): string {
  const date = new Date(`${dateInput}T00:00:00`);
  date.setDate(date.getDate() + days);
  return toDateInput(date.toISOString());
}

function errorToMessage(error: unknown): string {
  if (!error) {
    return 'Ocurrio un error inesperado.';
  }
  if (error instanceof AxiosError && isProblemDetail(error.response?.data)) {
    return toProblemMessage(error.response?.data as ProblemDetail);
  }
  return 'No se pudo completar la solicitud.';
}

function dayLabel(dayOfWeek: number): string {
  const labels = ['Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab', 'Dom'];
  return labels[dayOfWeek - 1] ?? `Dia ${dayOfWeek}`;
}

function hasOverlaps(windows: WorkingWindowRequest[]): boolean {
  const byDay = new Map<number, WorkingWindowRequest[]>();
  for (const window of windows) {
    if (!byDay.has(window.dayOfWeek)) {
      byDay.set(window.dayOfWeek, []);
    }
    byDay.get(window.dayOfWeek)?.push(window);
  }

  for (const dayWindows of byDay.values()) {
    const sorted = [...dayWindows].sort((a, b) =>
      a.startTime.localeCompare(b.startTime),
    );
    for (let i = 1; i < sorted.length; i++) {
      if (sorted[i].startTime < sorted[i - 1].endTime) {
        return true;
      }
    }
  }

  return false;
}

export function ProfessionalAvailabilityPage() {
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [loadingProfessionals, setLoadingProfessionals] = useState(true);
  const [selectedProfessionalId, setSelectedProfessionalId] = useState<string | null>(
    null,
  );

  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const [windowsDraft, setWindowsDraft] = useState<WorkingWindowRequest[]>([]);
  const [presetStartTime, setPresetStartTime] = useState<string>('09:00');
  const [presetEndTime, setPresetEndTime] = useState<string>('18:00');

  const today = useMemo(() => toDateInput(new Date().toISOString()), []);
  const [exceptionsFrom, setExceptionsFrom] = useState<string>(today);
  const [exceptionsTo, setExceptionsTo] = useState<string>(plusDays(today, 7));

  const [exceptionDraft, setExceptionDraft] = useState<ExceptionDraft>({
    date: today,
    startTime: '08:00',
    endTime: '09:00',
    mode: 'BLOCK',
    reason: '',
  });

  useEffect(() => {
    const load = async () => {
      try {
        const data = await fetchActiveProfessionals();
        setProfessionals(data);
        if (data.length > 0) {
          setSelectedProfessionalId(data[0].id);
        }
      } catch (error) {
        setFormError(errorToMessage(error));
      } finally {
        setLoadingProfessionals(false);
      }
    };

    void load();
  }, []);

  const workingWindowsQuery = useWorkingWindowsQuery(selectedProfessionalId);
  const exceptionsQuery = useScheduleExceptionsQuery(
    selectedProfessionalId,
    exceptionsFrom,
    exceptionsTo,
  );

  const replaceWorkingWindowsMutation = useReplaceWorkingWindowsMutation();
  const createExceptionMutation = useCreateScheduleExceptionMutation();
  const deleteExceptionMutation = useDeleteScheduleExceptionMutation();

  useEffect(() => {
    if (!workingWindowsQuery.data) {
                                           setWindowsDraft([]);
      return;
    }

    setWindowsDraft(
      workingWindowsQuery.data.windows.map((window) => ({
        dayOfWeek: window.dayOfWeek,
        startTime: window.startTime,
        endTime: window.endTime,
      })),
    );
  }, [workingWindowsQuery.data]);

  const onAddWindow = () => {
    setWindowsDraft((current) => [
      ...current,
      { dayOfWeek: 1, startTime: '08:00', endTime: '09:00' },
    ]);
  };

  const onChangeWindow = (
    index: number,
    field: keyof WorkingWindowRequest,
    value: string,
  ) => {
    setWindowsDraft((current) =>
      current.map((window, i) => {
        if (i !== index) {
          return window;
        }
        if (field === 'dayOfWeek') {
          return { ...window, dayOfWeek: Number(value) };
        }
        return { ...window, [field]: value };
      }),
    );
  };

  const onRemoveWindow = (index: number) => {
    setWindowsDraft((current) => current.filter((_, i) => i !== index));
  };

  const onApplyWeekPreset = () => {
    setFormError(null);
    setSuccessMessage(null);

    if (presetStartTime >= presetEndTime) {
      setFormError('El horario base debe tener inicio menor que fin.');
      return;
    }

    setWindowsDraft([
      { dayOfWeek: 1, startTime: presetStartTime, endTime: presetEndTime },
      { dayOfWeek: 2, startTime: presetStartTime, endTime: presetEndTime },
      { dayOfWeek: 3, startTime: presetStartTime, endTime: presetEndTime },
      { dayOfWeek: 4, startTime: presetStartTime, endTime: presetEndTime },
      { dayOfWeek: 5, startTime: presetStartTime, endTime: presetEndTime },
    ]);
    setSuccessMessage('Plantilla Lun-Vie aplicada. Recuerda guardar cambios.');
  };

  const onApplyQuickPreset = (startTime: string, endTime: string, label: string) => {
    setPresetStartTime(startTime);
    setPresetEndTime(endTime);
    setFormError(null);
    setSuccessMessage(null);

    setWindowsDraft([
      { dayOfWeek: 1, startTime, endTime },
      { dayOfWeek: 2, startTime, endTime },
      { dayOfWeek: 3, startTime, endTime },
      { dayOfWeek: 4, startTime, endTime },
      { dayOfWeek: 5, startTime, endTime },
    ]);
    setSuccessMessage(`Plantilla ${label} aplicada. Recuerda guardar cambios.`);
  };

  const onSaveWindows = async () => {
    setFormError(null);
    setSuccessMessage(null);

    if (!selectedProfessionalId) {
      setFormError('Selecciona un profesional.');
      return;
    }

    for (const window of windowsDraft) {
      if (window.dayOfWeek < 1 || window.dayOfWeek > 7) {
        setFormError('El dia de semana debe estar entre 1 y 7.');
        return;
      }
      if (window.startTime >= window.endTime) {
        setFormError('Cada bloque debe tener inicio menor que fin.');
        return;
      }
    }

    if (hasOverlaps(windowsDraft)) {
      setFormError('Hay bloques solapados en un mismo dia.');
      return;
    }

    try {
      await replaceWorkingWindowsMutation.mutateAsync({
        professionalId: selectedProfessionalId,
        payload: { windows: windowsDraft },
      });
      setSuccessMessage('Horarios semanales actualizados.');
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onCreateException = async () => {
    setFormError(null);
    setSuccessMessage(null);

    if (!selectedProfessionalId) {
      setFormError('Selecciona un profesional.');
      return;
    }
    if (exceptionDraft.startTime >= exceptionDraft.endTime) {
      setFormError('La excepcion debe tener inicio menor que fin.');
      return;
    }
    if (exceptionDraft.reason.length > 255) {
      setFormError('La razon no puede exceder 255 caracteres.');
      return;
    }

    try {
      await createExceptionMutation.mutateAsync({
        professionalId: selectedProfessionalId,
        payload: {
          date: exceptionDraft.date,
          startTime: exceptionDraft.startTime,
          endTime: exceptionDraft.endTime,
          mode: exceptionDraft.mode,
          reason: exceptionDraft.reason.trim() || undefined,
        },
      });
      setSuccessMessage('Excepcion creada.');
      setExceptionDraft((current) => ({ ...current, reason: '' }));
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onDeleteException = async (exception: ScheduleException) => {
    setFormError(null);
    setSuccessMessage(null);

    if (!selectedProfessionalId) {
      return;
    }

    try {
      await deleteExceptionMutation.mutateAsync({
        professionalId: selectedProfessionalId,
        exceptionId: exception.id,
      });
      setSuccessMessage('Excepcion eliminada.');
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  return (
    <main className="page">
      <div className="page-header">
        <h1>Disponibilidad por profesional</h1>
        <div className="actions">
          <Link to="/booking">Volver a reservas</Link>
          <Link to="/calendar">Calendario general</Link>
          <Link to="/professionals">Gestionar profesionales</Link>
        </div>
      </div>
      <p className="subtitle">
        Define horarios semanales y excepciones de agenda para un profesional.
      </p>

      {formError && <p className="error">{formError}</p>}
      {successMessage && <p className="success">{successMessage}</p>}

      <section className="card">
        <h2>Profesional</h2>
        {loadingProfessionals ? (
          <p>Cargando profesionales...</p>
        ) : (
          <label>
            Seleccion
            <select
              value={selectedProfessionalId ?? ''}
              onChange={(event) => setSelectedProfessionalId(event.target.value)}
            >
              {professionals.map((professional) => (
                <option key={professional.id} value={professional.id}>
                  {professional.displayName} ({professional.specialty ?? 'Sin especialidad'})
                </option>
              ))}
            </select>
          </label>
        )}
      </section>

      <section className="card">
        <h2>Horario semanal</h2>
        <div className="grid">
          <label>
            Plantilla inicio
            <input
              type="time"
              value={presetStartTime}
              onChange={(event) => setPresetStartTime(event.target.value)}
            />
          </label>
          <label>
            Plantilla fin
            <input
              type="time"
              value={presetEndTime}
              onChange={(event) => setPresetEndTime(event.target.value)}
            />
          </label>
        </div>
        <div className="actions">
          <button
            type="button"
            onClick={() => onApplyQuickPreset('08:00', '17:00', 'Lun-Vie 08:00-17:00')}
          >
            Rapida Lun-Vie 08:00-17:00
          </button>
          <button
            type="button"
            onClick={() => onApplyQuickPreset('09:00', '18:00', 'Lun-Vie 09:00-18:00')}
          >
            Rapida Lun-Vie 09:00-18:00
          </button>
        </div>
        <button type="button" onClick={onApplyWeekPreset}>
          Aplicar Lun-Vie a todo el bloque
        </button>
        {workingWindowsQuery.isLoading ? (
          <p>Cargando horario semanal...</p>
        ) : (
          <>
            <div className="table-like">
              <div className="table-row table-header-row">
                <span>Dia</span>
                <span>Inicio</span>
                <span>Fin</span>
                <span>Acciones</span>
              </div>
              {windowsDraft.map((window, index) => (
                <div className="table-row" key={`${index}-${window.dayOfWeek}-${window.startTime}`}>
                  <select
                    value={window.dayOfWeek}
                    onChange={(event) =>
                      onChangeWindow(index, 'dayOfWeek', event.target.value)
                    }
                  >
                    {[1, 2, 3, 4, 5, 6, 7].map((day) => (
                      <option key={day} value={day}>
                        {dayLabel(day)}
                      </option>
                    ))}
                  </select>
                  <input
                    type="time"
                    value={window.startTime}
                    onChange={(event) =>
                      onChangeWindow(index, 'startTime', event.target.value)
                    }
                  />
                  <input
                    type="time"
                    value={window.endTime}
                    onChange={(event) =>
                      onChangeWindow(index, 'endTime', event.target.value)
                    }
                  />
                  <button type="button" onClick={() => onRemoveWindow(index)}>
                    Quitar
                  </button>
                </div>
              ))}
            </div>

            <div className="actions">
              <button type="button" onClick={onAddWindow}>
                Agregar bloque
              </button>
              <button
                type="button"
                onClick={onSaveWindows}
                disabled={replaceWorkingWindowsMutation.isPending || !selectedProfessionalId}
              >
                {replaceWorkingWindowsMutation.isPending
                  ? 'Guardando...'
                  : 'Guardar horario semanal'}
              </button>
            </div>
          </>
        )}
      </section>

      <section className="card">
        <h2>Excepciones</h2>
        <div className="grid">
          <label>
            Desde
            <input
              type="date"
              value={exceptionsFrom}
              onChange={(event) => setExceptionsFrom(event.target.value)}
            />
          </label>
          <label>
            Hasta
            <input
              type="date"
              value={exceptionsTo}
              onChange={(event) => setExceptionsTo(event.target.value)}
            />
          </label>
        </div>

        {exceptionsQuery.isLoading ? (
          <p>Cargando excepciones...</p>
        ) : (
          <div className="table-like">
            <div className="table-row table-row-5 table-header-row">
              <span>Fecha</span>
              <span>Horario</span>
              <span>Modo</span>
              <span>Razon</span>
              <span>Acciones</span>
            </div>
            {(exceptionsQuery.data ?? []).map((exception) => (
              <div className="table-row table-row-5" key={exception.id}>
                <span>{exception.date}</span>
                <span>
                  {exception.startTime} - {exception.endTime}
                </span>
                <span>{exception.mode}</span>
                <span>{exception.reason ?? '-'}</span>
                <button
                  type="button"
                  onClick={() => onDeleteException(exception)}
                  disabled={deleteExceptionMutation.isPending}
                >
                  Eliminar
                </button>
              </div>
            ))}
          </div>
        )}

        <h3>Nueva excepcion</h3>
        <div className="grid">
          <label>
            Fecha
            <input
              type="date"
              value={exceptionDraft.date}
              onChange={(event) =>
                setExceptionDraft((current) => ({
                  ...current,
                  date: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Inicio
            <input
              type="time"
              value={exceptionDraft.startTime}
              onChange={(event) =>
                setExceptionDraft((current) => ({
                  ...current,
                  startTime: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Fin
            <input
              type="time"
              value={exceptionDraft.endTime}
              onChange={(event) =>
                setExceptionDraft((current) => ({
                  ...current,
                  endTime: event.target.value,
                }))
              }
            />
          </label>
          <label>
            Modo
            <select
              value={exceptionDraft.mode}
              onChange={(event) =>
                setExceptionDraft((current) => ({
                  ...current,
                  mode: event.target.value as 'BLOCK' | 'ADD',
                }))
              }
            >
              <option value="BLOCK">BLOCK</option>
              <option value="ADD">ADD</option>
            </select>
          </label>
          <label>
            Razon
            <input
              type="text"
              value={exceptionDraft.reason}
              onChange={(event) =>
                setExceptionDraft((current) => ({
                  ...current,
                  reason: event.target.value,
                }))
              }
            />
          </label>
        </div>

        <button
          type="button"
          onClick={onCreateException}
          disabled={createExceptionMutation.isPending || !selectedProfessionalId}
        >
          {createExceptionMutation.isPending
            ? 'Creando...'
            : 'Crear excepcion'}
        </button>
      </section>
    </main>
  );
}

