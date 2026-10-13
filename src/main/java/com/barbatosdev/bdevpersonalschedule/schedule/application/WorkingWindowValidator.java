package com.barbatosdev.bdevpersonalschedule.schedule.application;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkingWindowValidator {

    public void validate(List<WorkingWindowDraft> windows) {
        Map<Integer, List<WorkingWindowDraft>> byDay = windows.stream()
            .peek(this::validateInterval)
            .collect(java.util.stream.Collectors.groupingBy(WorkingWindowDraft::dayOfWeek));

        for (List<WorkingWindowDraft> sameDay : byDay.values()) {
            List<WorkingWindowDraft> sorted = sameDay.stream()
                .sorted(Comparator.comparing(WorkingWindowDraft::startTime))
                .toList();

            for (int i = 1; i < sorted.size(); i++) {
                WorkingWindowDraft previous = sorted.get(i - 1);
                WorkingWindowDraft current = sorted.get(i);
                if (overlaps(previous.startTime(), previous.endTime(), current.startTime(), current.endTime())) {
                    throw new IllegalArgumentException("Working windows overlap for dayOfWeek=" + current.dayOfWeek());
                }
            }
        }
    }

    private void validateInterval(WorkingWindowDraft window) {
        if (!window.startTime().isBefore(window.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }
}

