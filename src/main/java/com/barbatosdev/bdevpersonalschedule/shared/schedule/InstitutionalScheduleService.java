package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalScheduleService {

    private final InstitutionalScheduleRepository repository;
    private final InstitutionalScheduleProperties fallbackProperties;
    private final Clock clock;

    public InstitutionalScheduleService(
        InstitutionalScheduleRepository repository,
        InstitutionalScheduleProperties fallbackProperties,
        Clock clock
    ) {
        this.repository = repository;
        this.fallbackProperties = fallbackProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InstitutionalScheduleConfig getScheduleConfig() {
        return repository.findScheduleConfig().orElseGet(this::defaultConfig);
    }

    @Transactional
    public InstitutionalScheduleConfig updateScheduleConfig(LocalTime dayStart, LocalTime dayEnd, Integer slotMinutes) {
        if (dayStart == null || dayEnd == null || slotMinutes == null) {
            throw new IllegalArgumentException("dayStart, dayEnd and slotMinutes are required");
        }
        if (!dayStart.isBefore(dayEnd)) {
            throw new IllegalArgumentException("dayStart must be before dayEnd");
        }
        if (slotMinutes <= 0) {
            throw new IllegalArgumentException("slotMinutes must be positive");
        }

        return repository.saveScheduleConfig(new InstitutionalScheduleConfig(
            dayStart,
            dayEnd,
            slotMinutes,
            LocalDateTime.now(clock)
        ));
    }

    @Transactional(readOnly = true)
    public List<InstitutionalBlockRule> listBlockRules(Boolean active) {
        return repository.findBlockRules(active);
    }

    @Transactional
    public InstitutionalBlockRule createBlockRule(InstitutionalBlockDraft draft) {
        InstitutionalBlockRule normalized = toRule(UUID.randomUUID(), draft, LocalDateTime.now(clock), null);
        return repository.saveBlockRule(normalized);
    }

    @Transactional
    public InstitutionalBlockRule updateBlockRule(UUID ruleId, InstitutionalBlockDraft draft) {
        InstitutionalBlockRule existing = repository.findBlockRuleById(ruleId)
            .orElseThrow(() -> new NotFoundException("Institutional block rule not found: " + ruleId));

        InstitutionalBlockRule normalized = toRule(ruleId, draft, existing.createdAt(), LocalDateTime.now(clock));
        return repository.updateBlockRule(normalized);
    }

    @Transactional
    public void deleteBlockRule(UUID ruleId) {
        boolean deleted = repository.deleteBlockRule(ruleId);
        if (!deleted) {
            throw new NotFoundException("Institutional block rule not found: " + ruleId);
        }
    }

    @Transactional(readOnly = true)
    public List<InstitutionalBlockRule> listActiveBlockRules() {
        return repository.findBlockRules(true);
    }

    private InstitutionalBlockRule toRule(
        UUID id,
        InstitutionalBlockDraft draft,
        LocalDateTime createdAt,
        LocalDateTime forcedUpdatedAt
    ) {
        if (draft == null) {
            throw new IllegalArgumentException("block rule payload is required");
        }
        if (draft.name() == null || draft.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (draft.startTime() == null || draft.endTime() == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        if (!draft.startTime().isBefore(draft.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        InstitutionalRuleType ruleType = InstitutionalRuleType.fromValue(draft.ruleType());
        Integer dayOfWeek = draft.dayOfWeek();
        LocalDate startDate = draft.startDate();
        LocalDate endDate = draft.endDate();

        if (ruleType == InstitutionalRuleType.WEEKLY) {
            if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
                throw new IllegalArgumentException("dayOfWeek must be between 1 and 7 for WEEKLY rules");
            }
            if (startDate != null || endDate != null) {
                throw new IllegalArgumentException("startDate and endDate must be null for WEEKLY rules");
            }
        }

        if (ruleType == InstitutionalRuleType.DATE_RANGE) {
            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("startDate and endDate are required for DATE_RANGE rules");
            }
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("startDate must be before or equal to endDate");
            }
            if (dayOfWeek != null) {
                throw new IllegalArgumentException("dayOfWeek must be null for DATE_RANGE rules");
            }
        }

        String category = normalizeNullableText(draft.category());
        String normalizedCategory = category == null ? "OTHER" : category.toUpperCase();
        LocalDateTime now = forcedUpdatedAt != null ? forcedUpdatedAt : LocalDateTime.now(clock);

        return new InstitutionalBlockRule(
            id,
            draft.name().trim(),
            normalizedCategory,
            ruleType,
            dayOfWeek,
            draft.startTime(),
            draft.endTime(),
            startDate,
            endDate,
            draft.active(),
            createdAt != null ? createdAt : now,
            now
        );
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InstitutionalScheduleConfig defaultConfig() {
        return new InstitutionalScheduleConfig(
            fallbackProperties.getDayStart(),
            fallbackProperties.getDayEnd(),
            fallbackProperties.getSlotMinutes(),
            LocalDateTime.now(clock)
        );
    }
}

