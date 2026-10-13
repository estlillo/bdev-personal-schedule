package com.barbatosdev.bdevpersonalschedule.schedule.application;

import com.barbatosdev.bdevpersonalschedule.professional.application.ProfessionalService;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.ScheduleExceptionEntry;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.WorkingWindow;
import com.barbatosdev.bdevpersonalschedule.schedule.persistence.ScheduleRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final ProfessionalService professionalService;
    private final WorkingWindowValidator workingWindowValidator;

    public ScheduleService(
        ScheduleRepository repository,
        ProfessionalService professionalService,
        WorkingWindowValidator workingWindowValidator
    ) {
        this.repository = repository;
        this.professionalService = professionalService;
        this.workingWindowValidator = workingWindowValidator;
    }

    @Transactional(readOnly = true)
    public List<WorkingWindow> getWorkingWindows(UUID professionalId) {
        professionalService.getById(professionalId);
        return repository.findWorkingWindows(professionalId);
    }

    @Transactional
    public List<WorkingWindow> replaceWorkingWindows(UUID professionalId, List<WorkingWindowDraft> drafts) {
        professionalService.getById(professionalId);
        workingWindowValidator.validate(drafts);

        List<WorkingWindow> windows = drafts.stream()
            .map(draft -> new WorkingWindow(
                UUID.randomUUID(),
                professionalId,
                draft.dayOfWeek(),
                draft.startTime(),
                draft.endTime()
            ))
            .toList();

        repository.replaceWorkingWindows(professionalId, windows);
        return repository.findWorkingWindows(professionalId);
    }

    @Transactional(readOnly = true)
    public List<ScheduleExceptionEntry> getExceptions(UUID professionalId, LocalDate from, LocalDate to) {
        professionalService.getById(professionalId);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        return repository.findExceptions(professionalId, from, to);
    }

    @Transactional
    public ScheduleExceptionEntry createException(UUID professionalId, ScheduleExceptionDraft draft) {
        professionalService.getById(professionalId);
        if (!draft.startTime().isBefore(draft.endTime())) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (!"BLOCK".equals(draft.mode()) && !"ADD".equals(draft.mode())) {
            throw new IllegalArgumentException("mode must be BLOCK or ADD");
        }

        ScheduleExceptionEntry scheduleException = new ScheduleExceptionEntry(
            UUID.randomUUID(),
            professionalId,
            draft.date(),
            draft.startTime(),
            draft.endTime(),
            draft.mode(),
            normalizeNullableText(draft.reason())
        );
        return repository.saveException(scheduleException);
    }

    @Transactional
    public void deleteException(UUID professionalId, UUID exceptionId) {
        professionalService.getById(professionalId);
        boolean deleted = repository.deleteException(professionalId, exceptionId);
        if (!deleted) {
            throw new NotFoundException("Schedule exception not found: " + exceptionId);
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

