import { apiClient } from '../../../shared/api/client';
import type {
  InstitutionalBlockRule,
  InstitutionalBlockRuleRequest,
  InstitutionalSchedule,
  UpdateInstitutionalScheduleRequest,
} from '../types/institutional.types';

export async function getInstitutionalSchedule(): Promise<InstitutionalSchedule> {
  const response = await apiClient.get<InstitutionalSchedule>('/institutional-schedule');
  return response.data;
}

export async function updateInstitutionalSchedule(
  payload: UpdateInstitutionalScheduleRequest,
): Promise<InstitutionalSchedule> {
  const response = await apiClient.put<InstitutionalSchedule>('/institutional-schedule', payload);
  return response.data;
}

export async function listInstitutionalBlocks(active?: boolean): Promise<InstitutionalBlockRule[]> {
  const response = await apiClient.get<InstitutionalBlockRule[]>('/institutional-schedule/blocks', {
    params: active === undefined ? undefined : { active },
  });
  return response.data;
}

export async function createInstitutionalBlock(
  payload: InstitutionalBlockRuleRequest,
): Promise<InstitutionalBlockRule> {
  const response = await apiClient.post<InstitutionalBlockRule>('/institutional-schedule/blocks', payload);
  return response.data;
}

export async function updateInstitutionalBlock(
  blockId: string,
  payload: InstitutionalBlockRuleRequest,
): Promise<InstitutionalBlockRule> {
  const response = await apiClient.patch<InstitutionalBlockRule>(`/institutional-schedule/blocks/${blockId}`, payload);
  return response.data;
}

export async function deleteInstitutionalBlock(blockId: string): Promise<void> {
  await apiClient.delete(`/institutional-schedule/blocks/${blockId}`);
}

