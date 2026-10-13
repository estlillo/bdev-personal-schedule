package com.barbatosdev.bdevpersonalschedule.shared.error;

public class NotFoundException extends RuntimeException {

    private final ErrorCode code;

    public NotFoundException(String message) {
        super(message);
        this.code = ErrorCode.NOT_FOUND;
    }

    public ErrorCode getCode() {
        return code;
    }
}

