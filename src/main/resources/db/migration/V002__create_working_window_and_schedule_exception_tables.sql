CREATE TABLE IF NOT EXISTS working_window (
    id TEXT PRIMARY KEY,
    professional_id TEXT NOT NULL,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    FOREIGN KEY (professional_id) REFERENCES professional(id) ON DELETE CASCADE,
    CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS schedule_exception (
    id TEXT PRIMARY KEY,
    professional_id TEXT NOT NULL,
    date TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    mode TEXT NOT NULL CHECK (mode IN ('BLOCK', 'ADD')),
    reason TEXT NULL,
    FOREIGN KEY (professional_id) REFERENCES professional(id) ON DELETE CASCADE,
    CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_working_window_prof_day
    ON working_window(professional_id, day_of_week);

CREATE INDEX IF NOT EXISTS idx_schedule_exception_prof_date
    ON schedule_exception(professional_id, date);

