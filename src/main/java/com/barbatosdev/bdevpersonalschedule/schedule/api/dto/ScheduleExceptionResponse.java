package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionResponse(
    UUID id,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String mode,
    String reason
) {
}

