package com.barbatosdev.bdevpersonalschedule.schedule.api.dto;

import java.time.LocalTime;

public record WorkingWindowResponse(
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}

