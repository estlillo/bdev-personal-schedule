package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record InstitutionalBlockDraft(
    String name,
    String category,
    String ruleType,
    Integer dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    LocalDate startDate,
    LocalDate endDate,
    boolean active
) {
}

