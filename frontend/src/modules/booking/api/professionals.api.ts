import { apiClient } from '../../../shared/api/client';
import type { Professional } from '../types/booking.types';

export async function fetchProfessionals(active?: boolean): Promise<Professional[]> {
  const response = await apiClient.get<Professional[]>('/professionals', {
    params: active === undefined ? undefined : { active },
  });
  return response.data;
}

export async function fetchActiveProfessionals(): Promise<Professional[]> {
  return fetchProfessionals(true);
}

