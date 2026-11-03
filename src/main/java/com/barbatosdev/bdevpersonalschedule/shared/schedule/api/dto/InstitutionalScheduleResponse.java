package com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto;

import java.time.LocalTime;

public record InstitutionalScheduleResponse(
    LocalTime dayStart,
    LocalTime dayEnd,
    int slotMinutes
) {
}

