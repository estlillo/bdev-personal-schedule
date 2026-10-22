package com.barbatosdev.bdevpersonalschedule.availability.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailabilitySearchRequest(
    @NotEmpty List<@NotNull UUID> professionalIds,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @Positive Integer durationMinutes,
    @Positive Integer stepMinutes
) {
}

