import { apiClient } from '../../../shared/api/client';
import type {
  CreateReservationRequest,
  Reservation,
} from '../types/booking.types';

export async function createReservation(
  payload: CreateReservationRequest,
): Promise<Reservation> {
  const response = await apiClient.post<Reservation>('/reservations', payload);
  return response.data;
}

export async function updateReservationStatus(
  reservationId: string,
  status: 'CONFIRMED' | 'CANCELLED',
): Promise<Reservation> {
  const response = await apiClient.patch<Reservation>(
    `/reservations/${reservationId}/status`,
    { status },
  );
  return response.data;
}
