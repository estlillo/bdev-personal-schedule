package com.barbatosdev.bdevpersonalschedule.calendar.application;

import com.barbatosdev.bdevpersonalschedule.calendar.domain.CalendarEvent;
import com.barbatosdev.bdevpersonalschedule.calendar.persistence.CalendarEventRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import com.barbatosdev.bdevpersonalschedule.shared.error.ScheduleConflictException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarEventService {

    private final CalendarEventRepository repository;
    private final ProfessionalService professionalService;

    public CalendarEventService(CalendarEventRepository repository, ProfessionalService professionalService) {
        this.repository = repository;
        this.professionalService = professionalService;
    }

    @Transactional(readOnly = true)
    public List<CalendarEvent> list(UUID professionalId, LocalDate from, LocalDate to) {
        professionalService.getById(professionalId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();
        return repository.findByProfessionalAndRange(professionalId, fromInclusive, toExclusive);
    }

    @Transactional
    public CalendarEvent create(CreateCalendarEventCommand command) {
        professionalService.getById(command.professionalId());
        validateInterval(command.startsAt(), command.endsAt());
        validateCategory(command.category());
        ensureNoConflict(command.professionalId(), command.startsAt(), command.endsAt(), null);

        CalendarEvent calendarEvent = new CalendarEvent(
            UUID.randomUUID(),
            command.professionalId(),
            command.startsAt(),
            command.endsAt(),
            normalizeCategory(command.category()),
            normalizeNullableText(command.note())
        );
        return repository.save(calendarEvent);
    }

    @Transactional
    public CalendarEvent update(UUID eventId, UpdateCalendarEventCommand command) {
        CalendarEvent current = repository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Calendar event not found: " + eventId));

        UUID targetProfessionalId = command.professionalId() != null ? command.professionalId() : current.professionalId();
        LocalDateTime targetStartsAt = command.startsAt() != null ? command.startsAt() : current.startsAt();
        LocalDateTime targetEndsAt = command.endsAt() != null ? command.endsAt() : current.endsAt();

        professionalService.getById(targetProfessionalId);
        validateInterval(targetStartsAt, targetEndsAt);
        if (command.category() != null) {
            validateCategory(command.category());
        }
        ensureNoConflict(targetProfessionalId, targetStartsAt, targetEndsAt, current.id());

        CalendarEvent updated = new CalendarEvent(
            current.id(),
            targetProfessionalId,
            targetStartsAt,
            targetEndsAt,
            command.category() != null ? normalizeCategory(command.category()) : current.category(),
            command.note() != null ? normalizeNullableText(command.note()) : current.note()
        );

        return repository.update(updated);
    }

    @Transactional
    public void delete(UUID eventId) {
        boolean deleted = repository.deleteById(eventId);
        if (!deleted) {
            throw new NotFoundException("Calendar event not found: " + eventId);
        }
    }

    private void validateInterval(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    private void ensureNoConflict(UUID professionalId, LocalDateTime startsAt, LocalDateTime endsAt, UUID excludedEventId) {
        if (repository.hasOverlap(professionalId, startsAt, endsAt, excludedEventId)) {
            throw new ScheduleConflictException("Calendar event overlaps existing event");
        }
    }

    private void validateCategory(String category) {
        String normalized = normalizeCategory(category);
        if (!"MEETING".equals(normalized) && !"WORKSHOP".equals(normalized) && !"OTHER".equals(normalized)) {
            throw new IllegalArgumentException("category must be MEETING, WORKSHOP or OTHER");
        }
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }
        return category.trim().toUpperCase();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

