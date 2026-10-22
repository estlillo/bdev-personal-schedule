package com.barbatosdev.bdevpersonalschedule.calendar.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCalendarEventRequest(
    UUID professionalId,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    @Pattern(regexp = "MEETING|WORKSHOP|OTHER") String category,
    @Size(max = 255) String note
) {
}

