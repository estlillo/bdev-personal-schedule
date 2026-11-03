# 02 — Domain model

## Tables

### professional
- id UUID (TEXT primary key)
- display_name TEXT NOT NULL
- specialty TEXT NULL
- active BOOLEAN NOT NULL DEFAULT true
- created_at, updated_at DATETIME NOT NULL

### working_window
- id UUID (TEXT primary key)
- professional_id FK professional NOT NULL
- day_of_week INTEGER NOT NULL (1=Monday .. 7=Sunday)
- start_time TIME NOT NULL
- end_time TIME NOT NULL
- CHECK start_time < end_time

### schedule_exception
- id UUID (TEXT primary key)
- professional_id FK professional NOT NULL
- date DATE NOT NULL
- start_time TIME NOT NULL
- end_time TIME NOT NULL
- mode TEXT NOT NULL (BLOCK, ADD)
- reason TEXT NULL (generic, no clinical details)
- CHECK start_time < end_time

### calendar_event
- id UUID (TEXT primary key)
- professional_id FK professional NOT NULL
- starts_at DATETIME NOT NULL
- ends_at DATETIME NOT NULL
- category TEXT NOT NULL (MEETING, WORKSHOP, OTHER)
- note TEXT NULL (no patient-identifying information)
- CHECK starts_at < ends_at

### reservation
- id UUID (TEXT primary key)
- starts_at DATETIME NOT NULL
- ends_at DATETIME NOT NULL
- status TEXT NOT NULL (HOLD, CONFIRMED, CANCELLED)
- reference_code TEXT NULL (opaque, non-identifying)
- description TEXT NULL (short non-identifying note)
- created_at, updated_at DATETIME NOT NULL
- CHECK starts_at < ends_at

### reservation_professional
- reservation_id FK reservation NOT NULL
- professional_id FK professional NOT NULL
- PRIMARY KEY (reservation_id, professional_id)

## Constraints
- Enable PRAGMA foreign_keys=ON on every SQLite connection.
- Index calendar_event(professional_id, starts_at, ends_at), working_window(professional_id, day_of_week), schedule_exception(professional_id, date), reservation(starts_at, ends_at), reservation_professional(professional_id, reservation_id).
- Never use patient-identifying details in free-text fields.
- Use application-level overlap validation inside serialized SQLite write transactions (BEGIN IMMEDIATE or equivalent), including calendar_event and non-cancelled reservation intervals.
- Avoid relying on Java synchronized locks alone for integrity.
