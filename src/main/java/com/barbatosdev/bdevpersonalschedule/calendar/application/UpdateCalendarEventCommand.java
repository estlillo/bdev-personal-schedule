package com.barbatosdev.bdevpersonalschedule.calendar.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCalendarEventCommand(
    UUID professionalId,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String category,
    String note
) {
}

