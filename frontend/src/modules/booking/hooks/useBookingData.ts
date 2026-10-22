import { useMutation, useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { searchAvailability } from '../api/availability.api';
import { fetchActiveProfessionals } from '../api/professionals.api';
import {
  createReservation,
  updateReservationStatus,
} from '../api/reservations.api';
import type {
  AvailabilitySearchRequest,
  AvailabilitySearchResponse,
  CreateReservationRequest,
  Reservation,
} from '../types/booking.types';
import type { ProblemDetail } from '../../../shared/api/problem-details';

export function useActiveProfessionalsQuery() {
  return useQuery({
    queryKey: ['professionals', 'active'],
    queryFn: fetchActiveProfessionals,
  });
}

export function useAvailabilitySearchMutation() {
  return useMutation<
    AvailabilitySearchResponse,
    AxiosError<ProblemDetail>,
    AvailabilitySearchRequest
  >({
    mutationFn: searchAvailability,
  });
}

export function useCreateReservationMutation() {
  return useMutation<
    Reservation,
    AxiosError<ProblemDetail>,
    CreateReservationRequest
  >({
    mutationFn: createReservation,
  });
}

export function useUpdateReservationStatusMutation() {
  return useMutation<
    Reservation,
    AxiosError<ProblemDetail>,
    { reservationId: string; status: 'CONFIRMED' | 'CANCELLED' }
  >({
    mutationFn: ({ reservationId, status }) =>
      updateReservationStatus(reservationId, status),
  });
}
