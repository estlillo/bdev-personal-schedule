package com.barbatosdev.bdevpersonalschedule.professional.application;

import com.barbatosdev.bdevpersonalschedule.professional.api.dto.CreateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.api.dto.UpdateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.professional.persistence.ProfessionalRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalService {

    private final ProfessionalRepository repository;
    private final Clock clock;

    public ProfessionalService(ProfessionalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Professional> list(Boolean active) {
        return repository.findAll(active);
    }

    @Transactional(readOnly = true)
    public Professional getById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Professional not found: " + id));
    }

    @Transactional
    public Professional create(CreateProfessionalRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Professional professional = new Professional(
            UUID.randomUUID(),
            request.displayName().trim(),
            normalizeNullableText(request.specialty()),
            true,
            now,
            now
        );
        return repository.save(professional);
    }

    @Transactional
    public Professional update(UUID id, UpdateProfessionalRequest request) {
        Professional current = getById(id);
        Professional updated = new Professional(
            current.id(),
            request.displayName() != null ? request.displayName().trim() : current.displayName(),
            request.specialty() != null ? normalizeNullableText(request.specialty()) : current.specialty(),
            request.active() != null ? request.active() : current.active(),
            current.createdAt(),
            LocalDateTime.now(clock)
        );
        return repository.update(updated);
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

