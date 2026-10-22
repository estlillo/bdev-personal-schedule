package com.barbatosdev.bdevpersonalschedule.availability.application;

import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import java.time.LocalDateTime;
import java.util.List;

public record AvailabilitySearchResult(
    List<TimeInterval> slots,
    LocalDateTime asOf,
    String warning
) {
}

