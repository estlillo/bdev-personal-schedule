package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    List<UUID> professionalIds,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String status,
    String referenceCode,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

