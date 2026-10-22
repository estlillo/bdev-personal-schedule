package com.barbatosdev.bdevpersonalschedule.calendar.api;

import com.barbatosdev.bdevpersonalschedule.calendar.api.dto.CalendarEventResponse;
import com.barbatosdev.bdevpersonalschedule.calendar.api.dto.CreateCalendarEventRequest;
import com.barbatosdev.bdevpersonalschedule.calendar.api.dto.UpdateCalendarEventRequest;
import com.barbatosdev.bdevpersonalschedule.calendar.application.CalendarEventService;
import com.barbatosdev.bdevpersonalschedule.calendar.application.CreateCalendarEventCommand;
import com.barbatosdev.bdevpersonalschedule.calendar.application.UpdateCalendarEventCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar-events")
@Validated
public class CalendarEventController {

    private final CalendarEventService service;

    public CalendarEventController(CalendarEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<CalendarEventResponse> list(
        @NotNull @RequestParam UUID professionalId,
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.list(professionalId, from, to).stream()
            .map(CalendarEventMapper::toResponse)
            .toList();
    }

    @PostMapping
    public ResponseEntity<CalendarEventResponse> create(@Valid @RequestBody CreateCalendarEventRequest request) {
        CalendarEventResponse response = CalendarEventMapper.toResponse(service.create(new CreateCalendarEventCommand(
            request.professionalId(),
            request.startsAt(),
            request.endsAt(),
            request.category(),
            request.note()
        )));

        return ResponseEntity.created(URI.create("/api/v1/calendar-events/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public CalendarEventResponse patch(@PathVariable UUID id, @Valid @RequestBody UpdateCalendarEventRequest request) {
        return CalendarEventMapper.toResponse(service.update(id, new UpdateCalendarEventCommand(
            request.professionalId(),
            request.startsAt(),
            request.endsAt(),
            request.category(),
            request.note()
        )));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

