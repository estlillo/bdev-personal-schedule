import { apiClient } from '../../../shared/api/client';
import type {
  CreateScheduleExceptionRequest,
  ReplaceWorkingWindowsRequest,
  ScheduleException,
  WorkingWindowsResponse,
} from '../types/schedule.types';

export async function getWorkingWindows(
  professionalId: string,
): Promise<WorkingWindowsResponse> {
  const response = await apiClient.get<WorkingWindowsResponse>(
    `/professionals/${professionalId}/working-windows`,
  );
  return response.data;
}

export async function replaceWorkingWindows(
  professionalId: string,
  payload: ReplaceWorkingWindowsRequest,
): Promise<WorkingWindowsResponse> {
  const response = await apiClient.put<WorkingWindowsResponse>(
    `/professionals/${professionalId}/working-windows`,
    payload,
  );
  return response.data;
}

export async function getScheduleExceptions(
  professionalId: string,
  from: string,
  to: string,
): Promise<ScheduleException[]> {
  const response = await apiClient.get<ScheduleException[]>(
    `/professionals/${professionalId}/exceptions`,
    {
      params: { from, to },
    },
  );
  return response.data;
}

export async function createScheduleException(
  professionalId: string,
  payload: CreateScheduleExceptionRequest,
): Promise<ScheduleException> {
  const response = await apiClient.post<ScheduleException>(
    `/professionals/${professionalId}/exceptions`,
    payload,
  );
  return response.data;
}

export async function deleteScheduleException(
  professionalId: string,
  exceptionId: string,
): Promise<void> {
  await apiClient.delete(
    `/professionals/${professionalId}/exceptions/${exceptionId}`,
  );
}

