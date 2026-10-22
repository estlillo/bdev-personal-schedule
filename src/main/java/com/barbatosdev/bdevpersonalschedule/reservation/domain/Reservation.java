package com.barbatosdev.bdevpersonalschedule.reservation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record Reservation(
    UUID id,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    ReservationStatus status,
    String referenceCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<UUID> professionalIds
) {
}

