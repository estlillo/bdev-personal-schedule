export type Professional = {
  id: string;
  displayName: string;
  specialty: string | null;
  active: boolean;
};

export type AvailabilitySlot = {
  startsAt: string;
  endsAt: string;
};

export type AvailabilitySearchRequest = {
  professionalIds: string[];
  startDate: string;
  endDate: string;
  durationMinutes: number;
  stepMinutes?: number;
};

export type AvailabilitySearchResponse = {
  slots: AvailabilitySlot[];
  asOf: string;
  warning: string;
};

export type CreateReservationRequest = {
  professionalIds: string[];
  startsAt: string;
  endsAt: string;
  referenceCode?: string;
  description?: string;
};

export type Reservation = {
  id: string;
  professionalIds: string[];
  startsAt: string;
  endsAt: string;
  status: 'HOLD' | 'CONFIRMED' | 'CANCELLED';
  referenceCode: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
};

