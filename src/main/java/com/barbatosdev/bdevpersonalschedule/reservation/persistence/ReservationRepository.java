package com.barbatosdev.bdevpersonalschedule.reservation.persistence;

import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    List<Reservation> findByRange(LocalDate from, LocalDate to, ReservationStatus status);

    Optional<Reservation> findById(UUID id);

    Reservation createAtomic(Reservation reservation);

    Reservation updateStatus(UUID id, ReservationStatus status, LocalDateTime updatedAt);

    Reservation updateMetadata(UUID id, String referenceCode, String description, LocalDateTime updatedAt);
}

