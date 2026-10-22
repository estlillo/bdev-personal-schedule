package com.barbatosdev.bdevpersonalschedule.availability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.availability.persistence.AvailabilityQueryRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityQueryRepository repository;

    @Mock
    private ProfessionalService professionalService;

    private AvailabilityService service;

    @BeforeEach
    void setUp() {
        AvailabilityLimitsProperties properties = new AvailabilityLimitsProperties();
        properties.setMaxProfessionals(7);
        properties.setMaxRangeDays(30);
        properties.setDefaultStepMinutes(15);
        properties.setMaxReturnedSlots(500);

        Clock clock = Clock.fixed(Instant.parse("2026-10-12T18:00:00Z"), ZoneId.of("America/Santiago"));
        service = new AvailabilityService(repository, professionalService, properties, new IntervalOperations(), clock);
    }

    @Test
    void searchShouldRejectInactiveProfessional() {
        UUID professionalId = UUID.randomUUID();
        when(professionalService.getById(professionalId)).thenReturn(professional(professionalId, false));

        AvailabilitySearchCommand command = new AvailabilitySearchCommand(
            List.of(professionalId),
            LocalDate.parse("2026-10-19"),
            LocalDate.parse("2026-10-19"),
            45,
            15
        );

        assertThatThrownBy(() -> service.search(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inactive");
    }

    @Test
    void searchShouldComposeWindowsExceptionsAndEvents() {
        UUID professionalId = UUID.randomUUID();
        when(professionalService.getById(professionalId)).thenReturn(professional(professionalId, true));

        when(repository.findWorkingWindows(anyList())).thenReturn(List.of(
            new AvailabilityQueryRepository.WorkingWindowRow(
                professionalId,
                1,
                java.time.LocalTime.parse("08:00"),
                java.time.LocalTime.parse("12:00")
            )
        ));
        when(repository.findExceptions(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(
            new AvailabilityQueryRepository.ScheduleExceptionRow(
                professionalId,
                LocalDate.parse("2026-10-19"),
                java.time.LocalTime.parse("10:00"),
                java.time.LocalTime.parse("11:00"),
                "BLOCK"
            ),
            new AvailabilityQueryRepository.ScheduleExceptionRow(
                professionalId,
                LocalDate.parse("2026-10-19"),
                java.time.LocalTime.parse("18:00"),
                java.time.LocalTime.parse("19:00"),
                "ADD"
            )
        ));
        when(repository.findCalendarEvents(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(
            new AvailabilityQueryRepository.CalendarEventRow(
                professionalId,
                LocalDateTime.parse("2026-10-19T08:30:00"),
                LocalDateTime.parse("2026-10-19T09:00:00")
            )
        ));

        AvailabilitySearchResult result = service.search(new AvailabilitySearchCommand(
            List.of(professionalId),
            LocalDate.parse("2026-10-19"),
            LocalDate.parse("2026-10-19"),
            30,
            15
        ));

        assertThat(result.warning()).contains("registros locales");
        assertThat(result.slots())
            .extracting(slot -> slot.startsAt().toString())
            .contains("2026-10-19T08:00", "2026-10-19T09:00", "2026-10-19T18:00")
            .doesNotContain("2026-10-19T08:30");
    }

    @Test
    void searchShouldIntersectMultipleProfessionals() {
        UUID professionalA = UUID.randomUUID();
        UUID professionalB = UUID.randomUUID();

        when(professionalService.getById(professionalA)).thenReturn(professional(professionalA, true));
        when(professionalService.getById(professionalB)).thenReturn(professional(professionalB, true));

        when(repository.findWorkingWindows(anyList())).thenReturn(List.of(
            new AvailabilityQueryRepository.WorkingWindowRow(
                professionalA,
                1,
                java.time.LocalTime.parse("08:00"),
                java.time.LocalTime.parse("12:00")
            ),
            new AvailabilityQueryRepository.WorkingWindowRow(
                professionalB,
                1,
                java.time.LocalTime.parse("09:00"),
                java.time.LocalTime.parse("11:00")
            )
        ));
        when(repository.findExceptions(anyList(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(repository.findCalendarEvents(anyList(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        AvailabilitySearchResult result = service.search(new AvailabilitySearchCommand(
            List.of(professionalA, professionalB),
            LocalDate.parse("2026-10-19"),
            LocalDate.parse("2026-10-19"),
            60,
            30
        ));

        assertThat(result.slots())
            .extracting(slot -> slot.startsAt().toString())
            .containsExactly("2026-10-19T09:00", "2026-10-19T09:30", "2026-10-19T10:00");
    }

    private Professional professional(UUID id, boolean active) {
        LocalDateTime now = LocalDateTime.parse("2026-10-12T10:00:00");
        return new Professional(id, "Profesional", "KINE", active, now, now);
    }
}
