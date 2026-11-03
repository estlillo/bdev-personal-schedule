import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  createProfessional,
  getProfessionals,
  updateProfessional,
} from '../api/professionals.api';
import type {
  CreateProfessionalRequest,
  Professional,
  UpdateProfessionalRequest,
} from '../types/professionals.types';

export function useProfessionalsQuery(activeFilter: 'all' | 'active' | 'inactive') {
  return useQuery({
    queryKey: ['professionals', activeFilter],
    queryFn: () => {
      if (activeFilter === 'all') {
        return getProfessionals();
      }
      return getProfessionals(activeFilter === 'active');
    },
  });
}

export function useCreateProfessionalMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    Professional,
    AxiosError<ProblemDetail>,
    CreateProfessionalRequest
  >({
    mutationFn: createProfessional,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['professionals'] });
    },
  });
}

export function useUpdateProfessionalMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    Professional,
    AxiosError<ProblemDetail>,
    { professionalId: string; payload: UpdateProfessionalRequest }
  >({
    mutationFn: ({ professionalId, payload }) =>
      updateProfessional(professionalId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['professionals'] });
    },
  });
}

