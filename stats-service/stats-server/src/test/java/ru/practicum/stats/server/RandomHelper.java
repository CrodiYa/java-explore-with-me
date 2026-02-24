package ru.practicum.stats.server;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;

import static ru.practicum.dto.Formatter.FORMATTER;

public class RandomHelper {
    private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final java.util.Random random = new java.util.Random();

    public static EndpointHitDto getEndpointHitDto() {

        return EndpointHitDto.builder()
                .app(getRandomString())
                .ip(getRandomString())
                .uri("/" + getRandomString())
                .timestamp(getFormattedNow())
                .build();
    }

    public static ViewStatsDto getViewStatsDto() {
        return ViewStatsDto.builder()
                .app(getRandomString())
                .uri("/" + getRandomString())
                .hits(random.nextLong() * 10)
                .build();
    }

    public static String getRandomString() {
        int length = random.nextInt(10) + 1;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }
        return sb.toString();
    }

    public static String getFormattedNow() {
        return LocalDateTime.now().format(FORMATTER);
    }

}
