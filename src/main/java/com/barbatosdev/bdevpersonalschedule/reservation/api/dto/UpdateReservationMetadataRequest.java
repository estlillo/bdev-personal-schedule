package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateReservationMetadataRequest(
    @Size(max = 100) String referenceCode,
    @Size(max = 255) String description
) {
}

