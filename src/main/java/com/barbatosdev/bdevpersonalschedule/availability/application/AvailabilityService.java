package com.barbatosdev.bdevpersonalschedule.availability.application;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import com.barbatosdev.bdevpersonalschedule.availability.persistence.AvailabilityQueryRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
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
    private final Clock clock;

    public AvailabilityService(
        AvailabilityQueryRepository repository,
        ProfessionalService professionalService,
        AvailabilityLimitsProperties limits,
        IntervalOperations intervalOperations,
        Clock clock
    ) {
        this.repository = repository;
        this.professionalService = professionalService;
        this.limits = limits;
        this.intervalOperations = intervalOperations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AvailabilitySearchResult search(AvailabilitySearchCommand command) {
        validateCommand(command);

        List<UUID> professionalIds = command.professionalIds().stream().distinct().toList();
        int stepMinutes = command.stepMinutes() != null ? command.stepMinutes() : limits.getDefaultStepMinutes();

        for (UUID professionalId : professionalIds) {
            Professional professional = professionalService.getById(professionalId);
            if (!professional.active()) {
                throw new IllegalArgumentException("professional is inactive: " + professionalId);
            }
        }

        LocalDateTime fromInclusive = command.startDate().atStartOfDay();
        LocalDateTime toExclusive = command.endDate().plusDays(1).atStartOfDay();

        List<AvailabilityQueryRepository.WorkingWindowRow> windows = repository.findWorkingWindows(professionalIds);
        List<AvailabilityQueryRepository.ScheduleExceptionRow> exceptions = repository.findExceptions(
            professionalIds,
            command.startDate(),
            command.endDate()
        );
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
                    windows,
                    exceptions,
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

        List<TimeInterval> slots = intervalOperations.generateSlots(
            commonIntervals,
            command.durationMinutes(),
            stepMinutes,
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
        List<AvailabilityQueryRepository.WorkingWindowRow> allWindows,
        List<AvailabilityQueryRepository.ScheduleExceptionRow> allExceptions,
        List<AvailabilityQueryRepository.CalendarEventRow> allEvents,
        List<AvailabilityQueryRepository.ReservationRow> allReservations
    ) {
        List<AvailabilityQueryRepository.WorkingWindowRow> windows = allWindows.stream()
            .filter(window -> window.professionalId().equals(professionalId))
            .toList();
        List<AvailabilityQueryRepository.ScheduleExceptionRow> exceptions = allExceptions.stream()
            .filter(exceptionEntry -> exceptionEntry.professionalId().equals(professionalId))
            .toList();
        List<AvailabilityQueryRepository.CalendarEventRow> events = allEvents.stream()
            .filter(event -> event.professionalId().equals(professionalId))
            .toList();
        List<AvailabilityQueryRepository.ReservationRow> reservations = allReservations.stream()
            .filter(reservation -> reservation.professionalId().equals(professionalId))
            .toList();

        Map<Integer, List<AvailabilityQueryRepository.WorkingWindowRow>> windowsByDay = windows.stream()
            .collect(java.util.stream.Collectors.groupingBy(AvailabilityQueryRepository.WorkingWindowRow::dayOfWeek));
        Map<LocalDate, List<AvailabilityQueryRepository.ScheduleExceptionRow>> exceptionsByDate = exceptions.stream()
            .collect(java.util.stream.Collectors.groupingBy(AvailabilityQueryRepository.ScheduleExceptionRow::date));

        List<TimeInterval> result = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<TimeInterval> baseIntervals = new ArrayList<>();
            for (AvailabilityQueryRepository.WorkingWindowRow window : windowsByDay.getOrDefault(date.getDayOfWeek().getValue(), List.of())) {
                baseIntervals.add(new TimeInterval(date.atTime(window.startTime()), date.atTime(window.endTime())));
            }

            List<TimeInterval> addIntervals = new ArrayList<>();
            List<TimeInterval> blockIntervals = new ArrayList<>();
            for (AvailabilityQueryRepository.ScheduleExceptionRow exceptionEntry : exceptionsByDate.getOrDefault(date, List.of())) {
                TimeInterval interval = new TimeInterval(date.atTime(exceptionEntry.startTime()), date.atTime(exceptionEntry.endTime()));
                if ("ADD".equalsIgnoreCase(exceptionEntry.mode())) {
                    addIntervals.add(interval);
                } else {
                    blockIntervals.add(interval);
                }
            }

            List<TimeInterval> available = intervalOperations.merge(mergeLists(baseIntervals, addIntervals));
            available = intervalOperations.subtract(available, blockIntervals);

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

        if (command.durationMinutes() <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }

        int stepMinutes = command.stepMinutes() != null ? command.stepMinutes() : limits.getDefaultStepMinutes();
        if (stepMinutes <= 0) {
            throw new IllegalArgumentException("stepMinutes must be positive");
        }
    }

    private List<TimeInterval> mergeLists(List<TimeInterval> first, List<TimeInterval> second) {
        List<TimeInterval> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
