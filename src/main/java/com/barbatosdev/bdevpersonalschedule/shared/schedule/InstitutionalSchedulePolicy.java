package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import com.barbatosdev.bdevpersonalschedule.availability.domain.IntervalOperations;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalSchedulePolicy {

    private final InstitutionalScheduleService institutionalScheduleService;
    private final IntervalOperations intervalOperations;

    public InstitutionalSchedulePolicy(
        InstitutionalScheduleService institutionalScheduleService,
        IntervalOperations intervalOperations
    ) {
        this.institutionalScheduleService = institutionalScheduleService;
        this.intervalOperations = intervalOperations;
    }

    public int slotMinutes() {
        return institutionalScheduleService.getScheduleConfig().slotMinutes();
    }

    public List<TimeInterval> workingIntervals(LocalDate date) {
        InstitutionalScheduleConfig config = institutionalScheduleService.getScheduleConfig();
        List<TimeInterval> baseIntervals = new ArrayList<>();
        addIntervalIfValid(baseIntervals, date, config.dayStart(), config.dayEnd());

        List<TimeInterval> blockIntervals = institutionalScheduleService.listActiveBlockRules().stream()
            .filter(rule -> appliesOnDate(rule, date))
            .map(rule -> new TimeInterval(date.atTime(rule.startTime()), date.atTime(rule.endTime())))
            .toList();

        return intervalOperations.subtract(baseIntervals, blockIntervals);
    }

    public List<TimeInterval> slotIntervals(LocalDate date) {
        List<TimeInterval> slots = new ArrayList<>();
        int slotMinutes = slotMinutes();
        for (TimeInterval interval : workingIntervals(date)) {
            LocalDateTime cursor = interval.startsAt();
            while (!cursor.plusMinutes(slotMinutes).isAfter(interval.endsAt())) {
                slots.add(new TimeInterval(cursor, cursor.plusMinutes(slotMinutes)));
                cursor = cursor.plusMinutes(slotMinutes);
            }
        }
        return slots;
    }

    public boolean isValidInstitutionalSlot(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null || endsAt == null) {
            return false;
        }
        if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            return false;
        }
        if (!startsAt.plusMinutes(slotMinutes()).equals(endsAt)) {
            return false;
        }

        return slotIntervals(startsAt.toLocalDate()).stream()
            .anyMatch(slot -> slot.startsAt().equals(startsAt) && slot.endsAt().equals(endsAt));
    }

    private boolean appliesOnDate(InstitutionalBlockRule rule, LocalDate date) {
        if (!rule.active()) {
            return false;
        }
        if (rule.ruleType() == InstitutionalRuleType.WEEKLY) {
            return rule.dayOfWeek() != null && rule.dayOfWeek() == date.getDayOfWeek().getValue();
        }

        return rule.startDate() != null
            && rule.endDate() != null
            && !date.isBefore(rule.startDate())
            && !date.isAfter(rule.endDate());
    }

    private void addIntervalIfValid(
        List<TimeInterval> intervals,
        LocalDate date,
        java.time.LocalTime start,
        java.time.LocalTime end
    ) {
        if (start != null && end != null && start.isBefore(end)) {
            intervals.add(new TimeInterval(date.atTime(start), date.atTime(end)));
        }
    }
}

