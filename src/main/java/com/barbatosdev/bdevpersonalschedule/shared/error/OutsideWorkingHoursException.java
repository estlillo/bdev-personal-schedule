package com.barbatosdev.bdevpersonalschedule.shared.error;

public class OutsideWorkingHoursException extends RuntimeException {

    public OutsideWorkingHoursException(String message) {
        super(message);
    }

    public ErrorCode getCode() {
        return ErrorCode.OUTSIDE_WORKING_HOURS;
    }
}

