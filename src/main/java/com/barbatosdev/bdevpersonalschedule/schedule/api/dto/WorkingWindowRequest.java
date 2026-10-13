package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record WorkingWindowRequest(
    @Min(1) @Max(7) int dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {
}

