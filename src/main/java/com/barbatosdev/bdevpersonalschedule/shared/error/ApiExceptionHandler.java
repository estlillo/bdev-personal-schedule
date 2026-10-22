package com.barbatosdev.bdevpersonalschedule.shared.error;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, NativeWebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation error", detail, ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex, NativeWebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage(), ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex, NativeWebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), ex.getCode(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException ex, NativeWebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, NativeWebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage(), ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, NativeWebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage(), ErrorCode.VALIDATION_ERROR, request);
    }

    @ExceptionHandler(ScheduleConflictException.class)
    ProblemDetail handleScheduleConflict(ScheduleConflictException ex, NativeWebRequest request) {
        return problem(HttpStatus.CONFLICT, "Schedule conflict", ex.getMessage(), ex.getCode(), request);
    }

    @ExceptionHandler(OutsideWorkingHoursException.class)
    ProblemDetail handleOutsideWorkingHours(OutsideWorkingHoursException ex, NativeWebRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Outside working hours", ex.getMessage(), ex.getCode(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidStatusTransitionException ex, NativeWebRequest request) {
        return problem(HttpStatus.CONFLICT, "Invalid status transition", ex.getMessage(), ex.getCode(), request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, ErrorCode code, NativeWebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("about:blank"));
        String path = request.getDescription(false).replace("uri=", "");
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("code", code.name());
        return problemDetail;
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
