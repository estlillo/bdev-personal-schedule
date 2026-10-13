package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleExceptionRequest(
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull @Pattern(regexp = "BLOCK|ADD") String mode,
    @Size(max = 255) String reason
) {
}

