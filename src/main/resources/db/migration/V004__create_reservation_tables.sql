CREATE TABLE IF NOT EXISTS reservation (
    id TEXT PRIMARY KEY,
    starts_at TEXT NOT NULL,
    ends_at TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('HOLD', 'CONFIRMED', 'CANCELLED')),
    reference_code TEXT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (starts_at < ends_at)
);

CREATE TABLE IF NOT EXISTS reservation_professional (
    reservation_id TEXT NOT NULL,
    professional_id TEXT NOT NULL,
    PRIMARY KEY (reservation_id, professional_id),
    FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE CASCADE,
    FOREIGN KEY (professional_id) REFERENCES professional(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reservation_starts_ends
    ON reservation(starts_at, ends_at);

CREATE INDEX IF NOT EXISTS idx_reservation_professional_prof_res
    ON reservation_professional(professional_id, reservation_id);

