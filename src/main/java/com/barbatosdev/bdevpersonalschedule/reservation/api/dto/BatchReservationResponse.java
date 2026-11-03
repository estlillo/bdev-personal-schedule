package com.barbatosdev.bdevpersonalschedule.reservation.api.dto;

import java.util.List;

public record BatchReservationResponse(
    int requestedSessions,
    int createdSessions,
    List<ReservationResponse> reservations
) {
}

