package ru.practicum.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatterTest {

    @Test
    void shouldFormatInstantToString() {
        Instant instant = LocalDateTime.of(2025, 3, 15, 14, 30, 0)
                .toInstant(ZoneOffset.UTC);
        String expected = "2025-03-15 14:30:00";
        assertEquals(expected, Formatter.format(instant));
    }

    @Test
    void shouldFormatLocalDateTimeToString() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 3, 15, 14, 30, 0);
        String expected = "2025-03-15 14:30:00";
        assertEquals(expected, Formatter.format(dateTime));
    }

    @Test
    void shouldParseStringToInstant() {
        String text = "2025-03-15 14:30:00";
        Instant expected = LocalDateTime.of(2025, 3, 15, 14, 30, 0)
                .toInstant(ZoneOffset.UTC);
        assertEquals(expected, Formatter.toInstant(text));
    }

    @Test
    void shouldParseStringToLocalDateTime() {
        String text = "2025-03-15 14:30:00";
        LocalDateTime expected = LocalDateTime.of(2025, 3, 15, 14, 30, 0);
        assertEquals(expected, Formatter.toLocalDateTime(text));
    }

    @Test
    void shouldThrowExceptionWhenParsingInvalidInstantString() {
        String invalidText = "2025/03/15 14:30:00";
        assertThrows(DateTimeParseException.class, () -> Formatter.toInstant(invalidText));
    }

    @Test
    void shouldThrowExceptionWhenParsingInvalidLocalDateTimeString() {
        String invalidText = "2025/03/15 14:30:00";
        assertThrows(DateTimeParseException.class, () -> Formatter.toLocalDateTime(invalidText));
    }
}
