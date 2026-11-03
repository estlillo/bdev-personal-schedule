import { apiClient } from '../../../shared/api/client';
import type {
  CreateProfessionalRequest,
  Professional,
  UpdateProfessionalRequest,
} from '../types/professionals.types';

export async function getProfessionals(active?: boolean): Promise<Professional[]> {
  const response = await apiClient.get<Professional[]>('/professionals', {
    params: active === undefined ? undefined : { active },
  });
  return response.data;
}

export async function createProfessional(
  payload: CreateProfessionalRequest,
): Promise<Professional> {
  const response = await apiClient.post<Professional>('/professionals', payload);
  return response.data;
}

export async function updateProfessional(
  professionalId: string,
  payload: UpdateProfessionalRequest,
): Promise<Professional> {
  const response = await apiClient.patch<Professional>(
    `/professionals/${professionalId}`,
    payload,
  );
  return response.data;
}

