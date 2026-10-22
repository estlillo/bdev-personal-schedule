package com.barbatosdev.bdevpersonalschedule.shared.error;

public class ScheduleConflictException extends RuntimeException {

    private final ErrorCode code;

    public ScheduleConflictException(String message) {
        super(message);
        this.code = ErrorCode.SCHEDULE_CONFLICT;
    }

    public ErrorCode getCode() {
        return code;
    }
}

