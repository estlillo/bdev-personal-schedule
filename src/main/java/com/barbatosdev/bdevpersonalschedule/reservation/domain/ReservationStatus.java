package com.barbatosdev.bdevpersonalschedule.reservation.domain;

public enum ReservationStatus {
    HOLD,
    CONFIRMED,
    CANCELLED;

    public static ReservationStatus fromValue(String value) {
        return ReservationStatus.valueOf(value.trim().toUpperCase());
    }
}

