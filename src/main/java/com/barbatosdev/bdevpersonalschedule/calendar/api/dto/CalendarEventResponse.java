package com.barbatosdev.bdevpersonalschedule.calendar.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CalendarEventResponse(
    UUID id,
    UUID professionalId,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String category,
    String note
) {
}

