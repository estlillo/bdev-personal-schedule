import { apiClient } from '../../../shared/api/client';
import type { Professional } from '../types/booking.types';

export async function fetchActiveProfessionals(): Promise<Professional[]> {
  const response = await apiClient.get<Professional[]>('/professionals', {
    params: { active: true },
  });
  return response.data;
}
