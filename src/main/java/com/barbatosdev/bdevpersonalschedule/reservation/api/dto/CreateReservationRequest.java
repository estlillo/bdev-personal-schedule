package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateReservationRequest(
    @NotEmpty List<@NotNull UUID> professionalIds,
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt,
    @Size(max = 100) String referenceCode
) {
}

