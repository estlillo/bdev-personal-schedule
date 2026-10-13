package com.barbatosdev.bdevpersonalschedule.schedule.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionEntry(
    UUID id,
    UUID professionalId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String mode,
    String reason
) {
}

