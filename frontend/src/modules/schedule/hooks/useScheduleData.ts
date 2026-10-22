import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  createScheduleException,
  deleteScheduleException,
  getScheduleExceptions,
  getWorkingWindows,
  replaceWorkingWindows,
} from '../api/schedule.api';
import type {
  CreateScheduleExceptionRequest,
  ReplaceWorkingWindowsRequest,
  ScheduleException,
  WorkingWindowsResponse,
} from '../types/schedule.types';

export function useWorkingWindowsQuery(professionalId: string | null) {
  return useQuery({
    queryKey: ['working-windows', professionalId],
    queryFn: () => getWorkingWindows(professionalId as string),
    enabled: Boolean(professionalId),
  });
}

export function useScheduleExceptionsQuery(
  professionalId: string | null,
  from: string,
  to: string,
) {
  return useQuery({
    queryKey: ['schedule-exceptions', professionalId, from, to],
    queryFn: () => getScheduleExceptions(professionalId as string, from, to),
    enabled: Boolean(professionalId) && Boolean(from) && Boolean(to),
  });
}

export function useReplaceWorkingWindowsMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    WorkingWindowsResponse,
    AxiosError<ProblemDetail>,
    { professionalId: string; payload: ReplaceWorkingWindowsRequest }
  >({
    mutationFn: ({ professionalId, payload }) =>
      replaceWorkingWindows(professionalId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['working-windows', variables.professionalId],
      });
    },
  });
}

export function useCreateScheduleExceptionMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    ScheduleException,
    AxiosError<ProblemDetail>,
    { professionalId: string; payload: CreateScheduleExceptionRequest }
  >({
    mutationFn: ({ professionalId, payload }) =>
      createScheduleException(professionalId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['schedule-exceptions', variables.professionalId],
      });
    },
  });
}

export function useDeleteScheduleExceptionMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    void,
    AxiosError<ProblemDetail>,
    { professionalId: string; exceptionId: string }
  >({
    mutationFn: ({ professionalId, exceptionId }) =>
      deleteScheduleException(professionalId, exceptionId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['schedule-exceptions', variables.professionalId],
      });
    },
  });
}

