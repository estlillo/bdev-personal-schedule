package com.barbatosdev.bdevpersonalschedule.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.barbatosdev.bdevpersonalschedule.calendar.domain.CalendarEvent;
import com.barbatosdev.bdevpersonalschedule.calendar.persistence.CalendarEventRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.shared.error.ScheduleConflictException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private CalendarEventRepository repository;

    @Mock
    private ProfessionalService professionalService;

    private CalendarEventService service;

    @BeforeEach
    void setUp() {
        service = new CalendarEventService(repository, professionalService);
    }

    @Test
    void createShouldRejectOverlap() {
        UUID professionalId = UUID.randomUUID();
        when(repository.hasOverlap(eq(professionalId), any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
            .thenReturn(true);

        CreateCalendarEventCommand command = new CreateCalendarEventCommand(
            professionalId,
            LocalDateTime.parse("2026-10-16T08:00:00"),
            LocalDateTime.parse("2026-10-16T09:00:00"),
            "MEETING",
            null
        );

        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(ScheduleConflictException.class);
    }

    @Test
    void createShouldTrimNoteAndPersist() {
        UUID professionalId = UUID.randomUUID();
        when(repository.hasOverlap(eq(professionalId), any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
            .thenReturn(false);
        when(repository.save(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCalendarEventCommand command = new CreateCalendarEventCommand(
            professionalId,
            LocalDateTime.parse("2026-10-16T10:00:00"),
            LocalDateTime.parse("2026-10-16T11:00:00"),
            "WORKSHOP",
            "  Reunion interna  "
        );

        CalendarEvent created = service.create(command);

        assertThat(created.note()).isEqualTo("Reunion interna");
        assertThat(created.category()).isEqualTo("WORKSHOP");
    }

    @Test
    void updateShouldPatchOnlyProvidedFields() {
        UUID eventId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();

        CalendarEvent existing = new CalendarEvent(
            eventId,
            professionalId,
            LocalDateTime.parse("2026-10-16T08:00:00"),
            LocalDateTime.parse("2026-10-16T09:00:00"),
            "MEETING",
            "Nota"
        );

        when(repository.findById(eventId)).thenReturn(Optional.of(existing));
        when(repository.hasOverlap(eq(professionalId), any(LocalDateTime.class), any(LocalDateTime.class), eq(eventId)))
            .thenReturn(false);
        when(repository.update(any(CalendarEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CalendarEvent updated = service.update(eventId, new UpdateCalendarEventCommand(
            null,
            null,
            LocalDateTime.parse("2026-10-16T09:30:00"),
            null,
            null
        ));

        assertThat(updated.startsAt()).isEqualTo(LocalDateTime.parse("2026-10-16T08:00:00"));
        assertThat(updated.endsAt()).isEqualTo(LocalDateTime.parse("2026-10-16T09:30:00"));
    }
}

