import { apiClient } from '../../../shared/api/client';
import type {
  CreateReservationRequest,
  Reservation,
  UpdateReservationMetadataRequest,
} from '../types/booking.types';

type ListReservationsParams = {
  from: string;
  to: string;
  status?: 'HOLD' | 'CONFIRMED' | 'CANCELLED';
};

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

export async function listReservations(
  params: ListReservationsParams,
): Promise<Reservation[]> {
  const response = await apiClient.get<Reservation[]>('/reservations', {
    params,
  });
  return response.data;
}

export async function updateReservationMetadata(
  reservationId: string,
  payload: UpdateReservationMetadataRequest,
): Promise<Reservation> {
  const response = await apiClient.patch<Reservation>(`/reservations/${reservationId}`, payload);
  return response.data;
}

