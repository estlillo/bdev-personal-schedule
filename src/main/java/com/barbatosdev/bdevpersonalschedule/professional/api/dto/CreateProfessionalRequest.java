package com.barbatosdev.bdevpersonalschedule.professional.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfessionalRequest(
    @NotBlank @Size(max = 120) String displayName,
    @Size(max = 120) String specialty
) {
}

