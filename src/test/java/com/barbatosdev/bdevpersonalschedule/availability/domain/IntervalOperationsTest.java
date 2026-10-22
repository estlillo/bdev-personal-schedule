package com.barbatosdev.bdevpersonalschedule.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntervalOperationsTest {

    private final IntervalOperations operations = new IntervalOperations();

    @Test
    void mergeShouldJoinOverlappingAndAdjacentIntervals() {
        List<TimeInterval> merged = operations.merge(List.of(
            interval("2026-10-19T08:00:00", "2026-10-19T09:00:00"),
            interval("2026-10-19T09:00:00", "2026-10-19T10:00:00"),
            interval("2026-10-19T10:30:00", "2026-10-19T11:00:00")
        ));

        assertThat(merged).containsExactly(
            interval("2026-10-19T08:00:00", "2026-10-19T10:00:00"),
            interval("2026-10-19T10:30:00", "2026-10-19T11:00:00")
        );
    }

    @Test
    void subtractShouldCreateLunchGap() {
        List<TimeInterval> free = operations.subtract(
            List.of(interval("2026-10-19T08:00:00", "2026-10-19T17:00:00")),
            List.of(interval("2026-10-19T12:00:00", "2026-10-19T13:00:00"))
        );

        assertThat(free).containsExactly(
            interval("2026-10-19T08:00:00", "2026-10-19T12:00:00"),
            interval("2026-10-19T13:00:00", "2026-10-19T17:00:00")
        );
    }

    @Test
    void intersectShouldReturnOnlyCommonTime() {
        List<TimeInterval> common = operations.intersect(
            List.of(interval("2026-10-19T08:00:00", "2026-10-19T12:00:00")),
            List.of(interval("2026-10-19T09:30:00", "2026-10-19T11:00:00"))
        );

        assertThat(common).containsExactly(
            interval("2026-10-19T09:30:00", "2026-10-19T11:00:00")
        );
    }

    @Test
    void generateSlotsShouldHonorDurationAndStep() {
        List<TimeInterval> slots = operations.generateSlots(
            List.of(interval("2026-10-19T08:00:00", "2026-10-19T10:00:00")),
            45,
            15,
            100
        );

        assertThat(slots).containsExactly(
            interval("2026-10-19T08:00:00", "2026-10-19T08:45:00"),
            interval("2026-10-19T08:15:00", "2026-10-19T09:00:00"),
            interval("2026-10-19T08:30:00", "2026-10-19T09:15:00"),
            interval("2026-10-19T08:45:00", "2026-10-19T09:30:00"),
            interval("2026-10-19T09:00:00", "2026-10-19T09:45:00"),
            interval("2026-10-19T09:15:00", "2026-10-19T10:00:00")
        );
    }

    private TimeInterval interval(String startsAt, String endsAt) {
        return new TimeInterval(LocalDateTime.parse(startsAt), LocalDateTime.parse(endsAt));
    }
}

