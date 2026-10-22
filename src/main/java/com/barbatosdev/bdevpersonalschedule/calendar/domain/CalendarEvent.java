package com.barbatosdev.bdevpersonalschedule.calendar.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarEvent(
    UUID id,
    UUID professionalId,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String category,
    String note
) {
}

