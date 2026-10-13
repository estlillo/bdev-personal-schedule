package com.barbatosdev.bdevpersonalschedule.schedule.application;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleExceptionDraft(
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String mode,
    String reason
) {
}

