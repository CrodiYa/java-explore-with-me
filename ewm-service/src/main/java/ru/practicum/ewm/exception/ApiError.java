package ru.practicum.ewm.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private String message;
    private int status;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
    private String path;
}