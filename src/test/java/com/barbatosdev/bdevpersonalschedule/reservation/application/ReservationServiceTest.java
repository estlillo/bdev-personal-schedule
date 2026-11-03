package com.barbatosdev.bdevpersonalschedule.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import com.barbatosdev.bdevpersonalschedule.availability.persistence.AvailabilityQueryRepository;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import com.barbatosdev.bdevpersonalschedule.reservation.persistence.ReservationRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.InvalidStatusTransitionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ProfessionalService professionalService;

    @Mock
    private AvailabilityQueryRepository availabilityQueryRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-12T18:00:00Z"), ZoneId.of("America/Santiago"));
        reservationService = new ReservationService(
            reservationRepository,
            professionalService,
            availabilityQueryRepository,
            new IntervalOperations(),
            clock
        );
    }

    @Test
    void createShouldRejectOutsideWorkingHours() {
        UUID professionalId = UUID.randomUUID();
        when(professionalService.getById(professionalId)).thenReturn(activeProfessional(professionalId));
        when(availabilityQueryRepository.findWorkingWindows(anyList())).thenReturn(List.of());
        when(availabilityQueryRepository.findExceptions(anyList(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> reservationService.create(new CreateReservationCommand(
            List.of(professionalId),
            LocalDateTime.parse("2026-10-19T08:00:00"),
            LocalDateTime.parse("2026-10-19T08:45:00"),
            "CASO-001",
            "Sesion inicial"
        ))).hasMessageContaining("outside effective working hours");
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

