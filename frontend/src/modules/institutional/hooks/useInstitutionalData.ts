import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import type { ProblemDetail } from '../../../shared/api/problem-details';
import {
  createInstitutionalBlock,
  deleteInstitutionalBlock,
  getInstitutionalSchedule,
  listInstitutionalBlocks,
  updateInstitutionalBlock,
  updateInstitutionalSchedule,
} from '../api/institutional.api';
import type {
  InstitutionalBlockRule,
  InstitutionalBlockRuleRequest,
  InstitutionalSchedule,
  UpdateInstitutionalScheduleRequest,
} from '../types/institutional.types';

export function useInstitutionalScheduleQuery() {
  return useQuery({
    queryKey: ['institutional', 'schedule'],
    queryFn: getInstitutionalSchedule,
  });
}

export function useInstitutionalBlocksQuery(active?: boolean) {
  return useQuery({
    queryKey: ['institutional', 'blocks', active ?? 'all'],
    queryFn: () => listInstitutionalBlocks(active),
  });
}

export function useUpdateInstitutionalScheduleMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    InstitutionalSchedule,
    AxiosError<ProblemDetail>,
    UpdateInstitutionalScheduleRequest
  >({
    mutationFn: updateInstitutionalSchedule,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['institutional', 'schedule'] });
    },
  });
}

export function useCreateInstitutionalBlockMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    InstitutionalBlockRule,
    AxiosError<ProblemDetail>,
    InstitutionalBlockRuleRequest
  >({
    mutationFn: createInstitutionalBlock,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['institutional', 'blocks'] });
    },
  });
}

export function useUpdateInstitutionalBlockMutation() {
  const queryClient = useQueryClient();
  return useMutation<
    InstitutionalBlockRule,
    AxiosError<ProblemDetail>,
    { blockId: string; payload: InstitutionalBlockRuleRequest }
  >({
    mutationFn: ({ blockId, payload }) => updateInstitutionalBlock(blockId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['institutional', 'blocks'] });
    },
  });
}

export function useDeleteInstitutionalBlockMutation() {
  const queryClient = useQueryClient();
  return useMutation<void, AxiosError<ProblemDetail>, { blockId: string }>({
    mutationFn: ({ blockId }) => deleteInstitutionalBlock(blockId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['institutional', 'blocks'] });
    },
  });
}

