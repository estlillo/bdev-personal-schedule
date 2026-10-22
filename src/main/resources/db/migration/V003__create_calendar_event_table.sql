CREATE TABLE IF NOT EXISTS calendar_event (
    id TEXT PRIMARY KEY,
    professional_id TEXT NOT NULL,
    starts_at TEXT NOT NULL,
    ends_at TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('MEETING', 'WORKSHOP', 'OTHER')),
    note TEXT NULL,
    FOREIGN KEY (professional_id) REFERENCES professional(id) ON DELETE CASCADE,
    CHECK (starts_at < ends_at)
);

CREATE INDEX IF NOT EXISTS idx_calendar_event_prof_starts_ends
    ON calendar_event(professional_id, starts_at, ends_at);

