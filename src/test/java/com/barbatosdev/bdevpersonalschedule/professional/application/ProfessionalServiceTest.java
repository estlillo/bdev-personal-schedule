package com.barbatosdev.bdevpersonalschedule.professional.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.barbatosdev.bdevpersonalschedule.professional.api.dto.CreateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.api.dto.UpdateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.professional.persistence.ProfessionalRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @Mock
    private ProfessionalRepository repository;

    private ProfessionalService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("America/Santiago"));
        service = new ProfessionalService(repository, fixedClock);
    }

    @Test
    void createShouldTrimValuesAndSetActive() {
        when(repository.save(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Professional created = service.create(new CreateProfessionalRequest("  Profesional A  ", "  FONO  "));

        assertThat(created.id()).isNotNull();
        assertThat(created.displayName()).isEqualTo("Profesional A");
        assertThat(created.specialty()).isEqualTo("FONO");
        assertThat(created.active()).isTrue();
    }

    @Test
    void getByIdShouldThrowWhenProfessionalDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateShouldPatchOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Professional current = new Professional(
            id,
            "Nombre",
            "KINESIO",
            true,
            LocalDateTime.parse("2026-10-12T08:00:00"),
            LocalDateTime.parse("2026-10-12T08:00:00")
        );

        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.update(any(Professional.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Professional updated = service.update(id, new UpdateProfessionalRequest(null, "  ", false));

        assertThat(updated.displayName()).isEqualTo("Nombre");
        assertThat(updated.specialty()).isNull();
        assertThat(updated.active()).isFalse();
    }

    @Test
    void listShouldDelegateFilter() {
        when(repository.findAll(true)).thenReturn(List.of());

        List<Professional> professionals = service.list(true);

        assertThat(professionals).isEmpty();
    }
}

