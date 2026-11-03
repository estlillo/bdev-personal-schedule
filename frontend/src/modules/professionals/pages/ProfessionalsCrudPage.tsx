import { AxiosError } from 'axios';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  isProblemDetail,
  toProblemMessage,
} from '../../../shared/api/problem-details';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  useCreateProfessionalMutation,
  useProfessionalsQuery,
  useUpdateProfessionalMutation,
} from '../hooks/useProfessionalsData';
import type { Professional } from '../types/professionals.types';

type ProfessionalForm = {
  displayName: string;
  specialty: string;
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

function toForm(professional: Professional): ProfessionalForm {
  return {
    displayName: professional.displayName,
    specialty: professional.specialty ?? '',
  };
}

export function ProfessionalsCrudPage() {
  const [activeFilter, setActiveFilter] = useState<'all' | 'active' | 'inactive'>(
    'all',
  );
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create');
  const [editingProfessionalId, setEditingProfessionalId] = useState<string | null>(null);
  const [form, setForm] = useState<ProfessionalForm>({
    displayName: '',
    specialty: '',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const professionalsQuery = useProfessionalsQuery(activeFilter);
  const createMutation = useCreateProfessionalMutation();
  const updateMutation = useUpdateProfessionalMutation();

  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  const editingProfessional = useMemo(() => {
    if (!editingProfessionalId || !professionalsQuery.data) {
      return null;
    }
    return (
      professionalsQuery.data.find(
        (professional) => professional.id === editingProfessionalId,
      ) ?? null
    );
  }, [editingProfessionalId, professionalsQuery.data]);

  const resetForm = () => {
    setFormMode('create');
    setEditingProfessionalId(null);
    setForm({ displayName: '', specialty: '' });
  };

  const onSelectEdit = (professional: Professional) => {
    setFormError(null);
    setSuccessMessage(null);
    setFormMode('edit');
    setEditingProfessionalId(professional.id);
    setForm(toForm(professional));
  };

  const onToggleActive = async (professional: Professional) => {
    setFormError(null);
    setSuccessMessage(null);

    try {
      await updateMutation.mutateAsync({
        professionalId: professional.id,
        payload: { active: !professional.active },
      });
      setSuccessMessage(
        professional.active
          ? 'Profesional desactivado correctamente.'
          : 'Profesional activado correctamente.',
      );
      if (editingProfessionalId === professional.id) {
        setEditingProfessionalId(null);
        setFormMode('create');
      }
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  const onSubmitForm = async () => {
    setFormError(null);
    setSuccessMessage(null);

    const displayName = form.displayName.trim();
    const specialty = form.specialty.trim();

    if (!displayName) {
      setFormError('El nombre visible es obligatorio.');
      return;
    }

    try {
      if (formMode === 'create') {
        await createMutation.mutateAsync({
          displayName,
          specialty: specialty || undefined,
        });
        setSuccessMessage('Profesional creado correctamente.');
        resetForm();
        return;
      }

      if (!editingProfessionalId) {
        setFormError('Selecciona un profesional a editar.');
        return;
      }

      await updateMutation.mutateAsync({
        professionalId: editingProfessionalId,
        payload: {
          displayName,
          specialty: specialty || undefined,
        },
      });
      setSuccessMessage('Profesional actualizado correctamente.');
    } catch (error) {
      setFormError(errorToMessage(error));
    }
  };

  return (
    <main className="page">
      <div className="page-header">
        <h1>Profesionales</h1>
        <div className="actions">
          <Link to="/booking">Ir a reservas</Link>
          <Link to="/calendar">Calendario general</Link>
          <Link to="/institutional-schedule">Configuracion institucional</Link>
          <Link to="/availability">Definir disponibilidad</Link>
        </div>
      </div>
      <p className="subtitle">Gestiona profesionales: crear, editar y activar/desactivar.</p>

      {formError && <p className="error">{formError}</p>}
      {successMessage && <p className="success">{successMessage}</p>}

      <section className="card">
        <h2>Formulario</h2>
        <div className="grid">
          <label>
            Nombre visible
            <input
              type="text"
              value={form.displayName}
              onChange={(event) =>
                setForm((current) => ({ ...current, displayName: event.target.value }))
              }
              placeholder="Ej: Profesional Kine A"
            />
          </label>
          <label>
            Especialidad
            <input
              type="text"
              value={form.specialty}
              onChange={(event) =>
                setForm((current) => ({ ...current, specialty: event.target.value }))
              }
              placeholder="Ej: KINE"
            />
          </label>
        </div>

        {formMode === 'edit' && editingProfessional && (
          <p className="subtitle">
            Editando: <strong>{editingProfessional.displayName}</strong>
          </p>
        )}

        <div className="actions">
          <button type="button" disabled={isSubmitting} onClick={onSubmitForm}>
            {isSubmitting
              ? 'Guardando...'
              : formMode === 'create'
                ? 'Crear profesional'
                : 'Guardar cambios'}
          </button>
          {formMode === 'edit' && (
            <button type="button" onClick={resetForm} disabled={isSubmitting}>
              Cancelar edicion
            </button>
          )}
        </div>
      </section>

      <section className="card">
        <h2>Listado</h2>
        <div className="actions">
          <label>
            Filtro
            <select
              value={activeFilter}
              onChange={(event) =>
                setActiveFilter(event.target.value as 'all' | 'active' | 'inactive')
              }
            >
              <option value="all">Todos</option>
              <option value="active">Solo activos</option>
              <option value="inactive">Solo inactivos</option>
            </select>
          </label>
        </div>

        {professionalsQuery.isLoading ? (
          <p>Cargando profesionales...</p>
        ) : professionalsQuery.error ? (
          <p className="error">{errorToMessage(professionalsQuery.error)}</p>
        ) : (professionalsQuery.data?.length ?? 0) === 0 ? (
          <p>No hay profesionales para el filtro seleccionado.</p>
        ) : (
          <div className="table-like">
            <div className="table-row table-row-5 table-header-row">
              <span>Nombre</span>
              <span>Especialidad</span>
              <span>Estado</span>
              <span>Actualizado</span>
              <span>Acciones</span>
            </div>
            {professionalsQuery.data?.map((professional) => (
              <div key={professional.id} className="table-row table-row-5">
                <span>{professional.displayName}</span>
                <span>{professional.specialty ?? '-'}</span>
                <span>{professional.active ? 'Activo' : 'Inactivo'}</span>
                <span>{professional.updatedAt.replace('T', ' ')}</span>
                <div className="actions">
                  <button type="button" onClick={() => onSelectEdit(professional)}>
                    Editar
                  </button>
                  <button
                    type="button"
                    onClick={() => onToggleActive(professional)}
                    disabled={updateMutation.isPending}
                  >
                    {professional.active ? 'Desactivar' : 'Activar'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

