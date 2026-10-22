package com.barbatosdev.bdevpersonalschedule.calendar.api;

import com.barbatosdev.bdevpersonalschedule.calendar.api.dto.CalendarEventResponse;
import com.barbatosdev.bdevpersonalschedule.calendar.domain.CalendarEvent;

final class CalendarEventMapper {

    private CalendarEventMapper() {
    }

    static CalendarEventResponse toResponse(CalendarEvent calendarEvent) {
        return new CalendarEventResponse(
            calendarEvent.id(),
            calendarEvent.professionalId(),
            calendarEvent.startsAt(),
            calendarEvent.endsAt(),
            calendarEvent.category(),
            calendarEvent.note()
        );
    }
}

