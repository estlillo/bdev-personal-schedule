package com.barbatosdev.bdevpersonalschedule.availability.application;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import com.barbatosdev.bdevpersonalschedule.availability.persistence.AvailabilityQueryRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalSchedulePolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private static final String WARNING_MESSAGE =
        "Disponibilidad basada en registros locales; verificar agenda oficial";

    private final AvailabilityQueryRepository repository;
    private final ProfessionalService professionalService;
    private final AvailabilityLimitsProperties limits;
    private final IntervalOperations intervalOperations;
    private final InstitutionalSchedulePolicy institutionalSchedulePolicy;
    private final Clock clock;

    public AvailabilityService(
        AvailabilityQueryRepository repository,
        ProfessionalService professionalService,
        AvailabilityLimitsProperties limits,
        IntervalOperations intervalOperations,
        InstitutionalSchedulePolicy institutionalSchedulePolicy,
        Clock clock
    ) {
        this.repository = repository;
        this.professionalService = professionalService;
        this.limits = limits;
        this.intervalOperations = intervalOperations;
        this.institutionalSchedulePolicy = institutionalSchedulePolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AvailabilitySearchResult search(AvailabilitySearchCommand command) {
        validateCommand(command);

        List<UUID> professionalIds = command.professionalIds().stream().distinct().toList();

        for (UUID professionalId : professionalIds) {
            Professional professional = professionalService.getById(professionalId);
            if (!professional.active()) {
                throw new IllegalArgumentException("professional is inactive: " + professionalId);
            }
        }

        LocalDateTime fromInclusive = command.startDate().atStartOfDay();
        LocalDateTime toExclusive = command.endDate().plusDays(1).atStartOfDay();

        List<AvailabilityQueryRepository.CalendarEventRow> events = repository.findCalendarEvents(
            professionalIds,
            fromInclusive,
            toExclusive
        );
        List<AvailabilityQueryRepository.ReservationRow> activeReservations = repository.findActiveReservations(
            professionalIds,
            fromInclusive,
            toExclusive
        );

        Map<UUID, List<TimeInterval>> freeIntervalsByProfessional = new HashMap<>();
        for (UUID professionalId : professionalIds) {
            freeIntervalsByProfessional.put(
                professionalId,
                buildProfessionalAvailability(
                    professionalId,
                    command.startDate(),
                    command.endDate(),
                    events,
                    activeReservations
                )
            );
        }

        List<TimeInterval> commonIntervals = freeIntervalsByProfessional.get(professionalIds.get(0));
        for (int i = 1; i < professionalIds.size(); i++) {
            UUID professionalId = professionalIds.get(i);
            commonIntervals = intervalOperations.intersect(commonIntervals, freeIntervalsByProfessional.get(professionalId));
            if (commonIntervals.isEmpty()) {
                break;
            }
        }

        List<TimeInterval> slots = generateInstitutionalSlotsWithinIntervals(
            command.startDate(),
            command.endDate(),
            commonIntervals,
            limits.getMaxReturnedSlots()
        );

        return new AvailabilitySearchResult(
            slots,
            LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS),
            WARNING_MESSAGE
        );
    }

    private List<TimeInterval> buildProfessionalAvailability(
        UUID professionalId,
        LocalDate from,
        LocalDate to,
        List<AvailabilityQueryRepository.CalendarEventRow> allEvents,
        List<AvailabilityQueryRepository.ReservationRow> allReservations
    ) {
        List<AvailabilityQueryRepository.CalendarEventRow> events = allEvents.stream()
            .filter(event -> event.professionalId().equals(professionalId))
            .toList();
        List<AvailabilityQueryRepository.ReservationRow> reservations = allReservations.stream()
            .filter(reservation -> reservation.professionalId().equals(professionalId))
            .toList();

        List<TimeInterval> result = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<TimeInterval> available = new ArrayList<>(institutionalSchedulePolicy.workingIntervals(date));

            List<TimeInterval> dayEvents = new ArrayList<>();
            for (AvailabilityQueryRepository.CalendarEventRow event : events) {
                LocalDateTime startsAt = max(dayStart, event.startsAt());
                LocalDateTime endsAt = min(dayEnd, event.endsAt());
                if (startsAt.isBefore(endsAt)) {
                    dayEvents.add(new TimeInterval(startsAt, endsAt));
                }
            }

            List<TimeInterval> dayReservations = new ArrayList<>();
            for (AvailabilityQueryRepository.ReservationRow reservation : reservations) {
                LocalDateTime startsAt = max(dayStart, reservation.startsAt());
                LocalDateTime endsAt = min(dayEnd, reservation.endsAt());
                if (startsAt.isBefore(endsAt)) {
                    dayReservations.add(new TimeInterval(startsAt, endsAt));
                }
            }

            available = intervalOperations.subtract(available, dayEvents);
            available = intervalOperations.subtract(available, dayReservations);
            result.addAll(available);

            date = date.plusDays(1);
        }

        return intervalOperations.merge(result);
    }

    private void validateCommand(AvailabilitySearchCommand command) {
        if (command.professionalIds() == null || command.professionalIds().isEmpty()) {
            throw new IllegalArgumentException("professionalIds must not be empty");
        }
        if (command.professionalIds().size() > limits.getMaxProfessionals()) {
            throw new IllegalArgumentException("professionalIds exceeds maxProfessionals=" + limits.getMaxProfessionals());
        }
        if (command.startDate() == null || command.endDate() == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (command.startDate().isAfter(command.endDate())) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        long requestedDays = ChronoUnit.DAYS.between(command.startDate(), command.endDate()) + 1;
        if (requestedDays > limits.getMaxRangeDays()) {
            throw new IllegalArgumentException("requested date range exceeds maxRangeDays=" + limits.getMaxRangeDays());
        }

        if (command.durationMinutes() != institutionalSchedulePolicy.slotMinutes()) {
            throw new IllegalArgumentException("durationMinutes must be exactly " + institutionalSchedulePolicy.slotMinutes());
        }

        int stepMinutes = command.stepMinutes() != null ? command.stepMinutes() : limits.getDefaultStepMinutes();
        if (stepMinutes != institutionalSchedulePolicy.slotMinutes()) {
            throw new IllegalArgumentException("stepMinutes must be exactly " + institutionalSchedulePolicy.slotMinutes());
        }
    }

    private List<TimeInterval> generateInstitutionalSlotsWithinIntervals(
        LocalDate from,
        LocalDate to,
        List<TimeInterval> availableIntervals,
        int maxReturnedSlots
    ) {
        List<TimeInterval> mergedAvailability = intervalOperations.merge(availableIntervals);
        List<TimeInterval> slots = new ArrayList<>();

        LocalDate date = from;
        while (!date.isAfter(to)) {
            for (TimeInterval slot : institutionalSchedulePolicy.slotIntervals(date)) {
                boolean isCovered = mergedAvailability.stream()
                    .anyMatch(interval ->
                        !slot.startsAt().isBefore(interval.startsAt()) &&
                            !slot.endsAt().isAfter(interval.endsAt())
                    );
                if (isCovered) {
                    slots.add(slot);
                    if (slots.size() >= maxReturnedSlots) {
                        return slots;
                    }
                }
            }
            date = date.plusDays(1);
        }

        return slots;
    }


    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
