package com.barbatosdev.bdevpersonalschedule.reservation.api;

import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.CreateReservationRequest;
import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.BatchReservationResponse;
import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.CreateBatchReservationRequest;
import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.ReservationResponse;
import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.UpdateReservationMetadataRequest;
import com.barbatosdev.bdevpersonalschedule.reservation.api.dto.UpdateReservationStatusRequest;
import com.barbatosdev.bdevpersonalschedule.reservation.application.CreateBatchReservationCommand;
import com.barbatosdev.bdevpersonalschedule.reservation.application.CreateReservationCommand;
import com.barbatosdev.bdevpersonalschedule.reservation.application.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/reservations")
@Validated
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationResponse> list(
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String status
    ) {
        return service.list(from, to, status).stream()
            .map(ReservationMapper::toResponse)
            .toList();
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = ReservationMapper.toResponse(service.create(new CreateReservationCommand(
            request.professionalIds(),
            request.startsAt(),
            request.endsAt(),
            request.referenceCode(),
            request.description()
        )));

        return ResponseEntity.created(URI.create("/api/v1/reservations/" + response.id())).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchReservationResponse> createBatch(@Valid @RequestBody CreateBatchReservationRequest request) {
        CreateBatchReservationCommand.Recurrence recurrence = null;
        if (request.recurrence() != null) {
            recurrence = new CreateBatchReservationCommand.Recurrence(
                request.recurrence().startDate(),
                request.recurrence().occurrences(),
                request.recurrence().intervalWeeks(),
                request.recurrence().daysOfWeek()
            );
        }

        List<ReservationResponse> reservations = service.createBatch(new CreateBatchReservationCommand(
                request.professionalIds(),
                request.startTime(),
                request.endTime(),
                request.referenceCode(),
                request.description(),
                request.sessionDates(),
                recurrence
            ))
            .stream()
            .map(ReservationMapper::toResponse)
            .toList();

        BatchReservationResponse response = new BatchReservationResponse(
            reservations.size(),
            reservations.size(),
            reservations
        );

        return ResponseEntity.created(URI.create("/api/v1/reservations")).body(response);
    }

    @PatchMapping("/{id}/status")
    public ReservationResponse patchStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateReservationStatusRequest request
    ) {
        return ReservationMapper.toResponse(service.updateStatus(id, request.status()));
    }

    @PatchMapping("/{id}")
    public ReservationResponse patchMetadata(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateReservationMetadataRequest request
    ) {
        return ReservationMapper.toResponse(service.updateMetadata(id, request.referenceCode(), request.description()));
    }
}

