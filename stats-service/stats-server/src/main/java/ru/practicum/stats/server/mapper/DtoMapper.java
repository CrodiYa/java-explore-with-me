package ru.practicum.stats.server.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DtoMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static EndpointHit toEndpoint(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .ip(dto.getIp())
                .uri(dto.getUri())
                .createDate(LocalDateTime.parse(dto.getTimestamp(), FORMATTER))
                .build();
    }
}
