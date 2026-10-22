package com.barbatosdev.bdevpersonalschedule.availability.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilitySearchResponse(
    List<AvailabilitySlotResponse> slots,
    LocalDateTime asOf,
    String warning
) {
}

