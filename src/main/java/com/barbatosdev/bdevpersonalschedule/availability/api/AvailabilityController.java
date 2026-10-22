package com.barbatosdev.bdevpersonalschedule.availability.api;

import com.barbatosdev.bdevpersonalschedule.availability.api.dto.AvailabilitySearchRequest;
import com.barbatosdev.bdevpersonalschedule.availability.api.dto.AvailabilitySearchResponse;
import com.barbatosdev.bdevpersonalschedule.availability.application.AvailabilitySearchCommand;
import com.barbatosdev.bdevpersonalschedule.availability.application.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/availability")
@Validated
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/search")
    public AvailabilitySearchResponse search(@Valid @RequestBody AvailabilitySearchRequest request) {
        return AvailabilityMapper.toResponse(availabilityService.search(new AvailabilitySearchCommand(
            request.professionalIds(),
            request.startDate(),
            request.endDate(),
            request.durationMinutes(),
            request.stepMinutes()
        )));
    }
}

