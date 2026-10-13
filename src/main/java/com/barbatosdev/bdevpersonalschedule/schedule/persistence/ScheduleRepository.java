package com.barbatosdev.bdevpersonalschedule.schedule.persistence;

import com.barbatosdev.bdevpersonalschedule.schedule.domain.ScheduleExceptionEntry;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.WorkingWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository {

    List<WorkingWindow> findWorkingWindows(UUID professionalId);

    void replaceWorkingWindows(UUID professionalId, List<WorkingWindow> windows);

    List<ScheduleExceptionEntry> findExceptions(UUID professionalId, LocalDate from, LocalDate to);

    ScheduleExceptionEntry saveException(ScheduleExceptionEntry scheduleException);

    boolean deleteException(UUID professionalId, UUID exceptionId);
}

