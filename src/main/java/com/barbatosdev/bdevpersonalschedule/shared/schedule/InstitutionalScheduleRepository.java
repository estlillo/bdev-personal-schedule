package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionalScheduleRepository {

    Optional<InstitutionalScheduleConfig> findScheduleConfig();

    InstitutionalScheduleConfig saveScheduleConfig(InstitutionalScheduleConfig config);

    List<InstitutionalBlockRule> findBlockRules(Boolean active);

    Optional<InstitutionalBlockRule> findBlockRuleById(UUID ruleId);

    InstitutionalBlockRule saveBlockRule(InstitutionalBlockRule rule);

    InstitutionalBlockRule updateBlockRule(InstitutionalBlockRule rule);

    boolean deleteBlockRule(UUID ruleId);
}

