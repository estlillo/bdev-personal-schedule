CREATE TABLE IF NOT EXISTS institutional_schedule_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    day_start TEXT NOT NULL,
    day_end TEXT NOT NULL,
    slot_minutes INTEGER NOT NULL CHECK (slot_minutes > 0),
    updated_at TEXT NOT NULL,
    CHECK (day_start < day_end)
);

CREATE TABLE IF NOT EXISTS institutional_block_rule (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    rule_type TEXT NOT NULL CHECK (rule_type IN ('WEEKLY', 'DATE_RANGE')),
    day_of_week INTEGER NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    start_date TEXT NULL,
    end_date TEXT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (start_time < end_time),
    CHECK (
        (rule_type = 'WEEKLY' AND day_of_week BETWEEN 1 AND 7 AND start_date IS NULL AND end_date IS NULL) OR
        (rule_type = 'DATE_RANGE' AND day_of_week IS NULL AND start_date IS NOT NULL AND end_date IS NOT NULL AND start_date <= end_date)
    )
);

CREATE INDEX IF NOT EXISTS idx_institutional_block_rule_active_type_day
    ON institutional_block_rule(active, rule_type, day_of_week);

CREATE INDEX IF NOT EXISTS idx_institutional_block_rule_active_dates
    ON institutional_block_rule(active, start_date, end_date);

INSERT INTO institutional_schedule_config (id, day_start, day_end, slot_minutes, updated_at)
SELECT 1, '08:00', '17:00', 45, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_schedule_config WHERE id = 1);

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '11111111-2222-3333-4444-555555555555',
    'Pausa saludable',
    'HEALTH_BREAK',
    'WEEKLY',
    1,
    '10:15',
    '10:30',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '11111111-2222-3333-4444-555555555555');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '66666666-7777-8888-9999-aaaaaaaaaaaa',
    'Hora de colacion',
    'LUNCH',
    'WEEKLY',
    1,
    '13:00',
    '14:00',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '66666666-7777-8888-9999-aaaaaaaaaaaa');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '11111111-2222-3333-4444-555555555556',
    'Pausa saludable',
    'HEALTH_BREAK',
    'WEEKLY',
    2,
    '10:15',
    '10:30',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '11111111-2222-3333-4444-555555555556');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '66666666-7777-8888-9999-aaaaaaaaaaab',
    'Hora de colacion',
    'LUNCH',
    'WEEKLY',
    2,
    '13:00',
    '14:00',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '66666666-7777-8888-9999-aaaaaaaaaaab');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '11111111-2222-3333-4444-555555555557',
    'Pausa saludable',
    'HEALTH_BREAK',
    'WEEKLY',
    3,
    '10:15',
    '10:30',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '11111111-2222-3333-4444-555555555557');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '66666666-7777-8888-9999-aaaaaaaaaaac',
    'Hora de colacion',
    'LUNCH',
    'WEEKLY',
    3,
    '13:00',
    '14:00',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '66666666-7777-8888-9999-aaaaaaaaaaac');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '11111111-2222-3333-4444-555555555558',
    'Pausa saludable',
    'HEALTH_BREAK',
    'WEEKLY',
    4,
    '10:15',
    '10:30',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '11111111-2222-3333-4444-555555555558');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '66666666-7777-8888-9999-aaaaaaaaaaad',
    'Hora de colacion',
    'LUNCH',
    'WEEKLY',
    4,
    '13:00',
    '14:00',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '66666666-7777-8888-9999-aaaaaaaaaaad');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '11111111-2222-3333-4444-555555555559',
    'Pausa saludable',
    'HEALTH_BREAK',
    'WEEKLY',
    5,
    '10:15',
    '10:30',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '11111111-2222-3333-4444-555555555559');

INSERT INTO institutional_block_rule (
    id, name, category, rule_type, day_of_week, start_time, end_time, start_date, end_date, active, created_at, updated_at
)
SELECT
    '66666666-7777-8888-9999-aaaaaaaaaaae',
    'Hora de colacion',
    'LUNCH',
    'WEEKLY',
    5,
    '13:00',
    '14:00',
    NULL,
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM institutional_block_rule WHERE id = '66666666-7777-8888-9999-aaaaaaaaaaae');

