package com.barbatosdev.bdevpersonalschedule.reservation.api;

import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.ReservationResponse;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;

public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.id(),
            reservation.professionalIds(),
            reservation.startsAt(),
            reservation.endsAt(),
            reservation.status().name(),
            reservation.referenceCode(),
            reservation.createdAt(),
            reservation.updatedAt()
        );
    }
}

