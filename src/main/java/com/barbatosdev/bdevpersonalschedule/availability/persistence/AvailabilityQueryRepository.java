package com.barbatosdev.bdevpersonalschedule.availability.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AvailabilityQueryRepository {

    List<WorkingWindowRow> findWorkingWindows(List<UUID> professionalIds);

    List<ScheduleExceptionRow> findExceptions(List<UUID> professionalIds, LocalDate from, LocalDate to);

    List<CalendarEventRow> findCalendarEvents(List<UUID> professionalIds, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    List<ReservationRow> findActiveReservations(List<UUID> professionalIds, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    record WorkingWindowRow(
        UUID professionalId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
    ) {
    }

    record ScheduleExceptionRow(
        UUID professionalId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String mode
    ) {
    }

    record CalendarEventRow(
        UUID professionalId,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    ) {
    }

    record ReservationRow(
        UUID professionalId,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    ) {
    }
}
