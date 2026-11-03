package com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record InstitutionalBlockRuleResponse(
    UUID id,
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

