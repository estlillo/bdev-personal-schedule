package com.barbatosdev.bdevpersonalschedule.availability.api.dto;

import java.time.LocalDateTime;

public record AvailabilitySlotResponse(
    LocalDateTime startsAt,
    LocalDateTime endsAt
) {
}

