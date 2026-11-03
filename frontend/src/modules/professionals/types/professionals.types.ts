export type Professional = {
  id: string;
  displayName: string;
  specialty: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateProfessionalRequest = {
  displayName: string;
  specialty?: string;
};

export type UpdateProfessionalRequest = {
  displayName?: string;
  specialty?: string;
  active?: boolean;
};

