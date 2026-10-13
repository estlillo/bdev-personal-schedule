package com.barbatosdev.bdevpersonalschedule.schedule.api;

import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.ScheduleExceptionResponse;
import com.barbatosdev.bdevpersonalschedule.schedule.api.dto.WorkingWindowResponse;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.ScheduleExceptionEntry;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.WorkingWindow;

final class ScheduleMapper {

    private ScheduleMapper() {
    }

    static WorkingWindowResponse toWorkingWindowResponse(WorkingWindow window) {
        return new WorkingWindowResponse(window.dayOfWeek(), window.startTime(), window.endTime());
    }

    static ScheduleExceptionResponse toScheduleExceptionResponse(ScheduleExceptionEntry scheduleException) {
        return new ScheduleExceptionResponse(
            scheduleException.id(),
            scheduleException.date(),
            scheduleException.startTime(),
            scheduleException.endTime(),
            scheduleException.mode(),
            scheduleException.reason()
        );
    }
}

