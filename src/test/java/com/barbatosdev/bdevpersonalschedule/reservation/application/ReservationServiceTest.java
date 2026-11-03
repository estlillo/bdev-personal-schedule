package com.barbatosdev.bdevpersonalschedule.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import com.barbatosdev.bdevpersonalschedule.reservation.persistence.ReservationRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.InvalidStatusTransitionException;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalBlockRule;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalSchedulePolicy;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalScheduleConfig;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalScheduleService;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalRuleType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProfessionalService professionalService;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-12T18:00:00Z"), ZoneId.of("America/Santiago"));
        InstitutionalScheduleService institutionalScheduleService = Mockito.mock(InstitutionalScheduleService.class);
        Mockito.lenient().when(institutionalScheduleService.getScheduleConfig()).thenReturn(new InstitutionalScheduleConfig(
            LocalTime.parse("08:00"),
            LocalTime.parse("17:00"),
            45,
            LocalDateTime.parse("2026-10-12T10:00:00")
        ));
        Mockito.lenient().when(institutionalScheduleService.listActiveBlockRules()).thenReturn(List.of(
            new InstitutionalBlockRule(
                UUID.randomUUID(),
                "Pausa saludable",
                "HEALTH_BREAK",
                InstitutionalRuleType.WEEKLY,
                1,
                LocalTime.parse("10:15"),
                LocalTime.parse("10:30"),
                null,
                null,
                true,
                LocalDateTime.parse("2026-10-12T10:00:00"),
                LocalDateTime.parse("2026-10-12T10:00:00")
            ),
            new InstitutionalBlockRule(
                UUID.randomUUID(),
                "Hora de colacion",
                "LUNCH",
                InstitutionalRuleType.WEEKLY,
                1,
                LocalTime.parse("13:00"),
                LocalTime.parse("14:00"),
                null,
                null,
                true,
                LocalDateTime.parse("2026-10-12T10:00:00"),
                LocalDateTime.parse("2026-10-12T10:00:00")
            )
        ));
        InstitutionalSchedulePolicy schedulePolicy = new InstitutionalSchedulePolicy(
            institutionalScheduleService,
            new IntervalOperations()
        );
        reservationService = new ReservationService(
            reservationRepository,
            professionalService,
            schedulePolicy,
            clock
        );
    }

    @Test
    void createShouldRejectOutsideWorkingHours() {
        UUID professionalId = UUID.randomUUID();

        assertThatThrownBy(() -> reservationService.create(new CreateReservationCommand(
            List.of(professionalId),
            LocalDateTime.parse("2026-10-19T10:15:00"),
            LocalDateTime.parse("2026-10-19T11:00:00"),
            "CASO-001",
            "Sesion inicial"
        ))).hasMessageContaining("outside institutional schedule");
    }

    @Test
    void updateStatusShouldAllowHoldToConfirmed() {
        UUID reservationId = UUID.randomUUID();
        Reservation hold = reservation(reservationId, ReservationStatus.HOLD);
        Reservation confirmed = reservation(reservationId, ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(hold));
        when(reservationRepository.updateStatus(any(), any(), any())).thenReturn(confirmed);

        Reservation updated = reservationService.updateStatus(reservationId, "CONFIRMED");

        assertThat(updated.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void updateStatusShouldRejectCancelledToConfirmed() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(reservationId, ReservationStatus.CANCELLED)));

        assertThatThrownBy(() -> reservationService.updateStatus(reservationId, "CONFIRMED"))
            .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateMetadataShouldPersistTrimmedValues() {
        UUID reservationId = UUID.randomUUID();
        Reservation existing = reservation(reservationId, ReservationStatus.HOLD);
        Reservation updated = new Reservation(
            reservationId,
            existing.startsAt(),
            existing.endsAt(),
            existing.status(),
            "CASO-NEW",
            "Nueva descripcion",
            existing.createdAt(),
            existing.updatedAt(),
            existing.professionalIds()
        );

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(existing));
        when(reservationRepository.updateMetadata(any(), any(), any(), any())).thenReturn(updated);

        Reservation result = reservationService.updateMetadata(
            reservationId,
            "  CASO-NEW  ",
            "  Nueva descripcion  "
        );

        assertThat(result.referenceCode()).isEqualTo("CASO-NEW");
        assertThat(result.description()).isEqualTo("Nueva descripcion");
    }

    @Test
    void updateMetadataShouldRejectBlankPayload() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation(reservationId, ReservationStatus.HOLD)));

        assertThatThrownBy(() -> reservationService.updateMetadata(reservationId, " ", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("referenceCode or description");
    }

    @Test
    void createBatchShouldCreateAllRequestedSessions() {
        UUID professionalId = UUID.randomUUID();
        when(professionalService.getById(professionalId)).thenReturn(activeProfessional(professionalId));
        when(reservationRepository.createBatchAtomic(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Reservation> created = reservationService.createBatch(new CreateBatchReservationCommand(
            List.of(professionalId),
            java.time.LocalTime.parse("08:45"),
            java.time.LocalTime.parse("09:30"),
            "BATCH-TEST",
            "Pack",
            List.of(java.time.LocalDate.parse("2026-10-19"), java.time.LocalDate.parse("2026-10-20")),
            null
        ));

        assertThat(created).hasSize(2);
        assertThat(created.get(0).status()).isEqualTo(ReservationStatus.HOLD);
    }

    @Test
    void createBatchShouldRejectDuplicatedSessionDates() {
        UUID professionalId = UUID.randomUUID();

        assertThatThrownBy(() -> reservationService.createBatch(new CreateBatchReservationCommand(
            List.of(professionalId),
            java.time.LocalTime.parse("08:45"),
            java.time.LocalTime.parse("09:30"),
            "BATCH-TEST",
            "Pack",
            List.of(java.time.LocalDate.parse("2026-10-19"), java.time.LocalDate.parse("2026-10-19")),
            null
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sessionDates must not contain duplicates");
    }

    @Test
    void createBatchWithRecurrenceShouldRespectSelectedWeekDay() {
        UUID professionalId = UUID.randomUUID();
        when(professionalService.getById(professionalId)).thenReturn(activeProfessional(professionalId));
        when(reservationRepository.createBatchAtomic(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Reservation> created = reservationService.createBatch(new CreateBatchReservationCommand(
            List.of(professionalId),
            java.time.LocalTime.parse("08:45"),
            java.time.LocalTime.parse("09:30"),
            "BATCH-FRI",
            "Viernes",
            null,
            new CreateBatchReservationCommand.Recurrence(
                java.time.LocalDate.parse("2026-10-14"),
                5,
                1,
                List.of(5)
            )
        ));

        assertThat(created).hasSize(5);
        assertThat(created)
            .allMatch(item -> item.startsAt().toLocalDate().getDayOfWeek().getValue() == 5);
    }

    private Professional activeProfessional(UUID id) {
        LocalDateTime now = LocalDateTime.parse("2026-10-12T10:00:00");
        return new Professional(id, "Profesional", "KINE", true, now, now);
    }

    private Reservation reservation(UUID id, ReservationStatus status) {
        LocalDateTime now = LocalDateTime.parse("2026-10-12T10:00:00");
        return new Reservation(
            id,
            LocalDateTime.parse("2026-10-19T08:00:00"),
            LocalDateTime.parse("2026-10-19T08:45:00"),
            status,
            "CASO-001",
            "Descripcion",
            now,
            now,
            List.of(UUID.randomUUID())
        );
    }
}

