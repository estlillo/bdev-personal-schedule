package com.barbatosdev.bdevpersonalschedule.reservation.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateReservationCommand(
    List<UUID> professionalIds,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String referenceCode,
    String description
) {
}

