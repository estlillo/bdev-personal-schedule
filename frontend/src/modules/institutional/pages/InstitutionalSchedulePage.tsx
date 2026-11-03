import { AxiosError } from 'axios';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  isProblemDetail,
  toProblemMessage,
} from '../../../shared/api/problem-details';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  useCreateInstitutionalBlockMutation,
  useDeleteInstitutionalBlockMutation,
  useInstitutionalBlocksQuery,
  useInstitutionalScheduleQuery,
  useUpdateInstitutionalBlockMutation,
  useUpdateInstitutionalScheduleMutation,
} from '../hooks/useInstitutionalData';
import type {
  InstitutionalBlockRule,
  InstitutionalBlockRuleRequest,
  UpdateInstitutionalScheduleRequest,
} from '../types/institutional.types';

type BlockForm = {
  name: string;
  category: string;
  ruleType: 'WEEKLY' | 'DATE_RANGE';
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  startDate: string;
  endDate: string;
  active: boolean;
};

function errorToMessage(error: unknown): string {
  if (!error) {
    return 'Ocurrio un error inesperado.';
  }
  if (error instanceof AxiosError && isProblemDetail(error.response?.data)) {
    return toProblemMessage(error.response.data as ProblemDetail);
  }
  return 'No se pudo completar la solicitud.';
}

function toBlockPayload(form: BlockForm): InstitutionalBlockRuleRequest {
  return {
    name: form.name.trim(),
    category: form.category.trim() || undefined,
    ruleType: form.ruleType,
    dayOfWeek: form.ruleType === 'WEEKLY' ? form.dayOfWeek : undefined,
    startTime: form.startTime,
    endTime: form.endTime,
    startDate: form.ruleType === 'DATE_RANGE' ? form.startDate : undefined,
    endDate: form.ruleType === 'DATE_RANGE' ? form.endDate : undefined,
    active: form.active,
  };
}

function fromBlockRule(rule: InstitutionalBlockRule): BlockForm {
  return {
    name: rule.name,
    category: rule.category,
    ruleType: rule.ruleType,
    dayOfWeek: rule.dayOfWeek ?? 1,
    startTime: rule.startTime,
    endTime: rule.endTime,
    startDate: rule.startDate ?? '',
    endDate: rule.endDate ?? '',
    active: rule.active,
  };
}

const INITIAL_BLOCK_FORM: BlockForm = {
  name: '',
  category: 'OTHER',
  ruleType: 'WEEKLY',
  dayOfWeek: 1,
  startTime: '10:15',
  endTime: '10:30',
  startDate: '',
  endDate: '',
  active: true,
};

const WEEK_DAYS = [1, 2, 3, 4, 5, 6, 7];

export function InstitutionalSchedulePage() {
  const scheduleQuery = useInstitutionalScheduleQuery();
  const blocksQuery = useInstitutionalBlocksQuery();
  const updateScheduleMutation = useUpdateInstitutionalScheduleMutation();
  const createBlockMutation = useCreateInstitutionalBlockMutation();
  const updateBlockMutation = useUpdateInstitutionalBlockMutation();
  const deleteBlockMutation = useDeleteInstitutionalBlockMutation();

  const [scheduleForm, setScheduleForm] = useState<UpdateInstitutionalScheduleRequest>({
    dayStart: '08:00',
    dayEnd: '17:00',
    slotMinutes: 45,
  });
  const [editingBlockId, setEditingBlockId] = useState<string | null>(null);
  const [blockForm, setBlockForm] = useState<BlockForm>(INITIAL_BLOCK_FORM);
  const [applyWholeWeek, setApplyWholeWeek] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (scheduleQuery.data) {
      setScheduleForm({
        dayStart: scheduleQuery.data.dayStart,
        dayEnd: scheduleQuery.data.dayEnd,
        slotMinutes: scheduleQuery.data.slotMinutes,
      });
    }
  }, [scheduleQuery.data]);

  const isSaving =
    updateScheduleMutation.isPending ||
    createBlockMutation.isPending ||
    updateBlockMutation.isPending ||
    deleteBlockMutation.isPending;

  const editingBlock = useMemo(
    () => blocksQuery.data?.find((item) => item.id === editingBlockId) ?? null,
    [blocksQuery.data, editingBlockId],
  );

  const onSaveSchedule = async () => {
    setFormError(null);
    setSuccessMessage(null);

    if (!scheduleForm.dayStart || !scheduleForm.dayEnd || scheduleForm.slotMinutes <= 0) {
      setFormError('Completa horario institucional y duracion de sesion.');
      return;
    }

    try {
      await updateScheduleMutation.mutateAsync(scheduleForm);
      setSuccessMessage('Horario institucional actualizado.');
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const resetBlockForm = () => {
    setEditingBlockId(null);
    setBlockForm(INITIAL_BLOCK_FORM);
    setApplyWholeWeek(false);
  };

  const onEditBlock = (rule: InstitutionalBlockRule) => {
    setFormError(null);
    setSuccessMessage(null);
    setEditingBlockId(rule.id);
    setBlockForm(fromBlockRule(rule));
    setApplyWholeWeek(false);
  };

  const onSaveBlock = async () => {
    setFormError(null);
    setSuccessMessage(null);

    if (!blockForm.name.trim()) {
      setFormError('El nombre del bloqueo es obligatorio.');
      return;
    }
    if (blockForm.startTime >= blockForm.endTime) {
      setFormError('El inicio debe ser menor que el fin.');
      return;
    }
    if (blockForm.ruleType === 'DATE_RANGE' && (!blockForm.startDate || !blockForm.endDate)) {
      setFormError('Para DATE_RANGE debes indicar fecha inicio y fin.');
      return;
    }

    try {
      if (!editingBlockId) {
        if (blockForm.ruleType === 'WEEKLY' && applyWholeWeek) {
          for (const dayOfWeek of WEEK_DAYS) {
            await createBlockMutation.mutateAsync(
              toBlockPayload({
                ...blockForm,
                dayOfWeek,
              }),
            );
          }
          setSuccessMessage('Bloqueo institucional creado para toda la semana.');
          resetBlockForm();
          return;
        }

        await createBlockMutation.mutateAsync(toBlockPayload(blockForm));
        setSuccessMessage('Bloqueo institucional creado.');
        resetBlockForm();
        return;
      }

      await updateBlockMutation.mutateAsync({
        blockId: editingBlockId,
        payload: toBlockPayload(blockForm),
      });
      setSuccessMessage('Bloqueo institucional actualizado.');
      resetBlockForm();
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onDeleteBlock = async (blockId: string) => {
    setFormError(null);
    setSuccessMessage(null);

    try {
      await deleteBlockMutation.mutateAsync({ blockId });
      setSuccessMessage('Bloqueo institucional eliminado.');
      if (editingBlockId === blockId) {
        resetBlockForm();
      }
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  return (
    <main className="page">
      <div className="page-header">
        <h1>Configuracion institucional</h1>
        <div className="actions">
          <Link to="/booking">Ir a reservas</Link>
          <Link to="/calendar">Calendario general</Link>
          <Link to="/availability">Disponibilidad profesional</Link>
        </div>
      </div>
      <p className="subtitle">
        Gestiona el horario base y bloqueos generales (pausas, almuerzo, reuniones).
      </p>

      {formError && <p className="error">{formError}</p>}
      {successMessage && <p className="success">{successMessage}</p>}

      <section className="card">
        <h2>Horario base</h2>
        {scheduleQuery.isLoading ? (
          <p>Cargando configuracion institucional...</p>
        ) : scheduleQuery.error ? (
          <p className="error">{errorToMessage(scheduleQuery.error)}</p>
        ) : (
          <>
            <div className="grid">
              <label>
                Inicio de jornada
                <input
                  type="time"
                  value={scheduleForm.dayStart}
                  onChange={(event) =>
                    setScheduleForm((current) => ({ ...current, dayStart: event.target.value }))
                  }
                />
              </label>
              <label>
                Fin de jornada
                <input
                  type="time"
                  value={scheduleForm.dayEnd}
                  onChange={(event) =>
                    setScheduleForm((current) => ({ ...current, dayEnd: event.target.value }))
                  }
                />
              </label>
              <label>
                Duracion de sesion (min)
                <input
                  type="number"
                  min={5}
                  value={scheduleForm.slotMinutes}
                  onChange={(event) =>
                    setScheduleForm((current) => ({
                      ...current,
                      slotMinutes: Number(event.target.value),
                    }))
                  }
                />
              </label>
            </div>
            <button type="button" onClick={onSaveSchedule} disabled={isSaving}>
              Guardar horario base
            </button>
          </>
        )}
      </section>

      <section className="card">
        <h2>Bloqueos institucionales</h2>
        {blocksQuery.isLoading ? (
          <p>Cargando bloqueos...</p>
        ) : blocksQuery.error ? (
          <p className="error">{errorToMessage(blocksQuery.error)}</p>
        ) : (blocksQuery.data?.length ?? 0) === 0 ? (
          <p>No hay bloqueos institucionales.</p>
        ) : (
          <div className="table-like">
            <div className="table-row table-row-5 table-header-row">
              <span>Nombre</span>
              <span>Tipo</span>
              <span>Horario</span>
              <span>Estado</span>
              <span>Acciones</span>
            </div>
            {blocksQuery.data?.map((rule) => (
              <div key={rule.id} className="table-row table-row-5">
                <span>{rule.name}</span>
                <span>{rule.ruleType}</span>
                <span>
                  {rule.startTime} - {rule.endTime}
                </span>
                <span>{rule.active ? 'Activo' : 'Inactivo'}</span>
                <div className="actions">
                  <button type="button" onClick={() => onEditBlock(rule)}>
                    Editar
                  </button>
                  <button
                    type="button"
                    onClick={() => onDeleteBlock(rule.id)}
                    disabled={deleteBlockMutation.isPending}
                  >
                    Eliminar
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        <h3>{editingBlock ? `Editar bloqueo: ${editingBlock.name}` : 'Nuevo bloqueo'}</h3>
        <div className="grid">
          <label>
            Nombre
            <input
              type="text"
              value={blockForm.name}
              onChange={(event) =>
                setBlockForm((current) => ({ ...current, name: event.target.value }))
              }
            />
          </label>
          <label>
            Categoria
            <input
              type="text"
              value={blockForm.category}
              onChange={(event) =>
                setBlockForm((current) => ({ ...current, category: event.target.value }))
              }
              placeholder="HEALTH_BREAK, LUNCH, MEETING, OTHER"
            />
          </label>
          <label>
            Tipo regla
            <select
              value={blockForm.ruleType}
              onChange={(event) =>
                setBlockForm((current) => ({
                  ...current,
                  ruleType: event.target.value as 'WEEKLY' | 'DATE_RANGE',
                }))
              }
            >
              <option value="WEEKLY">WEEKLY</option>
              <option value="DATE_RANGE">DATE_RANGE</option>
            </select>
          </label>
          {blockForm.ruleType === 'WEEKLY' ? (
            <>
              <label>
                Dia de semana
                <select
                  value={blockForm.dayOfWeek}
                  onChange={(event) =>
                    setBlockForm((current) => ({ ...current, dayOfWeek: Number(event.target.value) }))
                  }
                  disabled={applyWholeWeek && !editingBlock}
                >
                  {[1, 2, 3, 4, 5, 6, 7].map((day) => (
                    <option key={day} value={day}>
                      {day}
                    </option>
                  ))}
                </select>
              </label>
              {!editingBlockId && (
                <label>
                  Aplicar a semana completa
                  <input
                    type="checkbox"
                    checked={applyWholeWeek}
                    onChange={(event) => setApplyWholeWeek(event.target.checked)}
                  />
                </label>
              )}
            </>
          ) : (
            <>
              <label>
                Fecha inicio
                <input
                  type="date"
                  value={blockForm.startDate}
                  onChange={(event) =>
                    setBlockForm((current) => ({ ...current, startDate: event.target.value }))
                  }
                />
              </label>
              <label>
                Fecha fin
                <input
                  type="date"
                  value={blockForm.endDate}
                  onChange={(event) =>
                    setBlockForm((current) => ({ ...current, endDate: event.target.value }))
                  }
                />
              </label>
            </>
          )}
          <label>
            Inicio bloqueo
            <input
              type="time"
              value={blockForm.startTime}
              onChange={(event) =>
                setBlockForm((current) => ({ ...current, startTime: event.target.value }))
              }
            />
          </label>
          <label>
            Fin bloqueo
            <input
              type="time"
              value={blockForm.endTime}
              onChange={(event) =>
                setBlockForm((current) => ({ ...current, endTime: event.target.value }))
              }
            />
          </label>
          <label>
            Activo
            <select
              value={blockForm.active ? 'true' : 'false'}
              onChange={(event) =>
                setBlockForm((current) => ({
                  ...current,
                  active: event.target.value === 'true',
                }))
              }
            >
              <option value="true">Activo</option>
              <option value="false">Inactivo</option>
            </select>
          </label>
        </div>

        <div className="actions">
          <button type="button" onClick={onSaveBlock} disabled={isSaving}>
            {editingBlock ? 'Guardar cambios' : 'Crear bloqueo'}
          </button>
          {editingBlock && (
            <button type="button" onClick={resetBlockForm} disabled={isSaving}>
              Cancelar edicion
            </button>
          )}
        </div>
      </section>
    </main>
  );
}

