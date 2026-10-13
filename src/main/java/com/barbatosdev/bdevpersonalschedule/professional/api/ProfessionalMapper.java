package com.barbatosdev.bdevpersonalschedule.professional.api;

import com.barbatosdev.bdevpersonalschedule.professional.api.dto.ProfessionalResponse;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;

final class ProfessionalMapper {

    private ProfessionalMapper() {
    }

    static ProfessionalResponse toResponse(Professional professional) {
        return new ProfessionalResponse(
            professional.id(),
            professional.displayName(),
            professional.specialty(),
            professional.active(),
            professional.createdAt(),
            professional.updatedAt()
        );
    }
}

