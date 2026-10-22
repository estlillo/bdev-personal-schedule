package com.barbatosdev.bdevpersonalschedule.availability.api;

import com.barbatosdev.bdevpersonalschedule.availability.api.dto.AvailabilitySearchResponse;
import com.barbatosdev.bdevpersonalschedule.availability.api.dto.AvailabilitySlotResponse;
import com.barbatosdev.bdevpersonalschedule.availability.application.AvailabilitySearchResult;

public final class AvailabilityMapper {

    private AvailabilityMapper() {
    }

    public static AvailabilitySearchResponse toResponse(AvailabilitySearchResult result) {
        return new AvailabilitySearchResponse(
            result.slots().stream()
                .map(slot -> new AvailabilitySlotResponse(slot.startsAt(), slot.endsAt()))
                .toList(),
            result.asOf(),
            result.warning()
        );
    }
}

