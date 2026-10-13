package com.barbatosdev.bdevpersonalschedule.schedule.domain;

import java.time.LocalTime;
import java.util.UUID;

public record WorkingWindow(
    UUID id,
    UUID professionalId,
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}

