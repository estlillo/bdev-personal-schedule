package com.barbatosdev.bdevpersonalschedule.professional.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Professional(
    UUID id,
    String displayName,
    String specialty,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}

