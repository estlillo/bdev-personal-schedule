package com.barbatosdev.bdevpersonalschedule.shared.schedule.api;

import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalBlockRule;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.InstitutionalScheduleConfig;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.InstitutionalBlockRuleResponse;
import com.barbatosdev.bdevpersonalschedule.shared.schedule.api.dto.InstitutionalScheduleResponse;

public final class InstitutionalScheduleMapper {

    private InstitutionalScheduleMapper() {
    }

    public static InstitutionalScheduleResponse toScheduleResponse(InstitutionalScheduleConfig config) {
        return new InstitutionalScheduleResponse(config.dayStart(), config.dayEnd(), config.slotMinutes());
    }

    public static InstitutionalBlockRuleResponse toBlockRuleResponse(InstitutionalBlockRule rule) {
        return new InstitutionalBlockRuleResponse(
            rule.id(),
            rule.name(),
            rule.category(),
            rule.ruleType().name(),
            rule.dayOfWeek(),
            rule.startTime(),
            rule.endTime(),
            rule.startDate(),
            rule.endDate(),
            rule.active()
        );
    }
}

