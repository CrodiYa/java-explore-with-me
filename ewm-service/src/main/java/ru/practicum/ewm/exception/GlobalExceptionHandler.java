package ru.practicum.ewm.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {

        logInfo(ex, request);

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return createBadRequest(request.getRequestURI(), errors);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleMismatchViolation(Exception ex, HttpServletRequest request) {

        logInfo(ex, request);
        return createBadRequest(request.getRequestURI(), Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        logInfo(ex, request);
        return createResponseEntity(HttpStatus.NOT_FOUND,
                request.getRequestURI(),
                Collections.singletonMap("error", ex.getMessage()));

    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        logInfo(ex, request);
        return createBadRequest(request.getRequestURI(), Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException ex, HttpServletRequest request) {
        logInfo(ex, request);
        return createResponseEntity(HttpStatus.CONFLICT,
                request.getRequestURI(),
                Collections.singletonMap("error", ex.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ApiError> handleDateTimeParse(DateTimeParseException ex,
                                                        HttpServletRequest request) {
        logInfo(ex, request);
        return createBadRequest(request.getRequestURI(),
                Collections.singletonMap("error", "Invalid date format. Expected: yyyy-MM-dd HH:mm:ss"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAnyException(Exception ex,
                                                       HttpServletRequest request) {
        log.error("Request: [{}]", formatRequestInfo(request), ex);

        return createResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                Collections.singletonMap("error", "Error"));
    }

    private ResponseEntity<ApiError> createResponseEntity(HttpStatus status, String path, Map<String, String> errors) {
        ApiError error = ApiError.builder()
                .status(status.value())
                .message(status.getReasonPhrase())
                .path(path)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ApiError> createBadRequest(String path, Map<String, String> errors) {
        return createResponseEntity(
                HttpStatus.BAD_REQUEST,
                path,
                errors);
    }

    private void logInfo(Throwable ex, HttpServletRequest request) {
        log.info("Resolved: [{}] Request: [{}]", ex.getClass().getName(), formatRequestInfo(request));
        log.debug("Request: [{}]", formatRequestInfo(request), ex);
    }

    private String formatRequestInfo(HttpServletRequest request) {
        return String.format("method=%s, uri=%s, query=%s, remoteAddress=%s",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getRemoteAddr());
    }
}
