export type InstitutionalSchedule = {
  dayStart: string;
  dayEnd: string;
  slotMinutes: number;
};

export type UpdateInstitutionalScheduleRequest = {
  dayStart: string;
  dayEnd: string;
  slotMinutes: number;
};

export type InstitutionalBlockRule = {
  id: string;
  name: string;
  category: string;
  ruleType: 'WEEKLY' | 'DATE_RANGE';
  dayOfWeek: number | null;
  startTime: string;
  endTime: string;
  startDate: string | null;
  endDate: string | null;
  active: boolean;
};

export type InstitutionalBlockRuleRequest = {
  name: string;
  category?: string;
  ruleType: 'WEEKLY' | 'DATE_RANGE';
  dayOfWeek?: number;
  startTime: string;
  endTime: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
};

