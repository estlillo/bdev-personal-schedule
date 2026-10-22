package com.barbatosdev.bdevpersonalschedule.availability.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IntervalOperations {

    public List<TimeInterval> merge(List<TimeInterval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparing(TimeInterval::startsAt));

        List<TimeInterval> merged = new ArrayList<>();
        TimeInterval current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeInterval next = sorted.get(i);
            if (!next.startsAt().isAfter(current.endsAt())) {
                LocalDateTime end = current.endsAt().isAfter(next.endsAt()) ? current.endsAt() : next.endsAt();
                current = new TimeInterval(current.startsAt(), end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    public List<TimeInterval> subtract(List<TimeInterval> baseIntervals, List<TimeInterval> blockedIntervals) {
        if (baseIntervals.isEmpty()) {
            return List.of();
        }
        if (blockedIntervals.isEmpty()) {
            return merge(baseIntervals);
        }

        List<TimeInterval> bases = merge(baseIntervals);
        List<TimeInterval> blocks = merge(blockedIntervals);
        List<TimeInterval> result = new ArrayList<>();

        for (TimeInterval base : bases) {
            List<TimeInterval> segments = new ArrayList<>();
            segments.add(base);

            for (TimeInterval block : blocks) {
                List<TimeInterval> nextSegments = new ArrayList<>();
                for (TimeInterval segment : segments) {
                    if (!segment.overlaps(block)) {
                        nextSegments.add(segment);
                        continue;
                    }

                    if (block.startsAt().isAfter(segment.startsAt())) {
                        nextSegments.add(new TimeInterval(segment.startsAt(), min(block.startsAt(), segment.endsAt())));
                    }
                    if (block.endsAt().isBefore(segment.endsAt())) {
                        nextSegments.add(new TimeInterval(max(block.endsAt(), segment.startsAt()), segment.endsAt()));
                    }
                }
                segments = nextSegments;
                if (segments.isEmpty()) {
                    break;
                }
            }

            result.addAll(segments);
        }

        return merge(result);
    }

    public List<TimeInterval> intersect(List<TimeInterval> leftIntervals, List<TimeInterval> rightIntervals) {
        if (leftIntervals.isEmpty() || rightIntervals.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> left = merge(leftIntervals);
        List<TimeInterval> right = merge(rightIntervals);
        List<TimeInterval> intersections = new ArrayList<>();

        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            TimeInterval a = left.get(i);
            TimeInterval b = right.get(j);

            LocalDateTime startsAt = max(a.startsAt(), b.startsAt());
            LocalDateTime endsAt = min(a.endsAt(), b.endsAt());
            if (startsAt.isBefore(endsAt)) {
                intersections.add(new TimeInterval(startsAt, endsAt));
            }

            if (a.endsAt().isBefore(b.endsAt())) {
                i++;
            } else {
                j++;
            }
        }

        return intersections;
    }

    public List<TimeInterval> generateSlots(
        List<TimeInterval> availabilityIntervals,
        int durationMinutes,
        int stepMinutes,
        int maxReturnedSlots
    ) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        if (stepMinutes <= 0) {
            throw new IllegalArgumentException("stepMinutes must be positive");
        }
        if (maxReturnedSlots <= 0) {
            throw new IllegalArgumentException("maxReturnedSlots must be positive");
        }

        List<TimeInterval> merged = merge(availabilityIntervals);
        List<TimeInterval> slots = new ArrayList<>();

        for (TimeInterval interval : merged) {
            LocalDateTime cursor = alignToStep(interval.startsAt(), stepMinutes);
            while (!cursor.plusMinutes(durationMinutes).isAfter(interval.endsAt())) {
                slots.add(new TimeInterval(cursor, cursor.plusMinutes(durationMinutes)));
                if (slots.size() >= maxReturnedSlots) {
                    return slots;
                }
                cursor = cursor.plusMinutes(stepMinutes);
            }
        }

        return slots;
    }

    private LocalDateTime alignToStep(LocalDateTime dateTime, int stepMinutes) {
        LocalDateTime normalized = dateTime.withSecond(0).withNano(0);
        if (normalized.isBefore(dateTime)) {
            normalized = normalized.plusMinutes(1);
        }

        int minuteOfDay = normalized.getHour() * 60 + normalized.getMinute();
        int remainder = minuteOfDay % stepMinutes;
        if (remainder == 0) {
            return normalized;
        }
        return normalized.plusMinutes(stepMinutes - remainder);
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}

