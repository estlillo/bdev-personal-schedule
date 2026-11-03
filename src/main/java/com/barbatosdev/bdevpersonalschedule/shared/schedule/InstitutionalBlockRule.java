package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record InstitutionalBlockRule(
    UUID id,
    String name,
    String category,
    InstitutionalRuleType ruleType,
    Integer dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

