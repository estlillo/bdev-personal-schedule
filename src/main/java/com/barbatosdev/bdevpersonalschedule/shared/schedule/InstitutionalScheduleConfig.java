package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record InstitutionalScheduleConfig(
    LocalTime dayStart,
    LocalTime dayEnd,
    int slotMinutes,
    LocalDateTime updatedAt
) {
}

