package com.barbatosdev.bdevpersonalschedule.reservation.application;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import com.barbatosdev.bdevpersonalschedule.availability.persistence.AvailabilityQueryRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import com.barbatosdev.bdevpersonalschedule.reservation.persistence.ReservationRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.InvalidStatusTransitionException;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import com.barbatosdev.bdevpersonalschedule.shared.error.OutsideWorkingHoursException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProfessionalService professionalService;
    private final AvailabilityQueryRepository availabilityQueryRepository;
    private final IntervalOperations intervalOperations;
    private final Clock clock;

    public ReservationService(
        ReservationRepository reservationRepository,
        ProfessionalService professionalService,
        AvailabilityQueryRepository availabilityQueryRepository,
        IntervalOperations intervalOperations,
        Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.professionalService = professionalService;
        this.availabilityQueryRepository = availabilityQueryRepository;
        this.intervalOperations = intervalOperations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Reservation> list(LocalDate from, LocalDate to, String status) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        ReservationStatus normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = ReservationStatus.fromValue(status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("status must be HOLD, CONFIRMED or CANCELLED");
            }
        }

        return reservationRepository.findByRange(from, to, normalizedStatus);
    }

    public Reservation create(CreateReservationCommand command) {
        validateBasicCommand(command);

        List<UUID> professionalIds = command.professionalIds().stream().distinct().toList();
        for (UUID professionalId : professionalIds) {
            Professional professional = professionalService.getById(professionalId);
            if (!professional.active()) {
                throw new IllegalArgumentException("professional is inactive: " + professionalId);
            }
            ensureInsideWorkingHours(professionalId, command.startsAt(), command.endsAt());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = new Reservation(
            UUID.randomUUID(),
            command.startsAt(),
            command.endsAt(),
            ReservationStatus.HOLD,
            normalizeNullableText(command.referenceCode()),
            normalizeNullableText(command.description()),
            now,
            now,
            professionalIds
        );

        return reservationRepository.createAtomic(reservation);
    }

    @Transactional
    public Reservation updateStatus(UUID reservationId, String targetStatusRaw) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found: " + reservationId));

        ReservationStatus targetStatus;
        try {
            targetStatus = ReservationStatus.fromValue(targetStatusRaw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status must be HOLD, CONFIRMED or CANCELLED");
        }

        if (!isTransitionAllowed(reservation.status(), targetStatus)) {
            throw new InvalidStatusTransitionException(
                "Invalid status transition from " + reservation.status() + " to " + targetStatus
            );
        }

        return reservationRepository.updateStatus(reservationId, targetStatus, LocalDateTime.now(clock));
    }

    private void validateBasicCommand(CreateReservationCommand command) {
        if (command.professionalIds() == null || command.professionalIds().isEmpty()) {
            throw new IllegalArgumentException("professionalIds must not be empty");
        }
        if (command.startsAt() == null || command.endsAt() == null) {
            throw new IllegalArgumentException("startsAt and endsAt are required");
        }
        if (!command.startsAt().isBefore(command.endsAt())) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    private boolean isTransitionAllowed(ReservationStatus current, ReservationStatus target) {
        if (current == ReservationStatus.HOLD) {
            return target == ReservationStatus.CONFIRMED || target == ReservationStatus.CANCELLED;
        }
        if (current == ReservationStatus.CONFIRMED) {
            return target == ReservationStatus.CANCELLED;
        }
        return false;
    }

    private void ensureInsideWorkingHours(UUID professionalId, LocalDateTime startsAt, LocalDateTime endsAt) {
        LocalDate from = startsAt.toLocalDate();
        LocalDate to = endsAt.toLocalDate();

        List<AvailabilityQueryRepository.WorkingWindowRow> windows = availabilityQueryRepository.findWorkingWindows(List.of(professionalId));
        List<AvailabilityQueryRepository.ScheduleExceptionRow> exceptions =
            availabilityQueryRepository.findExceptions(List.of(professionalId), from, to);

        Map<Integer, List<AvailabilityQueryRepository.WorkingWindowRow>> windowsByDay = windows.stream()
            .collect(java.util.stream.Collectors.groupingBy(AvailabilityQueryRepository.WorkingWindowRow::dayOfWeek));
        Map<LocalDate, List<AvailabilityQueryRepository.ScheduleExceptionRow>> exceptionsByDate = exceptions.stream()
            .collect(java.util.stream.Collectors.groupingBy(AvailabilityQueryRepository.ScheduleExceptionRow::date));

        List<TimeInterval> effectiveWorkingIntervals = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            List<TimeInterval> baseIntervals = new ArrayList<>();
            for (AvailabilityQueryRepository.WorkingWindowRow window : windowsByDay.getOrDefault(date.getDayOfWeek().getValue(), List.of())) {
                baseIntervals.add(new TimeInterval(date.atTime(window.startTime()), date.atTime(window.endTime())));
            }

            List<TimeInterval> addIntervals = new ArrayList<>();
            List<TimeInterval> blockIntervals = new ArrayList<>();
            for (AvailabilityQueryRepository.ScheduleExceptionRow exceptionEntry : exceptionsByDate.getOrDefault(date, List.of())) {
                TimeInterval exceptionInterval = new TimeInterval(
                    date.atTime(exceptionEntry.startTime()),
                    date.atTime(exceptionEntry.endTime())
                );
                if ("ADD".equalsIgnoreCase(exceptionEntry.mode())) {
                    addIntervals.add(exceptionInterval);
                } else {
                    blockIntervals.add(exceptionInterval);
                }
            }

            List<TimeInterval> merged = intervalOperations.merge(mergeLists(baseIntervals, addIntervals));
            effectiveWorkingIntervals.addAll(intervalOperations.subtract(merged, blockIntervals));

            date = date.plusDays(1);
        }

        TimeInterval requested = new TimeInterval(startsAt, endsAt);
        boolean covered = intervalOperations.merge(effectiveWorkingIntervals).stream()
            .anyMatch(interval -> !requested.startsAt().isBefore(interval.startsAt()) && !requested.endsAt().isAfter(interval.endsAt()));

        if (!covered) {
            throw new OutsideWorkingHoursException(
                "Reservation interval is outside effective working hours for professional " + professionalId
            );
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<TimeInterval> mergeLists(List<TimeInterval> first, List<TimeInterval> second) {
        List<TimeInterval> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }
}

