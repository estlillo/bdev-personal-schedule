import { useMutation, useQuery } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { searchAvailability } from '../api/availability.api';
import {
  fetchActiveProfessionals,
  fetchProfessionals,
} from '../api/professionals.api';
import {
  createBatchReservation,
  createReservation,
  listReservations,
  updateReservationMetadata,
  updateReservationStatus,
} from '../api/reservations.api';
import type {
  AvailabilitySearchRequest,
  AvailabilitySearchResponse,
  BatchReservationResponse,
  CreateReservationRequest,
  CreateBatchReservationRequest,
  Reservation,
  UpdateReservationMetadataRequest,
} from '../types/booking.types';
import type { ProblemDetail } from '../../../shared/api/problem-details';

export function useActiveProfessionalsQuery() {
  return useQuery({
    queryKey: ['professionals', 'active'],
    queryFn: fetchActiveProfessionals,
  });
}

export function useAllProfessionalsQuery() {
  return useQuery({
    queryKey: ['professionals', 'all'],
    queryFn: () => fetchProfessionals(),
  });
}

export function useReservationsByRangeQuery(from: string, to: string) {
  return useQuery({
    queryKey: ['reservations', from, to],
    queryFn: () => listReservations({ from, to }),
    enabled: from.length > 0 && to.length > 0,
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

export function useCreateBatchReservationMutation() {
  return useMutation<
    BatchReservationResponse,
    AxiosError<ProblemDetail>,
    CreateBatchReservationRequest
  >({
    mutationFn: createBatchReservation,
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

export function useUpdateReservationMetadataMutation() {
  return useMutation<
    Reservation,
    AxiosError<ProblemDetail>,
    { reservationId: string; payload: UpdateReservationMetadataRequest }
  >({
    mutationFn: ({ reservationId, payload }) =>
      updateReservationMetadata(reservationId, payload),
  });
}

