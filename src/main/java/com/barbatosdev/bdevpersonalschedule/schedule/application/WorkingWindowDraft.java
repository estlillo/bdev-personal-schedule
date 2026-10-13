package com.barbatosdev.bdevpersonalschedule.schedule.application;

import java.time.LocalTime;

public record WorkingWindowDraft(
    int dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {
}

