package com.barbatosdev.bdevpersonalschedule.schedule.api;

import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.CreateScheduleExceptionRequest;
import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.ReplaceWorkingWindowsRequest;
import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.ScheduleExceptionResponse;
import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.WorkingWindowsResponse;
import com.barbatosdev.bdevpersonalschedule.schedule.application.ScheduleExceptionDraft;
import com.barbatosdev.bdevpersonalschedule.schedule.application.ScheduleService;
import com.barbatosdev.bdevpersonalschedule.schedule.application.WorkingWindowDraft;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/professionals/{professionalId}")
@Validated
public class ProfessionalScheduleController {

    private final ScheduleService scheduleService;

    public ProfessionalScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/working-windows")
    public WorkingWindowsResponse getWorkingWindows(@PathVariable UUID professionalId) {
        return new WorkingWindowsResponse(scheduleService.getWorkingWindows(professionalId).stream()
            .map(ScheduleMapper::toWorkingWindowResponse)
            .toList());
    }

    @PutMapping("/working-windows")
    public WorkingWindowsResponse replaceWorkingWindows(
        @PathVariable UUID professionalId,
        @Valid @RequestBody ReplaceWorkingWindowsRequest request
    ) {
        List<WorkingWindowDraft> drafts = request.windows().stream()
            .map(window -> new WorkingWindowDraft(window.dayOfWeek(), window.startTime(), window.endTime()))
            .toList();

        return new WorkingWindowsResponse(scheduleService.replaceWorkingWindows(professionalId, drafts).stream()
            .map(ScheduleMapper::toWorkingWindowResponse)
            .toList());
    }

    @GetMapping("/exceptions")
    public List<ScheduleExceptionResponse> getExceptions(
        @PathVariable UUID professionalId,
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return scheduleService.getExceptions(professionalId, from, to).stream()
            .map(ScheduleMapper::toScheduleExceptionResponse)
            .toList();
    }

    @PostMapping("/exceptions")
    public ResponseEntity<ScheduleExceptionResponse> createException(
        @PathVariable UUID professionalId,
        @Valid @RequestBody CreateScheduleExceptionRequest request
    ) {
        ScheduleExceptionResponse response = ScheduleMapper.toScheduleExceptionResponse(scheduleService.createException(
            professionalId,
            new ScheduleExceptionDraft(
                request.date(),
                request.startTime(),
                request.endTime(),
                request.mode(),
                request.reason()
            )
        ));

        URI location = URI.create("/api/v1/professionals/" + professionalId + "/exceptions/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/exceptions/{exceptionId}")
    public ResponseEntity<Void> deleteException(
        @PathVariable UUID professionalId,
        @PathVariable UUID exceptionId
    ) {
        scheduleService.deleteException(professionalId, exceptionId);
        return ResponseEntity.noContent().build();
    }
}

