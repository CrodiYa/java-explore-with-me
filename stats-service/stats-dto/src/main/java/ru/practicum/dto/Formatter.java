package ru.practicum.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Formatter {
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Pattern: {@value #PATTERN}
     */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN);


    /**
     * Formats an {@link Instant} to a string using the UTC time zone.
     * The output format is {@value #PATTERN}.
     *
     * @param instant the instant to format, not null
     * @return the formatted date-time string in pattern {@value #PATTERN}
     */
    public static String format(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).format(FORMATTER);
    }

    /**
     * Parses a text string into an {@link Instant} using the UTC time zone.
     * The text must be in the format {@value #PATTERN}.
     *
     * @param text the text to parse in pattern {@value #PATTERN}, not null
     * @return the parsed instant
     * @throws java.time.format.DateTimeParseException if the text cannot be parsed
     */
    public static Instant toInstant(String text) {
        return LocalDateTime.parse(text, FORMATTER).toInstant(ZoneOffset.UTC);
    }
}
