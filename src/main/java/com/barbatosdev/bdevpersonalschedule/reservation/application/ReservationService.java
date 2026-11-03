package com.barbatosdev.bdevpersonalschedule.reservation.application;

import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import com.barbatosdev.bdevpersonalschedule.reservation.persistence.ReservationRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.InvalidStatusTransitionException;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import com.barbatosdev.bdevpersonalschedule.shared.error.OutsideWorkingHoursException;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalSchedulePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProfessionalService professionalService;
    private final InstitutionalSchedulePolicy institutionalSchedulePolicy;
    private final Clock clock;

    public ReservationService(
        ReservationRepository reservationRepository,
        ProfessionalService professionalService,
        InstitutionalSchedulePolicy institutionalSchedulePolicy,
        Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.professionalService = professionalService;
        this.institutionalSchedulePolicy = institutionalSchedulePolicy;
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
        ensureInstitutionalSlot(command.startsAt(), command.endsAt());
        for (UUID professionalId : professionalIds) {
            Professional professional = professionalService.getById(professionalId);
            if (!professional.active()) {
                throw new IllegalArgumentException("professional is inactive: " + professionalId);
            }
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

    public List<Reservation> createBatch(CreateBatchReservationCommand command) {
        validateBatchCommand(command);

        List<LocalDate> sessionDates = resolveSessionDates(command);

        List<UUID> professionalIds = command.professionalIds().stream().distinct().toList();
        for (UUID professionalId : professionalIds) {
            Professional professional = professionalService.getById(professionalId);
            if (!professional.active()) {
                throw new IllegalArgumentException("professional is inactive: " + professionalId);
            }
        }
        String normalizedReferenceCode = normalizeNullableText(command.referenceCode());
        String normalizedDescription = normalizeNullableText(command.description());
        LocalDateTime now = LocalDateTime.now(clock);

        List<Reservation> batch = new ArrayList<>();
        for (LocalDate sessionDate : sessionDates) {
            LocalDateTime startsAt = sessionDate.atTime(command.startTime());
            LocalDateTime endsAt = sessionDate.atTime(command.endTime());
            ensureInstitutionalSlot(startsAt, endsAt);


            batch.add(new Reservation(
                UUID.randomUUID(),
                startsAt,
                endsAt,
                ReservationStatus.HOLD,
                normalizedReferenceCode,
                normalizedDescription,
                now,
                now,
                professionalIds
            ));
        }

        return reservationRepository.createBatchAtomic(batch);
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

    @Transactional
    public Reservation updateMetadata(UUID reservationId, String referenceCode, String description) {
        reservationRepository.findById(reservationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found: " + reservationId));

        String normalizedReferenceCode = normalizeNullableText(referenceCode);
        String normalizedDescription = normalizeNullableText(description);
        if (normalizedReferenceCode == null && normalizedDescription == null) {
            throw new IllegalArgumentException("referenceCode or description must contain text");
        }

        return reservationRepository.updateMetadata(
            reservationId,
            normalizedReferenceCode,
            normalizedDescription,
            LocalDateTime.now(clock)
        );
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

    private void validateBatchCommand(CreateBatchReservationCommand command) {
        if (command.professionalIds() == null || command.professionalIds().isEmpty()) {
            throw new IllegalArgumentException("professionalIds must not be empty");
        }
        if (command.startTime() == null || command.endTime() == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!command.startTime().isBefore(command.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        boolean hasDates = command.sessionDates() != null && !command.sessionDates().isEmpty();
        boolean hasRecurrence = command.recurrence() != null;
        if (hasDates == hasRecurrence) {
            throw new IllegalArgumentException("provide either sessionDates or recurrence");
        }
    }

    private List<LocalDate> resolveSessionDates(CreateBatchReservationCommand command) {
        if (command.sessionDates() != null && !command.sessionDates().isEmpty()) {
            Set<LocalDate> unique = new LinkedHashSet<>(command.sessionDates());
            if (unique.size() != command.sessionDates().size()) {
                throw new IllegalArgumentException("sessionDates must not contain duplicates");
            }
            return command.sessionDates().stream().sorted().toList();
        }

        CreateBatchReservationCommand.Recurrence recurrence = command.recurrence();
        if (recurrence == null) {
            throw new IllegalArgumentException("recurrence is required when sessionDates is empty");
        }
        if (recurrence.startDate() == null || recurrence.occurrences() == null || recurrence.intervalWeeks() == null) {
            throw new IllegalArgumentException("recurrence.startDate, recurrence.occurrences and recurrence.intervalWeeks are required");
        }
        if (recurrence.occurrences() < 1) {
            throw new IllegalArgumentException("recurrence.occurrences must be >= 1");
        }
        if (recurrence.intervalWeeks() < 1) {
            throw new IllegalArgumentException("recurrence.intervalWeeks must be >= 1");
        }
        if (recurrence.daysOfWeek() == null || recurrence.daysOfWeek().isEmpty()) {
            throw new IllegalArgumentException("recurrence.daysOfWeek must not be empty");
        }

        List<Integer> dayOrder = recurrence.daysOfWeek().stream()
            .distinct()
            .sorted()
            .toList();

        for (Integer dayOfWeek : dayOrder) {
            if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
                throw new IllegalArgumentException("recurrence.daysOfWeek must contain values from 1 to 7");
            }
        }

        List<LocalDate> result = new ArrayList<>();
        LocalDate firstWeekStart = recurrence.startDate().minusDays(recurrence.startDate().getDayOfWeek().getValue() - 1L);
        LocalDate cursorWeekStart = firstWeekStart;

        while (result.size() < recurrence.occurrences()) {
            for (Integer dayOfWeek : dayOrder) {
                LocalDate candidate = cursorWeekStart.plusDays(dayOfWeek - 1L);
                if (candidate.isBefore(recurrence.startDate())) {
                    continue;
                }
                result.add(candidate);
                if (result.size() == recurrence.occurrences()) {
                    break;
                }
            }
            cursorWeekStart = cursorWeekStart.plusWeeks(recurrence.intervalWeeks());
        }

        return result.stream().sorted(Comparator.naturalOrder()).toList();
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

    private void ensureInstitutionalSlot(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!institutionalSchedulePolicy.isValidInstitutionalSlot(startsAt, endsAt)) {
            throw new OutsideWorkingHoursException("Reservation interval is outside institutional schedule");
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}

