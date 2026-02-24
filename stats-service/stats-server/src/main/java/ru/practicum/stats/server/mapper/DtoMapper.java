package ru.practicum.stats.server.mapper;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.stats.server.model.EndpointHit;

import java.time.LocalDateTime;

import static ru.practicum.dto.Formatter.FORMATTER;

public class DtoMapper {

    public static EndpointHit toEndpoint(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .ip(dto.getIp())
                .uri(dto.getUri())
                .createDate(LocalDateTime.parse(dto.getTimestamp(), FORMATTER))
                .build();
    }

    public static EndpointHitDto from(EndpointHit hit) {
        return EndpointHitDto.builder()
                .ip(hit.getIp())
                .app(hit.getApp())
                .uri(hit.getUri())
                .timestamp(hit.getCreateDate().format(FORMATTER))
                .build();
    }
}
