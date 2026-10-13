package com.barbatosdev.bdevpersonalschedule.professional.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfessionalResponse(
    UUID id,
    String displayName,
    String specialty,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

