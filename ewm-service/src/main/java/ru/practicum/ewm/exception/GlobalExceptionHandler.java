package ru.practicum.ewm.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAnyException(Exception ex,
                                                       HttpServletRequest request) {
        log.error("Request: [{}]", request, ex);

        return createResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                Collections.singletonMap("error", "Error"));
    }

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(ConstraintViolationException ex,
                                                                          HttpServletRequest request) {
        logInfo(ex, request);
        return createBadRequest(request.getRequestURI(), Collections.singletonMap("error", ex.getMessage()));
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
        log.info("Resolved: [{}] Request: [{}]", ex.getClass().getName(), request);
        log.debug("Request: [{request}]", ex);
    }
}
