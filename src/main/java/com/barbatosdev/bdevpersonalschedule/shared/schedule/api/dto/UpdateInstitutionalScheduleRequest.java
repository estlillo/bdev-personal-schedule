package com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public record UpdateInstitutionalScheduleRequest(
    @NotNull LocalTime dayStart,
    @NotNull LocalTime dayEnd,
    @NotNull @Positive Integer slotMinutes
) {
}

