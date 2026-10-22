import { apiClient } from '../../../shared/api/client';
import type {
  AvailabilitySearchRequest,
  AvailabilitySearchResponse,
} from '../types/booking.types';

export async function searchAvailability(
  payload: AvailabilitySearchRequest,
): Promise<AvailabilitySearchResponse> {
  const response = await apiClient.post<AvailabilitySearchResponse>(
    '/availability/search',
    payload,
  );
  return response.data;
}
