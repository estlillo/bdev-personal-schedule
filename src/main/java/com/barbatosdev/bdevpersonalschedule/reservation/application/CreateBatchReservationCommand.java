package com.barbatosdev.bdevpersonalschedule.reservation.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateBatchReservationCommand(
    List<UUID> professionalIds,
    LocalTime startTime,
    LocalTime endTime,
    String referenceCode,
    String description,
    List<LocalDate> sessionDates,
    Recurrence recurrence
) {

    public record Recurrence(
        LocalDate startDate,
        Integer occurrences,
        Integer intervalWeeks,
        List<Integer> daysOfWeek
    ) {
    }
}

