package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import com.barbatosdev.bdevpersonalschedule.availability.domain.TimeInterval;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalSchedulePolicy {

    private final InstitutionalScheduleProperties properties;

    public InstitutionalSchedulePolicy(InstitutionalScheduleProperties properties) {
        this.properties = properties;
    }

    public int slotMinutes() {
        return properties.getSlotMinutes();
    }

    public List<TimeInterval> workingIntervals(LocalDate date) {
        List<TimeInterval> intervals = new ArrayList<>();
        addIntervalIfValid(intervals, date, properties.getDayStart(), properties.getHealthyBreakStart());
        addIntervalIfValid(intervals, date, properties.getHealthyBreakEnd(), properties.getLunchBreakStart());
        addIntervalIfValid(intervals, date, properties.getLunchBreakEnd(), properties.getDayEnd());
        return intervals;
    }

    public List<TimeInterval> slotIntervals(LocalDate date) {
        List<TimeInterval> slots = new ArrayList<>();
        for (TimeInterval interval : workingIntervals(date)) {
            LocalDateTime cursor = interval.startsAt();
            while (!cursor.plusMinutes(properties.getSlotMinutes()).isAfter(interval.endsAt())) {
                slots.add(new TimeInterval(cursor, cursor.plusMinutes(properties.getSlotMinutes())));
                cursor = cursor.plusMinutes(properties.getSlotMinutes());
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
        if (!startsAt.plusMinutes(properties.getSlotMinutes()).equals(endsAt)) {
            return false;
        }

        return slotIntervals(startsAt.toLocalDate()).stream()
            .anyMatch(slot -> slot.startsAt().equals(startsAt) && slot.endsAt().equals(endsAt));
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

