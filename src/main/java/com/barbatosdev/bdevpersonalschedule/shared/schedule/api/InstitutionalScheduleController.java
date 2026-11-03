package com.barbatosdev.bdevpersonalschedule.shared.schedule.api;

import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalBlockDraft;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.InstitutionalBlockRuleRequest;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.InstitutionalBlockRuleResponse;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.InstitutionalScheduleResponse;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.UpdateInstitutionalScheduleRequest;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalScheduleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/institutional-schedule")
@Validated
public class InstitutionalScheduleController {

    private final InstitutionalScheduleService service;

    public InstitutionalScheduleController(InstitutionalScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public InstitutionalScheduleResponse getSchedule() {
        return InstitutionalScheduleMapper.toScheduleResponse(service.getScheduleConfig());
    }

    @PutMapping
    public InstitutionalScheduleResponse updateSchedule(@Valid @RequestBody UpdateInstitutionalScheduleRequest request) {
        return InstitutionalScheduleMapper.toScheduleResponse(service.updateScheduleConfig(
            request.dayStart(),
            request.dayEnd(),
            request.slotMinutes()
        ));
    }

    @GetMapping("/blocks")
    public List<InstitutionalBlockRuleResponse> listBlocks(@RequestParam(required = false) Boolean active) {
        return service.listBlockRules(active).stream()
            .map(InstitutionalScheduleMapper::toBlockRuleResponse)
            .toList();
    }

    @PostMapping("/blocks")
    public ResponseEntity<InstitutionalBlockRuleResponse> createBlock(@Valid @RequestBody InstitutionalBlockRuleRequest request) {
        var created = service.createBlockRule(toDraft(request));
        InstitutionalBlockRuleResponse response = InstitutionalScheduleMapper.toBlockRuleResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/institutional-schedule/blocks/" + response.id())).body(response);
    }

    @PatchMapping("/blocks/{blockId}")
    public InstitutionalBlockRuleResponse updateBlock(
        @PathVariable UUID blockId,
        @Valid @RequestBody InstitutionalBlockRuleRequest request
    ) {
        return InstitutionalScheduleMapper.toBlockRuleResponse(service.updateBlockRule(blockId, toDraft(request)));
    }

    @DeleteMapping("/blocks/{blockId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable UUID blockId) {
        service.deleteBlockRule(blockId);
        return ResponseEntity.noContent().build();
    }

    private InstitutionalBlockDraft toDraft(InstitutionalBlockRuleRequest request) {
        return new InstitutionalBlockDraft(
            request.name(),
            request.category(),
            request.ruleType(),
            request.dayOfWeek(),
            request.startTime(),
            request.endTime(),
            request.startDate(),
            request.endDate(),
            request.active()
        );
    }
}

