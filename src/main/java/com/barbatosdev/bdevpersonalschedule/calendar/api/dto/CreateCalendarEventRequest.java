package com.barbatosdev.bdevpersonalschedule.calendar.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCalendarEventRequest(
    @NotNull UUID professionalId,
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt,
    @NotNull @Pattern(regexp = "MEETING|WORKSHOP|OTHER") String category,
    @Size(max = 255) String note
) {
}

