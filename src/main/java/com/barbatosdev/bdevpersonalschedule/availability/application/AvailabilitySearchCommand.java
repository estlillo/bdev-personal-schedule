package com.barbatosdev.bdevpersonalschedule.availability.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilitySearchCommand(
    List<UUID> professionalIds,
    LocalDate startDate,
    LocalDate endDate,
    int durationMinutes,
    Integer stepMinutes
) {
}

