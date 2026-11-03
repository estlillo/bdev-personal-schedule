package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateBatchReservationRequest(
    @NotEmpty List<@NotNull UUID> professionalIds,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @Size(max = 100) String referenceCode,
    @Size(max = 255) String description,
    List<@NotNull LocalDate> sessionDates,
    @Valid RecurrenceRequest recurrence
) {

    public record RecurrenceRequest(
        @NotNull LocalDate startDate,
        @NotNull @Min(1) Integer occurrences,
        @NotNull @Min(1) Integer intervalWeeks,
        @NotEmpty List<@NotNull @Min(1) @Max(7) Integer> daysOfWeek
    ) {
    }
}

