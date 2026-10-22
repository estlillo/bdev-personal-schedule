package com.barbatosdev.bdevpersonalschedule.availability.domain;

import java.time.LocalDateTime;

public record TimeInterval(LocalDateTime startsAt, LocalDateTime endsAt) {

    public TimeInterval {
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("startsAt and endsAt are required");
        }
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("startsAt must be before endsAt");
        }
    }

    public boolean overlaps(TimeInterval other) {
        return startsAt.isBefore(other.endsAt()) && other.startsAt().isBefore(endsAt);
    }
}

