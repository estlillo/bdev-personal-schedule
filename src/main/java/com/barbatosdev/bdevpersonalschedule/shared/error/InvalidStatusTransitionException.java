package com.barbatosdev.bdevpersonalschedule.shared.error;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public ErrorCode getCode() {
        return ErrorCode.INVALID_STATUS_TRANSITION;
    }
}

