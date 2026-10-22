export type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
};

const PROBLEM_CODE_MESSAGES_ES: Record<string, string> = {
  VALIDATION_ERROR: 'Datos invalidos. Revisa los campos ingresados.',
  NOT_FOUND: 'No se encontro el recurso solicitado.',
  SCHEDULE_CONFLICT: 'Conflicto de agenda. El bloque ya no esta disponible.',
  OUTSIDE_WORKING_HOURS: 'El horario seleccionado esta fuera de jornada laboral.',
  INVALID_STATUS_TRANSITION: 'La transicion de estado no esta permitida.',
};

export function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record.title === 'string' ||
    typeof record.detail === 'string' ||
    typeof record.code === 'string'
  );
}

export function toProblemMessage(problem: ProblemDetail): string {
  const code = problem.code ?? 'ERROR';
  const friendlyMessage = PROBLEM_CODE_MESSAGES_ES[code] ?? 'Ocurrio un error inesperado.';
  return problem.detail ? `${friendlyMessage} (${problem.detail})` : friendlyMessage;
}

