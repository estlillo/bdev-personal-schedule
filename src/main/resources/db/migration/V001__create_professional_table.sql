CREATE TABLE IF NOT EXISTS professional (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    specialty TEXT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (length(trim(display_name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_professional_active ON professional(active);

