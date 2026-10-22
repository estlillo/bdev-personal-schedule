package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateReservationStatusRequest(
    @NotBlank @Pattern(regexp = "HOLD|CONFIRMED|CANCELLED") String status
) {
}

