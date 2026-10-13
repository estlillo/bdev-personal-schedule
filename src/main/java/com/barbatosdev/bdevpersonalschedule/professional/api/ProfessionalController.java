package com.barbatosdev.bdevpersonalschedule.professional.api;

import com.barbatosdev.bdevpersonalschedule.professional.api.dto.CreateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.api.dto.ProfessionalResponse;
import com.barbatosdev.bdevpersonalschedule.professional.api.dto.UpdateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/professionals")
@Validated
public class ProfessionalController {

    private final ProfessionalService service;

    public ProfessionalController(ProfessionalService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfessionalResponse> list(@RequestParam(required = false) Boolean active) {
        return service.list(active).stream()
            .map(ProfessionalMapper::toResponse)
            .toList();
    }

    @GetMapping("/{id}")
    public ProfessionalResponse getById(@PathVariable UUID id) {
        return ProfessionalMapper.toResponse(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody CreateProfessionalRequest request) {
        ProfessionalResponse response = ProfessionalMapper.toResponse(service.create(request));
        return ResponseEntity.created(URI.create("/api/v1/professionals/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ProfessionalResponse patch(@PathVariable UUID id, @Valid @RequestBody UpdateProfessionalRequest request) {
        return ProfessionalMapper.toResponse(service.update(id, request));
    }
}

