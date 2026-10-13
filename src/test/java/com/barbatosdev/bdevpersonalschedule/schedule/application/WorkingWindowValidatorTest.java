package com.barbatosdev.bdevpersonalschedule.schedule.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkingWindowValidatorTest {

    private final WorkingWindowValidator validator = new WorkingWindowValidator();

    @Test
    void shouldAcceptAdjacentWindowsInSameDay() {
        List<WorkingWindowDraft> windows = List.of(
            new WorkingWindowDraft(1, LocalTime.parse("08:00"), LocalTime.parse("12:00")),
            new WorkingWindowDraft(1, LocalTime.parse("12:00"), LocalTime.parse("17:00"))
        );

        assertThatCode(() -> validator.validate(windows)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectOverlappingWindowsInSameDay() {
        List<WorkingWindowDraft> windows = List.of(
            new WorkingWindowDraft(1, LocalTime.parse("08:00"), LocalTime.parse("12:30")),
            new WorkingWindowDraft(1, LocalTime.parse("12:00"), LocalTime.parse("17:00"))
        );

        assertThatThrownBy(() -> validator.validate(windows))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("overlap");
    }

    @Test
    void shouldRejectInvalidInterval() {
        List<WorkingWindowDraft> windows = List.of(
            new WorkingWindowDraft(1, LocalTime.parse("10:00"), LocalTime.parse("10:00"))
        );

        assertThatThrownBy(() -> validator.validate(windows))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("startTime must be before endTime");
    }
}

