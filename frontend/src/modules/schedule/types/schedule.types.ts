export type WorkingWindowResponse = {
  id: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
};

export type WorkingWindowsResponse = {
  windows: WorkingWindowResponse[];
};

export type WorkingWindowRequest = {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
};

export type ReplaceWorkingWindowsRequest = {
  windows: WorkingWindowRequest[];
};

export type ScheduleException = {
  id: string;
  date: string;
  startTime: string;
  endTime: string;
  mode: 'BLOCK' | 'ADD';
  reason: string | null;
};

export type CreateScheduleExceptionRequest = {
  date: string;
  startTime: string;
  endTime: string;
  mode: 'BLOCK' | 'ADD';
  reason?: string;
};

