package com.barbatosdev.bdevpersonalschedule.professional.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfessionalRequest(
    @Size(max = 120) String displayName,
    @Size(max = 120) String specialty,
    Boolean active
) {
}

