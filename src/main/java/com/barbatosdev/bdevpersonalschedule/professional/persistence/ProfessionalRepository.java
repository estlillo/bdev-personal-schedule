package com.barbatosdev.bdevpersonalschedule.professional.persistence;

import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository {

    List<Professional> findAll(Boolean active);

    Optional<Professional> findById(UUID id);

    Professional save(Professional professional);

    Professional update(Professional professional);
}

