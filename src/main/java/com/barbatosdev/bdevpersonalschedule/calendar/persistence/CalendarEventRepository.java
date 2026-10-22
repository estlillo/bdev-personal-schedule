package com.barbatosdev.bdevpersonalschedule.calendar.persistence;

import com.barbatosdev.bdevpersonalschedule.calendar.domain.CalendarEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository {

    List<CalendarEvent> findByProfessionalAndRange(UUID professionalId, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    Optional<CalendarEvent> findById(UUID id);

    CalendarEvent save(CalendarEvent calendarEvent);

    CalendarEvent update(CalendarEvent calendarEvent);

    boolean deleteById(UUID id);

    boolean hasOverlap(UUID professionalId, LocalDateTime startsAt, LocalDateTime endsAt, UUID excludedEventId);
}

