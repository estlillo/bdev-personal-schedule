package com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record InstitutionalBlockRuleRequest(
    @NotBlank String name,
    String category,
    @NotBlank String ruleType,
    Integer dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    LocalDate startDate,
    LocalDate endDate,
    @NotNull Boolean active
) {
}

